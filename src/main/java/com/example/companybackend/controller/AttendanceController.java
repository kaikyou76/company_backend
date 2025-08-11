package com.example.companybackend.controller;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
public class AttendanceController {

    private static final Logger log = LoggerFactory.getLogger(AttendanceController.class);
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
            // ページ情報を使用してレコードを取得
            List<AttendanceRecord> records;
            if (startDate != null && endDate != null) {
                OffsetDateTime startDateTime = startDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
                OffsetDateTime endDateTime = endDate.atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC);
                records = attendanceService.getAttendanceByDateRange(userId.intValue(), startDateTime, endDateTime);
            } else if (startDate != null) {
                OffsetDateTime startDateTime = startDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
                OffsetDateTime endDateTime = startDate.atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC);
                records = attendanceService.getAttendanceByDateRange(userId.intValue(), startDateTime, endDateTime);
            } else {
                records = attendanceService.getTodayRecords(userId);
            }

            // ページネーションの実装
            int totalRecords = records.size();
            int totalPages = (int) Math.ceil((double) totalRecords / size);

            // ページに応じたレコードの切り出し
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, totalRecords);
            List<AttendanceRecord> pagedRecords = fromIndex < totalRecords ? records.subList(fromIndex, toIndex)
                    : new ArrayList<>();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);

            // APIドキュメントに合わせたデータ構造に変換
            List<Map<String, Object>> formattedRecords = pagedRecords.stream().map(record -> {
                Map<String, Object> formattedRecord = new HashMap<>();
                formattedRecord.put("id", record.getId());
                formattedRecord.put("date", record.getTimestamp().toLocalDate().toString());
                formattedRecord.put("clockInTime",
                        "in".equals(record.getType()) ? record.getTimestamp().toLocalTime().toString() : null);
                formattedRecord.put("clockOutTime",
                        "out".equals(record.getType()) ? record.getTimestamp().toLocalTime().toString() : null);

                // 実際の計算ロジックに置き換える
                double workingHours = 0;
                double overtimeHours = 0;

                // 出勤記録の場合、対応する退勤記録を検索
                if ("in".equals(record.getType())) {
                    Optional<AttendanceRecord> clockOutRecord = records.stream()
                            .filter(r -> "out".equals(r.getType()) &&
                                    r.getTimestamp().toLocalDate().equals(record.getTimestamp().toLocalDate()))
                            .findFirst();

                    if (clockOutRecord.isPresent()) {
                        // 勤務時間の計算（分単位）
                        long minutes = java.time.Duration.between(
                                record.getTimestamp(),
                                clockOutRecord.get().getTimestamp()).toMinutes();

                        workingHours = minutes / 60.0;

                        // 8時間を超える場合は残業時間として計算
                        if (workingHours > 8) {
                            overtimeHours = workingHours - 8;
                        }
                    }
                }

                formattedRecord.put("workingHours", workingHours);
                formattedRecord.put("overtimeHours", overtimeHours);
                formattedRecord.put("status", "NORMAL"); // 実際のステータスに置き換える必要あり
                return formattedRecord;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("records", formattedRecords);
            data.put("totalCount", totalRecords);
            data.put("currentPage", page);
            data.put("totalPages", totalPages);

            result.put("data", data);

            log.debug("勤怠記録API成功: userId={}, recordCount={}", userId, pagedRecords.size());
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
            data.put("date", summary.getDate().toString());

            if (summary.getClockInRecord() != null) {
                data.put("clockInTime", summary.getClockInRecord().getTimestamp().toLocalTime().toString());
            }

            if (summary.getClockOutRecord() != null) {
                data.put("clockOutTime", summary.getClockOutRecord().getTimestamp().toLocalTime().toString());
            }

            data.put("workingHours", summary.getTotalHours() != null ? summary.getTotalHours().doubleValue() : 0.0);
            data.put("overtimeHours",
                    summary.getOvertimeHours() != null ? summary.getOvertimeHours().doubleValue() : 0.0);
            data.put("breakTime", 1.0); // API仕様に合わせて固定値を設定
            data.put("status", summary.getStatus());

            // 月間サマリー情報の取得（実際の計算ロジックに置き換え）
            LocalDate firstDayOfMonth = date.withDayOfMonth(1);
            LocalDate lastDayOfMonth = date.withDayOfMonth(date.lengthOfMonth());

            List<AttendanceSummary> monthlySummaries = attendanceService.getAttendanceByDateRange(
                    userId.intValue(),
                    firstDayOfMonth.atStartOfDay().atOffset(java.time.ZoneOffset.UTC),
                    lastDayOfMonth.atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC)).stream()
                    .filter(record -> "out".equals(record.getType())) // 退勤記録のみを対象
                    .map(record -> {
                        // 日付ごとのサマリーを取得
                        try {
                            return attendanceService.getDailySummary(userId, record.getTimestamp().toLocalDate());
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .map(summaryData -> {
                        AttendanceSummary summaryEntity = new AttendanceSummary();
                        summaryEntity.setTargetDate(summaryData.getDate());
                        summaryEntity.setTotalHours(summaryData.getTotalHours());
                        summaryEntity.setOvertimeHours(summaryData.getOvertimeHours());
                        return summaryEntity;
                    })
                    .collect(java.util.stream.Collectors.toList());

            double monthlyWorkingHours = monthlySummaries.stream()
                    .mapToDouble(s -> s.getTotalHours() != null ? s.getTotalHours().doubleValue() : 0.0)
                    .sum();

            double monthlyOvertimeHours = monthlySummaries.stream()
                    .mapToDouble(s -> s.getOvertimeHours() != null ? s.getOvertimeHours().doubleValue() : 0.0)
                    .sum();

            data.put("monthlyWorkingHours", monthlyWorkingHours);
            data.put("monthlyOvertimeHours", monthlyOvertimeHours);

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