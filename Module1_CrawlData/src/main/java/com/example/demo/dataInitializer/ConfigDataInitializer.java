package com.example.demo.dataInitializer;

import com.example.demo.model.Config;
import com.example.demo.model.FileType;
import com.example.demo.model.Status;
import com.example.demo.service.ConfigService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

@Controller
@Order(34)
public class ConfigDataInitializer implements CommandLineRunner {

    private final ConfigService configService;

    public ConfigDataInitializer(ConfigService configService) {
        this.configService = configService;
    }
    String currentDir = System.getProperty("user.dir");
    @Override
    public void run(String... args) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        Config config1 = new Config();
        config1.setId(1);
        config1.setFileName("crawl_data_" + timestamp);
        config1.setFilePath("data");
        config1.setFileType(FileType.CSV);
        config1.setFileEncoding("UTF-8");
        config1.setSourcePath("https://tiki.vn/api/v2/products/%s");
        config1.setDestinationPath(currentDir);
        config1.setBackupPath("data_temporary");
        config1.setDelimiter(",");
        config1.setColumns("id VARCHAR(250), sku VARCHAR(250), name VARCHAR(250), price FLOAT, original_price FLOAT, brand_name VARCHAR(250), discount FLOAT, thumbnail_url TEXT, short_description TEXT, images TEXT, colors TEXT, sizes TEXT, rating_average DOUBLE, review_count INT, discount_rate DOUBLE, quantity_sold INT, url_key VARCHAR(250), url_path TEXT, short_url TEXT, type VARCHAR(250), date VARCHAR(250)");
        config1.setTables("Product_Dim, Date_Dim");
        config1.setStatus(Status.READY_EXTRACT);
        config1.setSTAGING_source_username("root");
        config1.setSTAGING_source_password("");
        config1.setSTAGING_source_host("127.0.0.1");
        config1.setSTAGING_source_port(3306);
        config1.setDW_source_username("root");
        config1.setDW_source_password("");
        config1.setDW_source_host("127.0.0.1");
        config1.setDW_source_port(3306);
        config1.setDataSize(10);
        config1.setCrawlFrequency(5);
        config1.setLastCrawlTime(null);
        config1.setTimeout(60);
        config1.setRetryCount(3);
        config1.setActive(true);
        config1.setLastUpdated(LocalDateTime.now());
        config1.setNotificationEmails("tuhoangnguyen2003@gmail.com");
        config1.setNote("This is a sample config.");
        config1.setVersion("1.0");
        config1.setCreatedBy("Admin");
        config1.setUpdatedBy("Admin");
        config1.setCreateTime(LocalDateTime.now());
        config1.setUpdateTime(LocalDateTime.now());

        configService.save(config1);
    }
}
