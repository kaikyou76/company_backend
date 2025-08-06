package com.example.companybackend.security.exception;

/**
 * CSRF保護例外クラス
 * 
 * CSRF攻撃が検出された際に発生する例外
 */
public class CsrfProtectionException extends RuntimeException {

    private final CsrfViolationType violationType;
    private final String clientIp;
    private final String requestUri;

    public CsrfProtectionException(CsrfViolationType violationType,
            String message, String clientIp, String requestUri) {
        super(message);
        this.violationType = violationType;
        this.clientIp = clientIp;
        this.requestUri = requestUri;
    }

    public CsrfViolationType getViolationType() {
        return violationType;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getRequestUri() {
        return requestUri;
    }

    /**
     * CSRF違反タイプ
     */
    public enum CsrfViolationType {
        INVALID_ORIGIN("Invalid origin header"),
        MISSING_TOKEN("Missing CSRF token"),
        INVALID_TOKEN("Invalid CSRF token"),
        EXPIRED_TOKEN("Expired CSRF token"),
        TOKEN_MISMATCH("CSRF token mismatch"),
        MISSING_ORIGIN("Missing origin and referer headers");

        private final String description;

        CsrfViolationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}