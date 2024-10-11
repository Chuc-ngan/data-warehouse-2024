package com.nlu.app.entity;

import java.time.LocalDateTime;

public class Log {
    private int id;
    private String idConfig;
    private String name;
    private Status status;
    private String fileName;
    private int count;
    private String location;
    private LocalDateTime time;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createdBy;
    private String updatedBy;
    private String errorMessage;
    private long dataSize;      // Kích thước dữ liệu crawl
    private long crawlDuration; // Thời gian hoàn thành crawl


}
