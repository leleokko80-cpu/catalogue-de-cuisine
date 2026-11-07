package com.recipecatalog.backend.recipe;

import jakarta.persistence.Embeddable;

@Embeddable
public class Ingredient {
    private String name;
    private String quantity;

    public Ingredient() {}
    public Ingredient(String name, String quantity) { this.name = name; this.quantity = quantity; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
}