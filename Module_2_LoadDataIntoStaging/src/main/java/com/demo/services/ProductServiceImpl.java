package com.demo.services;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ProductServiceImpl implements  ProductService {

    @Autowired
    private DatabaseService databaseService;

    @Override
    public void importCSV(String filePath, String databaseName) {
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
            String sqlCreateTable = "CREATE TABLE IF NOT EXISTS product (" +
                    "id VARCHAR(250) PRIMARY KEY," +
                    "sku VARCHAR(250)," +
                    "name VARCHAR(250)," +
                    "price FLOAT," +
                    "original_price FLOAT," +
                    "branch_name VARCHAR(250)," +
                    "discount FLOAT," +
                    "thumbnail_url TEXT," +
                    "short_description TEXT," +
                    "images TEXT," +
                    "colors TEXT," +
                    "sizes TEXT," +
                    "rating_average DOUBLE," +
                    "review_count INT," +
                    "discount_rate DOUBLE," +
                    "quantity_sold INT," +
                    "url_key VARCHAR(250)," +
                    "url_path TEXT," +
                    "short_url TEXT," +
                    "type VARCHAR(250)," +
                    "date VARCHAR(250)" +

                    ")";

            // Tạo bảng product nếu chưa tồn tại
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sqlCreateTable);
            }

            // Câu lệnh insert dữ liệu vào bảng product
            String sqlInsert = "INSERT INTO product (id, sku, name, price, original_price, branch_name, discount, thumbnail_url, " +
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
            }

        } catch (IOException | CsvException | SQLException e) {
            e.printStackTrace(); // Xử lý lỗi
        }
    }
}
