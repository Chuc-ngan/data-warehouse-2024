package com.demo.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class ConfigRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> getStagingConfig() {
        String sql = "SELECT * FROM config WHERE staging_source_name = 'staging' LIMIT 1";
        return jdbcTemplate.queryForMap(sql);
    }
}

