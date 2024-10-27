package com.demo.services;

public interface MailService {

    public boolean send(String from, String to, String subject, String content);
}
