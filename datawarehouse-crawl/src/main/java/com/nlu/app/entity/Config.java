package com.nlu.app.entity;

import java.time.LocalDateTime;
import java.util.List;

public class Config {
    private String id;
    private String fileName;
    private String filePath;  // Đường dẫn tới file trên hệ thống lưu trữ
    private FileType fileType;  // Loại file dữ liệu (hoặc extension)
    private String fileEncoding;  // Mã hóa của file dữ liệu (ví dụ: UTF-8)
    private String sourcePath;  // Đường dẫn nguồn của dữ liệu
    private String destinationPath;   // Đường dẫn đích đến
    private String backupPath;  // Đường dẫn lưu trữ bản sao dự phòng
    private String delimiter;  // Dấu phân cách trong file (ví dụ: , ; \t)
    private List<String> columns;  // Danh sách các cột của dữ liệu cần được crawl
    private List<String> tables;  // Danh sách các bảng trong cơ sở dữ liệu
    private String STAGING_source_username; // Tên người dùng để truy cập nguồn dữ liệu
    private String STAGING_source_password; // Mật khẩu để truy cập nguồn dữ liệu
    private String STAGING_source_host; // Địa chỉ host của nguồn dữ liệu
    private int STAGING_source_port; // Cổng để kết nối tới nguồn dữ liệu
    private String DW_source_username; // Tên người dùng để truy cập nguồn dữ liệu
    private String DW_source_password; // Mật khẩu để truy cập nguồn dữ liệu
    private String DW_source_host; // Địa chỉ host của nguồn dữ liệu
    private int DW_source_port; // Cổng để kết nối tới nguồn dữ liệu
    private long dataSize;   // Kích thước của dữ liệu
    private int crawlFrequency;  // Tần suất crawl dữ liệu
    private int timeout;    // Thời gian timeout tối đa cho một phiên crawl
    private LocalDateTime lastCrawlTime;  // Thời gian crawl dữ liệu cuối cùng
    private int retryCount;  // Số lần thử lại khi crawl dữ liệu thất bại
    private boolean isActive;  // Thêm biến isActive để bật/tắt cấu hình
    private LocalDateTime lastUpdated;  // Thời gian cập nhật cấu hình gần nhất
    private List<String> notificationEmails;  // Danh sách email thông báo
    private String note;  // Ghi chú thêm về cấu hình
    private String version;  // Phiên bản của cấu hình
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
