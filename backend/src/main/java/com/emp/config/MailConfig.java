package com.emp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {
    
    @Bean
    public JavaMailSender javaMailSender() {
        // Create a simple implementation for development
        return new JavaMailSenderImpl();
    }
}