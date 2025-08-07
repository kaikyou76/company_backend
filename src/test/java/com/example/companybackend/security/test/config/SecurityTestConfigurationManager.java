package com.example.companybackend.security.test.config;

import com.example.companybackend.security.test.SecurityTestDataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * セキュリティテスト設定管理クラス
 * 
 * 目的:
 * - セキュリティテストの設定管理
 * - テストレベル設定
 * - 定期実行スケジューラー
 * 
 * 機能:
 * - 設定管理インターフェース
 * - テストレベル設定機能
 * - 定期実行スケジューラー
 * 
 * 要件対応:
 * - フェーズ5の要件7.5を満たす
 * - フェーズ5の要件7.8を満たす
 */
@Component
public class SecurityTestConfigurationManager {

    @Autowired
    private SecurityTestDataManager testDataManager;

    private final Map<String, String> configuration = new ConcurrentHashMap<>();

    /**
     * 初期化メソッド
     */
    @PostConstruct
    public void initializeConfiguration() {
        loadConfigurationFromDatabase();
    }

    /**
     * データベースから設定を読み込む
     */
    private void loadConfigurationFromDatabase() {
        try {
            Map<String, Object> configData = testDataManager.getConfiguration();
            for (Map.Entry<String, Object> entry : configData.entrySet()) {
                if (entry.getValue() != null) {
                    configuration.put(entry.getKey(), entry.getValue().toString());
                }
            }
        } catch (Exception e) {
            // デフォルト設定を使用
            loadDefaultConfiguration();
        }
    }

    /**
     * デフォルト設定を読み込む
     */
    private void loadDefaultConfiguration() {
        configuration.put("jwt.test.expiration", "300000");
        configuration.put("rate.limit.requests.per.minute", "10");
        configuration.put("rate.limit.requests.per.hour", "100");
        configuration.put("xss.protection.enabled", "true");
        configuration.put("csrf.protection.enabled", "true");
        configuration.put("sql.injection.protection.enabled", "true");
        configuration.put("test.schedule.enabled", "false");
        configuration.put("test.schedule.cron", "0 0 2 * * ?");
        configuration.put("test.level", "COMPREHENSIVE");
    }

    /**
     * 設定値を取得する
     * 
     * @param key 設定キー
     * @param defaultValue デフォルト値
     * @return 設定値
     */
    public String getConfigurationValue(String key, String defaultValue) {
        return configuration.getOrDefault(key, defaultValue);
    }

    /**
     * 設定値を取得する（整数型）
     * 
     * @param key 設定キー
     * @param defaultValue デフォルト値
     * @return 設定値（整数）
     */
    public int getConfigurationValueAsInt(String key, int defaultValue) {
        String value = configuration.get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 設定値を取得する（真偽値型）
     * 
     * @param key 設定キー
     * @param defaultValue デフォルト値
     * @return 設定値（真偽値）
     */
    public boolean getConfigurationValueAsBoolean(String key, boolean defaultValue) {
        String value = configuration.get(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    /**
     * 設定値を更新する
     * 
     * @param key 設定キー
     * @param value 設定値
     */
    public void updateConfigurationValue(String key, String value) {
        configuration.put(key, value);
        // 実際の実装では、データベースにも保存する
        testDataManager.updateConfiguration(key, value, "STRING");
    }

    /**
     * テストレベルを取得する
     * 
     * @return テストレベル
     */
    public TestLevel getTestLevel() {
        String level = configuration.getOrDefault("test.level", "COMPREHENSIVE");
        try {
            return TestLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TestLevel.COMPREHENSIVE;
        }
    }

    /**
     * テストレベルを設定する
     * 
     * @param level テストレベル
     */
    public void setTestLevel(TestLevel level) {
        configuration.put("test.level", level.name());
        testDataManager.updateConfiguration("test.level", level.name(), "STRING");
    }

    /**
     * スケジュールが有効かどうかを確認する
     * 
     * @return スケジュールが有効な場合はtrue、それ以外はfalse
     */
    public boolean isScheduleEnabled() {
        return getConfigurationValueAsBoolean("test.schedule.enabled", false);
    }

    /**
     * スケジュールを有効化する
     * 
     * @param enabled 有効化フラグ
     */
    public void setScheduleEnabled(boolean enabled) {
        configuration.put("test.schedule.enabled", String.valueOf(enabled));
        testDataManager.updateConfiguration("test.schedule.enabled", String.valueOf(enabled), "BOOLEAN");
    }

    /**
     * CRON式を取得する
     * 
     * @return CRON式
     */
    public String getCronExpression() {
        return getConfigurationValue("test.schedule.cron", "0 0 2 * * ?");
    }

    /**
     * CRON式を設定する
     * 
     * @param cron CRON式
     */
    public void setCronExpression(String cron) {
        configuration.put("test.schedule.cron", cron);
        testDataManager.updateConfiguration("test.schedule.cron", cron, "STRING");
    }

    /**
     * すべての設定を取得する
     * 
     * @return すべての設定
     */
    public Map<String, String> getAllConfigurations() {
        return new ConcurrentHashMap<>(configuration);
    }

    /**
     * テストレベル列挙型
     */
    public enum TestLevel {
        /**
         * 基本テストレベル - 主要なセキュリティテストのみ実行
         */
        BASIC,

        /**
         * 標準テストレベル - 一般的なセキュリティテストを実行
         */
        STANDARD,

        /**
         * 総合テストレベル - すべてのセキュリティテストを実行
         */
        COMPREHENSIVE,

        /**
         * 詳細テストレベル - 追加の負荷テストやパフォーマンステストを含む
         */
        DETAILED
    }
}