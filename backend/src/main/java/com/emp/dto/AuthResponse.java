package com.emp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String role;
    
    // Adding explicit setter methods
    public void setToken(String token) {
        this.token = token;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
}
