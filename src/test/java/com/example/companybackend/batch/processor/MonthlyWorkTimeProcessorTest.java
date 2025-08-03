package com.example.companybackend.batch.processor;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.repository.AttendanceSummaryRepository;
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
class MonthlyWorkTimeProcessorTest {

    @Mock
    private AttendanceSummaryRepository attendanceSummaryRepository;

    private MonthlyWorkTimeProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new MonthlyWorkTimeProcessor();
        processor.setAttendanceSummaryRepository(attendanceSummaryRepository);
    }

    @Test
    void testProcess_WithValidInRecord_ShouldCreateMonthlySummary() throws Exception {
        // Given
        AttendanceRecord inRecord = new AttendanceRecord();
        inRecord.setUserId(1);
        inRecord.setType("in");
        inRecord.setTimestamp(OffsetDateTime.now());

        // Mock: 既存の月次サマリーなし
        when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
                eq(1), eq("monthly"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Mock: 日次サマリーデータあり
        AttendanceSummary dailySummary1 = new AttendanceSummary();
        dailySummary1.setUserId(1);
        dailySummary1.setTargetDate(LocalDate.now().minusDays(1));
        dailySummary1.setTotalHours(new BigDecimal("8.00"));
        dailySummary1.setOvertimeHours(new BigDecimal("1.00"));
        dailySummary1.setLateNightHours(new BigDecimal("0.50"));
        dailySummary1.setHolidayHours(new BigDecimal("0.00"));
        dailySummary1.setSummaryType("daily");

        AttendanceSummary dailySummary2 = new AttendanceSummary();
        dailySummary2.setUserId(1);
        dailySummary2.setTargetDate(LocalDate.now());
        dailySummary2.setTotalHours(new BigDecimal("7.50"));
        dailySummary2.setOvertimeHours(new BigDecimal("0.00"));
        dailySummary2.setLateNightHours(new BigDecimal("0.00"));
        dailySummary2.setHolidayHours(new BigDecimal("0.00"));
        dailySummary2.setSummaryType("daily");

        List<AttendanceSummary> dailySummaries = Arrays.asList(dailySummary1, dailySummary2);

        when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
                eq(1), eq("daily"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(dailySummaries);

        // When
        AttendanceSummary result = processor.process(inRecord);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals("monthly", result.getSummaryType());
        assertEquals(new BigDecimal("15.50"), result.getTotalHours());
        assertEquals(new BigDecimal("1.00"), result.getOvertimeHours());
        assertEquals(new BigDecimal("0.50"), result.getLateNightHours());
        assertEquals(new BigDecimal("0.00"), result.getHolidayHours());
    }

    @Test
    void testProcess_WithOutRecord_ShouldReturnNull() throws Exception {
        // Given
        AttendanceRecord outRecord = new AttendanceRecord();
        outRecord.setUserId(1);
        outRecord.setType("out");
        outRecord.setTimestamp(OffsetDateTime.now());

        // When
        AttendanceSummary result = processor.process(outRecord);

        // Then
        assertNull(result);
    }

    @Test
    void testProcess_WithExistingMonthlySummary_ShouldReturnNull() throws Exception {
        // Given
        AttendanceRecord inRecord = new AttendanceRecord();
        inRecord.setUserId(1);
        inRecord.setType("in");
        inRecord.setTimestamp(OffsetDateTime.now());

        // Mock: 既存の月次サマリーあり
        AttendanceSummary existingMonthlySummary = new AttendanceSummary();
        existingMonthlySummary.setUserId(1);
        existingMonthlySummary.setSummaryType("monthly");

        when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
                eq(1), eq("monthly"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(existingMonthlySummary));

        // When
        AttendanceSummary result = processor.process(inRecord);

        // Then
        assertNull(result);
    }

    @Test
    void testProcess_WithNoDailySummaries_ShouldReturnNull() throws Exception {
        // Given
        AttendanceRecord inRecord = new AttendanceRecord();
        inRecord.setUserId(1);
        inRecord.setType("in");
        inRecord.setTimestamp(OffsetDateTime.now());

        // Mock: 既存の月次サマリーなし
        when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
                eq(1), eq("monthly"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Mock: 日次サマリーデータなし
        when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
                eq(1), eq("daily"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        AttendanceSummary result = processor.process(inRecord);

        // Then
        assertNull(result);
    }

    @Test
    void testProcess_WithNullValues_ShouldHandleGracefully() throws Exception {
        // Given
        AttendanceRecord inRecord = new AttendanceRecord();
        inRecord.setUserId(1);
        inRecord.setType("in");
        inRecord.setTimestamp(OffsetDateTime.now());

        // Mock: 既存の月次サマリーなし
        when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
                eq(1), eq("monthly"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Mock: 日次サマリーデータ（null値含む）
        AttendanceSummary dailySummary = new AttendanceSummary();
        dailySummary.setUserId(1);
        dailySummary.setTargetDate(LocalDate.now());
        dailySummary.setTotalHours(null);
        dailySummary.setOvertimeHours(null);
        dailySummary.setLateNightHours(null);
        dailySummary.setHolidayHours(null);
        dailySummary.setSummaryType("daily");

        when(attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween(
                eq(1), eq("daily"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(dailySummary));

        // When
        AttendanceSummary result = processor.process(inRecord);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("0.00"), result.getTotalHours());
        assertEquals(new BigDecimal("0.00"), result.getOvertimeHours());
        assertEquals(new BigDecimal("0.00"), result.getLateNightHours());
        assertEquals(new BigDecimal("0.00"), result.getHolidayHours());
    }
}