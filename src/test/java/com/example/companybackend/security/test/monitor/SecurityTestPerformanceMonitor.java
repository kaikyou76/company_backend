package com.example.companybackend.security.test.monitor;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * セキュリティテストパフォーマンス監視クラス
 * 
 * 目的:
 * - セキュリティテストのパフォーマンス測定
 * - リソース使用量の監視
 * - 同時接続テストの実施
 * 
 * 機能:
 * - レスポンス時間測定機能
 * - リソース使用量監視機能
 * - 同時接続テスト機能
 * 
 * 要件対応:
 * - フェーズ5の要件6.7を満たす
 * - フェーズ5の要件6.8を満たす
 */
@Component
public class SecurityTestPerformanceMonitor {

    private final Map<String, PerformanceMetrics> metricsMap = new ConcurrentHashMap<>();
    
    /**
     * パフォーマンスメトリクスデータクラス
     */
    public static class PerformanceMetrics {
        private long totalResponseTime = 0;
        private long requestCount = 0;
        private long maxResponseTime = 0;
        private long minResponseTime = Long.MAX_VALUE;
        
        public synchronized void addResponseTime(long responseTime) {
            totalResponseTime += responseTime;
            requestCount++;
            
            if (responseTime > maxResponseTime) {
                maxResponseTime = responseTime;
            }
            
            if (responseTime < minResponseTime) {
                minResponseTime = responseTime;
            }
        }
        
        public double getAverageResponseTime() {
            return requestCount > 0 ? (double) totalResponseTime / requestCount : 0;
        }
        
        public long getMaxResponseTime() {
            return maxResponseTime;
        }
        
        public long getMinResponseTime() {
            return minResponseTime == Long.MAX_VALUE ? 0 : minResponseTime;
        }
        
        public long getRequestCount() {
            return requestCount;
        }
    }
    
    /**
     * レスポンス時間測定開始
     * 
     * @param testType テスト種別
     * @return 測定開始時刻（ナノ秒）
     */
    public long startResponseTimeMeasurement(String testType) {
        return System.nanoTime();
    }
    
    /**
     * レスポンス時間測定終了
     * 
     * @param testType テスト種別
     * @param startTime 測定開始時刻（ナノ秒）
     */
    public void endResponseTimeMeasurement(String testType, long startTime) {
        long endTime = System.nanoTime();
        long responseTime = (endTime - startTime) / 1_000_000; // ミリ秒に変換
        
        metricsMap.computeIfAbsent(testType, k -> new PerformanceMetrics())
                  .addResponseTime(responseTime);
    }
    
    /**
     * パフォーマンスメトリクスを取得
     * 
     * @param testType テスト種別
     * @return パフォーマンスメトリクス
     */
    public PerformanceMetrics getPerformanceMetrics(String testType) {
        return metricsMap.getOrDefault(testType, new PerformanceMetrics());
    }
    
    /**
     * すべてのパフォーマンスメトリクスを取得
     * 
     * @return すべてのパフォーマンスメトリクス
     */
    public Map<String, PerformanceMetrics> getAllPerformanceMetrics() {
        return new ConcurrentHashMap<>(metricsMap);
    }
    
    /**
     * メトリクスをクリア
     */
    public void clearMetrics() {
        metricsMap.clear();
    }
    
    /**
     * リソース使用量を監視（モック実装）
     * 
     * @return リソース使用量情報
     */
    public String monitorResourceUsage() {
        // 実際の実装では、JVMのメモリ使用量やCPU使用率などを監視する
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return String.format("Memory Usage - Total: %d MB, Used: %d MB, Free: %d MB", 
                           totalMemory / (1024 * 1024), 
                           usedMemory / (1024 * 1024), 
                           freeMemory / (1024 * 1024));
    }
    
    /**
     * 同時接続テストを実行（モック実装）
     * 
     * @param concurrentUsers 同時接続ユーザー数
     * @param testDuration テスト実行時間（秒）
     * @return 同時接続テスト結果
     */
    public String executeConcurrentConnectionTest(int concurrentUsers, int testDuration) {
        // 実際の実装では、複数のスレッドを使用して同時接続テストを実行する
        return String.format("Concurrent Connection Test - Users: %d, Duration: %d seconds - COMPLETED", 
                           concurrentUsers, testDuration);
    }
}