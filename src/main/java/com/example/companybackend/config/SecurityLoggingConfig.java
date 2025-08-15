package com.example.companybackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * セキュリティ設定のログ出力
 */
@Component
public class SecurityLoggingConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityLoggingConfig.class);

    @Autowired
    private Environment environment;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private CsrfProperties csrfProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void logSecurityConfiguration() {
        logger.info("=== セキュリティ設定情報 ===");

        // 環境情報
        String[] activeProfiles = environment.getActiveProfiles();
        logger.info("アクティブプロファイル: {}", String.join(", ", activeProfiles));

        // サーバー設定
        String serverPort = environment.getProperty("server.port", "8080");
        String sslEnabled = environment.getProperty("server.ssl.enabled", "false");
        logger.info("サーバーポート: {}", serverPort);
        logger.info("SSL有効: {}", sslEnabled);

        // CORS設定
        logger.info("許可されたオリジン: {}", String.join(", ", securityProperties.getAllowedOrigins()));
        logger.info("CORS最大有効期間: {} 秒", securityProperties.getMaxAge());

        // CSRF設定
        logger.info("CSRFクッキーセキュア: {}", csrfProperties.isCookieSecure());
        logger.info("CSRFクッキードメイン: {}", csrfProperties.getCookieDomain());
        logger.info("CSRFクッキーパス: {}", csrfProperties.getCookiePath());

        // 警告メッセージ
        if ("false".equals(sslEnabled) && isProductionLike(activeProfiles)) {
            logger.warn("警告: 本番環境でSSLが無効になっています！");
        }

        if (!csrfProperties.isCookieSecure() && isProductionLike(activeProfiles)) {
            logger.warn("警告: 本番環境でCSRFクッキーのセキュアフラグが無効になっています！");
        }

        logger.info("=== セキュリティ設定情報終了 ===");
    }

    private boolean isProductionLike(String[] profiles) {
        for (String profile : profiles) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                return true;
            }
        }
        return false;
    }
}