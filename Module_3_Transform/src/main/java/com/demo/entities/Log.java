package com.demo.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "logs")
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id; // Khóa chính

    @JoinColumn(name = "id_config") // Tên cột trong bảng logs
    private int idConfig;

    @Enumerated(EnumType.STRING) // Nếu LogLevel là enum
    private LogLevel logLevel;
    private String destinationPath;   // Đường dẫn đích đến
    private int count;
    private String location;
    private LocalDateTime updateTime;
    private String errorMessage;
    private String stackTrace;  // Chi tiết stack trace khi xảy ra lỗi

    @Enumerated(EnumType.STRING) // Nếu Status là enum
    private Status status;  // Trạng thái của quá trình crawl
    private String createdBy;
    private LocalDateTime createTime;

    public Log(int idConfig, LogLevel logLevel, String destinationPath, int count, String location, LocalDateTime updateTime, String errorMessage, String stackTrace, Status status, String createdBy, LocalDateTime createTime) {
        this.idConfig = idConfig;
        this.logLevel = logLevel;
        this.destinationPath = destinationPath;
        this.count = count;
        this.location = location;
        this.updateTime = updateTime;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.status = status;
        this.createdBy = createdBy;
        this.createTime = createTime;
    }
}
