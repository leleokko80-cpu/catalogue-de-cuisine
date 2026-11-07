package com.recipecatalog.backend.recipe;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {
    private final RecipeRepository repo;

    public DataSeeder(RecipeRepository repo) { this.repo = repo; }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;

        repo.save(make("Spaghetti Carbonara", "Classic Roman pasta with eggs, cheese and crispy pork.",
                10, 15, 4,
                List.of(new Ingredient("Spaghetti", "400 g"), new Ingredient("Eggs", "4"),
                        new Ingredient("Pecorino", "100 g"), new Ingredient("Guanciale", "150 g")),
                List.of("Boil the pasta in salted water.", "Fry the guanciale until crisp.",
                        "Mix the eggs and the cheese.", "Combine everything off the heat and serve.")));

        repo.save(make("Tomato Omelette", "Quick and light breakfast.",
                5, 10, 2,
                List.of(new Ingredient("Eggs", "4"), new Ingredient("Tomato", "2"), new Ingredient("Olive oil", "1 tbsp")),
                List.of("Dice the tomatoes.", "Beat the eggs.", "Cook the tomatoes, add the eggs and fold.")));

        repo.save(make("Tunisian Chakchouka", "Spicy eggs poached in a pepper and tomato sauce.",
                15, 25, 3,
                List.of(new Ingredient("Eggs", "5"), new Ingredient("Peppers", "3"),
                        new Ingredient("Tomatoes", "4"), new Ingredient("Harissa", "1 tsp")),
                List.of("Fry the peppers and tomatoes.", "Add harissa and salt.",
                        "Break the eggs on top and cover until set.")));
    }

    private Recipe make(String title, String desc, int prep, int cook, int servings,
                        List<Ingredient> ingredients, List<String> steps) {
        Recipe r = new Recipe();
        r.setTitle(title);
        r.setDescription(desc);
        r.setPrepTimeMinutes(prep);
        r.setCookTimeMinutes(cook);
        r.setServings(servings);
        r.setIngredients(new ArrayList<>(ingredients));
        r.setSteps(new ArrayList<>(steps));
        return r;
    }
}