package com.nlu.app.service.selenium;

import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

@SpringBootApplication
@RequiredArgsConstructor
public class CrawlDataSelenium {
    public static void main(String[] args) throws InterruptedException, IOException {
        SpringApplication.run(CrawlDataSelenium.class, args);

        // Tạo ChromeOptions và set chế độ chạy ẩn
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Chế độ chạy ẩn
        options.addArguments("--disable-gpu"); // Tắt GPU (tùy chọn này có thể cần trên Windows)
        options.addArguments("--no-sandbox"); // Tùy chọn an toàn hơn cho môi trường Linux
        options.addArguments("--disable-dev-shm-usage"); // Giảm thiểu tài nguyên bộ nhớ chia sẻ
        options.addArguments("--window-size=1920,1080"); // Đặt kích thước cửa sổ để tránh lỗi layout
        options.addArguments("--fake-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36"); // Add a fake user-agent if needed

        // Khởi tạo ChromeDriver với ChromeOptions
        WebDriver driver = new ChromeDriver(options);
        Actions actions = new Actions(driver);
        driver.get("https://tiki.vn/giay-dep-nam/c1686");

        // Khởi tạo WebDriverWait
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        int maxAttempts = 10; // Số lần tối đa cố gắng
        int attempts = 0; // Biến đếm số lần cố gắng
        int maxClicks = 5; // Giới hạn số lần nhấp nút Show More
        int clickCount = 0; // Biến đếm số lần nhấp nút Show More
        List<WebElement> products = driver.findElements(By.cssSelector(".styles__ProductItemContainerStyled-sc-bszvl7-0"));
        while (attempts < maxAttempts && clickCount < maxClicks) {
            try {
                // Kiểm tra sự tồn tại của nút Show More
                List<WebElement> showMoreButtonList = driver.findElements(By.cssSelector(".styles__StyledLoadingContainer-sc-1p0mhu9-0"));
                if (showMoreButtonList.isEmpty()) {
                    System.out.println("Nút Show More không còn tồn tại.");
                    break; // Thoát vòng lặp khi nút không còn tồn tại
                }

                System.out.println("Chạy vòng lặp, thử số: " + (attempts + 1));
                WebElement showMoreButton = showMoreButtonList.get(0);

                // Chờ cho nút có thể nhấp được
                wait.until(ExpectedConditions.elementToBeClickable(showMoreButton));

                // Kiểm tra trạng thái nút trước khi bấm
                if (showMoreButton.isEnabled() && showMoreButton.isDisplayed()) {
                    // Bấm vào nút để load thêm dữ liệu
                    actions.moveToElement(showMoreButton).click().perform();
                    Thread.sleep(100); // Chờ cho dữ liệu mới được tải
                    clickCount++; // Tăng số lần nhấp nút
                    System.out.println("Đã bấm nút Show More, số lần nhấp: " + clickCount);

                    // Chờ dữ liệu mới xuất hiện
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".styles__ProductItemContainerStyled-sc-bszvl7-0")));

                    // Kiểm tra xem có sản phẩm mới được thêm vào không
                    List<WebElement> newProducts = driver.findElements(By.cssSelector(".styles__ProductItemContainerStyled-sc-bszvl7-0"));
                    if (newProducts.size() == products.size()) {
                        System.out.println("Không có sản phẩm mới được tải thêm. Thoát vòng lặp.");
                        break; // Thoát nếu không có sản phẩm mới
                    }

                    products = newProducts; // Cập nhật danh sách sản phẩm
                    attempts = 0; // Đặt lại số lần thử nếu thành công
                } else {
                    System.out.println("Nút Show More không thể bấm.");
                    break; // Thoát vòng lặp nếu nút không thể bấm
                }
            } catch (Exception e) {
                e.printStackTrace();
                attempts++; // Tăng số lần thử nếu có lỗi xảy ra
            }
        }

        // Tiếp tục với phần thu thập dữ liệu sản phẩm như trước
        System.out.println("Chạy xong vòng lặp.");

        CSVWriter writer = new CSVWriter(new FileWriter("D:\\workspace\\Project\\DataWarehouse\\data\\crawl.csv"));
        String[] header = {"productLink","productName", "productImg", "description", "size", "color", "price", "trademark"};
        writer.writeNext(header);
        String nameHtml;
        String imgHtml;
        List<String> infoHtml;
        int count = 0;
        for (WebElement product : products) {
            try {
                var href = product.findElement(By.cssSelector("a[href]"));
                String productHref = href.getAttribute("href");

                var name = product.findElement(By.cssSelector(".style__NameStyled-sc-139nb47-8"));
                nameHtml = name.getAttribute("outerHTML");
                String productName = name.getText();

                var price = product.findElement(By.cssSelector(".price-discount__price"));
//                nameHtml = price.getAttribute("outerHTML");
                String productPrice = price.getText();

                var imageLink = product.findElement(By.cssSelector(".styles__StyledImg-sc-p9s3t3-0"));
                String imgUrl = imageLink.getAttribute("srcset"); // Nếu hình ảnh có srcset
                if (imgUrl == null || imgUrl.isEmpty()) {
                    imgUrl = imageLink.getAttribute("data-src"); // Nếu srcset không có, thử lấy từ data-src
                }

                String productLink = href.getAttribute("href");
                System.out.println("product link: " + productLink);

                infoHtml = DataCrawlService.crawl(productLink);

                System.out.println("Tên sản phẩm: " + productName);
                System.out.println("Hình ảnh sản phẩm: " + imgUrl);
//                System.out.println("Thông tin sản phẩm: " + infoHtml);
                System.out.println("-------------------------------------------");
//                String[] header = {"productLink","productName", "productImg", "description", "price", "trademark"};
                writer.writeNext(new String[]{productLink,productName, imgUrl,infoHtml.get(0) ,infoHtml.get(1) ,infoHtml.get(2),productPrice, infoHtml.get(3)});
                count++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Tổng số sản phẩm crawl được: " + count);
        driver.quit();
    }


}
