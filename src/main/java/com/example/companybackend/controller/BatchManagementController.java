package com.example.companybackend.controller;

import com.example.companybackend.service.BatchMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * バッチ管理コントローラー
 * 既存Spring Batchテーブル（comsys_dump.sql）完全活用
 * 
 * エンドポイント：
 * - GET /api/v1/batch/instances - 全ジョブインスタンス取得
 * - GET /api/v1/batch/executions/{jobName} - ジョブ実行履歴取得
 * - GET /api/v1/batch/steps/{jobExecutionId} - ステップ実行履歴取得
 * - GET /api/v1/batch/statistics - バッチ実行統計取得
 * - GET /api/v1/batch/running - 実行中ジョブ取得
 */
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchManagementController {

    private final BatchMonitoringService batchMonitoringService;

    /**
     * 全ジョブインスタンス取得
     * batch_job_instanceテーブル活用
     */
    @GetMapping("/instances")
    public ResponseEntity<Map<String, Object>> getJobInstances() {
        try {
            List<Map<String, Object>> instances = batchMonitoringService.getAllJobInstances();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCount", instances.size());
            response.put("instances", instances);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("ジョブインスタンス取得エラー: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "ジョブインスタンス取得に失敗しました: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * ジョブ実行履歴取得
     * batch_job_executionテーブル活用
     */
    @GetMapping("/executions/{jobName}")
    public ResponseEntity<Map<String, Object>> getJobExecutionHistory(
            @PathVariable String jobName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<Map<String, Object>> executions = 
                batchMonitoringService.getJobExecutionHistory(jobName, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobName", jobName);
            response.put("page", page);
            response.put("size", size);
            response.put("totalCount", executions.size());
            response.put("executions", executions);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("ジョブ実行履歴取得エラー ({}): {}", jobName, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "ジョブ実行履歴取得に失敗しました: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * ステップ実行履歴取得
     * batch_step_executionテーブル活用
     */
    @GetMapping("/steps/{jobExecutionId}")
    public ResponseEntity<Map<String, Object>> getStepExecutionHistory(
            @PathVariable Long jobExecutionId) {
        try {
            List<Map<String, Object>> steps = 
                batchMonitoringService.getStepExecutionHistory(jobExecutionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobExecutionId", jobExecutionId);
            response.put("totalCount", steps.size());
            response.put("steps", steps);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("ステップ実行履歴取得エラー ({}): {}", jobExecutionId, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "ステップ実行履歴取得に失敗しました: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * バッチ実行統計取得
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getBatchStatistics() {
        try {
            Map<String, Object> statistics = batchMonitoringService.getBatchExecutionStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("バッチ統計取得エラー: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "バッチ統計取得に失敗しました: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 実行中ジョブ取得
     */
    @GetMapping("/running")
    public ResponseEntity<Map<String, Object>> getRunningJobs() {
        try {
            List<Map<String, Object>> runningJobs = batchMonitoringService.getRunningJobs();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCount", runningJobs.size());
            response.put("runningJobs", runningJobs);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("実行中ジョブ取得エラー: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "実行中ジョブ取得に失敗しました: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * ジョブ名一覧取得
     */
    @GetMapping("/job-names")
    public ResponseEntity<Map<String, Object>> getJobNames() {
        try {
            List<String> jobNames = batchMonitoringService.getJobNames();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCount", jobNames.size());
            response.put("jobNames", jobNames);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("ジョブ名一覧取得エラー: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "ジョブ名一覧取得に失敗しました: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 特定ジョブの最新実行情報取得
     */
    @GetMapping("/latest/{jobName}")
    public ResponseEntity<Map<String, Object>> getLatestJobExecution(@PathVariable String jobName) {
        try {
            Map<String, Object> latestExecution = 
                batchMonitoringService.getLatestJobExecution(jobName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobName", jobName);
            response.put("latestExecution", latestExecution);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("最新ジョブ実行情報取得エラー ({}): {}", jobName, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "最新ジョブ実行情報取得に失敗しました: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}