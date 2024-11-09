package com.demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private JdbcTemplate jdbcTemplate; // Kết nối tới database control

    @Override
    public void TransformData() {
        // Gọi stored procedure và lấy kết quả
        List<Map<String, Object>> results = jdbcTemplate.queryForList("CALL transform_and_cleaning_data()");

        // In kết quả ra console
        for (Map<String, Object> row : results) {
            System.out.println(row);
        }
    }
}
