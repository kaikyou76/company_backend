package com.example.companybackend.service;

import com.example.companybackend.entity.AttendanceSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.io.PrintWriter;
import java.io.IOException;

public interface AttendanceSummaryService {
    
    /**
     * 日別サマリー取得
     * @param startDate 開始日
     * @param endDate 終了日
     * @param pageable ページ情報
     * @return 日別サマリーのページ
     */
    Page<AttendanceSummary> getDailySummaries(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    /**
     * 月別サマリー取得
     * @param yearMonth 年月
     * @param pageable ページ情報
     * @return 月別サマリーのページ
     */
    Page<AttendanceSummary> getMonthlySummaries(YearMonth yearMonth, Pageable pageable);
    
    /**
     * 日付指定サマリー取得
     * @param date 対象日
     * @return サマリー
     */
    AttendanceSummary getSummaryByDate(LocalDate date);
    
    /**
     * 日別サマリー生成
     * @param date 対象日
     * @return 生成されたサマリー
     */
    AttendanceSummary generateDailySummary(LocalDate date);
    
    /**
     * サマリー統計情報取得
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 統計情報
     */
    Map<String, Object> getSummaryStatistics(LocalDate startDate, LocalDate endDate);
    
    /**
     * エクスポート用サマリー取得
     * @param startDate 開始日
     * @param endDate 終了日
     * @return サマリーのリスト
     */
    List<AttendanceSummary> getSummariesForExport(LocalDate startDate, LocalDate endDate);
    
    /**
     * CSV形式でサマリーをエクスポート
     * @param summaries サマリーのリスト
     * @param writer 出力先Writer
     * @throws IOException IO例外
     */
    void exportSummariesToCSV(List<AttendanceSummary> summaries, PrintWriter writer) throws IOException;
    
    /**
     * JSON形式でサマリーをエクスポート
     * @param summaries サマリーのリスト
     * @param writer 出力先Writer
     * @throws IOException IO例外
     */
    void exportSummariesToJSON(List<AttendanceSummary> summaries, PrintWriter writer) throws IOException;
    
    /**
     * 月別統計情報取得
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 月別統計情報
     */
    Map<String, Object> getMonthlyStatistics(LocalDate startDate, LocalDate endDate);
}