package com.nlu.app.entity;

public class Variation {
    private int childId;
    private String sku;
    private String name;
    private double originalPrice;
    private double price;
    private String thumbnailUrl;
    private String inventoryStatus;
    private String options;

    public Variation(int childId, String sku, String name, double originalPrice, double price, String thumbnailUrl, String inventoryStatus, String options) {
        this.childId = childId;
        this.sku = sku;
        this.name = name;
        this.originalPrice = originalPrice;
        this.price = price;
        this.thumbnailUrl = thumbnailUrl;
        this.inventoryStatus = inventoryStatus;
        this.options = options;
    }
}
