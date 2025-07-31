package com.example.companybackend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "overtime_reports")
public class OvertimeReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "target_month", nullable = false)
    private LocalDate targetMonth;

    @Column(name = "total_overtime", nullable = false)
    private BigDecimal totalOvertime;

    @Column(name = "total_late_night", nullable = false)
    private BigDecimal totalLateNight;

    @Column(name = "total_holiday", nullable = false)
    private BigDecimal totalHoliday;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public OvertimeReport() {}

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

    public LocalDate getTargetMonth() {
        return targetMonth;
    }

    public void setTargetMonth(LocalDate targetMonth) {
        this.targetMonth = targetMonth;
    }

    public BigDecimal getTotalOvertime() {
        return totalOvertime;
    }

    public void setTotalOvertime(BigDecimal totalOvertime) {
        this.totalOvertime = totalOvertime;
    }

    public BigDecimal getTotalLateNight() {
        return totalLateNight;
    }

    public void setTotalLateNight(BigDecimal totalLateNight) {
        this.totalLateNight = totalLateNight;
    }

    public BigDecimal getTotalHoliday() {
        return totalHoliday;
    }

    public void setTotalHoliday(BigDecimal totalHoliday) {
        this.totalHoliday = totalHoliday;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}