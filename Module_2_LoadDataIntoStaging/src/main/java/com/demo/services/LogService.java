package com.demo.services;

import com.demo.entities.Log;

import java.util.List;

public interface LogService {

    public String getFilePathData(String id);


    public void insertLog(Log log);

    public List<Log> findByStatusAndCreatedTimeToday();
}
