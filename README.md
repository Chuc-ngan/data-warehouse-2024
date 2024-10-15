# Data Warehouse

## Mô Tả Dự Án
Dự án này là một ứng dụng Java Spring Boot dùng để crawl dữ liệu từ trang web Tiki và lưu trữ dữ liệu sản phẩm vào cơ sở dữ liệu. Ứng dụng cũng hỗ trợ gửi email thông báo khi hoàn tất quá trình crawl dữ liệu.

## Yêu Cầu Hệ Thống
- Java 11 hoặc cao hơn
- Maven
- Cơ sở dữ liệu (MySQL)

## Cài Đặt

1. **Tạo Cơ Sở Dữ Liệu:**
   - Trước khi chạy ứng dụng, hãy tạo cơ sở dữ liệu trong hệ thống quản lý cơ sở dữ liệu của bạn MySQL.
   - Ghi chú tên cơ sở dữ liệu vì bạn sẽ cần sử dụng nó trong file cấu hình.

2. **Cấu Hình File `application.properties`:**
   - Mở file `src/main/resources/application.properties` và điền thông tin cấu hình sau:

   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/<tên_cơ_sở_dữ_liệu>
   spring.datasource.username=<tên_người_dùng>
   spring.datasource.password=<mật_khẩu>
   spring.mail.username=<email_username>
   spring.mail.password=<email_password>
   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true


### Ghi Chú
- Hãy chắc chắn rằng bạn đã thay đổi các thông tin như `<tên_cơ_sở_dữ_liệu>`, `<tên_người_dùng>`, `<mật_khẩu>`, `<email_username>`, và `<email_password>` với thông tin thực tế của bạn.
- Đảm bảo bạn thay thế `email@example.com` bằng địa chỉ email thực tế của người nhận trước khi chạy ứng dụng.
- Bạn có thể thêm hoặc chỉnh sửa các phần trong README.md dựa trên nhu cầu cụ thể của dự án hoặc người dùng.
