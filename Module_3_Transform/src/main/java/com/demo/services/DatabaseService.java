package com.demo.services;

import com.demo.entities.Config;
import com.demo.repository.ConfigRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class DatabaseService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ConfigRepository configRepository;

    public DataSource connectToStagingDatabase(String configId) {
        // Lấy config dựa vào configId được truyền vào
        Config config = configRepository.findById(configId).orElseThrow(() ->
                new RuntimeException("Config with ID " + configId + " not found"));

        // Lấy thông tin kết nối từ config
        String url = "jdbc:mysql://" + config.getStagingSourceHost() + ":"
                + config.getStagingSourcePort() + "/staging";
        String username = config.getStagingSourceUsername();
        String password = config.getStagingSourcePassword();

        // Tạo DataSource dựa trên thông tin từ config
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        return dataSource;
    }
}
