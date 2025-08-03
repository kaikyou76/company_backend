package com.example.companybackend.batch.processor;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.repository.AttendanceSummaryRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class MonthlyWorkTimeProcessor implements ItemProcessor<AttendanceRecord, AttendanceSummary> {

    @Autowired
    private AttendanceSummaryRepository attendanceSummaryRepository;

    // Setter方法用于依赖注入
    public void setAttendanceSummaryRepository(AttendanceSummaryRepository attendanceSummaryRepository) {
        this.attendanceSummaryRepository = attendanceSummaryRepository;
    }

    @Override
    public AttendanceSummary process(AttendanceRecord attendanceRecord) throws Exception {
        // 只处理'in'类型的记录，避免重复计算
        if (!"in".equals(attendanceRecord.getType())) {
            return null;
        }

        Integer userId = attendanceRecord.getUserId();
        LocalDate recordDate = attendanceRecord.getTimestamp().toLocalDate();
        YearMonth targetMonth = YearMonth.from(recordDate);

        // 检查是否已经为该用户和月份创建了月次汇总
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        List<AttendanceSummary> existingMonthlySummaries = attendanceSummaryRepository
                .findByUserIdAndSummaryTypeAndTargetDateBetween(userId, "monthly", monthStart, monthEnd);

        // 如果已存在月次汇总，跳过处理
        if (!existingMonthlySummaries.isEmpty()) {
            return null;
        }

        // 获取该用户该月的所有日次汇总数据
        List<AttendanceSummary> dailySummaries = attendanceSummaryRepository
                .findByUserIdAndSummaryTypeAndTargetDateBetween(userId, "daily", monthStart, monthEnd);

        // 如果没有日次汇总数据，跳过处理
        if (dailySummaries.isEmpty()) {
            return null;
        }

        // 计算月次汇总
        MonthlyWorkTimeResult result = calculateMonthlyWorkTime(dailySummaries);

        // 创建月次考勤汇总
        AttendanceSummary monthlySummary = new AttendanceSummary();
        monthlySummary.setUserId(userId);
        monthlySummary.setTargetDate(monthStart); // 使用月初日期作为目标日期
        monthlySummary.setTotalHours(result.totalHours);
        monthlySummary.setOvertimeHours(result.overtimeHours);
        monthlySummary.setLateNightHours(result.lateNightHours);
        monthlySummary.setHolidayHours(result.holidayHours);
        monthlySummary.setSummaryType("monthly");
        monthlySummary.setCreatedAt(java.time.OffsetDateTime.now());

        return monthlySummary;
    }

    /**
     * 根据日次汇总数据计算月次工作时间
     * 
     * @param dailySummaries 日次汇总数据列表
     * @return 月次工作时间计算结果
     */
    private MonthlyWorkTimeResult calculateMonthlyWorkTime(List<AttendanceSummary> dailySummaries) {
        MonthlyWorkTimeResult result = new MonthlyWorkTimeResult();

        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal overtimeHours = BigDecimal.ZERO;
        BigDecimal lateNightHours = BigDecimal.ZERO;
        BigDecimal holidayHours = BigDecimal.ZERO;

        // 汇总所有日次数据
        for (AttendanceSummary dailySummary : dailySummaries) {
            totalHours = totalHours
                    .add(dailySummary.getTotalHours() != null ? dailySummary.getTotalHours() : BigDecimal.ZERO);
            overtimeHours = overtimeHours
                    .add(dailySummary.getOvertimeHours() != null ? dailySummary.getOvertimeHours() : BigDecimal.ZERO);
            lateNightHours = lateNightHours
                    .add(dailySummary.getLateNightHours() != null ? dailySummary.getLateNightHours() : BigDecimal.ZERO);
            holidayHours = holidayHours
                    .add(dailySummary.getHolidayHours() != null ? dailySummary.getHolidayHours() : BigDecimal.ZERO);
        }

        // 设置计算结果，保留2位小数
        result.totalHours = totalHours.setScale(2, RoundingMode.HALF_UP);
        result.overtimeHours = overtimeHours.setScale(2, RoundingMode.HALF_UP);
        result.lateNightHours = lateNightHours.setScale(2, RoundingMode.HALF_UP);
        result.holidayHours = holidayHours.setScale(2, RoundingMode.HALF_UP);

        return result;
    }

    /**
     * 月次工作时间计算结果类
     */
    private static class MonthlyWorkTimeResult {
        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal overtimeHours = BigDecimal.ZERO;
        BigDecimal lateNightHours = BigDecimal.ZERO;
        BigDecimal holidayHours = BigDecimal.ZERO;
    }
}