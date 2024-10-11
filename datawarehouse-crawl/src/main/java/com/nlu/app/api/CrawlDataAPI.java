package com.nlu.app.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlu.app.helper.CsvReader;
import com.nlu.app.model.Product;
import com.nlu.app.helper.ProductParser;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CrawlDataAPI {
    private static final Logger logger = LoggerFactory.getLogger(CrawlDataAPI.class);
    private static final String API_URL = "https://tiki.vn/api/v2/products/";
    private static final String CSV_FILE_PATH = "crawled_data.csv";
    private static final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Product> fetchProducts(List<String> productIds) {
        List<Product> products = new ArrayList<>();

        for (String pid : productIds) {
            try {
                // Tạo URL từ API_URL và product ID
                URL url = new URL(API_URL + pid);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setRequestProperty("Accept", "application/json");

                // Kiểm tra phản hồi
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Đọc phản hồi từ API
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // Phân tích dữ liệu JSON
                    JsonNode jsonNode = objectMapper.readTree(response.toString());
                    Product product = ProductParser.parseProduct(jsonNode.toString());
                    products.add(product);
                    logger.info("Crawl data {} success !!!", pid);

                    // Lấy giá trị spid
                    JsonNode impressionInfoArray = jsonNode.path("impression_info");
                    if (impressionInfoArray.isArray() && impressionInfoArray.size() > 0) {
                        JsonNode metadata = impressionInfoArray.get(0).path("metadata");
                        int spid = metadata.path("spid").asInt(-1); // Trả về -1 nếu không có spid
                        if (spid != -1) {
                            System.out.println("spid: " + spid);

                            // Gọi API để lấy mô tả và danh sách ảnh
                            URL detailUrl = new URL(API_URL + pid + "?platform=web&spid=" + spid + "&version=3");
                            HttpURLConnection detailConnection = (HttpURLConnection) detailUrl.openConnection();
                            detailConnection.setRequestMethod("GET");
                            detailConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
                            detailConnection.setRequestProperty("Accept", "application/json");

                            int detailResponseCode = detailConnection.getResponseCode();
                            if (detailResponseCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader detailIn = new BufferedReader(new InputStreamReader(detailConnection.getInputStream()));
                                StringBuilder detailResponse = new StringBuilder();
                                while ((inputLine = detailIn.readLine()) != null) {
                                    detailResponse.append(inputLine);
                                }
                                detailIn.close();

                                JsonNode detailNode = objectMapper.readTree(detailResponse.toString());
                                String description = detailNode.path("short_description").asText("Không có mô tả");
                                String thumbnailUrl = detailNode.path("thumbnail_url").asText("");

                                // Lấy danh sách thumbnail_url
                                List<String> images = new ArrayList<>();
                                JsonNode imagesNode = detailNode.path("images");
                                if (imagesNode.isArray()) {
                                    for (JsonNode imageNode : imagesNode) {
                                        String imageUrl = imageNode.path("thumbnail_url").asText();
                                        images.add(imageUrl);
                                    }
                                }

                                // Lưu các thuộc tính cần thiết vào sản phẩm
                                product.setShortDescription(description);
                                product.setThumbnailUrl(thumbnailUrl);
                                product.setImages(images);
                            } else {
                                logger.error("Failed to fetch details for product ID {}: HTTP response code {}", pid, detailResponseCode);
                            }
                        } else {
                            logger.warn("spid not found for product ID: {}", pid);
                        }
                    }
                } else {
                    logger.error("Failed to fetch data for product ID {}: HTTP response code {}", pid, responseCode);
                }

                // Thêm độ trễ ngẫu nhiên giữa các yêu cầu
                TimeUnit.SECONDS.sleep(3 + random.nextInt(3));  // Đợi ngẫu nhiên từ 3 đến 5 giây
            } catch (IOException | InterruptedException e) {
                logger.error("Error fetching data for product ID {}: {}", pid, e.getMessage());
            }
        }
        return products;
    }

    public void saveProductsToCsv(List<Product> products) {
        String csvFilePath = "D:\\workspace\\Project\\DataWarehouse\\data-warehouse-2024\\datawarehouse-crawl\\crawl.csv"; // Đường dẫn file CSV

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFilePath))) {
            // Ghi tiêu đề CSV
            String[] header = {"id", "sku", "description", "price", "listPrice", "original_price", "discount", "discountRate",
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

    public static void main(String[] args) {
        CrawlDataAPI crawlDataAPI = new CrawlDataAPI();
        CsvReader csvReader = new CsvReader();
        List<String> productIds = csvReader.readProductIdsFromCsv("D:\\workspace\\Project\\DataWarehouse\\data-warehouse-2024\\datawarehouse-crawl\\products_id.csv");

        // Lấy tối đa 10 ID sản phẩm từ danh sách
        List<String> limitedProductIds = new ArrayList<>(productIds.subList(0, Math.min(productIds.size(), 10)));

        // Crawl dữ liệu sản phẩm
        List<Product> products = crawlDataAPI.fetchProducts(limitedProductIds);
        crawlDataAPI.saveProductsToCsv(products);
    }
}
