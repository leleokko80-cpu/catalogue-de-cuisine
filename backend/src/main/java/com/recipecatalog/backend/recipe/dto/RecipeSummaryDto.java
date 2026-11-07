package com.recipecatalog.backend.recipe.dto;

public record RecipeSummaryDto(Long id, String title, String imageUrl,
                               int prepTimeMinutes, int cookTimeMinutes, String authorName) {}