package com.example.companybackend.controller;

import com.example.companybackend.security.CsrfTokenService;
import com.example.companybackend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * CSRF保護関連のエンドポイント
 * 
 * 機能:
 * - CSRFトークンの配布
 * - トークンの更新
 * - セキュリティ状態の確認
 */
@RestController
@RequestMapping("/api/csrf")
@RequiredArgsConstructor
public class CsrfController {

    private static final Logger log = LoggerFactory.getLogger(CsrfController.class);
    private final CsrfTokenService csrfTokenService;
    private final JwtUtil jwtUtil;

    /**
     * CSRFトークンを取得
     * 
     * @param request  HTTPリクエスト
     * @param response HTTPレスポンス
     * @return CSRFトークン情報
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> getCsrfToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            // JWTからセッションIDを取得
            String sessionId = extractSessionId(request);
            if (sessionId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }

            // CSRFトークンを生成
            String csrfToken = csrfTokenService.generateToken(sessionId);

            // レスポンスヘッダーにトークンを設定
            response.setHeader("X-CSRF-TOKEN", csrfToken);

            // Cookieにもトークンを設定（Double Submit Cookie パターン）
            Cookie csrfCookie = new Cookie("CSRF-TOKEN", csrfToken);
            csrfCookie.setHttpOnly(false); // JavaScriptからアクセス可能にする
            csrfCookie.setSecure(false); // 開発環境ではfalse、本番環境ではtrue
            csrfCookie.setPath("/");
            csrfCookie.setMaxAge(30 * 60); // 30分

            response.addCookie(csrfCookie);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("csrfToken", csrfToken);
            responseBody.put("expiresIn", 30 * 60); // 30分（秒）
            responseBody.put("message", "CSRF token generated successfully");

            log.debug("CSRF token generated for session: {}", sessionId);
            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            log.error("Failed to generate CSRF token", e);
            return ResponseEntity.status(500).body(createErrorResponse("Failed to generate CSRF token"));
        }
    }

    /**
     * CSRFトークンを更新
     * 
     * @param request  HTTPリクエスト
     * @param response HTTPレスポンス
     * @return 更新されたCSRFトークン情報
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshCsrfToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        // 既存のトークンを無効化して新しいトークンを生成
        return getCsrfToken(request, response);
    }

    /**
     * CSRF保護の状態を確認
     * 
     * @param request HTTPリクエスト
     * @return CSRF保護の状態情報
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCsrfStatus(HttpServletRequest request) {

        try {
            String sessionId = extractSessionId(request);
            if (sessionId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }

            String currentToken = csrfTokenService.getTokenForSession(sessionId);
            boolean hasValidToken = currentToken != null;

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("csrfProtectionEnabled", true);
            responseBody.put("hasValidToken", hasValidToken);
            responseBody.put("sessionId", sessionId);

            if (hasValidToken) {
                responseBody.put("message", "CSRF protection is active with valid token");
            } else {
                responseBody.put("message", "CSRF protection is active but no valid token found");
            }

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            log.error("Failed to get CSRF status", e);
            return ResponseEntity.status(500).body(createErrorResponse("Failed to get CSRF status"));
        }
    }

    /**
     * JWTからセッションIDを抽出
     */
    private String extractSessionId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtUtil.extractUsername(token); // ユーザー名をセッションIDとして使用
            } catch (Exception e) {
                log.warn("Failed to extract session ID from JWT", e);
                return null;
            }
        }
        return null;
    }

    /**
     * エラーレスポンスを作成
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", "CSRF_TOKEN_ERROR");
        errorResponse.put("message", message);
        return errorResponse;
    }
}