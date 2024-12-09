package com.demo.services;

import com.demo.entities.Log;
import com.demo.entities.Status;

import java.time.LocalDateTime;

public interface LogService {

    public String getFilePathData(int id);

    public void insertLog(Log log);

    void updateLogStatus(int logId, String errorMessage, Status status);

    Integer getLogIdForToday();
}
