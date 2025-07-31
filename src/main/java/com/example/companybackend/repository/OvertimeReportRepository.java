package com.example.companybackend.repository;

import com.example.companybackend.entity.OvertimeReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 残業報告リポジトリ
 * comsys_dump.sql overtime_reportsテーブル完全対応
 * 
 * テーブル構造:
 * - id (SERIAL PRIMARY KEY)
 * - user_id (INTEGER NOT NULL REFERENCES users(id))
 * - date (DATE NOT NULL)
 * - hours (DECIMAL(4,2) NOT NULL)
 * - reason (TEXT)
 * - created_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 * - updated_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 */
@Repository
public interface OvertimeReportRepository extends JpaRepository<OvertimeReport, Integer> {

    /**
     * ユーザーID別残業報告取得
     * @param userId ユーザーID
     * @return 該当ユーザーの残業報告リスト
     */
    List<OvertimeReport> findByUserId(Integer userId);

    /**
     * 対象月別残業報告取得
     * @param targetMonth 対象月
     * @return 該当月の残業報告リスト
     */
    List<OvertimeReport> findByTargetMonth(LocalDate targetMonth);

    /**
     * ユーザー・対象月別残業報告取得
     * @param userId ユーザーID
     * @param targetMonth 対象月
     * @return 該当ユーザーの指定月残業報告リスト
     */
    List<OvertimeReport> findByUserIdAndTargetMonth(Integer userId, LocalDate targetMonth);

