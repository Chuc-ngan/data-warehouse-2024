package com.example.demo.service.emailService;

import java.io.File;

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
}