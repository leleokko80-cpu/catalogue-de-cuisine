package com.recipecatalog.backend.recipe.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RecipeDetailDto(Long id, String title, String description, String imageUrl,
                              int prepTimeMinutes, int cookTimeMinutes, int servings,
                              List<IngredientDto> ingredients, List<String> steps,
                              String authorName, LocalDateTime createdAt) {}