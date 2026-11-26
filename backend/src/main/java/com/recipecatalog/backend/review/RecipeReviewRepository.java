package com.recipecatalog.backend.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecipeReviewRepository extends JpaRepository<RecipeReview, Long> {

    List<RecipeReview> findByRecipeIdOrderByCreatedAtDesc(Long recipeId);

    Optional<RecipeReview> findByRecipeIdAndUserId(Long recipeId, Long userId);

    @Query("SELECT AVG(r.rating) FROM RecipeReview r")
    Double findGlobalAverageRating();

    @Query("SELECT r FROM RecipeReview r JOIN FETCH r.recipe WHERE r.comment IS NOT NULL AND TRIM(r.comment) <> '' ORDER BY r.createdAt DESC, r.id DESC")
    List<RecipeReview> findAllWithCommentOrderByCreatedAtDesc();

    long countByRecipeId(Long recipeId);

    @Query("SELECT r.recipe.id, AVG(r.rating), COUNT(r) FROM RecipeReview r GROUP BY r.recipe.id")
    List<Object[]> findAllRatingStats();

    @Query("SELECT AVG(r.rating), COUNT(r) FROM RecipeReview r WHERE r.recipe.id = :recipeId")
    List<Object[]> findRatingStatsByRecipeId(@Param("recipeId") Long recipeId);

    @Modifying
    @Query("delete from RecipeReview r where r.recipe.id = :recipeId")
    void deleteByRecipeId(@Param("recipeId") Long recipeId);
}
