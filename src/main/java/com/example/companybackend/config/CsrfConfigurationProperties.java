package com.example.companybackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * CSRF保護設定プロパティ
 * 
 * 緊急対応として基本的なOrigin検証を実装
 * 段階的にCSRF保護機能を強化していく
 */
@Data
@ConfigurationProperties(prefix = "app.security.csrf")
public class CsrfConfigurationProperties {

        /**
         * CSRF保護の有効/無効
         */
        private boolean enabled = true;


        /**
         * 許可されたオリジンのリスト
         */
        private List<String> allowedOrigins = Arrays.asList(
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "https://localhost:3000",
                        "https://localhost:8080",
                        "https://main.d1inikqen7hbn4.amplifyapp.com");

        /**
         * 除外パス（CSRF保護を適用しないパス）
         */
        private List<String> excludedPaths = Arrays.asList(
                        "/actuator/**",
                        "/api/public/**");

        /**
         * 状態変更操作として扱うHTTPメソッド
         */
        private List<String> protectedMethods = Arrays.asList(
                        "POST", "PUT", "DELETE", "PATCH");
}