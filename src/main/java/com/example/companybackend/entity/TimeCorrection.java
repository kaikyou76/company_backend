package com.example.companybackend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "time_corrections")
public class TimeCorrection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "attendance_id", nullable = false)
    private Long attendanceId;

    @Column(name = "request_type", nullable = false)
    private String requestType;

    @Column(name = "before_time", nullable = false)
    private OffsetDateTime beforeTime;

    @Column(name = "current_type", nullable = false)
    private String currentType;

    @Column(name = "requested_time")
    private OffsetDateTime requestedTime;

    @Column(name = "requested_type")
    private String requestedType;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "approver_id")
    private Integer approverId;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public TimeCorrection() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Long getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public OffsetDateTime getBeforeTime() {
        return beforeTime;
    }

    public void setBeforeTime(OffsetDateTime beforeTime) {
        this.beforeTime = beforeTime;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getApproverId() {
        return approverId;
    }

    public void setApproverId(Integer approverId) {
        this.approverId = approverId;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 申请是否处于待审批状态
     * @return 如果是待审批状态返回true
     */
    public boolean isPending() {
        return "pending".equals(status);
    }
}