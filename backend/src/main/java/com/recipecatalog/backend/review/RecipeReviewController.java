package com.recipecatalog.backend.review;

import com.recipecatalog.backend.review.dto.CreateReviewRequest;
import com.recipecatalog.backend.review.dto.ReviewDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes/{recipeId}/reviews")
public class RecipeReviewController {

    private final RecipeReviewService service;

    public RecipeReviewController(RecipeReviewService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReviewDto> list(@PathVariable Long recipeId) {
        return service.listForRecipe(recipeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto create(@PathVariable Long recipeId,
                            @Valid @RequestBody CreateReviewRequest request,
                            Authentication authentication) {
        return service.create(recipeId, request, authentication);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long recipeId,
                       @PathVariable Long reviewId,
                       Authentication authentication) {
        service.delete(recipeId, reviewId, authentication);
    }
}
