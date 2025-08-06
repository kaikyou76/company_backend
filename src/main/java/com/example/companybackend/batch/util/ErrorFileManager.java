package com.example.companybackend.batch.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * エンタープライズ級エラーファイル管理
 * バッチ処理中のエラー情報をファイル出力・管理
 */
@Component
public class ErrorFileManager {

    private static final Logger log = LoggerFactory.getLogger(ErrorFileManager.class);

    private final String errorDir;
    private final String errorPrefix;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final AtomicLong errorSequence = new AtomicLong(0);

    // 修改构造函数，使用@Value注解から配置ファイル获取パラメータ
    public ErrorFileManager(@Value("${batch.error.dir:/tmp/batch/errors}") String errorDir,
            @Value("${batch.error.prefix:BATCH_ERR_}") String errorPrefix) {
        this.errorDir = errorDir;
        this.errorPrefix = errorPrefix;
        initializeErrorDirectory();
    }

    /**
     * エラーディレクトリ初期化
     */
    private void initializeErrorDirectory() {
        try {
            Path dirPath = Paths.get(errorDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                log.info("エラーディレクトリを作成しました: {}", errorDir);
            }
        } catch (IOException e) {
            log.error("エラーディレクトリの作成に失敗しました: {}", errorDir, e);
            throw new RuntimeException("エラーディレクトリ作成失敗", e);
        }
    }

