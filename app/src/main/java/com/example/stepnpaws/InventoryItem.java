package com.example.stepnpaws;

public class InventoryItem {
    private String name;
    private String type;
    private int imageRes;

    public InventoryItem(String name, String type, int imageRes) {
        this.name = name;
        this.type = type;
        this.imageRes = imageRes;
    }

    // Getters
    public String getName() { return name; }
    public String getType() { return type; }
    public int getImageRes() { return imageRes; }
}