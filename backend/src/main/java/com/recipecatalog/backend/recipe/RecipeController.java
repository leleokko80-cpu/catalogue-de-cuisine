package com.recipecatalog.backend.recipe;

import com.recipecatalog.backend.recipe.dto.*;
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
}