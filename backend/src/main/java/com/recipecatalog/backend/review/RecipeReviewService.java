package com.recipecatalog.backend.review;

import com.recipecatalog.backend.recipe.Recipe;
import com.recipecatalog.backend.recipe.RecipeRepository;
import com.recipecatalog.backend.review.dto.CreateReviewRequest;
import com.recipecatalog.backend.review.dto.ReviewDto;
import com.recipecatalog.backend.user.User;
import com.recipecatalog.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RecipeReviewService {

    private final RecipeReviewRepository reviewRepo;
    private final RecipeRepository recipeRepo;
    private final UserRepository userRepo;

    public RecipeReviewService(RecipeReviewRepository reviewRepo,
                               RecipeRepository recipeRepo,
                               UserRepository userRepo) {
        this.reviewRepo = reviewRepo;
        this.recipeRepo = recipeRepo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> listForRecipe(Long recipeId) {
        if (!recipeRepo.existsById(recipeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
        }
        return reviewRepo.findByRecipeIdOrderByCreatedAtDesc(recipeId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ReviewDto create(Long recipeId, CreateReviewRequest req, Authentication auth) {
        User user = requireUser(auth);
        Recipe recipe = recipeRepo.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        if (recipe.getAuthor() != null && recipe.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot review your own recipe");
        }

        RecipeReview review = reviewRepo.findByRecipeIdAndUserId(recipeId, user.getId())
                .orElseGet(() -> {
                    RecipeReview r = new RecipeReview();
                    r.setRecipe(recipe);
                    r.setUser(user);
                    return r;
                });
        review.setReviewerName(user.getName());
        review.setRating(req.rating());
        review.setComment(req.comment().trim());

        return toDto(reviewRepo.save(review));
    }

    @Transactional
    public void delete(Long recipeId, Long reviewId, Authentication auth) {
        User user = requireUser(auth);
        RecipeReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

        if (!review.getRecipe().getId().equals(recipeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found for this recipe");
        }
        if (review.getUser() == null || !review.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own comments");
        }
        reviewRepo.delete(review);
    }

    private User requireUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private ReviewDto toDto(RecipeReview r) {
        return new ReviewDto(
                r.getId(),
                r.getRecipe().getId(),
                r.getUser() == null ? null : r.getUser().getId(),
                r.getReviewerName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
