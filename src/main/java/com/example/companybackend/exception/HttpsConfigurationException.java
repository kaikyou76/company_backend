package com.example.companybackend.exception;

/**
 * HTTPS設定関連の例外
 */
public class HttpsConfigurationException extends RuntimeException {

    public HttpsConfigurationException(String message) {
        super(message);
    }

    public HttpsConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}