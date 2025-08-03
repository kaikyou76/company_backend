package com.example.companybackend.batch.processor;

import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.entity.OvertimeReport;
import com.example.companybackend.repository.OvertimeReportRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 残業監視バッチプロセッサー
 * 月次集計データから残業レポートを生成し、残業時間の監視を行う
 */
public class OvertimeMonitoringProcessor implements ItemProcessor<AttendanceSummary, OvertimeReport> {

    @Autowired
    private OvertimeReportRepository overtimeReportRepository;

    // 残業時間監視の閾値（時間）
    private static final BigDecimal OVERTIME_THRESHOLD = new BigDecimal("45.00"); // 月45時間
    private static final BigDecimal LATE_NIGHT_THRESHOLD = new BigDecimal("20.00"); // 月20時間
    private static final BigDecimal HOLIDAY_THRESHOLD = new BigDecimal("15.00"); // 月15時間

    // Setter方法用于依赖注入
    public void setOvertimeReportRepository(OvertimeReportRepository overtimeReportRepository) {
        this.overtimeReportRepository = overtimeReportRepository;
    }

    @Override
    public OvertimeReport process(AttendanceSummary attendanceSummary) throws Exception {
        // 月次集計データのみ処理
        if (!"monthly".equals(attendanceSummary.getSummaryType())) {
            return null;
        }

        Integer userId = attendanceSummary.getUserId();
        LocalDate targetDate = attendanceSummary.getTargetDate();
        YearMonth targetMonth = YearMonth.from(targetDate);
        LocalDate monthStart = targetMonth.atDay(1);

        // 既存の残業レポートをチェック
        List<OvertimeReport> existingReports = overtimeReportRepository
                .findByUserIdAndTargetMonth(userId, monthStart);

        // 既存のレポートがある場合は更新、ない場合は新規作成
        OvertimeReport overtimeReport;
        if (!existingReports.isEmpty()) {
            overtimeReport = existingReports.get(0); // 最初のレポートを更新
            overtimeReport.setUpdatedAt(java.time.OffsetDateTime.now());
        } else {
            overtimeReport = new OvertimeReport();
            overtimeReport.setUserId(userId);
            overtimeReport.setTargetMonth(monthStart);
            overtimeReport.setCreatedAt(java.time.OffsetDateTime.now());
            overtimeReport.setUpdatedAt(java.time.OffsetDateTime.now());
        }

        // 残業データの設定
        BigDecimal totalOvertime = attendanceSummary.getOvertimeHours() != null ? attendanceSummary.getOvertimeHours()
                : BigDecimal.ZERO;
        BigDecimal totalLateNight = attendanceSummary.getLateNightHours() != null
                ? attendanceSummary.getLateNightHours()
                : BigDecimal.ZERO;
        BigDecimal totalHoliday = attendanceSummary.getHolidayHours() != null ? attendanceSummary.getHolidayHours()
                : BigDecimal.ZERO;

        overtimeReport.setTotalOvertime(totalOvertime);
        overtimeReport.setTotalLateNight(totalLateNight);
        overtimeReport.setTotalHoliday(totalHoliday);

        // ステータスの決定（監視ロジック）
        String status = determineOvertimeStatus(totalOvertime, totalLateNight, totalHoliday);
        overtimeReport.setStatus(status);

        return overtimeReport;
    }

    /**
     * 残業時間に基づいてステータスを決定
     * 
     * @param totalOvertime  総残業時間
     * @param totalLateNight 総深夜労働時間
     * @param totalHoliday   総休日労働時間
     * @return ステータス（draft/confirmed/approved）
     */
    private String determineOvertimeStatus(BigDecimal totalOvertime, BigDecimal totalLateNight,
            BigDecimal totalHoliday) {
        // 閾値を超える場合は要確認ステータス
        if (totalOvertime.compareTo(OVERTIME_THRESHOLD) > 0 ||
                totalLateNight.compareTo(LATE_NIGHT_THRESHOLD) > 0 ||
                totalHoliday.compareTo(HOLIDAY_THRESHOLD) > 0) {
            return "confirmed"; // 要確認
        }

        // 残業時間が存在する場合はドラフト
        if (totalOvertime.compareTo(BigDecimal.ZERO) > 0 ||
                totalLateNight.compareTo(BigDecimal.ZERO) > 0 ||
                totalHoliday.compareTo(BigDecimal.ZERO) > 0) {
            return "draft"; // ドラフト
        }

        // 残業時間がない場合は承認済み
        return "approved"; // 承認済み
    }

    /**
     * 残業監視の閾値を取得
     * 
     * @return 監視閾値情報
     */
    public static class OvertimeThresholds {
        public static final BigDecimal OVERTIME_THRESHOLD = new BigDecimal("45.00");
        public static final BigDecimal LATE_NIGHT_THRESHOLD = new BigDecimal("20.00");
        public static final BigDecimal HOLIDAY_THRESHOLD = new BigDecimal("15.00");
    }
}