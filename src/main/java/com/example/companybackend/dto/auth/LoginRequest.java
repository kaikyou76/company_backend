package com.example.companybackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ログインリクエストDTO
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "社員コードは必須です")
    private String employeeCode;
    
    @NotBlank(message = "パスワードは必須です")
    private String password;
    
    // Getters and Setters
    public String getEmployeeCode() {
        return employeeCode;
    }
    
    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}