package com.recipecatalog.backend.recipe.dto;

import jakarta.validation.constraints.NotBlank;

public record IngredientDto(@NotBlank(message = "Ingredient name is required") String name,
                            String quantity) {}