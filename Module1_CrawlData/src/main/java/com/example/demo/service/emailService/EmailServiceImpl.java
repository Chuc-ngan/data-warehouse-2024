package com.example.demo.service.emailService;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.example.demo.model.EmailDetails;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender emailSender;

    public EmailServiceImpl(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Value("${spring.mail.username}") private String sender;

    public String sendSuccessEmail(String recipient, String outputCsvFilePath, int productCount) {
        String subject = "Thông báo lưu dữ liệu thành công";
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

        String body = "<html>" +
                "<body>" +
                "<h2 style='color:green;'>Dữ liệu sản phẩm đã được lưu thành công!</h2>" +
                "<p style='font-size:16px;'>File lưu trữ: <strong>" + outputCsvFilePath + "</strong></p>" +
                "<p style='font-size:16px;'>Thời gian crawl: <strong>" + currentTime + "</strong></p>" +
                "<p style='font-size:16px;'>Số lượng sản phẩm đã lưu: <strong>" + productCount + "</strong></p>" +
                "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!</p>" +
                "</body>" +
                "</html>";

        EmailDetails details = new EmailDetails(recipient, body, subject, "");
        return sendHtmlEmail(details);
    }

    public String sendFailureEmail(String recipient, String errorMessage) {
        String subject = "Thông báo lỗi trong quá trình lưu dữ liệu";
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

        String body = "<html>" +
                "<body>" +
                "<h2 style='color:red;'>Đã xảy ra lỗi trong quá trình lưu dữ liệu</h2>" +
                "<p style='font-size:16px;'>Mô tả lỗi: <strong>" + errorMessage + "</strong></p>" +
                "<p style='font-size:16px;'>Thời gian xảy ra lỗi: <strong>" + currentTime + "</strong></p>" +
                "<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!</p>" +
                "</body>" +
                "</html>";

        EmailDetails details = new EmailDetails(recipient, body, subject, "");
        return sendHtmlEmail(details);
    }

    public String sendSimpleMail(EmailDetails details)
    {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();

            mailMessage.setFrom(sender);
            mailMessage.setTo(details.getRecipient());
            mailMessage.setText(details.getMsgBody());
            mailMessage.setSubject(details.getSubject());

            // Sending the mail
            emailSender.send(mailMessage);
            return "Mail Sent Successfully...";
        }

        catch (Exception e) {
            e.printStackTrace(); // In thông tin chi tiết lỗi
            return "Error while Sending Mail: " + e.getMessage();
        }
    }

    public String sendMailWithAttachment(EmailDetails details) {
        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper;
        try {
            mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setFrom(sender);
            mimeMessageHelper.setTo(details.getRecipient());
            mimeMessageHelper.setText(details.getMsgBody());
            mimeMessageHelper.setSubject(details.getSubject());

            FileSystemResource file = new FileSystemResource(new File(details.getAttachment()));
            mimeMessageHelper.addAttachment(file.getFilename(), file);

            emailSender.send(mimeMessage);
            return "Mail sent Successfully";
        }

        catch (Exception e) {
            e.printStackTrace();
            return "Error while Sending Mail: " + e.getMessage();
        }
    }
    public String sendHtmlEmail(EmailDetails details) {
        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper;

        try {
            mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setFrom(sender);
            mimeMessageHelper.setTo(details.getRecipient());
            mimeMessageHelper.setText(details.getMsgBody(), true);
            mimeMessageHelper.setSubject(details.getSubject());

            emailSender.send(mimeMessage);
            return "Mail sent Successfully";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error while Sending Mail: " + e.getMessage();
        }
    }
}