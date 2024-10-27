package com.demo.services;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ProductServiceImpl implements  ProductService {

    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private JdbcTemplate jdbcTemplate; // Kết nối tới database control

    @Autowired
    private MailService mailService;

    @Autowired
    private Environment environment;

    @Override
    public void createTable(DataSource stagingDataSource) {
        // 1. Lấy giá trị từ cột 'columns' trong bảng config của database 'control'
        String sqlSelectColumns = "SELECT columns FROM config LIMIT 1";

        String columnsString;
        try {
            columnsString = jdbcTemplate.queryForObject(sqlSelectColumns, String.class);
            if (columnsString == null || columnsString.isEmpty()) {
                System.out.println("Không tìm thấy giá trị trong cột 'columns'");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // 2. Tạo bảng trong database 'staging' với cấu trúc lấy từ 'control'
        try (Connection connection = stagingDataSource.getConnection();
             Statement createStmt = connection.createStatement()) {
            String sqlCreateTable = "CREATE TABLE IF NOT EXISTS product (" + columnsString + ")";
            createStmt.execute(sqlCreateTable);
            System.out.println("Bảng 'product' đã được tạo thành công trong database 'staging'!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void checkOldData() {
        // Kết nối đến database 'staging'
        DataSource stagingDataSource = databaseService.connectToStagingDatabase();

        // Lấy ngày hiện tại
        LocalDateTime today = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String todayString = today.format(formatter);

        // Truy vấn để kiểm tra xem có bản ghi nào với ngày hôm nay không
        String sqlCheckDate = "SELECT COUNT(*) FROM product WHERE DATE_FORMAT(STR_TO_DATE(date, '%d/%m/%Y'), '%d/%m/%Y') = ?";

        try (Connection connection = stagingDataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sqlCheckDate)) {

            pstmt.setString(1, todayString);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                if (count == 0) {
                    // Nếu không có bản ghi nào với ngày hôm nay, xóa toàn bộ dữ liệu
                    System.out.println("Không có dữ liệu của ngày hôm nay. Đang thực hiện TRUNCATE bảng 'product'...");
                    String sqlTruncateTable = "TRUNCATE TABLE product";
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(sqlTruncateTable);
                        System.out.println("Bảng 'product' đã được xóa dữ liệu thành công!");
                    }
                } else {
                    System.out.println("Có dữ liệu của ngày hôm nay. Không cần xóa dữ liệu.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Xử lý lỗi
        }
    }

    @Override
    public void importCSV() {

        boolean isSuccess = false;
        // Lấy đường dẫn file từ bảng config trong database control
        String filePath = getFilePathFromConfig();
        if (filePath == null) {
            System.out.println("Không thể lấy đường dẫn file từ bảng config.");
            insertLog(isSuccess, filePath);
            return;
        }

        // Kiểm tra điều kiện từ bảng log trong database control
        if (!checkLogStatusAndDate()) {
            System.out.println("Không tìm thấy bản ghi log phù hợp. Dừng quy trình nhập CSV.");
            insertLog(isSuccess, filePath);
            return;
        }

        // Kết nối đến database dựa trên thông tin từ control
        DataSource dataSource = databaseService.connectToStagingDatabase();



        // Cấu hình parser để xử lý dấu phẩy và dấu ngoặc kép
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withQuoteChar('"')
                .build();


        try (CSVReader csvReader = new CSVReaderBuilder(new FileReader(filePath))
                .withCSVParser(parser)
                .build();
             Connection connection = dataSource.getConnection()) {

            // Đọc toàn bộ dữ liệu từ file CSV
            List<String[]> allData = csvReader.readAll();

            // Bỏ qua dòng tiêu đề nếu có
            if (!allData.isEmpty()) {
                allData.remove(0);
            }

            // Câu lệnh tạo bảng nếu chưa tồn tại
//            String sqlCreateTable = "CREATE TABLE IF NOT EXISTS product (" +
//                    "id VARCHAR(250) PRIMARY KEY," +
//                    "sku VARCHAR(250)," +
//                    "name VARCHAR(250)," +
//                    "price FLOAT," +
//                    "original_price FLOAT," +
//                    "brand_name VARCHAR(250)," +
//                    "discount FLOAT," +
//                    "thumbnail_url TEXT," +
//                    "short_description TEXT," +
//                    "images TEXT," +
//                    "colors TEXT," +
//                    "sizes TEXT," +
//                    "rating_average DOUBLE," +
//                    "review_count INT," +
//                    "discount_rate DOUBLE," +
//                    "quantity_sold INT," +
//                    "url_key VARCHAR(250)," +
//                    "url_path TEXT," +
//                    "short_url TEXT," +
//                    "type VARCHAR(250)," +
//                    "date VARCHAR(250)" +
//
//                    ")";
//
//            // Tạo bảng product nếu chưa tồn tại
//            try (Statement stmt = connection.createStatement()) {
//                stmt.execute(sqlCreateTable);
//            }

            String sqlDropTable = "DROP TABLE IF EXISTS products";
            jdbcTemplate.execute(sqlDropTable);
            System.out.println("Bảng 'products' trong database 'control' đã được xóa!");
            createTable(dataSource);

            // Gọi phương thức checkOldDate để check dữ liệu trong db staging có phải dữ liệu cũ không
            checkOldData();
            System.out.println("Dữ liệu cũ đã được xóa đi");

            // Câu lệnh insert dữ liệu vào bảng product
            String sqlInsert = "INSERT INTO product (id, sku, name, price, original_price, brand_name, discount, thumbnail_url, " +
                    "short_description, images, colors, sizes, rating_average, review_count, discount_rate, " +
                    "quantity_sold, url_key, url_path, short_url, type, date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            // Import dữ liệu từ file CSV
            try (PreparedStatement pstmt = connection.prepareStatement(sqlInsert)) {
                for (String[] data : allData) {
                    pstmt.setString(1, data[0]); // id
                    pstmt.setString(2, data[1]); // sku
                    pstmt.setString(3, data[2]); // name
                    pstmt.setFloat(4, Float.parseFloat(data[3])); // price
                    pstmt.setFloat(5, Float.parseFloat(data[4])); // original_price
                    pstmt.setString(6, data[5]); // branch_name
                    pstmt.setFloat(7, Float.parseFloat(data[6])); // discount
                    pstmt.setString(8, data[7]); // thumbnail_url
                    pstmt.setString(9, data[8]); // short description
                    pstmt.setString(10, data[9]); // images
                    pstmt.setString(11, data[10]); // colors
                    pstmt.setString(12, data[11]); // sizes
                    pstmt.setDouble(13, Double.parseDouble(data[12])); // rating_average
                    pstmt.setInt(14, Integer.parseInt(data[13])); // review_count
                    pstmt.setDouble(15, Double.parseDouble(data[14])); // discount_rate
                    pstmt.setInt(16, Integer.parseInt(data[15])); // quantity_sold
                    pstmt.setString(17, data[16]); // url_key
                    pstmt.setString(18, data[17]); // url_path
                    pstmt.setString(19, data[18]); // short_url
                    pstmt.setString(20, data[19]); // type
                    // Parse chuỗi thời gian ban đầu thành LocalDateTime
                    LocalDateTime dateTime = LocalDateTime.parse(data[20]);

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                    pstmt.setString(21, dateTime.format(formatter));

                    pstmt.addBatch();
                }

                // Thực thi batch insert
                pstmt.executeBatch();
                isSuccess = true;
                String from = environment.getProperty("spring.mail.username");
                String body = "<html>" +
                        "<body>" +
                        "<h2 style='color:green;'>Load file csv thành công!</h2>" +
                        "<p>Chúng tôi đã lưu trữ dữ liệu sản phẩm thành công vào database staging</p>" +

                        "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!</p>" +
                        "</body>" +
                        "</html>";
                mailService.send(from, "tuhoangnguyen2003@gmail.com", "Load data vào staging thành công",body);
            }

        } catch (IOException | CsvException | SQLException e) {
            e.printStackTrace(); // Xử lý lỗi
            isSuccess = false;
        }

        insertLog(isSuccess,filePath);
    }


    // Phương thức lấy đường dẫn file từ bảng config
    private String getFilePathFromConfig() {
        String sql = "SELECT destination_path FROM logs order by id desc LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                String destinationPath = rs.getString("destination_path");
                // Chuyển đổi dấu phân tách thành dấu tương thích với hệ điều hành
                destinationPath =  destinationPath.replace("\\", "\\\\");
                System.out.println("Đường dẫn đầy đủ: " + destinationPath); // Debug đường dẫn
                return destinationPath;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Phương thức kiểm tra log trong database 'control'
    private boolean checkLogStatusAndDate() {
        // Truy vấn kiểm tra log mới nhất với status là 'SUCCESS_EXTRACT' và date là ngày hiện tại
        String sqlCheckLog = "SELECT COUNT(*) FROM logs " +
                "WHERE status = ? AND DATE_FORMAT(create_time, '%Y-%m-%d') = CURDATE() " +
                "ORDER BY id DESC LIMIT 1";

        try {
            int count = jdbcTemplate.queryForObject(sqlCheckLog, new Object[]{"SUCCESS_EXTRACT"}, Integer.class);
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Ghi log vào db control
    private void insertLog(boolean isSuccess, String filePath) {
        String sqlInsertLog = "INSERT INTO logs (count, id_config, create_time, time, created_by, destination_path, error_message, location, stack_trace, log_level, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String status = isSuccess ? "SUCCESS_EXTRACT" : "FAILED";
        int count = isSuccess ? 1 : 0;
        int id_config = 1;
        int time = 0;
        String createdBy = "Admin";
        String errorMessage = isSuccess ? "Crawl hoàn thành" : "Lỗi khi tải dữ liệu";
        String location = "Crawl Data";
        String stackTrace = isSuccess ? "INFO" : "ERROR";
        String logLevel = "INFO";

        jdbcTemplate.update(sqlInsertLog, count, id_config, Timestamp.valueOf(LocalDateTime.now()), time, createdBy, filePath, errorMessage, location, stackTrace, logLevel, status);
    }

}
