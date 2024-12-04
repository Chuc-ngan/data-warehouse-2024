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

    @Override
    public void TransformData() {
        LocalDateTime currentTime = LocalDateTime.now();
        String procedureCall = "CALL transform_and_cleaning_data()";
        try {
            // Gọi stored procedure và lấy kết quả
            List<Map<String, Object>> results = jdbcTemplate.queryForList(procedureCall);

            for (Map<String, Object> record : results){
                if ("Không có record nào hết".equals(record.get("error"))) {
                    // Gửi email thông báo thành công
                    String from = environment.getProperty("spring.mail.username");
                    String body = "<html>" +
                            "<body>" +
                            "<h2 style='color:red;'>Transform dữ liệu thất bại</h2>" +
                            "<p>Vui lòng kiểm tra lại</p>" +
                            "</body>" +
                            "</html>";
                    mailService.send(from, "phamnhuttan.9a6.2017@gmail.com",
                            "Transform thất bại", body);

                    // Ghi log nếu bị lỗi
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
                    return;
                }
                String productId= record.get("product_id").toString();
                System.out.println(productId);
            }
        } catch (Exception e) {
//            String from = environment.getProperty("spring.mail.username");
//            String body = "<html>" +
//                    "<body>" +
//                    "<h2 style='color:red;'>Load data vào staging thất bại!</h2>" +
//                    "<p>Đã xảy ra lỗi trong quá trình load dữ liệu vào staging</p>" +
//                    "<p>" + e.getMessage() + "</p>" +
//                    "</body>" +
//                    "</html>";
//            mailService.send(from, "phamnhuttan.9a6.2017@gmail.com",
//                    "Load data vào database staging thất bại", body);
            e.printStackTrace();
        }
    }
}
