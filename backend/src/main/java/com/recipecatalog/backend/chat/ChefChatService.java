package com.recipecatalog.backend.chat;

import com.recipecatalog.backend.favorite.FavoriteRepository;
import com.recipecatalog.backend.recipe.Ingredient;
import com.recipecatalog.backend.recipe.IngredientNormalizer;
import com.recipecatalog.backend.recipe.Recipe;
import com.recipecatalog.backend.recipe.RecipeRepository;
import com.recipecatalog.backend.recipe.RecipeService;
import com.recipecatalog.backend.recipe.dto.RecipeSummaryDto;
import com.recipecatalog.backend.recipe.dto.ReviewNoteDto;
import com.recipecatalog.backend.user.User;
import com.recipecatalog.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChefChatService {

    private static final Logger log = LoggerFactory.getLogger(ChefChatService.class);
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int RATE_LIMIT_MAX_REQUESTS = 20;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;
    private static final long CACHE_TTL_MS = 60_000L;
    private static final List<String> DEFAULT_GEMINI_MODELS = List.of(
            "gemini-3.8-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.7-flash"
    );
    private static final long[] RETRY_BACKOFF_MS = {1000L, 2000L, 4000L};

    private final RecipeRepository recipeRepo;
    private final RecipeService recipeService;
    private final IngredientNormalizer normalizer;
    private final FavoriteRepository favoriteRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String geminiApiKey;
    private final List<String> geminiModels;
    private final String openAiApiKey;

    private final Map<String, Deque<Long>> rateBuckets = new ConcurrentHashMap<>();

    private record CachedCatalog(
            long timestampMs,
            String contextText,
            Map<Long, RecipeSummaryDto> byId,
            List<RecipeSummaryDto> ranked
    ) {}

    private volatile CachedCatalog catalogCache;

    public record ChatSuggestion(Long id, String title, String category, String imageUrl,
                                 int totalMinutes, String budgetLevel, Double estimatedCostTnd, double averageRating) {}

    public record ChatResponse(String reply, List<ChatSuggestion> suggestions, boolean offTopic, boolean aiPowered) {}

    private record ParsedAiOutput(String reply, List<Long> recipeIds) {}

    private static final List<String> OFF_TOPIC_KEYWORDS = List.of(
            "bitcoin", "crypto", "javascript", "python", "java code", "homework", "calculus",
            "president", "election", "politics", "football", "soccer", "weather", "stock market",
            "programming", "debug", "equation", "movie review", "video game"
    );

    private static final List<String> COOKING_KEYWORDS = List.of(
            "cook", "recipe", "eat", "food", "meal", "dinner", "lunch", "breakfast", "brunch",
            "dessert", "sweet", "cake", "pasta", "egg", "tomato", "chicken", "salmon", "cheese",
            "date", "romantic", "allergy", "allergic", "gluten", "dairy", "nut", "vegan", "vegetarian",
            "budget", "cheap", "cheapest", "affordable", "tnd", "dinar", "fridge", "have", "ingredient",
            "hungry", "craving", "drink", "snack", "bread", "soup", "salad", "spicy", "quick", "fast",
            "menu", "plan", "top", "best", "rank", "ranking", "rating", "review", "comment", "say about",
            "mayo", "chef", "kitchen", "recette", "manger", "cuisine", "plat", "entree", "entrée",
            "aperitif", "apéritif", "boisson", "boulange", "allergie", "substitute", "replace", "swap",
            "step", "oven", "bake", "fry", "boil", "serve", "portion", "calorie", "healthy", "website",
            "site", "favorite", "edit", "add", "hello", "hi", "hey", "bonjour", "salut", "help", "marhaba", "ahla"
    );

    public ChefChatService(RecipeRepository recipeRepo,
                           RecipeService recipeService,
                           IngredientNormalizer normalizer,
                           FavoriteRepository favoriteRepo,
                           UserRepository userRepo,
                           ObjectMapper objectMapper,
                           @Value("${GEMINI_API_KEY:}") String geminiApiKey,
                           @Value("${GEMINI_MODEL:}") String geminiModel,
                           @Value("${GEMINI_MODELS:}") String geminiModelsCsv,
                           @Value("${OPENAI_API_KEY:}") String openAiApiKey) {
        this.recipeRepo = recipeRepo;
        this.recipeService = recipeService;
        this.normalizer = normalizer;
        this.favoriteRepo = favoriteRepo;
        this.userRepo = userRepo;
        this.objectMapper = objectMapper;
        this.geminiApiKey = resolveEnv(geminiApiKey, "GEMINI_API_KEY", "");
        String primaryOverride = resolveEnv(geminiModel, "GEMINI_MODEL", "");
        String modelsCsv = resolveEnv(geminiModelsCsv, "GEMINI_MODELS", "");
        this.geminiModels = buildModelList(primaryOverride, modelsCsv);
        this.openAiApiKey = resolveEnv(openAiApiKey, "OPENAI_API_KEY", "");

        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5000);
        rf.setReadTimeout(20000);
        this.restClient = RestClient.builder().requestFactory(rf).build();

        log.info("Mr. Mayo LLM config: geminiKeyDetected={}, geminiModels={}, openAiKeyDetected={}",
                !this.geminiApiKey.isEmpty(), this.geminiModels, !this.openAiApiKey.isEmpty());
    }

    private List<String> buildModelList(String primaryOverride, String modelsCsv) {
        LinkedHashSet<String> models = new LinkedHashSet<>();
        if (modelsCsv != null && !modelsCsv.isBlank()) {
            for (String m : modelsCsv.split(",")) {
                String trimmed = m.trim();
                if (!trimmed.isEmpty()) models.add(trimmed);
            }
        }
        if (models.isEmpty()) {
            if (primaryOverride != null && !primaryOverride.isBlank()) {
                models.add(primaryOverride.trim());
            }
            models.addAll(DEFAULT_GEMINI_MODELS);
        }
        return List.copyOf(models);
    }

    private String resolveEnv(String configured, String envName, String fallback) {
        if (configured != null && !configured.isBlank()) return configured.trim();
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) return env.trim();
        return fallback;
    }

    @Transactional(readOnly = true)
    public ChatResponse respond(String rawMessage) {
        return respond(rawMessage, null, null, null, "local", null);
    }

    @Transactional(readOnly = true)
    public ChatResponse respond(String rawMessage,
                                Long recipeId,
                                Double budgetTnd,
                                List<ChefChatController.ChatTurn> history,
                                String clientKey,
                                Authentication authentication) {
        checkRateLimit(clientKey);

        String msg = rawMessage == null ? "" : rawMessage.trim();
        if (msg.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Message is too long (max " + MAX_MESSAGE_LENGTH + " characters).");
        }

        if (msg.isEmpty()) {
            return new ChatResponse(
                    "Bonjour! I'm Mr. Mayo 👨‍🍳 Tell me what's in your fridge, your allergies, your budget in TND, or ask for a romantic date-night menu!",
                    List.of(),
                    false,
                    false
            );
        }

        String lower = msg.toLowerCase(Locale.ROOT);
        Recipe activeRecipe = recipeId == null ? null : recipeRepo.findById(recipeId).orElse(null);

        // When recipeId is present, skip keyword/off-topic logic and always use the LLM if configured
        if (recipeId == null) {
            boolean explicitlyOffTopic = OFF_TOPIC_KEYWORDS.stream().anyMatch(lower::contains);
            boolean mentionsCooking = COOKING_KEYWORDS.stream().anyMatch(lower::contains);
            if (explicitlyOffTopic || (!mentionsCooking && msg.split("\\s+").length > 3 && !looksLikeIngredients(lower))) {
                return new ChatResponse(
                        "Hon hon! I'm Mr. Mayo, your kitchen chef — I only talk about cooking, recipes on Catalogue de Cuisine, date-night menus, allergies, pantry ideas, and budget meals! Ask me what to cook tonight!",
                        List.of(),
                        true,
                        false
                );
            }
        }

        CachedCatalog catalog = getCatalogContext();
        User currentUser = resolveCurrentUser(authentication);

        if (hasLlmKey()) {
            String rawLlmText = callLlmWithRetriesAndFallback(msg, activeRecipe, budgetTnd, catalog, currentUser, history);
            if (rawLlmText != null && !rawLlmText.isBlank()) {
                ParsedAiOutput parsed = parseAiOutput(rawLlmText, catalog.byId());
                List<RecipeSummaryDto> matched = parsed.recipeIds().stream()
                        .map(catalog.byId()::get)
                        .filter(Objects::nonNull)
                        .distinct()
                        .limit(5)
                        .toList();
                return new ChatResponse(
                        parsed.reply(),
                        toSuggestions(matched),
                        false,
                        true
                );
            }

            if (activeRecipe != null) {
                return new ChatResponse(
                        answerFromRecipeData(lower, activeRecipe),
                        List.of(),
                        false,
                        false
                );
            }
            return new ChatResponse(
                    "Mr. Mayo is busy right now, try again in a moment.",
                    List.of(),
                    false,
                    false
            );
        }

        return respondRuleBased(msg, lower, activeRecipe, budgetTnd, catalog.ranked());
    }

    private CachedCatalog getCatalogContext() {
        long now = System.currentTimeMillis();
        CachedCatalog current = catalogCache;
        if (current != null && (now - current.timestampMs()) < CACHE_TTL_MS) {
            return current;
        }

        List<RecipeSummaryDto> ranked = recipeService.ranking();
        List<Recipe> entities = recipeRepo.findAll();
        Map<Long, List<String>> stepsById = new HashMap<>();
        for (Recipe r : entities) {
            stepsById.put(r.getId(), r.getSteps() == null ? List.of() : List.copyOf(r.getSteps()));
        }

        Map<Long, RecipeSummaryDto> byId = new LinkedHashMap<>();
        for (RecipeSummaryDto dto : ranked) {
            byId.put(dto.id(), dto);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("CATEGORIES: ENTREES (Entrees), PLATS (Main Dishes), DESSERTS, SWEETS, BREAKFAST (Brunch), APERITIFS, BOULANGE (Bakery), BOISSONS (Drinks).\n\n");
        sb.append("HOW THE SITE WORKS:\n");
        sb.append("- Users can browse/filter recipes on /recipes, add recipes on /recipes/new, and edit/delete their own recipes on /my-recipes or the recipe detail page.\n");
        sb.append("- Ingredient auto-correct automatically fixes common typos (e.g. 'egf' -> 'Eggs') and supports English, French, and Tunisian ingredients.\n");
        sb.append("- Logged-in users can save favorites (heart button, viewed on /favorites).\n");
        sb.append("- The leaderboard (/ranking) ranks recipes based ONLY on people's star ratings (1-5) using a Bayesian average; favorites are only a tie-breaker and 0-review recipes rank below reviewed ones. One review per user per recipe; authors cannot review their own recipe.\n");
        sb.append("- Recipes include an estimated cost in Tunisian Dinars (TND).\n\n");

        sb.append("TOP 10 RANKING (in exact #1 to #10 order):\n");
        int rankNum = 1;
        for (RecipeSummaryDto r : ranked.stream().limit(10).toList()) {
            sb.append("#").append(rankNum++).append(" [id=").append(r.id()).append("] ")
              .append(r.title()).append(" - ").append(r.averageRating()).append("/5 (")
              .append(r.ratingCount()).append(" reviews, ").append(r.favoriteCount()).append(" favs)\n");
        }
        sb.append("\nALL RECIPES IN DATABASE (").append(ranked.size()).append(" total):\n");

        for (RecipeSummaryDto r : ranked) {
            int totalMin = r.prepTimeMinutes() + r.cookTimeMinutes();
            String ings = r.ingredients().stream()
                    .map(i -> (i.quantity() == null || i.quantity().isBlank() ? "" : i.quantity() + " ") + i.name())
                    .collect(Collectors.joining(", "));
            List<String> steps = stepsById.getOrDefault(r.id(), List.of());
            String stepSummary = steps.isEmpty() ? "none" : String.join(" -> ", steps);
            if (stepSummary.length() > 160) stepSummary = stepSummary.substring(0, 157) + "...";

            String notes = (r.topNotes() == null || r.topNotes().isEmpty()) ? "none"
                    : r.topNotes().stream()
                        .map(n -> n.reviewerName() + " (" + n.rating() + "*): \"" + truncate(n.comment(), 70) + "\"")
                        .collect(Collectors.joining("; "));

            sb.append("- id=").append(r.id())
              .append(" | ").append(r.title())
              .append(" | cat=").append(r.category())
              .append(" | ").append(totalMin).append("min")
              .append(" | servings=").append(r.servings())
              .append(" | cost=").append(r.estimatedCostTnd() == null ? "?" : r.estimatedCostTnd()).append(" TND")
              .append(" | budget=").append(r.budgetLevel())
              .append(" | diet=").append(nullToNone(r.dietaryTags()))
              .append(" | allergens=").append(nullToNone(r.allergens()))
              .append(" | rating=").append(r.averageRating()).append("/5 (").append(r.ratingCount()).append(" reviews, ").append(r.favoriteCount()).append(" favs)")
              .append(" | author=").append(r.authorName())
              .append(" | ings=[").append(ings).append("]")
              .append(" | steps=[").append(stepSummary).append("]")
              .append(" | reviews=[").append(notes).append("]\n");
        }

        CachedCatalog refreshed = new CachedCatalog(now, sb.toString(), Collections.unmodifiableMap(byId), List.copyOf(ranked));
        catalogCache = refreshed;
        return refreshed;
    }

    private String nullToNone(String s) {
        return (s == null || s.isBlank()) ? "none" : s;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        String clean = s.replaceAll("\\s+", " ").trim();
        return clean.length() <= max ? clean : clean.substring(0, max - 3) + "...";
    }

    private User resolveCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepo.findByEmail(auth.getName()).orElse(null);
    }

    private boolean hasLlmKey() {
        return !geminiApiKey.isEmpty() || !openAiApiKey.isEmpty();
    }

    private void checkRateLimit(String clientKey) {
        String key = (clientKey == null || clientKey.isBlank()) ? "unknown" : clientKey;
        long now = Instant.now().toEpochMilli();
        Deque<Long> timestamps = rateBuckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && (now - timestamps.peekFirst()) > RATE_LIMIT_WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= RATE_LIMIT_MAX_REQUESTS) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Whoa, slow down chef! Too many messages — please wait a minute.");
            }
            timestamps.addLast(now);
        }
    }

    private String callLlmWithRetriesAndFallback(String userMessage,
                                                 Recipe activeRecipe,
                                                 Double budgetTnd,
                                                 CachedCatalog catalog,
                                                 User currentUser,
                                                 List<ChefChatController.ChatTurn> history) {
        String systemPrompt = buildSystemPrompt(activeRecipe, budgetTnd, catalog, currentUser);

        if (!geminiApiKey.isEmpty()) {
            for (String model : geminiModels) {
                String reply = callGeminiWithRetries(model, systemPrompt, userMessage, history);
                if (reply != null && !reply.isBlank()) {
                    log.info("Mr. Mayo answered using Gemini model={}", model);
                    return reply;
                }
            }
        }

        if (!openAiApiKey.isEmpty()) {
            try {
                String reply = callOpenAi(systemPrompt, userMessage, history);
                if (reply != null && !reply.isBlank()) {
                    log.info("Mr. Mayo answered using OpenAI model=gpt-4o-mini");
                    return reply;
                }
            } catch (RestClientResponseException ex) {
                log.error("OpenAI HTTP error: status={}, body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            } catch (Exception ex) {
                log.error("OpenAI call failed: {}", ex.getMessage(), ex);
            }
        }

        return null;
    }

    private String callGeminiWithRetries(String model, String systemPrompt, String userMessage, List<ChefChatController.ChatTurn> history) {
        for (int attempt = 0; attempt <= RETRY_BACKOFF_MS.length; attempt++) {
            try {
                String text = callGemini(model, systemPrompt, userMessage, history);
                if (text != null && !text.isBlank()) {
                    return text;
                }
                log.warn("Gemini model={} returned an empty response (attempt {})", model, attempt + 1);
                return null;
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                String body = ex.getResponseBodyAsString();
                log.error("Gemini HTTP error: model={}, attempt={}, status={}, body={}",
                        model, attempt + 1, status, body);

                // Never retry on 404; move directly to the next model on 404, 429, or 503 after retries
                if (status == 404) {
                    return null;
                }
                boolean retryable = (status == 503 || status == 429);
                if (!retryable || attempt == RETRY_BACKOFF_MS.length) {
                    return null;
                }
                sleepBackoff(RETRY_BACKOFF_MS[attempt]);
            } catch (Exception ex) {
                log.error("Gemini call failed: model={}, attempt={}, error={}", model, attempt + 1, ex.getMessage(), ex);
                return null;
            }
        }
        return null;
    }

    private void sleepBackoff(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    private String callGemini(String model, String systemPrompt, String userMessage, List<ChefChatController.ChatTurn> history) {
        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null) {
            int start = Math.max(0, history.size() - 6);
            for (int i = start; i < history.size(); i++) {
                ChefChatController.ChatTurn turn = history.get(i);
                if (turn.text() == null || turn.text().isBlank()) continue;
                String role = "user".equalsIgnoreCase(turn.role()) ? "user" : "model";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", turn.text()))
                ));
            }
        }
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 550,
                        "responseMimeType", "application/json"
                )
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + geminiApiKey;
        Map<String, Object> response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null) return null;
        Object candidatesObj = response.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) return null;
        Object first = candidates.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) return null;
        Object contentObj = firstMap.get("content");
        if (!(contentObj instanceof Map<?, ?> contentMap)) return null;
        Object partsObj = contentMap.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) return null;
        Object part0 = parts.get(0);
        if (!(part0 instanceof Map<?, ?> partMap)) return null;
        Object text = partMap.get("text");
        return text == null ? null : text.toString();
    }

    @SuppressWarnings("unchecked")
    private String callOpenAi(String systemPrompt, String userMessage, List<ChefChatController.ChatTurn> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (history != null) {
            int start = Math.max(0, history.size() - 6);
            for (int i = start; i < history.size(); i++) {
                ChefChatController.ChatTurn turn = history.get(i);
                if (turn.text() == null || turn.text().isBlank()) continue;
                String role = "user".equalsIgnoreCase(turn.role()) ? "user" : "assistant";
                messages.add(Map.of("role", role, "content", turn.text()));
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", messages,
                "temperature", 0.4,
                "max_tokens", 550,
                "response_format", Map.of("type", "json_object")
        );

        Map<String, Object> response = restClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + openAiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null) return null;
        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) return null;
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) return null;
        Object msgObj = firstMap.get("message");
        if (!(msgObj instanceof Map<?, ?> msgMap)) return null;
        Object content = msgMap.get("content");
        return content == null ? null : content.toString();
    }

    private ParsedAiOutput parseAiOutput(String rawText, Map<Long, RecipeSummaryDto> validRecipes) {
        String cleaned = rawText.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "").trim();
        }
        try {
            JsonNode root = objectMapper.readTree(cleaned);
            if (root != null && root.isObject() && root.has("reply")) {
                String reply = root.get("reply").asText("");
                LinkedHashSet<Long> ids = new LinkedHashSet<>();
                JsonNode idsNode = root.get("recipeIds");
                if (idsNode != null && idsNode.isArray()) {
                    for (JsonNode n : idsNode) {
                        if (n.isNumber() || n.isString()) {
                            try {
                                long id = Long.parseLong(n.asText().trim());
                                if (validRecipes.containsKey(id)) {
                                    ids.add(id);
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
                if (!reply.isBlank()) {
                    return new ParsedAiOutput(reply.trim(), List.copyOf(ids));
                }
            }
        } catch (Exception ex) {
            log.debug("Could not parse LLM JSON output, falling back to plain text: {}", ex.getMessage());
        }
        return new ParsedAiOutput(rawText.trim(), List.of());
    }

    private String answerFromRecipeData(String lower, Recipe recipe) {
        int prep = recipe.getPrepTimeMinutes();
        int cook = recipe.getCookTimeMinutes();
        int total = prep + cook;

        if (lower.contains("time") || lower.contains("long") || lower.contains("minute")
                || lower.contains("prep") || lower.contains("cook") || lower.contains("temps") || lower.contains("durée")) {
            return "Mr. Mayo is busy right now, but here's the timing for \"" + recipe.getTitle() + "\": "
                    + prep + " min prep + " + cook + " min cook = " + total + " min total.";
        }

        if (lower.contains("serving") || lower.contains("portion") || lower.contains("people")
                || lower.contains("person") || lower.contains("combien")) {
            return "Mr. Mayo is busy right now, but \"" + recipe.getTitle() + "\" makes "
                    + recipe.getServings() + " serving(s).";
        }

        if (lower.contains("allerg") || lower.contains("gluten") || lower.contains("dairy")
                || lower.contains("nut") || lower.contains("egg") || lower.contains("safe")) {
            String alg = (recipe.getAllergens() == null || recipe.getAllergens().isBlank())
                    ? "none listed (always double-check ingredient labels)"
                    : recipe.getAllergens();
            return "For \"" + recipe.getTitle() + "\", listed allergens: " + alg + ".";
        }

        if (lower.contains("ingredient") || lower.contains("ingrédient") || lower.contains("need")
                || lower.contains("what's in") || lower.contains("whats in")) {
            String ings = recipe.getIngredients().stream()
                    .map(i -> (i.getQuantity() == null || i.getQuantity().isBlank() ? "" : i.getQuantity() + " ") + i.getName())
                    .collect(Collectors.joining(", "));
            return "Ingredients for \"" + recipe.getTitle() + "\": " + ings + ".";
        }

        if (lower.contains("step") || lower.contains("how") || lower.contains("make")
                || lower.contains("prepare") || lower.contains("instruction") || lower.contains("étape")) {
            List<String> steps = recipe.getSteps();
            if (steps.isEmpty()) {
                return "Mr. Mayo is busy right now, try again in a moment.";
            }
            StringBuilder sb = new StringBuilder("Steps for \"" + recipe.getTitle() + "\":\n");
            for (int i = 0; i < steps.size(); i++) {
                sb.append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
            return sb.toString().trim();
        }

        return "Mr. Mayo is busy right now, try again in a moment. (Quick info for \"" + recipe.getTitle()
                + "\": " + total + " min total, " + recipe.getServings() + " servings, "
                + recipe.getIngredients().size() + " ingredients.)";
    }

    private String buildSystemPrompt(Recipe activeRecipe, Double budgetTnd, CachedCatalog catalog, User currentUser) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are Mr. Mayo, a warm, witty, helpful chef assistant on the 'Catalogue de Cuisine' recipe website.\n");
        sb.append("STRICT RULES:\n");
        sb.append("1. Answer ONLY from the website catalog data provided below. NEVER invent recipes, ratings, prices, reviews, or features.\n");
        sb.append("2. Recommend ONLY recipes that exist in the catalog below, using their exact numeric `id` in `recipeIds`.\n");
        sb.append("3. If unsure or if the catalog doesn't have something, say so honestly.\n");
        sb.append("4. Use the user's budget in TND if provided.\n");
        sb.append("5. Reply in the same language the user writes in (English, French, or Arabic).\n");
        sb.append("6. Keep answers short, friendly, and formatted cleanly (markdown bold/bullets allowed).\n");
        sb.append("7. For health/nutrition/calorie questions, give a rough estimate and explicitly state it is an estimate.\n");
        sb.append("8. ALWAYS return valid JSON in this exact shape: {\"reply\": \"...\", \"recipeIds\": [1, 2]} (use [] when no recipe cards should be shown).\n\n");

        if (budgetTnd != null && budgetTnd > 0) {
            sb.append("USER STATED BUDGET: ").append(String.format(Locale.ROOT, "%.1f TND", budgetTnd))
              .append(" (if unclear whether per person or total, briefly ask).\n\n");
        }

        if (currentUser != null) {
            List<Long> favIds = favoriteRepo.findRecipeIdsByUser(currentUser);
            List<Long> ownIds = recipeRepo.findByAuthorIdOrderByIdDesc(currentUser.getId())
                    .stream().map(Recipe::getId).toList();
            sb.append("CURRENT LOGGED-IN USER: ").append(currentUser.getName())
              .append(" | favoriteRecipeIds=").append(favIds)
              .append(" | ownRecipeIds=").append(ownIds).append("\n\n");
        }

        if (activeRecipe != null) {
            sb.append("CURRENTLY OPENED RECIPE:\n");
            sb.append("- id=").append(activeRecipe.getId()).append(" | Title: ").append(activeRecipe.getTitle()).append("\n");
            sb.append("- Category: ").append(activeRecipe.getCategory()).append(", Servings: ").append(activeRecipe.getServings())
              .append(", Time: ").append(activeRecipe.getPrepTimeMinutes() + activeRecipe.getCookTimeMinutes()).append(" min")
              .append(", Cost: ").append(activeRecipe.getEstimatedCostTnd() == null ? "see catalog" : activeRecipe.getEstimatedCostTnd() + " TND").append("\n");
            if (activeRecipe.getAllergens() != null && !activeRecipe.getAllergens().isBlank()) {
                sb.append("- Allergens: ").append(activeRecipe.getAllergens()).append("\n");
            }
            sb.append("- Ingredients: ").append(
                    activeRecipe.getIngredients().stream()
                            .map(i -> (i.getQuantity() == null || i.getQuantity().isBlank() ? "" : i.getQuantity() + " ") + i.getName())
                            .collect(Collectors.joining(", "))
            ).append("\n");
            if (!activeRecipe.getSteps().isEmpty()) {
                sb.append("- Steps: ").append(String.join(" | ", activeRecipe.getSteps())).append("\n");
            }
            sb.append("\n");
        }

        sb.append(catalog.contextText());
        return sb.toString();
    }

    private ChatResponse respondRuleBased(String msg, String lower, Recipe activeRecipe, Double budgetTnd, List<RecipeSummaryDto> all) {
        if (activeRecipe != null) {
            if (lower.contains("substitute") || lower.contains("replace") || lower.contains("swap") || lower.contains("remplacer")) {
                String ings = activeRecipe.getIngredients().stream().map(Ingredient::getName).collect(Collectors.joining(", "));
                return new ChatResponse(
                        "Looking at \"" + activeRecipe.getTitle() + "\" (" + ings + "): common swaps are Greek yogurt for cream, olive oil for butter, or chickpeas/tofu for meat. Which ingredient would you like to swap?",
                        List.of(),
                        false,
                        false
                );
            }
            if (lower.contains("allerg") || lower.contains("safe") || lower.contains("gluten") || lower.contains("dairy") || lower.contains("nut")) {
                String alg = activeRecipe.getAllergens() == null || activeRecipe.getAllergens().isBlank()
                        ? "none listed, though always check ingredient labels"
                        : activeRecipe.getAllergens();
                return new ChatResponse(
                        "For \"" + activeRecipe.getTitle() + "\", listed allergens are: " + alg + ". Ingredients: "
                                + activeRecipe.getIngredients().stream().map(Ingredient::getName).collect(Collectors.joining(", ")) + ".",
                        List.of(),
                        false,
                        false
                );
            }
        }

        if (lower.matches("^(hi|hello|hey|bonjour|salut|yo|hola)[!?.\\s]*$")) {
            String greeting = activeRecipe != null
                    ? "Bonjour chef! 👨‍🍳 I see you're looking at \"" + activeRecipe.getTitle() + "\". Ask me about substitutions, timing, or pairings!"
                    : "Bonjour chef! 👨‍🍳 Welcome to my kitchen corner. Need a romantic date plan, budget-friendly dishes, allergy-safe recipes, or ideas using ingredients you already have?";
            return new ChatResponse(
                    greeting,
                    toSuggestions(all.stream().limit(3).toList()),
                    false,
                    false
            );
        }

        if (lower.contains("date") || lower.contains("romantic") || lower.contains("rendez-vous") || lower.contains("anniversary")) {
            RecipeSummaryDto starter = pickFirst(all, r -> r.category().equalsIgnoreCase("ENTREES") || r.category().equalsIgnoreCase("APERITIFS"));
            RecipeSummaryDto main = pickFirst(all, r -> r.category().equalsIgnoreCase("PLATS") && (r.dietaryTags() != null && r.dietaryTags().contains("DATE_NIGHT")));
            if (main == null) main = pickFirst(all, r -> r.category().equalsIgnoreCase("PLATS"));
            RecipeSummaryDto dessert = pickFirst(all, r -> r.category().equalsIgnoreCase("DESSERTS") || r.category().equalsIgnoreCase("SWEETS"));
            RecipeSummaryDto drink = pickFirst(all, r -> r.category().equalsIgnoreCase("BOISSONS"));

            List<RecipeSummaryDto> plan = new ArrayList<>();
            if (starter != null) plan.add(starter);
            if (main != null) plan.add(main);
            if (dessert != null) plan.add(dessert);
            if (drink != null) plan.add(drink);

            StringBuilder sb = new StringBuilder("Ooooh, a romantic date-night menu! 💕 Here is Mr. Mayo's curated 4-course plan from our catalogue:\n");
            if (starter != null) sb.append("• Starter: ").append(starter.title()).append(" (").append(starter.prepTimeMinutes() + starter.cookTimeMinutes()).append(" min)\n");
            if (main != null) sb.append("• Main Course: ").append(main.title()).append(" ⭐\n");
            if (dessert != null) sb.append("• Sweet Finale: ").append(dessert.title()).append("\n");
            if (drink != null) sb.append("• Sip Pairing: ").append(drink.title()).append("\n");
            sb.append("Pro tip: Dim the lights, turn on our upbeat kitchen music in the navbar, and plate everything warm!");

            return new ChatResponse(sb.toString(), toSuggestions(plan), false, false);
        }

        List<String> detectedAllergens = detectAllergens(lower);
        boolean wantsVegan = lower.contains("vegan");
        boolean wantsVegetarian = lower.contains("vegetarian") || lower.contains("veggie") || lower.contains("végétarien");
        if (!detectedAllergens.isEmpty() || lower.contains("allergy") || lower.contains("allergic") || lower.contains("allergie") || wantsVegan || wantsVegetarian) {
            String excludeCsv = String.join(",", detectedAllergens);
            String diet = wantsVegan ? "VEGAN" : (wantsVegetarian ? "VEGETARIAN" : null);
            List<RecipeSummaryDto> safe = recipeService.list(null, null, null, null, null, excludeCsv, diet, "rating");

            if (safe.isEmpty()) {
                return new ChatResponse(
                        "Hmm, none of our current recipes match all those restrictions at once! Try adding a custom safe recipe in 'Add Recipe', or adjust the ingredient filters on the Recipes page.",
                        List.of(),
                        false,
                        false
                );
            }

            String label = !detectedAllergens.isEmpty()
                    ? "without " + String.join(", ", detectedAllergens)
                    : (wantsVegan ? "vegan-friendly" : "vegetarian");
            return new ChatResponse(
                    "Got it! Safety first in Mr. Mayo's kitchen 🛡️ Here are top-rated " + label + " recipes from our catalogue (always double-check specific ingredient brands):",
                    toSuggestions(safe.stream().limit(4).toList()),
                    false,
                    false
            );
        }

        if ((budgetTnd != null && budgetTnd > 0) || lower.contains("budget") || lower.contains("cheap") || lower.contains("broke") || lower.contains("affordable") || lower.contains("money") || lower.contains("pas cher") || lower.contains("tnd") || lower.contains("dinar")) {
            List<RecipeSummaryDto> budgetList = recipeService.list(null, null, null, null, "BUDGET", null, null, "rating");
            String budgetIntro = (budgetTnd != null && budgetTnd > 0)
                    ? String.format(Locale.ROOT, "Working with %.1f TND? Let's make it stretch! (Quick check: is that per person or total?) Here are our best budget-friendly picks:", budgetTnd)
                    : "Cooking delicious food on a budget is my favorite superpower! 💸 These budget-friendly recipes use affordable pantry staples and still taste gourmet:";
            return new ChatResponse(
                    budgetIntro,
                    toSuggestions(budgetList.stream().limit(4).toList()),
                    false,
                    false
            );
        }

        List<String> userIngredients = extractIngredients(lower, all);
        if (!userIngredients.isEmpty() || lower.contains("fridge") || lower.contains("have") || lower.contains("pantry") || lower.contains("ingredients")) {
            if (userIngredients.isEmpty()) {
                return new ChatResponse(
                        "Tell me which ingredients you have on hand (e.g., \"I have eggs, tomatoes, and cheese\") and I'll match them to our recipes right away!",
                        List.of(),
                        false,
                        false
                );
            }

            List<ScoredRecipe> scored = new ArrayList<>();
            for (RecipeSummaryDto r : all) {
                List<String> recipeIngs = r.ingredients().stream()
                        .map(i -> i.name().toLowerCase(Locale.ROOT))
                        .toList();
                int matches = 0;
                List<String> missing = new ArrayList<>();
                for (String ing : recipeIngs) {
                    boolean matched = userIngredients.stream().anyMatch(u -> ing.contains(u) || u.contains(ing));
                    if (matched) matches++;
                    else missing.add(ing);
                }
                if (matches > 0) {
                    scored.add(new ScoredRecipe(r, matches, missing));
                }
            }
            scored.sort(Comparator.comparingInt(ScoredRecipe::matches).reversed()
                    .thenComparingInt(s -> s.missing().size()));

            if (scored.isEmpty()) {
                return new ChatResponse(
                        "With " + String.join(", ", userIngredients) + ", you could whip up a quick stir-fry or omelet! Our catalogue doesn't have an exact match yet — want to add your creation?",
                        toSuggestions(all.stream().limit(2).toList()),
                        false,
                        false
                );
            }

            StringBuilder sb = new StringBuilder("Look what you can cook with " + String.join(", ", userIngredients) + "! 🍳\n");
            for (int i = 0; i < Math.min(3, scored.size()); i++) {
                ScoredRecipe sr = scored.get(i);
                sb.append("• ").append(sr.recipe().title()).append(" (matches ").append(sr.matches()).append(" ingredient(s)");
                if (!sr.missing().isEmpty()) {
                    sb.append(", just add: ").append(String.join(", ", sr.missing().stream().limit(3).toList()));
                } else {
                    sb.append(" — you have everything!");
                }
                sb.append(")\n");
            }
            return new ChatResponse(sb.toString().trim(),
                    toSuggestions(scored.stream().limit(3).map(ScoredRecipe::recipe).toList()),
                    false,
                    false);
        }

        if (lower.contains("sweet") || lower.contains("dessert") || lower.contains("sugar") || lower.contains("chocolate")) {
            List<RecipeSummaryDto> sweets = all.stream()
                    .filter(r -> r.category().equalsIgnoreCase("DESSERTS") || r.category().equalsIgnoreCase("SWEETS"))
                    .limit(4)
                    .toList();
            return new ChatResponse(
                    "Craving something sweet? 🍰 Mr. Mayo highly recommends these indulgent treats from our Desserts & Sweets shelf:",
                    toSuggestions(sweets),
                    false,
                    false
            );
        }

        if (lower.contains("quick") || lower.contains("fast") || lower.contains("15") || lower.contains("20")) {
            List<RecipeSummaryDto> fast = recipeService.list(null, null, null, 20, null, null, null, "fastest");
            return new ChatResponse(
                    "Short on time? ⚡ These delicious dishes go from pan to plate in 20 minutes or less:",
                    toSuggestions(fast.stream().limit(4).toList()),
                    false,
                    false
            );
        }

        if (lower.contains("best") || lower.contains("top") || lower.contains("rank") || lower.contains("popular")) {
            return new ChatResponse(
                    "Here are our community's highest-ranked favorites right now 🏆:",
                    toSuggestions(all.stream().limit(4).toList()),
                    false,
                    false
            );
        }

        return new ChatResponse(
                "I'm ready to help in the kitchen! 👨‍🍳 Ask me things like:\n• \"Plan a romantic date night menu\"\n• \"I have eggs and tomatoes, what can I cook?\"\n• \"Nut-free and dairy-free dinner ideas\"\n• \"Best recipes on a tight budget\"",
                toSuggestions(all.stream().limit(3).toList()),
                false,
                false
        );
    }

    private record ScoredRecipe(RecipeSummaryDto recipe, int matches, List<String> missing) {}

    private List<String> detectAllergens(String lower) {
        List<String> list = new ArrayList<>();
        if (lower.contains("nut") || lower.contains("peanut") || lower.contains("almond") || lower.contains("pistachio") || lower.contains("noix")) list.add("nuts");
        if (lower.contains("dairy") || lower.contains("lactose") || lower.contains("milk") || lower.contains("cheese") || lower.contains("lait")) list.add("dairy");
        if (lower.contains("gluten") || lower.contains("wheat") || lower.contains("flour")) list.add("gluten");
        if (lower.contains("egg") || lower.contains("oeuf")) list.add("eggs");
        if (lower.contains("pork") || lower.contains("halal") || lower.contains("porc")) list.add("pork");
        if (lower.contains("fish") || lower.contains("salmon") || lower.contains("poisson")) list.add("fish");
        return list;
    }

    private List<String> extractIngredients(String lower, List<RecipeSummaryDto> all) {
        Set<String> catalogWords = new LinkedHashSet<>();
        for (RecipeSummaryDto r : all) {
            r.ingredients().forEach(i -> {
                String n = i.name().toLowerCase(Locale.ROOT).trim();
                if (n.length() >= 3) catalogWords.add(n);
            });
        }
        List<String> found = new ArrayList<>();
        for (String w : catalogWords) {
            if (lower.contains(w) || (w.endsWith("s") && lower.contains(w.substring(0, w.length() - 1)))) {
                found.add(w);
            }
        }
        for (String token : lower.replaceAll("[^\\p{L}\\s]", " ").split("\\s+")) {
            if (token.length() < 2) continue;
            IngredientNormalizer.NormalizationResult norm = normalizer.normalize(token);
            if (norm.valid() && norm.corrected()) {
                String fixed = norm.normalized().toLowerCase(Locale.ROOT);
                if (!found.contains(fixed)) found.add(fixed);
            }
        }
        return found;
    }

    private boolean looksLikeIngredients(String lower) {
        return lower.contains(",") || lower.contains("egg") || lower.contains("tomato")
                || lower.contains("cheese") || lower.contains("chocolate") || lower.contains("chicken")
                || lower.contains("salmon") || lower.contains("potato");
    }

    private RecipeSummaryDto pickFirst(List<RecipeSummaryDto> list, java.util.function.Predicate<RecipeSummaryDto> pred) {
        return list.stream().filter(pred).findFirst().orElse(null);
    }

    private List<ChatSuggestion> toSuggestions(List<RecipeSummaryDto> list) {
        return list.stream()
                .map(r -> new ChatSuggestion(
                        r.id(),
                        r.title(),
                        r.category(),
                        r.imageUrl(),
                        r.prepTimeMinutes() + r.cookTimeMinutes(),
                        r.budgetLevel(),
                        r.estimatedCostTnd(),
                        r.averageRating()
                ))
                .toList();
    }
}
