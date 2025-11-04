package com.emp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LeaveManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeaveManagementApplication.class, args);
        System.out.println("Leave Management Application started on port 8080");
    }
}
