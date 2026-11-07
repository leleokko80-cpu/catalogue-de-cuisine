package com.recipecatalog.backend.recipe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateRecipeRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Size(max = 1000, message = "Image URL must be at most 1000 characters")
        String imageUrl,

        @Min(value = 0, message = "Prep time cannot be negative")
        int prepTimeMinutes,

        @Min(value = 0, message = "Cook time cannot be negative")
        int cookTimeMinutes,

        @Min(value = 1, message = "Servings must be at least 1")
        int servings,

        @NotEmpty(message = "Add at least one ingredient")
        List<@Valid IngredientDto> ingredients,

        @NotEmpty(message = "Add at least one step")
        List<@NotBlank(message = "Steps cannot be empty") String> steps
) {}