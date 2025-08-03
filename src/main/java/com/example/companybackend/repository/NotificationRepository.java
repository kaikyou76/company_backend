package com.example.companybackend.repository;

import com.example.companybackend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 通知リポジトリ
 * comsys_test_dump.sql notificationsテーブル対応
 * 
 * テーブル構造:
 * - id (bigint PRIMARY KEY)
 * - user_id (integer NOT NULL REFERENCES users(id))
 * - title (character varying(255) NOT NULL)
 * - message (text NOT NULL)
 * - type (character varying(255) NOT NULL) - 'leave', 'correction', 'system'
 * - is_read (boolean DEFAULT false NOT NULL)
 * - related_id (integer)
 * - created_at (timestamp with time zone DEFAULT now() NOT NULL)
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * ユーザーID別通知取得（作成日時降順）
     * 
     * @param userId ユーザーID
     * @return 該当ユーザーの通知リスト
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);

    /**
     * ユーザーの未読通知取得
     * 
     * @param userId ユーザーID
     * @return 該当ユーザーの未読通知リスト
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Integer userId);

    /**
     * ユーザーの既読通知取得
     * 
     * @param userId ユーザーID
     * @return 該当ユーザーの既読通知リスト
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isRead = true ORDER BY n.createdAt DESC")
    List<Notification> findReadByUserId(@Param("userId") Integer userId);

    /**
     * ユーザーの未読通知数取得
     * 
     * @param userId ユーザーID
     * @return 未読通知数
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") Integer userId);

    /**
     * 通知タイプ別取得
     * 
     * @param userId ユーザーID
     * @param type   通知タイプ
     * @return 該当タイプの通知リスト
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndType(@Param("userId") Integer userId, @Param("type") String type);

    /**
     * 関連ID別通知取得
     * 
     * @param relatedId 関連ID
     * @param type      通知タイプ
     * @return 該当する通知リスト
     */
    @Query("SELECT n FROM Notification n WHERE n.relatedId = :relatedId AND n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByRelatedIdAndType(@Param("relatedId") Integer relatedId, @Param("type") String type);

    /**
     * 期間別通知取得
     * 
     * @param userId    ユーザーID
     * @param startDate 開始日時
     * @param endDate   終了日時
     * @return 指定期間内の通知リスト
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndCreatedAtBetween(@Param("userId") Integer userId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    /**
     * 古い通知取得（削除対象）
     * 
     * @param cutoffDate 削除対象日時
     * @return 削除対象通知リスト
     */
    @Query("SELECT n FROM Notification n WHERE n.createdAt < :cutoffDate")
    List<Notification> findOldNotifications(@Param("cutoffDate") OffsetDateTime cutoffDate);

    /**
     * 古い通知削除
     * 
     * @param cutoffDate 削除対象日時
     * @return 削除件数
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOldNotifications(@Param("cutoffDate") OffsetDateTime cutoffDate);

    /**
     * ユーザーの全通知を既読にする
     * 
     * @param userId ユーザーID
     * @return 更新件数
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Integer userId);

    /**
     * 重複通知検索（同一ユーザー、同一タイトル、指定時間内）
     * 
     * @param userId    ユーザーID
     * @param title     タイトル
     * @param afterTime 指定時間以降
     * @return 重複通知リスト
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.title = :title AND n.createdAt > :afterTime")
    List<Notification> findDuplicateNotifications(@Param("userId") Integer userId,
            @Param("title") String title,
            @Param("afterTime") OffsetDateTime afterTime);

    /**
     * 通知統計取得（期間別）
     * 
     * @param startDate 開始日時
     * @param endDate   終了日時
     * @return 通知統計
     */
    @Query("SELECT COUNT(n) as total, " +
            "COUNT(CASE WHEN n.isRead = true THEN 1 END) as readCount, " +
            "COUNT(CASE WHEN n.isRead = false THEN 1 END) as unreadCount, " +
            "COUNT(CASE WHEN n.type = 'leave' THEN 1 END) as leaveCount, " +
            "COUNT(CASE WHEN n.type = 'correction' THEN 1 END) as correctionCount, " +
            "COUNT(CASE WHEN n.type = 'system' THEN 1 END) as systemCount " +
            "FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate")
    Object[] getNotificationStatistics(@Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    /**
     * ユーザー別通知統計取得
     * 
     * @param userId ユーザーID
     * @return ユーザー別通知統計
     */
    @Query("SELECT COUNT(n) as total, " +
            "COUNT(CASE WHEN n.isRead = true THEN 1 END) as readCount, " +
            "COUNT(CASE WHEN n.isRead = false THEN 1 END) as unreadCount " +
            "FROM Notification n WHERE n.userId = :userId")
    Object[] getUserNotificationStatistics(@Param("userId") Integer userId);
}