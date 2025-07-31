package com.example.companybackend.controller;

import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.service.AttendanceSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 勤怠サマリー管理コントローラー
 * Spring Boot 3.x対応 - Jakarta EE準拠
 * API Endpoints:
 * - GET /api/reports/attendance/daily
 * - GET /api/reports/attendance/monthly
 * - GET /api/reports/attendance/overtime
 * - GET /api/reports/attendance/statistics
 * - GET /api/reports/attendance/export
 */
@RestController
@RequestMapping("/api/reports/attendance")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AttendanceSummaryController {

    private final AttendanceSummaryService attendanceSummaryService;

    /**
     * 日別勤務時間サマリー取得 API
     * GET /api/reports/attendance/daily
     */
    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> getDailySummaries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.debug("日別勤務時間サマリー取得API呼び出し: startDate={}, endDate={}", startDate, endDate);
        
        try {
            Page<AttendanceSummary> summaries = attendanceSummaryService.getDailySummaries(startDate, endDate, pageable);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            Map<String, Object> data = new HashMap<>();
            data.put("summaries", summaries.getContent());
            data.put("totalCount", summaries.getTotalElements());
            data.put("currentPage", summaries.getNumber());
            data.put("totalPages", summaries.getTotalPages());
            
            result.put("data", data);
            
            log.debug("日別勤務時間サマリー取得API成功: recordCount={}", summaries.getContent().size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("日別勤務時間サマリー取得API例外: startDate={}, endDate={}", startDate, endDate, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "日別勤務時間サマリーの取得に失敗しました");
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 月別勤務時間サマリー取得 API
     * GET /api/reports/attendance/monthly
     */
    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlySummaries(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.debug("月別勤務時間サマリー取得API呼び出し: yearMonth={}", yearMonth);
        
        try {
            Page<AttendanceSummary> summaries = attendanceSummaryService.getMonthlySummaries(yearMonth, pageable);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            Map<String, Object> data = new HashMap<>();
            data.put("summaries", summaries.getContent());
            data.put("totalCount", summaries.getTotalElements());
            data.put("currentPage", summaries.getNumber());
            data.put("totalPages", summaries.getTotalPages());
            data.put("targetMonth", yearMonth.toString());
            
            result.put("data", data);
            
            log.debug("月別勤務時間サマリー取得API成功: recordCount={}", summaries.getContent().size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("月別勤務時間サマリー取得API例外: yearMonth={}", yearMonth, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "月別勤務時間サマリーの取得に失敗しました");
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 残業時間統計取得 API
     * GET /api/reports/attendance/overtime
     */
    @GetMapping("/overtime")
    public ResponseEntity<Map<String, Object>> getOvertimeStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.debug("残業時間統計取得API呼び出し: startDate={}, endDate={}", startDate, endDate);
        
        try {
            Map<String, Object> statistics = attendanceSummaryService.getSummaryStatistics(startDate, endDate);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            Map<String, Object> data = new HashMap<>();
            data.put("statistics", statistics);
            data.put("startDate", startDate);
            data.put("endDate", endDate);
            
            result.put("data", data);
            
            log.debug("残業時間統計取得API成功");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("残業時間統計取得API例外: startDate={}, endDate={}", startDate, endDate, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "残業時間統計の取得に失敗しました");
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 勤務統計レポート取得 API
     * GET /api/reports/attendance/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getSummaryStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.debug("勤務統計レポート取得API呼び出し: startDate={}, endDate={}", startDate, endDate);
        
        try {
            Map<String, Object> statistics = attendanceSummaryService.getSummaryStatistics(startDate, endDate);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            Map<String, Object> data = new HashMap<>();
            data.put("statistics", statistics);
            data.put("startDate", startDate);
            data.put("endDate", endDate);
            
            result.put("data", data);
            
            log.debug("勤務統計レポート取得API成功");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("勤務統計レポート取得API例外: startDate={}, endDate={}", startDate, endDate, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "勤務統計レポートの取得に失敗しました");
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 勤務時間データエクスポート API
     * GET /api/reports/attendance/export
     */
    @GetMapping("/export")
    public void exportAttendanceData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "csv") String format,
            HttpServletResponse response) throws IOException {
        
        log.debug("勤務時間データエクスポートAPI呼び出し: startDate={}, endDate={}, format={}", startDate, endDate, format);
        
        try {
            response.setCharacterEncoding("UTF-8");
            if ("json".equalsIgnoreCase(format)) {
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Content-Disposition", 
                    String.format("attachment; filename=\"attendance_data_%s_%s.json\"", startDate, endDate));
            } else {
                response.setContentType("text/csv;charset=UTF-8");
                response.setHeader("Content-Disposition", 
                    String.format("attachment; filename=\"attendance_data_%s_%s.csv\"", startDate, endDate));
            }
            
            PrintWriter writer = response.getWriter();
            try {
                if ("json".equalsIgnoreCase(format)) {
                    attendanceSummaryService.exportSummariesToJSON(
                        attendanceSummaryService.getSummariesForExport(startDate, endDate), writer);
                } else {
                    attendanceSummaryService.exportSummariesToCSV(
                        attendanceSummaryService.getSummariesForExport(startDate, endDate), writer);
                }
                
                log.debug("勤務時間データエクスポートAPI成功");
            } finally {
                writer.flush();
            }
        } catch (Exception e) {
            log.error("勤務時間データエクスポートAPI例外: startDate={}, endDate={}, format={}", startDate, endDate, format, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            writer.write("{\"success\": false, \"message\": \"勤務時間データのエクスポートに失敗しました\"}");
            writer.flush();
        }
    }

    /**
     * 日別サマリー計算 API
     * POST /api/reports/attendance/daily/calculate
     */
    @PostMapping("/daily/calculate")
    public ResponseEntity<Map<String, Object>> calculateDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
        
        log.debug("日別サマリー計算API呼び出し: targetDate={}", targetDate);
        
        try {
            AttendanceSummary summary = attendanceSummaryService.generateDailySummary(targetDate);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "日別サマリーの計算が完了しました");
            
            Map<String, Object> data = new HashMap<>();
            data.put("summary", summary);
            data.put("targetDate", targetDate);
            
            result.put("data", data);
            
            log.debug("日別サマリー計算API成功: summaryId={}", summary != null ? summary.getId() : null);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("日別サマリー計算API例外: targetDate={}", targetDate, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "日別サマリーの計算に失敗しました");
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 月別サマリー計算 API
     * POST /api/reports/attendance/monthly/calculate
     */
    @PostMapping("/monthly/calculate")
    public ResponseEntity<Map<String, Object>> calculateMonthlySummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        
        log.debug("月別サマリー計算API呼び出し: yearMonth={}", yearMonth);
        
        try {
            // 月別サマリーの計算ロジックを実装
            Map<String, Object> monthlyStats = attendanceSummaryService.getMonthlyStatistics(
                yearMonth.atDay(1), 
                yearMonth.atEndOfMonth());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "月別サマリーの計算が完了しました");
            
            Map<String, Object> data = new HashMap<>();
            data.put("statistics", monthlyStats);
            data.put("targetMonth", yearMonth.toString());
            
            result.put("data", data);
            
            log.debug("月別サマリー計算API成功");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("月別サマリー計算API例外: yearMonth={}", yearMonth, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "月別サマリーの計算に失敗しました");
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
}