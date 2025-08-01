package com.example.companybackend.service.impl;

import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.entity.User;
import com.example.companybackend.repository.AttendanceSummaryRepository;
import com.example.companybackend.repository.AttendanceRecordRepository;
import com.example.companybackend.repository.HolidayRepository;
import com.example.companybackend.repository.UserRepository;
import com.example.companybackend.service.AttendanceSummaryService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.PrintWriter;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceSummaryServiceImpl implements AttendanceSummaryService {

    private final AttendanceSummaryRepository attendanceSummaryRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final HolidayRepository holidayRepository;
    private final UserRepository userRepository;

    @Override
    public Page<AttendanceSummary> getDailySummaries(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return attendanceSummaryRepository.findByTargetDateBetween(startDate, endDate, pageable);
    }

    @Override
    public Page<AttendanceSummary> getMonthlySummaries(YearMonth yearMonth, Pageable pageable) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        return attendanceSummaryRepository.findByTargetDateBetween(startDate, endDate, pageable);
    }

    @Override
    public AttendanceSummary getSummaryByDate(LocalDate date) {
        List<AttendanceSummary> summaries = attendanceSummaryRepository.findByTargetDate(date);
        return summaries.isEmpty() ? null : summaries.get(0);
    }

    @Override
    @Transactional
    public AttendanceSummary generateDailySummary(LocalDate date) {
        // 実際の実装では、AttendanceRecordから集計データを生成するロジックが必要
        // ここではダミーの実装を提供
        AttendanceSummary summary = new AttendanceSummary();
        summary.setTargetDate(date);
        summary.setTotalHours(java.math.BigDecimal.ZERO);
        summary.setOvertimeHours(java.math.BigDecimal.ZERO);
        summary.setLateNightHours(java.math.BigDecimal.ZERO);
        summary.setHolidayHours(java.math.BigDecimal.ZERO);
        summary.setSummaryType("daily");
        summary.setCreatedAt(java.time.OffsetDateTime.now());
        return attendanceSummaryRepository.save(summary);
    }

    @Override
    public Map<String, Object> getSummaryStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> statistics = new HashMap<>();
        List<AttendanceSummary> summaries = attendanceSummaryRepository.findByTargetDateBetween(startDate, endDate);
        
        double totalHours = summaries.stream()
                .mapToDouble(summary -> summary.getTotalHours() != null ? summary.getTotalHours().doubleValue() : 0.0)
                .sum();
                
        double overtimeHours = summaries.stream()
                .mapToDouble(summary -> summary.getOvertimeHours() != null ? summary.getOvertimeHours().doubleValue() : 0.0)
                .sum();
                
        statistics.put("totalRecords", summaries.size());
        statistics.put("totalHours", totalHours);
        statistics.put("overtimeHours", overtimeHours);
        
        return statistics;
    }

    @Override
    public List<AttendanceSummary> getSummariesForExport(LocalDate startDate, LocalDate endDate) {
        // ページングなしで全件取得
        return attendanceSummaryRepository.findByTargetDateBetween(startDate, endDate);
    }

    @Override
    public void exportSummariesToCSV(List<AttendanceSummary> summaries, PrintWriter writer) throws IOException {
        // CSV header
        writer.println("Date,Total Hours,Overtime Hours,Late Night Hours,Holiday Hours");
        
        // CSV data
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (AttendanceSummary summary : summaries) {
            writer.printf("%s,%.2f,%.2f,%.2f,%.2f%n",
                    summary.getTargetDate().format(formatter),
                    summary.getTotalHours() != null ? summary.getTotalHours().doubleValue() : 0.0,
                    summary.getOvertimeHours() != null ? summary.getOvertimeHours().doubleValue() : 0.0,
                    summary.getLateNightHours() != null ? summary.getLateNightHours().doubleValue() : 0.0,
                    summary.getHolidayHours() != null ? summary.getHolidayHours().doubleValue() : 0.0);
        }
    }

    @Override
    public void exportSummariesToJSON(List<AttendanceSummary> summaries, PrintWriter writer) throws IOException {
        writer.println("[");
        for (int i = 0; i < summaries.size(); i++) {
            AttendanceSummary summary = summaries.get(i);
            writer.printf("  {%n");
            writer.printf("    \"date\": \"%s\",%n", summary.getTargetDate().toString());
            writer.printf("    \"totalHours\": %.2f,%n", summary.getTotalHours() != null ? summary.getTotalHours().doubleValue() : 0.0);
            writer.printf("    \"overtimeHours\": %.2f,%n", summary.getOvertimeHours() != null ? summary.getOvertimeHours().doubleValue() : 0.0);
            writer.printf("    \"lateNightHours\": %.2f,%n", summary.getLateNightHours() != null ? summary.getLateNightHours().doubleValue() : 0.0);
            writer.printf("    \"holidayHours\": %.2f%n", summary.getHolidayHours() != null ? summary.getHolidayHours().doubleValue() : 0.0);
            writer.printf("  }%s%n", (i < summaries.size() - 1) ? "," : "");
        }
        writer.println("]");
    }

    @Override
    public Map<String, Object> getMonthlyStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> statistics = new HashMap<>();
        List<AttendanceSummary> summaries = attendanceSummaryRepository.findByTargetDateBetween(startDate, endDate);
        
        Map<String, Double> monthlyStats = summaries.stream()
                .collect(Collectors.groupingBy(
                        summary -> summary.getTargetDate().withDayOfMonth(1).toString(),
                        Collectors.summingDouble(s -> s.getTotalHours() != null ? s.getTotalHours().doubleValue() : 0.0)
                ));
                
        statistics.put("monthlyHours", monthlyStats);
        statistics.put("averageDailyHours", summaries.stream()
                .mapToDouble(s -> s.getTotalHours() != null ? s.getTotalHours().doubleValue() : 0.0)
                .average()
                .orElse(0.0));
                
        return statistics;
    }
    
    @Override
    public Map<String, Object> getPersonalAttendanceStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> statistics = new HashMap<>();
        
        // ユーザーの勤怠サマリーを取得
        List<AttendanceSummary> summaries = attendanceSummaryRepository.findByUserIdAndTargetDateBetween(
                userId.intValue(), startDate, endDate);
        
        // 統計情報を計算
        double totalHours = summaries.stream()
                .mapToDouble(summary -> summary.getTotalHours() != null ? summary.getTotalHours().doubleValue() : 0.0)
                .sum();
                
        double overtimeHours = summaries.stream()
                .mapToDouble(summary -> summary.getOvertimeHours() != null ? summary.getOvertimeHours().doubleValue() : 0.0)
                .sum();
                
        double lateNightHours = summaries.stream()
                .mapToDouble(summary -> summary.getLateNightHours() != null ? summary.getLateNightHours().doubleValue() : 0.0)
                .sum();
                
        double holidayHours = summaries.stream()
                .mapToDouble(summary -> summary.getHolidayHours() != null ? summary.getHolidayHours().doubleValue() : 0.0)
                .sum();
        
        statistics.put("userId", userId);
        statistics.put("totalRecords", summaries.size());
        statistics.put("totalHours", totalHours);
        statistics.put("overtimeHours", overtimeHours);
        statistics.put("lateNightHours", lateNightHours);
        statistics.put("holidayHours", holidayHours);
        statistics.put("startDate", startDate);
        statistics.put("endDate", endDate);
        
        return statistics;
    }
    
    @Override
    public Map<String, Object> getDepartmentAttendanceStatistics(Integer departmentId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 部門に所属するユーザーを取得
        List<User> departmentUsers = userRepository.findByDepartmentId(departmentId);
        List<Integer> userIds = departmentUsers.stream()
                .map(user -> Math.toIntExact(user.getId()))
                .collect(Collectors.toList());
        
        // 部門ユーザーの勤怠サマリーを取得
        List<AttendanceSummary> departmentSummaries = userIds.stream()
                .flatMap(userId -> attendanceSummaryRepository.findByUserIdAndTargetDateBetween(userId, startDate, endDate).stream())
                .collect(Collectors.toList());
        
        // 統計情報を計算
        double totalHours = departmentSummaries.stream()
                .mapToDouble(summary -> summary.getTotalHours() != null ? summary.getTotalHours().doubleValue() : 0.0)
                .sum();
                
        double overtimeHours = departmentSummaries.stream()
                .mapToDouble(summary -> summary.getOvertimeHours() != null ? summary.getOvertimeHours().doubleValue() : 0.0)
                .sum();
                
        double lateNightHours = departmentSummaries.stream()
                .mapToDouble(summary -> summary.getLateNightHours() != null ? summary.getLateNightHours().doubleValue() : 0.0)
                .sum();
                
        double holidayHours = departmentSummaries.stream()
                .mapToDouble(summary -> summary.getHolidayHours() != null ? summary.getHolidayHours().doubleValue() : 0.0)
                .sum();
        
        statistics.put("departmentId", departmentId);
        statistics.put("userCount", departmentUsers.size());
        statistics.put("totalRecords", departmentSummaries.size());
        statistics.put("totalHours", totalHours);
        statistics.put("averageHoursPerUser", departmentUsers.size() > 0 ? totalHours / departmentUsers.size() : 0);
        statistics.put("overtimeHours", overtimeHours);
        statistics.put("lateNightHours", lateNightHours);
        statistics.put("holidayHours", holidayHours);
        statistics.put("startDate", startDate);
        statistics.put("endDate", endDate);
        
        return statistics;
    }
}