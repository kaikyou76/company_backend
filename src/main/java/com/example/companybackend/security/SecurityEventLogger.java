package com.example.companybackend.security;

import com.example.companybackend.entity.SecurityEvent;
import com.example.companybackend.repository.SecurityEventRepository;
import com.example.companybackend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * セキュリティイベントロガーサービス
 * 
 * CSRF攻撃やその他のセキュリティ違反を記録・監視する
 */
@Service
@RequiredArgsConstructor
public class SecurityEventLogger {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventLogger.class);

    private final SecurityEventRepository securityEventRepository;
    private final JwtUtil jwtUtil;

    // アラート閾値設定
    private static final int CSRF_ATTACK_THRESHOLD = 5; // 5分間に5回以上
    private static final int ALERT_TIME_WINDOW_MINUTES = 5;

    /**
     * CSRF攻撃の記録
     */
    public void logCsrfViolation(HttpServletRequest request, String reason, String payload) {
        try {
            String clientIp = getClientIpAddress(request);
            Long userId = extractUserId(request);
            String sessionId = extractSessionId(request);

            SecurityEvent event = SecurityEvent.builder()
                    .eventType("CSRF_VIOLATION")
                    .ipAddress(clientIp)
                    .userAgent(request.getHeader("User-Agent"))
                    .requestUri(request.getRequestURI())
                    .httpMethod(request.getMethod())
                    .reason(reason)
                    .payload(payload)
                    .userId(userId)
                    .sessionId(sessionId)
                    .severityLevel(determineSeverityLevel(reason))
                    .actionTaken("BLOCKED")
                    .createdAt(LocalDateTime.now())
                    .build();

            // データベースに記録
            securityEventRepository.save(event);

            // セキュリティログに記録
            log.warn("CSRF violation detected: {} from IP: {} for URI: {} - Reason: {}",
                    event.getEventType(), clientIp, request.getRequestURI(), reason);

            // 閾値チェックとアラート
            checkAndTriggerAlert(clientIp, "CSRF_VIOLATION");

        } catch (Exception e) {
            log.error("Failed to log CSRF violation", e);
        }
    }

    /**
     * XSS攻撃の記録
     */
    public void logXssAttempt(HttpServletRequest request, String reason, String payload) {
        try {
            String clientIp = getClientIpAddress(request);
            Long userId = extractUserId(request);

            SecurityEvent event = SecurityEvent.builder()
                    .eventType("XSS_ATTEMPT")
                    .ipAddress(clientIp)
                    .userAgent(request.getHeader("User-Agent"))
                    .requestUri(request.getRequestURI())
                    .httpMethod(request.getMethod())
                    .reason(reason)
                    .payload(payload)
                    .userId(userId)
                    .severityLevel("HIGH")
                    .actionTaken("BLOCKED")
                    .build();

            securityEventRepository.save(event);
            log.warn("XSS attempt detected: {} from IP: {} - Payload: {}",
                    clientIp, request.getRequestURI(), payload);

        } catch (Exception e) {
            log.error("Failed to log XSS attempt", e);
        }
    }

    /**
     * 認証失敗の記録
     */
    public void logAuthenticationFailure(HttpServletRequest request, String reason) {
        try {
            String clientIp = getClientIpAddress(request);

            SecurityEvent event = SecurityEvent.builder()
                    .eventType("AUTH_FAILURE")
                    .ipAddress(clientIp)
                    .userAgent(request.getHeader("User-Agent"))
                    .requestUri(request.getRequestURI())
                    .httpMethod(request.getMethod())
                    .reason(reason)
                    .severityLevel("MEDIUM")
                    .actionTaken("BLOCKED")
                    .build();

            securityEventRepository.save(event);
            log.warn("Authentication failure: {} from IP: {} - Reason: {}",
                    clientIp, request.getRequestURI(), reason);

        } catch (Exception e) {
            log.error("Failed to log authentication failure", e);
        }
    }

    /**
     * 攻撃閾値チェックとアラート発生
     */
    private void checkAndTriggerAlert(String ipAddress, String eventType) {
        try {
            OffsetDateTime since = OffsetDateTime.now().minusMinutes(ALERT_TIME_WINDOW_MINUTES);
            long recentAttacks = securityEventRepository.countByIpAddressAndEventTypeAndCreatedAtAfter(
                    ipAddress, eventType, since);

            if (recentAttacks >= CSRF_ATTACK_THRESHOLD) {
                triggerSecurityAlert(ipAddress, eventType, recentAttacks);
            }

        } catch (Exception e) {
            log.error("Failed to check attack threshold", e);
        }
    }

    /**
     * セキュリティアラートの発生
     */
    private void triggerSecurityAlert(String ipAddress, String eventType, long attackCount) {
        // 重要: 実際の運用では、メール通知、Slack通知、監視システム連携などを実装
        log.error("SECURITY ALERT: {} attacks detected from IP: {} in the last {} minutes. Attack count: {}",
                eventType, ipAddress, ALERT_TIME_WINDOW_MINUTES, attackCount);

        // アラートイベントとして記録
        SecurityEvent alertEvent = SecurityEvent.builder()
                .eventType("SECURITY_ALERT")
                .ipAddress(ipAddress)
                .reason(String.format("%s attacks threshold exceeded: %d attacks in %d minutes",
                        eventType, attackCount, ALERT_TIME_WINDOW_MINUTES))
                .severityLevel("CRITICAL")
                .actionTaken("ALERT_TRIGGERED")
                .build();

        securityEventRepository.save(alertEvent);
    }

    /**
     * 重要度レベルの決定
     */
    private String determineSeverityLevel(String reason) {
        if (reason.contains("token mismatch") || reason.contains("invalid token")) {
            return "HIGH";
        } else if (reason.contains("missing") || reason.contains("origin")) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * メタデータの構築
     */
    private String buildMetadata(HttpServletRequest request) {
        StringBuilder metadata = new StringBuilder();
        metadata.append("{");
        metadata.append("\"referer\":\"").append(request.getHeader("Referer")).append("\",");
        metadata.append("\"origin\":\"").append(request.getHeader("Origin")).append("\",");
        metadata.append("\"contentType\":\"").append(request.getContentType()).append("\",");
        metadata.append("\"contentLength\":").append(request.getContentLength());
        metadata.append("}");
        return metadata.toString();
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
     * JWTからユーザーIDを抽出
     */
    private Long extractUserId(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtUtil.extractUsername(token);
                // 実際の実装では、ユーザー名からユーザーIDを取得する処理が必要
                return null; // 簡略化のためnullを返す
            }
        } catch (Exception e) {
            log.debug("Failed to extract user ID from JWT", e);
        }
        return null;
    }

    /**
     * セッションIDの抽出
     */
    private String extractSessionId(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return jwtUtil.extractUsername(token); // ユーザー名をセッションIDとして使用
            }
        } catch (Exception e) {
            log.debug("Failed to extract session ID", e);
        }
        return null;
    }
}