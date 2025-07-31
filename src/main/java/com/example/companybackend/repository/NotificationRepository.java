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
 * comsys_dump.sql notificationsテーブル完全対応
 * 
 * テーブル構造:
 * - id (SERIAL PRIMARY KEY)
 * - user_id (INTEGER NOT NULL REFERENCES users(id))
 * - title (TEXT NOT NULL)
 * - message (TEXT NOT NULL)
 * - is_read (BOOLEAN DEFAULT FALSE)
 * - read_at (TIMESTAMP WITH TIME ZONE)
 * - created_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 * - updated_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    /**
     * ユーザーID別通知取得
     * @param userId ユーザーID
     * @return 該当ユーザーの通知リスト
     */
    List<Notification> findByUserId(Integer userId);

    /**
     * ユーザーの未読通知取得
     * @param userId ユーザーID
     * @return 該当ユーザーの未読通知リスト
     */
    @Query(nativeQuery = true, value = "SELECT n.* FROM notifications n WHERE n.user_id = :userId AND n.is_read = false ORDER BY n.created_at DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Integer userId);

    /**
     * ユーザーの既読通知取得
     * @param userId ユーザーID
     * @return 該当ユーザーの既読通知リスト
     */
    @Query(nativeQuery = true, value = "SELECT n.* FROM notifications n WHERE n.user_id = :userId AND n.is_read = true ORDER BY n.read_at DESC")
    List<Notification> findReadByUserId(@Param("userId") Integer userId);

    /**
     * ユーザーの未読通知数取得
     * @param userId ユーザーID
     * @return 未読通知数
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(n) FROM notifications n WHERE n.user_id = :userId AND n.is_read = false")
    long countUnreadByUserId(@Param("userId") Integer userId);

    /**
     * ユーザーの最新通知取得
     * @param userId ユーザーID
     * @param limit 取得件数
     * @return 最新通知リスト
     */
    @Query(nativeQuery = true, value = "SELECT n.* FROM notifications n WHERE n.user_id = :userId ORDER BY n.created_at DESC LIMIT :limit")
    List<Notification> findLatestByUserId(@Param("userId") Integer userId, @Param("limit") int limit);

    /**
     * タイトル部分一致検索
     * @param userId ユーザーID
     * @param titlePattern タイトルパターン
     * @return 該当通知リスト
     */
    @Query(nativeQuery = true, value = "SELECT n.* FROM notifications n WHERE n.user_id = :userId AND n.title LIKE CONCAT('%', :titlePattern, '%') ORDER BY n.created_at DESC")
    List<Notification> findByUserIdAndTitleContaining(@Param("userId") Integer userId, @Param("titlePattern") String titlePattern);

    /**
     * メッセージ部分一致検索
     * @param userId ユーザーID
     * @param messagePattern メッセージパターン
     * @return 該当通知リスト
     */
    @Query(nativeQuery = true, value = "SELECT n.* FROM notifications n WHERE n.user_id = :userId AND n.message LIKE CONCAT('%', :messagePattern, '%') ORDER BY n.created_at DESC")
    List<Notification> findByUserIdAndMessageContaining(@Param("userId") Integer userId, @Param("messagePattern") String messagePattern);

    /**
     * 期間別通知取得
     * @param userId ユーザーID
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 指定期間内の通知リスト
     */
    @Query(nativeQuery = true, value = "SELECT n.* FROM notifications n WHERE n.user_id = :userId AND n.created_at BETWEEN :startDate AND :endDate ORDER BY n.created_at DESC")
    List<Notification> findByUserIdAndCreatedAtBetween(@Param("userId") Integer userId, @Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    /**
     * 通知一括既読化
     * @param userId ユーザーID
     * @param readAt 既読時刻
     * @return 更新件数
     */
    @Modifying
    @Query(nativeQuery = true, value = "UPDATE notifications n SET n.is_read = true, n.read_at = :readAt, n.updated_at = :readAt WHERE n.user_id = :userId AND n.is_read = false")
    int markAllAsReadByUserId(@Param("userId") Integer userId, @Param("readAt") OffsetDateTime readAt);

    /**
     * 特定通知既読化
     * @param notificationId 通知ID
     * @param readAt 既読時刻
     * @return 更新件数
     */
    @Modifying
    @Query(nativeQuery = true, value = "UPDATE notifications n SET n.is_read = true, n.read_at = :readAt, n.updated_at = :readAt WHERE n.id = :notificationId AND n.is_read = false")
    int markAsRead(@Param("notificationId") Integer notificationId, @Param("readAt") OffsetDateTime readAt);

    /**
     * 古い通知削除用検索
     * @param cutoffDate 削除対象日時
     * @return 削除対象通知リスト
     */
    @Query(nativeQuery = true, value = "SELECT n.* FROM notifications n WHERE n.created_at < :cutoffDate")
    List<Notification> findOldNotifications(@Param("cutoffDate") OffsetDateTime cutoffDate);

    /**
     * 古い既読通知削除
     * @param cutoffDate 削除対象日時
     * @return 削除件数
     */
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM notifications WHERE is_read = true AND read_at < :cutoffDate")
    int deleteOldReadNotifications(@Param("cutoffDate") OffsetDateTime cutoffDate);

    /**
     * ユーザー通知統計取得
     * @param userId ユーザーID
     * @return 通知統計情報
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            COUNT(n) as totalNotifications,
            COUNT(CASE WHEN n.is_read = false THEN 1 END) as unreadCount,
            COUNT(CASE WHEN n.is_read = true THEN 1 END) as readCount
        FROM notifications n 
        WHERE n.user_id = :userId
        """)
    java.util.Map<String, Object> getNotificationStatistics(@Param("userId") Integer userId);

    /**
     * 全ユーザー通知統計
     * @return 全ユーザーの通知統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            n.user_id as userId,
            COUNT(n) as totalNotifications,
            COUNT(CASE WHEN n.is_read = false THEN 1 END) as unreadCount,
            COUNT(CASE WHEN n.is_read = true THEN 1 END) as readCount
        FROM notifications n 
        GROUP BY n.user_id
        ORDER BY COUNT(CASE WHEN n.is_read = false THEN 1 END) DESC
        """)
    List<java.util.Map<String, Object>> getAllUsersNotificationStatistics();

    /**
     * 日別通知作成統計
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 日別通知作成統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            DATE(n.created_at) as date,
            COUNT(n) as notificationCount
        FROM notifications n 
        WHERE n.created_at BETWEEN :startDate AND :endDate
        GROUP BY DATE(n.created_at)
        ORDER BY DATE(n.created_at) ASC
        """)
    List<java.util.Map<String, Object>> getDailyNotificationStatistics(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    /**
     * 最近の通知取得（全ユーザー）
     * @param hours 過去何時間以内
     * @return 最近の通知リスト
     */
    @Query(nativeQuery = true, value = "SELECT n.* FROM notifications n WHERE n.created_at >= CURRENT_TIMESTAMP - INTERVAL ':hours hours' ORDER BY n.created_at DESC")
    List<Notification> findRecentNotifications(@Param("hours") int hours);

    /**
     * 長期間未読の通知取得
     * @param days 何日以上未読
     * @return 長期間未読の通知リスト
     */
    @Query(nativeQuery = true, value = "SELECT n.* FROM notifications n WHERE n.is_read = false AND n.created_at < CURRENT_TIMESTAMP - INTERVAL ':days days' ORDER BY n.created_at ASC")
    List<Notification> findLongUnreadNotifications(@Param("days") int days);

    /**
     * 重複通知検索
     * 同一ユーザーに同一タイトルで短時間内に作成された通知
     * @param userId ユーザーID
     * @param title タイトル
     * @param minutes 何分以内
     * @return 重複通知リスト
     */
    @Query(nativeQuery = true, value = "SELECT n.* FROM notifications n WHERE n.user_id = :userId AND n.title = :title AND n.created_at >= CURRENT_TIMESTAMP - INTERVAL ':minutes minutes'")
    List<Notification> findDuplicateNotifications(@Param("userId") Integer userId, @Param("title") String title, @Param("minutes") int minutes);

    /**
     * バッチ処理用：大量データ通知取得
     * @param offset オフセット
     * @param limit 取得件数
     * @return 通知リスト
     */
    @Query(nativeQuery = true, value = "SELECT n.* FROM notifications n ORDER BY n.id ASC LIMIT :limit OFFSET :offset")
    List<Notification> findForBatchProcessing(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * データ整合性チェック：孤立ユーザー参照検索
     * @return 存在しないユーザーを参照している通知リスト
     */
    @Query(nativeQuery = true, value = """
        SELECT n.* FROM notifications n 
        WHERE NOT EXISTS (
            SELECT 1 FROM users u WHERE u.id = n.user_id
        )
        """)
    List<Notification> findNotificationsWithOrphanedUserId();

    /**
     * 既読率統計取得
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 期間内の既読率統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            COUNT(n) as totalNotifications,
            COUNT(CASE WHEN n.is_read = true THEN 1 END) as readNotifications,
            CASE WHEN COUNT(n) > 0 
                THEN CAST(COUNT(CASE WHEN n.is_read = true THEN 1 END) * 100.0 / COUNT(n) AS double precision)
                ELSE 0.0 
            END as readRate
        FROM notifications n 
        WHERE n.created_at BETWEEN :startDate AND :endDate
        """)
    java.util.Map<String, Object> getReadRateStatistics(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);
}