package com.nlu.app.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DataCrawlService {
    public static List<String> crawl(String webURL) throws InterruptedException {
        List<String> productDetails = new ArrayList<>(); // Danh sách chứa thông tin sản phẩm

        // Tạo ChromeOptions và set chế độ chạy ẩn
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Chế độ chạy ẩn
        options.addArguments("--disable-gpu"); // Tắt GPU (tùy chọn này có thể cần trên Windows)
        options.addArguments("--no-sandbox"); // Tùy chọn an toàn hơn cho môi trường Linux
        options.addArguments("--disable-dev-shm-usage"); // Giảm thiểu tài nguyên bộ nhớ chia sẻ
        options.addArguments("--window-size=1920,1080"); // Đặt kích thước cửa sổ để tránh lỗi layout
        options.addArguments("--fake-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36"); // Add a fake user-agent if needed

        var driver = new ChromeDriver(options);
        try {
            driver.get(webURL);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(25));
            Actions actions = new Actions(driver);

            // Cuộn trang bằng JavaScript để đảm bảo tải hết nội dung
            JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
            for (int i = 0; i < 5; i++) {
                jsExecutor.executeScript("window.scrollBy(0,1000);");
                Thread.sleep(500); // Đợi trang tải thêm nội dung
            }

            // Tìm và nhấp vào nút "Xem thêm"
            WebElement buttonBlock = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".ToggleContent__Wrapper-sc-fbuwol-1")));
            actions.scrollToElement(buttonBlock).perform(); // Cuộn đến nút

            List<WebElement> buttons = driver.findElements(By.cssSelector(".ToggleContent__Wrapper-sc-fbuwol-1 .btn-more"));
            if (!buttons.isEmpty()) {
                actions.moveToElement(buttons.get(0)).click().perform(); // Nhấp vào nút "Xem thêm"

                // Đợi cho nội dung modal xuất hiện
                WebElement modalContent = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".styles__BodyContainerStyled-sc-10qbgzc-0")));

                // Kiểm tra và in nội dung modal
                if (modalContent.isDisplayed()) {
                    String content = modalContent.getText();

                    // Lấy thông tin kích thước từ các nút kích thước
                    List<WebElement> sizeOptions = modalContent.findElements(By.xpath("//div[@data-view-id='pdp_main_select_configuration_item' and not(./span/div)]"));
                    StringBuilder sizes = new StringBuilder();
                    for (WebElement size : sizeOptions) {
                        sizes.append(size.getText()).append(", "); // Thêm kích thước vào chuỗi
                    }

                    // Xử lý chuỗi kích thước
                    if (sizes.length() > 0) {
                        sizes.setLength(sizes.length() - 2); // Loại bỏ dấu phẩy và khoảng trắng cuối cùng
                    }
                    String sizesString = sizes.toString();

                    // Lấy thông tin màu sắc
                    List<WebElement> colorOptions = modalContent.findElements(By.xpath("//div[@data-view-id='pdp_main_select_configuration_item' and ./span/div]"));
                    List<String> colors = new ArrayList<>();
                    for (WebElement colorOption : colorOptions) {
                        // Lấy tên màu từ thẻ <span> thứ hai (trong cấu trúc HTML bạn cung cấp)
                        String colorName = colorOption.findElement(By.xpath(".//span[contains(text(), '')]")).getText();
                        colors.add(colorName);
                    }

                    // Tạo chuỗi từ danh sách màu sắc
                    String colorsString = String.join(", ", colors);

                    // Tìm thẻ a chứa thương hiệu
                    WebElement brandElement = modalContent.findElement(By.cssSelector("a[data-view-id='pdp_details_view_brand']"));

                    // Lấy URL của thương hiệu
                    String brandUrl = brandElement.getAttribute("href");
                    String brandName = brandElement.getText(); // Lấy tên thương hiệu

//                    System.out.println("URL thương hiệu: " + brandUrl);
//                    System.out.println("Tên thương hiệu: " + brandName);

                    productDetails.add(content); // Thêm nội dung vào danh sách
                    productDetails.add(sizesString); // Thêm kích thước vào danh sách
                    productDetails.add(colorsString); // Thêm màu sắc vào danh sách
                    productDetails.add(brandName);//Thêm thương hiệu vào danh sách
                    return productDetails; // Trả về danh sách chứa thông tin

                } else {
                    System.out.println("Nội dung modal không hiển thị.");
                }
            } else {
                System.out.println("Không tìm thấy nút 'Xem thêm' để nhấp.");
            }
        } finally {
            driver.quit(); // Đảm bảo driver luôn được đóng
        }
        return null; // Trả về null nếu không có nội dung
    }

    public static void main(String[] args) throws InterruptedException {
        List<String> test = DataCrawlService.crawl("https://tiki.vn/giay-the-thao-nam-giay-chay-bo-de-em-nhe-thoang-khi-de-cao-su-duc-chong-tron-truot-han-che-mon-gna2024-p275181298.html?itm_campaign=CTP_YPD_TKA_PLA_UNK_ALL_UNK_UNK_UNK_UNK_X.293779_Y.1876099_Z.3957053_CN.GNA2024&itm_medium=CPC&itm_source=tiki-ads&spid=275181306");
        if (test != null) {
            System.out.println("Nội dung sản phẩm: " + test.get(0));
            System.out.println("Kích thước: " + test.get(1));
            System.out.println("Màu sắc: " + test.get(2));
        } else {
            System.out.println("Không có thông tin sản phẩm.");
        }
    }
}
