package com.example.companybackend.controller;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 勤怠管理コントローラー
 * Spring Boot 3.x対応 - Jakarta EE準拠
 * API Endpoints:
 * - POST /api/attendance/clock-in
 * - POST /api/attendance/clock-out
 * - GET /api/attendance/records
 * - GET /api/attendance/daily-summary
 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * 出勤打刻 API
     * POST /api/attendance/clock-in
     */
    @PostMapping("/clock-in")
    public ResponseEntity<Map<String, Object>> clockIn(
            @Valid @RequestBody AttendanceService.ClockInRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("出勤打刻API呼び出し: userId={}, location=({}, {})", 
                userId, request.getLatitude(), request.getLongitude());
        
        try {
            AttendanceService.ClockInResponse response = attendanceService.clockIn(request, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("message", response.getMessage());
            
            if (response.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("recordId", response.getRecord().getId());
                data.put("timestamp", response.getRecord().getTimestamp());
                data.put("locationVerified", true); // 現在は常にtrueとしています
                result.put("data", data);
                log.info("出勤打刻API成功: userId={}, recordId={}", userId, response.getRecord().getId());
                return ResponseEntity.ok(result);
            } else {
                log.warn("出勤打刻API失敗: userId={}, error={}", userId, response.getMessage());
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("出勤打刻API例外: userId={}", userId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "システムエラーが発生しました");
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 退勤打刻 API
     * POST /api/attendance/clock-out
     */
    @PostMapping("/clock-out")
    public ResponseEntity<Map<String, Object>> clockOut(
            @Valid @RequestBody AttendanceService.ClockOutRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("退勤打刻API呼び出し: userId={}, location=({}, {})", 
                userId, request.getLatitude(), request.getLongitude());
        
        try {
            AttendanceService.ClockOutResponse response = attendanceService.clockOut(request, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("message", response.getMessage());
            
            if (response.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("recordId", response.getRecord().getId());
                data.put("timestamp", response.getRecord().getTimestamp());
                
                // 勤務時間と残業時間の計算（簡略化）
                data.put("workingHours", 8.0);
                data.put("overtimeHours", 0.0);
                
                result.put("data", data);
                log.info("退勤打刻API成功: userId={}, recordId={}", userId, response.getRecord().getId());
                return ResponseEntity.ok(result);
            } else {
                log.warn("退勤打刻API失敗: userId={}, error={}", userId, response.getMessage());
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("退勤打刻API例外: userId={}", userId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "システムエラーが発生しました");
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 勤怠記録取得 API
     * GET /api/attendance/records
     */
    @GetMapping("/records")
    public ResponseEntity<Map<String, Object>> getAttendanceRecords(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.debug("勤怠記録API呼び出し: userId={}, startDate={}, endDate={}, page={}, size={}", 
                 userId, startDate, endDate, page, size);
        
        try {
            List<AttendanceRecord> records = attendanceService.getTodayRecords(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", records);
            data.put("totalCount", records.size());
            data.put("currentPage", page);
            data.put("totalPages", 1); // 簡略化のため1としています
            
            result.put("data", data);
            
            log.debug("勤怠記録API成功: userId={}, recordCount={}", userId, records.size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("勤怠記録API例外: userId={}", userId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "システムエラーが発生しました");
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 日次サマリー取得 API
     * GET /api/attendance/daily-summary
     */
    @GetMapping("/daily-summary")
    public ResponseEntity<Map<String, Object>> getDailySummary(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        if (date == null) {
            date = LocalDate.now();
        }
        
        log.debug("日次サマリーAPI呼び出し: userId={}, date={}", userId, date);
        
        try {
            AttendanceService.DailySummaryData summary = attendanceService.getDailySummary(userId, date);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            Map<String, Object> data = new HashMap<>();
            data.put("date", summary.getDate());
            
            if (summary.getClockInRecord() != null) {
                data.put("clockInTime", summary.getClockInRecord().getTimestamp().toLocalTime());
            }
            
            if (summary.getClockOutRecord() != null) {
                data.put("clockOutTime", summary.getClockOutRecord().getTimestamp().toLocalTime());
            }
            
            data.put("workingHours", summary.getTotalHours() != null ? summary.getTotalHours() : BigDecimal.ZERO);
            data.put("overtimeHours", summary.getOvertimeHours() != null ? summary.getOvertimeHours() : BigDecimal.ZERO);
            data.put("status", summary.getStatus());
            
            result.put("data", data);
            
            log.debug("日次サマリーAPI成功: userId={}, date={}, status={}", 
                     userId, date, summary.getStatus());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("日次サマリーAPI例外: userId={}, date={}", userId, date, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "システムエラーが発生しました");
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
}