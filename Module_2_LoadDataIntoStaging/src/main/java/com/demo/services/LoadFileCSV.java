package com.demo.services;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javax.sql.DataSource;

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

//    public void loadCSVToStaging(String configId) {
//        LocalDateTime currentTime = LocalDateTime.now();
//        try {
//            // Gọi stored procedure để lấy thông tin cấu hình với các OUT parameters
//            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("get_load_config_data_multiple");
//            query.registerStoredProcedureParameter("file_path", String.class, ParameterMode.OUT);
//            query.registerStoredProcedureParameter("fields_terminated", String.class, ParameterMode.OUT);
//            query.registerStoredProcedureParameter("optionally_enclosed", String.class, ParameterMode.OUT);
//            query.registerStoredProcedureParameter("lines_terminated", String.class, ParameterMode.OUT);
//            query.registerStoredProcedureParameter("ignore_row", Integer.class, ParameterMode.OUT);
//            query.registerStoredProcedureParameter("stg_fields", String.class, ParameterMode.OUT);
//            query.registerStoredProcedureParameter("log_id", Integer.class, ParameterMode.OUT);
//            query.registerStoredProcedureParameter("table_staging", String.class, ParameterMode.OUT);
//            query.registerStoredProcedureParameter("status", String.class, ParameterMode.OUT);
//            query.registerStoredProcedureParameter("create_time", LocalDateTime.class, ParameterMode.OUT);
//
//
//            query.execute();
//
//
//
//            // Lấy giá trị từ OUT parameters của stored procedure
//            String filePath = (String) query.getOutputParameterValue("file_path");
//            String fieldsTerminated = (String) query.getOutputParameterValue("fields_terminated");
//            String optionallyEnclosed = (String) query.getOutputParameterValue("optionally_enclosed");
//            String linesTerminated = (String) query.getOutputParameterValue("lines_terminated");
//            Integer ignoreRow = (Integer) query.getOutputParameterValue("ignore_row");
//            String stgFields = (String) query.getOutputParameterValue("stg_fields");
//            Integer logId = (Integer) query.getOutputParameterValue("log_id");
//            String tableStaging = (String) query.getOutputParameterValue("table_staging");
//            String status = (String) query.getOutputParameterValue("status");
//            LocalDateTime createTime = (LocalDateTime) query.getOutputParameterValue("create_time");
//
//            // Kiểm tra nếu tất cả các giá trị đều là NULL thì dừng chương trình
//            if (filePath == null && fieldsTerminated == null && optionallyEnclosed == null && linesTerminated == null &&
//                    ignoreRow == null && stgFields == null && logId == null && tableStaging == null &&
//                    status == null && createTime == null) {
//                System.out.println("Dừng project vì tất cả các trường đều NULL.");
//                return;
//            }
//
//            filePath = filePath.replace("\\", "/");
//
//            if (!fieldsTerminated.equals(",") && !fieldsTerminated.equals("\t")) {
//                fieldsTerminated = ","; // Gán giá trị mặc định nếu giá trị không hợp lệ
//            }
//
//
//            if (!optionallyEnclosed.equals("\"") && !optionallyEnclosed.equals("'")) {
//                optionallyEnclosed = "\""; // Gán giá trị mặc định nếu giá trị không hợp lệ
//            }
//
//
//            if (!linesTerminated.equals("\n") && !linesTerminated.equals("\r\n")) {
//                linesTerminated = "\n"; // Gán giá trị mặc định nếu giá trị không hợp lệ
//            }
//
//            if (ignoreRow == null || ignoreRow < 0) {
//                ignoreRow = 0; // Đảm bảo rằng giá trị ignore_row không bỏ qua nhiều dòng hơn cần thiết
//            }
//
//
//            System.out.println("File Path: " + filePath);
//            System.out.println("Fields Terminated By: " + fieldsTerminated);
//            System.out.println("Optionally Enclosed By: " + optionallyEnclosed);
//            System.out.println("Lines Terminated By: " + linesTerminated);
//            System.out.println("Ignore Rows: " + ignoreRow);
//            System.out.println("Staging Fields: " + stgFields);
//            System.out.println("Table Staging: " + tableStaging);
//
//
//            // Kiểm tra điều kiện dừng nếu create_time không phải ngày hiện tại hoặc status không là SUCCESS_EXTRACT
//            LocalDate currentDate = LocalDate.now();
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//            System.out.println("Status: " + status);
//            System.out.println("Create time: " + formatter);
//
//            if (!"SUCCESS_EXTRACT".equals(status)) {
//                System.out.println("Dừng project do trạng thái không phù hợp hoặc create_time không phải ngày hiện tại.");
//                return;
//            }
//
//            // Kết nối tới db staging và thực hiện TRUNCATE và LOAD DATA INFILE
//            DataSource stagingDataSource = databaseService.connectToStagingDatabase(configId);
//
//            String truncateSql = "TRUNCATE TABLE " + tableStaging;
//            System.out.println("Bảng " + tableStaging + " đã được truncate.");
//            String loadSql = "LOAD DATA INFILE '" + filePath + "' " +
//                    "INTO TABLE " + tableStaging + " " +
//                    "FIELDS TERMINATED BY '" + fieldsTerminated + "' " +
//                    "OPTIONALLY ENCLOSED BY '" + optionallyEnclosed + "' " +
//                    "LINES TERMINATED BY '" + linesTerminated + "' " +
//                    "IGNORE " + ignoreRow + " ROWS " +
//                    "(" + stgFields + ")";
//
//            try (Connection connection = stagingDataSource.getConnection();
//                 Statement stmt = connection.createStatement()) {
//                // Thực hiện TRUNCATE
//                stmt.execute(truncateSql);
//                System.out.println("Bảng " + tableStaging + " đã được truncate.");
//
//                // Thực hiện LOAD DATA INFILE
//                stmt.execute(loadSql);
//                System.out.println("Dữ liệu đã được load thành công vào bảng " + tableStaging);
//                String from = environment.getProperty("spring.mail.username");
//                String body = "<html>" +
//                        "<body>" +
//                        "<h2 style='color:green;'>Load file csv thành công!</h2>" +
//                        "<p>Chúng tôi đã lưu trữ dữ liệu sản phẩm thành công vào database staging</p>" +
//
//                        "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!</p>" +
//                        "</body>" +
//                        "</html>";
//                mailService.send(from, "tuhoangnguyen2003@gmail.com", "Load data vào staging thành công",body);
//                logService.insertLog(new Log(Integer.parseInt(configId),LogLevel.INFO, filePath, 1 ,"Load data", currentTime,"Load data vào database staging thành công" , "",Status.SUCCESS_EXTRACT,"ADMIN",currentTime));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            logService.insertLog(new Log(Integer.parseInt(configId),LogLevel.ERROR, null, 0 ,"Load data", currentTime,"Load data vào database staging thất bại" , "",Status.FAILURE_EXTRACT,"ADMIN",currentTime));
//
//        }
//    }


    public void loadCSVToStaging() {
        LocalDateTime currentTime = LocalDateTime.now();
<<<<<<< HEAD
        String procedureCall = "CALL get_load_config_data_mul()";
=======
        String procedureCall = "CALL get_load_config_data_multiple()";
>>>>>>> e91568f16d12a9d7a9bc43eb775060ed9384783e

        try {
            // Lấy danh sách kết quả từ stored procedure
            List<Map<String, Object>> resultList = jdbcTemplate.queryForList(procedureCall);

            if (resultList.isEmpty()) {
<<<<<<< HEAD
                String from = environment.getProperty("spring.mail.username");
                String body = "<html>" +
                        "<body>" +
                        "<h2 style='color:red;'>Load dữ liệu vào Staging không thành công!</h2>" +
                        "<p>Đã xảy ra lỗi trong quá trình tải dữ liệu do không có dữ liệu nào ngày hôm nay.</p>" +
                        "</body>" +
                        "</html>";
                mailService.send(from, "tuhoangnguyen2003@gmail.com",
                        "Load data vào stagng thất bại", body);
//                System.out.println("Không có dữ liệu nào để load data vào staging.");
=======
                System.out.println("Không có dữ liệu nào trong bảng config và logs.");
>>>>>>> e91568f16d12a9d7a9bc43eb775060ed9384783e
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

<<<<<<< HEAD
            System.out.println(validIdConfigs);
=======
>>>>>>> e91568f16d12a9d7a9bc43eb775060ed9384783e

            for (String configId : validIdConfigs) {
                boolean hasValidData = false;

                for (Map<String, Object> row : resultList) {
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
                    String loadSql = "LOAD DATA INFILE '" + filePath + "' " +
                            "INTO TABLE " + tableStaging + " " +
                            "FIELDS TERMINATED BY '" + fieldsTerminated + "' " +
                            "OPTIONALLY ENCLOSED BY '" + optionallyEnclosed + "' " +
                            "LINES TERMINATED BY '" + linesTerminated + "' " +
                            "IGNORE " + ignoreRow + " ROWS " +
                            "(" + stgFields + ")";

                    try (Connection connection = stagingDataSource.getConnection();
                         Statement stmt = connection.createStatement()) {
                        stmt.execute(truncateSql);
                        stmt.execute(loadSql);

                        // Gửi email thông báo thành công
                        String from = environment.getProperty("spring.mail.username");
                        String body = "<html>" +
                                "<body>" +
<<<<<<< HEAD
                                "<h2 style='color:green;'>Load dữ liệu vào staging thành công!</h2>" +
=======
                                "<h2 style='color:green;'>Load file CSV thành công!</h2>" +
>>>>>>> e91568f16d12a9d7a9bc43eb775060ed9384783e
                                "<p>File: " + filePath + "</p>" +
                                "<p>Bảng: " + tableStaging + "</p>" +
                                "<p>Dữ liệu đã được tải thành công vào bảng staging của configId " + configId + ".</p>" +
                                "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!</p>" +
                                "</body>" +
                                "</html>";
                        mailService.send(from, "tuhoangnguyen2003@gmail.com",
                                "Load data vào bảng " + tableStaging + " thành công", body);


                        // Ghi log thành công
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
                    }
                }

                if (!hasValidData) {
<<<<<<< HEAD
                    String from = environment.getProperty("spring.mail.username");
                    String body = "<html>" +
                            "<body>" +
                            "<h2 style='color:red;'>Load dữ liệu vào Staging không thành công!</h2>" +
                            "<p>Đã xảy ra lỗi trong quá trình tải dữ liệu do dữ liệu không hợp lệ.</p>" +
                            "</body>" +
                            "</html>";
                    mailService.send(from, "tuhoangnguyen2003@gmail.com",
                            "Load data vào stagng thất bại", body);
//                    System.out.println("Không có dữ liệu hợp lệ cho id_config: " + configId);
                }
            }
        } catch (Exception e) {
            String from = environment.getProperty("spring.mail.username");
            String body = "<html>" +
                    "<body>" +
                    "<h2 style='color:red;'>Load dữ liệu vào Staging không thành công!</h2>" +
                    "<p>Đã xảy ra lỗi trong quá trình tải dữ liệu do lỗi không mong muốn.</p>" +
                    "</body>" +
                    "</html>";
            mailService.send(from, "tuhoangnguyen2003@gmail.com",
                    "Load data vào stagng thất bại", body);
=======
                    System.out.println("Không có dữ liệu hợp lệ cho id_config: " + configId);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi không mong muốn: " + e.getMessage());
>>>>>>> e91568f16d12a9d7a9bc43eb775060ed9384783e
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




}
