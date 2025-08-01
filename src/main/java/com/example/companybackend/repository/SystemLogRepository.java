package com.example.companybackend.repository;

import com.example.companybackend.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * システムログリポジトリ
 * comsys_dump.sql system_logsテーブル完全対応
 * 
 * テーブル構造:
 * - id (SERIAL PRIMARY KEY)
 * - user_id (INTEGER REFERENCES users(id))
 * - action (TEXT NOT NULL)
 * - ip_address (INET)
 * - user_agent (TEXT)
 * - details (JSONB)
 * - created_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 */
@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Integer> {

    /**
     * ユーザーID別ログ取得
     * @param userId ユーザーID
     * @return 該当ユーザーのログリスト
     */
    List<SystemLog> findByUserId(Integer userId);

    /**
     * アクション別ログ取得
     * @param action アクション
     * @return 該当アクションのログリスト
     */
    List<SystemLog> findByAction(String action);

    /**
     * IPアドレス別ログ取得
     * @param ipAddress IPアドレス
     * @return 該当IPアドレスのログリスト
     */
    List<SystemLog> findByIpAddress(String ipAddress);

    /**
     * 期間別ログ取得
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 指定期間内のログリスト
     */
    @Query(nativeQuery = true, value = "SELECT sl.* FROM system_logs sl WHERE sl.created_at BETWEEN :startDate AND :endDate ORDER BY sl.created_at DESC")
    List<SystemLog> findByCreatedAtBetween(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    /**
     * ユーザー・アクション別ログ取得
     * @param userId ユーザーID
     * @param action アクション
     * @return 該当ユーザーの指定アクションログリスト
     */
    List<SystemLog> findByUserIdAndAction(Integer userId, String action);

    /**
     * 最新ログ取得（件数指定）
     * @param limit 取得件数
     * @return 最新ログリスト
     */
    @Query(nativeQuery = true, value = "SELECT sl.* FROM system_logs sl ORDER BY sl.created_at DESC LIMIT :limit")
    List<SystemLog> findLatestLogs(@Param("limit") int limit);

    /**
     * 今日のログ取得
     * @return 今日のログリスト
     */
    @Query(nativeQuery = true, value = "SELECT sl.* FROM system_logs sl WHERE DATE(sl.created_at) = CURRENT_DATE ORDER BY sl.created_at DESC")
    List<SystemLog> findTodayLogs();

    /**
     * 過去N時間のログ取得
     * @param hours 時間数
     * @return 過去N時間のログリスト
     */
    @Query(nativeQuery = true, value = "SELECT sl.* FROM system_logs sl WHERE sl.created_at >= CURRENT_TIMESTAMP - INTERVAL ':hours hours' ORDER BY sl.created_at DESC")
    List<SystemLog> findRecentLogs(@Param("hours") int hours);

    /**
     * ユーザーの最新ログ取得
     * @param userId ユーザーID
     * @param limit 取得件数
     * @return 該当ユーザーの最新ログリスト
     */
    @Query(nativeQuery = true, value = "SELECT sl.* FROM system_logs sl WHERE sl.user_id = :userId ORDER BY sl.created_at DESC LIMIT :limit")
    List<SystemLog> findLatestLogsByUser(@Param("userId") Integer userId, @Param("limit") int limit);

    /**
     * アクション別統計取得
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return アクション別カウント統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            sl.action as action,
            COUNT(sl) as count
        FROM system_logs sl 
        WHERE sl.created_at BETWEEN :startDate AND :endDate
        GROUP BY sl.action
        ORDER BY COUNT(sl) DESC
        """)
    List<java.util.Map<String, Object>> getActionStatistics(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    /**
     * ユーザー別アクティビティ統計
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return ユーザー別アクティビティ統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            sl.user_id as userId,
            COUNT(sl) as actionCount,
            COUNT(DISTINCT sl.action) as uniqueActions
        FROM system_logs sl 
        WHERE sl.created_at BETWEEN :startDate AND :endDate
        GROUP BY sl.user_id
        ORDER BY COUNT(sl) DESC
        """)
    List<java.util.Map<String, Object>> getUserActivityStatistics(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    /**
     * IPアドレス別アクセス統計
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return IPアドレス別アクセス統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            sl.ip_address as ipAddress,
            COUNT(sl) as accessCount,
            COUNT(DISTINCT sl.user_id) as uniqueUsers
        FROM system_logs sl 
        WHERE sl.created_at BETWEEN :startDate AND :endDate
        AND sl.ip_address IS NOT NULL
        GROUP BY sl.ip_address
        ORDER BY COUNT(sl) DESC
        """)
    List<java.util.Map<String, Object>> getIpAccessStatistics(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    /**
     * 時間別アクティビティ統計
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 時間別アクティビティ統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            EXTRACT(HOUR FROM sl.created_at) as hour,
            COUNT(sl) as activityCount
        FROM system_logs sl 
        WHERE sl.created_at BETWEEN :startDate AND :endDate
        GROUP BY EXTRACT(HOUR FROM sl.created_at)
        ORDER BY EXTRACT(HOUR FROM sl.created_at) ASC
        """)
    List<java.util.Map<String, Object>> getHourlyActivityStatistics(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    /**
     * 疑わしいアクティビティ検索
     * 同一IPから短時間で大量アクセス
     * @param hours 時間数
     * @param threshold アクセス数閾値
     * @return 疑わしいIPアドレスリスト
     */
    @Query(nativeQuery = true, value = """
        SELECT sl.ip_address, COUNT(sl) as accessCount
        FROM system_logs sl 
        WHERE sl.created_at >= CURRENT_TIMESTAMP - INTERVAL ':hours hours'
        AND sl.ip_address IS NOT NULL
        GROUP BY sl.ip_address
        HAVING COUNT(sl) > :threshold
        ORDER BY COUNT(sl) DESC
        """)
    List<Object[]> findSuspiciousActivity(@Param("hours") int hours, @Param("threshold") long threshold);

    /**
     * 失敗ログイン試行検索
     * @param hours 時間数
     * @return 失敗ログイン試行リスト
     */
    @Query(nativeQuery = true, value = "SELECT sl.* FROM system_logs sl WHERE sl.action LIKE '%login%failed%' AND sl.created_at >= CURRENT_TIMESTAMP - INTERVAL ':hours hours' ORDER BY sl.created_at DESC")
    List<SystemLog> findFailedLoginAttempts(@Param("hours") int hours);

    /**
     * User Agent別統計
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return User Agent別統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            sl.user_agent as userAgent,
            COUNT(sl) as count
        FROM system_logs sl 
        WHERE sl.created_at BETWEEN :startDate AND :endDate
        AND sl.user_agent IS NOT NULL
        GROUP BY sl.user_agent
        ORDER BY COUNT(sl) DESC
        """)
    List<java.util.Map<String, Object>> getUserAgentStatistics(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    /**
     * ログ総数取得（期間指定）
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return ログ総数
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(sl) FROM system_logs sl WHERE sl.created_at BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    /**
     * 古いログ削除用検索
     * @param cutoffDate 削除対象日時
     * @return 削除対象ログリスト
     */
    @Query(nativeQuery = true, value = "SELECT sl.* FROM system_logs sl WHERE sl.created_at < :cutoffDate")
    List<SystemLog> findLogsOlderThan(@Param("cutoffDate") OffsetDateTime cutoffDate);

    /**
     * バッチ処理用：大量データログ取得
     * @param offset オフセット
     * @param limit 取得件数
     * @return ログリスト
     */
    @Query(nativeQuery = true, value = "SELECT sl.* FROM system_logs sl ORDER BY sl.id ASC LIMIT :limit OFFSET :offset")
    List<SystemLog> findForBatchProcessing(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * データ整合性チェック：孤立ユーザー参照検索
     * @return 存在しないユーザーを参照しているログリスト
     */
    @Query(nativeQuery = true, value = """
        SELECT sl.* FROM system_logs sl 
        WHERE sl.user_id IS NOT NULL 
        AND NOT EXISTS (
            SELECT 1 FROM users u WHERE u.id = sl.user_id
        )
        """)
    List<SystemLog> findLogsWithOrphanedUserId();

    /**
     * JSON詳細情報による検索
     * @param jsonPath JSONBパス
     * @param value 検索値
     * @return 該当ログリスト
     */
    @Query(nativeQuery = true, value = "SELECT sl.* FROM system_logs sl WHERE jsonb_extract_path_text(sl.details, :jsonPath) = :value")
    List<SystemLog> findByJsonDetails(@Param("jsonPath") String jsonPath, @Param("value") String value);
    
    // 追加メソッド
    
    /**
     * フィルター条件でログを検索（ページング対応）
     * @param action アクション
     * @param status ステータス
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @param pageable ページング情報
     * @return ページングされたログリスト
     */
    @Query(nativeQuery = true, value = """
        SELECT sl.* FROM system_logs sl 
        WHERE (:action IS NULL OR sl.action = :action)
        AND (:status IS NULL OR sl.status = :status)
        AND (:startDate IS NULL OR sl.created_at >= :startDate)
        AND (:endDate IS NULL OR sl.created_at <= :endDate)
        ORDER BY sl.created_at DESC
        """)
    Page<SystemLog> findByFilters(@Param("action") String action, 
                                  @Param("status") String status,
                                  @Param("startDate") OffsetDateTime startDate,
                                  @Param("endDate") OffsetDateTime endDate,
                                  Pageable pageable);
                                  
    /**
     * エクスポート用にログを検索
     * @param action アクション
     * @param status ステータス
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return ログリスト
     */
    @Query(nativeQuery = true, value = """
        SELECT sl.* FROM system_logs sl 
        WHERE (:action IS NULL OR sl.action = :action)
        AND (:status IS NULL OR sl.status = :status)
        AND (:startDate IS NULL OR sl.created_at >= :startDate)
        AND (:endDate IS NULL OR sl.created_at <= :endDate)
        ORDER BY sl.created_at DESC
        """)
    List<SystemLog> findForExport(@Param("action") String action, 
                                  @Param("status") String status,
                                  @Param("startDate") OffsetDateTime startDate,
                                  @Param("endDate") OffsetDateTime endDate);

    /**
     * アクション別カウント統計を取得
     * @return アクション別カウント統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            sl.action as action,
            COUNT(sl) as count
        FROM system_logs sl 
        GROUP BY sl.action
        ORDER BY COUNT(sl) DESC
        """)
    List<java.util.Map<String, Object>> countByActionGrouped();

    /**
     * ステータス別カウント統計を取得
     * @return ステータス別カウント統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            sl.status as status,
            COUNT(sl) as count
        FROM system_logs sl 
        GROUP BY sl.status
        ORDER BY COUNT(sl) DESC
        """)
    List<java.util.Map<String, Object>> countByStatusGrouped();

    /**
     * ユーザー別カウント統計を取得
     * @return ユーザー別カウント統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            sl.user_id as userId,
            COUNT(sl) as count
        FROM system_logs sl 
        WHERE sl.user_id IS NOT NULL
        GROUP BY sl.user_id
        ORDER BY COUNT(sl) DESC
        """)
    List<java.util.Map<String, Object>> countByUserGrouped();

    /**
     * 日別カウント統計を取得
     * @return 日別カウント統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            DATE(sl.created_at) as date,
            COUNT(sl) as count
        FROM system_logs sl 
        GROUP BY DATE(sl.created_at)
        ORDER BY DATE(sl.created_at) DESC
        """)
    List<java.util.Map<String, Object>> countByDateGrouped();

    /**
     * キーワードでログを検索（ページング対応）
     * @param keyword キーワード
     * @param pageable ページング情報
     * @return ページングされたログリスト
     */
    @Query(nativeQuery = true, value = """
        SELECT sl.* FROM system_logs sl 
        WHERE sl.action ILIKE '%' || :keyword || '%'
        OR sl.status ILIKE '%' || :keyword || '%'
        OR sl.ip_address ILIKE '%' || :keyword || '%'
        OR sl.user_agent ILIKE '%' || :keyword || '%'
        OR CAST(sl.user_id AS TEXT) ILIKE '%' || :keyword || '%'
        ORDER BY sl.created_at DESC
        """)
    Page<SystemLog> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}