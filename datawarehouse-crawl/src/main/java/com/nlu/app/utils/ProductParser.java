package com.nlu.app.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlu.app.entity.Product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        product.setOriginalPrice(json.has("original_price") ? json.get("original_price").asDouble() : 0.0);
        product.setDiscount(json.has("discount") ? json.get("discount").asDouble() : 0.0);
        product.setDiscountRate(json.has("discount_rate") ? json.get("discount_rate").asDouble() : 0.0);
        product.setReviewCount(json.has("review_count") ? json.get("review_count").asInt() : 0);
        product.setInventoryStatus(json.has("inventory_status") ? json.get("inventory_status").asText() : null);
        product.setStockItemQty(json.has("stock_item") ? json.get("stock_item").get("qty").asInt() : 0);
        product.setStockItemMaxSaleQty(json.has("stock_item") ? json.get("stock_item").get("max_sale_qty").asInt() : 0);
        product.setProductName(json.has("name") ? json.get("name").asText() : null);
        product.setThumbnailUrl(json.has("thumbnail_url") ? json.get("thumbnail_url").asText() : null);
        product.setRatingAverage(json.has("rating_average") ? json.get("rating_average").asDouble() : 0.0);
        product.setUrlKey(json.has("url_key") ? json.get("url_key").asText() : null);
        product.setUrlPath(json.has("url_path") ? json.get("url_path").asText() : null);

        JsonNode brand = json.get("brand");
        product.setBrandId(brand != null && brand.has("id") ? brand.get("id").asInt() : 0);
        product.setBrandName(brand != null && brand.has("name") ? brand.get("name").asText() : null);

        // Xử lý trường images
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
            product.setImages(new ArrayList<>()); // Khởi tạo danh sách rỗng nếu không có hình ảnh
        }

        // Lấy kích thước và màu sắc từ cấu hình
        product.setSize(getOptions(json, "Size"));
        product.setColor(getOptions(json, "Màu"));

        // Xử lý danh sách sản phẩm con (variations)
        product.setVariations(getVariations(json.path("configurable_products")));
        return product;
    }

    private static Map<String, List<String>> getOptions(JsonNode json, String optionName) {
        Map<String, List<String>> options = new HashMap<>();
        JsonNode configurableOptions = json.path("configurable_options");
        if (configurableOptions.isArray()) {
            for (JsonNode option : configurableOptions) {
                if (option.has("name") && option.get("name").asText().equals(optionName)) {
                    List<String> optionValues = new ArrayList<>();
                    for (JsonNode value : option.path("values")) {
                        // Kiểm tra xem 'label' và 'id' có tồn tại hay không
                        if (value.has("label") && !value.get("label").isNull()) {
                            String label = value.get("label").asText("Không có");
                            optionValues.add(label);
                        }
                    }
                    // Chỉ thêm vào map nếu danh sách optionValues không rỗng
                    if (!optionValues.isEmpty()) {
                        options.put(optionName, optionValues);
                    }
                }
            }
        }
        return options;
    }

    private static List<Map<String, Object>> getVariations(JsonNode configurableProducts) {
        List<Map<String, Object>> variations = new ArrayList<>();
        if (configurableProducts.isArray()) {
            for (JsonNode variation : configurableProducts) {
                Map<String, Object> variationData = new HashMap<>();
                variationData.put("child_id", variation.path("child_id").asInt());
                variationData.put("sku", variation.path("sku").asText());
                variationData.put("name", variation.path("name").asText());
                variationData.put("original_price", variation.path("original_price").asDouble());
                variationData.put("price", variation.path("price").asDouble());
                variationData.put("inventory_status", variation.path("inventory_status").asText());
                variationData.put("options", getVariationOptions(variation)); // Nếu bạn vẫn muốn giữ tùy chọn.

                variations.add(variationData);
            }
        }
        return variations;
    }

    private static String getVariationOptions(JsonNode variation) {
        List<String> variationOptions = new ArrayList<>();

        // Lấy tùy chọn 1
        String option1Name = variation.path("option1").asText("Unknown");
        String option1Value = variation.path("option1").asText("Không có");
        variationOptions.add(option1Name + ": " + option1Value);

        // Lấy tùy chọn 2
        String option2Name = variation.path("option2").asText("Unknown");
        String option2Value = variation.path("option2").asText("Không có");
        variationOptions.add(option2Name + ": " + option2Value);

        return String.join(", ", variationOptions);
    }
}
