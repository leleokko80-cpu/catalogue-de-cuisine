package com.recipecatalog.backend.recipe;
import com.recipecatalog.backend.user.User;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Recipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(length = 2000)
    private String description;
    @Column(length = 1000)
    private String imageUrl;
    private int prepTimeMinutes;
    private int cookTimeMinutes;
    private int servings;

    @ElementCollection
    @CollectionTable(name = "recipe_ingredients")
    @OrderColumn
    private List<Ingredient> ingredients = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "recipe_steps")
    @OrderColumn
    @Column(name = "step", length = 1000)
    private List<String> steps = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author; // null for seeded recipes

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public int getPrepTimeMinutes() { return prepTimeMinutes; }
    public void setPrepTimeMinutes(int v) { this.prepTimeMinutes = v; }
    public int getCookTimeMinutes() { return cookTimeMinutes; }
    public void setCookTimeMinutes(int v) { this.cookTimeMinutes = v; }
    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }
    public List<Ingredient> getIngredients() { return ingredients; }
    public void setIngredients(List<Ingredient> ingredients) { this.ingredients = ingredients; }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}