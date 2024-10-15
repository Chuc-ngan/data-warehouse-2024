package com.example.demo;

import com.example.demo.model.Product;
import com.example.demo.service.crawler.CrawlService;
import com.example.demo.utils.CsvReader;
import com.example.demo.utils.CsvWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner; // Thêm import này
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

	@Autowired
	private CrawlService crawlService;

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		CsvReader csvReader = new CsvReader();
		String currentDirectory = System.getProperty("user.dir");
		String csvFilePath = currentDirectory + FileSystems.getDefault().getSeparator()+ "data" +  FileSystems.getDefault().getSeparator()+"products_id.csv";
		List<String> productIds = csvReader.readProductIdsFromCsv(csvFilePath);
		List<String> limitedProductIds = productIds.size() > 10 ? productIds.subList(0, 10) : productIds;

		List<Product> products = crawlService.crawlProducts(limitedProductIds);

		CsvWriter csvWriter = new CsvWriter();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String timestamp = dateFormat.format(new Date());
		String outputCsvFilePath = currentDirectory + FileSystems.getDefault().getSeparator()+ "data" +  FileSystems.getDefault().getSeparator()+"crawl_data_" + timestamp + ".csv";
		csvWriter.writeProductsToCsv(products, outputCsvFilePath);
		products.forEach(product -> {
			System.out.println("Product ID: " + product.getId());
			System.out.println("Product Sku: " + product.getSku());
			System.out.println("Product Name: " + product.getProductName());
			System.out.println("Price: " + product.getPrice());
			System.out.println("Original Price: " + product.getOriginalPrice());
			System.out.println("Brand Name: " + product.getBrandName());
			System.out.println("Discount: " + product.getDiscount());
			System.out.println("Thumbnail URL: " + product.getThumbnailUrl());
			System.out.println("Short Description: " + product.getShortDescription());
			System.out.println("Images: " + product.getImages());
			System.out.println("Colors: " + product.getColor());
			System.out.println("Size: " + product.getSizes());
			System.out.println("RatingAverage: " + product.getRatingAverage());
			System.out.println("ReviewCount: " + product.getReviewCount());
			System.out.println("DiscountRate: " + product.getDiscountRate());
			System.out.println("QuantitySold: " + product.getQuantitySold());
			System.out.println("UrlKey: " + product.getUrlKey());
			System.out.println("UrlPath: " + product.getUrlPath());
			System.out.println("ShortUrl: " + product.getShortUrl());
			System.out.println("Type: " + product.getType());
			System.out.println("-----------------------------------------------------");
		});
	}
}
