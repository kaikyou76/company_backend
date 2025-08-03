package com.example.companybackend.repository;

import com.example.companybackend.entity.AttendanceSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 勤怠サマリーリポジトリ
 * comsys_dump.sql attendance_summariesテーブル完全対応
 * 
 * テーブル構造:
 * - id (BIGINT PRIMARY KEY)
 * - user_id (INTEGER NOT NULL)
 * - target_date (DATE NOT NULL)
 * - total_hours (NUMERIC(5,2) DEFAULT 0.00 NOT NULL)
 * - overtime_hours (NUMERIC(5,2) DEFAULT 0.00 NOT NULL)
 * - late_night_hours (NUMERIC(5,2) DEFAULT 0.00 NOT NULL)
 * - holiday_hours (NUMERIC(5,2) DEFAULT 0.00 NOT NULL)
 * - summary_type (VARCHAR(20) DEFAULT 'daily' CHECK ('daily', 'monthly'))
 * - created_at (TIMESTAMP WITH TIME ZONE DEFAULT NOW())
 */
@Repository
public interface AttendanceSummaryRepository extends JpaRepository<AttendanceSummary, Long> {

        /**
         * ユーザーID別サマリー取得
         * 
         * @param userId ユーザーID
         * @return 該当ユーザーのサマリーリスト
         */
        List<AttendanceSummary> findByUserId(Integer userId);

        /**
         * サマリータイプ別取得
         * 
         * @param summaryType サマリータイプ
         * @return 該当タイプのサマリーリスト
         */
        List<AttendanceSummary> findBySummaryType(String summaryType);

        /**
         * 対象日別サマリー取得
         * 
         * @param targetDate 対象日
         * @return 該当日のサマリーリスト
         */
        List<AttendanceSummary> findByTargetDate(LocalDate targetDate);

        /**
         * ユーザー・対象日別サマリー取得
         * 
         * @param userId     ユーザーID
         * @param targetDate 対象日
         * @return 該当サマリー
         */
        Optional<AttendanceSummary> findByUserIdAndTargetDate(Integer userId, LocalDate targetDate);

        /**
         * ユーザー・サマリータイプ別取得
         * 
         * @param userId      ユーザーID
         * @param summaryType サマリータイプ
         * @return 該当サマリーリスト
         */
        List<AttendanceSummary> findByUserIdAndSummaryType(Integer userId, String summaryType);

        /**
         * 期間別サマリー取得
         * 
         * @param userId    ユーザーID
         * @param startDate 開始日
         * @param endDate   終了日
         * @return 指定期間内のサマリーリスト
         */
        @Query(nativeQuery = true, value = "SELECT attsum.* FROM attendance_summaries attsum WHERE attsum.user_id = :userId AND attsum.target_date BETWEEN :startDate AND :endDate ORDER BY attsum.target_date ASC")
        List<AttendanceSummary> findByUserIdAndTargetDateBetween(@Param("userId") Integer userId,
                        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * 期間別サマリー取得（ページング対応）
         * 
         * @param startDate 開始日
         * @param endDate   終了日
         * @param pageable  ページ情報
         * @return 指定期間内のサマリーのページ
         */
        @Query("SELECT a FROM AttendanceSummary a WHERE a.targetDate BETWEEN :startDate AND :endDate")
        Page<AttendanceSummary> findByTargetDateBetween(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate, Pageable pageable);

        /**
         * 期間別サマリー取得（ページングなし）
         * 
         * @param startDate 開始日
         * @param endDate   終了日
         * @return 指定期間内のサマリーのリスト
         */
        @Query("SELECT a FROM AttendanceSummary a WHERE a.targetDate BETWEEN :startDate AND :endDate ORDER BY a.targetDate ASC")
        List<AttendanceSummary> findByTargetDateBetween(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * ユーザー・サマリータイプ・期間別サマリー取得
         * 
         * @param userId      ユーザーID
         * @param summaryType サマリータイプ
         * @param startDate   開始日
         * @param endDate     終了日
         * @return 指定条件のサマリーリスト
         */
        @Query("SELECT a FROM AttendanceSummary a WHERE a.userId = :userId AND a.summaryType = :summaryType AND a.targetDate BETWEEN :startDate AND :endDate ORDER BY a.targetDate ASC")
        List<AttendanceSummary> findByUserIdAndSummaryTypeAndTargetDateBetween(@Param("userId") Integer userId,
                        @Param("summaryType") String summaryType, @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * サマリータイプ・期間別サマリー取得（残業監視用）
         * 
         * @param summaryType サマリータイプ
         * @param startDate   開始日
         * @param endDate     終了日
         * @return 指定条件のサマリーリスト
         */
        @Query("SELECT a FROM AttendanceSummary a WHERE a.summaryType = :summaryType AND a.targetDate BETWEEN :startDate AND :endDate ORDER BY a.userId ASC, a.targetDate ASC")
        List<AttendanceSummary> findBySummaryTypeAndTargetDateBetween(@Param("summaryType") String summaryType,
                        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}