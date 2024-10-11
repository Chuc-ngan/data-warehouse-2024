package com.nlu.app.entity;

import java.time.LocalDateTime;
import java.util.List;

public class Config {
    private String id;
    private String fileName;
    private String filePath;
    private FileType fileType;
    private String sourcePath;
    private String destinationPath;
    private List<String> columns;
    private List<String> tables;
    private long dataSize;
    private LogLevel logLevel;
    private int crawlFrequency;   // Tần suất crawl dữ liệu
    private int timeout;
    private boolean isActive;     // Thêm biến isActive để bật/tắt cấu hình
    private LocalDateTime lastUpdated;  // Thời gian cập nhật cấu hình gần nhất
    

}
