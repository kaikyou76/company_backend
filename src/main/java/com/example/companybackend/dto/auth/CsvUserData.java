package com.example.companybackend.dto.auth;

/**
 * CSVユーザー情報DTO
 */
public class CsvUserData {
    private String username;
    private String password;
    private String fullName;
    private String locationType;
    private Double clientLatitude;
    private Double clientLongitude;
    private Integer departmentId;
    private Integer positionId;
    private Integer managerId;

    // コンストラクタ
    public CsvUserData() {
    }

    public CsvUserData(String username, String password, String fullName, String locationType,
            Double clientLatitude, Double clientLongitude,
            Integer departmentId, Integer positionId, Integer managerId) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.locationType = locationType;
        this.clientLatitude = clientLatitude;
        this.clientLongitude = clientLongitude;
        this.departmentId = departmentId;
        this.positionId = positionId;
        this.managerId = managerId;
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

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }
}