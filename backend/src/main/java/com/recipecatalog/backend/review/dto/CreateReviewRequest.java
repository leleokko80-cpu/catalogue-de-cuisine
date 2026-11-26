package com.recipecatalog.backend.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @Min(value = 1, message = "Rating must be at least 1 star")
        @Max(value = 5, message = "Rating must be at most 5 stars")
        int rating,

        @NotBlank(message = "Please write a comment about this recipe")
        @Size(max = 1500, message = "Comment must be at most 1500 characters")
        String comment
) {}
