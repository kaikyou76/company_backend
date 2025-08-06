package com.example.companybackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * セキュリティイベントエンティティ
 * セキュリティ関連のイベントを記録するためのエンティティクラス
 */
@Entity
@Table(name = "security_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "username")
    private String username;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "details")
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // 新增字段
    @Column(name = "request_uri")
    private String requestUri;
    
    @Column(name = "http_method")
    private String httpMethod;
    
    @Column(name = "reason")
    private String reason;
    
    @Column(name = "payload")
    private String payload;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "severity_level")
    private String severityLevel;
    
    @Column(name = "action_taken")
    private String actionTaken;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static class Builder {
        private Long id;
        private String eventType;
        private String username;
        private String ipAddress;
        private String userAgent;
        private String details;
        private LocalDateTime createdAt;
        private String requestUri;
        private String httpMethod;
        private String reason;
        private String payload;
        private Long userId;
        private String sessionId;
        private String severityLevel;
        private String actionTaken;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder requestUri(String requestUri) {
            this.requestUri = requestUri;
            return this;
        }
        
        public Builder httpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }
        
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }
        
        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder severityLevel(String severityLevel) {
            this.severityLevel = severityLevel;
            return this;
        }
        
        public Builder actionTaken(String actionTaken) {
            this.actionTaken = actionTaken;
            return this;
        }

        public SecurityEvent build() {
            SecurityEvent securityEvent = new SecurityEvent();
            securityEvent.id = this.id;
            securityEvent.eventType = this.eventType;
            securityEvent.username = this.username;
            securityEvent.ipAddress = this.ipAddress;
            securityEvent.userAgent = this.userAgent;
            securityEvent.details = this.details;
            securityEvent.createdAt = this.createdAt;
            securityEvent.requestUri = this.requestUri;
            securityEvent.httpMethod = this.httpMethod;
            securityEvent.reason = this.reason;
            securityEvent.payload = this.payload;
            securityEvent.userId = this.userId;
            securityEvent.sessionId = this.sessionId;
            securityEvent.severityLevel = this.severityLevel;
            securityEvent.actionTaken = this.actionTaken;
            return securityEvent;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}