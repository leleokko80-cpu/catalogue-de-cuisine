package com.recipecatalog.backend.recipe;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Validates and auto-corrects culinary ingredient names (e.g. "egf" -> "Eggs", "tomto" -> "Tomato").
 */
@Component
public class IngredientNormalizer {

    private static final Pattern VALID_CHAR_PATTERN = Pattern.compile("^[\\p{L}0-9\\s'&.,()-]+$");
    private static final Pattern ONLY_DIGITS_OR_SYMBOLS = Pattern.compile("^[0-9\\s\\p{Punct}]+$");

    private static final Map<String, String> TYPO_MAP = Map.ofEntries(
            Map.entry("egf", "Eggs"),
            Map.entry("egy", "Eggs"),
            Map.entry("eg", "Egg"),
            Map.entry("egs", "Eggs"),
            Map.entry("egss", "Eggs"),
            Map.entry("egsg", "Eggs"),
            Map.entry("eegg", "Egg"),
            Map.entry("oeuf", "Eggs"),
            Map.entry("oeufs", "Eggs"),
            Map.entry("tomto", "Tomato"),
            Map.entry("tmato", "Tomato"),
            Map.entry("tomtao", "Tomato"),
            Map.entry("tomatoe", "Tomato"),
            Map.entry("tomatos", "Tomatoes"),
            Map.entry("tomate", "Tomato"),
            Map.entry("tomates", "Tomatoes"),
            Map.entry("chiken", "Chicken"),
            Map.entry("chikn", "Chicken"),
            Map.entry("chickn", "Chicken"),
            Map.entry("poulet", "Chicken"),
            Map.entry("courgette", "Zucchini"),
            Map.entry("courgettes", "Zucchini"),
            Map.entry("zuccini", "Zucchini"),
            Map.entry("aubergine", "Eggplant"),
            Map.entry("aubergines", "Eggplant"),
            Map.entry("pois chiches", "Chickpeas"),
            Map.entry("pois chiche", "Chickpeas"),
            Map.entry("chickpea", "Chickpeas"),
            Map.entry("thon", "Tuna"),
            Map.entry("agneau", "Lamb"),
            Map.entry("boeuf", "Beef"),
            Map.entry("crevette", "Shrimp"),
            Map.entry("crevettes", "Shrimp"),
            Map.entry("pomme de terre", "Potatoes"),
            Map.entry("pommes de terre", "Potatoes"),
            Map.entry("carotte", "Carrots"),
            Map.entry("carottes", "Carrots"),
            Map.entry("poivron", "Bell pepper"),
            Map.entry("poivrons", "Bell pepper"),
            Map.entry("persil", "Parsley"),
            Map.entry("coriandre", "Coriander"),
            Map.entry("menthe", "Mint"),
            Map.entry("amande", "Almonds"),
            Map.entry("amandes", "Almonds"),
            Map.entry("datte", "Dates"),
            Map.entry("dattes", "Dates"),
            Map.entry("semoule", "Semolina"),
            Map.entry("harisa", "Harissa"),
            Map.entry("bsisa", "Bsissa"),
            Map.entry("tabil", "Tabil"),
            Map.entry("merguez", "Merguez"),
            Map.entry("zgougou", "Zgougou"),
            Map.entry("flr", "Flour"),
            Map.entry("flor", "Flour"),
            Map.entry("flourr", "Flour"),
            Map.entry("farine", "Flour"),
            Map.entry("sugr", "Sugar"),
            Map.entry("suger", "Sugar"),
            Map.entry("sucre", "Sugar"),
            Map.entry("buter", "Butter"),
            Map.entry("buttr", "Butter"),
            Map.entry("beurre", "Butter"),
            Map.entry("mlk", "Milk"),
            Map.entry("mkl", "Milk"),
            Map.entry("lait", "Milk"),
            Map.entry("garlc", "Garlic"),
            Map.entry("garlik", "Garlic"),
            Map.entry("ail", "Garlic"),
            Map.entry("onin", "Onion"),
            Map.entry("onionn", "Onion"),
            Map.entry("oignon", "Onion"),
            Map.entry("chese", "Cheese"),
            Map.entry("cheeze", "Cheese"),
            Map.entry("fromage", "Cheese"),
            Map.entry("chocolte", "Chocolate"),
            Map.entry("choclate", "Chocolate"),
            Map.entry("chocolat", "Chocolate"),
            Map.entry("vanila", "Vanilla"),
            Map.entry("vanille", "Vanilla"),
            Map.entry("slat", "Salt"),
            Map.entry("saltt", "Salt"),
            Map.entry("sel", "Salt"),
            Map.entry("peper", "Pepper"),
            Map.entry("pepr", "Pepper"),
            Map.entry("poivre", "Pepper"),
            Map.entry("oliv oil", "Olive oil"),
            Map.entry("olive oill", "Olive oil"),
            Map.entry("huile dolive", "Olive oil"),
            Map.entry("spagetti", "Spaghetti"),
            Map.entry("spagheti", "Spaghetti"),
            Map.entry("psta", "Pasta"),
            Map.entry("ricee", "Rice"),
            Map.entry("riz", "Rice"),
            Map.entry("potto", "Potato"),
            Map.entry("potao", "Potato"),
            Map.entry("lemonn", "Lemon"),
            Map.entry("citron", "Lemon"),
            Map.entry("stwb", "Strawberry"),
            Map.entry("strawbery", "Strawberry"),
            Map.entry("fraise", "Strawberry"),
            Map.entry("bananna", "Banana"),
            Map.entry("banan", "Banana"),
            Map.entry("hony", "Honey"),
            Map.entry("miel", "Honey"),
            Map.entry("crem", "Cream"),
            Map.entry("creme", "Cream"),
            Map.entry("avocdo", "Avocado"),
            Map.entry("avocat", "Avocado"),
            Map.entry("salmn", "Salmon"),
            Map.entry("saumon", "Salmon")
    );

