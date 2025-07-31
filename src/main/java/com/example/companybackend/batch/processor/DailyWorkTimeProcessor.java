package com.example.companybackend.batch.processor;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.entity.Holiday;
import com.example.companybackend.repository.HolidayRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class DailyWorkTimeProcessor implements ItemProcessor<AttendanceRecord, AttendanceSummary> {
    
    @Autowired
    private HolidayRepository holidayRepository;
    
    @Override
    public AttendanceSummary process(AttendanceRecord attendanceRecord) throws Exception {
        // Process attendance record and calculate work time
        AttendanceSummary summary = new AttendanceSummary();
        
        // Implementation would calculate work hours based on attendance records
        // This is a simplified implementation - in reality, we would need to match
        // clock-in and clock-out pairs for the same user on the same day
        
        // For F-301: Daily work time aggregation
        // Calculate total hours based on in/out pairs
        
        // For F-303: Overtime calculation
        // Calculate overtime hours (exceeding standard work hours per day)
        BigDecimal standardHours = new BigDecimal("8.00"); // Standard work hours per day
        BigDecimal totalHours = calculateWorkHours(attendanceRecord);
        BigDecimal overtimeHours = totalHours.subtract(standardHours);
        if (overtimeHours.compareTo(BigDecimal.ZERO) < 0) {
            overtimeHours = BigDecimal.ZERO;
        }
        
        // For F-304: Late night work time calculation
        // Calculate late night hours (22:00-05:00)
        BigDecimal lateNightHours = calculateLateNightHours(attendanceRecord);
        
        // For F-305: Holiday work time calculation
        // Check if the work date is a holiday
        BigDecimal holidayHours = calculateHolidayHours(attendanceRecord);
        
        summary.setUserId(attendanceRecord.getUserId());
        summary.setTargetDate(attendanceRecord.getTimestamp().toLocalDate());
        summary.setTotalHours(totalHours);
        summary.setOvertimeHours(overtimeHours);
        summary.setLateNightHours(lateNightHours);
        summary.setHolidayHours(holidayHours);
        summary.setSummaryType("daily");
        summary.setCreatedAt(java.time.OffsetDateTime.now());
        
        return summary;
    }
    
    private BigDecimal calculateWorkHours(AttendanceRecord record) {
        // Simplified calculation - in reality would need to match with corresponding out record
        // Assuming 8 hours work day for demonstration
        return new BigDecimal("8.00");
    }
    
    private BigDecimal calculateLateNightHours(AttendanceRecord record) {
        // Calculate hours worked between 22:00 and 05:00
        LocalDateTime timestamp = record.getTimestamp().toLocalDateTime();
        LocalTime time = timestamp.toLocalTime();
        
        // If worked between 22:00-24:00 or 00:00-05:00, count as late night
        if ((time.isAfter(LocalTime.of(21, 59)) && time.isBefore(LocalTime.of(23, 59))) || 
            (time.isAfter(LocalTime.of(0, 0)) && time.isBefore(LocalTime.of(5, 1)))) {
            // Simplified - assuming 1 hour of late night work for demonstration
            return new BigDecimal("1.00");
        }
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateHolidayHours(AttendanceRecord record) {
        LocalDate workDate = record.getTimestamp().toLocalDate();
        
        // Check if work date is weekend or holiday
        if (workDate.getDayOfWeek().getValue() > 5) { // Weekend
            // Assuming 8 hours work day for demonstration
            return new BigDecimal("8.00");
        }
        
        // Check if work date is a registered holiday
        List<Holiday> holidays = holidayRepository.findAll();
        for (Holiday holiday : holidays) {
            if (holiday.getDate().equals(workDate) && holiday.getIsRecurring()) {
                return new BigDecimal("8.00");
            }
        }
        
        return BigDecimal.ZERO;
    }
}