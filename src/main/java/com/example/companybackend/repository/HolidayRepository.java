package com.example.companybackend.repository;

import com.example.companybackend.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 祝日リポジトリ
 * comsys_dump.sql holidaysテーブル完全対応
 * 
 * テーブル構造:
 * - id (SERIAL PRIMARY KEY)
 * - name (TEXT NOT NULL)
 * - date (DATE NOT NULL UNIQUE)
 * - created_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 * - updated_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 */
@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Integer> {

    /**
     * 日付による祝日検索
     * @param date 日付
     * @return 祝日情報
     */
    Optional<Holiday> findByDate(LocalDate date);

    /**
     * 祝日名による検索
     * @param name 祝日名
     * @return 祝日情報
     */
    Optional<Holiday> findByName(String name);

    /**
     * 日付による祝日存在確認
     * @param date 日付
     * @return 祝日かどうか
     */
    boolean existsByDate(LocalDate date);

    /**
     * 年別祝日取得
     * @param year 年
     * @return 指定年の祝日リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM holidays h WHERE EXTRACT(YEAR FROM h.date) = :year ORDER BY h.date ASC")
    List<Holiday> findByYear(@Param("year") int year);

    /**
     * 月別祝日取得
     * @param year 年
     * @param month 月
     * @return 指定年月の祝日リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM holidays h WHERE EXTRACT(YEAR FROM h.date) = :year AND EXTRACT(MONTH FROM h.date) = :month ORDER BY h.date ASC")
    List<Holiday> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    /**
     * 期間内祝日取得
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 指定期間内の祝日リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM holidays h WHERE h.date BETWEEN :startDate AND :endDate ORDER BY h.date ASC")
    List<Holiday> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 今年の祝日取得
     * @return 今年の祝日リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM holidays h WHERE EXTRACT(YEAR FROM h.date) = EXTRACT(YEAR FROM CURRENT_DATE) ORDER BY h.date ASC")
    List<Holiday> findCurrentYearHolidays();

    /**
     * 今月の祝日取得
     * @return 今月の祝日リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM holidays h WHERE EXTRACT(YEAR FROM h.date) = EXTRACT(YEAR FROM CURRENT_DATE) AND EXTRACT(MONTH FROM h.date) = EXTRACT(MONTH FROM CURRENT_DATE) ORDER BY h.date ASC")
    List<Holiday> findCurrentMonthHolidays();

    /**
     * 次の祝日取得
     * @return 今日以降の最初の祝日
     */
    @Query(nativeQuery = true, value = "SELECT * FROM holidays h WHERE h.date >= CURRENT_DATE ORDER BY h.date ASC LIMIT 1")
    Optional<Holiday> findNextHoliday();

    /**
     * 前の祝日取得
     * @return 今日以前の最後の祝日
     */
    @Query(nativeQuery = true, value = "SELECT * FROM holidays h WHERE h.date < CURRENT_DATE ORDER BY h.date DESC LIMIT 1")
    Optional<Holiday> findPreviousHoliday();

    /**
     * 祝日名部分一致検索
     * @param namePattern 祝日名パターン
     * @return 該当祝日リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM holidays h WHERE h.name LIKE CONCAT('%', :namePattern, '%') ORDER BY h.date ASC")
    List<Holiday> findByNameContaining(@Param("namePattern") String namePattern);

    /**
     * 年別祝日数取得
     * @param year 年
     * @return 指定年の祝日数
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM holidays h WHERE EXTRACT(YEAR FROM h.date) = :year")
    long countByYear(@Param("year") int year);

    /**
     * 月別祝日数取得
     * @param year 年
     * @param month 月
     * @return 指定年月の祝日数
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM holidays h WHERE EXTRACT(YEAR FROM h.date) = :year AND EXTRACT(MONTH FROM h.date) = :month")
    long countByYearAndMonth(@Param("year") int year, @Param("month") int month);

    /**
     * 期間内祝日数取得
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 指定期間内の祝日数
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM holidays h WHERE h.date BETWEEN :startDate AND :endDate")
    long countByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 年別祝日統計取得
     * @return 年別祝日数統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            EXTRACT(YEAR FROM date) as year,
            COUNT(*) as holidayCount
        FROM holidays
        GROUP BY EXTRACT(YEAR FROM date)
        ORDER BY EXTRACT(YEAR FROM date) DESC
        """)
    List<java.util.Map<String, Object>> getYearlyStatistics();

    /**
     * 月別祝日統計取得（指定年）
     * @param year 年
     * @return 月別祝日数統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            EXTRACT(MONTH FROM h.date) as month,
            COUNT(h) as holidayCount
        FROM holidays h 
        WHERE EXTRACT(YEAR FROM h.date) = :year
        GROUP BY EXTRACT(MONTH FROM h.date)
        ORDER BY EXTRACT(MONTH FROM h.date) ASC
        """)
    List<java.util.Map<String, Object>> getMonthlyStatistics(@Param("year") int year);

    /**
     * 連休検索
     * 連続する祝日（土日含む）を検索
     * @param year 年
     * @return 連休情報
     */
    @Query(nativeQuery = true, value = """
        SELECT h.* FROM holidays h 
        WHERE EXTRACT(YEAR FROM h.date) = :year
        AND EXISTS (
            SELECT 1 FROM holidays h2 
            WHERE h2.date = h.date + 1 
            OR (EXTRACT(DOW FROM h.date) = 5 AND h2.date = h.date + 3)
            OR (EXTRACT(DOW FROM h.date) = 6 AND h2.date = h.date + 2)
        )
        ORDER BY h.date ASC
        """)
    List<Holiday> findConsecutiveHolidays(@Param("year") int year);

    /**
     * 週末と祝日の重複チェック
     * @param year 年
     * @return 土日と重複する祝日リスト
     */
    @Query(nativeQuery = true, value = "SELECT h.* FROM holidays h WHERE EXTRACT(YEAR FROM h.date) = :year AND EXTRACT(DOW FROM h.date) IN (0, 6)")
    List<Holiday> findWeekendHolidays(@Param("year") int year);

    /**
     * 平日の祝日取得
     * @param year 年
     * @return 平日（月～金）の祝日リスト
     */
    @Query(nativeQuery = true, value = "SELECT h.* FROM holidays h WHERE EXTRACT(YEAR FROM h.date) = :year AND EXTRACT(DOW FROM h.date) BETWEEN 1 AND 5")
    List<Holiday> findWeekdayHolidays(@Param("year") int year);

    /**
     * 最近追加された祝日取得
     * @param days 過去何日以内
     * @return 最近追加された祝日リスト
     */
    @Query(nativeQuery = true, value = "SELECT h.* FROM holidays h WHERE h.created_at >= CURRENT_TIMESTAMP - INTERVAL ':days days' ORDER BY h.created_at DESC")
    List<Holiday> findRecentlyAdded(@Param("days") int days);

    /**
     * バッチ処理用：大量データ祝日取得
     * @param offset オフセット
     * @param limit 取得件数
     * @return 祝日リスト
     */
    @Query(nativeQuery = true, value = "SELECT h.* FROM holidays h ORDER BY h.date ASC LIMIT :limit OFFSET :offset")
    List<Holiday> findForBatchProcessing(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 重複日付チェック
     * @param date 日付
     * @param excludeId 除外するID（更新時）
     * @return 重複する祝日があるかどうか
     */
    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM holidays h WHERE h.date = :date AND (:excludeId IS NULL OR h.id != :excludeId)")
    boolean existsByDateExcludingId(@Param("date") LocalDate date, @Param("excludeId") Integer excludeId);
}