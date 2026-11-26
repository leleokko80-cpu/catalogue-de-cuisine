package com.recipecatalog.backend.favorite;

import com.recipecatalog.backend.recipe.Recipe;
import com.recipecatalog.backend.recipe.RecipeRepository;
import com.recipecatalog.backend.recipe.RecipeService;
import com.recipecatalog.backend.recipe.dto.RecipeSummaryDto;
import com.recipecatalog.backend.user.User;
import com.recipecatalog.backend.user.UserRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeService recipeService;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           UserRepository userRepository,
                           RecipeRepository recipeRepository,
                           RecipeService recipeService) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.recipeService = recipeService;
    }

   
    public void add(String email, Long recipeId) {
        User user = currentUser(email);
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
        if (favoriteRepository.existsByUserAndRecipe(user, recipe)) {
            return;
        }
        try {
            favoriteRepository.save(new Favorite(user, recipe));
        } catch (DataIntegrityViolationException ignored) {
            // deux clics simultanés : la contrainte unique a déjà protégé la base, c'est OK
        }
    }

    /** Idempotent : pas d'erreur si la recette n'était pas en favori. */
    @Transactional
    public void remove(String email, Long recipeId) {
        User user = currentUser(email);
        favoriteRepository.deleteByUserAndRecipeId(user, recipeId);
    }

    public List<Long> favoriteIds(String email) {
        return favoriteRepository.findRecipeIdsByUser(currentUser(email));
    }

    @Transactional(readOnly = true)
    public List<RecipeSummaryDto> favoriteRecipes(String email) {
        List<Recipe> favorites = favoriteRepository.findRecipesByUserOrderByCreatedAtDesc(currentUser(email));
        return recipeService.toSummaries(favorites);
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
