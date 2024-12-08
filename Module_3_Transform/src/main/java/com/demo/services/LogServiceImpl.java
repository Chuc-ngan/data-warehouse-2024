package com.demo.services;

import com.demo.entities.Log;
import com.demo.entities.Status;
import com.demo.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class LogServiceImpl implements LogService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LogRepository logRepository;

    @Override
    public String getFilePathData(int logId) {
        Log log = logRepository.findById(logId).orElseThrow(() ->
                new RuntimeException("Config with ID " + logId + " not found"));

        return log.getDestinationPath();
    }

    @Override
    public void insertLog(Log log) {
        logRepository.save(log);
    }

    @Override
    public void updateLogStatus(int logId, String errorMessage) {
        String sql = "UPDATE control.logs " +
                "SET status = 'SUCCESS_TRANSFORM', " +
                "error_message = " + errorMessage + ", " +
                "location = 'Transform', " +
                "update_time = CURTIME() " +
                "WHERE id = ?";

        jdbcTemplate.update(sql, logId);
    }

    @Override
    public Integer getLogIdForToday() {
        String sql = "SELECT l.id FROM `control`.`logs` l " +
                                     "WHERE l.`status` = 'SUCCESS_LOAD_DATA'" +
                                     "AND DATE(l.create_time) = CURDATE()";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
