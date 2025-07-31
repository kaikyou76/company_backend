package com.example.companybackend.dto.response;

import com.example.companybackend.entity.TimeCorrection;

public class CreateTimeCorrectionResponse {
    private boolean success;
    private String message;
    private TimeCorrection timeCorrection;

    public CreateTimeCorrectionResponse() {}

    public CreateTimeCorrectionResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public CreateTimeCorrectionResponse(boolean success, String message, TimeCorrection timeCorrection) {
        this.success = success;
        this.message = message;
        this.timeCorrection = timeCorrection;
    }

    public static CreateTimeCorrectionResponse success(TimeCorrection timeCorrection) {
        return new CreateTimeCorrectionResponse(true, "打刻修正申請を作成しました", timeCorrection);
    }

    public static CreateTimeCorrectionResponse error(String message) {
        return new CreateTimeCorrectionResponse(false, message);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TimeCorrection getTimeCorrection() {
        return timeCorrection;
    }

    public void setTimeCorrection(TimeCorrection timeCorrection) {
        this.timeCorrection = timeCorrection;
    }
}