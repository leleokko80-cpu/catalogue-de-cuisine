package com.recipecatalog.backend.recipe;

import com.recipecatalog.backend.recipe.dto.*;
import com.recipecatalog.backend.user.User;
import com.recipecatalog.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecipeService {
    private final RecipeRepository repo;
    private final UserRepository userRepo;

    public RecipeService(RecipeRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public List<RecipeSummaryDto> list(String q) {
        List<Recipe> recipes = (q == null || q.isBlank())
                ? repo.findAll()
                : repo.findByTitleContainingIgnoreCase(q.trim());
        return recipes.stream().map(this::toSummary).toList();
    }
    @Transactional(readOnly = true)
    public RecipeDetailDto get(Long id) {
        Recipe r = repo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        return toDetail(r);
    }

    @Transactional
    public RecipeDetailDto create(CreateRecipeRequest req, Authentication auth) {
        Recipe r = new Recipe();
        r.setTitle(req.title().trim());
        r.setDescription(req.description());
        r.setImageUrl(req.imageUrl());
        r.setPrepTimeMinutes(req.prepTimeMinutes());
        r.setCookTimeMinutes(req.cookTimeMinutes());
        r.setServings(req.servings());
        r.setIngredients(new ArrayList<>(req.ingredients().stream()
                .map(i -> new Ingredient(i.name().trim(), i.quantity())).toList()));
        r.setSteps(new ArrayList<>(req.steps().stream().map(String::trim).toList()));
        r.setAuthor(currentUser(auth));
        return toDetail(repo.save(r));
    }

    /**
     * The JWT filter (Tessnim) must set authentication.getName() = the user's
     * email.
     */
    private User currentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null; // temporary until auth is merged
        }
        return userRepo.findByEmail(auth.getName()).orElse(null);
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