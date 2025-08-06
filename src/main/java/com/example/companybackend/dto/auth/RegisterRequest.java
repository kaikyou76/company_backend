package com.example.companybackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 登録リクエストDTO
 * comsys_dump.sql usersテーブル準拠
 */
public class RegisterRequest {

    @NotBlank(message = "ユーザー名は必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String username;

    @NotBlank(message = "パスワードは必須です")
    private String password;

    @NotBlank(message = "パスワード確認は必須です")
    private String confirmPassword;

    @NotBlank(message = "氏名は必須です")
    private String fullName;

    @NotBlank(message = "勤務地タイプは必須です")
    @Pattern(regexp = "office|client", message = "勤務地タイプは'office'または'client'である必要があります")
    private String locationType;

    private Double clientLatitude;
    private Double clientLongitude;
    private Integer managerId;
    private Integer departmentId;
    private Integer positionId;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public Double getClientLatitude() {
        return clientLatitude;
    }

    public void setClientLatitude(Double clientLatitude) {
        this.clientLatitude = clientLatitude;
    }

    public Double getClientLongitude() {
        return clientLongitude;
    }

    public void setClientLongitude(Double clientLongitude) {
        this.clientLongitude = clientLongitude;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getPositionId() {
        return positionId;
    }

    public void setPositionId(Integer positionId) {
        this.positionId = positionId;
    }
}