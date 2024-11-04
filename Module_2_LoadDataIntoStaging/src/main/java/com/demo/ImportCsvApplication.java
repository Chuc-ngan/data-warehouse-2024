package com.demo;

import com.demo.services.ProductService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ImportCsvApplication {

	public static void main(String[] args) {

		// Chạy Spring Boot application
		ConfigurableApplicationContext context = SpringApplication.run(ImportCsvApplication.class, args);

		// Lấy ProductService từ Spring context
		ProductService productService = context.getBean(ProductService.class);

		// Đường dẫn đến file CSV và tên database cần import (tùy theo cấu hình của bạn)
		String filePath = "D:\\workspace\\Project\\DataWarehouse\\data-warehouse-2024\\Module1_CrawlData\\data\\crawl_data_20241024_145402.csv"; // Thay đường dẫn file CSV thực tế

		try {
				// Gọi phương thức importCsvToDatabase để thực hiện import dữ liệu
				productService.importCSV();
				System.out.println("Import thành công!");

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Có lỗi xảy ra trong quá trình import: " + e.getMessage());
		}

		// Đóng Spring context sau khi hoàn thành
		context.close();


	}




}