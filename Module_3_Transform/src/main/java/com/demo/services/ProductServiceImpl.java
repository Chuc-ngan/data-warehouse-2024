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
    LocalDateTime currentTime;

    @Override
    public void TransformData() {
        String procedureCall = "CALL transform_and_cleaning_data()";
        String res;
        Integer logId = logService.getLogIdForToday();
        try {
            // Gọi stored procedure và lấy kết quả
            List<Map<String, Object>> results = jdbcTemplate.queryForList(procedureCall);

            for (Map<String, Object> record : results) {
                currentTime = LocalDateTime.now();
                if ("Không có record nào hết".equals(record.get("error"))) {
                    // Gửi email thông báo thất bại
                    String body = "<html>" +
                            "<body>" +
                            "<h2 style='color:red;'>Transform dữ liệu thất bại</h2>" +
                            "<p>Quy trình load file vào bảng tạm chưa được thực hiện, " +
                            "không thể thực hiện transform</p>" +
                            "<p>" + "Vào lúc " + currentTime + "</p>" +
                            "</body>" +
                            "</html>";

                    String from = environment.getProperty("spring.mail.username");
                    mailService.send(from, "phamnhuttan.9a6.2017@gmail.com",
                            "Transform thất bại", body);
                    return;
                }

                // Kiểm tra xem có trường nào null không
                if (isAnyFieldNull(record)) {
                    return; // Nếu có trường null, ngừng ngay lập tức
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

            }

            //27. Gửi mail tới người chịu trách nhiệm về việc bảo trì process với nội dung là Transform toàn bộ dữ liệu hoàn tất
            String body = "<html>" +
                    "<body>" +
                    "<h2 style='color:green;'>Transform thành công!</h2>" +
                    "<p>Transform toàn bộ dữ liệu hoàn tất</p>" +
                    "<p>" + "Vào lúc " + currentTime + "</p>" +
                    "</body>" +
                    "</html>";

            String from = environment.getProperty("spring.mail.username");
            mailService.send(from, "phamnhuttan.9a6.2017@gmail.com",
                    "Transform thành công!", body);

            logService.updateLogStatus(
                    logId,
                    "Transform thành công",
                    Status.SUCCESS_TRANSFORM
            );

            return;

        } catch (Exception e) {
            String body = "<html>" +
                    "<body>" +
                    "<h2 style='color:red;'>Transform thất bại!</h2>" +
                    "<p>Đã xảy ra lỗi trong quá trình transform vào staging db</p>" +
                    "<p>" + "Vào lúc " + currentTime + "</p>" +
                    "<p>" + e.getMessage() + "</p>" +
                    "</body>" +
                    "</html>";

            String from = environment.getProperty("spring.mail.username");
            mailService.send(from, "phamnhuttan.9a6.2017@gmail.com",
                    "Transform thất bại", body);

            if (logId != null) {
                logService.updateLogStatus(
                        logId,
                        "Transform thất bại",
                        Status.FAILURE_TRANSFORM
                );
            } else {
                currentTime = LocalDateTime.now();
                logService.insertLog(new Log(
                        0,
                        LogLevel.ERROR,
                        null,
                        0,
                        "Tranform",
                        currentTime,
                        "Transform thất bại",
                        "",
                        Status.FAILURE_TRANSFORM,
                        "ADMIN",
                        currentTime
                ));
            }
            e.printStackTrace();
            return;
        }
    }

    private boolean checkPriceValidate(String fieldName, String fieldValue) {
        if (fieldName.toLowerCase().contains("đ")) {
            // 25. Gửi mail tới người chịu trách nhiệm về việc bảo trì process với nội dung là có chứa ký tự không hợp lệ trong giá tiền
            String body = "<html>" +
                    "<body>" +
                    "<h2 style='color:red;'>Transform thất bại!</h2>" +
                    "<p>" + fieldValue + "có chứa ký dự không hợp lệ như đ </p>" +
                    "<p>" + "Vào lúc " + currentTime + "</p>" +
                    "</body>" +
                    "</html>";

            String from = environment.getProperty("spring.mail.username");
            mailService.send(from, "phamnhuttan.9a6.2017@gmail.com",
                    "Transform thất bại", body);

            //26. Ghi log về lỗi với nội dung là có chứa ký dự không hợp lệ như đ
            Integer logId = logService.getLogIdForToday();
            logService.updateLogStatus(
                    logId,
                    "Transform thất bại do" + fieldName + " có chứa ký dự không hợp lệ như đ",
                    Status.FAILURE_TRANSFORM
            );

            return true;
        }
        return false;
    }

    private boolean checkNegativeNumber(String fieldName, String fieldValue) {
        currentTime = LocalDateTime.now();
        double value = Double.parseDouble(fieldValue);
        if (value < 0.0) {
            String body = "<html>" +
                    "<body>" +
                    "<h2 style='color:red;'>Transform thất bại!</h2>" +
                    "<p> Transform thất bại do " + fieldName + " có chứa trường có giá trị âm</p>" +
                    "<p>" + "Vào lúc " + currentTime + "</p>" +
                    "</body>" +
                    "</html>";

            String from = environment.getProperty("spring.mail.username");
            mailService.send(from, "phamnhuttan.9a6.2017@gmail.com",
                    "Transform thất bại", body);

            Integer logId = logService.getLogIdForToday();
            logService.updateLogStatus(
                    logId,
                    "Transform thất bại do" + fieldName + " có chứa trường có giá trị âm",
                    Status.FAILURE_TRANSFORM
            );
            return true;
        }

        return false;
    }

    // Kiểm tra xem có bất kỳ trường nào có giá trị null không
    private boolean isAnyFieldNull(Map<String, Object> record) {
        for (String field : record.keySet()) {
            if (record.get(field) == null) {
                // Gửi email khi phát hiện trường null
                String body = "<html>" +
                        "<body>" +
                        "<h2 style='color:red;'>Transform thất bại!</h2>" +
                        "<p>Trường " + field + " có giá trị null</p>" +
                        "<p>" + "Vào lúc " + currentTime + "</p>" +
                        "</body>" +
                        "</html>";

                String from = environment.getProperty("spring.mail.username");
                mailService.send(from, "phamnhuttan.9a6.2017@gmail.com", "Transform thất bại", body);

                Integer logId = logService.getLogIdForToday();
                logService.updateLogStatus(
                        logId,
                        "Transform thất bại do" + field + " có giá trị null",
                        Status.FAILURE_TRANSFORM
                );

//                logService.insertLog(new Log(
//                        0,
//                        LogLevel.ERROR,
//                        null,
//                        0,
//                        "Tranform",
//                        currentTime,
//                        "Transform thất bại do" + field + " có giá trị null",
//                        "",
//                        Status.FAILURE_TRANSFORM,
//                        "ADMIN",
//                        currentTime
//                ));

                return true;
            }
        }
        return false;
    }
}
