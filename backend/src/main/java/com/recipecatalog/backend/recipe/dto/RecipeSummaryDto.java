package com.recipecatalog.backend.recipe.dto;

import java.util.List;

public record RecipeSummaryDto(
        Long id,
        String title,
        String description,
        String imageUrl,
        int prepTimeMinutes,
        int cookTimeMinutes,
        int servings,
        String authorName,
        String category,
        String funNote,
        String budgetLevel,
        Double estimatedCostTnd,
        String dietaryTags,
        String allergens,
        List<IngredientDto> ingredients,
        double averageRating,
        int ratingCount,
        long favoriteCount,
        List<ReviewNoteDto> topNotes
) {}
