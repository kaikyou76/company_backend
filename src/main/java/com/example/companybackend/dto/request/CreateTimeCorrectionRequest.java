package com.example.companybackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public class CreateTimeCorrectionRequest {
    
    @NotBlank(message = "申請タイプは必須です")
    private String requestType; // "time", "type", "both"
    
    @NotNull(message = "対象となる打刻IDは必須です")
    private Long attendanceId;
    
    @NotBlank(message = "現在の打刻タイプは必須です")
    private String currentType; // "in", "out"
    
    private OffsetDateTime requestedTime;
    
    private String requestedType; // "in", "out"
    
    @NotBlank(message = "修正理由は必須です")
    private String reason;

    // Getters and Setters
    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public Long getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public String getCurrentType() {
        return currentType;
    }

    public void setCurrentType(String currentType) {
        this.currentType = currentType;
    }

    public OffsetDateTime getRequestedTime() {
        return requestedTime;
    }

    public void setRequestedTime(OffsetDateTime requestedTime) {
        this.requestedTime = requestedTime;
    }

    public String getRequestedType() {
        return requestedType;
    }

    public void setRequestedType(String requestedType) {
        this.requestedType = requestedType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}