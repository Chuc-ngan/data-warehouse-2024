package com.nlu.app.utils;

import com.nlu.app.entity.Product;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ProductCsvWriter {
    private static final Logger logger = LoggerFactory.getLogger(ProductCsvWriter.class);

    public void saveProductsToCsv(List<Product> products, String csvFilePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFilePath))) {
            // Ghi tiêu đề CSV
            String[] header = {"id", "sku", "description", "price", "listPrice", "original_price","sizes", "discount", "discountRate",
                    "reviewCount", "inventoryStatus", "stockItemMaxSaleQty",
                    "productName", "brandId", "brandName", "thumbnail_url", "url_key", "url_path", "rating_average", "images"};
            writer.writeNext(header);

            // Ghi dữ liệu sản phẩm
            for (Product product : products) {
                String[] data = {
                        product.getId(),
                        product.getSku(),
                        product.getShortDescription(),
                        String.valueOf(product.getPrice()),
                        String.valueOf(product.getListPrice()),
                        String.valueOf(product.getOriginalPrice()),
                        String.valueOf(product.getSize()),
                        String.valueOf(product.getDiscount()),
                        String.valueOf(product.getDiscountRate()),
                        String.valueOf(product.getReviewCount()),
                        product.getInventoryStatus(),
                        String.valueOf(product.getStockItemMaxSaleQty()),
                        product.getProductName(),
                        String.valueOf(product.getBrandId()),
                        product.getBrandName(),
                        product.getThumbnailUrl(),
                        product.getUrlKey(),
                        product.getUrlPath(),
                        String.valueOf(product.getRatingAverage()),
                        String.valueOf(product.getImages())
                };
                writer.writeNext(data);
            }

            logger.info("Products saved to CSV at: {}", csvFilePath);
        } catch (IOException e) {
            logger.error("Error saving products to CSV: {}", e.getMessage());
        }
    }
}
