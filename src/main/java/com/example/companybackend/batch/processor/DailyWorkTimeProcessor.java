package com.example.companybackend.batch.processor;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.entity.Holiday;
import com.example.companybackend.repository.HolidayRepository;
import com.example.companybackend.repository.AttendanceRecordRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class DailyWorkTimeProcessor implements ItemProcessor<AttendanceRecord, AttendanceSummary> {
    
    @Autowired
    private HolidayRepository holidayRepository;
    
    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;
    
    // 标准工作时间（小时）
    private static final BigDecimal STANDARD_HOURS = new BigDecimal("8.00");
    
    // 深夜工作时间范围
    private static final LocalTime LATE_NIGHT_START = LocalTime.of(22, 0); // 22:00
    private static final LocalTime LATE_NIGHT_END = LocalTime.of(5, 0);    // 05:00
    
    // Setter方法用于依赖注入
    public void setHolidayRepository(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }
    
    public void setAttendanceRecordRepository(AttendanceRecordRepository attendanceRecordRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
    }
    
    @Override
    public AttendanceSummary process(AttendanceRecord attendanceRecord) throws Exception {
        // 只处理'in'类型的记录，避免重复计算
        if (!"in".equals(attendanceRecord.getType())) {
            return null;
        }
        
        Integer userId = attendanceRecord.getUserId();
        LocalDate targetDate = attendanceRecord.getTimestamp().toLocalDate();
        
        // 获取用户当天的所有考勤记录
        List<AttendanceRecord> dailyRecords = attendanceRecordRepository.findByUserIdAndDate(userId, targetDate);
        
        // 按时间排序
        dailyRecords.sort((r1, r2) -> r1.getTimestamp().compareTo(r2.getTimestamp()));
        
        // 如果没有配对的记录，则跳过处理
        if (dailyRecords.size() < 2) {
            return null;
        }
        
        // 计算工作时间
        WorkTimeCalculationResult result = calculateWorkTime(dailyRecords);
        
        // 创建考勤汇总
        AttendanceSummary summary = new AttendanceSummary();
        summary.setUserId(userId);
        summary.setTargetDate(targetDate);
        summary.setTotalHours(result.totalHours);
        summary.setOvertimeHours(result.overtimeHours);
        summary.setLateNightHours(result.lateNightHours);
        summary.setHolidayHours(result.holidayHours);
        summary.setSummaryType("daily");
        summary.setCreatedAt(java.time.OffsetDateTime.now());
        
        return summary;
    }
    
    /**
     * 根据用户的打卡记录计算工作时间
     * @param records 用户的打卡记录
     * @return 工作时间计算结果
     */
    private WorkTimeCalculationResult calculateWorkTime(List<AttendanceRecord> records) {
        WorkTimeCalculationResult result = new WorkTimeCalculationResult();
        
        BigDecimal totalMinutes = BigDecimal.ZERO;
        BigDecimal lateNightMinutes = BigDecimal.ZERO;
        
        // 配对'in'和'out'记录来计算工作时间
        for (int i = 0; i < records.size() - 1; i++) {
            AttendanceRecord inRecord = records.get(i);
            AttendanceRecord outRecord = records.get(i + 1);
            
            // 确保是配对的'in'和'out'记录
            if ("in".equals(inRecord.getType()) && "out".equals(outRecord.getType())) {
                // 计算工作分钟数
                long minutes = java.time.temporal.ChronoUnit.MINUTES.between(
                    inRecord.getTimestamp().toLocalDateTime(),
                    outRecord.getTimestamp().toLocalDateTime()
                );
                
                BigDecimal sessionMinutes = new BigDecimal(minutes);
                totalMinutes = totalMinutes.add(sessionMinutes);
                
                // 计算深夜工作时间
                lateNightMinutes = lateNightMinutes.add(calculateLateNightMinutes(
                    inRecord.getTimestamp().toLocalDateTime(),
                    outRecord.getTimestamp().toLocalDateTime()
                ));
                
                // 跳过下一个记录，因为我们已经处理了这个配对
                i++;
            }
        }
        
        // 将分钟转换为小时，保留2位小数
        result.totalHours = totalMinutes.divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
        
        // 计算加班时间（超过8小时的部分）
        result.overtimeHours = result.totalHours.subtract(STANDARD_HOURS);
        if (result.overtimeHours.compareTo(BigDecimal.ZERO) < 0) {
            result.overtimeHours = BigDecimal.ZERO;
        }
        
        // 深夜工作时间（小时）
        result.lateNightHours = lateNightMinutes.divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
        
        // 计算节假日工作时间
        if (!records.isEmpty()) {
            LocalDate workDate = records.get(0).getTimestamp().toLocalDate();
            result.holidayHours = calculateHolidayHours(workDate, result.totalHours);
        }
        
        return result;
    }
    
    /**
     * 计算深夜工作时间（分钟）
     * @param inTime 打卡进入时间
     * @param outTime 打卡离开时间
     * @return 深夜工作分钟数
     */
    private BigDecimal calculateLateNightMinutes(LocalDateTime inTime, LocalDateTime outTime) {
        BigDecimal lateNightMinutes = BigDecimal.ZERO;
        
        // 简化计算：按天分别处理深夜时间段
        LocalDate startDate = inTime.toLocalDate();
        LocalDate endDate = outTime.toLocalDate();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // 当天的工作时间范围
            LocalDateTime dayStart = date.equals(startDate) ? inTime : date.atTime(0, 0);
            LocalDateTime dayEnd = date.equals(endDate) ? outTime : date.atTime(23, 59, 59);
            
            // 计算当天深夜时间段1: 22:00-23:59
            LocalDateTime lateNightStart1 = date.atTime(LATE_NIGHT_START);
            LocalDateTime lateNightEnd1 = date.atTime(23, 59, 59);
            
            if (dayStart.isBefore(lateNightEnd1) && dayEnd.isAfter(lateNightStart1)) {
                LocalDateTime overlapStart = dayStart.isAfter(lateNightStart1) ? dayStart : lateNightStart1;
                LocalDateTime overlapEnd = dayEnd.isBefore(lateNightEnd1) ? dayEnd : lateNightEnd1;
                
                if (overlapStart.isBefore(overlapEnd)) {
                    long minutes = java.time.temporal.ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
                    lateNightMinutes = lateNightMinutes.add(new BigDecimal(minutes));
                }
            }
            
            // 计算当天深夜时间段2: 00:00-05:00
            LocalDateTime lateNightStart2 = date.atTime(0, 0);
            LocalDateTime lateNightEnd2 = date.atTime(LATE_NIGHT_END);
            
            if (dayStart.isBefore(lateNightEnd2) && dayEnd.isAfter(lateNightStart2)) {
                LocalDateTime overlapStart = dayStart.isAfter(lateNightStart2) ? dayStart : lateNightStart2;
                LocalDateTime overlapEnd = dayEnd.isBefore(lateNightEnd2) ? dayEnd : lateNightEnd2;
                
                if (overlapStart.isBefore(overlapEnd)) {
                    long minutes = java.time.temporal.ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
                    lateNightMinutes = lateNightMinutes.add(new BigDecimal(minutes));
                }
            }
        }
        
        return lateNightMinutes;
    }
    
    /**
     * 计算节假日工作时间
     * @param workDate 工作日期
     * @param totalHours 总工作小时数
     * @return 节假日工作小时数
     */
    private BigDecimal calculateHolidayHours(LocalDate workDate, BigDecimal totalHours) {
        // 检查是否为周末
        if (workDate.getDayOfWeek().getValue() > 5) { // 6=周六, 7=周日
            return totalHours;
        }
        
        // 检查是否为法定节假日
        List<Holiday> holidays = holidayRepository.findAll();
        for (Holiday holiday : holidays) {
            if (holiday.getDate().equals(workDate)) {
                return totalHours;
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * 工作时间计算结果类
     */
    private static class WorkTimeCalculationResult {
        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal overtimeHours = BigDecimal.ZERO;
        BigDecimal lateNightHours = BigDecimal.ZERO;
        BigDecimal holidayHours = BigDecimal.ZERO;
    }
}