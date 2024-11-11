package com.demo.services;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.sql.DataSource;

import com.demo.entities.Log;
import com.demo.entities.LogLevel;
import com.demo.entities.Status;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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

    public void loadCSVToStaging(String configId) {
        LocalDateTime currentTime = LocalDateTime.now();
        try {
            // Gọi stored procedure để lấy thông tin cấu hình với các OUT parameters
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("get_load_config_data_3");
            query.registerStoredProcedureParameter("file_path", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("fields_terminated", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("optionally_enclosed", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("lines_terminated", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("ignore_row", Integer.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("stg_fields", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("log_id", Integer.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("table_staging", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("status", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("create_time", LocalDateTime.class, ParameterMode.OUT);


            query.execute();



            // Lấy giá trị từ OUT parameters của stored procedure
            String filePath = (String) query.getOutputParameterValue("file_path");
            String fieldsTerminated = (String) query.getOutputParameterValue("fields_terminated");
            String optionallyEnclosed = (String) query.getOutputParameterValue("optionally_enclosed");
            String linesTerminated = (String) query.getOutputParameterValue("lines_terminated");
            Integer ignoreRow = (Integer) query.getOutputParameterValue("ignore_row");
            String stgFields = (String) query.getOutputParameterValue("stg_fields");
            Integer logId = (Integer) query.getOutputParameterValue("log_id");
            String tableStaging = (String) query.getOutputParameterValue("table_staging");
            String status = (String) query.getOutputParameterValue("status");
            LocalDateTime createTime = (LocalDateTime) query.getOutputParameterValue("create_time");

            // Kiểm tra nếu tất cả các giá trị đều là NULL thì dừng chương trình
            if (filePath == null && fieldsTerminated == null && optionallyEnclosed == null && linesTerminated == null &&
                    ignoreRow == null && stgFields == null && logId == null && tableStaging == null &&
                    status == null && createTime == null) {
                System.out.println("Dừng project vì tất cả các trường đều NULL.");
                return;
            }

            filePath = filePath.replace("\\", "/");

            if (!fieldsTerminated.equals(",") && !fieldsTerminated.equals("\t")) {
                fieldsTerminated = ","; // Gán giá trị mặc định nếu giá trị không hợp lệ
            }


            if (!optionallyEnclosed.equals("\"") && !optionallyEnclosed.equals("'")) {
                optionallyEnclosed = "\""; // Gán giá trị mặc định nếu giá trị không hợp lệ
            }


            if (!linesTerminated.equals("\n") && !linesTerminated.equals("\r\n")) {
                linesTerminated = "\n"; // Gán giá trị mặc định nếu giá trị không hợp lệ
            }

            if (ignoreRow == null || ignoreRow < 0) {
                ignoreRow = 0; // Đảm bảo rằng giá trị ignore_row không bỏ qua nhiều dòng hơn cần thiết
            }


            System.out.println("File Path: " + filePath);
            System.out.println("Fields Terminated By: " + fieldsTerminated);
            System.out.println("Optionally Enclosed By: " + optionallyEnclosed);
            System.out.println("Lines Terminated By: " + linesTerminated);
            System.out.println("Ignore Rows: " + ignoreRow);
            System.out.println("Staging Fields: " + stgFields);
            System.out.println("Table Staging: " + tableStaging);


            // Kiểm tra điều kiện dừng nếu create_time không phải ngày hiện tại hoặc status không là SUCCESS_EXTRACT
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            System.out.println("Status: " + status);
            System.out.println("Create time: " + formatter);

            if (!"SUCCESS_EXTRACT".equals(status)) {
                System.out.println("Dừng project do trạng thái không phù hợp hoặc create_time không phải ngày hiện tại.");
                return;
            }

            // Kết nối tới db staging và thực hiện TRUNCATE và LOAD DATA INFILE
            DataSource stagingDataSource = databaseService.connectToStagingDatabase(configId);

            String truncateSql = "TRUNCATE TABLE " + tableStaging;
            System.out.println("Bảng " + tableStaging + " đã được truncate.");
            String loadSql = "LOAD DATA INFILE '" + filePath + "' " +
                    "INTO TABLE " + tableStaging + " " +
                    "FIELDS TERMINATED BY '" + fieldsTerminated + "' " +
                    "OPTIONALLY ENCLOSED BY '" + optionallyEnclosed + "' " +
                    "LINES TERMINATED BY '" + linesTerminated + "' " +
                    "IGNORE " + ignoreRow + " ROWS " +
                    "(" + stgFields + ")";

            try (Connection connection = stagingDataSource.getConnection();
                 Statement stmt = connection.createStatement()) {
                // Thực hiện TRUNCATE
                stmt.execute(truncateSql);
                System.out.println("Bảng " + tableStaging + " đã được truncate.");

                // Thực hiện LOAD DATA INFILE
                stmt.execute(loadSql);
                System.out.println("Dữ liệu đã được load thành công vào bảng " + tableStaging);
                String from = environment.getProperty("spring.mail.username");
                String body = "<html>" +
                        "<body>" +
                        "<h2 style='color:green;'>Load file csv thành công!</h2>" +
                        "<p>Chúng tôi đã lưu trữ dữ liệu sản phẩm thành công vào database staging</p>" +

                        "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!</p>" +
                        "</body>" +
                        "</html>";
                mailService.send(from, "tuhoangnguyen2003@gmail.com", "Load data vào staging thành công",body);
                logService.insertLog(new Log(Integer.parseInt(configId),LogLevel.INFO, filePath, 1 ,"Load data", currentTime,"Load data vào database staging thành công" , "",Status.SUCCESS_EXTRACT,"ADMIN",currentTime));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logService.insertLog(new Log(Integer.parseInt(configId),LogLevel.ERROR, null, 0 ,"Load data", currentTime,"Load data vào database staging thất bại" , "",Status.FAILURE_EXTRACT,"ADMIN",currentTime));

        }
    }
}
