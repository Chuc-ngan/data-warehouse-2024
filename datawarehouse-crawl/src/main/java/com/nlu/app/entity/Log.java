package com.nlu.app.entity;

import java.time.LocalDateTime;

public class Log {
    private int id;
    private Config idConfig;
    private LogLevel logLevel;
    private int count;
    private String location;
    private LocalDateTime time;
    private String errorMessage;
    private String stackTrace;  // Chi tiết stack trace khi xảy ra lỗi
    private int retryCount;  // Số lần thử lại khi crawl thất bại
    private Status status;  // Trạng thái của quá trình crawl
    private long dataSize;  // Kích thước dữ liệu đã crawl
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;


}
