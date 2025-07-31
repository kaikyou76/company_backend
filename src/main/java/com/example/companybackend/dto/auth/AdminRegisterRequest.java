package com.example.companybackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminRegisterRequest {

    @NotBlank(message = "ユーザー名（メールアドレス）は必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String username;

    @NotBlank(message = "パスワードは必須です")
    @Size(min = 6, max = 100, message = "パスワードは6文字以上100文字以下である必要があります")
    private String password;

    @NotBlank(message = "パスワード確認は必須です")
    private String confirmPassword;

    @NotBlank(message = "氏名は必須です")
    private String fullName;

    @NotNull(message = "管理者役職の指定は必須です")
    private Integer positionId;

    private String locationType = "office";

    private Double clientLatitude;

    private Double clientLongitude;

    private Integer departmentId;

    private Integer managerId;

    private String adminNotes;

    public AdminRegisterRequest() {}

    public AdminRegisterRequest(String username, String password, String confirmPassword, Integer positionId) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.positionId = positionId;
    }

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

    public Integer getPositionId() {
        return positionId;
    }

    public void setPositionId(Integer positionId) {
        this.positionId = positionId;
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

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    @Override
    public String toString() {
        return "AdminRegisterRequest{" +
                "username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", positionId=" + positionId +
                ", locationType=" + locationType +
                ", departmentId=" + departmentId +
                ", managerId=" + managerId +
                ", adminNotes='" + adminNotes + '\'' +
                '}';
    }
}