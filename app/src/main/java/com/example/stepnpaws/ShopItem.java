package com.example.stepnpaws;

public class ShopItem {
    private String name;
    private String description;
    private int price;
    private int imageRes;
    private String type; // "pet" or "background"

    public ShopItem(String name, String description, int price, int imageRes, String type) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageRes = imageRes;
        this.type = type;
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPrice() { return price; }
    public int getImageRes() { return imageRes; }
    public String getType() { return type; }
}