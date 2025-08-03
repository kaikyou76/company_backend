package com.example.companybackend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "related_id")
    private Integer relatedId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public Notification() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Integer getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Integer relatedId) {
        this.relatedId = relatedId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Helper methods

    /**
     * 通知が未読かどうかを確認
     * 
     * @return 未読の場合true
     */
    public boolean isUnread() {
        return !Boolean.TRUE.equals(this.isRead);
    }

    /**
     * 通知を既読にする
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * 通知タイプが休暇申請かどうかを確認
     * 
     * @return 休暇申請通知の場合true
     */
    public boolean isLeaveNotification() {
        return "leave".equals(this.type);
    }

    /**
     * 通知タイプが時刻修正申請かどうかを確認
     * 
     * @return 時刻修正申請通知の場合true
     */
    public boolean isCorrectionNotification() {
        return "correction".equals(this.type);
    }

    /**
     * 通知タイプがシステム通知かどうかを確認
     * 
     * @return システム通知の場合true
     */
    public boolean isSystemNotification() {
        return "system".equals(this.type);
    }

    /**
     * 通知が古いかどうかを確認（30日以上前）
     * 
     * @return 30日以上前の通知の場合true
     */
    public boolean isOld() {
        if (this.createdAt == null) {
            return false;
        }
        return this.createdAt.isBefore(OffsetDateTime.now().minusDays(30));
    }

    /**
     * 通知の作成用ファクトリーメソッド
     * 
     * @param userId    ユーザーID
     * @param title     タイトル
     * @param message   メッセージ
     * @param type      通知タイプ
     * @param relatedId 関連ID
     * @return 新しい通知インスタンス
     */
    public static Notification create(Integer userId, String title, String message, String type, Integer relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRelatedId(relatedId);
        notification.setIsRead(false);
        notification.setCreatedAt(OffsetDateTime.now());
        return notification;
    }
}