package com.demo.services;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.demo.entities.Product;
import com.demo.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class ProductServiceImpl implements ProductService{
	
	@Autowired
	private ProductRepository productRepository;

	@Override
	public void importCSV(String filePath) {
		try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
            String[] values;
            boolean isFirstLine = true;
            try {
				while ((values = csvReader.readNext()) != null) {
					if(isFirstLine) {
						isFirstLine = false;
						continue;
					}
				    Product product = new Product();
				    product.setId(Integer.parseInt(values[0]));
				    product.setSku(values[1]);
				    product.setDescription(values[2]);
				    product.setPrice(Float.parseFloat(values[3]));
				    product.setListPrice(Float.parseFloat(values[4]));
				    product.setOriginalPrice(Float.parseFloat(values[5]));
				    
				    
				    List<String> sizes = parseColors(values[6]);
				    String sizesString = String.join(", ", sizes);
				    product.setSizes(values[6]);
				    
				    List<String> colors = parseColors(values[7]);
				    String colorsString = String.join(", ", colors);
	                product.setColors(colorsString);
	                
	                product.setDiscount(Float.parseFloat(values[8]));
	                product.setDiscountRate(Double.parseDouble(values[9]));
	                product.setReviewCount(Integer.parseInt(values[10]));
	                product.setInventoryStatus(values[11]);
	                product.setStockItemMaxSaleQty(Integer.parseInt(values[12]));
	                product.setProductName(values[13]);
	                product.setBranchId(Integer.parseInt(values[14]));
	                product.setBranchName(values[15]);
	                product.setThumbnailUrl(values[16]);
	                product.setUrlKey(values[17]);
	                product.setUrlPath(values[18]);
	                product.setRatingAverage(Double.parseDouble(values[19]));
	                
	                product.setImages(values[20]);
	                
	                
	                productRepository.save(product);

				}
			} catch (CsvValidationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        } catch (IOException e) {
            e.printStackTrace();
        }
		
	}
	
	private List<String> parseColors(String value) {
	    // Kiểm tra xem giá trị có chứa "Màu" không
	    if (value.startsWith("{Màu=[") && value.endsWith("]}")) {
	        // Tách chuỗi để lấy phần bên trong
	        String colorsString = value.substring(6, value.length() - 2); // Bỏ đi "{Màu=[" và "]}"
	        return Arrays.asList(colorsString.split(",\\s*")); // Tách bằng dấu phẩy và loại bỏ khoảng trắng
	    }
	    return Collections.emptyList(); // Trả về danh sách rỗng nếu không đúng định dạng
	}
	
	private List<String> parseSizes(String value) {
	    // Kiểm tra xem giá trị có chứa "Size" không
	    if (value.startsWith("{Size=[") && value.endsWith("]}")) {
	        // Tách chuỗi để lấy phần bên trong
	        String sizesString = value.substring(7, value.length() - 2); // Bỏ đi "{Size=[" và "]}"
	        String[] sizesArray = sizesString.split(",\\s*"); // Tách bằng dấu phẩy và loại bỏ khoảng trắng

	        // Chuyển đổi chuỗi thành Integer
	        List<String> sizes = new ArrayList<>();
	        for (String size : sizesArray) {
	            try {
	                sizes.add(size);
	            } catch (NumberFormatException e) {
	                System.out.println("Lỗi chuyển đổi kích thước: " + size);
	            }
	        }
	        return sizes;
	    }
	    return Collections.emptyList(); // Trả về danh sách rỗng nếu không đúng định dạng
	}
	
	
	private List<Map<String, Object>> parseVariations(String json) {
	    ObjectMapper objectMapper = new ObjectMapper();
	    try {
	        return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
	    } catch (IOException e) {
	        System.out.println("Lỗi phân tích JSON cho variations: " + e.getMessage());
	        return Collections.emptyList();
	    }
	}
	

}
