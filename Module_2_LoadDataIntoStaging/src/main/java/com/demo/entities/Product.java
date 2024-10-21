package com.demo.entities;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity // Đánh dấu lớp này là một thực thể JPA
@Table(name = "products") // Tên bảng trong cơ sở dữ liệu
public class Product {
	@Id
	private String id;

	private String sku;
	private String productName;
	private String shortDescription;
	private double price;
	private double originalPrice;
	private double discount;
	private int quantitySold;
	private String description;

	@ElementCollection
	private List<String> images;

	@ElementCollection
	private List<String> sizes;

	@ElementCollection
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

	private LocalDateTime createTime;
}
