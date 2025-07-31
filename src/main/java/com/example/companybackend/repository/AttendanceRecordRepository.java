package com.example.companybackend.repository;

import com.example.companybackend.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 勤怠記録リポジトリ
 * attendance_records テーブルに対応
 * comsys_dump.sql準拠
 */
@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    
    /**
     * ユーザーIDで勤怠記録を検索
     */
    List<AttendanceRecord> findByUserId(Integer userId);
    
    /**
     * ユーザーIDと日付で勤怠記録を検索
     */
    @Query(nativeQuery = true, value = "SELECT ar.* FROM attendance_records ar WHERE ar.user_id = :userId AND DATE(ar.timestamp) = :date")
    List<AttendanceRecord> findByUserIdAndDate(@Param("userId") Integer userId, @Param("date") LocalDate date);
    
    /**
     * ユーザーIDとタイプで勤怠記録を検索
     */
    List<AttendanceRecord> findByUserIdAndType(Integer userId, String type);
    
    /**
     * ユーザーIDと日付範囲で勤怠記録を検索
     */
    @Query(nativeQuery = true, value = "SELECT ar.* FROM attendance_records ar WHERE ar.user_id = :userId AND ar.timestamp BETWEEN :startDate AND :endDate")
    List<AttendanceRecord> findByUserIdAndDateRange(@Param("userId") Integer userId, 
                                                   @Param("startDate") OffsetDateTime startDate, 
                                                   @Param("endDate") OffsetDateTime endDate);
    
    /**
     * 今日の勤怠記録をユーザーIDで検索
     */
    @Query(nativeQuery = true, value = "SELECT ar.* FROM attendance_records ar WHERE ar.user_id = :userId AND DATE(ar.timestamp) = CURRENT_DATE")
    List<AttendanceRecord> findTodayRecordsByUserId(@Param("userId") Integer userId);
    
    /**
     * 最新の勤怠記録をユーザーIDで検索
     */
    @Query(nativeQuery = true, value = "SELECT ar.* FROM attendance_records ar WHERE ar.user_id = :userId ORDER BY ar.timestamp DESC")
    List<AttendanceRecord> findTopByUserIdOrderByTimestampDesc(@Param("userId") Integer userId);
    
    /**
     * 最近の勤怠記録をユーザーID、タイプ、時間で検索
     */
    @Query(nativeQuery = true, value = "SELECT ar.* FROM attendance_records ar WHERE ar.user_id = :userId AND ar.type = :type AND ar.timestamp >= :timestamp")
    List<AttendanceRecord> findRecentRecordsByUserIdAndType(@Param("userId") Integer userId, 
                                                           @Param("type") String type, 
                                                           @Param("timestamp") OffsetDateTime timestamp);
    
    /**
     * ユーザーIDと日付で勤怠記録を検索（開始時刻昇順）
     */
    @Query(nativeQuery = true, value = "SELECT ar.* FROM attendance_records ar WHERE ar.user_id = :userId AND DATE(ar.timestamp) = :date ORDER BY ar.timestamp ASC")
    List<AttendanceRecord> findByUserIdAndDateOrderByTimestampAsc(@Param("userId") Integer userId, 
                                                                @Param("date") LocalDate date);
    
    /**
     * 特定の日付範囲の勤怠記録を取得
     */
    @Query(nativeQuery = true, value = "SELECT ar.* FROM attendance_records ar WHERE ar.timestamp BETWEEN :startDate AND :endDate ORDER BY ar.user_id, ar.timestamp")
    List<AttendanceRecord> findByDateRange(@Param("startDate") OffsetDateTime startDate, 
                                          @Param("endDate") OffsetDateTime endDate);
    
    /**
     * ユーザーの出勤記録を取得
     */
    @Query(nativeQuery = true, value = "SELECT ar.* FROM attendance_records ar WHERE ar.user_id = :userId AND ar.type = 'in' ORDER BY ar.timestamp DESC")
    List<AttendanceRecord> findClockInRecordsByUserId(@Param("userId") Integer userId);
    
    /**
     * ユーザーの退勤記録を取得
     */
    @Query(nativeQuery = true, value = "SELECT ar.* FROM attendance_records ar WHERE ar.user_id = :userId AND ar.type = 'out' ORDER BY ar.timestamp DESC")
    List<AttendanceRecord> findClockOutRecordsByUserId(@Param("userId") Integer userId);
    
    /**
     * 未処理の勤怠記録を取得
     */
    @Query(nativeQuery = true, value = "SELECT ar.* FROM attendance_records ar WHERE ar.processed = false ORDER BY ar.timestamp ASC")
    List<AttendanceRecord> findUnprocessedRecords();
    
    /**
     * ユーザーの特定タイプの最新勤怠記録を取得
     */
    @Query(nativeQuery = true, value = "SELECT * FROM attendance_records ar WHERE ar.user_id = :userId AND ar.type = :type ORDER BY ar.timestamp DESC")
    List<AttendanceRecord> findLatestByUserIdAndType(@Param("userId") Integer userId, @Param("type") String type);
    
    /**
     * 特定日時の前後の勤怠記録を取得
     */
    @Query(nativeQuery = true, value = "SELECT * FROM attendance_records ar WHERE ar.user_id = :userId AND ar.timestamp BETWEEN :startTime AND :endTime")
    List<AttendanceRecord> findByUserIdAndTimeRange(@Param("userId") Integer userId,
                                                   @Param("startTime") OffsetDateTime startTime,
                                                   @Param("endTime") OffsetDateTime endTime);
                                                   
    /**
     * ユーザーIDと月で勤怠記録を検索
     */
    @Query(nativeQuery = true, value = "SELECT * FROM attendance_records ar WHERE ar.user_id = :userId AND EXTRACT(YEAR FROM ar.timestamp) = :year AND EXTRACT(MONTH FROM ar.timestamp) = :month")
    List<AttendanceRecord> findByUserIdAndYearAndMonth(@Param("userId") Integer userId, @Param("year") int year, @Param("month") int month);
    
    
    /**
     * 本日の打刻ユーザー数取得
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(DISTINCT ar.user_id) FROM attendance_records ar WHERE DATE(ar.timestamp) = CURRENT_DATE AND ar.type = 'in'")
    Long countTodayClockInUsers();
    
    /**
     * 本日の勤怠記録数取得
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM attendance_records ar WHERE DATE(ar.timestamp) = CURRENT_DATE")
    Long countTodayRecords();
    
    
    /**
     * 部署IDと日付で勤怠記録を検索
     */
    @Query(nativeQuery = true, value = "SELECT ar.* FROM attendance_records ar WHERE ar.user_id IN (SELECT u.id FROM users u WHERE u.department_id = :departmentId) AND DATE(ar.timestamp) = :date")
    List<AttendanceRecord> findByDepartmentAndDate(@Param("departmentId") Integer departmentId, @Param("date") LocalDate date);
}