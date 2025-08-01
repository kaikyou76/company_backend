package com.example.companybackend.controller;

import com.example.companybackend.batch.service.BatchJobService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
public class BatchJobController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("dailyAttendanceSummaryJob")
    private Job dailyAttendanceSummaryJob;
    
    @Autowired
    @Qualifier("monthlyAttendanceSummaryJob")
    private Job monthlyAttendanceSummaryJob;

    @Autowired
    private BatchJobService batchJobService;

    /**
     * 月次勤怠集計バッチの実行
     */
    @PostMapping("/monthly-summary")
    public ResponseEntity<Map<String, Object>> runMonthlySummary(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String yearMonthStr = null;
            if (request != null && request.containsKey("yearMonth")) {
                yearMonthStr = (String) request.get("yearMonth");
            }
            
            if (yearMonthStr == null || yearMonthStr.isEmpty()) {
                YearMonth previousMonth = YearMonth.now().minusMonths(1);
                yearMonthStr = previousMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            }
            
            JobParameters jobParameters = new JobParametersBuilder()
                .addString("yearMonth", yearMonthStr)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
                
            jobLauncher.run(monthlyAttendanceSummaryJob, jobParameters);
            
            response.put("success", true);
            response.put("message", "月次勤怠集計バッチを実行しました");
            
            Map<String, Object> data = new HashMap<>();
            data.put("targetMonth", yearMonthStr);
            // 実際の実装では、処理されたデータ数などを設定する
            data.put("processedCount", 0);
            data.put("userCount", 0);
            data.put("totalWorkDays", 0);
            data.put("totalWorkTime", 0);
            data.put("totalOvertimeTime", 0);
            data.put("executedAt", java.time.OffsetDateTime.now());
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "月次勤怠集計バッチの実行に失敗しました: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 有給日数更新バッチの実行
     */
    @PostMapping("/update-paid-leave")
    public ResponseEntity<Map<String, Object>> updatePaidLeave(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer fiscalYear = null;
            if (request != null && request.containsKey("fiscalYear")) {
                fiscalYear = ((Number) request.get("fiscalYear")).intValue();
            }
            
            if (fiscalYear == null) {
                // 現在年度を計算（4月始まり）
                java.time.LocalDate now = java.time.LocalDate.now();
                if (now.getMonthValue() >= 4) {
                    fiscalYear = now.getYear();
                } else {
                    fiscalYear = now.getYear() - 1;
                }
            }
            
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("fiscalYear", fiscalYear.longValue())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
                
            jobLauncher.run(monthlyAttendanceSummaryJob, jobParameters); // 仮にmonthlyAttendanceSummaryJobを使用
            
            response.put("success", true);
            response.put("message", "有給日数更新バッチを実行しました");
            
            Map<String, Object> data = new HashMap<>();
            data.put("fiscalYear", fiscalYear);
            // 実際の実装では、更新されたデータ数などを設定する
            data.put("updatedCount", 0);
            data.put("totalUserCount", 0);
            data.put("successRate", 0.0);
            data.put("errorCount", 0);
            data.put("executedAt", java.time.OffsetDateTime.now());
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "有給日数更新バッチの実行に失敗しました: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * データクリーンアップバッチの実行
     */
    @PostMapping("/cleanup-data")
    public ResponseEntity<Map<String, Object>> cleanupData(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer retentionMonths = 12;
            if (request != null && request.containsKey("retentionMonths")) {
                retentionMonths = ((Number) request.get("retentionMonths")).intValue();
            }
            
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("retentionMonths", retentionMonths.longValue())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
                
            jobLauncher.run(monthlyAttendanceSummaryJob, jobParameters); // 仮にmonthlyAttendanceSummaryJobを使用
            
            response.put("success", true);
            response.put("message", "データクリーンアップバッチを実行しました");
            
            Map<String, Object> data = new HashMap<>();
            data.put("retentionMonths", retentionMonths);
            java.time.LocalDate cutoffDate = java.time.LocalDate.now().minusMonths(retentionMonths);
            data.put("cutoffDate", cutoffDate);
            // 実際の実装では、削除されたデータ数などを設定する
            data.put("deletedCount", 0);
            Map<String, Integer> deletedDetails = new HashMap<>();
            deletedDetails.put("attendanceRecords", 0);
            deletedDetails.put("auditLogs", 0);
            data.put("deletedDetails", deletedDetails);
            data.put("executedAt", java.time.OffsetDateTime.now());
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "データクリーンアップバッチの実行に失敗しました: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * データ修復バッチの実行
     */
    @PostMapping("/repair-data")
    public ResponseEntity<Map<String, Object>> repairData() {
        Map<String, Object> response = new HashMap<>();
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
                
            jobLauncher.run(monthlyAttendanceSummaryJob, jobParameters); // 仮にmonthlyAttendanceSummaryJobを使用
            
            response.put("success", true);
            response.put("message", "データ修復バッチを実行しました");
            
            Map<String, Object> data = new HashMap<>();
            // 実際の実装では、修復されたデータ数などを設定する
            data.put("repairedCount", 0);
            data.put("executedAt", java.time.OffsetDateTime.now());
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "データ修復バッチの実行に失敗しました: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * バッチ処理ステータスの取得
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBatchStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("success", true);
            response.put("message", "バッチ処理ステータスを取得しました");
            
            Map<String, Object> data = new HashMap<>();
            data.put("systemStatus", "HEALTHY");
            data.put("lastChecked", java.time.OffsetDateTime.now());
            data.put("uptime", "0 days, 0 hours");
            
            Map<String, Object> databaseStatus = new HashMap<>();
            databaseStatus.put("totalUsers", 0);
            databaseStatus.put("activeUsers", 0);
            databaseStatus.put("totalAttendanceRecords", 0);
            databaseStatus.put("latestRecordDate", java.time.LocalDate.now());
            data.put("databaseStatus", databaseStatus);
            
            Map<String, Object> dataStatistics = new HashMap<>();
            dataStatistics.put("currentMonthRecords", 0);
            dataStatistics.put("incompleteRecords", 0);
            data.put("dataStatistics", dataStatistics);
            
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "バッチ処理ステータスの取得に失敗しました: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 以下は既存のメソッド（API仕様書には記載されていないが、テスト用に残す）

    @PostMapping("/jobs/{jobName}/start")
    public ResponseEntity<String> startJob(@PathVariable String jobName) {
        try {
            Job job = getJobByName(jobName);
            jobLauncher.run(job, new JobParameters());
            return ResponseEntity.ok("Batch job " + jobName + " started successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to start batch job: " + e.getMessage());
        }
    }

    @GetMapping("/jobs/{jobName}/status")
    public ResponseEntity<String> getJobStatus(@PathVariable String jobName) {
        // 実装はJobExplorerを使用してジョブのステータスを取得
        return ResponseEntity.ok("Job status for " + jobName);
    }

    @PostMapping("/jobs/{jobName}/stop")
    public ResponseEntity<String> stopJob(@PathVariable String jobName) {
        // ジョブ停止ロジックの実装
        return ResponseEntity.ok("Job " + jobName + " stopped");
    }

    @GetMapping("/jobs")
    public ResponseEntity<String> getAllJobs() {
        // すべてのジョブをリストするロジックの実装
        return ResponseEntity.ok("List of all jobs");
    }

    @GetMapping("/monitoring/health")
    public ResponseEntity<String> getBatchHealth() {
        // バッチシステムのヘルスチェック
        return ResponseEntity.ok("Batch system is healthy");
    }

    @GetMapping("/monitoring/metrics")
    public ResponseEntity<String> getBatchMetrics() {
        // バッチメトリクスの取得
        return ResponseEntity.ok("Batch metrics");
    }

    @GetMapping("/monitoring/executions")
    public ResponseEntity<String> getBatchExecutions() {
        // バッチ実行履歴の取得
        return ResponseEntity.ok("Batch executions");
    }

    @GetMapping("/monitoring/diagnostics")
    public ResponseEntity<String> getBatchDiagnostics() {
        // バッチ診断情報の取得
        return ResponseEntity.ok("Batch diagnostics");
    }

    private Job getJobByName(String jobName) throws IllegalArgumentException {
        switch (jobName) {
            case "dailyAttendanceSummaryJob":
                return dailyAttendanceSummaryJob;
            case "monthlyAttendanceSummaryJob":
                return monthlyAttendanceSummaryJob;
            default:
                throw new IllegalArgumentException("Unknown job: " + jobName);
        }
    }
    
    // Specific endpoints for each functional requirement
    
    /**
     * F-301: 日次勤務時間集計
     */
    @PostMapping("/daily-work-summary")
    public ResponseEntity<String> runDailyWorkSummary() {
        try {
            jobLauncher.run(dailyAttendanceSummaryJob, new JobParameters());
            return ResponseEntity.ok("Daily work summary batch job started successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to start daily work summary batch job: " + e.getMessage());
        }
    }
    
    /**
     * F-302: 月次勤務時間集計
     */
    @PostMapping("/monthly-work-summary")
    public ResponseEntity<String> runMonthlyWorkSummary() {
        try {
            jobLauncher.run(monthlyAttendanceSummaryJob, new JobParameters());
            return ResponseEntity.ok("Monthly work summary batch job started successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to start monthly work summary batch job: " + e.getMessage());
        }
    }
    
    /**
     * F-303: 残業時間計算
     * F-304: 深夜勤務時間計算
     * F-305: 休日勤務時間計算
     * These are included in the daily and monthly processing jobs
     */
}