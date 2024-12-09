package com.demo.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity // Đánh dấu lớp này là một thực thể JPA
@Table(name = "products") // Tên bảng trong cơ sở dữ liệu
public class Product {
    @Id
    private String id;

    private String sku;
    private String product_name;
    private String short_description;
    private double price;
    private double original_price;
    private double discount;
    private int quantity_sold;
    private String description;


    private String images;


    private String sizes;


    private String color;

    private String brand_name;
    private String thumbnail_url;
    private int discount_rate;
    private double rating_average;
    private int review_count;
    private String url_key;
    private String url_path;
    private String short_url;
    private String type;

    private LocalDateTime create_time;
}