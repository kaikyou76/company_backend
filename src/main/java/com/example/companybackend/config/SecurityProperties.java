package com.example.companybackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * セキュリティ関連の設定プロパティ
 */
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private String[] allowedOrigins = { "http://localhost:3000" };
    private int maxAge = 3600;
    private boolean httpsEnabled = false;

    // Getters and Setters
    public String[] getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public boolean isHttpsEnabled() {
        return httpsEnabled;
    }

    public void setHttpsEnabled(boolean httpsEnabled) {
        this.httpsEnabled = httpsEnabled;
    }
}