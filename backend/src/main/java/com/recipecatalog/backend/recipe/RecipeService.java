package com.recipecatalog.backend.recipe;

import com.recipecatalog.backend.recipe.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RecipeService {
    private final RecipeRepository repo;

    public RecipeService(RecipeRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<RecipeSummaryDto> list() {
        return repo.findAll().stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public RecipeDetailDto get(Long id) {
        Recipe r = repo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        return toDetail(r);
    }

    public RecipeSummaryDto toSummary(Recipe r) {
        return new RecipeSummaryDto(r.getId(), r.getTitle(), r.getImageUrl(),
                r.getPrepTimeMinutes(), r.getCookTimeMinutes(), authorName(r));
    }

    public RecipeDetailDto toDetail(Recipe r) {
        return new RecipeDetailDto(r.getId(), r.getTitle(), r.getDescription(), r.getImageUrl(),
                r.getPrepTimeMinutes(), r.getCookTimeMinutes(), r.getServings(),
                r.getIngredients().stream().map(i -> new IngredientDto(i.getName(), i.getQuantity())).toList(),
                List.copyOf(r.getSteps()), authorName(r), r.getCreatedAt());
    }

    private String authorName(Recipe r) {
        return r.getAuthor() == null ? "Anonymous" : r.getAuthor().getName();
    }
}