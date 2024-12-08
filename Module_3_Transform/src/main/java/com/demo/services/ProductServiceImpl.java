package com.demo.services;

import com.demo.entities.Log;
import com.demo.entities.LogLevel;
import com.demo.entities.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private JdbcTemplate jdbcTemplate; // Kết nối tới database control
    @Autowired
    private Environment environment;
    @Autowired
    private MailService mailService;
    @Autowired
    private LogService logService;
    LocalDateTime currentTime = LocalDateTime.now();

    @Override
    public void TransformData() {
        String procedureCall = "CALL transform_and_cleaning_data()";
        String res;
        try {
            // Gọi stored procedure và lấy kết quả
            List<Map<String, Object>> results = jdbcTemplate.queryForList(procedureCall);

            for (Map<String, Object> record : results) {
                if ("Không có record nào hết".equals(record.get("error"))) {
                    // Gửi email thông báo thất bại
                    String body = "<html>" +
                            "<body>" +
                            "<h2 style='color:red;'>Transform dữ liệu thất bại</h2>" +
                            "<p>Quy trình load file vào bảng tạm chưa được thực hiện, " +
                            "không thể thực hiện transform</p>" +
                            "</body>" +
                            "</html>";

                    String from = environment.getProperty("spring.mail.username");
                    mailService.send(from, "phamnhuttan.9a6.2017@gmail.com",
                            "Transform thất bại", body);
                    return;
                }

                String productId = record.get("product_id").toString();
                String sku = record.get("sku").toString();
                String productName = record.get("product_name").toString();
                String price = record.get("price").toString();
                String originalPrice = record.get("original_price").toString();
                String brand = record.get("brand_name").toString();
                String discountValue = record.get("discount_value").toString();
                String thumbnailUrl = record.get("thumbnail_url").toString();
                String shortDescription = record.get("short_description").toString();
                String imageUrls = record.get("image_urls").toString();
                String colorOptions = record.get("color_options").toString();
                String sizeOptions = record.get("size_options").toString();
                String ratingAverage = record.get("rating_average").toString();
                String reviewCount = record.get("review_count").toString();
                String discountRate = record.get("discount_rate").toString();
                String quantitySold = record.get("quantity_sold").toString();
                String urlKey = record.get("url_key").toString();
                String urlPath = record.get("url_path").toString();
                String shortUrl = record.get("short_url").toString();
                String productType = record.get("product_type").toString();
                String createdAt = record.get("created_at").toString();
                String idDate = record.get("id_date").toString();

                // Kiểm tra null cho từng trường
                if (checkFieldNull("product_id", productId) ||
                        checkFieldNull("sku", sku) ||
                        checkFieldNull("product_name", productName) ||
                        checkFieldNull("price", price) ||
                        checkFieldNull("original_price", originalPrice) ||
                        checkFieldNull("brand_name", brand) ||
                        checkFieldNull("discount_value", discountValue) ||
                        checkFieldNull("thumbnail_url", thumbnailUrl) ||
                        checkFieldNull("short_description", shortDescription) ||
                        checkFieldNull("image_urls", imageUrls) ||
                        checkFieldNull("color_options", colorOptions) ||
                        checkFieldNull("size_options", sizeOptions) ||
                        checkFieldNull("rating_average", ratingAverage) ||
                        checkFieldNull("review_count", reviewCount) ||
                        checkFieldNull("discount_rate", discountRate) ||
                        checkFieldNull("quantity_sold", quantitySold) ||
                        checkFieldNull("url_key", urlKey) ||
                        checkFieldNull("url_path", urlPath) ||
                        checkFieldNull("short_url", shortUrl) ||
                        checkFieldNull("product_type", productType) ||
                        checkFieldNull("created_at", createdAt) ||
                        checkFieldNull("id_date", idDate)) {
                    return; // Nếu có trường null, ngừng ngay lập tức
                }

                //24. Gọi phương thức checkPriceValidate để kiểm tra có trường nào liên quan tới giá tiền hợp lệ không?
                if (checkPriceValidate(price, "price") ||
                        checkPriceValidate(originalPrice, "original_price")) {
                    return;
                }

                if(checkNegativeNumber("discount_value", discountValue)||
                        checkNegativeNumber("rating_average", ratingAverage)||
                        checkNegativeNumber("review_count", reviewCount)||
                        checkNegativeNumber("discount_rate", discountRate)||
                        checkNegativeNumber("quantity_sold", quantitySold)){
                    return;
                }

                //27. Gửi mail tới người chịu trách nhiệm về việc bảo trì process với nội dung là Transform toàn bộ dữ liệu hoàn tất
                String body = "<html>" +
                        "<body>" +
                        "<h2 style='color:green;'>Transform thành công!</h2>" +
                        "<p>Transform toàn bộ dữ liệu hoàn tất</p>" +
                        "</body>" +
                        "</html>";

                String from = environment.getProperty("spring.mail.username");
                mailService.send(from, "phamnhuttan.9a6.2017@gmail.com",
                        "Transform thành công!", body);
            }
        } catch (Exception e) {
            String body = "<html>" +
                    "<body>" +
                    "<h2 style='color:red;'>Transform thất bại!</h2>" +
                    "<p>Đã xảy ra lỗi trong quá trình transform vào staging db</p>" +
                    "<p>" + e.getMessage() + "</p>" +
                    "</body>" +
                    "</html>";

            String from = environment.getProperty("spring.mail.username");
            mailService.send(from, "phamnhuttan.9a6.2017@gmail.com",
                    "Transform thất bại", body);

            Integer logId = logService.getLogIdForToday();
            if (logId != null) {
                logService.updateLogStatus(
                        logId,
                        "Transform thất bại"
                );
            } else {
            logService.insertLog(new Log(
                    0,
                    LogLevel.ERROR,
                    null,
                    0,
                    "Tranform",
                    currentTime,
                    "Không tìm thấy record nào",
                    "",
                    Status.FAILURE_TRANSFORM,
                    "ADMIN",
                    currentTime
            ));
            }
            e.printStackTrace();
        }
    }

    private boolean checkFieldNull(String fieldName, String fieldValue) {
        if (fieldValue == null) {
            String body = "<html>" +
                    "<body>" +
                    "<h2 style='color:red;'>Transform thất bại!</h2>" +
                    "<p>" + fieldName + " có giá trị null" + "</p>" +
                    "</body>" +
                    "</html>";

            String from = environment.getProperty("spring.mail.username");
            mailService.send(from, "phamnhuttan.9a6.2017@gmail.com",
                    "Transform thất bại!", body);

            Integer logId = logService.getLogIdForToday();
            logService.updateLogStatus(
                    logId,
                    "Transform thất bại do" + fieldName + " có giá trị null"
            );

            logService.insertLog(new Log(
                    0,
                    LogLevel.ERROR,
                    null,
                    0,
                    "Tranform",
                    currentTime,
                    "Transform thất bại do" + fieldName + " có giá trị null",
                    "",
                    Status.FAILURE_TRANSFORM,
                    "ADMIN",
                    currentTime
            ));

            return true;
        }
        return false;
    }

    private boolean checkPriceValidate(String fieldName, String fieldValue) {
        if (fieldName.toLowerCase().contains("đ")) {
            // 25. Gửi mail tới người chịu trách nhiệm về việc bảo trì process với nội dung là có chứa ký tự không hợp lệ trong giá tiền
            String body = "<html>" +
                    "<body>" +
                    "<h2 style='color:red;'>Transform thất bại!</h2>" +
                    "<p>" + fieldValue + "có chứa ký dự không hợp lệ như đ </p>" +
                    "</body>" +
                    "</html>";

            String from = environment.getProperty("spring.mail.username");
            mailService.send(from, "phamnhuttan.9a6.2017@gmail.com",
                    "Transform thất bại", body);

            //26. Ghi log về lỗi với nội dung là có chứa ký dự không hợp lệ như đ
            Integer logId = logService.getLogIdForToday();
            logService.updateLogStatus(
                    logId,
                    "Transform thất bại do" + fieldName + " có chứa ký dự không hợp lệ như đ"
            );

//            logService.insertLog(new Log(
//                    0,
//                    LogLevel.ERROR,
//                    null,
//                    0,
//                    "Tranform",
//                    currentTime,
//                    "Transform thất bại do" + fieldName + " có chứa ký dự không hợp lệ như đ",
//                    "",
//                    Status.FAILURE_TRANSFORM,
//                    "ADMIN",
//                    currentTime
//            ));

            return true;
        }
        return false;
    }

    private boolean checkNegativeNumber(String fieldName, String fieldValue) {
        double value = Double.parseDouble(fieldValue);
        String body = "<html>" +
                "<body>" +
                "<h2 style='color:red;'>Transform thất bại!</h2>" +
                "<p>" + fieldValue + "có chứa trường có giá trị âm</p>" +
                "</body>" +
                "</html>";

        String from = environment.getProperty("spring.mail.username");
        mailService.send(from, "phamnhuttan.9a6.2017@gmail.com",
                "Transform thất bại", body);
        if (value < 0) {
//            logService.insertLog(new Log(
//                    0,
//                    LogLevel.ERROR,
//                    null,
//                    0,
//                    "Tranform",
//                    currentTime,
//                    "Transform thất bại do" + fieldName + " có chứa trường có giá trị âm",
//                    "",
//                    Status.FAILURE_TRANSFORM,
//                    "ADMIN",
//                    currentTime
//            ));

            Integer logId = logService.getLogIdForToday();
            logService.updateLogStatus(
                    logId,
                    "Transform thất bại do" + fieldName + " có chứa trường có giá trị âm"
            );
            return true;
        }

        return false;
    }
}
