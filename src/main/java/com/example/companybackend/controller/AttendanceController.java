package com.example.companybackend.controller;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 勤怠管理コントローラー
 * Spring Boot 3.x対応 - Jakarta EE準拠
 * API Endpoints:
 * - POST /api/v1/attendance/clock-in
 * - POST /api/v1/attendance/clock-out
 * - GET /api/v1/attendance/today
 * - GET /api/v1/attendance/status
 * - GET /api/v1/attendance/summary/daily
 */
@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * 出勤打刻 API
     * POST /api/v1/attendance/clock-in
     */
    @PostMapping("/clock-in")
    public ResponseEntity<AttendanceService.ClockInResponse> clockIn(
            @Valid @RequestBody AttendanceService.ClockInRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("出勤打刻API呼び出し: userId={}, location=({}, {})", 
                userId, request.getLatitude(), request.getLongitude());
        
        try {
            AttendanceService.ClockInResponse response = attendanceService.clockIn(request, userId);
            
            if (response.isSuccess()) {
                log.info("出勤打刻API成功: userId={}, recordId={}", userId, response.getRecord().getId());
                return ResponseEntity.ok(response);
            } else {
                log.warn("出勤打刻API失敗: userId={}, error={}", userId, response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("出勤打刻API例外: userId={}", userId, e);
            AttendanceService.ClockInResponse errorResponse = 
                AttendanceService.ClockInResponse.error("システムエラーが発生しました");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 退勤打刻 API
     * POST /api/v1/attendance/clock-out
     */
    @PostMapping("/clock-out")
    public ResponseEntity<AttendanceService.ClockOutResponse> clockOut(
            @Valid @RequestBody AttendanceService.ClockOutRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("退勤打刻API呼び出し: userId={}, location=({}, {})", 
                userId, request.getLatitude(), request.getLongitude());
        
        try {
            AttendanceService.ClockOutResponse response = attendanceService.clockOut(request, userId);
            
            if (response.isSuccess()) {
                log.info("退勤打刻API成功: userId={}, recordId={}", userId, response.getRecord().getId());
                return ResponseEntity.ok(response);
            } else {
                log.warn("退勤打刻API失敗: userId={}, error={}", userId, response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("退勤打刻API例外: userId={}", userId, e);
            AttendanceService.ClockOutResponse errorResponse = 
                AttendanceService.ClockOutResponse.error("システムエラーが発生しました");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 今日の勤怠記録取得 API
     * GET /api/v1/attendance/today
     */
    @GetMapping("/today")
    public ResponseEntity<TodayAttendanceResponse> getTodayAttendance(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.debug("今日の勤怠記録API呼び出し: userId={}", userId);
        
        try {
            List<AttendanceRecord> records = attendanceService.getTodayRecords(userId);
            
            TodayAttendanceResponse response = new TodayAttendanceResponse();
            response.setUserId(userId);
            response.setDate(LocalDate.now());
            response.setRecords(records);
            response.setRecordCount(records.size());
            
            // 出勤・退勤状況判定
            boolean hasClockIn = records.stream().anyMatch(r -> "in".equals(r.getType()));
            boolean hasClockOut = records.stream().anyMatch(r -> "out".equals(r.getType()));
            
            if (!hasClockIn) {
                response.setStatus("未出勤");
            } else if (!hasClockOut) {
                response.setStatus("勤務中");
            } else {
                response.setStatus("勤務完了");
            }
            
            log.debug("今日の勤怠記録API成功: userId={}, recordCount={}, status={}", 
                     userId, records.size(), response.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("今日の勤怠記録API例外: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 勤怠状況取得 API
     * GET /api/v1/attendance/status
     */
    @GetMapping("/status")
    public ResponseEntity<AttendanceStatusResponse> getAttendanceStatus(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.debug("勤怠状況API呼び出し: userId={}", userId);
        
        try {
            var latestRecord = attendanceService.getLatestRecord(userId);
            var todayRecords = attendanceService.getTodayRecords(userId);
            
            AttendanceStatusResponse response = new AttendanceStatusResponse();
            response.setUserId(userId);
            response.setLatestRecord(latestRecord.orElse(null));
            response.setTodayRecordCount(todayRecords.size());
            
            // 現在の状況判定
            if (latestRecord.isPresent()) {
                AttendanceRecord latest = latestRecord.get();
                response.setCurrentStatus("in".equals(latest.getType()) ? "勤務中" : "退勤済み");
                response.setLastActionTime(latest.getTimestamp());
            } else {
                response.setCurrentStatus("未出勤");
            }
            
            // 次のアクション判定
            boolean canClockIn = latestRecord.isEmpty() || 
                               "out".equals(latestRecord.get().getType());
            boolean canClockOut = latestRecord.isPresent() && 
                                "in".equals(latestRecord.get().getType());
            
            response.setCanClockIn(canClockIn);
            response.setCanClockOut(canClockOut);
            
            log.debug("勤怠状況API成功: userId={}, status={}, canClockIn={}, canClockOut={}", 
                     userId, response.getCurrentStatus(), canClockIn, canClockOut);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("勤怠状況API例外: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 日次サマリー取得 API
     * GET /api/v1/attendance/summary/daily
     */
    @GetMapping("/summary/daily")
    public ResponseEntity<AttendanceService.DailySummaryData> getDailySummary(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(name = "date", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        if (date == null) {
            date = LocalDate.now();
        }
        
        log.debug("日次サマリーAPI呼び出し: userId={}, date={}", userId, date);
        
        try {
            AttendanceService.DailySummaryData summary = attendanceService.getDailySummary(userId, date);
            
            log.debug("日次サマリーAPI成功: userId={}, date={}, status={}", 
                     userId, date, summary.getStatus());
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            log.error("日次サマリーAPI例外: userId={}, date={}", userId, date, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Response DTOクラス
    public static class TodayAttendanceResponse {
        private Long userId;
        private LocalDate date;
        private List<AttendanceRecord> records;
        private Integer recordCount;
        private String status;
        
        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public List<AttendanceRecord> getRecords() { return records; }
        public void setRecords(List<AttendanceRecord> records) { this.records = records; }
        public Integer getRecordCount() { return recordCount; }
        public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class AttendanceStatusResponse {
        private Long userId;
        private AttendanceRecord latestRecord;
        private String currentStatus;
        private java.time.OffsetDateTime lastActionTime;
        private Integer todayRecordCount;
        private Boolean canClockIn;
        private Boolean canClockOut;
        
        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public AttendanceRecord getLatestRecord() { return latestRecord; }
        public void setLatestRecord(AttendanceRecord latestRecord) { this.latestRecord = latestRecord; }
        public String getCurrentStatus() { return currentStatus; }
        public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
        public java.time.OffsetDateTime getLastActionTime() { return lastActionTime; }
        public void setLastActionTime(java.time.OffsetDateTime lastActionTime) { this.lastActionTime = lastActionTime; }
        public Integer getTodayRecordCount() { return todayRecordCount; }
        public void setTodayRecordCount(Integer todayRecordCount) { this.todayRecordCount = todayRecordCount; }
        public Boolean getCanClockIn() { return canClockIn; }
        public void setCanClockIn(Boolean canClockIn) { this.canClockIn = canClockIn; }
        public Boolean getCanClockOut() { return canClockOut; }
        public void setCanClockOut(Boolean canClockOut) { this.canClockOut = canClockOut; }
    }
}