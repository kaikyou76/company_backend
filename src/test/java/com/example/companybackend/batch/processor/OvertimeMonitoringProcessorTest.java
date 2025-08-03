package com.example.companybackend.batch.processor;

import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.entity.OvertimeReport;
import com.example.companybackend.repository.OvertimeReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OvertimeMonitoringProcessorTest {

    @Mock
    private OvertimeReportRepository overtimeReportRepository;

    private OvertimeMonitoringProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new OvertimeMonitoringProcessor();
        processor.setOvertimeReportRepository(overtimeReportRepository);
    }

    @Test
    void testProcess_WithMonthlySummary_ShouldCreateOvertimeReport() throws Exception {
        // Given
        AttendanceSummary monthlySummary = new AttendanceSummary();
        monthlySummary.setUserId(1);
        monthlySummary.setSummaryType("monthly");
        monthlySummary.setTargetDate(LocalDate.of(2025, 2, 1));
        monthlySummary.setOvertimeHours(new BigDecimal("30.00"));
        monthlySummary.setLateNightHours(new BigDecimal("10.00"));
        monthlySummary.setHolidayHours(new BigDecimal("5.00"));

        // Mock: 既存の残業レポートなし
        when(overtimeReportRepository.findByUserIdAndTargetMonth(eq(1), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        OvertimeReport result = processor.process(monthlySummary);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals(LocalDate.of(2025, 2, 1), result.getTargetMonth());
        assertEquals(new BigDecimal("30.00"), result.getTotalOvertime());
        assertEquals(new BigDecimal("10.00"), result.getTotalLateNight());
        assertEquals(new BigDecimal("5.00"), result.getTotalHoliday());
        assertEquals("draft", result.getStatus()); // 閾値以下なのでdraft
    }

    @Test
    void testProcess_WithHighOvertimeHours_ShouldSetConfirmedStatus() throws Exception {
        // Given
        AttendanceSummary monthlySummary = new AttendanceSummary();
        monthlySummary.setUserId(1);
        monthlySummary.setSummaryType("monthly");
        monthlySummary.setTargetDate(LocalDate.of(2025, 2, 1));
        monthlySummary.setOvertimeHours(new BigDecimal("50.00")); // 閾値45時間を超過
        monthlySummary.setLateNightHours(new BigDecimal("15.00"));
        monthlySummary.setHolidayHours(new BigDecimal("8.00"));

        // Mock: 既存の残業レポートなし
        when(overtimeReportRepository.findByUserIdAndTargetMonth(eq(1), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        OvertimeReport result = processor.process(monthlySummary);

        // Then
        assertNotNull(result);
        assertEquals("confirmed", result.getStatus()); // 閾値超過なのでconfirmed
        assertEquals(new BigDecimal("50.00"), result.getTotalOvertime());
    }

    @Test
    void testProcess_WithHighLateNightHours_ShouldSetConfirmedStatus() throws Exception {
        // Given
        AttendanceSummary monthlySummary = new AttendanceSummary();
        monthlySummary.setUserId(1);
        monthlySummary.setSummaryType("monthly");
        monthlySummary.setTargetDate(LocalDate.of(2025, 2, 1));
        monthlySummary.setOvertimeHours(new BigDecimal("20.00"));
        monthlySummary.setLateNightHours(new BigDecimal("25.00")); // 閾値20時間を超過
        monthlySummary.setHolidayHours(new BigDecimal("5.00"));

        // Mock: 既存の残業レポートなし
        when(overtimeReportRepository.findByUserIdAndTargetMonth(eq(1), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        OvertimeReport result = processor.process(monthlySummary);

        // Then
        assertNotNull(result);
        assertEquals("confirmed", result.getStatus()); // 深夜労働時間閾値超過なのでconfirmed
        assertEquals(new BigDecimal("25.00"), result.getTotalLateNight());
    }

    @Test
    void testProcess_WithNoOvertimeHours_ShouldSetApprovedStatus() throws Exception {
        // Given
        AttendanceSummary monthlySummary = new AttendanceSummary();
        monthlySummary.setUserId(1);
        monthlySummary.setSummaryType("monthly");
        monthlySummary.setTargetDate(LocalDate.of(2025, 2, 1));
        monthlySummary.setOvertimeHours(BigDecimal.ZERO);
        monthlySummary.setLateNightHours(BigDecimal.ZERO);
        monthlySummary.setHolidayHours(BigDecimal.ZERO);

        // Mock: 既存の残業レポートなし
        when(overtimeReportRepository.findByUserIdAndTargetMonth(eq(1), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        OvertimeReport result = processor.process(monthlySummary);

        // Then
        assertNotNull(result);
        assertEquals("approved", result.getStatus()); // 残業時間なしなのでapproved
        assertEquals(BigDecimal.ZERO, result.getTotalOvertime());
    }

    @Test
    void testProcess_WithExistingReport_ShouldUpdateExistingReport() throws Exception {
        // Given
        AttendanceSummary monthlySummary = new AttendanceSummary();
        monthlySummary.setUserId(1);
        monthlySummary.setSummaryType("monthly");
        monthlySummary.setTargetDate(LocalDate.of(2025, 2, 1));
        monthlySummary.setOvertimeHours(new BigDecimal("35.00"));
        monthlySummary.setLateNightHours(new BigDecimal("12.00"));
        monthlySummary.setHolidayHours(new BigDecimal("6.00"));

        // Mock: 既存の残業レポートあり
        OvertimeReport existingReport = new OvertimeReport();
        existingReport.setId(1L);
        existingReport.setUserId(1);
        existingReport.setTargetMonth(LocalDate.of(2025, 2, 1));
        existingReport.setCreatedAt(OffsetDateTime.now().minusDays(1));

        when(overtimeReportRepository.findByUserIdAndTargetMonth(eq(1), any(LocalDate.class)))
                .thenReturn(Arrays.asList(existingReport));

        // When
        OvertimeReport result = processor.process(monthlySummary);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId()); // 既存レポートのIDが保持される
        assertEquals(new BigDecimal("35.00"), result.getTotalOvertime());
        assertEquals(new BigDecimal("12.00"), result.getTotalLateNight());
        assertEquals(new BigDecimal("6.00"), result.getTotalHoliday());
        assertEquals("draft", result.getStatus());
        assertNotNull(result.getUpdatedAt()); // 更新日時が設定される
    }

    @Test
    void testProcess_WithDailySummary_ShouldReturnNull() throws Exception {
        // Given
        AttendanceSummary dailySummary = new AttendanceSummary();
        dailySummary.setUserId(1);
        dailySummary.setSummaryType("daily"); // 日次サマリー
        dailySummary.setTargetDate(LocalDate.of(2025, 2, 1));

        // When
        OvertimeReport result = processor.process(dailySummary);

        // Then
        assertNull(result); // 日次サマリーは処理対象外
    }

    @Test
    void testProcess_WithNullValues_ShouldHandleGracefully() throws Exception {
        // Given
        AttendanceSummary monthlySummary = new AttendanceSummary();
        monthlySummary.setUserId(1);
        monthlySummary.setSummaryType("monthly");
        monthlySummary.setTargetDate(LocalDate.of(2025, 2, 1));
        monthlySummary.setOvertimeHours(null);
        monthlySummary.setLateNightHours(null);
        monthlySummary.setHolidayHours(null);

        // Mock: 既存の残業レポートなし
        when(overtimeReportRepository.findByUserIdAndTargetMonth(eq(1), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        OvertimeReport result = processor.process(monthlySummary);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalOvertime());
        assertEquals(BigDecimal.ZERO, result.getTotalLateNight());
        assertEquals(BigDecimal.ZERO, result.getTotalHoliday());
        assertEquals("approved", result.getStatus()); // null値は0として扱われるのでapproved
    }

    @Test
    void testProcess_WithHighHolidayHours_ShouldSetConfirmedStatus() throws Exception {
        // Given
        AttendanceSummary monthlySummary = new AttendanceSummary();
        monthlySummary.setUserId(1);
        monthlySummary.setSummaryType("monthly");
        monthlySummary.setTargetDate(LocalDate.of(2025, 2, 1));
        monthlySummary.setOvertimeHours(new BigDecimal("20.00"));
        monthlySummary.setLateNightHours(new BigDecimal("10.00"));
        monthlySummary.setHolidayHours(new BigDecimal("20.00")); // 閾値15時間を超過

        // Mock: 既存の残業レポートなし
        when(overtimeReportRepository.findByUserIdAndTargetMonth(eq(1), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        OvertimeReport result = processor.process(monthlySummary);

        // Then
        assertNotNull(result);
        assertEquals("confirmed", result.getStatus()); // 休日労働時間閾値超過なのでconfirmed
        assertEquals(new BigDecimal("20.00"), result.getTotalHoliday());
    }
}