package com.emp.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String password;
    private String name; // optional for register
    
    // Adding explicit getter methods
    public String getEmail() {
        return email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getName() {
        return name;
    }
}
