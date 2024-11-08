package com.demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private JdbcTemplate jdbcTemplate; // Kết nối tới database control


    @Override
    public void TransformData() {

    }
}
