package com.recipecatalog.backend.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByTitleContainingIgnoreCase(String title);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Recipe r WHERE r.author.id = :authorId ORDER BY r.id DESC")
    List<Recipe> findByAuthorIdOrderByIdDesc(@org.springframework.data.repository.query.Param("authorId") Long authorId);
}