    private static final List<String> KNOWN_INGREDIENTS = List.of(
            "Eggs", "Egg", "Tomato", "Tomatoes", "Chicken", "Beef", "Pork", "Salmon", "Tuna", "Shrimp",
            "Flour", "Sugar", "Brown sugar", "Powdered sugar", "Butter", "Milk", "Heavy cream", "Cream",
            "Yogurt", "Cheese", "Mozzarella", "Parmesan", "Pecorino", "Cheddar", "Feta", "Ricotta",
            "Garlic", "Onion", "Red onion", "Shallot", "Ginger", "Olive oil", "Vegetable oil", "Sesame oil",
            "Salt", "Pepper", "Black pepper", "Paprika", "Cumin", "Harissa", "Cinnamon", "Nutmeg", "Oregano",
            "Basil", "Parsley", "Cilantro", "Thyme", "Rosemary", "Mint", "Vanilla", "Vanilla extract",
            "Chocolate", "Dark chocolate", "Cocoa powder", "Honey", "Maple syrup", "Baking powder",
            "Baking soda", "Yeast", "Spaghetti", "Pasta", "Rice", "Couscous", "Quinoa", "Bread",
            "Potato", "Potatoes", "Sweet potato", "Carrot", "Carrots", "Bell pepper", "Peppers",
            "Zucchini", "Eggplant", "Spinach", "Mushroom", "Mushrooms", "Broccoli", "Avocado",
            "Lemon", "Lime", "Orange", "Apple", "Banana", "Strawberry", "Strawberries", "Blueberries",
            "Almonds", "Walnuts", "Hazelnuts", "Pistachios", "Peanuts", "Coconut milk", "Soy sauce",
            "Vinegar", "Balsamic vinegar", "Mustard", "Ketchup", "Mayonnaise", "Guanciale", "Bacon",
            "Chickpeas", "Lentils", "Black beans", "Water", "Coffee", "Espresso", "Matcha", "Green tea"
    );

    public record NormalizationResult(String normalized, boolean valid, boolean corrected, String reason) {}

    public NormalizationResult normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return new NormalizationResult("", false, false, "Ingredient cannot be empty");
        }
        String cleaned = raw.trim().replaceAll("\\s+", " ");
        if (cleaned.length() < 2 || cleaned.length() > 80) {
            return new NormalizationResult(cleaned, false, false, "Ingredient must be 2 to 80 characters");
        }
        if (!VALID_CHAR_PATTERN.matcher(cleaned).matches() || ONLY_DIGITS_OR_SYMBOLS.matcher(cleaned).matches()) {
            return new NormalizationResult(cleaned, false, false, "Enter a real food ingredient name");
        }
        // Reject obvious keyboard mash like "asdfghj" or "zzzzz"
        String lettersOnly = cleaned.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}]", "");
        if (lettersOnly.length() < 2 || hasExcessiveRepeats(lettersOnly)) {
            return new NormalizationResult(cleaned, false, false, "Please enter a valid ingredient name");
        }

        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (TYPO_MAP.containsKey(lower)) {
            String fixed = TYPO_MAP.get(lower);
            return new NormalizationResult(fixed, true, !fixed.equalsIgnoreCase(cleaned), "Auto-corrected spelling");
        }

        // Exact case-insensitive match in known list -> canonical capitalization
        for (String known : KNOWN_INGREDIENTS) {
            if (known.equalsIgnoreCase(cleaned)) {
                return new NormalizationResult(known, true, !known.equals(cleaned), null);
            }
        }

        // Fuzzy Levenshtein match for short typos (distance <= 1 for <= 4 chars, distance <= 2 for longer)
        String bestMatch = null;
        int bestDist = Integer.MAX_VALUE;
        for (String known : KNOWN_INGREDIENTS) {
            int maxAllowed = known.length() <= 4 ? 1 : 2;
            int dist = levenshtein(lower, known.toLowerCase(Locale.ROOT));
            if (dist <= maxAllowed && dist < bestDist) {
                bestDist = dist;
                bestMatch = known;
            }
        }
        if (bestMatch != null) {
            return new NormalizationResult(bestMatch, true, true, "Auto-corrected spelling");
        }

        String capitalized = capitalize(cleaned);
        return new NormalizationResult(capitalized, true, false, null);
    }

    private boolean hasExcessiveRepeats(String s) {
        int streak = 1;
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) == s.charAt(i - 1)) {
                streak++;
                if (streak >= 4) return true;
            } else {
                streak = 1;
            }
        }
        return false;
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            System.arraycopy(curr, 0, prev, 0, curr.length);
        }
        return prev[b.length()];
    }
}
