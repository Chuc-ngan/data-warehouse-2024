package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class Product {
    private String id;
    private String sku;
    private String productName;
    private String shortDescription;
    private double price;
    private double originalPrice;
    private double discount;
    private int quantitySold;
    private String description;
    private List<String> images;
    private List<String> sizes;
    private List<String> color;
    private String brandName;
    private String thumbnailUrl;
    private int discountRate;
    private double ratingAverage;
    private int reviewCount;
    private String urlKey;
    private String urlPath;
    private String shortUrl;
    private String type;



}