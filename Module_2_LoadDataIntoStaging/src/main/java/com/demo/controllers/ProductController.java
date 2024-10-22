package com.demo.controllers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.demo.helpers.FileHelper;
import com.demo.services.ProductService;

@RestController
@RequestMapping("api")
public class ProductController {

	@Autowired
	private ProductService productService;

	@Autowired
	private Environment environment;

	@PostMapping(value = "import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> importCSV(@RequestPart("file") MultipartFile file) {
		try {
			// Kiểm tra file có rỗng không
			if (file.isEmpty()) {
				return new ResponseEntity<>("File is empty.", HttpStatus.BAD_REQUEST);
			}

			// Kiểm tra định dạng file (chỉ cho phép .csv)
			if (!file.getOriginalFilename().endsWith(".csv")) {
				return new ResponseEntity<>("Invalid file format. Please upload a CSV file.", HttpStatus.BAD_REQUEST);
			}

			// Đường dẫn lưu file
			String uploadDir = environment.getProperty("upload.path");
			File uploadFolder = new File(uploadDir);
			if (!uploadFolder.exists()) {
				uploadFolder.mkdirs();
			}

			String fileName = FileHelper.generateFileName(file.getOriginalFilename());
			Path path = Paths.get(uploadFolder.getAbsolutePath(), fileName);
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

			// Gọi service để import dữ liệu từ file
			productService.importCSV(path.toString());
			return new ResponseEntity<>("Import successful!", HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace(); // In ra lỗi để theo dõi
			return new ResponseEntity<>("Import failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
}