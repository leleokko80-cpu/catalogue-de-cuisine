package com.recipecatalog.backend.favorite;

import com.recipecatalog.backend.recipe.Recipe;
import com.recipecatalog.backend.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserAndRecipe(User user, Recipe recipe);

    @Query("select f.recipe.id from Favorite f where f.user = :user order by f.createdAt desc")
    List<Long> findRecipeIdsByUser(@Param("user") User user);

    // Appelé depuis FavoriteService.remove (méthode @Transactional)
    @Modifying
    @Query("delete from Favorite f where f.user = :user and f.recipe.id = :recipeId")
    void deleteByUserAndRecipeId(@Param("user") User user, @Param("recipeId") Long recipeId);
}
