package com.nlu.app.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlu.app.entity.Product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProductParser {

    public static Product parseProduct(String jsonResponse) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode json = objectMapper.readTree(jsonResponse);

        // Kiểm tra xem các trường bắt buộc có tồn tại hay không
        if (!json.has("id") || !json.has("sku")) {
            throw new IOException("Invalid JSON response");
        }

        Product product = new Product();
        product.setId(json.get("id").asText());
        product.setSku(json.get("sku").asText());
        product.setShortDescription(json.has("short_description") ? json.get("short_description").asText() : null);
        product.setPrice(json.has("price") ? json.get("price").asDouble() : 0.0);
        product.setListPrice(json.has("list_price") ? json.get("list_price").asDouble() : 0.0);
        product.setOriginalPrice(json.has("original_price") ? json.get("original_price").asDouble() : 0.0);  // Thêm originalPrice
        product.setDiscount(json.has("discount") ? json.get("discount").asDouble() : 0.0);
        product.setDiscountRate(json.has("discount_rate") ? json.get("discount_rate").asDouble() : 0.0);
        product.setReviewCount(json.has("review_count") ? json.get("review_count").asInt() : 0);
        product.setInventoryStatus(json.has("inventory_status") ? json.get("inventory_status").asText() : null);
        product.setStockItemQty(json.has("stock_item") ? json.get("stock_item").get("qty").asInt() : 0); // Thêm stockItemQty
        product.setStockItemMaxSaleQty(json.has("stock_item") ? json.get("stock_item").get("max_sale_qty").asInt() : 0); // Thêm stockItemMaxSaleQty
        product.setProductName(json.has("name") ? json.get("name").asText() : null); // Thay đổi từ meta_title thành name
        product.setThumbnailUrl(json.has("thumbnail_url") ? json.get("thumbnail_url").asText() : null); // Thêm thumbnailUrl
        product.setRatingAverage(json.has("rating_average") ? json.get("rating_average").asDouble() : 0.0); // Thêm ratingAverage
        product.setUrlKey(json.has("url_key") ? json.get("url_key").asText() : null); // Thêm urlKey
        product.setUrlPath(json.has("url_path") ? json.get("url_path").asText() : null); // Thêm urlPath

        JsonNode brand = json.get("brand");
        product.setBrandId(brand != null && brand.has("id") ? brand.get("id").asInt() : 0);
        product.setBrandName(brand != null && brand.has("name") ? brand.get("name").asText() : null);

        // Thêm xử lý cho trường images
        if (json.has("images") && json.get("images").isArray()) {
            List<String> images = new ArrayList<>();
            for (JsonNode imageNode : json.get("images")) {
                // Kiểm tra nếu thumbnail_url tồn tại và là chuỗi
                if (imageNode.has("thumbnail_url") && imageNode.get("thumbnail_url").isTextual()) {
                    String imageUrl = imageNode.get("thumbnail_url").asText();
                    images.add(imageUrl);
                }
            }
            product.setImages(images);
        } else {
            product.setImages(new ArrayList<>()); // Nếu không có hình ảnh, khởi tạo danh sách rỗng
        }

        // Lấy danh sách kích cỡ từ 'configurable_options'
        List<String> sizes = new ArrayList<>();
        JsonNode configurableOptions = json.get("configurable_options");
        if (configurableOptions != null && configurableOptions.isArray()) {
            for (JsonNode option : configurableOptions) {
                if (option.has("name") && option.get("name").asText().equals("Kích cỡ")) {  // Chỉ lấy size từ trường 'Kích cỡ'
                    for (JsonNode value : option.get("values")) {
                        if (value.has("label")) {
                            sizes.add(value.get("label").asText());
                        }
                    }
                }
            }
        }
        product.setSize(sizes);

        return product;
    }
}
