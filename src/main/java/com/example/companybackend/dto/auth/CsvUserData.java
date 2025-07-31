package com.example.companybackend.dto.auth;

import lombok.Data;

/**
 * CSVユーザー情報DTO
 */
@Data
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
    public CsvUserData() {}
    
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
}