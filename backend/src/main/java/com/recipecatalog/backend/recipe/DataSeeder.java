package com.recipecatalog.backend.recipe;

import com.recipecatalog.backend.favorite.FavoriteRepository;
import com.recipecatalog.backend.review.RecipeReview;
import com.recipecatalog.backend.review.RecipeReviewRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class DataSeeder implements CommandLineRunner {
    private final RecipeRepository repo;
    private final RecipeReviewRepository reviewRepo;
    private final FavoriteRepository favoriteRepo;

    private record SeedReview(String reviewerName, int rating, String comment) {}

    private record SeedRecipe(
            String title,
            String description,
            String imageUrl,
            int prep,
            int cook,
            int servings,
            String category,
            String funNote,
            String budgetLevel,
            double estimatedCostTnd,
            String dietaryTags,
            String allergens,
            List<Ingredient> ingredients,
            List<String> steps,
            List<SeedReview> reviews
    ) {}

    public DataSeeder(RecipeRepository repo,
                      RecipeReviewRepository reviewRepo,
                      FavoriteRepository favoriteRepo) {
        this.repo = repo;
        this.reviewRepo = reviewRepo;
        this.favoriteRepo = favoriteRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        cleanupSeededDuplicates();

        List<Recipe> existing = repo.findAll();
        Map<String, Recipe> byTitleLower = new HashMap<>();
        for (Recipe r : existing) {
            if (r.getTitle() != null) {
                byTitleLower.put(RecipeService.normalizeTitleKey(r.getTitle()), r);
            }
            backfillLegacy(r);
        }

        for (SeedRecipe seed : buildCatalog()) {
            String key = RecipeService.normalizeTitleKey(seed.title());
            Recipe recipe = byTitleLower.get(key);
            if (recipe == null) {
                recipe = repo.save(toEntity(seed));
                byTitleLower.put(key, recipe);
            } else if (recipe.getAuthor() == null) {
                boolean changed = false;
                if (recipe.getEstimatedCostTnd() == null) {
                    recipe.setEstimatedCostTnd(seed.estimatedCostTnd());
                    changed = true;
                }
                if (!Objects.equals(recipe.getImageUrl(), seed.imageUrl())) {
                    recipe.setImageUrl(seed.imageUrl());
                    changed = true;
                }
                if (changed) repo.save(recipe);
            }
            if (reviewRepo.countByRecipeId(recipe.getId()) == 0) {
                for (SeedReview rev : seed.reviews()) {
                    addReview(recipe, rev.reviewerName(), rev.rating(), rev.comment());
                }
            }
        }
    }

    /**
     * Removes duplicate SEEDED recipes (author == null) by normalized title or identical imageUrl.
     * Never deletes recipes created by real users.
     */
    private void cleanupSeededDuplicates() {
        List<Recipe> seeded = repo.findAll().stream()
                .filter(r -> r.getAuthor() == null)
                .sorted(Comparator.comparingLong(Recipe::getId))
                .toList();

        Set<String> seenTitles = new HashSet<>();
        Set<String> seenImages = new HashSet<>();
        List<Recipe> toDelete = new ArrayList<>();

        for (Recipe r : seeded) {
            String titleKey = RecipeService.normalizeTitleKey(r.getTitle());
            String imgKey = r.getImageUrl() == null ? "" : r.getImageUrl().trim();
            boolean dupTitle = !titleKey.isEmpty() && !seenTitles.add(titleKey);
            boolean dupImg = !imgKey.isEmpty() && !seenImages.add(imgKey);
            if (dupTitle || dupImg) {
                toDelete.add(r);
            }
        }

        for (Recipe dup : toDelete) {
            favoriteRepo.deleteByRecipeId(dup.getId());
            reviewRepo.deleteByRecipeId(dup.getId());
            repo.delete(dup);
        }
        if (!toDelete.isEmpty()) {
            repo.flush();
        }
    }

    private void backfillLegacy(Recipe r) {
        boolean dirty = false;
        if (r.getEstimatedCostTnd() == null) {
            r.setEstimatedCostTnd(switch (r.getBudgetLevel()) {
                case "BUDGET" -> 8.0;
                case "GOURMET" -> 32.0;
                default -> 16.0;
            });
            dirty = true;
        }
        if (dirty) repo.save(r);
    }

    private Recipe toEntity(SeedRecipe s) {
        Recipe r = new Recipe();
        r.setTitle(s.title());
        r.setDescription(s.description());
        r.setImageUrl(s.imageUrl());
        r.setPrepTimeMinutes(s.prep());
        r.setCookTimeMinutes(s.cook());
        r.setServings(s.servings());
        r.setCategory(s.category());
        r.setFunNote(s.funNote());
        r.setBudgetLevel(s.budgetLevel());
        r.setEstimatedCostTnd(s.estimatedCostTnd());
        r.setDietaryTags(s.dietaryTags());
        r.setAllergens(s.allergens());
        r.setIngredients(new ArrayList<>(s.ingredients()));
        r.setSteps(new ArrayList<>(s.steps()));
        return r;
    }

    private void addReview(Recipe r, String name, int rating, String comment) {
        RecipeReview rev = new RecipeReview();
        rev.setRecipe(r);
        rev.setReviewerName(name);
        rev.setRating(rating);
        rev.setComment(comment);
        reviewRepo.save(rev);
    }

    private static Ingredient ing(String name, String qty) {
        return new Ingredient(name, qty);
    }

    private static SeedReview rev(String name, int rating, String comment) {
        return new SeedReview(name, rating, comment);
    }

    private SeedRecipe make(
            String title, String desc, String img, int prep, int cook, int servings,
            String cat, String note, String budget, double costTnd, String tags, String allergens,
            List<Ingredient> ings, List<String> steps, List<SeedReview> revs) {
        return new SeedRecipe(title, desc, img, prep, cook, servings, cat, note, budget, costTnd, tags, allergens, ings, steps, revs);
    }

    private List<SeedRecipe> buildCatalog() {
        List<SeedRecipe> all = new ArrayList<>();
        all.addAll(entrees());
        all.addAll(plats());
        all.addAll(desserts());
        all.addAll(sweets());
        all.addAll(breakfast());
        all.addAll(aperitifs());
        all.addAll(boulange());
        all.addAll(boissons());
        return all;
    }

    private List<SeedRecipe> entrees() {
        return List.of(
            make("Burrata & Heirloom Tomato Bruschetta",
                "Crisp garlic-rubbed sourdough topped with creamy burrata, basil oil and sweet ripe tomatoes.",
                "https://images.unsplash.com/photo-1572695157366-5e585ab2b69f?auto=format&fit=crop&w=900&q=80",
                12, 5, 2, "ENTREES",
                "Rubbing raw garlic on warm toasted bread makes your kitchen smell like a Rome bistro.",
                "MODERATE", 18.0, "VEGETARIAN,DATE_NIGHT,QUICK,NUT_FREE", "dairy,gluten",
                List.of(ing("Bread","4 slices"), ing("Tomatoes","3"), ing("Cheese","150 g"), ing("Garlic","1 clove"), ing("Basil","1 handful"), ing("Olive oil","2 tbsp")),
                List.of("Slice rustic bread and toast until golden and crisp.", "Rub warm slices gently with raw garlic.", "Dice tomatoes and toss with olive oil, salt and pepper.", "Top toast with cheese, tomatoes and fresh basil."),
                List.of(rev("Hugo P.", 5, "Fresh, crispy and gorgeous on the table for guests."), rev("Lina M.", 4, "Added a drizzle of balsamic glaze and loved every bite."))),

            make("Creamy Garlic Potato Soup",
                "Budget-friendly velvety potato and leek soup finished with thyme croutons.",
                "https://images.unsplash.com/photo-1547592166-23ac45744acd?auto=format&fit=crop&w=900&q=80",
                10, 20, 4, "ENTREES",
                "Costs pennies per bowl and feels like a warm blanket on a rainy evening.",
                "BUDGET", 6.5, "VEGETARIAN,GLUTEN_FREE,NUT_FREE,COMFORT", "dairy",
                List.of(ing("Potatoes","600 g"), ing("Onion","1"), ing("Garlic","2 cloves"), ing("Butter","20 g"), ing("Cream","100 ml"), ing("Thyme","1 tsp")),
                List.of("Melt butter and sauté sliced onion and garlic until soft.", "Add diced potatoes, thyme, salt and water.", "Simmer 18 minutes until tender.", "Blend smooth, stir in warm cream and serve hot."),
                List.of(rev("Samira K.", 5, "So creamy without needing hours of simmering!"), rev("Tom B.", 4, "Great weekday starter with toasted bread."))),

            make("Tunisian Mechouia Salad",
                "Charred bell peppers, tomatoes and garlic pounded with caraway, olive oil and tuna.",
                "https://www.themealdb.com/images/media/meals/tbj1bs1764118062.jpg",
                15, 20, 4, "ENTREES",
                "The smoky char is the soul of Mechouia — don't be shy with the grill!",
                "BUDGET", 7.0, "GLUTEN_FREE,DAIRY_FREE,NUT_FREE,HEALTHY", "fish,eggs",
                List.of(ing("Bell pepper","4"), ing("Tomatoes","3"), ing("Garlic","3 cloves"), ing("Olive oil","4 tbsp"), ing("Tuna","120 g"), ing("Olives","12")),
                List.of("Grill peppers, tomatoes and garlic until skins blister.", "Peel skins and chop finely with caraway and salt.", "Drizzle generously with olive oil.", "Top with tuna, olives and boiled egg wedges."),
                List.of(rev("Yassine B.", 5, "Tastes just like summer in Tunis."), rev("Clara D.", 5, "Bright, smoky and super healthy!"))),

            make("Mediterranean Mezze Lablabi Bowl",
                "Warm cumin-scented chickpea soup over crusty torn bread with harissa and olive oil.",
                "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=900&q=80",
                10, 20, 2, "ENTREES",
                "Every Tunisian student's favorite cold-weather hug in a bowl.",
                "BUDGET", 5.0, "VEGAN,BUDGET,DAIRY_FREE,HEALTHY", "gluten",
                List.of(ing("Chickpeas","300 g"), ing("Garlic","3 cloves"), ing("Cumin","1.5 tsp"), ing("Harissa","1 tbsp"), ing("Bread","2 slices"), ing("Olive oil","3 tbsp")),
                List.of("Simmer chickpeas with garlic, cumin and salt for 20 minutes.", "Tear crusty bread into warm bowls.", "Ladle chickpeas and broth over the bread.", "Finish with harissa, lemon juice and olive oil."),
                List.of(rev("Mehdi T.", 5, "Pure comfort on a chilly evening."), rev("Nour H.", 4, "Added a poached egg on top for extra richness."))),

            make("Crispy Caprese Avocado Tartare",
                "Layered avocado, mozzarella and sweet cherry tomatoes with balsamic glaze.",
                "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=900&q=80",
                12, 0, 2, "ENTREES",
                "Zero cooking required, looks Michelin-starred in 10 minutes flat.",
                "MODERATE", 15.0, "VEGETARIAN,GLUTEN_FREE,QUICK,DATE_NIGHT,HEALTHY", "dairy",
                List.of(ing("Avocado","2"), ing("Tomatoes","150 g"), ing("Mozzarella","125 g"), ing("Basil","10 leaves"), ing("Olive oil","2 tbsp"), ing("Lemon","1")),
                List.of("Dice avocado and toss with lemon juice and salt.", "Dice cherry tomatoes and mozzarella.", "Layer avocado, mozzarella and tomatoes in a ring.", "Garnish with basil and olive oil."),
                List.of(rev("Emma R.", 5, "My go-to starter for date night!"))),

            make("Tunisian Salade Tounsia",
                "Crisp diced cucumber, tomatoes, onion and apple dressed in lemon, mint and olive oil.",
                "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=900&q=80",
                12, 0, 4, "ENTREES",
                "Tiny uniform dice makes every spoonful crunchy and refreshing.",
                "BUDGET", 4.5, "VEGAN,BUDGET,QUICK,HEALTHY,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Tomatoes","3"), ing("Cucumber","2"), ing("Onion","1"), ing("Lemon","1"), ing("Mint","1 tbsp"), ing("Olive oil","3 tbsp")),
                List.of("Finely dice tomatoes, cucumber and onion.", "Toss with dried mint, lemon juice and olive oil.", "Season with salt and pepper.", "Serve chilled alongside grilled dishes."),
                List.of(rev("Amina S.", 5, "Simple, crisp and pairs with everything."), rev("Karim Z.", 4, "Love the fresh mint touch."))),

            make("Creamy Roasted Tomato Soup",
                "Silky vine-ripened tomato soup simmered with basil, garlic and a swirl of cream.",
                "https://www.themealdb.com/images/media/meals/stpuws1511191310.jpg",
                10, 25, 4, "ENTREES",
                "Roasting the tomatoes first brings out deep natural sweetness.",
                "BUDGET", 7.0, "VEGETARIAN,BUDGET,HEALTHY,GLUTEN_FREE", "dairy",
                List.of(ing("Tomatoes","800 g"), ing("Garlic","3 cloves"), ing("Onion","1"), ing("Basil","1 handful"), ing("Cream","80 ml"), ing("Olive oil","2 tbsp")),
                List.of("Roast halved tomatoes, onion and garlic at 200°C for 20 minutes.", "Transfer to a pot with basil and broth.", "Blend smooth and stir in cream.", "Serve hot with crusty bread."),
                List.of(rev("Salma G.", 5, "Rich, bright and comforting."))),

            make("Tunisian Chorba Frik Lamb Soup",
                "Fragrant cracked green wheat soup with tender lamb, tomato, celery and coriander.",
                "https://www.themealdb.com/images/media/meals/p9tebp1764118792.jpg",
                15, 35, 4, "ENTREES",
                "The toasted wheat aroma fills the entire house during Ramadan.",
                "MODERATE", 16.0, "DAIRY_FREE,NUT_FREE,COMFORT", "gluten",
                List.of(ing("Lamb","250 g"), ing("Tomato paste","2 tbsp"), ing("Celery","2 stalks"), ing("Onion","1"), ing("Chickpeas","100 g"), ing("Coriander","1 handful")),
                List.of("Brown diced lamb with onion and celery in olive oil.", "Stir in tomato paste, paprika and water.", "Add frik wheat and chickpeas, simmer 30 minutes.", "Finish with fresh coriander and lemon."),
                List.of(rev("Youssef K.", 5, "Authentic flavor and deeply comforting."), rev("Ines M.", 5, "Best chorba recipe on the site!"))),

            make("Oyster Mushroom & Thyme Soup",
                "Earthy forest mushroom velouté simmered with shallots, garlic and fresh thyme.",
                "https://www.themealdb.com/images/media/meals/0r7y5n1782681336.jpg",
                12, 22, 4, "ENTREES",
                "Save a few crispy pan-seared mushroom slices to float on top.",
                "MODERATE", 13.0, "VEGETARIAN,GLUTEN_FREE,COMFORT", "dairy",
                List.of(ing("Mushrooms","400 g"), ing("Shallot","2"), ing("Garlic","2 cloves"), ing("Butter","25 g"), ing("Cream","100 ml"), ing("Thyme","1 tsp")),
                List.of("Sauté sliced mushrooms, shallots and garlic in butter.", "Add broth and thyme; simmer 18 minutes.", "Blend smooth and stir in cream.", "Garnish with seared mushrooms."),
                List.of(rev("Lucas V.", 5, "Deep earthy flavor!"))),

            make("Broccoli & Stilton Velouté",
                "Vibrant green broccoli soup enriched with crumbled Stilton and black pepper.",
                "https://www.themealdb.com/images/media/meals/tvvxpv1511191952.jpg",
                10, 18, 4, "ENTREES",
                "Don't overcook the broccoli so the soup stays bright emerald green.",
                "MODERATE", 12.0, "VEGETARIAN,GLUTEN_FREE,HEALTHY", "dairy",
                List.of(ing("Broccoli","500 g"), ing("Cheese","100 g"), ing("Onion","1"), ing("Potatoes","1"), ing("Butter","20 g"), ing("Pepper","1 tsp")),
                List.of("Sauté onion and diced potato in butter.", "Add broccoli florets and broth; simmer 10 minutes.", "Blend smooth and melt in cheese.", "Season with black pepper."),
                List.of(rev("Maya L.", 5, "Vibrant and savory!"), rev("Rami A.", 4, "Great starter for dinner parties."))),

            make("Beetroot Borscht Soup",
                "Ruby-red Eastern European beet, cabbage and carrot soup with dill and sour cream.",
                "https://www.themealdb.com/images/media/meals/zadvgb1699012544.jpg",
                15, 30, 4, "ENTREES",
                "A splash of vinegar at the end keeps the ruby color glowing.",
                "BUDGET", 6.5, "VEGETARIAN,BUDGET,HEALTHY,GLUTEN_FREE", "dairy",
                List.of(ing("Beetroot","3"), ing("Carrots","2"), ing("Potatoes","2"), ing("Onion","1"), ing("Dill","2 tbsp"), ing("Yogurt","100 g")),
                List.of("Sauté grated carrots, onion and beets.", "Add diced potatoes and broth; simmer 25 minutes.", "Finish with vinegar and fresh dill.", "Serve with a dollop of yogurt."),
                List.of(rev("Chloe N.", 5, "Stunning color and super healthy!"))),

            make("Chilled Ajo Blanco Almond Soup",
                "Andalusian cold white garlic-and-almond soup garnished with green grapes.",
                "https://www.themealdb.com/images/media/meals/5jdtie1763289302.jpg",
                15, 0, 4, "ENTREES",
                "Creamy and dairy-free — the Spanish cousin of gazpacho.",
                "MODERATE", 12.0, "VEGAN,HEALTHY,QUICK,DAIRY_FREE", "nuts,gluten",
                List.of(ing("Almonds","150 g"), ing("Bread","100 g"), ing("Garlic","1 clove"), ing("Olive oil","4 tbsp"), ing("Vinegar","1 tbsp"), ing("Salt","1 tsp")),
                List.of("Soak bread in cold water and squeeze.", "Blend almonds, garlic, bread, cold water and vinegar smooth.", "Drizzle in olive oil while blending.", "Chill well and serve cold."),
                List.of(rev("Sonia B.", 5, "Refreshing on hot summer afternoons."))),

            make("Black Bean & Cumin Soup",
                "Hearty smoky black bean soup topped with avocado, lime and fresh cilantro.",
                "https://www.themealdb.com/images/media/meals/x0mreq1784577446.jpg",
                10, 25, 4, "ENTREES",
                "Blending only half the pot keeps it thick while leaving whole beans for texture.",
                "BUDGET", 7.0, "VEGAN,BUDGET,HEALTHY,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Black beans","400 g"), ing("Onion","1"), ing("Garlic","3 cloves"), ing("Cumin","1.5 tsp"), ing("Avocado","1"), ing("Lemon","1")),
                List.of("Sauté onion, garlic and cumin in olive oil.", "Add black beans and broth; simmer 20 minutes.", "Blend half the soup for creaminess.", "Top with diced avocado and lime."),
                List.of(rev("Theo M.", 5, "Filling, smoky and budget-friendly."))),

            make("Aubergine & Couscous Warm Salad",
                "Roasted spiced eggplant cubes tossed with fluffy couscous, herbs and pomegranate.",
                "https://www.themealdb.com/images/media/meals/02s6gc1763799560.jpg",
                15, 25, 4, "ENTREES",
                "Roast the eggplant until deeply browned and creamy inside.",
                "BUDGET", 8.0, "VEGAN,BUDGET,HEALTHY,DAIRY_FREE", "gluten",
                List.of(ing("Eggplant","2"), ing("Couscous","200 g"), ing("Parsley","1 bunch"), ing("Lemon","1"), ing("Cumin","1 tsp"), ing("Olive oil","4 tbsp")),
                List.of("Cube eggplant, toss with oil and cumin, roast 25 minutes.", "Steam couscous in salted water.", "Toss couscous, roasted eggplant, parsley and lemon.", "Serve warm or room temperature."),
                List.of(rev("Leila F.", 5, "My favorite Mediterranean lunch!"))),

            make("Spiced Algerian Carrot Salad",
                "Tender steamed carrots glazed with garlic, cumin, caraway and lemon dressing.",
                "https://www.themealdb.com/images/media/meals/o2cd4r1764113576.jpg",
                10, 15, 4, "ENTREES",
                "Marinating warm carrots lets them soak up every drop of spiced olive oil.",
                "BUDGET", 4.5, "VEGAN,BUDGET,HEALTHY,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Carrots","500 g"), ing("Garlic","2 cloves"), ing("Cumin","1 tsp"), ing("Lemon","1"), ing("Parsley","2 tbsp"), ing("Olive oil","3 tbsp")),
                List.of("Slice carrots and steam until just tender.", "Sauté garlic and cumin gently in olive oil.", "Toss warm carrots with spiced oil and lemon.", "Scatter chopped parsley and chill."),
                List.of(rev("Hichem R.", 5, "Bright, zesty and tastes even better the next day.")))
        );
    }

    private List<SeedRecipe> plats() {
        return List.of(
            make("Spaghetti Carbonara",
                "Silky Roman pasta emulsified with eggs, Pecorino, black pepper and crispy pancetta.",
                "https://images.unsplash.com/photo-1612874742237-6526221588e3?auto=format&fit=crop&w=900&q=80",
                10, 15, 2, "PLATS",
                "Real Carbonara uses zero cream — just glossy egg magic and starchy pasta water!",
                "MODERATE", 18.0, "DATE_NIGHT,COMFORT", "eggs,dairy,gluten,pork",
                List.of(ing("Spaghetti","220 g"), ing("Eggs","3"), ing("Parmesan","60 g"), ing("Bacon","100 g"), ing("Garlic","1 clove"), ing("Pepper","1 tsp")),
                List.of("Boil spaghetti in salted water until al dente.", "Crisp bacon in a skillet, remove from heat.", "Whisk eggs with grated cheese and black pepper.", "Toss hot pasta with bacon and egg mix off-heat, adding pasta water until glossy."),
                List.of(rev("Marco V.", 5, "Silky sauce without a single drop of cream!"), rev("Julie S.", 4, "Super fast weeknight comfort."))),

            make("Tunisian Chakchouka",
                "Spiced tomato, bell pepper and onion ragout with runny poached eggs.",
                "https://images.unsplash.com/photo-1590412200988-a436970781fa?auto=format&fit=crop&w=900&q=80",
                10, 20, 3, "PLATS",
                "Grab crusty bread because leaving any sauce in the pan is basically illegal.",
                "BUDGET", 7.5, "VEGETARIAN,GLUTEN_FREE,DAIRY_FREE,NUT_FREE,BUDGET,HEALTHY", "eggs",
                List.of(ing("Tomatoes","5"), ing("Bell pepper","2"), ing("Onion","1"), ing("Garlic","3 cloves"), ing("Eggs","4"), ing("Olive oil","3 tbsp")),
                List.of("Sauté sliced onion and peppers in olive oil until soft.", "Add garlic, cumin, paprika and chopped tomatoes; simmer 12 minutes.", "Make wells and crack eggs into the sauce.", "Cover until whites set and yolks stay runny."),
                List.of(rev("Amine R.", 5, "Authentic, fiery and ready in 30 minutes."), rev("Sophie K.", 5, "Best brunch-for-dinner dish ever."))),

            make("Garlic Butter Herb Salmon",
                "Pan-seared salmon fillets basted in lemon, garlic butter and fresh dill.",
                "https://images.unsplash.com/photo-1467003909585-2f8a72700288?auto=format&fit=crop&w=900&q=80",
                8, 12, 2, "PLATS",
                "Crispy skin happens when you pat the salmon completely dry first!",
                "GOURMET", 34.0, "GLUTEN_FREE,NUT_FREE,QUICK,DATE_NIGHT,HEALTHY", "fish,dairy",
                List.of(ing("Salmon","2 fillets"), ing("Butter","30 g"), ing("Garlic","3 cloves"), ing("Lemon","1"), ing("Dill","1 tbsp"), ing("Olive oil","1 tbsp")),
                List.of("Pat salmon dry and season with salt and pepper.", "Sear skin-side down in olive oil for 4 minutes.", "Flip, add butter, crushed garlic and lemon slices.", "Baste with foaming butter 3 minutes and scatter dill."),
                List.of(rev("Nadia L.", 5, "Restaurant-quality dinner in 20 minutes!"), rev("David H.", 5, "Crispy skin and tender flaky center."))),

            make("Royal Tunisian Lamb Couscous",
                "Steamed semolina mounded with tender spiced lamb, chickpeas, carrots and pumpkin.",
                "https://images.unsplash.com/photo-1585937421612-70a008356fbe?auto=format&fit=crop&w=900&q=80",
                25, 50, 6, "PLATS",
                "Fluffing steamed couscous with olive oil by hand is pure kitchen therapy.",
                "GOURMET", 38.0, "DAIRY_FREE,NUT_FREE,COMFORT", "gluten",
                List.of(ing("Couscous","500 g"), ing("Lamb","600 g"), ing("Chickpeas","200 g"), ing("Carrots","3"), ing("Pumpkin","300 g"), ing("Tomato paste","2 tbsp")),
                List.of("Sear lamb with onion, turmeric, coriander and tomato paste.", "Cover with water, add chickpeas and simmer 30 minutes.", "Steam couscous over the broth while carrots and pumpkin cook.", "Fluff couscous with broth and top with meat and vegetables."),
                List.of(rev("Sami M.", 5, "Sunday family feast perfection!"), rev("Claire P.", 5, "The spices are balanced and deeply aromatic."))),

            make("Tunisian Ojja with Spicy Peppers",
                "Quick harissa-garlic tomato skillet simmered with merguez and soft eggs.",
                "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?auto=format&fit=crop&w=900&q=80",
                10, 15, 2, "PLATS",
                "Add caraway and a squeeze of lemon right at the end for peak street-food vibes.",
                "BUDGET", 9.5, "GLUTEN_FREE,DAIRY_FREE,NUT_FREE,QUICK,BUDGET", "eggs",
                List.of(ing("Tomatoes","4"), ing("Merguez","200 g"), ing("Bell pepper","2"), ing("Garlic","3 cloves"), ing("Harissa","1 tbsp"), ing("Eggs","3")),
                List.of("Brown sliced merguez in a pan, then add peppers and garlic.", "Stir in harissa and grated tomatoes, simmer 8 minutes.", "Crack eggs directly into the bubbling sauce.", "Cover 3 minutes and serve with warm bread."),
                List.of(rev("Oussama J.", 5, "Spicy, rich and ready in 25 minutes."))),

            make("Creamy Garlic Mushroom Chicken",
                "Golden chicken breasts simmered in a velvety garlic, thyme and mushroom cream sauce.",
                "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=900&q=80",
                10, 22, 3, "PLATS",
                "Spoon the extra mushroom sauce over rice or mashed potatoes.",
                "MODERATE", 19.0, "GLUTEN_FREE,NUT_FREE,DATE_NIGHT,COMFORT", "dairy",
                List.of(ing("Chicken","450 g"), ing("Mushrooms","250 g"), ing("Garlic","4 cloves"), ing("Cream","180 ml"), ing("Butter","20 g"), ing("Thyme","1 tsp")),
                List.of("Season chicken and sear in butter until golden.", "Sauté sliced mushrooms and garlic in the same skillet.", "Pour in cream and thyme, simmer 5 minutes.", "Return chicken to coat in sauce and serve hot."),
                List.of(rev("Elena W.", 5, "Even picky eaters ask for seconds!"))),

            make("Tunisian Ma9arouna Salsa",
                "Classic Tunisian spicy tomato, garlic and harissa pasta with tender chicken.",
                "https://www.themealdb.com/images/media/meals/5fu4ew1760524857.jpg",
                12, 25, 4, "PLATS",
                "Always toast the tomato paste in olive oil first to unlock deep sweetness.",
                "BUDGET", 9.0, "BUDGET,DAIRY_FREE,NUT_FREE,COMFORT", "gluten",
                List.of(ing("Pasta","400 g"), ing("Tomato paste","3 tbsp"), ing("Garlic","4 cloves"), ing("Harissa","1 tbsp"), ing("Chicken","300 g"), ing("Olive oil","4 tbsp")),
                List.of("Sauté chicken, garlic and bay leaf in olive oil.", "Fry tomato paste and harissa for 2 minutes, then add water.", "Simmer 18 minutes until thick and glossy.", "Toss with boiled pasta and serve piping hot."),
                List.of(rev("Bilel T.", 5, "The ultimate Tunisian comfort dish!"), rev("Sarra N.", 5, "Fiery, rich and feeds four for under 10 TND."))),

            make("Chilli Prawn Linguine",
                "Tender linguine tossed with juicy seared prawns, garlic, chili and cherry tomatoes.",
                "https://www.themealdb.com/images/media/meals/usywpp1511189717.jpg",
                10, 15, 2, "PLATS",
                "Cook prawns just 90 seconds per side so they stay plump and sweet.",
                "GOURMET", 28.0, "QUICK,DATE_NIGHT,DAIRY_FREE", "gluten,fish",
                List.of(ing("Pasta","220 g"), ing("Shrimp","250 g"), ing("Garlic","3 cloves"), ing("Tomatoes","150 g"), ing("Olive oil","3 tbsp"), ing("Parsley","2 tbsp")),
                List.of("Boil linguine until al dente.", "Sear shrimp with garlic and chili in olive oil.", "Toss in halved cherry tomatoes and pasta water.", "Fold in pasta and fresh parsley."),
                List.of(rev("Fares K.", 5, "Fresh, fiery and restaurant-worthy!"))),

            make("Tunisian Keleya Zaara Lamb",
                "Golden saffron-and-turmeric braised Tunisian lamb with garlic and parsley.",
                "https://www.themealdb.com/images/media/meals/8x09hy1560460923.jpg",
                15, 45, 4, "PLATS",
                "Slow-cooking lamb in turmeric and olive oil creates tender, aromatic bites.",
                "GOURMET", 32.0, "GLUTEN_FREE,DAIRY_FREE,COMFORT", "",
                List.of(ing("Lamb","500 g"), ing("Olive oil","60 ml"), ing("Garlic","4 cloves"), ing("Turmeric","1 tsp"), ing("Parsley","1 handful"), ing("Lemon","1")),
                List.of("Cut lamb into cubes and brown gently in olive oil.", "Add garlic, turmeric, salt and a splash of water.", "Simmer covered 40 minutes until meltingly tender.", "Scatter fresh parsley and lemon juice."),
                List.of(rev("Rym B.", 5, "Simple spices, unforgettable flavor."))),

            make("Classic Baked Beef Lasagne",
                "Layers of rich Bolognese ragù, silky béchamel and bubbling golden mozzarella.",
                "https://www.themealdb.com/images/media/meals/wtsvxx1511296896.jpg",
                25, 45, 6, "PLATS",
                "Let lasagne rest 15 minutes out of the oven so every slice holds cleanly.",
                "MODERATE", 24.0, "COMFORT", "dairy,gluten",
                List.of(ing("Beef","450 g"), ing("Pasta","250 g"), ing("Tomatoes","500 g"), ing("Milk","500 ml"), ing("Mozzarella","200 g"), ing("Butter","40 g")),
                List.of("Simmer beef with tomatoes, onion and garlic 25 minutes.", "Whisk butter, flour and milk into smooth béchamel.", "Layer pasta sheets, ragù, béchamel and mozzarella.", "Bake 40 minutes at 180°C until bubbling."),
                List.of(rev("Sophie M.", 5, "Ultimate Sunday comfort food!"))),

            make("Baingan Bharta Roasted Eggplant",
                "Smoky fire-roasted eggplant mashed with ginger, garlic, tomatoes and cumin.",
                "https://www.themealdb.com/images/media/meals/urtpqw1487341253.jpg",
                15, 30, 4, "PLATS",
                "Char the whole eggplant until collapsed for deep smoky flavor.",
                "BUDGET", 7.0, "VEGAN,BUDGET,HEALTHY,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Eggplant","2"), ing("Tomatoes","3"), ing("Onion","1"), ing("Garlic","3 cloves"), ing("Ginger","15 g"), ing("Cumin","1 tsp")),
                List.of("Roast whole eggplants until skins char and flesh is soft.", "Scoop out flesh and mash coarsely.", "Sauté onion, garlic, ginger, cumin and tomatoes.", "Fold in smoky eggplant and simmer 10 minutes."),
                List.of(rev("Zied A.", 5, "Rich, smoky and completely vegan!"))),

            make("Moroccan Spiced Barramundi Fish",
                "Crisp-skinned white fish fillet roasted with cumin, paprika, lemon and herbs.",
                "https://www.themealdb.com/images/media/meals/4o4wh11761848573.jpg",
                10, 15, 2, "PLATS",
                "Basting with spiced lemon oil keeps fish fillets moist and flaky.",
                "GOURMET", 29.0, "HEALTHY,GLUTEN_FREE,DAIRY_FREE,QUICK", "fish",
                List.of(ing("Fish","2 fillets"), ing("Cumin","1 tsp"), ing("Paprika","1 tsp"), ing("Lemon","1"), ing("Coriander","2 tbsp"), ing("Olive oil","3 tbsp")),
                List.of("Rub fish fillets with cumin, paprika, salt and olive oil.", "Sear or bake at 200°C for 12-14 minutes.", "Squeeze fresh lemon over the fillets.", "Scatter chopped coriander and serve."),
                List.of(rev("Mehdi S.", 5, "Flaky, fragrant and light."))),

            make("Margherita Neapolitan Pizza",
                "Crisp blistered crust topped with crushed San Marzano tomatoes, mozzarella and basil.",
                "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=900&q=80",
                20, 12, 2, "PLATS",
                "Bake on the hottest rack your oven allows for a bubbly char.",
                "MODERATE", 13.0, "VEGETARIAN,DATE_NIGHT", "gluten,dairy",
                List.of(ing("Flour","300 g"), ing("Tomatoes","200 g"), ing("Mozzarella","150 g"), ing("Basil","10 leaves"), ing("Olive oil","2 tbsp"), ing("Yeast","5 g")),
                List.of("Knead flour, water, yeast and salt; let rise.", "Stretch thin and spread crushed salted tomatoes.", "Top with torn mozzarella.", "Bake at 250°C for 10-12 minutes and top with basil."),
                List.of(rev("Luca R.", 5, "Simple ingredients, unbeatable crust."))),

            make("Cajun Spiced Fish Tacos",
                "Flaky Cajun-rubbed white fish in warm tortillas with crunchy slaw and lime.",
                "https://www.themealdb.com/images/media/meals/uvuyxu1503067369.jpg",
                12, 10, 3, "PLATS",
                "Warm the tortillas directly over a dry skillet for 20 seconds each.",
                "MODERATE", 17.0, "QUICK,DAIRY_FREE", "fish,gluten",
                List.of(ing("Fish","350 g"), ing("Paprika","1.5 tsp"), ing("Garlic","2 cloves"), ing("Lemon","1"), ing("Cabbage","150 g"), ing("Olive oil","2 tbsp")),
                List.of("Rub fish with paprika, garlic, cumin and salt.", "Sear fish 4 minutes per side until flaky.", "Toss shredded cabbage with lime juice.", "Assemble warm tacos with fish and slaw."),
                List.of(rev("Hana D.", 5, "Bright, zesty and ready in 20 minutes!"))),

            make("Smash Beef Burger & Caramelized Onions",
                "Lacy-edged smashed beef patties with melted cheddar and sweet balsamic onions.",
                "https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=900&q=80",
                10, 10, 2, "PLATS",
                "Press the patty firmly for 10 seconds right when it hits the smoking-hot pan.",
                "MODERATE", 18.0, "QUICK,COMFORT", "gluten,dairy",
                List.of(ing("Beef","300 g"), ing("Bread","2 buns"), ing("Cheese","2 slices"), ing("Onion","1"), ing("Butter","15 g"), ing("Tomatoes","1")),
                List.of("Caramelize sliced onion slowly in butter.", "Smash beef balls flat on a hot skillet, season and sear 2 minutes.", "Flip, top with cheese until melted.", "Serve on toasted buns with onions and tomato."),
                List.of(rev("Rayen B.", 5, "Better than any burger joint in town!"))),

            make("Cambodian Amok Fish Coconut Curry",
                "Silky turmeric and lemongrass coconut fish curry steamed until custard-soft.",
                "https://www.themealdb.com/images/media/meals/diuub11782687570.jpg",
                15, 20, 4, "PLATS",
                "Steaming keeps the fish remarkably tender in fragrant coconut sauce.",
                "GOURMET", 26.0, "GLUTEN_FREE,DAIRY_FREE,HEALTHY", "fish",
                List.of(ing("Fish","450 g"), ing("Coconut milk","300 ml"), ing("Garlic","3 cloves"), ing("Turmeric","1 tsp"), ing("Ginger","15 g"), ing("Rice","250 g")),
                List.of("Blend garlic, ginger, turmeric and coconut milk into curry sauce.", "Coat fish chunks gently in the sauce.", "Simmer or steam 15 minutes until fish flakes.", "Serve over steamed jasmine rice."),
                List.of(rev("Wafa G.", 5, "Aromatic, velvety and unique!")))
        );
    }

    private List<SeedRecipe> desserts() {
        return List.of(
            make("Velvety Chocolate Lava Cake",
                "Warm individual dark chocolate cakes with a molten fudgy center.",
                "https://images.unsplash.com/photo-1624353365286-3f8d62daad51?auto=format&fit=crop&w=900&q=80",
                12, 11, 2, "DESSERTS",
                "11 minutes is the sweet spot — edges firm, middle pure molten chocolate.",
                "MODERATE", 12.0, "VEGETARIAN,DATE_NIGHT,QUICK", "eggs,dairy,gluten",
                List.of(ing("Chocolate","120 g"), ing("Butter","80 g"), ing("Eggs","2"), ing("Sugar","50 g"), ing("Flour","35 g"), ing("Vanilla","1 tsp")),
                List.of("Melt chocolate and butter together.", "Whisk eggs, sugar and vanilla until pale.", "Fold in melted chocolate and flour.", "Bake in buttered ramekins at 200°C for 11 minutes."),
                List.of(rev("Camille F.", 5, "Pure lava magic on the first spoonful!"), rev("Adam Z.", 5, "Made this for our anniversary and nailed it."))),

            make("Classic Espresso Tiramisu",
                "Espresso-soaked ladyfingers layered with whipped mascarpone and cocoa.",
                "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?auto=format&fit=crop&w=900&q=80",
                25, 0, 6, "DESSERTS",
                "Resting overnight in the fridge turns this from great to unforgettable.",
                "MODERATE", 22.0, "VEGETARIAN,DATE_NIGHT", "eggs,dairy,gluten",
                List.of(ing("Mascarpone","250 g"), ing("Eggs","3"), ing("Sugar","80 g"), ing("Coffee","200 ml"), ing("Biscuits","200 g"), ing("Cocoa","2 tbsp")),
                List.of("Whisk egg yolks and sugar, fold into mascarpone.", "Fold in stiffly whipped egg whites.", "Dip biscuits quickly in cooled espresso and layer with cream.", "Chill 4 hours and dust with cocoa."),
                List.of(rev("Giulia M.", 5, "Light as a cloud and not overly sweet."))),

            make("Tunisian Assidat Zgougou",
                "Rich Aleppo pine-seed pudding topped with vanilla pastry cream and crunchy nuts.",
                "https://www.themealdb.com/images/media/meals/p277uc1764109195.jpg",
                20, 30, 6, "DESSERTS",
                "Traditional Mouled centerpiece — decorate with colorful pistachios and almonds.",
                "GOURMET", 35.0, "VEGETARIAN", "dairy,nuts,gluten",
                List.of(ing("Zgougou","250 g"), ing("Flour","150 g"), ing("Sugar","150 g"), ing("Milk","750 ml"), ing("Pistachio","50 g"), ing("Almonds","50 g")),
                List.of("Strain zgougou paste with water and whisk with flour and sugar.", "Cook stirring constantly until thick and glossy.", "Pour into bowls and top with vanilla milk cream.", "Decorate with pistachios and almonds."),
                List.of(rev("Ines B.", 5, "Silky, nutty and deeply nostalgic."))),

            make("Roasted Honey Fig & Yogurt Tart",
                "Crisp almond tart filled with honeyed Greek yogurt and caramelized figs.",
                "https://images.unsplash.com/photo-1519915028121-7d3463d20b13?auto=format&fit=crop&w=900&q=80",
                15, 25, 6, "DESSERTS",
                "Honey and thyme on roasted figs makes the whole kitchen smell heavenly.",
                "MODERATE", 19.0, "VEGETARIAN,DATE_NIGHT", "dairy,gluten,nuts",
                List.of(ing("Flour","200 g"), ing("Butter","100 g"), ing("Yogurt","250 g"), ing("Honey","3 tbsp"), ing("Figs","6"), ing("Almonds","40 g")),
                List.of("Press pastry dough into a tart pan and blind bake.", "Spread honeyed Greek yogurt over cooled crust.", "Top with halved figs and drizzle with honey.", "Scatter toasted almonds and serve."),
                List.of(rev("Clara D.", 5, "Light, pretty and not too sweet."))),

            make("Caramelized Banana Crêpes",
                "Paper-thin French crêpes folded around warm brown-sugar bananas and chocolate.",
                "https://images.unsplash.com/photo-1519676867240-f03562e64548?auto=format&fit=crop&w=900&q=80",
                10, 15, 4, "DESSERTS",
                "The first crêpe is always the chef's snack — embrace the tradition!",
                "BUDGET", 7.0, "VEGETARIAN,BUDGET,QUICK", "eggs,dairy,gluten",
                List.of(ing("Flour","150 g"), ing("Eggs","2"), ing("Milk","300 ml"), ing("Banana","2"), ing("Butter","30 g"), ing("Chocolate","60 g")),
                List.of("Whisk flour, eggs, milk and melted butter into smooth batter.", "Cook thin crêpes in a lightly buttered pan.", "Caramelize sliced bananas in butter and sugar.", "Fill crêpes with bananas and melted chocolate."),
                List.of(rev("Leo P.", 5, "Sunday evening perfection."))),

            make("Apple Frangipane Tart",
                "Flaky pastry shell filled with almond frangipane cream and fanned apple slices.",
                "https://www.themealdb.com/images/media/meals/wxywrq1468235067.jpg",
                20, 35, 8, "DESSERTS",
                "Warm apricot glaze brushed on top gives a French patisserie shine.",
                "MODERATE", 17.0, "VEGETARIAN,DATE_NIGHT", "eggs,dairy,gluten,nuts",
                List.of(ing("Flour","200 g"), ing("Almonds","100 g"), ing("Butter","120 g"), ing("Sugar","100 g"), ing("Eggs","2"), ing("Apple","3")),
                List.of("Line a tart pan with shortcrust pastry.", "Beat butter, sugar, eggs and ground almonds into frangipane.", "Spread frangipane and arrange sliced apples on top.", "Bake 35 minutes at 180°C."),
                List.of(rev("Sophie L.", 5, "Buttery almond filling and tender apples!"))),

            make("Apple & Blackberry Crumble",
                "Bubbling orchard apples and tart blackberries under a golden oat-butter crust.",
                "https://www.themealdb.com/images/media/meals/xvsurr1511719182.jpg",
                15, 30, 6, "DESSERTS",
                "Serve warm with a scoop of vanilla ice cream melting into the juices.",
                "BUDGET", 9.5, "VEGETARIAN,BUDGET,COMFORT", "dairy,gluten",
                List.of(ing("Apple","4"), ing("Blackberries","150 g"), ing("Flour","120 g"), ing("Oats","80 g"), ing("Butter","90 g"), ing("Sugar","80 g")),
                List.of("Toss apples and blackberries with sugar in a baking dish.", "Rub butter into flour, oats and sugar.", "Scatter crumble over fruit.", "Bake 30 minutes at 190°C until golden."),
                List.of(rev("Nour A.", 5, "Crisp topping and jammy berry filling."))),

            make("Rustic Spiced Apple Cake",
                "Tender cinnamon-scented yogurt cake studded with chunks of sweet baking apples.",
                "https://www.themealdb.com/images/media/meals/c0gmo31766594751.jpg",
                15, 35, 8, "DESSERTS",
                "Dust with powdered sugar just before slicing for effortless café style.",
                "BUDGET", 8.5, "VEGETARIAN,BUDGET,COMFORT", "eggs,dairy,gluten",
                List.of(ing("Apple","3"), ing("Flour","220 g"), ing("Eggs","3"), ing("Sugar","140 g"), ing("Yogurt","150 g"), ing("Cinnamon","1.5 tsp")),
                List.of("Whisk eggs, sugar and yogurt until frothy.", "Fold in flour, baking powder and cinnamon.", "Stir in diced apples.", "Bake 35 minutes at 180°C."),
                List.of(rev("Meriem H.", 5, "Moist, fragrant and super easy!"))),

            make("Tunisian Orange Blossom Almond Cake",
                "Moist gluten-free almond flour cake soaked in fragrant orange blossom syrup.",
                "https://images.unsplash.com/photo-1551024506-0bccd828d307?auto=format&fit=crop&w=900&q=80",
                15, 35, 8, "DESSERTS",
                "Stays moist for four days thanks to ground almonds and citrus syrup.",
                "MODERATE", 18.0, "VEGETARIAN,GLUTEN_FREE,DAIRY_FREE", "eggs,nuts",
                List.of(ing("Almonds","250 g"), ing("Eggs","4"), ing("Sugar","150 g"), ing("Orange","1"), ing("Olive oil","60 ml"), ing("Baking powder","1 tsp")),
                List.of("Whisk eggs and sugar until thick and pale.", "Fold in ground almonds, orange zest and olive oil.", "Bake at 175°C for 35 minutes.", "Brush warm cake with orange blossom syrup."),
                List.of(rev("Salma K.", 5, "Heavenly citrus aroma!"))),

            make("Classic Deep-Dish Apple Pie",
                "Flaky double-crust pie overflowing with cinnamon-nutmeg glazed apples.",
                "https://www.themealdb.com/images/media/meals/stnxzp1784835840.jpg",
                25, 45, 8, "DESSERTS",
                "Chill the rolled pie dough 20 minutes before baking for maximum flakiness.",
                "MODERATE", 15.0, "VEGETARIAN,COMFORT", "dairy,gluten",
                List.of(ing("Apple","6"), ing("Flour","300 g"), ing("Butter","160 g"), ing("Sugar","130 g"), ing("Cinnamon","1.5 tsp"), ing("Lemon","1")),
                List.of("Make cold butter shortcrust pastry and chill.", "Toss sliced apples with sugar, cinnamon and lemon.", "Fill bottom crust, cover with top crust and vent.", "Bake 45 minutes at 190°C."),
                List.of(rev("Lucas D.", 5, "Golden flaky crust and juicy apples."))),

            make("Walnut Stuffed Figs in Syrup",
                "Plump dried figs stuffed with toasted walnuts and poached in spiced honey syrup.",
                "https://www.themealdb.com/images/media/meals/dokbyt1779645030.jpg",
                15, 15, 6, "DESSERTS",
                "A jewel-like Mediterranean dessert that pairs beautifully with Greek yogurt.",
                "MODERATE", 16.0, "VEGAN,GLUTEN_FREE,DAIRY_FREE", "nuts",
                List.of(ing("Figs","12"), ing("Walnuts","100 g"), ing("Honey","120 ml"), ing("Cinnamon","1 stick"), ing("Lemon","1")),
                List.of("Slit figs gently and stuff with walnut halves.", "Simmer honey, water, cinnamon and lemon 5 minutes.", "Poach stuffed figs gently 10 minutes.", "Cool in syrup and serve."),
                List.of(rev("Yasmine R.", 5, "Rich, chewy and naturally sweet."))),

            make("Indonesian Apam Balik Peanut Pancake",
                "Thick crispy-edged sweet turnover pancake filled with crushed peanuts and sweet corn.",
                "https://www.themealdb.com/images/media/meals/adxcbq1619787919.jpg",
                10, 10, 4, "DESSERTS",
                "Cook covered on low heat so the honeycomb crumb sets light and airy.",
                "BUDGET", 6.5, "VEGETARIAN,BUDGET,QUICK", "eggs,dairy,gluten,nuts",
                List.of(ing("Flour","200 g"), ing("Eggs","1"), ing("Milk","220 ml"), ing("Sugar","60 g"), ing("Peanuts","80 g"), ing("Butter","20 g")),
                List.of("Whisk flour, sugar, egg, baking soda and milk.", "Pour into a buttered skillet and cook until bubbly.", "Scatter crushed peanuts and sugar, fold in half.", "Slice warm and serve."),
                List.of(rev("Omar T.", 5, "Crunchy peanuts and fluffy pancake!"))),

            make("Silky Berry Cream Parfait",
                "Chilled whipped cream and yogurt layered with macerated strawberries and crunch.",
                "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=900&q=80",
                10, 0, 2, "DESSERTS",
                "Macerate strawberries with a spoon of sugar 15 minutes to release sweet syrup.",
                "BUDGET", 8.0, "VEGETARIAN,QUICK,BUDGET,GLUTEN_FREE", "dairy",
                List.of(ing("Cream","200 ml"), ing("Yogurt","150 g"), ing("Strawberry","200 g"), ing("Sugar","40 g"), ing("Vanilla","1 tsp")),
                List.of("Toss sliced strawberries with half the sugar.", "Whip cream with vanilla and remaining sugar; fold in yogurt.", "Layer berries and cream in glasses.", "Chill 20 minutes and serve."),
                List.of(rev("Amel B.", 5, "Light, creamy and refreshing!"))),

            make("Danish Æbleskiver Pancake Puffs",
                "Warm spherical pancake puffs dusted with powdered sugar and raspberry jam.",
                "https://www.themealdb.com/images/media/meals/wkhg581762773124.jpg",
                10, 12, 4, "DESSERTS",
                "Flip gently with a skewer as soon as the bottom crust sets golden.",
                "BUDGET", 7.0, "VEGETARIAN,BUDGET", "eggs,dairy,gluten",
                List.of(ing("Flour","200 g"), ing("Milk","250 ml"), ing("Eggs","2"), ing("Butter","40 g"), ing("Sugar","30 g"), ing("Vanilla","1 tsp")),
                List.of("Whisk flour, sugar, baking powder, milk and egg yolks.", "Fold in whipped egg whites.", "Cook in buttered round wells turning to brown all sides.", "Dust with powdered sugar."),
                List.of(rev("Nadia K.", 5, "Fluffy little clouds!"))),

            make("Baked Cream Cheese Tart",
                "Velvety baked cheesecake tart with a crisp butter-biscuit crust.",
                "https://www.themealdb.com/images/media/meals/wurrux1468416624.jpg",
                15, 35, 8, "DESSERTS",
                "Let it cool inside the turned-off oven for 30 minutes to prevent cracks.",
                "MODERATE", 19.0, "VEGETARIAN,DATE_NIGHT", "eggs,dairy,gluten",
                List.of(ing("Cream cheese","400 g"), ing("Sugar","120 g"), ing("Eggs","3"), ing("Biscuits","180 g"), ing("Butter","70 g"), ing("Vanilla","1 tsp")),
                List.of("Press crushed biscuits and melted butter into a tart pan.", "Beat cream cheese, sugar, eggs and vanilla smooth.", "Pour over crust and bake 35 minutes at 165°C.", "Chill 3 hours before slicing."),
                List.of(rev("Julie M.", 5, "Creamy, rich and effortless!")))
        );
    }

    private List<SeedRecipe> sweets() {
        return List.of(
            make("Pistachio Rose Madeleines",
                "Shell-shaped French butter cakes scented with rosewater and crushed pistachios.",
                "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?auto=format&fit=crop&w=900&q=80",
                15, 11, 12, "SWEETS",
                "Chill the batter 30 minutes to get the signature tall madeleine bump!",
                "MODERATE", 15.0, "VEGETARIAN,DATE_NIGHT", "eggs,dairy,gluten,nuts",
                List.of(ing("Flour","120 g"), ing("Butter","100 g"), ing("Eggs","2"), ing("Sugar","90 g"), ing("Pistachio","40 g"), ing("Honey","1 tbsp")),
                List.of("Whisk eggs, sugar and honey until thick.", "Fold in flour, melted butter and crushed pistachios.", "Chill batter 30 minutes.", "Pipe into madeleine pans and bake at 200°C for 10 minutes."),
                List.of(rev("Nour C.", 5, "Delicate, buttery and gorgeous."))),

            make("Tunisian Date Makroudh",
                "Golden semolina diamonds stuffed with spiced date paste and bathed in honey.",
                "https://images.unsplash.com/photo-1517433670267-08bbd4be890f?auto=format&fit=crop&w=900&q=80",
                30, 20, 16, "SWEETS",
                "Warm honey + a hint of orange blossom gives that unmistakable festive aroma.",
                "BUDGET", 9.0, "VEGAN,BUDGET,DAIRY_FREE", "gluten",
                List.of(ing("Semolina","500 g"), ing("Dates","250 g"), ing("Olive oil","120 ml"), ing("Honey","150 ml"), ing("Cinnamon","1 tsp"), ing("Orange","1")),
                List.of("Rub semolina with warm oil and rest 20 minutes.", "Shape dough logs around spiced date paste.", "Slice into diamonds and bake or fry until golden.", "Dip warm makroudh in warm honey."),
                List.of(rev("Kenza M.", 5, "Crumbly semolina and sweet date filling!"))),

            make("Crispy Almond Samsa Triangles",
                "Honey-glazed phyllo triangles packed with toasted almonds and sesame.",
                "https://www.themealdb.com/images/media/meals/y4t9zg1777628842.jpg",
                25, 18, 12, "SWEETS",
                "Toast almonds lightly before grinding for deeper nutty flavor.",
                "MODERATE", 18.0, "VEGETARIAN", "gluten,nuts,sesame",
                List.of(ing("Almonds","250 g"), ing("Sugar","80 g"), ing("Butter","60 g"), ing("Honey","150 ml"), ing("Sesame","2 tbsp"), ing("Phyllo","1 pack")),
                List.of("Mix ground toasted almonds with sugar and orange blossom.", "Fold phyllo strips into triangles around filling.", "Bake until crisp and golden.", "Dip in warm honey and scatter sesame seeds."),
                List.of(rev("Yassine L.", 5, "Ultra-crunchy and irresistible with mint tea."))),

            make("Chewy Dark Chocolate Cookies",
                "Bakery-style brown-butter cookies with gooey dark chocolate chunks and sea salt.",
                "https://images.unsplash.com/photo-1499636136210-6f4ee915583e?auto=format&fit=crop&w=900&q=80",
                15, 10, 12, "SWEETS",
                "Pull them out while centers still look soft — they set as they cool.",
                "BUDGET", 8.5, "VEGETARIAN,BUDGET,QUICK", "eggs,dairy,gluten",
                List.of(ing("Flour","220 g"), ing("Butter","115 g"), ing("Sugar","150 g"), ing("Eggs","1"), ing("Chocolate","180 g"), ing("Vanilla","1 tsp")),
                List.of("Cream browned butter with sugar, egg and vanilla.", "Fold in flour, baking soda and chocolate chunks.", "Scoop onto baking sheet.", "Bake 10 minutes at 180°C and sprinkle flaky salt."),
                List.of(rev("Tom R.", 5, "Chewy centers, crisp edges — perfection!"))),

            make("Tunisian Bambalouni Sugar Rings",
                "Pillowy fried Tunisian dough rings rolled in granulated sugar while hot.",
                "https://images.unsplash.com/photo-1551024601-bec78aea704b?auto=format&fit=crop&w=900&q=80",
                20, 10, 8, "SWEETS",
                "Best eaten warm right by the beach with a squeeze of lemon!",
                "BUDGET", 4.0, "VEGAN,BUDGET,DAIRY_FREE", "gluten",
                List.of(ing("Flour","350 g"), ing("Yeast","7 g"), ing("Sugar","80 g"), ing("Salt","1 pinch"), ing("Olive oil","300 ml")),
                List.of("Knead flour, yeast, warm water and salt into soft dough; rise 1 hour.", "Stretch dough into rings with wet hands.", "Fry until puffed and golden.", "Roll immediately in sugar."),
                List.of(rev("Sami Z.", 5, "Just like Sidi Bou Said!"))),

            make("Dulce de Leche Alfajores Cookies",
                "Melt-in-your-mouth cornstarch shortbread sandwiches filled with caramel and coconut.",
                "https://www.themealdb.com/images/media/meals/a4kgf21763075288.jpg",
                20, 12, 12, "SWEETS",
                "Roll the caramel edges in shredded coconut for a classic patisserie look.",
                "MODERATE", 13.0, "VEGETARIAN", "eggs,dairy,gluten",
                List.of(ing("Flour","150 g"), ing("Cornstarch","150 g"), ing("Butter","120 g"), ing("Sugar","80 g"), ing("Caramel","150 g"), ing("Coconut","50 g")),
                List.of("Cream butter and sugar; mix in flour and cornstarch.", "Roll out, cut rounds and bake 11 minutes at 175°C.", "Sandwich cooled cookies with caramel.", "Roll edges in shredded coconut."),
                List.of(rev("Amina B.", 5, "Dissolves right on your tongue!"))),

            make("Golden Anzac Oat & Coconut Biscuits",
                "Chewy-crisp toasted oat and golden syrup cookies with coconut crunch.",
                "https://www.themealdb.com/images/media/meals/q47rkb1762324620.jpg",
                10, 12, 14, "SWEETS",
                "They stay crisp in a cookie tin for days.",
                "BUDGET", 6.5, "VEGETARIAN,BUDGET,QUICK", "dairy,gluten",
                List.of(ing("Oats","120 g"), ing("Flour","120 g"), ing("Coconut","80 g"), ing("Butter","100 g"), ing("Honey","2 tbsp"), ing("Sugar","90 g")),
                List.of("Mix oats, flour, coconut and sugar.", "Melt butter and honey together with baking soda.", "Stir into dry mix and scoop onto tray.", "Bake 12 minutes at 175°C."),
                List.of(rev("Lina S.", 5, "Simple, chewy and nutty!"))),

            make("Caramel Apple Pie Pops",
                "Bite-sized cinnamon apple pastry lollipops glazed with vanilla icing.",
                "https://www.themealdb.com/images/media/meals/ax643t1784731109.jpg",
                20, 18, 10, "SWEETS",
                "Fun handheld treats for kids and birthday dessert tables.",
                "BUDGET", 8.5, "VEGETARIAN,BUDGET", "dairy,gluten,eggs",
                List.of(ing("Puff pastry","1 sheet"), ing("Apple","2"), ing("Sugar","50 g"), ing("Cinnamon","1 tsp"), ing("Eggs","1")),
                List.of("Simmer diced apple with sugar and cinnamon until soft.", "Cut pastry circles, insert sticks and spoon filling.", "Seal with top circles, brush with egg.", "Bake 18 minutes at 190°C."),
                List.of(rev("Chloe M.", 5, "Cute, flaky and fun to eat!"))),

            make("East African Spiced Mandazi Puffs",
                "Cardamom and coconut-milk pillowy fried dough triangles.",
                "https://www.themealdb.com/images/media/meals/thazgm1555350962.jpg",
                15, 12, 12, "SWEETS",
                "Coconut milk and cardamom make these puffs irresistibly fragrant.",
                "BUDGET", 5.5, "VEGAN,BUDGET,DAIRY_FREE", "gluten",
                List.of(ing("Flour","350 g"), ing("Coconut milk","180 ml"), ing("Sugar","60 g"), ing("Yeast","7 g"), ing("Cardamom","1 tsp")),
                List.of("Knead flour, sugar, cardamom, yeast and coconut milk.", "Rest 30 minutes, roll and cut triangles.", "Fry in hot oil until puffed and golden.", "Serve warm with tea."),
                List.of(rev("Hugo D.", 5, "Fluffy inside with a lovely cardamom aroma!"))),

            make("Colombian Sweet Almojábanas Cheese Puffs",
                "Warm tender cornmeal and fresh cheese baked puffs.",
                "https://www.themealdb.com/images/media/meals/ymk7gt1783803106.jpg",
                10, 18, 10, "SWEETS",
                "Best eaten warm right out of the oven with coffee.",
                "BUDGET", 8.0, "VEGETARIAN,BUDGET,GLUTEN_FREE", "dairy,eggs",
                List.of(ing("Cornmeal","180 g"), ing("Cheese","200 g"), ing("Eggs","1"), ing("Milk","60 ml"), ing("Sugar","2 tbsp"), ing("Butter","25 g")),
                List.of("Mix cornmeal, grated cheese, sugar and baking powder.", "Stir in egg, melted butter and milk into soft dough.", "Roll into 10 balls.", "Bake 18 minutes at 200°C."),
                List.of(rev("Emma V.", 5, "Cheesy, lightly sweet and gluten-free!"))),

            make("Tunisian Yo-Yo Honey Donuts",
                "Citrus-scented cake donuts fried golden and glazed in warm orange syrup.",
                "https://www.themealdb.com/images/media/meals/oaqz9f1766593912.jpg",
                20, 12, 12, "SWEETS",
                "A sprinkle of toasted sesame seeds on top makes them shine.",
                "BUDGET", 6.0, "VEGETARIAN,BUDGET", "eggs,gluten,sesame",
                List.of(ing("Flour","350 g"), ing("Eggs","2"), ing("Sugar","100 g"), ing("Orange","1"), ing("Honey","120 ml"), ing("Sesame","2 tbsp")),
                List.of("Whisk eggs, sugar, oil and orange zest.", "Fold in flour and baking powder into soft dough.", "Cut rings and fry until golden.", "Dip in warm honey syrup and top with sesame."),
                List.of(rev("Marc L.", 5, "Soft inside and fragrant with orange."))),

            make("No-Bake Date & Almond Energy Truffles",
                "Fudgy cocoa-coated Medjool date bites rolled with roasted almonds and coconut.",
                "https://www.themealdb.com/images/media/meals/vgsipt1779917593.jpg",
                12, 0, 12, "SWEETS",
                "Tastes like brownies in bite-sized form with zero refined sugar.",
                "BUDGET", 8.5, "VEGAN,BUDGET,HEALTHY,QUICK,GLUTEN_FREE,DAIRY_FREE", "nuts",
                List.of(ing("Dates","200 g"), ing("Almonds","100 g"), ing("Cocoa","3 tbsp"), ing("Oats","50 g"), ing("Coconut","3 tbsp")),
                List.of("Pulse almonds and oats in a food processor.", "Add pitted dates and cocoa; blend until sticky.", "Roll into bite-sized truffles.", "Coat in coconut or cocoa and chill."),
                List.of(rev("Fatma N.", 5, "Best afternoon snack!"))),

            make("Honey Roasted Sesame Halva Bars",
                "Crunchy caramelized sesame seed candy bars with a hint of lemon.",
                "https://www.themealdb.com/images/media/meals/5vhbzt1782239221.jpg",
                10, 10, 10, "SWEETS",
                "Score into squares while still warm before it sets crisp.",
                "BUDGET", 5.5, "VEGAN,BUDGET,QUICK,GLUTEN_FREE,DAIRY_FREE", "sesame",
                List.of(ing("Sesame","200 g"), ing("Honey","120 ml"), ing("Sugar","60 g"), ing("Lemon","1")),
                List.of("Toast sesame seeds in a dry pan until golden.", "Boil honey, sugar and lemon juice 4 minutes.", "Stir in sesame seeds and press flat on oiled parchment.", "Score squares and cool completely."),
                List.of(rev("Olivia T.", 5, "Crunchy traditional treat!"))),

            make("Flaky Cream Cheese Breakfast Pastries",
                "Crisp puff pastry squares cradling sweet vanilla cream cheese and fresh berries.",
                "https://www.themealdb.com/images/media/meals/1543774956.jpg",
                12, 16, 6, "SWEETS",
                "Looks straight out of a Parisian bakery window with 5 ingredients.",
                "MODERATE", 13.0, "VEGETARIAN,QUICK", "dairy,gluten,eggs",
                List.of(ing("Puff pastry","1 sheet"), ing("Cream cheese","180 g"), ing("Sugar","50 g"), ing("Vanilla","1 tsp"), ing("Strawberry","150 g")),
                List.of("Cut puff pastry into 6 squares and score borders.", "Beat cream cheese, sugar and vanilla.", "Spoon cream cheese in centers and top with sliced berries.", "Bake 16 minutes at 200°C."),
                List.of(rev("Rania M.", 5, "Flaky, creamy and berry-filled!"))),

            make("Russian Thin Blini Crepes",
                "Lacy golden fermented-milk crepes served rolled with honey and berries.",
                "https://www.themealdb.com/images/media/meals/0206h11699013358.jpg",
                10, 15, 6, "SWEETS",
                "A splash of sparkling water in the batter makes lacy delicate edges.",
                "BUDGET", 6.0, "VEGETARIAN,BUDGET", "eggs,dairy,gluten",
                List.of(ing("Flour","180 g"), ing("Milk","350 ml"), ing("Eggs","2"), ing("Sugar","2 tbsp"), ing("Butter","25 g"), ing("Honey","3 tbsp")),
                List.of("Whisk eggs, sugar, milk and flour until silky.", "Stir in melted butter.", "Cook thin crepes in a hot buttered pan.", "Serve warm rolled with honey."),
                List.of(rev("Karim B.", 5, "Delicate and golden!")))
        );
    }

    private List<SeedRecipe> breakfast() {
        return List.of(
            make("Tomato Omelette",
                "Fluffy folded omelette with sweet tomatoes, herbs and melted cheese.",
                "https://images.unsplash.com/photo-1510693206972-df098062cb71?auto=format&fit=crop&w=900&q=80",
                5, 5, 1, "BREAKFAST",
                "Low heat gives you cloud-soft eggs — patience is your secret spatula.",
                "BUDGET", 5.0, "VEGETARIAN,QUICK,BUDGET,GLUTEN_FREE,NUT_FREE", "eggs,dairy",
                List.of(ing("Eggs","3"), ing("Tomatoes","1"), ing("Cheese","30 g"), ing("Butter","10 g"), ing("Salt","1 pinch")),
                List.of("Whisk eggs with salt and pepper.", "Sauté diced tomato in butter 2 minutes.", "Pour eggs in and cook gently.", "Add cheese, fold and serve."),
                List.of(rev("Samir B.", 5, "Fastest breakfast ever!"))),

            make("Fluffy Buttermilk Berry Pancakes",
                "Tall cloud-soft pancakes stacked with warm maple syrup and fresh berries.",
                "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?auto=format&fit=crop&w=900&q=80",
                10, 12, 3, "BREAKFAST",
                "Small lumps in pancake batter are your friend — never overmix!",
                "BUDGET", 7.5, "VEGETARIAN,BUDGET", "eggs,dairy,gluten",
                List.of(ing("Flour","200 g"), ing("Milk","240 ml"), ing("Eggs","2"), ing("Butter","40 g"), ing("Strawberry","150 g"), ing("Sugar","2 tbsp")),
                List.of("Whisk flour, baking powder and sugar.", "Stir in milk, eggs and melted butter gently.", "Cook on a buttered griddle until bubbles pop.", "Stack with berries and syrup."),
                List.of(rev("Sara L.", 5, "Tallest, fluffiest pancakes!"))),

            make("Smashed Avocado Poached Toast",
                "Sourdough topped with lemon-chili avocado, poached eggs and sesame.",
                "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=900&q=80",
                8, 5, 2, "BREAKFAST",
                "A splash of vinegar in simmering water keeps poached eggs tidy.",
                "MODERATE", 12.0, "VEGETARIAN,QUICK,HEALTHY", "eggs,gluten,sesame",
                List.of(ing("Bread","2 slices"), ing("Avocado","1"), ing("Eggs","2"), ing("Lemon","1"), ing("Olive oil","1 tbsp"), ing("Sesame","1 tsp")),
                List.of("Toast sourdough slices.", "Mash avocado with lemon, salt and chili flakes.", "Poach eggs 3 minutes in simmering water.", "Spread avocado, top with eggs and sesame."),
                List.of(rev("Maya C.", 5, "Café brunch at home in 12 minutes."))),

            make("Tunisian Bsissa Honey Bowl",
                "Nutritious roasted barley-chickpea breakfast porridge with olive oil, honey and dates.",
                "https://images.unsplash.com/photo-1511690656952-34342bb7c2f2?auto=format&fit=crop&w=900&q=80",
                5, 0, 2, "BREAKFAST",
                "Ancient Tunisian powerhouse breakfast that keeps you energized all morning.",
                "BUDGET", 4.5, "VEGAN,BUDGET,QUICK,HEALTHY,DAIRY_FREE", "gluten",
                List.of(ing("Bsissa","100 g"), ing("Olive oil","3 tbsp"), ing("Honey","2 tbsp"), ing("Dates","6"), ing("Almonds","20 g")),
                List.of("Mix bsissa flour with olive oil and warm water until creamy.", "Spoon into bowls.", "Top with chopped dates, honey and toasted almonds.", "Serve right away."),
                List.of(rev("Rami H.", 5, "Pure morning energy!"))),

            make("Spicy Bread Omelette Toast",
                "Crispy spiced street-style omelette folded right around toasted bread slices.",
                "https://www.themealdb.com/images/media/meals/hqaejl1695738653.jpg",
                5, 7, 1, "BREAKFAST",
                "Dip the bread into the wet egg mixture before flipping for instant French-toast crunch.",
                "BUDGET", 4.0, "VEGETARIAN,BUDGET,QUICK", "eggs,dairy,gluten",
                List.of(ing("Eggs","2"), ing("Bread","2 slices"), ing("Onion","1"), ing("Coriander","1 tbsp"), ing("Butter","10 g"), ing("Cheese","30 g")),
                List.of("Whisk eggs with finely chopped onion, coriander and salt.", "Pour into a buttered skillet and lay bread slices on top.", "Flip once set, top with cheese and fold.", "Toast crisp and serve hot."),
                List.of(rev("Lina P.", 5, "5-minute weekday lifesaver."))),

            make("Crispy Skillet Breakfast Potatoes",
                "Golden paprika-garlic roasted potato cubes with herbs and bell peppers.",
                "https://www.themealdb.com/images/media/meals/1550441882.jpg",
                10, 20, 4, "BREAKFAST",
                "Parboil or microwave cubed potatoes 3 minutes first for extra-fluffy centers.",
                "BUDGET", 5.0, "VEGAN,BUDGET,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Potatoes","600 g"), ing("Bell pepper","1"), ing("Onion","1"), ing("Garlic","2 cloves"), ing("Paprika","1 tsp"), ing("Olive oil","3 tbsp")),
                List.of("Dice potatoes into even 1-cm cubes.", "Pan-fry in olive oil until browned and crisp.", "Add diced pepper, onion, garlic and paprika.", "Cook 5 more minutes and season."),
                List.of(rev("Theo R.", 5, "Crispy, smoky and feeds four!"))),

            make("Full English Breakfast Platter",
                "Classic hearty breakfast with sunny eggs, grilled tomatoes, mushrooms and toast.",
                "https://www.themealdb.com/images/media/meals/utxryw1511721587.jpg",
                10, 15, 2, "BREAKFAST",
                "Keep cooked items warm in a low oven while you fry the eggs last.",
                "MODERATE", 15.0, "COMFORT", "eggs,gluten,pork",
                List.of(ing("Eggs","2"), ing("Tomatoes","2"), ing("Mushrooms","150 g"), ing("Bread","2 slices"), ing("Bacon","100 g"), ing("Butter","15 g")),
                List.of("Grill halved tomatoes and sear mushrooms in butter.", "Crisp bacon in the skillet.", "Fry eggs sunny-side up.", "Serve everything hot with buttered toast."),
                List.of(rev("Nadia T.", 5, "Ultimate Sunday morning feast!"))),

            make("Banana Oatmeal Pancakes",
                "Wholesome golden oat-and-banana pancakes sweetened naturally with honey.",
                "https://www.themealdb.com/images/media/meals/c400ok1764439058.jpg",
                5, 10, 2, "BREAKFAST",
                "Rest the batter 3 minutes so the oats absorb moisture before hitting the pan.",
                "BUDGET", 5.0, "VEGETARIAN,BUDGET,QUICK,HEALTHY,GLUTEN_FREE", "eggs",
                List.of(ing("Oats","120 g"), ing("Banana","2"), ing("Eggs","2"), ing("Milk","100 ml"), ing("Cinnamon","1 tsp"), ing("Honey","1 tbsp")),
                List.of("Blend oats, banana, eggs, milk and cinnamon smooth.", "Let rest 3 minutes.", "Cook small pancakes on medium heat.", "Serve with honey and sliced fruit."),
                List.of(rev("Ines S.", 5, "Healthy and naturally sweet!"))),

            make("Jamaican Spiced Cornmeal Porridge",
                "Creamy nutmeg-and-vanilla spiced cornmeal porridge with coconut milk.",
                "https://www.themealdb.com/images/media/meals/sng9bm1765320170.jpg",
                5, 15, 2, "BREAKFAST",
                "Whisk cornmeal into cold water first so it never forms lumps.",
                "BUDGET", 5.5, "VEGAN,BUDGET,GLUTEN_FREE,DAIRY_FREE,COMFORT", "",
                List.of(ing("Cornmeal","100 g"), ing("Coconut milk","250 ml"), ing("Cinnamon","1 tsp"), ing("Vanilla","1 tsp"), ing("Sugar","2 tbsp"), ing("Nutmeg","1 pinch")),
                List.of("Whisk cornmeal with cold water into a smooth slurry.", "Simmer coconut milk with cinnamon and nutmeg.", "Pour in slurry stirring constantly for 10 minutes.", "Sweeten with vanilla and sugar."),
                List.of(rev("Mehdi K.", 5, "Warming, creamy and spiced just right."))),

            make("Albanian Flija Layered Breakfast Crepe",
                "Savory-rich layered crepe cake brushed with cultured cream and butter.",
                "https://www.themealdb.com/images/media/meals/eqnf3p1779649407.jpg",
                15, 25, 4, "BREAKFAST",
                "Layering batter and cream under the broiler creates crisp golden ribbons.",
                "MODERATE", 12.0, "VEGETARIAN,COMFORT", "eggs,dairy,gluten",
                List.of(ing("Flour","250 g"), ing("Milk","350 ml"), ing("Eggs","2"), ing("Yogurt","150 g"), ing("Butter","40 g"), ing("Salt","1 tsp")),
                List.of("Whisk flour, milk, eggs and salt into smooth batter.", "Brush a baking pan with butter-yogurt mix and ladle thin batter.", "Broil 3 minutes per layer until golden.", "Repeat layers and serve warm."),
                List.of(rev("Claire B.", 5, "Unique and deeply comforting!"))),

            make("Creamy Southern Breakfast Grits",
                "Velvety stone-ground cornmeal simmered with butter and sharp cheddar.",
                "https://www.themealdb.com/images/media/meals/bfok4w1780242078.jpg",
                5, 15, 3, "BREAKFAST",
                "Stir in sharp cheese off the heat for ultra-smooth creamy grits.",
                "BUDGET", 6.0, "VEGETARIAN,BUDGET,GLUTEN_FREE,COMFORT", "dairy",
                List.of(ing("Cornmeal","150 g"), ing("Milk","300 ml"), ing("Butter","30 g"), ing("Cheese","80 g"), ing("Salt","1 tsp"), ing("Pepper","0.5 tsp")),
                List.of("Bring milk and water to a gentle simmer.", "Whisk in cornmeal slowly and cook 12 minutes.", "Remove from heat and stir in butter and cheese.", "Season with black pepper."),
                List.of(rev("Youssef A.", 5, "Creamy comfort in a bowl."))),

            make("Antiguan Saltfish & Chop-Up Scramble",
                "Colorful Caribbean breakfast of flaked fish sautéed with spinach, eggplant and tomato.",
                "https://www.themealdb.com/images/media/meals/jvjnoh1780086318.jpg",
                15, 15, 3, "BREAKFAST",
                "Packed with veggies and savory herbs for a bright morning plate.",
                "MODERATE", 14.0, "HEALTHY,GLUTEN_FREE,DAIRY_FREE", "fish",
                List.of(ing("Fish","200 g"), ing("Spinach","150 g"), ing("Eggplant","1"), ing("Tomatoes","2"), ing("Onion","1"), ing("Olive oil","2 tbsp")),
                List.of("Simmer diced eggplant and spinach until tender and mash lightly.", "Sauté onion, tomatoes and flaked fish in olive oil.", "Fold vegetable chop-up with seasoned fish.", "Serve warm."),
                List.of(rev("Julie P.", 5, "Savory, hearty and super healthy!"))),

            make("Mediterranean Spinach & Feta Scramble",
                "Soft scrambled eggs folded with wilted baby spinach, tomatoes and tangy feta.",
                "https://www.themealdb.com/images/media/meals/sqrtwu1511721265.jpg",
                5, 6, 2, "BREAKFAST",
                "Take eggs off the heat just before they look done for creamy curds.",
                "BUDGET", 6.5, "VEGETARIAN,BUDGET,QUICK,HEALTHY,GLUTEN_FREE", "eggs,dairy",
                List.of(ing("Eggs","4"), ing("Spinach","100 g"), ing("Feta","60 g"), ing("Tomatoes","1"), ing("Olive oil","1 tbsp")),
                List.of("Wilt spinach and diced tomato in olive oil.", "Pour in beaten eggs and stir gently.", "Fold in crumbled feta off the heat.", "Serve with toast."),
                List.of(rev("Sonia M.", 5, "High-protein and ready in 10 minutes."))),

            make("Cornmeal & Cheese Arepas",
                "Crisp-griddled golden cornmeal cakes split open and stuffed with melted cheese.",
                "https://www.themealdb.com/images/media/meals/ejht7k1780092390.jpg",
                10, 12, 4, "BREAKFAST",
                "Naturally gluten-free and crisp on the outside, steamy inside.",
                "BUDGET", 6.5, "VEGETARIAN,BUDGET,GLUTEN_FREE", "dairy",
                List.of(ing("Cornmeal","250 g"), ing("Cheese","150 g"), ing("Butter","20 g"), ing("Salt","1 tsp"), ing("Water","350 ml")),
                List.of("Mix cornmeal, salt and warm water; rest 5 minutes.", "Shape into thick patties and griddle 5 minutes per side.", "Split warm arepas and tuck in cheese.", "Serve while melty."),
                List.of(rev("Adam N.", 5, "Crisp outside and cheesy inside!"))),

            make("Beetroot & Herb Breakfast Latkes",
                "Vibrant crispy grated beet and potato pancakes topped with yogurt and dill.",
                "https://www.themealdb.com/images/media/meals/qwicc91764368097.jpg",
                15, 12, 3, "BREAKFAST",
                "Squeeze grated beets and potatoes dry in a clean towel for extra crunch.",
                "BUDGET", 6.0, "VEGETARIAN,BUDGET,HEALTHY", "eggs,dairy,gluten",
                List.of(ing("Beetroot","2"), ing("Potatoes","2"), ing("Eggs","1"), ing("Flour","3 tbsp"), ing("Yogurt","100 g"), ing("Dill","1 tbsp")),
                List.of("Grate beets and potatoes and squeeze dry.", "Mix with egg, flour, salt and pepper.", "Pan-fry spoonfuls until crisp and browned.", "Top with yogurt and fresh dill."),
                List.of(rev("Emma L.", 5, "Colorful, crisp and delicious!")))
        );
    }

    private List<SeedRecipe> aperitifs() {
        return List.of(
            make("Crispy Halloumi & Honey Bites",
                "Pan-seared golden halloumi cubes drizzled with warm chili honey and sesame.",
                "https://images.unsplash.com/photo-1541529086526-db283c563270?auto=format&fit=crop&w=900&q=80",
                5, 7, 4, "APERITIFS",
                "Serve straight from the pan while halloumi is squeaky-soft inside.",
                "MODERATE", 16.0, "VEGETARIAN,GLUTEN_FREE,QUICK,DATE_NIGHT", "dairy,sesame",
                List.of(ing("Cheese","250 g"), ing("Honey","2 tbsp"), ing("Olive oil","1 tbsp"), ing("Sesame","1 tbsp"), ing("Lemon","1")),
                List.of("Cube cheese and pat dry.", "Sear in hot olive oil 2 minutes per side until golden.", "Drizzle warm honey and lemon.", "Scatter sesame seeds and serve."),
                List.of(rev("Chloe M.", 5, "Sweet, salty and gone in seconds!"))),

            make("Tunisian Brik à l'Oeuf",
                "Paper-thin crispy malsouka pastry stuffed with tuna, parsley, capers and a soft egg.",
                "https://www.themealdb.com/images/media/meals/grhn401765687086.jpg",
                10, 6, 2, "APERITIFS",
                "Fold quickly and slide straight into hot oil so the yolk stays runny!",
                "BUDGET", 6.5, "BUDGET,QUICK,DAIRY_FREE,NUT_FREE", "eggs,fish,gluten",
                List.of(ing("Eggs","2"), ing("Tuna","100 g"), ing("Parsley","2 tbsp"), ing("Onion","1"), ing("Olive oil","250 ml"), ing("Lemon","1")),
                List.of("Spread tuna, parsley and onion on pastry sheets.", "Crack an egg in the center and fold into a triangle.", "Fry 2 minutes per side until crisp.", "Serve hot with lemon wedges."),
                List.of(rev("Karim A.", 5, "Crunchy perfection!"))),

            make("Tunisian Street Fricassé Bites",
                "Golden mini fried buns stuffed with tuna, harissa, boiled potato, egg and olives.",
                "https://www.themealdb.com/images/media/meals/cbfy1c1784672521.jpg",
                25, 15, 8, "APERITIFS",
                "The undisputed king of Tunisian party platters.",
                "BUDGET", 9.0, "BUDGET,DAIRY_FREE", "eggs,fish,gluten",
                List.of(ing("Flour","400 g"), ing("Yeast","7 g"), ing("Tuna","160 g"), ing("Potatoes","2"), ing("Eggs","2"), ing("Harissa","1 tbsp")),
                List.of("Knead dough, rise, shape oval buns and fry golden.", "Slit open and spread harissa inside.", "Stuff with diced boiled potato, tuna, egg and olives.", "Serve at room temperature."),
                List.of(rev("Houssem Z.", 5, "Everyone asks for the recipe!"))),

            make("Smoky Paprika Patatas Bravas",
                "Crisp roasted potato cubes tossed with smoky garlic-paprika tomato sauce.",
                "https://www.themealdb.com/images/media/meals/3m8yae1763257951.jpg",
                10, 22, 4, "APERITIFS",
                "Roasting at high heat gives blistered edges without deep frying.",
                "BUDGET", 5.0, "VEGAN,BUDGET,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Potatoes","600 g"), ing("Tomatoes","3"), ing("Garlic","2 cloves"), ing("Paprika","1.5 tsp"), ing("Olive oil","3 tbsp")),
                List.of("Cube potatoes, toss with oil and roast 22 minutes at 220°C.", "Simmer tomatoes, garlic and paprika into spicy sauce.", "Toss hot potatoes or drizzle sauce on top.", "Serve with toothpicks."),
                List.of(rev("Paul G.", 5, "Crispy Spanish tapas favorite!"))),

            make("Whipped Feta & Roasted Garlic Dip",
                "Creamy whipped feta and yogurt dip crowned with olive oil and pita chips.",
                "https://images.unsplash.com/photo-1546069901-d5bfd2cbfb1f?auto=format&fit=crop&w=900&q=80",
                10, 20, 4, "APERITIFS",
                "Warm pita wedges turn this into an instant crowd-pleasing mezze.",
                "MODERATE", 12.0, "VEGETARIAN,DATE_NIGHT", "dairy,gluten",
                List.of(ing("Feta","200 g"), ing("Yogurt","100 g"), ing("Garlic","1 bulb"), ing("Olive oil","3 tbsp"), ing("Bread","2 pitas")),
                List.of("Roast garlic bulb wrapped in foil 20 minutes.", "Blend feta, yogurt, roasted garlic and olive oil smooth.", "Swirl onto a plate with extra oil and herbs.", "Serve with warm pita."),
                List.of(rev("Sofia K.", 5, "Creamy, garlicky and addictive."))),

            make("Smoky Baba Ghanoush Eggplant Dip",
                "Charred roasted eggplant whipped with tahini, garlic, lemon and olive oil.",
                "https://www.themealdb.com/images/media/meals/dlmh401760524897.jpg",
                10, 25, 4, "APERITIFS",
                "Prick eggplants with a fork before roasting so they char evenly.",
                "BUDGET", 6.0, "VEGAN,BUDGET,HEALTHY,GLUTEN_FREE,DAIRY_FREE", "sesame",
                List.of(ing("Eggplant","2"), ing("Tahini","3 tbsp"), ing("Lemon","1"), ing("Garlic","2 cloves"), ing("Olive oil","3 tbsp"), ing("Parsley","2 tbsp")),
                List.of("Roast whole eggplants until collapsed and smoky.", "Scoop out flesh and drain excess liquid.", "Mash with tahini, lemon juice and garlic.", "Swirl with olive oil and parsley."),
                List.of(rev("Leila M.", 5, "Silky and smoky mezze dip!"))),

            make("Colombian Spicy Avocado Dip",
                "Creamy blended avocado, cilantro, green onion and lime party dip.",
                "https://www.themealdb.com/images/media/meals/jyvy8u1783800448.jpg",
                10, 0, 4, "APERITIFS",
                "Press plastic wrap directly onto leftovers to keep it bright green.",
                "BUDGET", 8.5, "VEGAN,BUDGET,QUICK,HEALTHY,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Avocado","3"), ing("Tomatoes","1"), ing("Onion","1"), ing("Lemon","1"), ing("Coriander","2 tbsp"), ing("Salt","1 tsp")),
                List.of("Mash ripe avocados coarsely with lime juice.", "Fold in finely diced tomato, onion and coriander.", "Season with salt.", "Serve immediately with tortilla chips."),
                List.of(rev("Marc B.", 5, "Fresh and zesty!"))),

            make("Aubergine & Hummus Mezze Grills",
                "Charred eggplant slices layered over creamy cumin hummus with olive oil.",
                "https://www.themealdb.com/images/media/meals/zub3s91764110535.jpg",
                12, 12, 4, "APERITIFS",
                "Great warm or room temperature on a Mediterranean mezze board.",
                "BUDGET", 7.0, "VEGAN,BUDGET,HEALTHY,GLUTEN_FREE,DAIRY_FREE", "sesame",
                List.of(ing("Eggplant","2"), ing("Chickpeas","300 g"), ing("Tahini","3 tbsp"), ing("Lemon","1"), ing("Garlic","2 cloves"), ing("Olive oil","3 tbsp")),
                List.of("Blend chickpeas, tahini, lemon and garlic into smooth hummus.", "Brush eggplant rounds with oil and grill until tender.", "Spread hummus on a platter and arrange warm eggplant.", "Drizzle with olive oil."),
                List.of(rev("Nour T.", 5, "Hearty plant-based mezze!"))),

            make("Baked Brie with Herb Jam",
                "Warm molten wheel of Brie topped with sweet-tart jam and toasted walnuts.",
                "https://www.themealdb.com/images/media/meals/fg7d641784666908.jpg",
                5, 15, 6, "APERITIFS",
                "Serve straight from the oven with sliced crusty baguette.",
                "GOURMET", 22.0, "VEGETARIAN,DATE_NIGHT,QUICK", "dairy,nuts,gluten",
                List.of(ing("Cheese","250 g"), ing("Honey","2 tbsp"), ing("Walnuts","40 g"), ing("Thyme","1 tsp"), ing("Bread","1 baguette")),
                List.of("Score top of cheese wheel and place in a baking dish.", "Top with honey, thyme and chopped walnuts.", "Bake 15 minutes at 180°C until gooey.", "Serve with baguette slices."),
                List.of(rev("Hugo L.", 5, "Instant dinner-party showstopper."))),

            make("Colombian Stuffed Crispy Potatoes",
                "Golden mashed-potato croquettes stuffed with spiced beef and herbs.",
                "https://www.themealdb.com/images/media/meals/zdyurx1783801146.jpg",
                20, 15, 6, "APERITIFS",
                "Chill filled potato balls 15 minutes before frying so they crisp evenly.",
                "BUDGET", 9.0, "BUDGET,DAIRY_FREE", "",
                List.of(ing("Potatoes","500 g"), ing("Beef","200 g"), ing("Onion","1"), ing("Garlic","2 cloves"), ing("Cumin","1 tsp"), ing("Flour","60 g")),
                List.of("Boil and mash potatoes with salt.", "Cook ground beef with onion, garlic and cumin.", "Wrap potato mash around beef filling into balls.", "Dip in batter and fry until golden."),
                List.of(rev("Giulia R.", 5, "Crunchy outside, savory inside!"))),

            make("Bolani Potato Stuffed Flatbread Triangles",
                "Crisp pan-fried Afghan flatbread triangles filled with seasoned potato and herbs.",
                "https://www.themealdb.com/images/media/meals/de0sns1779555283.jpg",
                20, 12, 6, "APERITIFS",
                "Cut into bite-sized wedges and serve with cool mint yogurt.",
                "BUDGET", 6.0, "VEGETARIAN,BUDGET,DAIRY_FREE", "gluten",
                List.of(ing("Flour","300 g"), ing("Potatoes","3"), ing("Onion","1"), ing("Coriander","1 handful"), ing("Olive oil","4 tbsp"), ing("Salt","1 tsp")),
                List.of("Knead flour, water, salt and oil into smooth dough.", "Mash boiled potatoes with chopped onion and coriander.", "Roll dough thin, fill half, fold and seal.", "Pan-fry until blistered and golden; slice wedges."),
                List.of(rev("Wafa S.", 5, "Crisp, savory and great for sharing."))),

            make("Avocado Dip with Crispy New Potatoes",
                "Roasted baby potatoes served with cool herbed avocado-lime dipping sauce.",
                "https://www.themealdb.com/images/media/meals/flrajf1762341295.jpg",
                10, 22, 4, "APERITIFS",
                "Smash the roasted potatoes lightly so the edges get extra crunchy.",
                "BUDGET", 7.5, "VEGAN,BUDGET,HEALTHY,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Potatoes","500 g"), ing("Avocado","2"), ing("Lemon","1"), ing("Garlic","1 clove"), ing("Olive oil","3 tbsp"), ing("Coriander","2 tbsp")),
                List.of("Roast halved baby potatoes with oil and salt 22 minutes.", "Blend avocado, lemon, garlic and coriander smooth.", "Pile warm potatoes onto a platter.", "Serve with avocado dip."),
                List.of(rev("Clara V.", 5, "Simple and crowd-pleasing!"))),

            make("Steamed Afghan Mantu Dumplings",
                "Delicate steamed dumplings filled with spiced beef, topped with yogurt and tomato.",
                "https://www.themealdb.com/images/media/meals/oemdp11779556068.jpg",
                30, 20, 6, "APERITIFS",
                "The garlicky yogurt and spiced tomato drizzle makes every bite pop.",
                "MODERATE", 15.0, "COMFORT", "dairy,gluten",
                List.of(ing("Flour","250 g"), ing("Beef","250 g"), ing("Onion","2"), ing("Yogurt","150 g"), ing("Tomatoes","2"), ing("Garlic","2 cloves")),
                List.of("Make thin dumpling wrappers and fill with spiced beef and onion.", "Steam dumplings 18 minutes.", "Warm diced tomatoes with garlic and cumin.", "Top steamed dumplings with garlic yogurt and tomato sauce."),
                List.of(rev("Emma P.", 5, "Worth every minute of folding!"))),

            make("Baked Crispy Yuca Fries",
                "Golden oven-baked cassava fries dusted with garlic salt and served with lime.",
                "https://www.themealdb.com/images/media/meals/j223gc1784579841.jpg",
                10, 25, 4, "APERITIFS",
                "Boil yuca sticks 10 minutes before baking for tender creamy centers.",
                "BUDGET", 6.5, "VEGAN,BUDGET,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Yuca","500 g"), ing("Olive oil","3 tbsp"), ing("Garlic","1 tsp"), ing("Paprika","1 tsp"), ing("Lemon","1"), ing("Salt","1 tsp")),
                List.of("Peel and boil yuca sticks 10 minutes until just tender.", "Toss with olive oil, garlic, paprika and salt.", "Bake at 220°C for 22 minutes until crisp.", "Serve with lime wedges."),
                List.of(rev("Rayen K.", 5, "Crispier than regular fries!"))),

            make("Roasted Garlic & Herb Tapas Olives",
                "Warm Mediterranean platter of marinated olives, sun-dried tomatoes and bread.",
                "https://images.unsplash.com/photo-1541014741259-de529411b96a?auto=format&fit=crop&w=900&q=80",
                5, 5, 6, "APERITIFS",
                "Warming olives gently wakes up all the aromatic oils.",
                "BUDGET", 6.0, "VEGAN,BUDGET,QUICK,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Olives","300 g"), ing("Orange","1"), ing("Garlic","3 cloves"), ing("Rosemary","2 sprigs"), ing("Olive oil","4 tbsp")),
                List.of("Warm olive oil gently with smashed garlic and rosemary.", "Add orange peel strips and olives.", "Toss 3 minutes until fragrant.", "Serve warm in a shallow bowl."),
                List.of(rev("Sara D.", 5, "Five minutes and tastes gourmet!")))
        );
    }

    private List<SeedRecipe> boulange() {
        return List.of(
            make("Crispy Brioche Cinnamon Rolls",
                "Pillow-soft spiraled brioche rolls swirled with cinnamon sugar and vanilla glaze.",
                "https://images.unsplash.com/photo-1509365465985-25d11c17e812?auto=format&fit=crop&w=900&q=80",
                25, 22, 8, "BOULANGE",
                "Warm vanilla glaze over fresh-out-of-the-oven rolls = instant weekend happiness.",
                "MODERATE", 13.0, "VEGETARIAN,COMFORT", "eggs,dairy,gluten",
                List.of(ing("Flour","420 g"), ing("Milk","180 ml"), ing("Butter","80 g"), ing("Sugar","90 g"), ing("Cinnamon","2 tbsp"), ing("Yeast","7 g")),
                List.of("Knead flour, warm milk, egg, butter and yeast; rise 1 hour.", "Roll flat, spread butter, sugar and cinnamon.", "Roll up, slice into 8 and rise 30 minutes.", "Bake 22 minutes at 180°C and drizzle icing."),
                List.of(rev("Ranya K.", 5, "Softer than bakery cinnamon rolls!"))),

            make("Tunisian Tabouna Sesame Bread",
                "Traditional crusty round Tunisian flatbread speckled with fennel and sesame.",
                "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=900&q=80",
                20, 18, 4, "BOULANGE",
                "Bake on a preheated baking stone or upside-down tray for a blistered bottom crust.",
                "BUDGET", 3.5, "VEGAN,BUDGET,DAIRY_FREE", "gluten,sesame",
                List.of(ing("Flour","350 g"), ing("Semolina","150 g"), ing("Yeast","7 g"), ing("Olive oil","3 tbsp"), ing("Sesame","2 tbsp"), ing("Salt","1.5 tsp")),
                List.of("Knead flour, semolina, yeast, olive oil and warm water; rise 45 minutes.", "Shape flat round loaves and dimple lightly.", "Sprinkle with sesame seeds.", "Bake at 230°C for 16-18 minutes."),
                List.of(rev("Fares D.", 5, "Smells like grandma's oven!"))),

            make("Rosemary Sea Salt Focaccia",
                "Dimpled olive-oil-rich Italian focaccia crisp on the outside and airy inside.",
                "https://images.unsplash.com/photo-1586444248902-2f64eddc13df?auto=format&fit=crop&w=900&q=80",
                20, 22, 6, "BOULANGE",
                "Be generous with olive oil when dimpling — that's what fries the crust golden.",
                "BUDGET", 6.0, "VEGAN,BUDGET,DAIRY_FREE", "gluten",
                List.of(ing("Flour","450 g"), ing("Olive oil","70 ml"), ing("Yeast","7 g"), ing("Rosemary","2 sprigs"), ing("Salt","1.5 tsp")),
                List.of("Mix flour, yeast, salt, water and olive oil; rise until doubled.", "Stretch into an oiled baking pan.", "Dimple deeply with fingers, drizzle oil and rosemary.", "Bake 22 minutes at 220°C."),
                List.of(rev("Luca M.", 5, "Crunchy, airy and great for sandwiches."))),

            make("Tunisian Stuffed Mtabga Flatbread",
                "Pan-griddled Tunisian semolina flatbread stuffed with spicy onion-tomato harissa filling.",
                "https://images.unsplash.com/photo-1565557623262-b51c2513a641?auto=format&fit=crop&w=900&q=80",
                25, 16, 4, "BOULANGE",
                "Crispy blistered pockets of savory spicy goodness straight from the skillet.",
                "BUDGET", 6.0, "VEGAN,BUDGET,DAIRY_FREE", "gluten",
                List.of(ing("Semolina","300 g"), ing("Flour","150 g"), ing("Onion","2"), ing("Tomatoes","2"), ing("Harissa","1.5 tbsp"), ing("Olive oil","4 tbsp")),
                List.of("Knead semolina, flour, salt and water into pliable dough.", "Cook diced onion, tomato and harissa into a thick filling.", "Roll dough thin, spread filling and fold.", "Griddle on a dry hot skillet 3 minutes per side."),
                List.of(rev("Amira H.", 5, "Southern Tunisian street food at home!"))),

            make("Golden Garlic Herb Dinner Rolls",
                "Pull-apart buttery dinner rolls brushed with roasted garlic and parsley butter.",
                "https://images.unsplash.com/photo-1549931319-a545dcf3bc73?auto=format&fit=crop&w=900&q=80",
                20, 18, 8, "BOULANGE",
                "Brush with garlic butter the second they leave the oven for glossy tops.",
                "BUDGET", 7.0, "VEGETARIAN,BUDGET,COMFORT", "dairy,eggs,gluten",
                List.of(ing("Flour","400 g"), ing("Milk","200 ml"), ing("Butter","60 g"), ing("Eggs","1"), ing("Garlic","2 cloves"), ing("Yeast","7 g")),
                List.of("Knead flour, warm milk, egg, butter and yeast; rise 1 hour.", "Divide into 8 balls in a baking dish; rise 25 minutes.", "Bake 18 minutes at 190°C.", "Brush with melted garlic-parsley butter."),
                List.of(rev("Chloe R.", 5, "Soft, pillowy and gone in minutes."))),

            make("Berber Bouzgene Bread with Pepper Sauce",
                "Crusty North African skillet-baked bread served with smoky roasted pepper dip.",
                "https://www.themealdb.com/images/media/meals/se5vhk1764114880.jpg",
                25, 20, 4, "BOULANGE",
                "Tear off warm pieces and dip straight into spicy olive-oil pepper sauce.",
                "BUDGET", 5.5, "VEGAN,BUDGET,DAIRY_FREE", "gluten",
                List.of(ing("Semolina","300 g"), ing("Flour","150 g"), ing("Bell pepper","2"), ing("Olive oil","4 tbsp"), ing("Yeast","7 g"), ing("Garlic","2 cloves")),
                List.of("Knead semolina, flour, yeast, salt and water; rise 45 minutes.", "Bake flat round loaves until golden.", "Roast peppers and blend with garlic and olive oil.", "Serve warm bread with pepper sauce."),
                List.of(rev("Bilel S.", 5, "Authentic Maghrebi bread!"))),

            make("Bajan Crisp Salt Bread Rolls",
                "Chewy golden Caribbean crusty rolls with a soft, pillowy interior.",
                "https://www.themealdb.com/images/media/meals/sr7chd1780264758.jpg",
                25, 20, 8, "BOULANGE",
                "A light mist of water in the oven creates a crisp bakery crust.",
                "BUDGET", 4.0, "VEGAN,BUDGET,DAIRY_FREE", "gluten",
                List.of(ing("Flour","500 g"), ing("Yeast","7 g"), ing("Sugar","1 tbsp"), ing("Salt","1.5 tsp"), ing("Olive oil","2 tbsp")),
                List.of("Knead flour, yeast, sugar, salt, oil and warm water; rise 1 hour.", "Shape 8 oval rolls and proof 30 minutes.", "Slash tops lightly.", "Bake 20 minutes at 210°C."),
                List.of(rev("Hugo B.", 5, "Crisp crust and fluffy crumb!"))),

            make("Sourdough Country Boule",
                "Crackly artisan crust loaf with deep fermented flavor and open crumb.",
                "https://images.unsplash.com/photo-1589367920969-ab8e050bbb04?auto=format&fit=crop&w=900&q=80",
                30, 35, 8, "BOULANGE",
                "Baking inside a covered pot traps steam for a shiny blistered crust.",
                "BUDGET", 4.0, "VEGAN,BUDGET,DAIRY_FREE", "gluten",
                List.of(ing("Flour","500 g"), ing("Yeast","5 g"), ing("Salt","1.5 tsp"), ing("Water","360 ml")),
                List.of("Mix flour, water, yeast and salt; fold every 30 minutes for 2 hours.", "Shape into a taut round loaf and proof.", "Score top with a sharp blade.", "Bake 35 minutes at 230°C."),
                List.of(rev("Rayen M.", 5, "Bakery-grade loaf at home."))),

            make("Soft Burger Brioche Buns",
                "Feather-light glazed brioche buns that hold up to juicy burgers without crumbling.",
                "https://images.unsplash.com/photo-1555507036-ab1f4038808a?auto=format&fit=crop&w=900&q=80",
                20, 15, 6, "BOULANGE",
                "Egg wash gives them a gorgeous mirror-shine bakery finish.",
                "BUDGET", 7.0, "VEGETARIAN,BUDGET", "eggs,dairy,gluten,sesame",
                List.of(ing("Flour","400 g"), ing("Milk","160 ml"), ing("Butter","55 g"), ing("Eggs","2"), ing("Yeast","7 g"), ing("Sesame","1 tbsp")),
                List.of("Knead flour, warm milk, 1 egg, butter, sugar and yeast; rise.", "Shape 6 smooth rounds and proof 35 minutes.", "Brush with beaten egg and sprinkle sesame.", "Bake 15 minutes at 190°C."),
                List.of(rev("Salma N.", 5, "Never buying store buns again."))),

            make("Garlic & Za'atar Pita Flatbreads",
                "Puffy pocket pitas brushed with fragrant za'atar and olive oil.",
                "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=900&q=80",
                20, 8, 6, "BOULANGE",
                "Bake on a blazing hot tray so they balloon into hollow pockets in 4 minutes!",
                "BUDGET", 4.5, "VEGAN,BUDGET,DAIRY_FREE", "gluten,sesame",
                List.of(ing("Flour","380 g"), ing("Yeast","7 g"), ing("Olive oil","3 tbsp"), ing("Zaatar","2 tbsp"), ing("Salt","1 tsp")),
                List.of("Knead flour, yeast, salt, oil and warm water; rise 1 hour.", "Roll into 6 thin circles.", "Bake at 240°C for 5 minutes until puffed.", "Brush with za'atar olive oil."),
                List.of(rev("Nour K.", 5, "Puffed up like balloons!"))),

            make("Classic French Butter Croissants",
                "Golden crescent pastries with crisp flaky layers for Sunday mornings.",
                "https://images.unsplash.com/photo-1555507036-ab1f4038808a?auto=format&fit=crop&w=901&q=80",
                35, 18, 8, "BOULANGE",
                "Keep the butter cold while folding for crisp, distinct pastry layers.",
                "MODERATE", 14.0, "VEGETARIAN", "dairy,eggs,gluten",
                List.of(ing("Flour","400 g"), ing("Butter","180 g"), ing("Milk","200 ml"), ing("Sugar","40 g"), ing("Yeast","7 g"), ing("Eggs","1")),
                List.of("Make cold dough, chill, and fold in cold butter sheet twice.", "Cut triangles and roll into crescents.", "Proof 45 minutes and brush with egg.", "Bake 18 minutes at 200°C."),
                List.of(rev("Camille L.", 5, "Crispy shattering layers!"))),

            make("Banana Walnut Bread Loaf",
                "Moist spiced banana loaf packed with crunchy toasted walnuts.",
                "https://images.unsplash.com/photo-1606101273945-e9eba91c0dc4?auto=format&fit=crop&w=900&q=80",
                12, 45, 8, "BOULANGE",
                "The spottier and blacker your bananas, the sweeter your loaf!",
                "BUDGET", 8.5, "VEGETARIAN,BUDGET", "eggs,dairy,gluten,nuts",
                List.of(ing("Banana","3"), ing("Flour","220 g"), ing("Butter","80 g"), ing("Sugar","100 g"), ing("Eggs","2"), ing("Walnuts","70 g")),
                List.of("Mash ripe bananas and whisk with melted butter, sugar and eggs.", "Fold in flour, baking soda, cinnamon and walnuts.", "Pour into a lined loaf pan.", "Bake 45 minutes at 175°C."),
                List.of(rev("Maya R.", 5, "Moist and fragrant!"))),

            make("Tunisian Mlawi Griddle Flatbread",
                "Flaky layered semolina flatbread folded with oil and cooked golden on a skillet.",
                "https://images.unsplash.com/photo-1574484284002-952d92456975?auto=format&fit=crop&w=900&q=80",
                20, 15, 6, "BOULANGE",
                "Roll paper-thin and fold like an envelope for dozens of flaky layers.",
                "BUDGET", 4.0, "VEGAN,BUDGET,DAIRY_FREE", "gluten",
                List.of(ing("Semolina","350 g"), ing("Flour","150 g"), ing("Olive oil","60 ml"), ing("Salt","1 tsp")),
                List.of("Knead semolina, flour, salt and water until elastic.", "Rest 20 minutes, divide into balls.", "Stretch thin with oil, fold into squares.", "Cook on a hot skillet 2 minutes per side."),
                List.of(rev("Theo G.", 5, "Perfect for wraps with tuna and harissa!"))),

            make("Cheesy Jalapeño Cornbread Muffins",
                "Tender golden cornmeal muffins flecked with cheddar and mild chili.",
                "https://images.unsplash.com/photo-1607958996333-41aef7caefaa?auto=format&fit=crop&w=900&q=80",
                10, 18, 8, "BOULANGE",
                "Serve warm alongside chili, lablabi or grilled meats.",
                "BUDGET", 7.5, "VEGETARIAN,BUDGET", "eggs,dairy,gluten",
                List.of(ing("Cornmeal","160 g"), ing("Flour","130 g"), ing("Milk","220 ml"), ing("Eggs","1"), ing("Cheese","100 g"), ing("Butter","50 g")),
                List.of("Whisk cornmeal, flour, baking powder and salt.", "Stir in milk, egg, melted butter and grated cheese.", "Divide into muffin cups.", "Bake 18 minutes at 195°C."),
                List.of(rev("Adam B.", 5, "Sweet-savory perfection."))),

            make("Tunisian Olive Oil & Anise Brioche",
                "Light, fragrant Tunisian braided sweet bread scented with anise and orange zest.",
                "https://www.themealdb.com/images/media/meals/swq12t1764112200.jpg",
                25, 25, 8, "BOULANGE",
                "No butter needed — fruity Tunisian olive oil keeps it super tender.",
                "BUDGET", 6.5, "VEGETARIAN,BUDGET,DAIRY_FREE", "eggs,gluten,sesame",
                List.of(ing("Flour","450 g"), ing("Olive oil","80 ml"), ing("Eggs","2"), ing("Sugar","90 g"), ing("Orange","1"), ing("Yeast","7 g")),
                List.of("Knead flour, yeast, eggs, sugar, olive oil and orange zest.", "Rise 1 hour, braid into a loaf and rise 30 minutes.", "Brush with egg and scatter sesame.", "Bake 25 minutes at 180°C."),
                List.of(rev("Ines R.", 5, "Light, aromatic and great with morning coffee!")))
        );
    }

    private List<SeedRecipe> boissons() {
        return List.of(
            make("Iced Strawberry Matcha Latte",
                "Layered sweet strawberry purée, cold oat milk and earthy ceremonial matcha.",
                "https://images.unsplash.com/photo-1515823064-d6e0c04616a7?auto=format&fit=crop&w=900&q=80",
                6, 0, 1, "BOISSONS",
                "Pour the milk slowly over ice so the three colors stay layered!",
                "MODERATE", 11.0, "VEGAN,QUICK,GLUTEN_FREE,DAIRY_FREE,HEALTHY", "",
                List.of(ing("Strawberry","100 g"), ing("Matcha","1 tsp"), ing("Milk","200 ml"), ing("Honey","1 tbsp"), ing("Ice","1 cup")),
                List.of("Mash strawberries with honey and spoon into a tall glass.", "Fill with ice and pour cold milk.", "Whisk matcha with warm water and pour on top.", "Stir before sipping!"),
                List.of(rev("Yasmine F.", 5, "Café-pretty and refreshing!"))),

            make("Tunisian Mint Tea with Pine Nuts",
                "Sweet amber green tea brewed with fresh spearmint and crowned with pine nuts.",
                "https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&w=900&q=80",
                5, 8, 4, "BOISSONS",
                "Pour from high up into tiny glasses to create classic foamy tops.",
                "MODERATE", 10.0, "VEGAN,GLUTEN_FREE,DAIRY_FREE,QUICK", "nuts",
                List.of(ing("Green tea","2 tbsp"), ing("Mint","1 bunch"), ing("Sugar","4 tbsp"), ing("Pine nuts","2 tbsp"), ing("Water","700 ml")),
                List.of("Simmer green tea leaves and sugar in water 6 minutes.", "Steep fresh mint leaves 2 minutes off heat.", "Place pine nuts in tea glasses.", "Pour hot tea high to foam and serve."),
                List.of(rev("Houssem T.", 5, "Pure hospitality in a glass."))),

            make("Tunisian Fresh Citronnade",
                "Whole-lemon chilled Tunisian lemonade blended with sugar and chilled water.",
                "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=900&q=80",
                8, 0, 4, "BOISSONS",
                "Blend whole quartered lemons briefly and strain right away so it stays zesty, never bitter.",
                "BUDGET", 3.5, "VEGAN,BUDGET,QUICK,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Lemon","4"), ing("Sugar","100 g"), ing("Mint","6 leaves"), ing("Water","1000 ml")),
                List.of("Quarter washed lemons and remove seeds.", "Blend with sugar and half the water for 10 seconds.", "Strain through a fine sieve and add remaining cold water.", "Serve over ice with mint."),
                List.of(rev("Sarra B.", 5, "Nothing beats homemade citronnade in summer!"))),

            make("Velvet Iced Caramel Espresso",
                "Double espresso shaken with cold milk, vanilla and salted caramel over ice.",
                "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?auto=format&fit=crop&w=900&q=80",
                5, 0, 1, "BOISSONS",
                "Shaking espresso with ice creates rich café-style microfoam.",
                "BUDGET", 6.0, "VEGETARIAN,BUDGET,QUICK,GLUTEN_FREE", "dairy",
                List.of(ing("Coffee","60 ml"), ing("Milk","180 ml"), ing("Caramel","1 tbsp"), ing("Vanilla","1 tsp"), ing("Ice","1 cup")),
                List.of("Drizzle caramel inside a tall glass.", "Fill with ice and cold milk.", "Shake hot espresso with ice and pour over.", "Stir and enjoy."),
                List.of(rev("Adam K.", 5, "Saves me 12 TND at coffee shops!"))),

            make("Mango Sunset Ginger Fizz",
                "Bright mango purée topped with sparkling water, fresh lime and zesty ginger.",
                "https://images.unsplash.com/photo-1546173159-315724a31696?auto=format&fit=crop&w=900&q=80",
                6, 0, 2, "BOISSONS",
                "Sparkling mocktail perfection for warm evenings.",
                "BUDGET", 7.5, "VEGAN,BUDGET,QUICK,HEALTHY,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Mango","1"), ing("Lemon","1"), ing("Ginger","10 g"), ing("Honey","1 tbsp"), ing("Sparkling water","300 ml")),
                List.of("Blend mango, lemon juice, grated ginger and honey.", "Divide purée into ice-filled glasses.", "Top slowly with sparkling water.", "Garnish with mint."),
                List.of(rev("Maya N.", 5, "Bright, fizzy and refreshing."))),

            make("Apple Berry Smoothie",
                "Crisp orchard apple blended with sweet strawberries and blackberries.",
                "https://www.thecocktaildb.com/images/media/drink/xwqvur1468876473.jpg",
                5, 0, 2, "BOISSONS",
                "Use chilled fruit so you don't dilute the smoothie with ice.",
                "BUDGET", 6.0, "VEGAN,BUDGET,QUICK,HEALTHY,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Apple","2"), ing("Strawberry","150 g"), ing("Blackberries","100 g"), ing("Honey","1 tbsp"), ing("Water","120 ml")),
                List.of("Core apples and chop coarsely.", "Blend apples, berries, honey and cold water until silky.", "Pour into chilled glasses.", "Serve immediately."),
                List.of(rev("Kenza B.", 5, "Bright berry flavor!"))),

            make("Strawberry Banana Milkshake",
                "Classic creamy pink shake blended from ripe strawberries, banana and cold milk.",
                "https://www.thecocktaildb.com/images/media/drink/vqquwx1472720634.jpg",
                4, 0, 2, "BOISSONS",
                "Frozen banana slices make it thick like an ice-cream shop shake.",
                "BUDGET", 5.5, "VEGETARIAN,BUDGET,QUICK,GLUTEN_FREE", "dairy",
                List.of(ing("Strawberry","180 g"), ing("Banana","1"), ing("Milk","250 ml"), ing("Vanilla","1 tsp"), ing("Honey","1 tbsp")),
                List.of("Add strawberries, frozen banana, milk, vanilla and honey to blender.", "Blend 45 seconds until frothy.", "Pour into tall glasses.", "Top with a strawberry."),
                List.of(rev("Zied M.", 5, "Creamy and refreshing!"))),

            make("Greek Frothy Iced Frappé",
                "Classic whipped instant-espresso foam poured over cold milk and ice.",
                "https://www.thecocktaildb.com/images/media/drink/vqwryq1441245927.jpg",
                4, 0, 1, "BOISSONS",
                "Shake coffee, sugar and 2 tbsp cold water vigorously for 20 seconds for thick foam.",
                "BUDGET", 3.5, "VEGETARIAN,BUDGET,QUICK,GLUTEN_FREE", "dairy",
                List.of(ing("Coffee","2 tsp"), ing("Sugar","1 tsp"), ing("Milk","180 ml"), ing("Ice","1 cup"), ing("Water","30 ml")),
                List.of("Shake coffee, sugar and cold water in a jar until thick foam forms.", "Fill a tall glass with ice and milk.", "Spoon foam on top.", "Sip with a straw."),
                List.of(rev("Leo T.", 5, "Summer afternoon essential!"))),

            make("Castillian Spiced Hot Chocolate",
                "Thick velvety Spanish-style sipping chocolate melted with cinnamon and milk.",
                "https://www.thecocktaildb.com/images/media/drink/3nbu4a1487603196.jpg",
                3, 7, 2, "BOISSONS",
                "Thick enough to dip warm churros or croissants right in!",
                "MODERATE", 10.0, "VEGETARIAN,QUICK,COMFORT,GLUTEN_FREE", "dairy",
                List.of(ing("Chocolate","120 g"), ing("Milk","350 ml"), ing("Cornstarch","1 tsp"), ing("Cinnamon","0.5 tsp"), ing("Sugar","1 tbsp")),
                List.of("Whisk cornstarch into cold milk.", "Heat gently with cinnamon and chopped dark chocolate.", "Stir until thick and glossy.", "Pour into warm mugs."),
                List.of(rev("Sami K.", 5, "Rich like melted chocolate bar!"))),

            make("Grape, Lemon & Pineapple Cooler",
                "Tropical cold-pressed juice blend of pineapple, sweet grapes and zesty lemon.",
                "https://www.thecocktaildb.com/images/media/drink/54z5h71487603583.jpg",
                6, 0, 2, "BOISSONS",
                "Naturally sweet without any added sugar.",
                "BUDGET", 7.0, "VEGAN,BUDGET,QUICK,HEALTHY,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Pineapple","200 g"), ing("Grapes","150 g"), ing("Lemon","1"), ing("Ice","1 cup")),
                List.of("Blend pineapple chunks, seedless grapes and lemon juice.", "Strain into ice-filled glasses.", "Garnish with a lemon wheel.", "Serve cold."),
                List.of(rev("Clara R.", 5, "Crisp and tropical."))),

            make("Banana Cantaloupe Sunrise Smoothie",
                "Silky sweet melon and banana breakfast smoothie with a hint of orange.",
                "https://www.thecocktaildb.com/images/media/drink/uqxqsy1468876703.jpg",
                5, 0, 2, "BOISSONS",
                "Super hydrating and naturally sweet morning refresher.",
                "BUDGET", 5.5, "VEGAN,BUDGET,QUICK,HEALTHY,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Melon","250 g"), ing("Banana","1"), ing("Orange","1"), ing("Ice","1 cup")),
                List.of("Cube chilled cantaloupe melon.", "Blend with banana, orange juice and ice until smooth.", "Pour into glasses.", "Serve right away."),
                List.of(rev("Emma S.", 5, "Light and sunny!"))),

            make("Tropical Aloha Fruit Punch",
                "Festive non-alcoholic fruit punch mixing orange, pineapple and grenadine over ice.",
                "https://www.thecocktaildb.com/images/media/drink/wsyvrt1468876267.jpg",
                5, 0, 4, "BOISSONS",
                "Great colorful party mocktail for brunch or weekend gatherings.",
                "BUDGET", 7.0, "VEGAN,BUDGET,QUICK,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Orange","3"), ing("Pineapple","200 ml"), ing("Lemon","1"), ing("Honey","1 tbsp"), ing("Ice","2 cups")),
                List.of("Squeeze fresh oranges and lemon.", "Stir with pineapple juice and honey.", "Pour over ice in tall glasses.", "Garnish with orange slices."),
                List.of(rev("Nour H.", 5, "Kids and adults both love it!"))),

            make("Afterglow Citrus Sunset Mocktail",
                "Layered orange and pineapple juice mocktail with a ruby berry swirl.",
                "https://www.thecocktaildb.com/images/media/drink/vuquyv1468876052.jpg",
                4, 0, 1, "BOISSONS",
                "Pour berry syrup down the side of the glass for a sunset gradient.",
                "BUDGET", 6.0, "VEGAN,BUDGET,QUICK,GLUTEN_FREE,DAIRY_FREE,DATE_NIGHT", "",
                List.of(ing("Orange","150 ml"), ing("Pineapple","100 ml"), ing("Strawberry","40 g"), ing("Ice","1 cup")),
                List.of("Fill a tall glass with ice.", "Pour orange and pineapple juices.", "Slowly pour strawberry syrup down the inside wall.", "Serve with a stir stick."),
                List.of(rev("Salma G.", 5, "Looks like a beach sunset!"))),

            make("Cranberry Ginger Sparkler",
                "Tart ruby cranberry juice lifted with sparkling water, lime and fresh mint.",
                "https://www.thecocktaildb.com/images/media/drink/mzgaqu1504389248.jpg",
                4, 0, 2, "BOISSONS",
                "Tart, bubbly and not overly sweet — awesome alongside rich dinners.",
                "BUDGET", 6.5, "VEGAN,BUDGET,QUICK,GLUTEN_FREE,DAIRY_FREE", "",
                List.of(ing("Cranberries","150 ml"), ing("Lemon","1"), ing("Sparkling water","250 ml"), ing("Mint","6 leaves"), ing("Ice","1 cup")),
                List.of("Fill glasses with ice and mint leaves.", "Pour cranberry juice and fresh lemon juice.", "Top with sparkling water.", "Stir gently and serve."),
                List.of(rev("Rami B.", 5, "Crisp and refreshing!"))),

            make("Velvet Banana Milkshake",
                "Creamy chilled vanilla-banana shake dusted with cinnamon.",
                "https://www.thecocktaildb.com/images/media/drink/rtwwsx1472720307.jpg",
                4, 0, 1, "BOISSONS",
                "Add a pinch of cinnamon or nutmeg to make it taste like banana bread.",
                "BUDGET", 4.0, "VEGETARIAN,BUDGET,QUICK,GLUTEN_FREE", "dairy",
                List.of(ing("Banana","2"), ing("Milk","250 ml"), ing("Vanilla","1 tsp"), ing("Cinnamon","1 pinch"), ing("Honey","1 tsp")),
                List.of("Blend ripe bananas, cold milk, vanilla and honey.", "Pour into a tall chilled glass.", "Dust with cinnamon.", "Serve immediately."),
                List.of(rev("Ines M.", 5, "Simple, creamy and delicious.")))
        );
    }
}
