package com.recipecatalog.backend.recipe;

import com.recipecatalog.backend.recipe.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {
    private final RecipeService service;
    private final IngredientNormalizer normalizer;

    public RecipeController(RecipeService service, IngredientNormalizer normalizer) {
        this.service = service;
        this.normalizer = normalizer;
    }

    @GetMapping
    public List<RecipeSummaryDto> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "ingredients", required = false) String ingredients,
            @RequestParam(name = "maxTime", required = false) Integer maxTime,
            @RequestParam(name = "budget", required = false) String budget,
            @RequestParam(name = "excludeAllergen", required = false) String excludeAllergen,
            @RequestParam(name = "diet", required = false) String diet,
            @RequestParam(name = "sort", required = false) String sort) {
        return service.list(q, category, ingredients, maxTime, budget, excludeAllergen, diet, sort);
    }

    @GetMapping("/ranking")
    public List<RecipeSummaryDto> ranking() {
        return service.ranking();
    }

    @PostMapping("/normalize-ingredient")
    public IngredientNormalizer.NormalizationResult normalizeIngredient(@RequestBody Map<String, String> body) {
        return normalizer.normalize(body.getOrDefault("name", ""));
    }

    @GetMapping("/{id}")
    public RecipeDetailDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeDetailDto create(@Valid @RequestBody CreateRecipeRequest request,
                                  Authentication authentication) {
        return service.create(request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        service.delete(id, authentication);
    }

    @PutMapping("/{id}")
    public RecipeDetailDto update(@PathVariable Long id,
                                  @Valid @RequestBody CreateRecipeRequest request,
                                  Authentication authentication) {
        return service.update(id, request, authentication);
    }

    @GetMapping({"/my", "/mine"})
    public List<RecipeSummaryDto> myRecipes(Authentication authentication) {
        return service.myRecipes(authentication);
    }
}
