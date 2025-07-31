package com.example.companybackend.dto.response;

import com.example.companybackend.entity.TimeCorrection;

public class RejectTimeCorrectionResponse {
    private boolean success;
    private String message;
    private TimeCorrection correction;

    public RejectTimeCorrectionResponse() {}

    public RejectTimeCorrectionResponse(boolean success, String message, TimeCorrection correction) {
        this.success = success;
        this.message = message;
        this.correction = correction;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public TimeCorrection getCorrection() { return correction; }
    public void setCorrection(TimeCorrection correction) { this.correction = correction; }
}