package com.demo.controllers;

import com.demo.entities.Product;
import com.demo.repository.primary.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/test")
public class DatabaseTestController {

    @Autowired
    private ProductRepository productRepository;



    @GetMapping("/primary")
    public ResponseEntity<String> testPrimary() {
        try {
            Optional<Product> product = productRepository.findById("107802069"); // Đếm số bản ghi trong một bảng bất kỳ của database thứ nhất
            return ResponseEntity.ok("Primary database connected successfully. Record count: " + product);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to connect to primary database: " + e.getMessage());
        }
    }


}
