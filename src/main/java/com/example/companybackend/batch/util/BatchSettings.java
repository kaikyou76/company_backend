package com.example.companybackend.batch.util;

import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * エンタープライズ級バッチ設定管理
 * 全バッチ関連設定の一元管理とバリデーション
 */
@Component
public class BatchSettings {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BatchSettings.class);
    
    private final Properties properties;

    public BatchSettings() {
        this.properties = new Properties();
        // デフォルト設定値をロード
        loadDefaultProperties();
        validateSettings();
    }
    
    private void loadDefaultProperties() {
        properties.setProperty("batch.chunk.size", "1000");
        properties.setProperty("batch.skip.limit", "100");
        properties.setProperty("batch.retry.limit", "3");
        properties.setProperty("batch.timeout.seconds", "3600");
        properties.setProperty("batch.monitoring.enabled", "true");
        properties.setProperty("batch.validation.strict", "true");
        properties.setProperty("batch.recovery.auto", "false");
    }

    /**
     * プロパティ値取得（デフォルト値付き）
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 数値プロパティ取得
     */
    public int getIntProperty(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("数値変換エラー: {} = {}, デフォルト値を使用: {}", key, properties.getProperty(key), defaultValue);
            return defaultValue;
        }
    }

    /**
     * boolean プロパティ取得
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    // ===== エンタープライズ級バッチ設定項目 =====

    /**
     * チャンクサイズ（大量データ処理最適化）
     */
    public int getChunkSize() {
        return getIntProperty("batch.chunk.size", 1000);
    }

    /**
     * スキップ制限（データ品質対応）
     */
    public int getSkipLimit() {
        return getIntProperty("batch.skip.limit", 100);
    }

    /**
     * リトライ制限（システム障害対応）
     */
    public int getRetryLimit() {
        return getIntProperty("batch.retry.limit", 3);
    }

    /**
     * タイムアウト設定（長時間処理対応）
     */
    public int getTimeoutSeconds() {
        return getIntProperty("batch.timeout.seconds", 3600);
    }

    /**
     * エラーファイル出力ディレクトリ
     */
    public String getErrorOutputDir() {
        return getProperty("batch.error.output.dir", "/tmp/batch/error");
    }

    /**
     * エラーファイル名プレフィックス
     */
    public String getErrorFilePrefix() {
        return getProperty("batch.error.file.prefix", "batch_error");
    }

    /**
     * 監視機能有効化フラグ
     */
    public boolean isMonitoringEnabled() {
        return getBooleanProperty("batch.monitoring.enabled", true);
    }

    /**
     * 厳密バリデーション有効化フラグ
     */
    public boolean isStrictValidation() {
        return getBooleanProperty("batch.validation.strict", true);
    }

    /**
     * 自動復旧機能有効化フラグ
     */
    public boolean isAutoRecovery() {
        return getBooleanProperty("batch.recovery.auto", false);
    }

    // ===== 閾値設定（BACKEND_TASKS.md準拠） =====

    /**
     * 従業員数閾値
     */
    public int getEmployeeThreshold() {
        return getIntProperty("batch.threshold.employee", 10000);
    }

    /**
     * 部署数閾値
     */
    public int getDepartmentThreshold() {
        return getIntProperty("batch.threshold.department", 1000);
    }

    /**
     * 勤怠記録数閾値
     */
    public int getAttendanceThreshold() {
        return getIntProperty("batch.threshold.attendance", 100000);
    }

    /**
     * 組織数閾値
     */
    public int getOrganizationThreshold() {
        return getIntProperty("batch.threshold.organization", 5000);
    }

    // ===== CSVファイル設定 =====

    /**
     * CSV入力ディレクトリ
     */
    public String getCsvInputDir() {
        return getProperty("batch.csv.input.dir", "/tmp/batch/input");
    }

    /**
     * CSV出力ディレクトリ
     */
    public String getCsvOutputDir() {
        return getProperty("batch.csv.output.dir", "/tmp/batch/output");
    }

    /**
     * CSV文字エンコーディング
     */
    public String getCsvEncoding() {
        return getProperty("batch.csv.encoding", "UTF-8");
    }

    // ===== データベース設定 =====

    /**
     * バッチ専用フェッチサイズ
     */
    public int getDbFetchSize() {
        return getIntProperty("batch.db.fetch.size", 1000);
    }

    /**
     * バッチ専用接続プールサイズ
     */
    public int getDbPoolSize() {
        return getIntProperty("batch.db.pool.size", 10);
    }

    /**
     * 設定値バリデーション
     */
    private void validateSettings() {
        // チャンクサイズ検証
        if (getChunkSize() <= 0 || getChunkSize() > 10000) {
            throw new IllegalArgumentException("チャンクサイズは1-10000の範囲で設定してください: " + getChunkSize());
        }

        // スキップ制限検証
        if (getSkipLimit() < 0) {
            throw new IllegalArgumentException("スキップ制限は0以上で設定してください: " + getSkipLimit());
        }

        // リトライ制限検証
        if (getRetryLimit() < 0 || getRetryLimit() > 10) {
            throw new IllegalArgumentException("リトライ制限は0-10の範囲で設定してください: " + getRetryLimit());
        }

        // タイムアウト検証
        if (getTimeoutSeconds() <= 0) {
            throw new IllegalArgumentException("タイムアウトは1以上で設定してください: " + getTimeoutSeconds());
        }

        log.info("バッチ設定検証完了 - チャンク:{}, スキップ制限:{}, リトライ制限:{}, タイムアウト:{}秒",
                getChunkSize(), getSkipLimit(), getRetryLimit(), getTimeoutSeconds());
    }

    /**
     * 設定値情報ログ出力
     */
    public void logSettings() {
        log.info("=== バッチ設定情報 ===");
        log.info("チャンクサイズ: {}", getChunkSize());
        log.info("スキップ制限: {}", getSkipLimit());
        log.info("リトライ制限: {}", getRetryLimit());
        log.info("タイムアウト: {}秒", getTimeoutSeconds());
        log.info("監視機能: {}", isMonitoringEnabled());
        log.info("厳密バリデーション: {}", isStrictValidation());
        log.info("自動復旧: {}", isAutoRecovery());
        log.info("従業員数閾値: {}", getEmployeeThreshold());
        log.info("部署数閾値: {}", getDepartmentThreshold());
        log.info("勤怠記録数閾値: {}", getAttendanceThreshold());
        log.info("======================");
    }
}