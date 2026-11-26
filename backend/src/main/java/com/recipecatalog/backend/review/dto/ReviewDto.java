package com.recipecatalog.backend.review.dto;

import java.time.LocalDateTime;

public record ReviewDto(
        Long id,
        Long recipeId,
        Long authorId,
        String reviewerName,
        int rating,
        String comment,
        LocalDateTime createdAt
) {}
