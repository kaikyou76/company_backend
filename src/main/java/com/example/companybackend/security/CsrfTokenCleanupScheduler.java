package com.example.companybackend.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CSRFトークンの定期クリーンアップスケジューラー
 * 
 * 機能:
 * - 期限切れCSRFトークンの定期削除
 * - メモリリークの防止
 * - システムパフォーマンスの維持
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.csrf.enabled", havingValue = "true", matchIfMissing = true)
public class CsrfTokenCleanupScheduler {

    private final CsrfTokenService csrfTokenService;

    /**
     * 期限切れCSRFトークンのクリーンアップ
     * 15分ごとに実行
     */
    @Scheduled(fixedRate = 15 * 60 * 1000) // 15分
    public void cleanupExpiredTokens() {
        try {
            log.debug("Starting CSRF token cleanup...");
            csrfTokenService.cleanupExpiredTokens();
            log.debug("CSRF token cleanup completed");
        } catch (Exception e) {
            log.error("Failed to cleanup expired CSRF tokens", e);
        }
    }

    /**
     * システム起動時の初期クリーンアップ
     */
    @Scheduled(initialDelay = 60 * 1000, fixedRate = Long.MAX_VALUE) // 起動1分後に1回だけ実行
    public void initialCleanup() {
        try {
            log.info("Performing initial CSRF token cleanup...");
            csrfTokenService.cleanupExpiredTokens();
            log.info("Initial CSRF token cleanup completed");
        } catch (Exception e) {
            log.error("Failed to perform initial CSRF token cleanup", e);
        }
    }
}