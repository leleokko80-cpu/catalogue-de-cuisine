package com.recipecatalog.backend.recipe;

import com.recipecatalog.backend.favorite.FavoriteRepository;
import com.recipecatalog.backend.recipe.dto.*;
import com.recipecatalog.backend.review.RecipeReview;
import com.recipecatalog.backend.review.RecipeReviewRepository;
import com.recipecatalog.backend.user.User;
import com.recipecatalog.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class RecipeService {
    private static final Set<String> VALID_CATEGORIES = Set.of(
            "ENTREES", "PLATS", "DESSERTS", "SWEETS", "BREAKFAST", "APERITIFS", "BOULANGE", "BOISSONS"
    );
    private static final Set<String> VALID_BUDGETS = Set.of("BUDGET", "MODERATE", "GOURMET");

    private static final List<String> FALLBACK_NOTES = List.of(
            "Chill Chef Tip: Put on your favorite playlist before chopping — food tastes 20% better when you're vibing.",
            "Cute Kitchen Note: Taste-testing as you go isn't snacking, it's quality assurance!",
            "Chef's Secret: If garlic starts smelling amazing, you're only 30 seconds away from perfection (don't burn it!).",
            "Cozy Reminder: Leftovers tomorrow are just future-you saying thank you.",
            "Mr. Mayo's Rule: A pinch of salt and a little patience fix almost everything in the pan!"
    );

    private final RecipeRepository repo;
    private final UserRepository userRepo;
    private final RecipeReviewRepository reviewRepo;
    private final FavoriteRepository favoriteRepo;
    private final IngredientNormalizer normalizer;

    public RecipeService(RecipeRepository repo,
                         UserRepository userRepo,
                         RecipeReviewRepository reviewRepo,
                         FavoriteRepository favoriteRepo,
                         IngredientNormalizer normalizer) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.reviewRepo = reviewRepo;
        this.favoriteRepo = favoriteRepo;
        this.normalizer = normalizer;
    }

    @Transactional(readOnly = true)
    public List<RecipeSummaryDto> list(String q, String category, String ingredientsParam,
                                       Integer maxTime, String budget, String excludeAllergen,
                                       String diet, String sort) {
        List<Recipe> all = repo.findAll();

        List<String> requiredIngredients = parseCsv(ingredientsParam);
        List<String> excludedAllergens = parseCsv(excludeAllergen);
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String cat = category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
        String bud = budget == null ? "" : budget.trim().toUpperCase(Locale.ROOT);
        String dietFilter = diet == null ? "" : diet.trim().toUpperCase(Locale.ROOT);

        List<Recipe> filtered = all.stream()
                .filter(r -> matchesQuery(r, query))
                .filter(r -> cat.isEmpty() || cat.equals("ALL") || r.getCategory().equalsIgnoreCase(cat))
                .filter(r -> matchesIngredients(r, requiredIngredients))
                .filter(r -> maxTime == null || (r.getPrepTimeMinutes() + r.getCookTimeMinutes()) <= maxTime)
                .filter(r -> bud.isEmpty() || bud.equals("ALL") || r.getBudgetLevel().equalsIgnoreCase(bud))
                .filter(r -> excludesAllergens(r, excludedAllergens))
                .filter(r -> dietFilter.isEmpty() || containsToken(r.getDietaryTags(), dietFilter))
                .toList();

        List<RecipeSummaryDto> dtos = new ArrayList<>(toSummaries(filtered));

        String sortMode = sort == null ? "newest" : sort.trim().toLowerCase(Locale.ROOT);
        switch (sortMode) {
            case "rating", "ranking" -> {
                Double rawGlobal = reviewRepo.findGlobalAverageRating();
                double globalAvg = rawGlobal == null ? 0.0 : rawGlobal;
                dtos.sort(Comparator
                        .comparing((RecipeSummaryDto r) -> r.ratingCount() > 0 ? 1 : 0, Comparator.reverseOrder())
                        .thenComparing(r -> rankingScore(r, globalAvg), Comparator.reverseOrder())
                        .thenComparing(RecipeSummaryDto::ratingCount, Comparator.reverseOrder())
                        .thenComparing(RecipeSummaryDto::favoriteCount, Comparator.reverseOrder()));
            }
            case "fastest" -> dtos.sort(Comparator.comparingInt(r -> r.prepTimeMinutes() + r.cookTimeMinutes()));
            case "popular" -> dtos.sort(Comparator
                    .comparing(RecipeSummaryDto::favoriteCount, Comparator.reverseOrder())
                    .thenComparing(RecipeSummaryDto::averageRating, Comparator.reverseOrder())
                    .thenComparing(RecipeSummaryDto::ratingCount, Comparator.reverseOrder()));
            default -> dtos.sort(Comparator.comparingLong(RecipeSummaryDto::id).reversed());
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<RecipeSummaryDto> toSummaries(List<Recipe> recipes) {
        if (recipes.isEmpty()) return List.of();
        Map<Long, RatingStat> ratingStats = loadRatingStats();
        Map<Long, Long> favoriteCounts = loadFavoriteCounts();
        Map<Long, List<ReviewNoteDto>> topNotesMap = loadTopNotes();
        return recipes.stream()
                .map(r -> toSummary(r, ratingStats, favoriteCounts, topNotesMap))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecipeSummaryDto> ranking() {
        return list(null, null, null, null, null, null, null, "ranking");
    }

    @Transactional(readOnly = true)
    public RecipeDetailDto get(Long id) {
        Recipe r = repo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        return toDetail(r);
    }

    @Transactional
    public RecipeDetailDto create(CreateRecipeRequest req, Authentication auth) {
        User author = currentUser(auth);
        String cleanTitle = req.title().trim();
        ensureUniqueTitleForAuthor(cleanTitle, author, null);

        List<Ingredient> normalizedIngredients = new ArrayList<>();
        for (IngredientDto ing : req.ingredients()) {
            IngredientNormalizer.NormalizationResult result = normalizer.normalize(ing.name());
            if (!result.valid()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid ingredient '" + ing.name() + "': " + result.reason());
            }
            String qty = ing.quantity() == null ? "" : ing.quantity().trim();
            normalizedIngredients.add(new Ingredient(result.normalized(), qty));
        }

        Recipe r = new Recipe();
        r.setTitle(cleanTitle);
        r.setDescription(req.description() == null ? null : req.description().trim());
        r.setImageUrl(req.imageUrl() == null || req.imageUrl().isBlank() ? null : req.imageUrl().trim());
        r.setPrepTimeMinutes(req.prepTimeMinutes());
        r.setCookTimeMinutes(req.cookTimeMinutes());
        r.setServings(req.servings());
        r.setIngredients(normalizedIngredients);
        r.setSteps(new ArrayList<>(req.steps().stream().map(String::trim).toList()));
        r.setAuthor(author);

        String cat = req.category() == null ? "PLATS" : req.category().trim().toUpperCase(Locale.ROOT);
        r.setCategory(VALID_CATEGORIES.contains(cat) ? cat : "PLATS");

        String budget = req.budgetLevel() == null ? "MODERATE" : req.budgetLevel().trim().toUpperCase(Locale.ROOT);
        r.setBudgetLevel(VALID_BUDGETS.contains(budget) ? budget : "MODERATE");
        r.setEstimatedCostTnd(resolveCostTnd(req.estimatedCostTnd(), r.getBudgetLevel()));

        r.setFunNote(req.funNote() == null || req.funNote().isBlank()
                ? FALLBACK_NOTES.get(Math.abs(r.getTitle().hashCode()) % FALLBACK_NOTES.size())
                : req.funNote().trim());
        r.setDietaryTags(req.dietaryTags() == null ? null : req.dietaryTags().trim());
        r.setAllergens(req.allergens() == null ? null : req.allergens().trim().toLowerCase(Locale.ROOT));

        return toDetail(repo.save(r));
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        Recipe r = repo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        User current = currentUser(auth);
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        if (r.getAuthor() == null || !r.getAuthor().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own recipes");
        }
        repo.delete(r);
    }

    @Transactional
    public RecipeDetailDto update(Long id, CreateRecipeRequest req, Authentication auth) {
        Recipe r = repo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        User current = currentUser(auth);
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        if (r.getAuthor() == null || !r.getAuthor().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own recipes");
        }
        String cleanTitle = req.title().trim();
        ensureUniqueTitleForAuthor(cleanTitle, current, r.getId());

        List<Ingredient> normalizedIngredients = new ArrayList<>();
        for (IngredientDto ing : req.ingredients()) {
            IngredientNormalizer.NormalizationResult result = normalizer.normalize(ing.name());
            if (!result.valid()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid ingredient '" + ing.name() + "': " + result.reason());
            }
            String qty = ing.quantity() == null ? "" : ing.quantity().trim();
            normalizedIngredients.add(new Ingredient(result.normalized(), qty));
        }

        r.setTitle(req.title().trim());
        r.setDescription(req.description() == null ? null : req.description().trim());
        r.setImageUrl(req.imageUrl() == null || req.imageUrl().isBlank() ? null : req.imageUrl().trim());
        r.setPrepTimeMinutes(req.prepTimeMinutes());
        r.setCookTimeMinutes(req.cookTimeMinutes());
        r.setServings(req.servings());
        r.setIngredients(normalizedIngredients);
        r.setSteps(new ArrayList<>(req.steps().stream().map(String::trim).toList()));

        String cat = req.category() == null ? r.getCategory() : req.category().trim().toUpperCase(Locale.ROOT);
        r.setCategory(VALID_CATEGORIES.contains(cat) ? cat : r.getCategory());

        String budget = req.budgetLevel() == null ? r.getBudgetLevel() : req.budgetLevel().trim().toUpperCase(Locale.ROOT);
        r.setBudgetLevel(VALID_BUDGETS.contains(budget) ? budget : r.getBudgetLevel());
        r.setEstimatedCostTnd(resolveCostTnd(req.estimatedCostTnd(), r.getBudgetLevel()));

        if (req.funNote() != null && !req.funNote().isBlank()) {
            r.setFunNote(req.funNote().trim());
        }
        r.setDietaryTags(req.dietaryTags() == null ? null : req.dietaryTags().trim());
        r.setAllergens(req.allergens() == null ? null : req.allergens().trim().toLowerCase(Locale.ROOT));

        return toDetail(repo.save(r));
    }

    @Transactional(readOnly = true)
    public List<RecipeSummaryDto> myRecipes(Authentication auth) {
        User current = currentUser(auth);
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        List<Recipe> mine = repo.findByAuthorIdOrderByIdDesc(current.getId());
        return toSummaries(mine);
    }

    private double rankingScore(RecipeSummaryDto r, double globalAvg) {
        if (r.ratingCount() <= 0) return -1.0;
        return (r.averageRating() * r.ratingCount() + globalAvg * 3.0) / (r.ratingCount() + 3.0);
    }

    private double resolveCostTnd(Double requested, String budgetLevel) {
        if (requested != null && requested >= 0) {
            return Math.round(requested * 10.0) / 10.0;
        }
        return switch (budgetLevel == null ? "MODERATE" : budgetLevel.toUpperCase(Locale.ROOT)) {
            case "BUDGET" -> 8.0;
            case "GOURMET" -> 32.0;
            default -> 16.0;
        };
    }

    private boolean matchesQuery(Recipe r, String query) {
        if (query.isEmpty()) return true;
        if (r.getTitle() != null && r.getTitle().toLowerCase(Locale.ROOT).contains(query)) return true;
        if (r.getDescription() != null && r.getDescription().toLowerCase(Locale.ROOT).contains(query)) return true;
        if (r.getCategory() != null && r.getCategory().toLowerCase(Locale.ROOT).contains(query)) return true;
        return r.getIngredients().stream()
                .anyMatch(i -> i.getName() != null && i.getName().toLowerCase(Locale.ROOT).contains(query));
    }

    private void ensureUniqueTitleForAuthor(String title, User author, Long excludeRecipeId) {
        String norm = normalizeTitleKey(title);
        List<Recipe> existing = author == null
                ? repo.findAll().stream().filter(r -> r.getAuthor() == null).toList()
                : repo.findByAuthorIdOrderByIdDesc(author.getId());
        boolean duplicate = existing.stream()
                .filter(r -> excludeRecipeId == null || !r.getId().equals(excludeRecipeId))
                .anyMatch(r -> normalizeTitleKey(r.getTitle()).equals(norm));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have a recipe titled \"" + title + "\".");
        }
    }

    public static String normalizeTitleKey(String title) {
        if (title == null) return "";
        String decomposed = java.text.Normalizer.normalize(title.trim(), java.text.Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private boolean matchesIngredients(Recipe r, List<String> required) {
        if (required.isEmpty()) return true;
        List<String> recipeIngs = r.getIngredients().stream()
                .map(i -> i.getName() == null ? "" : i.getName().toLowerCase(Locale.ROOT))
                .toList();
        return required.stream().allMatch(rawReq -> {
            var norm = normalizer.normalize(rawReq);
            String req = (norm.valid() ? norm.normalized() : rawReq).toLowerCase(Locale.ROOT).trim();
            String reqSingular = req.endsWith("s") && req.length() > 3 ? req.substring(0, req.length() - 1) : req;
            return recipeIngs.stream().anyMatch(ing -> {
                String ingSingular = ing.endsWith("s") && ing.length() > 3 ? ing.substring(0, ing.length() - 1) : ing;
                return ing.contains(req) || req.contains(ing)
                        || ing.contains(reqSingular) || ingSingular.contains(reqSingular);
            });
        });
    }

    private boolean excludesAllergens(Recipe r, List<String> excluded) {
        if (excluded.isEmpty()) return true;
        String allergenStr = r.getAllergens() == null ? "" : r.getAllergens().toLowerCase(Locale.ROOT);
        String ingCombined = r.getIngredients().stream()
                .map(i -> i.getName() == null ? "" : i.getName().toLowerCase(Locale.ROOT))
                .reduce("", (a, b) -> a + " " + b);
        for (String ex : excluded) {
            if (allergenStr.contains(ex) || ingCombined.contains(ex)) {
                return false;
            }
            if (ex.equals("dairy") && (ingCombined.contains("milk") || ingCombined.contains("cheese")
                    || ingCombined.contains("butter") || ingCombined.contains("cream")
                    || ingCombined.contains("pecorino") || ingCombined.contains("parmesan"))) {
                return false;
            }
            if (ex.equals("gluten") && (ingCombined.contains("flour") || ingCombined.contains("spaghetti")
                    || ingCombined.contains("pasta") || ingCombined.contains("bread"))) {
                return false;
            }
            if (ex.equals("eggs") && ingCombined.contains("egg")) {
                return false;
            }
            if (ex.equals("nuts") && (ingCombined.contains("almond") || ingCombined.contains("walnut")
                    || ingCombined.contains("peanut") || ingCombined.contains("pistachio")
                    || ingCombined.contains("hazelnut") || ingCombined.contains("pine nut"))) {
                return false;
            }
        }
        return true;
    }

    private boolean containsToken(String csv, String targetUpper) {
        if (csv == null || csv.isBlank()) return false;
        return Arrays.stream(csv.split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .anyMatch(t -> t.equals(targetUpper));
    }

    private List<String> parseCsv(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private User currentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepo.findByEmail(auth.getName()).orElse(null);
    }

    private record RatingStat(double average, int count) {}

    private Map<Long, RatingStat> loadRatingStats() {
        Map<Long, RatingStat> map = new HashMap<>();
        for (Object[] row : reviewRepo.findAllRatingStats()) {
            Long id = (Long) row[0];
            double avg = row[1] == null ? 0.0 : Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0;
            int cnt = row[2] == null ? 0 : ((Number) row[2]).intValue();
            map.put(id, new RatingStat(avg, cnt));
        }
        return map;
    }

    private Map<Long, Long> loadFavoriteCounts() {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : favoriteRepo.findAllFavoriteCounts()) {
            map.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    private Map<Long, List<ReviewNoteDto>> loadTopNotes() {
        Map<Long, List<ReviewNoteDto>> map = new HashMap<>();
        for (RecipeReview rev : reviewRepo.findAllWithCommentOrderByCreatedAtDesc()) {
            Long recipeId = rev.getRecipe().getId();
            List<ReviewNoteDto> notes = map.computeIfAbsent(recipeId, k -> new ArrayList<>());
            if (notes.size() < 3) {
                notes.add(new ReviewNoteDto(rev.getReviewerName(), rev.getRating(), rev.getComment().trim()));
            }
        }
        return map;
    }

    public RecipeSummaryDto toSummary(Recipe r, Map<Long, RatingStat> ratingStats,
                                      Map<Long, Long> favCounts, Map<Long, List<ReviewNoteDto>> topNotesMap) {
        RatingStat stat = ratingStats.getOrDefault(r.getId(), new RatingStat(0.0, 0));
        long favCount = favCounts.getOrDefault(r.getId(), 0L);
        String note = (r.getFunNote() != null && !r.getFunNote().isBlank())
                ? r.getFunNote()
                : FALLBACK_NOTES.get(Math.abs(r.getTitle().hashCode()) % FALLBACK_NOTES.size());
        List<ReviewNoteDto> topNotes = topNotesMap.getOrDefault(r.getId(), List.of());

        return new RecipeSummaryDto(
                r.getId(),
                r.getTitle(),
                r.getDescription(),
                r.getImageUrl(),
                r.getPrepTimeMinutes(),
                r.getCookTimeMinutes(),
                r.getServings(),
                authorName(r),
                r.getCategory(),
                note,
                r.getBudgetLevel(),
                resolveCostTnd(r.getEstimatedCostTnd(), r.getBudgetLevel()),
                r.getDietaryTags(),
                r.getAllergens(),
                r.getIngredients().stream().map(i -> new IngredientDto(i.getName(), i.getQuantity())).toList(),
                stat.average(),
                stat.count(),
                favCount,
                topNotes
        );
    }

    public RecipeDetailDto toDetail(Recipe r) {
        List<Object[]> stats = reviewRepo.findRatingStatsByRecipeId(r.getId());
        double avg = 0.0;
        int cnt = 0;
        if (!stats.isEmpty() && stats.get(0)[0] != null) {
            avg = Math.round(((Number) stats.get(0)[0]).doubleValue() * 10.0) / 10.0;
            cnt = ((Number) stats.get(0)[1]).intValue();
        }
        long favCount = favoriteRepo.countByRecipeId(r.getId());
        String note = (r.getFunNote() != null && !r.getFunNote().isBlank())
                ? r.getFunNote()
                : FALLBACK_NOTES.get(Math.abs(r.getTitle().hashCode()) % FALLBACK_NOTES.size());

        return new RecipeDetailDto(
                r.getId(),
                r.getTitle(),
                r.getDescription(),
                r.getImageUrl(),
                r.getPrepTimeMinutes(),
                r.getCookTimeMinutes(),
                r.getServings(),
                r.getIngredients().stream().map(i -> new IngredientDto(i.getName(), i.getQuantity())).toList(),
                List.copyOf(r.getSteps()),
                authorName(r),
                r.getAuthor() == null ? null : r.getAuthor().getId(),
                r.getCreatedAt(),
                r.getCategory(),
                note,
                r.getBudgetLevel(),
                resolveCostTnd(r.getEstimatedCostTnd(), r.getBudgetLevel()),
                r.getDietaryTags(),
                r.getAllergens(),
                avg,
                cnt,
                favCount
        );
    }

    private String authorName(Recipe r) {
        return r.getAuthor() == null ? "Chef Kitchenette" : r.getAuthor().getName();
    }
}
