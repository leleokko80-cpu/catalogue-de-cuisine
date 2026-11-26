package com.recipecatalog.backend.recipe.dto;

public record ReviewNoteDto(
        String reviewerName,
        int rating,
        String comment
) {}
