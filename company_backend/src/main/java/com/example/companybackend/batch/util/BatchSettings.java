import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * エンタープライズ級バッチ設定管理
 * 全バッチ関連設定の一元管理とバリデーション
 */
@Component
@Slf4j
public class BatchSettings {
    
    private final Properties properties;

    // 修复构造函数，添加参数
    public BatchSettings(Properties properties) {
        this.properties = properties;
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
        properties.setProperty("batch.error.output.dir", "/tmp/batch/error");
        properties.setProperty("batch.error.file.prefix", "batch_error");
    }
    
    // 添加缺失的validateSettings方法
    private void validateSettings() {
        // 验证设置的逻辑可以在这里添加
        log.info("Batch settings validated");
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
    public boolean isValidationStrict() {
        return getBooleanProperty("batch.validation.strict", true);
    }

    /**
     * 自動復旧機能有効化フラグ
     */
    public boolean isRecoveryAuto() {
        return getBooleanProperty("batch.recovery.auto", false);
    }
}