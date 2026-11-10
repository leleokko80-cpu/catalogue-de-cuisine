package com.recipecatalog.backend.favorite;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/recipes/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable Long id, Authentication authentication) {
        favoriteService.add(authentication.getName(), id);
    }

    @DeleteMapping("/recipes/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id, Authentication authentication) {
        favoriteService.remove(authentication.getName(), id);
    }

    @GetMapping("/favorites/ids")
    public List<Long> ids(Authentication authentication) {
        return favoriteService.favoriteIds(authentication.getName());
    }
}
