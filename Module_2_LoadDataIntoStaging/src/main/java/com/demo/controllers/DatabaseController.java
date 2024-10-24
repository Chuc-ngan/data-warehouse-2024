package com.demo.controllers;
import com.demo.services.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@RestController
@RequestMapping("api")
public class DatabaseController {

    @Autowired
    private DatabaseService databaseService;

    @GetMapping("/connect-staging")
    public String connectToStaging() {
        DataSource dataSource = databaseService.connectToStagingDatabase();
        try (Connection connection = dataSource.getConnection()) {
            return "Kết nối thành công đến database staging!";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Kết nối thất bại: " + e.getMessage();
        }
    }
}