    /**
     * エラー情報をファイル出力
     */
    public String writeErrorToFile(String stepName, String errorType, String errorMessage,
            String stackTrace, Object problematicData) {
        try {
            String fileName = generateErrorFileName(stepName, errorType);
            Path filePath = Paths.get(errorDir, fileName);

            StringBuilder errorContent = new StringBuilder();
            errorContent.append("=== バッチエラー情報 ===\n");
            errorContent.append("発生時刻: ").append(LocalDateTime.now()).append("\n");
            errorContent.append("ステップ名: ").append(stepName).append("\n");
            errorContent.append("エラータイプ: ").append(errorType).append("\n");
            errorContent.append("エラーメッセージ: ").append(errorMessage).append("\n");
            errorContent.append("シーケンス番号: ").append(errorSequence.get()).append("\n");
            errorContent.append("\n=== 問題データ ===\n");
            errorContent.append(problematicData != null ? problematicData.toString() : "N/A").append("\n");
            errorContent.append("\n=== スタックトレース ===\n");
            errorContent.append(stackTrace != null ? stackTrace : "N/A").append("\n");

            Files.write(filePath, errorContent.toString().getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            log.warn("エラー情報をファイルに出力しました: {} (Step: {}, Type: {})",
                    fileName, stepName, errorType);

            return fileName;

        } catch (IOException e) {
            log.error("エラーファイル出力に失敗しました", e);
            return null;
        }
    }

    /**
     * CSV形式エラーデータ出力
     */
    public String writeCsvErrorData(String stepName, String[] headers, Object[] errorData) {
        try {
            String fileName = generateErrorFileName(stepName, "CSV_DATA");
            Path filePath = Paths.get(errorDir, fileName);

            StringBuilder csvContent = new StringBuilder();

            // ヘッダー行（ファイルが新規の場合のみ）
            if (!Files.exists(filePath)) {
                csvContent.append("ERROR_TIMESTAMP,STEP_NAME,SEQUENCE,");
                if (headers != null) {
                    csvContent.append(String.join(",", headers));
                }
                csvContent.append("\n");
            }

            // データ行
            csvContent.append(LocalDateTime.now().format(dateFormatter)).append(",");
            csvContent.append(stepName).append(",");
            csvContent.append(errorSequence.incrementAndGet()).append(",");

            if (errorData != null) {
                for (int i = 0; i < errorData.length; i++) {
                    if (i > 0)
                        csvContent.append(",");
                    csvContent.append(errorData[i] != null ? errorData[i].toString() : "");
                }
            }
            csvContent.append("\n");

            Files.write(filePath, csvContent.toString().getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            log.warn("CSVエラーデータを出力しました: {} (Step: {})", fileName, stepName);
            return fileName;

        } catch (IOException e) {
            log.error("CSVエラーファイル出力に失敗しました", e);
            return null;
        }
    }

    /**
     * 集約エラーレポート出力
     */
    public String writeAggregatedErrorReport(String jobName, int totalErrors,
            java.util.Map<String, Integer> errorsByStep) {
        try {
            String fileName = generateErrorFileName(jobName, "SUMMARY");
            Path filePath = Paths.get(errorDir, fileName);

            StringBuilder reportContent = new StringBuilder();
            reportContent.append("=== バッチエラー集約レポート ===\n");
            reportContent.append("ジョブ名: ").append(jobName).append("\n");
            reportContent.append("レポート作成時刻: ").append(LocalDateTime.now()).append("\n");
            reportContent.append("総エラー数: ").append(totalErrors).append("\n");
            reportContent.append("\n=== ステップ別エラー数 ===\n");

            errorsByStep.forEach((stepName, errorCount) -> reportContent.append(stepName).append(": ")
                    .append(errorCount).append(" errors\n"));

            reportContent.append("\n=== 推奨対応 ===\n");
            if (totalErrors > 1000) {
                reportContent.append("- 大量エラー発生：データ品質を確認してください\n");
                reportContent.append("- システムリソースを確認してください\n");
            } else if (totalErrors > 100) {
                reportContent.append("- 中程度エラー発生：設定値を見直してください\n");
            } else {
                reportContent.append("- 軽微なエラー：個別対応で対処可能です\n");
            }

            Files.write(filePath, reportContent.toString().getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            log.info("集約エラーレポートを出力しました: {} (総エラー数: {})", fileName, totalErrors);
            return fileName;

        } catch (IOException e) {
            log.error("集約エラーレポート出力に失敗しました", e);
            return null;
        }
    }

    /**
     * エラーファイル名生成
     */
    private String generateErrorFileName(String stepName, String errorType) {
        String timestamp = LocalDateTime.now().format(dateFormatter);
        return String.format("%s_%s_%s_%s.txt",
                errorPrefix, stepName, errorType, timestamp);
    }

    /**
     * 古いエラーファイルクリーンアップ
     */
    public void cleanupOldErrorFiles(int retentionDays) {
        try {
            Path dirPath = Paths.get(errorDir);
            if (!Files.exists(dirPath))
                return;

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

            Files.list(dirPath)
                    .filter(path -> path.toString().startsWith(errorPrefix))
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path)
                                    .toInstant()
                                    .isBefore(cutoffDate.atZone(java.time.ZoneId.systemDefault()).toInstant());
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("古いエラーファイルを削除しました: {}", path.getFileName());
                        } catch (IOException e) {
                            log.warn("エラーファイル削除に失敗しました: {}", path.getFileName(), e);
                        }
                    });

        } catch (IOException e) {
            log.error("エラーファイルクリーンアップに失敗しました", e);
        }
    }

    /**
     * エラーファイル統計情報取得
     */
    public java.util.Map<String, Object> getErrorFileStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        try {
            Path dirPath = Paths.get(errorDir);
            if (!Files.exists(dirPath)) {
                stats.put("totalFiles", 0);
                stats.put("totalSize", 0L);
                return stats;
            }

            java.util.List<Path> errorFiles = Files.list(dirPath)
                    .filter(path -> path.toString().contains(errorPrefix))
                    .collect(java.util.stream.Collectors.toList());

            long totalSize = errorFiles.stream()
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();

            stats.put("totalFiles", errorFiles.size());
            stats.put("totalSize", totalSize);
            stats.put("totalSizeMB", totalSize / 1024.0 / 1024.0);
            stats.put("errorDirectory", errorDir);
            stats.put("currentSequence", errorSequence.get());

        } catch (IOException e) {
            log.error("エラーファイル統計取得に失敗しました", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }
}