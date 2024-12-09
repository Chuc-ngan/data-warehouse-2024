package com.demo;

import com.demo.services.ProductService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Module3TransformApplication {

    public static void main(String[] args) {
        // Chạy Spring Boot application
        ConfigurableApplicationContext context = SpringApplication.run(Module3TransformApplication.class, args);

        // Lấy ProductService từ Spring context
        ProductService productService = context.getBean(ProductService.class);

        productService.TransformData();
    }
}
