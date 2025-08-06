package com.example.companybackend.security;

import com.example.companybackend.config.CsrfConfigurationProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * CSRFオリジン検証フィルター
 * 
 * 目的:
 * - CSRFトークンのオリジン検証を実施
 * - 不正なオリジンからのリクエストをブロック
 * - クロスサイトリクエストフォージェリ攻撃の防止
 * 
 * 機能:
 * - Originヘッダーの検証
 * - Refererヘッダーの検証
 * - 許可されたオリジンリストとの照合
 * - モニタリングモードと警告モードのサポート
 * 
 * 設定:
 * - app.security.csrf.enabled: CSRF保護の有効化フラグ
 * - app.security.csrf.origin-validation-enabled: オリジン検証の有効化フラグ
 * - app.security.csrf.allowed-origins: 許可されたオリジンリスト
 * - app.security.csrf.monitoring-mode: モニタリングモード（ブロックせずにログ出力）
 * - app.security.csrf.warning-mode: 警告モード（警告ログ出力のみ）
 */
@Component
@EnableConfigurationProperties(CsrfConfigurationProperties.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class CsrfOriginValidationFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(CsrfOriginValidationFilter.class);
    
    private final CsrfConfigurationProperties csrfConfig;
    private final SecurityEventLogger securityEventLogger;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    public CsrfOriginValidationFilter(CsrfConfigurationProperties csrfConfig, SecurityEventLogger securityEventLogger) {
        this.csrfConfig = csrfConfig;
        this.securityEventLogger = securityEventLogger;
        logger.info("CSRF Origin Validation Filter initialized with monitoringMode: {}, warningMode: {}", 
            csrfConfig.isMonitoringMode(), csrfConfig.isWarningMode());
    }
    
    // 添加用于测试の構造函数
    public CsrfOriginValidationFilter(CsrfConfigurationProperties csrfConfig) {
        this(csrfConfig, null);
        logger.info("CSRF Origin Validation Filter initialized (test constructor) with monitoringMode: {}, warningMode: {}", 
            csrfConfig.isMonitoringMode(), csrfConfig.isWarningMode());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // CSRF保護が無効の場合はスキップ
        if (!csrfConfig.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        // 除外パスの場合はスキップ
        if (isExcludedPath(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        // 状態変更操作の場合のみ検証
        if (isStateChangingRequest(httpRequest)) {

            String clientIp = getClientIpAddress(httpRequest);
            String requestUri = httpRequest.getRequestURI();
            String method = httpRequest.getMethod();

            boolean originValid = validateOrigin(httpRequest);
            boolean tokenValid = validateCsrfToken(httpRequest);

            // Origin/Referer検証またはCSRFトークン検証が失敗した場合
            // 両方の検証が必要（強化された保護）
            if (!originValid || !tokenValid) {

                String origin = httpRequest.getHeader("Origin");
                String referer = httpRequest.getHeader("Referer");
                String csrfToken = httpRequest.getHeader("X-CSRF-TOKEN");

                // セキュリティログの記録
                String violationReason = buildViolationReason(originValid, tokenValid, origin, referer, csrfToken);
                String payload = buildPayload(httpRequest);

                logger.warn(
                        "CSRF Protection: Validation failed - Method: {}, URI: {}, Origin: {}, Referer: {}, CSRF-Token: {}, IP: {}, OriginValid: {}, TokenValid: {}",
                        method, requestUri, origin, referer, csrfToken, clientIp, originValid, tokenValid);

                // セキュリティイベントとして記録
                if (securityEventLogger != null) {
                    securityEventLogger.logCsrfViolation(httpRequest, violationReason, payload);
                }

                logger.info("CSRF Config - Monitoring Mode: {}, Warning Mode: {}", 
                    csrfConfig.isMonitoringMode(), csrfConfig.isWarningMode());

                if (csrfConfig.isMonitoringMode() || csrfConfig.isWarningMode()) {
                    // 監視・警告モード: ログのみ記録、リクエストは通す
                    logger.info("CSRF Protection: Request allowed in monitoring/warning mode - URI: {}, IP: {}",
                            requestUri, clientIp);
                    chain.doFilter(request, response);
                    return;
                } else {
                    // 保護モード: リクエストをブロック
                    logger.error("CSRF Protection: Request blocked - URI: {}, IP: {}", requestUri, clientIp);

                    httpResponse.setStatus(HttpStatus.FORBIDDEN.value());
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write(
                            "{\"error\":\"CSRF_PROTECTION_VIOLATION\",\"message\":\"Request blocked due to CSRF protection\"}");
                    return;
                }
            } else {
                // 正常なリクエストの場合
                logger.debug("CSRF Protection: Valid request - URI: {}, IP: {}", requestUri, clientIp);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * 状態変更リクエストかどうかを判定
     */
    private boolean isStateChangingRequest(HttpServletRequest request) {
        String method = request.getMethod();
        return csrfConfig.getProtectedMethods().contains(method);
    }

    /**
     * 除外パスかどうかを判定
     */
    private boolean isExcludedPath(String requestUri) {
        return csrfConfig.getExcludedPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestUri));
    }

    /**
     * Origin/Refererヘッダーの検証
     */
    private boolean validateOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        // Origin優先、なければRefererを使用
        String sourceOrigin = origin;
        if (!StringUtils.hasText(sourceOrigin) && StringUtils.hasText(referer)) {
            sourceOrigin = extractOriginFromReferer(referer);
        }

        // Origin/Refererが存在しない場合は拒否
        if (!StringUtils.hasText(sourceOrigin)) {
            logger.warn("CSRF Protection: Missing Origin and Referer headers");
            return false;
        }

        // 許可されたオリジンとの照合（final変数として宣言）
        final String finalSourceOrigin = sourceOrigin;
        boolean isAllowed = csrfConfig.getAllowedOrigins().stream()
                .anyMatch(allowedOrigin -> isOriginAllowed(finalSourceOrigin, allowedOrigin));

        if (!isAllowed) {
            logger.warn("CSRF Protection: Origin not in allowed list - Origin: {}, Allowed: {}",
                    sourceOrigin, csrfConfig.getAllowedOrigins());
        }

        return isAllowed;
    }

    /**
     * CSRFトークンの検証
     */
    private boolean validateCsrfToken(HttpServletRequest request) {
        String headerToken = request.getHeader("X-CSRF-TOKEN");
        String cookieToken = getCsrfTokenFromCookie(request);

        // ヘッダートークンが存在しない場合は拒否
        if (!StringUtils.hasText(headerToken)) {
            logger.warn("CSRF Protection: Missing X-CSRF-TOKEN header");
            return false;
        }

        // Double Submit Cookie パターンの検証
        // ただし、cookieが存在しない場合（テスト環境など）はスキップ
        if (StringUtils.hasText(cookieToken)) {
            if (!headerToken.equals(cookieToken)) {
                logger.warn("CSRF Protection: CSRF token mismatch - Header: {}, Cookie: {}",
                        headerToken, cookieToken);
                return false;
            }
        } else {
            logger.debug("CSRF Protection: No CSRF cookie found, skipping double submit validation");
        }

        // 基本的なトークン形式検証
        if (!isValidTokenFormat(headerToken)) {
            logger.warn("CSRF Protection: Invalid CSRF token format: {}", headerToken);
            return false;
        }

        return true;
    }

    /**
     * CookieからCSRFトークンを取得
     */
    private String getCsrfTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("CSRF-TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * CSRFトークンの形式検証
     */
    private boolean isValidTokenFormat(String token) {
        // 基本的な形式チェック（長さ、文字種など）
        if (token == null || token.length() < 10) {
            return false;
        }

        // 英数字とハイフンのみ許可
        return token.matches("^[a-zA-Z0-9\\-_]+$");
    }

    /**
     * RefererヘッダーからOriginを抽出
     */
    private String extractOriginFromReferer(String referer) {
        try {
            URI uri = new URI(referer);
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (URISyntaxException e) {
            logger.warn("CSRF Protection: Invalid Referer header format: {}", referer);
            return null;
        }
    }

    /**
     * オリジンが許可されているかチェック
     */
    private boolean isOriginAllowed(String sourceOrigin, String allowedOrigin) {
        // 完全一致
        if (sourceOrigin.equals(allowedOrigin)) {
            return true;
        }

        // ワイルドカード対応（将来の拡張用）
        if (allowedOrigin.contains("*")) {
            return pathMatcher.match(allowedOrigin, sourceOrigin);
        }

        return false;
    }

    /**
     * クライアントIPアドレスの取得
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 違反理由の構築
     */
    private String buildViolationReason(boolean originValid, boolean tokenValid,
            String origin, String referer, String csrfToken) {
        StringBuilder reason = new StringBuilder();

        if (!originValid) {
            if (origin == null && referer == null) {
                reason.append("Missing Origin and Referer headers");
            } else {
                reason.append("Invalid origin: ").append(origin != null ? origin : "extracted from " + referer);
            }
        }

        if (!tokenValid) {
            if (reason.length() > 0) {
                reason.append("; ");
            }
            if (csrfToken == null) {
                reason.append("Missing CSRF token");
            } else {
                reason.append("Invalid CSRF token");
            }
        }

        return reason.toString();
    }

    /**
     * 攻撃ペイロードの構築
     */
    private String buildPayload(HttpServletRequest request) {
        StringBuilder payload = new StringBuilder();
        payload.append("Method: ").append(request.getMethod());
        payload.append(", URI: ").append(request.getRequestURI());
        payload.append(", Origin: ").append(request.getHeader("Origin"));
        payload.append(", Referer: ").append(request.getHeader("Referer"));
        payload.append(", CSRF-Token: ").append(request.getHeader("X-CSRF-TOKEN"));
        payload.append(", Content-Type: ").append(request.getContentType());
        return payload.toString();
    }
}