package com.example.demo.dataInitializer;

import com.example.demo.model.Config;
import com.example.demo.model.FileType;
import com.example.demo.model.Status;
import com.example.demo.service.ConfigService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@Order(34)
public class ConfigDataInitializer implements CommandLineRunner {

    private final ConfigService configService;

    public ConfigDataInitializer(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public void run(String... args) throws Exception {
        Config config1 = new Config();
        config1.setFileName("crawl_data_");
        config1.setFilePath("data");
        config1.setFileType(FileType.CSV);
        config1.setFileEncoding("UTF-8");
        config1.setSourcePath("https://tiki.vn/api/v2/products/%s");
        config1.setDestinationPath("D:\\cc\\data-warehouse-2024\\Module1_CrawlData");
        config1.setBackupPath("data_temporary");
        config1.setDelimiter(",");
        config1.setColumns("id,sku,productName,shortDescription,price,originalPrice,discount,quantitySold,description,images,sizes,color,brandName,thumbnailUrl,discountRate,ratingAverage,reviewCount,urlKey,urlPath,shortUrl,type");
        config1.setTables("Product_Dim, Date_Dim");
        config1.setStatus(Status.READY_EXTRACT);
        config1.setDataSize(10);
        config1.setCrawlFrequency(5);
        config1.setLastCrawlTime(null);
        config1.setTimeout(60);
        config1.setRetryCount(3);
        config1.setActive(true);
        config1.setLastUpdated(LocalDateTime.now());
        config1.setNotificationEmails("ngannguyen16122003@gmail.com");
        config1.setNote("This is a sample config.");
        config1.setVersion("1.0");
        config1.setCreatedBy("Admin");
        config1.setUpdatedBy("Admin");
        config1.setCreateTime(LocalDateTime.now());

        configService.save(config1);
    }
}
