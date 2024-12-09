package com.demo.services;

import com.demo.entities.Config;
import com.demo.entities.Log;
import com.demo.repository.ConfigRepository;
import com.demo.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogServiceImpl implements LogService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LogRepository logRepository;

    @Override
    public String getFilePathData(String logId) {
        Log log = logRepository.findById(logId).orElseThrow(() ->
                new RuntimeException("Config with ID " + logId + " not found"));

        return log.getDestinationPath();
    }

    @Override
    public void insertLog(Log log) {
        logRepository.save(log);
    }

    @Override
    public List<Log> findByStatusAndCreatedTimeToday() {
        return logRepository.findByStatusAndCreatedTimeToday();
    }
}
