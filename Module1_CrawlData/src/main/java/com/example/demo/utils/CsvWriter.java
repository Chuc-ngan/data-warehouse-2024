package com.example.demo.utils;

import com.example.demo.model.Product;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvWriter {
    public void writeProductsToCsv(List<Product> products, String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            // Ghi tiêu đề
            String[] header = {"Product ID", "SKU", "Product Name", "Price", "Original Price",
                    "Brand Name", "Discount", "Thumbnail URL", "Short Description",
                    "Images", "Colors", "Size", "Rating Average", "Review Count",
                    "Discount Rate", "Quantity Sold", "URL Key", "URL Path", "Short URL", "Type"};
            writer.writeNext(header);

            // Ghi thông tin sản phẩm
            for (Product product : products) {
                String[] productData = {
                        product.getId(),
                        product.getSku(),
                        product.getProductName(),
                        String.valueOf(product.getPrice()),
                        String.valueOf(product.getOriginalPrice()),
                        product.getBrandName(),
                        String.valueOf(product.getDiscount()),
                        product.getThumbnailUrl(),
                        product.getShortDescription(),
                        String.join(";", product.getImages()),  // Nếu images là List<String>
                        String.join(";", product.getColor()),   // Nếu colors là List<String>
                        String.join(";", product.getSizes()),    // Nếu sizes là List<String>
                        String.valueOf(product.getRatingAverage()),
                        String.valueOf(product.getReviewCount()),
                        String.valueOf(product.getDiscountRate()),
                        String.valueOf(product.getQuantitySold()),
                        product.getUrlKey(),
                        product.getUrlPath(),
                        product.getShortUrl(),
                        product.getType()
                };
                writer.writeNext(productData);
                System.out.println("Dữ liệu đã được lưu vào file CSV: " + filePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
