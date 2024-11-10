package com.demo.services;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoadFileCSV {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DatabaseService databaseService;

    public void loadCSVToStaging(String configId) {
        try {
            // Gọi stored procedure để lấy thông tin cấu hình với các OUT parameters
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("get_load_config_data");
            query.registerStoredProcedureParameter("file_path", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("fields_terminated", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("optionally_enclosed", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("lines_terminated", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("ignore_row", Integer.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("stg_fields", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("log_id", Integer.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("table_staging", String.class, ParameterMode.OUT);

            query.execute();

            // Lấy giá trị từ OUT parameters của stored procedure
            String filePath = (String) query.getOutputParameterValue("file_path");
            filePath = filePath.replace("\\", "/");
            String fieldsTerminated = (String) query.getOutputParameterValue("fields_terminated");
            if (!fieldsTerminated.equals(",") && !fieldsTerminated.equals("\t")) {
                fieldsTerminated = ","; // Gán giá trị mặc định nếu giá trị không hợp lệ
            }

            String optionallyEnclosed = (String) query.getOutputParameterValue("optionally_enclosed");
            if (!optionallyEnclosed.equals("\"") && !optionallyEnclosed.equals("'")) {
                optionallyEnclosed = "\""; // Gán giá trị mặc định nếu giá trị không hợp lệ
            }

            String linesTerminated = (String) query.getOutputParameterValue("lines_terminated");
            if (!linesTerminated.equals("\n") && !linesTerminated.equals("\r\n")) {
                linesTerminated = "\n"; // Gán giá trị mặc định nếu giá trị không hợp lệ
            }
            Integer ignoreRow = (Integer) query.getOutputParameterValue("ignore_row");
            if (ignoreRow == null || ignoreRow < 0) {
                ignoreRow = 0; // Đảm bảo rằng giá trị ignore_row không bỏ qua nhiều dòng hơn cần thiết
            }

            String stgFields = (String) query.getOutputParameterValue("stg_fields");
            Integer logId = (Integer) query.getOutputParameterValue("log_id");
            String tableStaging = (String) query.getOutputParameterValue("table_staging");

            System.out.println("File Path: " + filePath);
            System.out.println("Fields Terminated By: " + fieldsTerminated);
            System.out.println("Optionally Enclosed By: " + optionallyEnclosed);
            System.out.println("Lines Terminated By: " + linesTerminated);
            System.out.println("Ignore Rows: " + ignoreRow);
            System.out.println("Staging Fields: " + stgFields);
            System.out.println("Table Staging: " + tableStaging);

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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
