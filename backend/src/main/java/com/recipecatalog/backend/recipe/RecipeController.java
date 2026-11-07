package com.recipecatalog.backend.recipe;

import com.recipecatalog.backend.recipe.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {
    private final RecipeService service;

    public RecipeController(RecipeService service) { this.service = service; }

    @GetMapping
    public List<RecipeSummaryDto> list() { return service.list(); }

    @GetMapping("/{id}")
    public RecipeDetailDto get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeDetailDto create(@Valid @RequestBody CreateRecipeRequest request,
                                  Authentication authentication) {
        return service.create(request, authentication);
    }
}