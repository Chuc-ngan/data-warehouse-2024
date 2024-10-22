package com.demo.services;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.demo.entities.Product;
import com.demo.repository.ProductRepository;


@Service
public class ProductServiceImpl implements ProductService{
	
	@Autowired
	private ProductRepository productRepository;

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
					product.setId(values[0]); // id
					product.setSku(values[1]); // sku
					product.setProductName(values[2]); // productName
					product.setPrice(Double.parseDouble(values[3])); // price
					product.setOriginalPrice(Double.parseDouble(values[4])); // originalPrice
					product.setBrandName(values[5]); // brandName
					product.setDiscount(Double.parseDouble(values[6])); // discount
					product.setThumbnailUrl(values[7]); // thumbnailUrl
					product.setShortDescription(values[8]); // shortDescription
					product.setImages(List.of(values[9].split(";"))); // images
					product.setColor(List.of(values[10].split(";"))); // color
					product.setSizes(List.of(values[11].split(";"))); // sizes
					product.setRatingAverage(Double.parseDouble(values[12])); // ratingAverage
					product.setReviewCount(Integer.parseInt(values[13])); // reviewCount
					product.setDiscountRate(Integer.parseInt(values[14])); // discountRate
					product.setQuantitySold(Integer.parseInt(values[15])); // quantitySold
					product.setUrlKey(values[16]); // urlKey
					product.setUrlPath(values[17]); // urlPath
					product.setShortUrl(values[18]); // shortUrl
					product.setType(values[19]); // type
					product.setCreateTime(LocalDateTime.parse(values[20]));
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

}
