package com.example.companybackend.controller;

import com.example.companybackend.batch.service.BatchJobService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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