    /**
     * 期間別残業報告取得
     * @param userId ユーザーID
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 指定期間内の残業報告リスト
     */
    @Query(nativeQuery = true, value = "SELECT ot.* FROM overtime_reports ot WHERE ot.user_id = :userId AND ot.target_month BETWEEN :startDate AND :endDate ORDER BY ot.target_month DESC")
    List<OvertimeReport> findByUserIdAndTargetMonthBetween(@Param("userId") Integer userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 月別残業報告取得
     * @param userId ユーザーID
     * @param year 年
     * @param month 月
     * @return 指定年月の残業報告リスト
     */
    @Query(nativeQuery = true, value = "SELECT ot.* FROM overtime_reports ot WHERE ot.user_id = :userId AND EXTRACT(YEAR FROM ot.target_month) = :year AND EXTRACT(MONTH FROM ot.target_month) = :month ORDER BY ot.target_month ASC")
    List<OvertimeReport> findByUserIdAndYearAndMonth(@Param("userId") Integer userId, @Param("year") int year, @Param("month") int month);

    /**
     * 年別残業報告取得
     * @param userId ユーザーID
     * @param year 年
     * @return 指定年の残業報告リスト
     */
    @Query(nativeQuery = true, value = "SELECT ot.* FROM overtime_reports ot WHERE ot.user_id = :userId AND EXTRACT(YEAR FROM ot.target_month) = :year ORDER BY ot.target_month ASC")
    List<OvertimeReport> findByUserIdAndYear(@Param("userId") Integer userId, @Param("year") int year);

    /**
     * 残業時間閾値以上の報告取得
     * @param minHours 最小残業時間
     * @return 指定時間以上の残業報告リスト
     */
    @Query(nativeQuery = true, value = "SELECT ot.* FROM overtime_reports ot WHERE ot.total_overtime >= :minHours ORDER BY ot.total_overtime DESC, ot.target_month DESC")
    List<OvertimeReport> findByHoursGreaterThanEqual(@Param("minHours") java.math.BigDecimal minHours);

    /**
     * ユーザーの月間残業時間合計取得
     * @param userId ユーザーID
     * @param year 年
     * @param month 月
     * @return 月間残業時間合計
     */
    @Query(nativeQuery = true, value = "SELECT COALESCE(SUM(ot.total_overtime), 0) FROM overtime_reports ot WHERE ot.user_id = :userId AND EXTRACT(YEAR FROM ot.target_month) = :year AND EXTRACT(MONTH FROM ot.target_month) = :month")
    java.math.BigDecimal getTotalHoursByUserIdAndYearAndMonth(@Param("userId") Integer userId, @Param("year") int year, @Param("month") int month);

    /**
     * ユーザーの年間残業時間合計取得
     * @param userId ユーザーID
     * @param year 年
     * @return 年間残業時間合計
     */
    @Query(nativeQuery = true, value = "SELECT COALESCE(SUM(ot.total_overtime), 0) FROM overtime_reports ot WHERE ot.user_id = :userId AND EXTRACT(YEAR FROM ot.target_month) = :year")
    java.math.BigDecimal getTotalHoursByUserIdAndYear(@Param("userId") Integer userId, @Param("year") int year);

    /**
     * 期間内残業時間合計取得
     * @param userId ユーザーID
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 期間内残業時間合計
     */
    @Query(nativeQuery = true, value = "SELECT COALESCE(SUM(ot.total_overtime), 0) FROM overtime_reports ot WHERE ot.user_id = :userId AND ot.target_month BETWEEN :startDate AND :endDate")
    java.math.BigDecimal getTotalHoursByUserIdAndDateBetween(@Param("userId") Integer userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 月別残業統計取得
     * @param targetMonth 対象月
     * @return 指定月の残業統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            COUNT(ot) as reportCount,
            COALESCE(SUM(ot.total_overtime), 0) as totalHours,
            COALESCE(AVG(ot.total_overtime), 0) as avgHours,
            COALESCE(MAX(ot.total_overtime), 0) as maxHours
        FROM overtime_reports ot 
        WHERE ot.target_month = :targetMonth
        """)
    java.util.Map<String, Object> getMonthlyStatistics(@Param("targetMonth") LocalDate targetMonth);

    /**
     * 月別残業統計取得
     * @param year 年
     * @param month 月
     * @return 指定年月の残業統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            COUNT(ot) as reportCount,
            COUNT(DISTINCT ot.user_id) as uniqueUsers,
            COALESCE(SUM(ot.total_overtime), 0) as totalHours,
            COALESCE(AVG(ot.total_overtime), 0) as avgHours,
            COALESCE(MAX(ot.total_overtime), 0) as maxHours
        FROM overtime_reports ot 
        WHERE EXTRACT(YEAR FROM ot.target_month) = :year 
        AND EXTRACT(MONTH FROM ot.target_month) = :month
        """)
    java.util.Map<String, Object> getMonthlyStatistics(@Param("year") int year, @Param("month") int month);

    /**
     * ユーザー別月間残業統計
     * @param year 年
     * @param month 月
     * @return ユーザー別月間残業統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            ot.user_id as userId,
            COUNT(ot) as reportCount,
            COALESCE(SUM(ot.total_overtime), 0) as totalHours,
            COALESCE(AVG(ot.total_overtime), 0) as avgHours
        FROM overtime_reports ot 
        WHERE EXTRACT(YEAR FROM ot.target_month) = :year 
        AND EXTRACT(MONTH FROM ot.target_month) = :month
        GROUP BY ot.user_id
        ORDER BY SUM(ot.total_overtime) DESC
        """)
    List<java.util.Map<String, Object>> getUserMonthlyStatistics(@Param("year") int year, @Param("month") int month);

    /**
     * 残業時間ランキング取得
     * @param year 年
     * @param month 月
     * @param limit 取得件数
     * @return 残業時間ランキング
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            ot.user_id as userId,
            COALESCE(SUM(ot.total_overtime), 0) as totalHours
        FROM overtime_reports ot 
        WHERE EXTRACT(YEAR FROM ot.target_month) = :year 
        AND EXTRACT(MONTH FROM ot.target_month) = :month
        GROUP BY ot.user_id
        ORDER BY SUM(ot.total_overtime) DESC
        LIMIT :limit
        """)
    List<java.util.Map<String, Object>> getOvertimeRanking(@Param("year") int year, @Param("month") int month, @Param("limit") int limit);

    /**
     * 長時間残業者検索
     * @param threshold 残業時間閾値
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 長時間残業者リスト
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            ot.user_id as userId,
            COALESCE(SUM(ot.total_overtime), 0) as totalHours,
            COUNT(ot) as reportCount
        FROM overtime_reports ot 
        WHERE ot.target_month BETWEEN :startDate AND :endDate
        GROUP BY ot.user_id
        HAVING SUM(ot.total_overtime) >= :threshold
        ORDER BY SUM(ot.total_overtime) DESC
        """)
    List<java.util.Map<String, Object>> findLongOvertimeUsers(@Param("threshold") java.math.BigDecimal threshold, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 連続残業日数検索
     * @param userId ユーザーID
     * @param minDays 最小連続日数
     * @return 連続残業期間リスト
     */
    @Query(nativeQuery = true, value = """
        SELECT ot.* FROM overtime_reports ot 
        WHERE ot.user_id = :userId 
        ORDER BY ot.target_month ASC
        """)
    List<OvertimeReport> findForConsecutiveAnalysis(@Param("userId") Integer userId);

    /**
     * 理由別残業統計
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 理由別残業統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            ot.reason as reason,
            COUNT(ot) as reportCount,
            COALESCE(SUM(ot.total_overtime), 0) as totalHours
        FROM overtime_reports ot 
        WHERE ot.target_month BETWEEN :startDate AND :endDate
        AND ot.reason IS NOT NULL
        GROUP BY ot.reason
        ORDER BY SUM(ot.total_overtime) DESC
        """)
    List<java.util.Map<String, Object>> getReasonStatistics(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 最近の残業報告取得
     * @param days 過去何日以内
     * @return 最近の残業報告リスト
     */
    @Query(nativeQuery = true, value = "SELECT ot.* FROM overtime_reports ot WHERE ot.target_month >= :cutoffDate ORDER BY ot.target_month DESC, ot.created_at DESC")
    List<OvertimeReport> findRecentReports(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 重複報告チェック
     * @param userId ユーザーID
     * @param date 日付
     * @param excludeId 除外するID（更新時）
     * @return 重複する報告があるかどうか
     */
    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(ot) > 0 THEN true ELSE false END FROM overtime_reports ot WHERE ot.user_id = :userId AND ot.target_month = :targetMonth AND (:excludeId IS NULL OR ot.id != :excludeId)")
    boolean existsByUserIdAndDateExcludingId(@Param("userId") Integer userId, @Param("targetMonth") LocalDate targetMonth, @Param("excludeId") Integer excludeId);

    /**
     * バッチ処理用：大量データ残業報告取得
     * @param offset オフセット
     * @param limit 取得件数
     * @return 残業報告リスト
     */
    @Query(nativeQuery = true, value = "SELECT ot.* FROM overtime_reports ot ORDER BY ot.id ASC LIMIT :limit OFFSET :offset")
    List<OvertimeReport> findForBatchProcessing(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * データ整合性チェック：孤立ユーザー参照検索
     * @return 存在しないユーザーを参照している報告リスト
     */
    @Query(nativeQuery = true, value = """
        SELECT ot.* FROM overtime_reports ot 
        WHERE NOT EXISTS (
            SELECT 1 FROM users u WHERE u.id = ot.user_id
        )
        """)
    List<OvertimeReport> findReportsWithOrphanedUserId();

    /**
     * 異常値検出：極端に長い残業時間の報告
     * @param threshold 異常値閾値（時間）
     * @return 異常値の可能性がある報告リスト
     */
    @Query(nativeQuery = true, value = "SELECT ot.* FROM overtime_reports ot WHERE ot.total_overtime > :threshold ORDER BY ot.total_overtime DESC")
    List<OvertimeReport> findAnomalousReports(@Param("threshold") java.math.BigDecimal threshold);
}