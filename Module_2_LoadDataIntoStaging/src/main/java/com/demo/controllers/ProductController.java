package com.demo.controllers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
			
			/* Upload File */
			File uploadFolder = new File(new ClassPathResource(".").getFile().getPath() + "/static/assets/csv");
			if(!uploadFolder.exists()) {
				uploadFolder.mkdirs();
			} 
			
			String fileName = FileHelper.generateFileName(file.getOriginalFilename());
			System.out.println(fileName);
			File saveFile = new ClassPathResource("static/assets/csv").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + fileName);
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			
			
			System.out.println("Path: " + path);
			productService.importCSV(path.toString()); 
			return new ResponseEntity<>("Import successful!", HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>("Import failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

}
