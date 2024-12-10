package com.demo.services;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import com.demo.entities.Config;
import com.demo.entities.Log;
import com.demo.entities.LogLevel;
import com.demo.entities.Status;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LoadFileCSV {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private MailService mailService;

    @Autowired
    private Environment environment;

    @Autowired
    private LogService logService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductService productService;


    public void loadCSVToStaging() {
        LocalDateTime currentTime = LocalDateTime.now();
        // Gọi procedure từ database để tiến hành load data
        String procedureCall = "CALL get_load_config_data_mul()";
        List<Log> todayLogExists = logService.findByStatusAndCreatedTimeToday();
        System.out.println(todayLogExists);
        try {
            // Lấy danh sách kết quả từ stored procedure
            List<Map<String, Object>> resultList = jdbcTemplate.queryForList(procedureCall);

            if (resultList.isEmpty()) {
                String from = environment.getProperty("spring.mail.username");
                String body = "<html>" +
                        "<body>" +
                        "<h2 style='color:red;'>Load dữ liệu vào Staging không thành công!</h2>" +
                        "<p>Đã xảy ra lỗi trong quá trình tải dữ liệu do không có dữ liệu nào ngày hôm nay.</p>" +
                        "</body>" +
                        "</html>";
                mailService.send(from, "tuhoangnguyen2003@gmail.com",
                        "Load data vào stagng thất bại", body);
                if(todayLogExists.isEmpty()) {
                    logService.insertLog(new Log(
                            1,
                            LogLevel.ERROR,
                            null,
                            0,
                            "Load data",
                            currentTime,
                            "Load data vào staging_tiki_1 thất bại",
                            null,
                            Status.FAILURE_LOAD_DATA,
                            "ADMIN",
                            currentTime
                    ));
                    logService.insertLog(new Log(
                            2,
                            LogLevel.ERROR,
                            null,
                            0,
                            "Load data",
                            currentTime,
                            "Load data vào staging_tiki_2 thất bại",
                            null,
                            Status.FAILURE_LOAD_DATA,
                            "ADMIN",
                            currentTime
                    ));
                } else {
                    for(Log log : todayLogExists) {
                        log.setUpdateTime(LocalDateTime.now());
                        logService.insertLog(log);
                    }
                 }

                return;
            }

            // Thu thập danh sách các id_config, xử lý linh hoạt kiểu dữ liệu
            Set<String> validIdConfigs = resultList.stream()
                    .map(row -> {
                        Object idConfigObj = row.get("id_config");
                        return idConfigObj != null ? idConfigObj.toString() : null; // Chuyển sang String
                    })
                    .filter(Objects::nonNull) // Bỏ qua các giá trị NULL
                    .collect(Collectors.toSet()); // Chuyển thành tập hợp để tránh trùng lặp


            for (String configId : validIdConfigs) {
                boolean hasValidData = false;

                for (Map<String, Object> row : resultList) {
                    // 6. Tiến hành lấy các thông tin từ procedure đã viết ở database control
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

                    // Xử lý và tải dữ liệu vào bảng staging
                    // 7. Kiểm tra các thông tin đó có hợp lệ không
                    String tableStaging = "staging_tiki_" + configId;
                    filePath = filePath.replace("\\", "/");
                    fieldsTerminated = validateFieldsTerminated(fieldsTerminated);
                    optionallyEnclosed = validateOptionallyEnclosed(optionallyEnclosed);
                    linesTerminated = validateLinesTerminated(linesTerminated);
                    ignoreRow = validateIgnoreRow(ignoreRow);

                    DataSource stagingDataSource = databaseService.connectToStagingDatabase(configId);
                    if (stagingDataSource == null) {
                        System.out.println("Không thể kết nối tới database staging với id_config: " + configId);
                        continue;
                    }

                    String truncateSql = "TRUNCATE TABLE " + tableStaging;
                    // 11. Từ các trường thông tin lấy được từ procedure để chuẩn bị load dữ liệu vào database staging
                    String loadSql = "LOAD DATA INFILE '" + filePath + "' " +
                            "INTO TABLE " + tableStaging + " " +
                            "FIELDS TERMINATED BY '" + fieldsTerminated + "' " +
                            "OPTIONALLY ENCLOSED BY '" + optionallyEnclosed + "' " +
                            "LINES TERMINATED BY '" + linesTerminated + "' " +
                            "IGNORE " + ignoreRow + " ROWS " +
                            "(" + stgFields + ")";

                    try (Connection connection = stagingDataSource.getConnection();
                         Statement stmt = connection.createStatement()) {
                        // 8. Kiểm tra và xóa dữ liệu cũ trong database Staging
                        if(checkOldData(tableStaging)) {
                            stmt.execute(truncateSql);
                            System.out.println("Bảng 'product' đã được xóa dữ liệu thành công!");
                        }
                        // 12. Chuẩn bị câu lệnh LOAD DATA INFILE để thêm dữ liệu vào bảng product
                        stmt.execute(loadSql);

                        // 13.  Gửi email thông báo thành công
                        String from = environment.getProperty("spring.mail.username");
                        String body = "<html>" +
                                "<body>" +
                                "<h2 style='color:green;'>Load file CSV thành công!</h2>" +
                                "<p>File: " + filePath + "</p>" +
                                "<p>Bảng: " + tableStaging + "</p>" +
                                "<p>Dữ liệu đã được tải thành công vào bảng staging của configId " + configId + ".</p>" +
                                "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!</p>" +
                                "</body>" +
                                "</html>";
                        mailService.send(from, "tuhoangnguyen2003@gmail.com",
                                "Load data vào bảng " + tableStaging + " thành công", body);

                        if(todayLogExists.isEmpty()) {
                            // 14. Ghi log thành công
                            logService.insertLog(new Log(
                                    Integer.parseInt(configId),
                                    LogLevel.INFO,
                                    filePath,
                                    1,
                                    "Load data",
                                    currentTime,
                                    "Load data vào bảng " + tableStaging + " thành công",
                                    "",
                                    Status.SUCCESS_LOAD_DATA,
                                    "ADMIN",
                                    currentTime
                            ));
                        } else {
                            for(Log log : todayLogExists) {
                                log.setUpdateTime(LocalDateTime.now());
                                logService.insertLog(log);
                            }
                        }

                    } catch (Exception e) {
                        // Gửi email thông báo lỗi
                        String from = environment.getProperty("spring.mail.username");
                        String body = "<html>" +
                                "<body>" +
                                "<h2 style='color:red;'>Load file CSV thất bại!</h2>" +
                                "<p>File: " + filePath + "</p>" +
                                "<p>Bảng: " + tableStaging + "</p>" +
                                "<p>Đã xảy ra lỗi trong quá trình tải dữ liệu:</p>" +
                                "<p>" + e.getMessage() + "</p>" +
                                "</body>" +
                                "</html>";
                        mailService.send(from, "tuhoangnguyen2003@gmail.com",
                                "Load data vào bảng " + tableStaging + " thất bại", body);
                        if(todayLogExists.isEmpty()) {
                            // Ghi log thất bại
                            logService.insertLog(new Log(
                                    Integer.parseInt(configId),
                                    LogLevel.ERROR,
                                    filePath,
                                    0,
                                    "Load data",
                                    currentTime,
                                    "Load data vào bảng " + tableStaging + " thất bại",
                                    e.getMessage(),
                                    Status.FAILURE_LOAD_DATA,
                                    "ADMIN",
                                    currentTime
                            ));
                        } else {
                            for(Log log : todayLogExists) {
                                log.setUpdateTime(LocalDateTime.now());
                                logService.insertLog(log);
                            }
                        }

                    }
                }

                if (!hasValidData) {
                    System.out.println("Không có dữ liệu hợp lệ cho id_config: " + configId);
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


    // Các hàm hỗ trợ validate
    private String validateFieldsTerminated(String fieldsTerminated) {
        return (fieldsTerminated != null && (fieldsTerminated.equals(",") || fieldsTerminated.equals("\t"))) ? fieldsTerminated : ",";
    }

    private String validateOptionallyEnclosed(String optionallyEnclosed) {
        return (optionallyEnclosed != null && (optionallyEnclosed.equals("\"") || optionallyEnclosed.equals("'"))) ? optionallyEnclosed : "\"";
    }

    private String validateLinesTerminated(String linesTerminated) {
        return (linesTerminated != null && (linesTerminated.equals("\n") || linesTerminated.equals("\r\n"))) ? linesTerminated : "\n";
    }

    private int validateIgnoreRow(Integer ignoreRow) {
        return (ignoreRow != null && ignoreRow >= 0) ? ignoreRow : 0;
    }


    //  8. Kiểm tra và xóa dữ liệu cũ trong database Staging
    private boolean checkOldData(String tableStaging) {
        // Kết nối đến database 'staging'
        DataSource stagingDataSource = databaseService.connectToStagingDatabase("1");
        System.out.println(tableStaging);
        // Lấy ngày hiện tại
        LocalDateTime today = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String todayString = today.format(formatter);

        // Truy vấn để kiểm tra xem có bản ghi nào với ngày hôm nay không
        String sqlCheckDate = "SELECT COUNT(*) FROM " + tableStaging +
                " WHERE DATE_FORMAT(STR_TO_DATE(created_at, '%d/%m/%Y'), '%d/%m/%Y') = ?";

        try (Connection connection = stagingDataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sqlCheckDate)) {

            pstmt.setString(1, todayString);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                if (count == 0) {
                    // Nếu không có bản ghi nào với ngày hôm nay, xóa toàn bộ dữ liệu
                    System.out.println("Không có dữ liệu của ngày hôm nay. Đang thực hiện TRUNCATE bảng 'product'...");
                    return true;
                } else {
                    System.out.println("Có dữ liệu của ngày hôm nay. Không cần xóa dữ liệu.");
                    return false;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Xử lý lỗi
        }
        return false;
    }


}
