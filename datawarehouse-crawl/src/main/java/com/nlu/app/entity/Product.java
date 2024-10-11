package com.nlu.app.entity;

import java.util.List;
import java.util.Map;

public class Product {

    private String id;
    private String spid;
    private String sku;
    private String shortDescription;
    private double price;
    private double listPrice;
    private double originalPrice;
    private double discount;
    private double discountRate;
    private int reviewCount;
    private int orderCount;
    private String inventoryStatus;
    private int stockItemQty;  // Thêm trường stockItemQty
    private int stockItemMaxSaleQty;
    private String productName;
    private int brandId;
    private String brandName;
    private String thumbnailUrl;
    private double ratingAverage;
    private String urlKey;
    private String urlPath;
    private List<String> images;
    private List<Map<String, Object>> variations;
    private Map<String, String> specifications;
    private Map<String, List<String>> size;
    private Map<String, List<String>> color;

    public Map<String, List<String>> getColor() {
        return color;
    }

    public void setColor(Map<String, List<String>> color) {
        this.color = color;
    }

    public Map<String, List<String>> getSize() {
        return size;
    }

    public void setSize(Map<String, List<String>> size) {
        this.size = size;
    }

    public List<Map<String, Object>> getVariations() {
        return variations;
    }

    public void setVariations(List<Map<String, Object>> variations) {
        this.variations = variations;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSpid() {
        return spid;
    }

    public void setSpid(String spid) {
        this.spid = spid;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getListPrice() {
        return listPrice;
    }

    public void setListPrice(double listPrice) {
        this.listPrice = listPrice;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(double discountRate) {
        this.discountRate = discountRate;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public int getStockItemQty() {
        return stockItemQty;
    }

    public void setStockItemQty(int stockItemQty) {
        this.stockItemQty = stockItemQty;
    }

    public int getStockItemMaxSaleQty() {
        return stockItemMaxSaleQty;
    }

    public void setStockItemMaxSaleQty(int stockItemMaxSaleQty) {
        this.stockItemMaxSaleQty = stockItemMaxSaleQty;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getBrandId() {
        return brandId;
    }

    public void setBrandId(int brandId) {
        this.brandId = brandId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public double getRatingAverage() {
        return ratingAverage;
    }

    public void setRatingAverage(double ratingAverage) {
        this.ratingAverage = ratingAverage;
    }

    public String getUrlKey() {
        return urlKey;
    }

    public void setUrlKey(String urlKey) {
        this.urlKey = urlKey;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public Map<String, String> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(Map<String, String> specifications) {
        this.specifications = specifications;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", sku='" + sku + '\'' +
                ", shortDescription='" + shortDescription + '\'' +
                ", price=" + price +
                ", listPrice=" + listPrice +
                ", originalPrice=" + originalPrice +
                ", discount=" + discount +
                ", discountRate=" + discountRate +
                ", reviewCount=" + reviewCount +
                ", orderCount=" + orderCount +
                ", inventoryStatus='" + inventoryStatus + '\'' +
                ", stockItemQty=" + stockItemQty +
                ", stockItemMaxSaleQty=" + stockItemMaxSaleQty +
                ", productName='" + productName + '\'' +
                ", brandId=" + brandId +
                ", brandName='" + brandName + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", ratingAverage=" + ratingAverage +
                ", urlKey='" + urlKey + '\'' +
                ", urlPath='" + urlPath + '\'' +
                ", images=" + images +
                '}';
    }


}
