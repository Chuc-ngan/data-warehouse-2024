package com.demo.services;

import com.demo.entities.Log;

public interface LogService {

    public String getFilePathData(String id);

    public void insertLog(Log log);
}
