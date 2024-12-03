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

            if (results.isEmpty()) {
                String from = environment.getProperty("spring.mail.username");
                String body = "<html>" +
                        "<body>" +
                        "<h2 style='color:red;'>Load dữ liệu vào bảng product staging không thành công!</h2>" +
                        "<p>Có lỗi trong quá trình transform dữ liệu hiện tại không có dữ liệu nào trong bảng tạm.</p>" +
                        "</body>" +
                        "</html>";
                mailService.send(from, "21130530@st.hcmuaf.edu.vn",
                        "Load data vào staging thất bại", body);
                return;
            }

            // Thu thập danh sách các id_config, xử lý linh hoạt kiểu dữ liệu
            Set<String> validIdConfigs = results.stream()
                    .map(row -> {
                        Object idConfigObj = row.get("id_config");
                        return idConfigObj != null ? idConfigObj.toString() : null; // Chuyển sang String
                    })
                    .filter(Objects::nonNull) // Bỏ qua các giá trị NULL
                    .collect(Collectors.toSet()); // Chuyển thành tập hợp để tránh trùng lặp

            for (String configId : validIdConfigs) {
                boolean hasValidData = false;

                for (Map<String, Object> row : results) {
                    String filePath = (String) row.get("file_path");
                    String fieldsTerminated = (String) row.get("fields_terminated");
                    String optionallyEnclosed = (String) row.get("optionally_enclosed");
                    String linesTerminated = (String) row.get("lines_terminated");
                    Integer ignoreRow = (Integer) row.get("ignore_row");
                    String stgFields = (String) row.get("stg_fields");
                    String status = (String) row.get("status");

                    // Lấy create_time
                    Object rawTimestamp = row.get("create_time");
                    LocalDateTime createTime = rawTimestamp instanceof Timestamp
                            ? ((Timestamp) rawTimestamp).toLocalDateTime()
                            : (LocalDateTime) rawTimestamp;

                    // Kiểm tra id_config
                    String rowIdConfig = row.get("id_config").toString();
                    if (!configId.equals(rowIdConfig)) {
                        continue; // Bỏ qua nếu không khớp id_config
                    }

                    if (filePath == null || status == null || !"SUCCESS_EXTRACT".equals(status.trim())) {
                        System.out.println("Bỏ qua id_config: " + configId + " vì trạng thái không hợp lệ hoặc thiếu filePath.");
                        continue;
                    }

                    hasValidData = true;

                    DataSource stagingDataSource = databaseService.connectToStagingDatabase(configId);
                    if (stagingDataSource == null) {
                        System.out.println("Không thể kết nối tới database staging với id_config: " + configId);
                        continue;
                    }
                    try (Connection connection = stagingDataSource.getConnection();
                         Statement stmt = connection.createStatement()) {

                        // Gửi email thông báo thành công
                        String from = environment.getProperty("spring.mail.username");
                        String body = "<html>" +
                                "<body>" +
                                "<h2 style='color:green;'>Load file CSV thành công!</h2>" +
                                "<p>Dữ liệu đã được tải thành công vào bảng staging của configId " + configId + ".</p>" +
                                "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!</p>" +
                                "</body>" +
                                "</html>";
                        mailService.send(from, "tuhoangnguyen2003@gmail.com",
                                "Load data vào bảng " + " thành công", body);

                        // Ghi log thành công
                        logService.insertLog(new Log(
                                Integer.parseInt(configId),
                                LogLevel.INFO,
                                filePath,
                                1,
                                "Load data",
                                currentTime,
                                "Transform vào bảng product_staging thành công",
                                "",
                                Status.SUCCESS_LOAD_DATA,
                                "ADMIN",
                                currentTime
                        ));
                    }catch (Exception e){
                        // Gửi email thông báo lỗi
                        String from = environment.getProperty("spring.mail.username");
                        String body = "<html>" +
                                "<body>" +
                                "<h2 style='color:red;'>Load file CSV thất bại!</h2>" +
                                "<p>Đã xảy ra lỗi trong quá trình tải dữ liệu:</p>" +
                                "<p>" + e.getMessage() + "</p>" +
                                "</body>" +
                                "</html>";
                        mailService.send(from, "21130530@st.hcmuaf.edu.vn",
                                "Transform vào bảng product_staging thất bại", body);

                        // Ghi log thất bại
                        logService.insertLog(new Log(
                                Integer.parseInt(configId),
                                LogLevel.ERROR,
                                filePath,
                                0,
                                "Load data",
                                currentTime,
                                "Transform vào bảng product_staging thất bại",
                                e.getMessage(),
                                Status.FAILURE_LOAD_DATA,
                                "ADMIN",
                                currentTime
                        ));
                    }
                }
            }

        } catch (Exception e) {
            String from = environment.getProperty("spring.mail.username");
            String body = "<html>" +
                    "<body>" +
                    "<h2 style='color:red;'>Load data vào staging thất bại!</h2>" +
                    "<p>Đã xảy ra lỗi trong quá trình load dữ liệu vào staging</p>" +
                    "<p>" + e.getMessage() + "</p>" +
                    "</body>" +
                    "</html>";
            mailService.send(from, "tuhoangnguyen2003@gmail.com",
                    "Load data vào database staging thất bại", body);
            e.printStackTrace();
        }
    }
}
