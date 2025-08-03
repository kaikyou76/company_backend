package com.example.orgchart_api.batch.controller;

import com.example.orgchart_api.batch.monitoring.BatchMetricsSnapshot;
import com.example.orgchart_api.batch.monitoring.BatchMonitoringService;
import com.example.orgchart_api.batch.monitoring.BatchPerformanceMetrics;
import com.example.orgchart_api.batch.monitoring.MonitoringStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * バッチジョブ監視用RESTエンドポイント
 * バッチジョブのステータス監視、診断情報取得、メトリクス報告を提供
 */
@RestController
@RequestMapping("/api/batch/monitoring")
@CrossOrigin(origins = "*")
public class BatchMonitoringController {
    private static final Logger logger = LoggerFactory.getLogger(BatchMonitoringController.class);

    @Autowired
    private BatchMonitoringService monitoringService;

    @Autowired
    private BatchPerformanceMetrics performanceMetrics;

    /**
     * バッチジョブの全体的なヘルスチェック
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            logger.info("バッチジョブヘルスチェック要求を受信");

            MonitoringStatus status = monitoringService.getMonitoringStatus();
            BatchMetricsSnapshot metrics = performanceMetrics.getCurrentSnapshot();

            Map<String, Object> healthStatus = new HashMap<>();
            healthStatus.put("timestamp", Instant.now());
            healthStatus.put("status", determineOverallHealth(metrics));
            healthStatus.put("monitoring", Map.of(
                    "enabled", status.isMonitoringEnabled(),
                    "active", status.isCurrentlyMonitoring(),
                    "lastCheck", status.getLastCheckTime()));
            healthStatus.put("metrics", Map.of(
                    "totalErrors", metrics.getTotalErrors(),
                    "totalWarnings", metrics.getTotalWarnings(),
                    "totalProcessedRecords", metrics.getTotalProcessedRecords(),
                    "errorRate", String.format("%.2f%%", metrics.getOverallErrorRate() * 100),
                    "warningRate", String.format("%.2f%%", metrics.getOverallWarningRate() * 100),
                    "activeSteps", metrics.getActiveStepsCount()));
            healthStatus.put("thresholds", Map.of(
                    "errorThreshold", String.format("%.2f%%", status.getErrorThreshold() * 100),
                    "warningThreshold", String.format("%.2f%%", status.getWarningThreshold() * 100)));

            logger.info("バッチジョブヘルスチェック完了: status={}", healthStatus.get("status"));
            return ResponseEntity.ok(healthStatus);

        } catch (Exception e) {
            logger.error("バッチジョブヘルスチェック中にエラーが発生しました", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("timestamp", Instant.now());
            errorResponse.put("status", "ERROR");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 詳細なバッチメトリクス情報を取得
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getDetailedMetrics() {
        try {
            logger.info("詳細バッチメトリクス要求を受信");

            BatchMetricsSnapshot metrics = performanceMetrics.getCurrentSnapshot();
            MonitoringStatus status = monitoringService.getMonitoringStatus();

            Map<String, Object> detailedMetrics = new HashMap<>();
            detailedMetrics.put("timestamp", Instant.now());
            detailedMetrics.put("overall", Map.of(
                    "totalErrors", metrics.getTotalErrors(),
                    "totalWarnings", metrics.getTotalWarnings(),
                    "totalProcessedRecords", metrics.getTotalProcessedRecords(),
                    "errorRate", metrics.getOverallErrorRate(),
                    "warningRate", metrics.getOverallWarningRate(),
                    "activeStepsCount", metrics.getActiveStepsCount(),
                    "isHealthy", metrics.isHealthy(status.getErrorThreshold(), status.getWarningThreshold())));

            // ステップ別メトリクス
            Map<String, Object> stepMetrics = new HashMap<>();
            metrics.getStepMetrics().forEach(step -> {
                stepMetrics.put(step.getStepName(), Map.of(
                        "executionCount", step.getExecutionCount(),
                        "averageDurationMs", step.getAverageDuration().toMillis(),
                        "totalProcessedRecords", step.getTotalProcessedRecords(),
                        "errorCount", step.getErrorCount(),
                        "warningCount", step.getWarningCount(),
                        "errorRate", step.getErrorRate(),
                        "warningRate", step.getWarningRate(),
                        "processingRate", step.getProcessingRate(),
                        "isHealthy", step.isHealthy(status.getErrorThreshold(), status.getWarningThreshold())));
            });
            detailedMetrics.put("steps", stepMetrics);

            detailedMetrics.put("monitoring", Map.of(
                    "enabled", status.isMonitoringEnabled(),
                    "active", status.isCurrentlyMonitoring(),
                    "errorThreshold", status.getErrorThreshold(),
                    "warningThreshold", status.getWarningThreshold(),
                    "checkIntervalSeconds", status.getCheckIntervalSeconds(),
                    "lastCheckTime", status.getLastCheckTime()));

            logger.info("詳細バッチメトリクス取得完了: steps={}, totalRecords={}",
                    stepMetrics.size(), metrics.getTotalProcessedRecords());

            return ResponseEntity.ok(detailedMetrics);

        } catch (Exception e) {
            logger.error("詳細バッチメトリクス取得中にエラーが発生しました", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("timestamp", Instant.now());
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 失敗したジョブの診断情報を取得
     */
    @GetMapping("/diagnostics")
    public ResponseEntity<Map<String, Object>> getDiagnosticInfo() {
        try {
            logger.info("バッチ診断情報要求を受信");

            BatchMetricsSnapshot metrics = performanceMetrics.getCurrentSnapshot();
            MonitoringStatus status = monitoringService.getMonitoringStatus();

            Map<String, Object> diagnostics = new HashMap<>();
            diagnostics.put("timestamp", Instant.now());
            diagnostics.put("systemInfo", getSystemInfo());
            diagnostics.put("batchStatus", Map.of(
                    "overallHealth", determineOverallHealth(metrics),
                    "totalErrors", metrics.getTotalErrors(),
                    "totalWarnings", metrics.getTotalWarnings(),
                    "errorRate", metrics.getOverallErrorRate(),
                    "warningRate", metrics.getOverallWarningRate()));

            // 問題のあるステップを特定
            Map<String, Object> problematicSteps = new HashMap<>();
            metrics.getStepMetrics().forEach(step -> {
                if (!step.isHealthy(status.getErrorThreshold(), status.getWarningThreshold())) {
                    problematicSteps.put(step.getStepName(), Map.of(
                            "errorRate", step.getErrorRate(),
                            "warningRate", step.getWarningRate(),
                            "errorCount", step.getErrorCount(),
                            "warningCount", step.getWarningCount(),
                            "processingRate", step.getProcessingRate(),
                            "averageDurationMs", step.getAverageDuration().toMillis()));
                }
            });
            diagnostics.put("problematicSteps", problematicSteps);

            // 推奨アクション
            diagnostics.put("recommendations", generateRecommendations(metrics, status));

            logger.info("バッチ診断情報取得完了: problematicSteps={}", problematicSteps.size());
            return ResponseEntity.ok(diagnostics);

        } catch (Exception e) {
            logger.error("バッチ診断情報取得中にエラーが発生しました", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("timestamp", Instant.now());
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 監視設定を更新
     */
    @PostMapping("/settings")
    public ResponseEntity<Map<String, Object>> updateMonitoringSettings(
            @RequestBody Map<String, Object> settings) {
        try {
            logger.info("監視設定更新要求を受信: {}", settings);

            double errorThreshold = ((Number) settings.getOrDefault("errorThreshold", 0.05)).doubleValue();
            double warningThreshold = ((Number) settings.getOrDefault("warningThreshold", 0.10)).doubleValue();
            int checkInterval = ((Number) settings.getOrDefault("checkIntervalSeconds", 30)).intValue();

            // 設定値の妥当性チェック
            if (errorThreshold < 0 || errorThreshold > 1) {
                throw new IllegalArgumentException("エラー閾値は0.0から1.0の間で設定してください");
            }
            if (warningThreshold < 0 || warningThreshold > 1) {
                throw new IllegalArgumentException("警告閾値は0.0から1.0の間で設定してください");
            }
            if (checkInterval < 10 || checkInterval > 3600) {
                throw new IllegalArgumentException("チェック間隔は10秒から3600秒の間で設定してください");
            }

            monitoringService.updateMonitoringSettings(errorThreshold, warningThreshold, checkInterval);

            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", Instant.now());
            response.put("status", "SUCCESS");
            response.put("message", "監視設定が正常に更新されました");
            response.put("settings", Map.of(
                    "errorThreshold", errorThreshold,
                    "warningThreshold", warningThreshold,
                    "checkIntervalSeconds", checkInterval));

            logger.info("監視設定更新完了: errorThreshold={}, warningThreshold={}, checkInterval={}",
                    errorThreshold, warningThreshold, checkInterval);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("監視設定更新で無効な値が指定されました: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("timestamp", Instant.now());
            errorResponse.put("status", "VALIDATION_ERROR");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("監視設定更新中にエラーが発生しました", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("timestamp", Instant.now());
            errorResponse.put("status", "ERROR");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 手動でヘルスチェックを実行
     */
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> performManualHealthCheck() {
        try {
            logger.info("手動ヘルスチェック要求を受信");

            monitoringService.performManualCheck();

            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", Instant.now());
            response.put("status", "SUCCESS");
            response.put("message", "手動ヘルスチェックが実行されました");

            logger.info("手動ヘルスチェック完了");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("手動ヘルスチェック中にエラーが発生しました", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("timestamp", Instant.now());
            errorResponse.put("status", "ERROR");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * メトリクスをリセット
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetMetrics() {
        try {
            logger.info("メトリクスリセット要求を受信");

            performanceMetrics.reset();

            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", Instant.now());
            response.put("status", "SUCCESS");
            response.put("message", "メトリクスがリセットされました");

            logger.info("メトリクスリセット完了");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("メトリクスリセット中にエラーが発生しました", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("timestamp", Instant.now());
            errorResponse.put("status", "ERROR");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 全体的な健全性を判定
     */
    private String determineOverallHealth(BatchMetricsSnapshot metrics) {
        if (metrics.getTotalErrors() == 0 && metrics.getTotalWarnings() == 0) {
            return "HEALTHY";
        } else if (metrics.getOverallErrorRate() > 0.05) {
            return "CRITICAL";
        } else if (metrics.getOverallWarningRate() > 0.10) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }

    /**
     * システム情報を取得
     */
    private Map<String, Object> getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        return Map.of(
                "javaVersion", System.getProperty("java.version"),
                "osName", System.getProperty("os.name"),
                "osVersion", System.getProperty("os.version"),
                "totalMemoryMB", runtime.totalMemory() / 1024 / 1024,
                "freeMemoryMB", runtime.freeMemory() / 1024 / 1024,
                "maxMemoryMB", runtime.maxMemory() / 1024 / 1024,
                "availableProcessors", runtime.availableProcessors());
    }

    /**
     * 推奨アクションを生成
     */
    private Map<String, String> generateRecommendations(BatchMetricsSnapshot metrics, MonitoringStatus status) {
        Map<String, String> recommendations = new HashMap<>();

        if (metrics.getOverallErrorRate() > status.getErrorThreshold()) {
            recommendations.put("highErrorRate",
                    "エラー率が高くなっています。ログを確認し、データ品質やシステムリソースをチェックしてください。");
        }

        if (metrics.getOverallWarningRate() > status.getWarningThreshold()) {
            recommendations.put("highWarningRate",
                    "警告率が高くなっています。データ検証ルールを見直すか、入力データの品質を改善してください。");
        }

        // 処理速度が遅いステップをチェック
        metrics.getStepMetrics().forEach(step -> {
            if (step.getProcessingRate() > 0 && step.getProcessingRate() < 1.0) {
                recommendations.put("slowStep_" + step.getStepName(),
                        String.format("ステップ '%s' の処理速度が遅くなっています。データベースインデックスやクエリの最適化を検討してください。",
                                step.getStepName()));
            }
        });

        if (recommendations.isEmpty()) {
            recommendations.put("general", "現在、特別な対応は必要ありません。システムは正常に動作しています。");
        }

        return recommendations;
    }
}