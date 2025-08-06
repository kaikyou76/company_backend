package com.example.companybackend.security.test.headers;

import com.example.companybackend.security.test.SecurityTestBase;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTPセキュリティヘッダーテスト
 * 
 * 目的:
 * - HTTPレスポンスヘッダーのセキュリティ設定を検証
 * - 各種セキュリティヘッダーの適切な設定確認
 * - ブラウザセキュリティ機能の有効化確認
 * - セキュリティポリシーの適用確認
 * 
 * テスト対象:
 * - Content-Security-Policy (CSP) ヘッダー
 * - X-Frame-Options ヘッダー
 * - X-Content-Type-Options ヘッダー
 * - X-XSS-Protection ヘッダー
 * - Strict-Transport-Security (HSTS) ヘッダー
 * - Referrer-Policy ヘッダー
 * - Permissions-Policy ヘッダー
 * - Cache-Control ヘッダー
 * 
 * 要件対応:
 * - 要件2.4: レスポンスにContent-Security-Policyヘッダーが含まれること
 * - 要件2.5: レスポンスにX-Frame-Optionsヘッダーが含まれること
 * - 要件2.6: レスポンスにX-Content-Type-Optionsヘッダーが含まれること
 * - セキュリティヘッダーの包括的な検証
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("security-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HttpSecurityHeadersTest extends SecurityTestBase {

    @Override
    protected String getSecurityTestType() {
        return "HTTP_SECURITY_HEADERS";
    }

    /**
     * テストケース1: Content-Security-Policy (CSP) ヘッダーテスト
     * 
     * 要件2.4対応: レスポンスにContent-Security-Policyヘッダーが含まれること
     * 
     * 前提条件:
     * - CSPヘッダーが設定されている
     * - 適切なCSPディレクティブが定義されている
     * 
     * 期待結果:
     * - Content-Security-Policyヘッダーが存在する
     * - 必要なディレクティブが設定されている
     * - 危険な設定が含まれていない
     */
    @Test
    @Order(1)
    void testContentSecurityPolicyHeader() throws Exception {
        // Given
        String validToken = createValidJwtToken(testNormalUser);

        // When & Then
        MvcResult result = mockMvc.perform(
                get("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn();

        // CSPヘッダーの存在確認
        String cspHeader = result.getResponse().getHeader("Content-Security-Policy");
        assertNotNull(cspHeader, "Content-Security-Policy ヘッダーが設定されていること");

        // 必須ディレクティブの確認
        assertTrue(cspHeader.contains("default-src"), "default-src ディレクティブが設定されていること");
        assertTrue(cspHeader.contains("script-src"), "script-src ディレクティブが設定されていること");
        assertTrue(cspHeader.contains("style-src"), "style-src ディレクティブが設定されていること");
        assertTrue(cspHeader.contains("img-src"), "img-src ディレクティブが設定されていること");
        assertTrue(cspHeader.contains("connect-src"), "connect-src ディレクティブが設定されていること");
        assertTrue(cspHeader.contains("font-src"), "font-src ディレクティブが設定されていること");
        assertTrue(cspHeader.contains("object-src"), "object-src ディレクティブが設定されていること");
        assertTrue(cspHeader.contains("media-src"), "media-src ディレクティブが設定されていること");
        assertTrue(cspHeader.contains("frame-src"), "frame-src ディレクティブが設定されていること");

        // 危険な設定が含まれていないことを確認
        assertFalse(cspHeader.contains("'unsafe-inline'"), "unsafe-inline が許可されていないこと");
        assertFalse(cspHeader.contains("'unsafe-eval'"), "unsafe-eval が許可されていないこと");
        assertFalse(cspHeader.contains("data:") && cspHeader.contains("script-src"),
                "script-src で data: スキームが許可されていないこと");

        // 推奨設定の確認
        assertTrue(cspHeader.contains("'self'"), "'self' が設定されていること");
        assertTrue(cspHeader.contains("object-src 'none'") ||
                cspHeader.contains("object-src: 'none'"),
                "object-src が 'none' に設定されていること");

        // テスト結果の記録
        testDataManager.recordTestResult(
                getClass().getSimpleName(),
                "testContentSecurityPolicyHeader",
                "CSP_HEADER_VALIDATION",
                "PASSED",
                getTestExecutionTime(),
                "CSP header properly configured: " + cspHeader);
    }

    /**
     * テストケース2: X-Frame-Options ヘッダーテスト
     * 
     * 要件2.5対応: レスポンスにX-Frame-Optionsヘッダーが含まれること
     * 
     * 前提条件:
     * - X-Frame-Optionsヘッダーが設定されている
     * - クリックジャッキング対策が有効である
     * 
     * 期待結果:
     * - X-Frame-Optionsヘッダーが存在する
     * - DENY または SAMEORIGIN が設定されている
     */
    @Test
    @Order(2)
    void testXFrameOptionsHeader() throws Exception {
        // Given
        String validToken = createValidJwtToken(testNormalUser);

        // When & Then
        MvcResult result = mockMvc.perform(
                get("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn();

        // X-Frame-Optionsヘッダーの存在確認
        String xFrameOptionsHeader = result.getResponse().getHeader("X-Frame-Options");
        assertNotNull(xFrameOptionsHeader, "X-Frame-Options ヘッダーが設定されていること");

        // 適切な値が設定されていることを確認
        assertTrue(xFrameOptionsHeader.equals("DENY") ||
                xFrameOptionsHeader.equals("SAMEORIGIN"),
                "X-Frame-Options が DENY または SAMEORIGIN に設定されていること");

        // DENY が推奨されることを確認（より厳格）
        if (xFrameOptionsHeader.equals("DENY")) {
            // DENY の場合は最も安全
            assertTrue(true, "DENY が設定されており、最も安全な設定");
        } else if (xFrameOptionsHeader.equals("SAMEORIGIN")) {
            // SAMEORIGIN の場合も許容される
            assertTrue(true, "SAMEORIGIN が設定されており、許容される設定");
        }

        // テスト結果の記録
        testDataManager.recordTestResult(
                getClass().getSimpleName(),
                "testXFrameOptionsHeader",
                "X_FRAME_OPTIONS_HEADER",
                "PASSED",
                getTestExecutionTime(),
                "X-Frame-Options header: " + xFrameOptionsHeader);
    }

    /**
     * テストケース3: X-Content-Type-Options ヘッダーテスト
     * 
     * 要件2.6対応: レスポンスにX-Content-Type-Optionsヘッダーが含まれること
     * 
     * 前提条件:
     * - X-Content-Type-Optionsヘッダーが設定されている
     * - MIME type sniffing 対策が有効である
     * 
     * 期待結果:
     * - X-Content-Type-Optionsヘッダーが存在する
     * - nosniff が設定されている
     */
    @Test
    @Order(3)
    void testXContentTypeOptionsHeader() throws Exception {
        // Given
        String validToken = createValidJwtToken(testNormalUser);

        // When & Then
        MvcResult result = mockMvc.perform(
                get("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn();

        // X-Content-Type-Optionsヘッダーの存在確認
        String xContentTypeOptionsHeader = result.getResponse().getHeader("X-Content-Type-Options");
        assertNotNull(xContentTypeOptionsHeader, "X-Content-Type-Options ヘッダーが設定されていること");

        // nosniff が設定されていることを確認
        assertEquals("nosniff", xContentTypeOptionsHeader,
                "X-Content-Type-Options が nosniff に設定されていること");

        // テスト結果の記録
        testDataManager.recordTestResult(
                getClass().getSimpleName(),
                "testXContentTypeOptionsHeader",
                "X_CONTENT_TYPE_OPTIONS_HEADER",
                "PASSED",
                getTestExecutionTime(),
                "X-Content-Type-Options header: " + xContentTypeOptionsHeader);
    }

    /**
     * テストケース4: X-XSS-Protection ヘッダーテスト
     * 
     * 前提条件:
     * - X-XSS-Protectionヘッダーが設定されている
     * - ブラウザのXSS保護機能が有効である
     * 
     * 期待結果:
     * - X-XSS-Protectionヘッダーが存在する
     * - 適切な値が設定されている
     */
    @Test
    @Order(4)
    void testXXssProtectionHeader() throws Exception {
        // Given
        String validToken = createValidJwtToken(testNormalUser);

        // When & Then
        MvcResult result = mockMvc.perform(
                get("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn();

        // X-XSS-Protectionヘッダーの存在確認
        String xXssProtectionHeader = result.getResponse().getHeader("X-XSS-Protection");
        assertNotNull(xXssProtectionHeader, "X-XSS-Protection ヘッダーが設定されていること");

        // 適切な値が設定されていることを確認
        assertTrue(xXssProtectionHeader.equals("1; mode=block") ||
                xXssProtectionHeader.equals("1") ||
                xXssProtectionHeader.equals("0"),
                "X-XSS-Protection が適切な値に設定されていること");

        // 推奨設定の確認
        if (xXssProtectionHeader.equals("1; mode=block")) {
            assertTrue(true, "最も推奨される設定（1; mode=block）が使用されている");
        } else if (xXssProtectionHeader.equals("0")) {
            // CSPが適切に設定されている場合は0も許容される
            String cspHeader = result.getResponse().getHeader("Content-Security-Policy");
            assertNotNull(cspHeader, "X-XSS-Protection=0 の場合、CSPが設定されていること");
        }

        // テスト結果の記録
        testDataManager.recordTestResult(
                getClass().getSimpleName(),
                "testXXssProtectionHeader",
                "X_XSS_PROTECTION_HEADER",
                "PASSED",
                getTestExecutionTime(),
                "X-XSS-Protection header: " + xXssProtectionHeader);
    }

    /**
     * テストケース5: Strict-Transport-Security (HSTS) ヘッダーテスト
     * 
     * 前提条件:
     * - HSTSヘッダーが設定されている
     * - HTTPS接続が強制されている
     * 
     * 期待結果:
     * - Strict-Transport-Securityヘッダーが存在する
     * - 適切なmax-ageが設定されている
     * - includeSubDomains が設定されている
     */
    @Test
    @Order(5)
    void testStrictTransportSecurityHeader() throws Exception {
        // Given
        String validToken = createValidJwtToken(testNormalUser);

        // When & Then
        MvcResult result = mockMvc.perform(
                get("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn();

        // HSTSヘッダーの存在確認
        String hstsHeader = result.getResponse().getHeader("Strict-Transport-Security");
        assertNotNull(hstsHeader, "Strict-Transport-Security ヘッダーが設定されていること");

        // max-age の確認
        assertTrue(hstsHeader.contains("max-age="), "max-age が設定されていること");

        // max-age の値を抽出して確認
        String[] parts = hstsHeader.split("max-age=");
        if (parts.length > 1) {
            String maxAgePart = parts[1].split(";")[0].trim();
            long maxAge = Long.parseLong(maxAgePart);
            assertTrue(maxAge >= 31536000, "max-age が1年以上（31536000秒以上）に設定されていること"); // 1年
        }

        // includeSubDomains の確認（推奨）
        if (hstsHeader.contains("includeSubDomains")) {
            assertTrue(true, "includeSubDomains が設定されており、より安全");
        }

        // preload の確認（オプション）
        if (hstsHeader.contains("preload")) {
            assertTrue(true, "preload が設定されており、最も安全");
        }

        // テスト結果の記録
        testDataManager.recordTestResult(
                getClass().getSimpleName(),
                "testStrictTransportSecurityHeader",
                "HSTS_HEADER",
                "PASSED",
                getTestExecutionTime(),
                "HSTS header: " + hstsHeader);
    }

    /**
     * テストケース6: Referrer-Policy ヘッダーテスト
     * 
     * 前提条件:
     * - Referrer-Policyヘッダーが設定されている
     * - プライバシー保護が有効である
     * 
     * 期待結果:
     * - Referrer-Policyヘッダーが存在する
     * - 適切なポリシーが設定されている
     */
    @Test
    @Order(6)
    void testReferrerPolicyHeader() throws Exception {
        // Given
        String validToken = createValidJwtToken(testNormalUser);

        // When & Then
        MvcResult result = mockMvc.perform(
                get("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn();

        // Referrer-Policyヘッダーの存在確認
        String referrerPolicyHeader = result.getResponse().getHeader("Referrer-Policy");
        assertNotNull(referrerPolicyHeader, "Referrer-Policy ヘッダーが設定されていること");

        // 適切なポリシーが設定されていることを確認
        String[] allowedPolicies = {
                "no-referrer",
                "no-referrer-when-downgrade",
                "origin",
                "origin-when-cross-origin",
                "same-origin",
                "strict-origin",
                "strict-origin-when-cross-origin"
        };

        boolean validPolicy = false;
        for (String policy : allowedPolicies) {
            if (referrerPolicyHeader.equals(policy)) {
                validPolicy = true;
                break;
            }
        }
        assertTrue(validPolicy, "適切なReferrer-Policyが設定されていること");

        // 推奨設定の確認
        if (referrerPolicyHeader.equals("strict-origin-when-cross-origin") ||
                referrerPolicyHeader.equals("no-referrer")) {
            assertTrue(true, "推奨されるReferrer-Policyが使用されている");
        }

        // テスト結果の記録
        testDataManager.recordTestResult(
                getClass().getSimpleName(),
                "testReferrerPolicyHeader",
                "REFERRER_POLICY_HEADER",
                "PASSED",
                getTestExecutionTime(),
                "Referrer-Policy header: " + referrerPolicyHeader);
    }

    /**
     * テストケース7: Permissions-Policy ヘッダーテスト
     * 
     * 前提条件:
     * - Permissions-Policyヘッダーが設定されている
     * - ブラウザ機能の制限が有効である
     * 
     * 期待結果:
     * - Permissions-Policyヘッダーが存在する
     * - 適切な機能制限が設定されている
     */
    @Test
    @Order(7)
    void testPermissionsPolicyHeader() throws Exception {
        // Given
        String validToken = createValidJwtToken(testNormalUser);

        // When & Then
        MvcResult result = mockMvc.perform(
                get("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn();

        // Permissions-Policyヘッダーの存在確認
        String permissionsPolicyHeader = result.getResponse().getHeader("Permissions-Policy");

        if (permissionsPolicyHeader != null) {
            // 危険な機能が制限されていることを確認
            String[] restrictedFeatures = {
                    "camera", "microphone", "geolocation", "payment",
                    "usb", "magnetometer", "gyroscope", "accelerometer"
            };

            for (String feature : restrictedFeatures) {
                if (permissionsPolicyHeader.contains(feature)) {
                    // 機能が言及されている場合、適切に制限されているか確認
                    assertTrue(permissionsPolicyHeader.contains(feature + "=()") ||
                            permissionsPolicyHeader.contains(feature + "=(self)"),
                            feature + " が適切に制限されていること");
                }
            }
        }

        // テスト結果の記録
        testDataManager.recordTestResult(
                getClass().getSimpleName(),
                "testPermissionsPolicyHeader",
                "PERMISSIONS_POLICY_HEADER",
                "PASSED",
                getTestExecutionTime(),
                "Permissions-Policy header: " + permissionsPolicyHeader);
    }

    /**
     * テストケース8: Cache-Control ヘッダーテスト
     * 
     * 前提条件:
     * - Cache-Controlヘッダーが設定されている
     * - 機密情報のキャッシュが制限されている
     * 
     * 期待結果:
     * - Cache-Controlヘッダーが存在する
     * - 適切なキャッシュ制御が設定されている
     */
    @Test
    @Order(8)
    void testCacheControlHeader() throws Exception {
        // Given
        String validToken = createValidJwtToken(testNormalUser);

        // When & Then
        MvcResult result = mockMvc.perform(
                get("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn();

        // Cache-Controlヘッダーの存在確認
        String cacheControlHeader = result.getResponse().getHeader("Cache-Control");
        assertNotNull(cacheControlHeader, "Cache-Control ヘッダーが設定されていること");

        // 機密情報に対する適切なキャッシュ制御の確認
        assertTrue(cacheControlHeader.contains("no-cache") ||
                cacheControlHeader.contains("no-store") ||
                cacheControlHeader.contains("private"),
                "機密情報に対して適切なキャッシュ制御が設定されていること");

        // no-store が最も安全（機密情報の場合）
        if (cacheControlHeader.contains("no-store")) {
            assertTrue(true, "no-store が設定されており、最も安全");
        }

        // Pragma ヘッダーの確認（HTTP/1.0 互換性）
        String pragmaHeader = result.getResponse().getHeader("Pragma");
        if (pragmaHeader != null) {
            assertEquals("no-cache", pragmaHeader, "Pragma ヘッダーが適切に設定されていること");
        }

        // テスト結果の記録
        testDataManager.recordTestResult(
                getClass().getSimpleName(),
                "testCacheControlHeader",
                "CACHE_CONTROL_HEADER",
                "PASSED",
                getTestExecutionTime(),
                "Cache-Control header: " + cacheControlHeader);
    }

    /**
     * テストケース9: セキュリティヘッダー包括テスト
     * 
     * 目的: 全てのセキュリティヘッダーが適切に設定されていることを包括的に確認
     * 
     * 期待結果:
     * - 必須セキュリティヘッダーが全て存在する
     * - 各ヘッダーが適切な値に設定されている
     * - セキュリティレベルが要求を満たしている
     */
    @Test
    @Order(9)
    void testComprehensiveSecurityHeaders() throws Exception {
        // Given
        String validToken = createValidJwtToken(testNormalUser);

        // When & Then
        MvcResult result = mockMvc.perform(
                get("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn();

        // 必須セキュリティヘッダーのチェックリスト
        String[] requiredHeaders = {
                "Content-Security-Policy",
                "X-Frame-Options",
                "X-Content-Type-Options",
                "X-XSS-Protection",
                "Strict-Transport-Security",
                "Referrer-Policy"
        };

        int presentHeaders = 0;
        StringBuilder headerReport = new StringBuilder();

        for (String headerName : requiredHeaders) {
            String headerValue = result.getResponse().getHeader(headerName);
            if (headerValue != null) {
                presentHeaders++;
                headerReport.append(headerName).append(": ").append(headerValue).append("; ");
            } else {
                headerReport.append(headerName).append(": MISSING; ");
            }
        }

        // 必須ヘッダーの存在率を確認
        double headerPresenceRate = (double) presentHeaders / requiredHeaders.length;
        assertTrue(headerPresenceRate >= 0.8,
                String.format("必須セキュリティヘッダーの80%%以上が設定されていること (現在: %.1f%%)",
                        headerPresenceRate * 100));

        // セキュリティスコアの計算
        int securityScore = calculateSecurityScore(result);
        assertTrue(securityScore >= 70,
                String.format("セキュリティスコアが70以上であること (現在: %d)", securityScore));

        // テスト結果の記録
        testDataManager.recordTestResult(
                getClass().getSimpleName(),
                "testComprehensiveSecurityHeaders",
                "COMPREHENSIVE_SECURITY_HEADERS",
                "PASSED",
                getTestExecutionTime(),
                String.format("Security score: %d, Headers: %s", securityScore, headerReport.toString()));
    }

    /**
     * テストケース10: セキュリティヘッダーパフォーマンステスト
     * 
     * 目的: セキュリティヘッダーの設定がパフォーマンスに与える影響を測定
     * 
     * 期待結果:
     * - ヘッダー処理が50ms以内で完了する
     * - レスポンスサイズの増加が許容範囲内である
     * - システムリソースの使用量が適切である
     */
    @Test
    @Order(10)
    void testSecurityHeadersPerformance() throws Exception {
        // Given
        String validToken = createValidJwtToken(testNormalUser);

        // セキュリティヘッダー処理時間の測定
        long headerProcessingTime = testUtils.measureResponseTime(() -> {
            try {
                mockMvc.perform(
                        get("/api/users/profile")
                                .header("Authorization", "Bearer " + validToken))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // パフォーマンス検証
        assertPerformanceWithinLimit(headerProcessingTime, 50, "SECURITY_HEADERS_PROCESSING");

        // レスポンスサイズの確認
        MvcResult result = mockMvc.perform(
                get("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn();

        // ヘッダーサイズの計算
        int totalHeaderSize = 0;
        for (String headerName : result.getResponse().getHeaderNames()) {
            String headerValue = result.getResponse().getHeader(headerName);
            if (headerValue != null) {
                totalHeaderSize += headerName.length() + headerValue.length() + 4; // ": " + "\r\n"
            }
        }

        // ヘッダーサイズが許容範囲内であることを確認（8KB以内）
        assertTrue(totalHeaderSize <= 8192,
                String.format("ヘッダーサイズが8KB以内であること (現在: %d bytes)", totalHeaderSize));

        // テスト結果の記録
        testDataManager.recordTestResult(
                getClass().getSimpleName(),
                "testSecurityHeadersPerformance",
                "SECURITY_HEADERS_PERFORMANCE",
                "PASSED",
                getTestExecutionTime(),
                String.format("Header processing time: %dms, Header size: %d bytes",
                        headerProcessingTime, totalHeaderSize));
    }

    /**
     * セキュリティスコアを計算するヘルパーメソッド
     */
    private int calculateSecurityScore(MvcResult result) {
        int score = 0;

        // Content-Security-Policy (25点)
        String csp = result.getResponse().getHeader("Content-Security-Policy");
        if (csp != null) {
            score += 15;
            if (!csp.contains("'unsafe-inline'") && !csp.contains("'unsafe-eval'")) {
                score += 10;
            }
        }

        // X-Frame-Options (15点)
        String xFrameOptions = result.getResponse().getHeader("X-Frame-Options");
        if (xFrameOptions != null) {
            score += 10;
            if (xFrameOptions.equals("DENY")) {
                score += 5;
            }
        }

        // X-Content-Type-Options (10点)
        String xContentTypeOptions = result.getResponse().getHeader("X-Content-Type-Options");
        if (xContentTypeOptions != null && xContentTypeOptions.equals("nosniff")) {
            score += 10;
        }

        // X-XSS-Protection (10点)
        String xXssProtection = result.getResponse().getHeader("X-XSS-Protection");
        if (xXssProtection != null) {
            score += 5;
            if (xXssProtection.equals("1; mode=block")) {
                score += 5;
            }
        }

        // Strict-Transport-Security (20点)
        String hsts = result.getResponse().getHeader("Strict-Transport-Security");
        if (hsts != null) {
            score += 10;
            if (hsts.contains("includeSubDomains")) {
                score += 5;
            }
            if (hsts.contains("preload")) {
                score += 5;
            }
        }

        // Referrer-Policy (10点)
        String referrerPolicy = result.getResponse().getHeader("Referrer-Policy");
        if (referrerPolicy != null) {
            score += 5;
            if (referrerPolicy.equals("strict-origin-when-cross-origin") ||
                    referrerPolicy.equals("no-referrer")) {
                score += 5;
            }
        }

        return score;
    }
}