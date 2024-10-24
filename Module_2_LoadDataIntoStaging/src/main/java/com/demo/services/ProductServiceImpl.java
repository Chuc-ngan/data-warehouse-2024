package com.demo.services;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;


import com.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.demo.entities.Product;


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
					product.setId(values[0]); // id
					product.setSku(values[1]); // sku
					product.setProduct_name(values[2]); // productName
					product.setPrice(Double.parseDouble(values[3])); // price
					product.setOriginal_price(Double.parseDouble(values[4])); // originalPrice
					product.setBrand_name(values[5]); // brandName
					product.setDiscount(Double.parseDouble(values[6])); // discount
					product.setThumbnail_url(values[7]); // thumbnailUrl
					product.setShort_description(values[8]); // shortDescription
					product.setImages(List.of(values[9].split(";"))); // images
					product.setColor(List.of(values[10].split(";"))); // color
					product.setSizes(List.of(values[11].split(";"))); // sizes
					product.setRating_average(Double.parseDouble(values[12])); // ratingAverage
					product.setReview_count(Integer.parseInt(values[13])); // reviewCount
					product.setDiscount_rate(Integer.parseInt(values[14])); // discountRate
					product.setQuantity_sold(Integer.parseInt(values[15])); // quantitySold
					product.setUrl_key(values[16]); // urlKey
					product.setUrl_path(values[17]); // urlPath
					product.setShort_url(values[18]); // shortUrl
					product.setType(values[19]); // type
					product.setCreate_time(LocalDateTime.parse(values[20]));
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