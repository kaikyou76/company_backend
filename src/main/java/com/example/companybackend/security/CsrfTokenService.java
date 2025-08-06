package com.example.companybackend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * CSRFトークン生成・管理サービス
 * 
 * 機能:
 * - セッション固有のCSRFトークン生成
 * - トークンの有効性検証
 * - トークン再利用防止
 * - 期限切れトークンの自動削除
 */
@Service
public class CsrfTokenService {

    private static final Logger log = LoggerFactory.getLogger(CsrfTokenService.class);
    private static final int TOKEN_LENGTH = 32;
    private static final int TOKEN_EXPIRY_MINUTES = 30;
    private static final String TOKEN_PREFIX = "csrf-";

    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> sessionTokenMap = new ConcurrentHashMap<>();

    /**
     * 新しいCSRFトークンを生成
     * 
     * @param sessionId セッションID（JWTのsubjectやユーザーIDを使用）
     * @return 生成されたCSRFトークン
     */
    public String generateToken(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }

        // 既存のトークンがあれば無効化
        String existingToken = sessionTokenMap.get(sessionId);
        if (existingToken != null) {
            tokenStore.remove(existingToken);
            log.debug("Invalidated existing CSRF token for session: {}", sessionId);
        }

        // 新しいトークンを生成
        String token = generateSecureToken();
        TokenInfo tokenInfo = new TokenInfo(sessionId, LocalDateTime.now());

        tokenStore.put(token, tokenInfo);
        sessionTokenMap.put(sessionId, token);

        log.debug("Generated new CSRF token for session: {}", sessionId);
        return token;
    }

    /**
     * CSRFトークンの有効性を検証
     * 
     * @param token     検証するトークン
     * @param sessionId セッションID
     * @return トークンが有効な場合true
     */
    public boolean validateToken(String token, String sessionId) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(sessionId)) {
            log.warn("CSRF token validation failed: token or sessionId is empty");
            return false;
        }

        TokenInfo tokenInfo = tokenStore.get(token);
        if (tokenInfo == null) {
            log.warn("CSRF token validation failed: token not found - {}", token);
            return false;
        }

        // セッションIDの一致確認
        if (!sessionId.equals(tokenInfo.getSessionId())) {
            log.warn("CSRF token validation failed: session mismatch - token session: {}, request session: {}",
                    tokenInfo.getSessionId(), sessionId);
            return false;
        }

        // 有効期限の確認
        if (isTokenExpired(tokenInfo)) {
            log.warn("CSRF token validation failed: token expired - {}", token);
            removeToken(token);
            return false;
        }

        log.debug("CSRF token validation successful for session: {}", sessionId);
        return true;
    }

    /**
     * CSRFトークンを使用済みとしてマーク（再利用防止）
     * 
     * @param token     使用するトークン
     * @param sessionId セッションID
     * @return 使用が成功した場合true
     */
    public boolean consumeToken(String token, String sessionId) {
        if (!validateToken(token, sessionId)) {
            return false;
        }

        // トークンを削除して再利用を防止
        removeToken(token);
        sessionTokenMap.remove(sessionId);

        log.debug("CSRF token consumed and removed for session: {}", sessionId);
        return true;
    }

    /**
     * セッションのCSRFトークンを取得
     * 
     * @param sessionId セッションID
     * @return 現在有効なCSRFトークン、存在しない場合はnull
     */
    public String getTokenForSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }

        String token = sessionTokenMap.get(sessionId);
        if (token != null) {
            TokenInfo tokenInfo = tokenStore.get(token);
            if (tokenInfo != null && !isTokenExpired(tokenInfo)) {
                return token;
            } else {
                // 期限切れトークンをクリーンアップ
                removeToken(token);
                sessionTokenMap.remove(sessionId);
            }
        }

        return null;
    }

    /**
     * 期限切れトークンのクリーンアップ
     */
    public void cleanupExpiredTokens() {
        int removedCount = 0;
        for (String token : tokenStore.keySet()) {
            TokenInfo tokenInfo = tokenStore.get(token);
            if (tokenInfo != null && isTokenExpired(tokenInfo)) {
                removeToken(token);
                sessionTokenMap.remove(tokenInfo.getSessionId());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("Cleaned up {} expired CSRF tokens", removedCount);
        }
    }

    /**
     * セキュアなトークンを生成
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        return token;
    }

    /**
     * トークンが期限切れかどうかを判定
     */
    private boolean isTokenExpired(TokenInfo tokenInfo) {
        return ChronoUnit.MINUTES.between(tokenInfo.getCreatedAt(), LocalDateTime.now()) > TOKEN_EXPIRY_MINUTES;
    }

    /**
     * トークンを削除
     */
    private void removeToken(String token) {
        tokenStore.remove(token);
    }

    /**
     * トークン情報を保持するクラス
     */
    private static class TokenInfo {
        private final String sessionId;
        private final LocalDateTime createdAt;

        public TokenInfo(String sessionId, LocalDateTime createdAt) {
            this.sessionId = sessionId;
            this.createdAt = createdAt;
        }

        public String getSessionId() {
            return sessionId;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}