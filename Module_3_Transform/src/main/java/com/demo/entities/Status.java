package com.demo.entities;

public enum Status {
    READY_EXTRACT,  // Sẵn sàng trích xuất
    SUCCESS_EXTRACT,  // Trích xuất thành công
    FAILURE_EXTRACT,  // Trích xuất thất bại
    PROCESSING,  // Đang xử lý
    SUCCESS_LOAD_DATA,  // Load Data thành công
    FAILURE_LOAD_DATA,  // Load Data thất bại
    ACTIVE;
}
