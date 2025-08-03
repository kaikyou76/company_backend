package com.example.companybackend.service;

import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.entity.User;
import com.example.companybackend.repository.AttendanceSummaryRepository;
import com.example.companybackend.repository.AttendanceRecordRepository;
import com.example.companybackend.repository.HolidayRepository;
import com.example.companybackend.repository.UserRepository;
import com.example.companybackend.service.impl.AttendanceSummaryServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceSummaryServiceTest {

        @Mock
        private AttendanceSummaryRepository attendanceSummaryRepository;

        @Mock
        private AttendanceRecordRepository attendanceRecordRepository;

        @Mock
        private HolidayRepository holidayRepository;

        @Mock
        private UserRepository userRepository;

        private AttendanceSummaryService attendanceSummaryService;

        // テスト用定数
        private static final Integer TEST_USER_ID = 1;
        private static final Long TEST_USER_ID_LONG = 1L;
        private static final Integer TEST_DEPARTMENT_ID = 10;
        private static final LocalDate TEST_DATE = LocalDate.of(2025, 2, 1);
        private static final LocalDate START_DATE = LocalDate.of(2025, 2, 1);
        private static final LocalDate END_DATE = LocalDate.of(2025, 2, 28);

        @BeforeEach
        void setUp() {
                attendanceSummaryService = new AttendanceSummaryServiceImpl(
                                attendanceSummaryRepository,
                                attendanceRecordRepository,
                                holidayRepository,
                                userRepository);
        }

        // ========== 日別サマリー取得テスト ==========

        @Test
        void testGetDailySummaries_WithValidDateRange_ShouldReturnPagedResults() {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                List<AttendanceSummary> summaries = Arrays.asList(
                                createAttendanceSummary(1L, TEST_USER_ID, TEST_DATE, "8.00", "1.00", "0.50", "0.00",
                                                "daily"),
                                createAttendanceSummary(2L, TEST_USER_ID, TEST_DATE.plusDays(1), "7.50", "0.00", "0.00",
                                                "0.00",
                                                "daily"));
                Page<AttendanceSummary> expectedPage = new PageImpl<>(summaries, pageable, summaries.size());

                when(attendanceSummaryRepository.findByTargetDateBetween(START_DATE, END_DATE, pageable))
                                .thenReturn(expectedPage);

                // When
                Page<AttendanceSummary> result = attendanceSummaryService.getDailySummaries(START_DATE, END_DATE,
                                pageable);

                // Then
                assertNotNull(result);
                assertEquals(2, result.getContent().size());
                assertEquals(TEST_USER_ID, result.getContent().get(0).getUserId());
                assertEquals(TEST_DATE, result.getContent().get(0).getTargetDate());
                assertEquals(new BigDecimal("8.00"), result.getContent().get(0).getTotalHours());

                verify(attendanceSummaryRepository).findByTargetDateBetween(START_DATE, END_DATE, pageable);
        }

        @Test
        void testGetDailySummaries_WithEmptyResult_ShouldReturnEmptyPage() {
                // Given
                Pageable pageable = PageRequest.of(0, 10);
                Page<AttendanceSummary> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

                when(attendanceSummaryRepository.findByTargetDateBetween(START_DATE, END_DATE, pageable))
                                .thenReturn(emptyPage);

                // When
                Page<AttendanceSummary> result = attendanceSummaryService.getDailySummaries(START_DATE, END_DATE,
                                pageable);

                // Then
                assertNotNull(result);
                assertTrue(result.getContent().isEmpty());
                assertEquals(0, result.getTotalElements());

                verify(attendanceSummaryRepository).findByTargetDateBetween(START_DATE, END_DATE, pageable);
        }

        // ========== 月別サマリー取得テスト ==========

        @Test
        void testGetMonthlySummaries_WithValidYearMonth_ShouldReturnPagedResults() {
                // Given
                YearMonth yearMonth = YearMonth.of(2025, 2);
                Pageable pageable = PageRequest.of(0, 10);
                LocalDate expectedStartDate = yearMonth.atDay(1);
                LocalDate expectedEndDate = yearMonth.atEndOfMonth();

                List<AttendanceSummary> summaries = Arrays.asList(
                                createAttendanceSummary(1L, TEST_USER_ID, expectedStartDate, "160.00", "20.00", "5.00",
                                                "8.00",
                                                "monthly"));
                Page<AttendanceSummary> expectedPage = new PageImpl<>(summaries, pageable, summaries.size());

                when(attendanceSummaryRepository.findByTargetDateBetween(expectedStartDate, expectedEndDate, pageable))
                                .thenReturn(expectedPage);

                // When
                Page<AttendanceSummary> result = attendanceSummaryService.getMonthlySummaries(yearMonth, pageable);

                // Then
                assertNotNull(result);
                assertEquals(1, result.getContent().size());
                assertEquals("monthly", result.getContent().get(0).getSummaryType());
                assertEquals(new BigDecimal("160.00"), result.getContent().get(0).getTotalHours());

                verify(attendanceSummaryRepository).findByTargetDateBetween(expectedStartDate, expectedEndDate,
                                pageable);
        }

        @Test
        void testGetMonthlySummaries_WithLeapYear_ShouldHandleFebruaryCorrectly() {
                // Given
                YearMonth leapYearMonth = YearMonth.of(2024, 2); // 2024年は閏年
                Pageable pageable = PageRequest.of(0, 10);
                LocalDate expectedStartDate = LocalDate.of(2024, 2, 1);
                LocalDate expectedEndDate = LocalDate.of(2024, 2, 29); // 閏年の2月29日

                Page<AttendanceSummary> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
                when(attendanceSummaryRepository.findByTargetDateBetween(expectedStartDate, expectedEndDate, pageable))
                                .thenReturn(emptyPage);

                // When
                Page<AttendanceSummary> result = attendanceSummaryService.getMonthlySummaries(leapYearMonth, pageable);

                // Then
                assertNotNull(result);
                verify(attendanceSummaryRepository).findByTargetDateBetween(expectedStartDate, expectedEndDate,
                                pageable);
        }

        // ========== 日付指定サマリー取得テスト ==========

        @Test
        void testGetSummaryByDate_WithExistingData_ShouldReturnSummary() {
                // Given
                AttendanceSummary expectedSummary = createAttendanceSummary(1L, TEST_USER_ID, TEST_DATE,
                                "8.00", "1.00", "0.50", "0.00", "daily");

                when(attendanceSummaryRepository.findByTargetDate(TEST_DATE))
                                .thenReturn(Arrays.asList(expectedSummary));

                // When
                AttendanceSummary result = attendanceSummaryService.getSummaryByDate(TEST_DATE);

                // Then
                assertNotNull(result);
                assertEquals(expectedSummary.getId(), result.getId());
                assertEquals(TEST_DATE, result.getTargetDate());
                assertEquals(new BigDecimal("8.00"), result.getTotalHours());

                verify(attendanceSummaryRepository).findByTargetDate(TEST_DATE);
        }

        @Test
        void testGetSummaryByDate_WithNoData_ShouldReturnNull() {
                // Given
                when(attendanceSummaryRepository.findByTargetDate(TEST_DATE))
                                .thenReturn(Collections.emptyList());

                // When
                AttendanceSummary result = attendanceSummaryService.getSummaryByDate(TEST_DATE);

                // Then
                assertNull(result);
                verify(attendanceSummaryRepository).findByTargetDate(TEST_DATE);
        }

        // ========== 日別サマリー生成テスト ==========

        @Test
        void testGenerateDailySummary_WithValidDate_ShouldCreateAndSaveSummary() {
                // Given
                AttendanceSummary savedSummary = createAttendanceSummary(1L, null, TEST_DATE,
                                "0.00", "0.00", "0.00", "0.00", "daily");

                when(attendanceSummaryRepository.save(any(AttendanceSummary.class)))
                                .thenReturn(savedSummary);

                // When
                AttendanceSummary result = attendanceSummaryService.generateDailySummary(TEST_DATE);

                // Then
                assertNotNull(result);
                assertEquals(TEST_DATE, result.getTargetDate());
                assertEquals("daily", result.getSummaryType());
                assertEquals(0, result.getTotalHours().compareTo(BigDecimal.ZERO));
                assertEquals(0, result.getOvertimeHours().compareTo(BigDecimal.ZERO));

                verify(attendanceSummaryRepository).save(any(AttendanceSummary.class));
        }

        // ========== 統計情報取得テスト ==========

        @Test
        void testGetSummaryStatistics_WithValidData_ShouldCalculateCorrectly() {
                // Given
                List<AttendanceSummary> summaries = Arrays.asList(
                                createAttendanceSummary(1L, TEST_USER_ID, TEST_DATE, "8.00", "1.00", "0.50", "0.00",
                                                "daily"),
                                createAttendanceSummary(2L, TEST_USER_ID, TEST_DATE.plusDays(1), "7.50", "0.00", "0.00",
                                                "2.00",
                                                "daily"),
                                createAttendanceSummary(3L, TEST_USER_ID, TEST_DATE.plusDays(2), "9.00", "2.00", "1.00",
                                                "0.00",
                                                "daily"));

                when(attendanceSummaryRepository.findByTargetDateBetween(START_DATE, END_DATE))
                                .thenReturn(summaries);

                // When
                Map<String, Object> result = attendanceSummaryService.getSummaryStatistics(START_DATE, END_DATE);

                // Then
                assertNotNull(result);
                assertEquals(3, result.get("totalRecords"));
                assertEquals(24.5, (Double) result.get("totalHours"), 0.01); // 8.00 + 7.50 + 9.00
                assertEquals(3.0, (Double) result.get("overtimeHours"), 0.01); // 1.00 + 0.00 + 2.00

                verify(attendanceSummaryRepository).findByTargetDateBetween(START_DATE, END_DATE);
        }

        @Test
        void testGetSummaryStatistics_WithNullValues_ShouldHandleGracefully() {
                // Given
                List<AttendanceSummary> summaries = Arrays.asList(
                                createAttendanceSummaryWithNulls(1L, TEST_USER_ID, TEST_DATE, "daily"));

                when(attendanceSummaryRepository.findByTargetDateBetween(START_DATE, END_DATE))
                                .thenReturn(summaries);

                // When
                Map<String, Object> result = attendanceSummaryService.getSummaryStatistics(START_DATE, END_DATE);

                // Then
                assertNotNull(result);
                assertEquals(1, result.get("totalRecords"));
                assertEquals(0.0, (Double) result.get("totalHours"), 0.01);
                assertEquals(0.0, (Double) result.get("overtimeHours"), 0.01);

                verify(attendanceSummaryRepository).findByTargetDateBetween(START_DATE, END_DATE);
        }

        // ========== エクスポート機能テスト ==========

        @Test
        void testGetSummariesForExport_WithValidDateRange_ShouldReturnAllData() {
                // Given
                List<AttendanceSummary> expectedSummaries = Arrays.asList(
                                createAttendanceSummary(1L, TEST_USER_ID, TEST_DATE, "8.00", "1.00", "0.50", "0.00",
                                                "daily"),
                                createAttendanceSummary(2L, TEST_USER_ID, TEST_DATE.plusDays(1), "7.50", "0.00", "0.00",
                                                "2.00",
                                                "daily"));

                when(attendanceSummaryRepository.findByTargetDateBetween(START_DATE, END_DATE))
                                .thenReturn(expectedSummaries);

                // When
                List<AttendanceSummary> result = attendanceSummaryService.getSummariesForExport(START_DATE, END_DATE);

                // Then
                assertNotNull(result);
                assertEquals(2, result.size());
                assertEquals(TEST_DATE, result.get(0).getTargetDate());
                assertEquals(new BigDecimal("8.00"), result.get(0).getTotalHours());

                verify(attendanceSummaryRepository).findByTargetDateBetween(START_DATE, END_DATE);
        }

        @Test
        void testExportSummariesToCSV_WithValidData_ShouldGenerateCorrectFormat() throws Exception {
                // Given
                List<AttendanceSummary> summaries = Arrays.asList(
                                createAttendanceSummary(1L, TEST_USER_ID, TEST_DATE, "8.00", "1.00", "0.50", "0.00",
                                                "daily"),
                                createAttendanceSummary(2L, TEST_USER_ID, TEST_DATE.plusDays(1), "7.50", "0.00", "0.00",
                                                "2.00",
                                                "daily"));

                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);

                // When
                attendanceSummaryService.exportSummariesToCSV(summaries, printWriter);
                printWriter.flush();

                // Then
                String csvOutput = stringWriter.toString();
                assertNotNull(csvOutput);
                assertTrue(csvOutput.contains("Date,Total Hours,Overtime Hours,Late Night Hours,Holiday Hours"));
                assertTrue(csvOutput.contains("2025-02-01,8.00,1.00,0.50,0.00"));
                assertTrue(csvOutput.contains("2025-02-02,7.50,0.00,0.00,2.00"));

                // 行数確認（ヘッダー + データ2行 = 3行）
                String[] lines = csvOutput.trim().split("\n");
                assertEquals(3, lines.length);
        }

        @Test
        void testExportSummariesToCSV_WithEmptyData_ShouldGenerateHeaderOnly() throws Exception {
                // Given
                List<AttendanceSummary> emptySummaries = Collections.emptyList();
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);

                // When
                attendanceSummaryService.exportSummariesToCSV(emptySummaries, printWriter);
                printWriter.flush();

                // Then
                String csvOutput = stringWriter.toString();
                assertNotNull(csvOutput);
                assertTrue(csvOutput.contains("Date,Total Hours,Overtime Hours,Late Night Hours,Holiday Hours"));

                // ヘッダーのみ（1行）
                String[] lines = csvOutput.trim().split("\n");
                assertEquals(1, lines.length);
        }

        @Test
        void testExportSummariesToJSON_WithValidData_ShouldGenerateCorrectFormat() throws Exception {
                // Given
                List<AttendanceSummary> summaries = Arrays.asList(
                                createAttendanceSummary(1L, TEST_USER_ID, TEST_DATE, "8.00", "1.00", "0.50", "0.00",
                                                "daily"),
                                createAttendanceSummary(2L, TEST_USER_ID, TEST_DATE.plusDays(1), "7.50", "0.00", "0.00",
                                                "2.00",
                                                "daily"));

                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);

                // When
                attendanceSummaryService.exportSummariesToJSON(summaries, printWriter);
                printWriter.flush();

                // Then
                String jsonOutput = stringWriter.toString();
                assertNotNull(jsonOutput);
                assertTrue(jsonOutput.contains("["));
                assertTrue(jsonOutput.contains("]"));
                assertTrue(jsonOutput.contains("\"date\": \"2025-02-01\""));
                assertTrue(jsonOutput.contains("\"totalHours\": 8.00"));
                assertTrue(jsonOutput.contains("\"overtimeHours\": 1.00"));
                assertTrue(jsonOutput.contains("\"date\": \"2025-02-02\""));
                assertTrue(jsonOutput.contains("\"holidayHours\": 2.00"));
        }

        @Test
        void testExportSummariesToJSON_WithEmptyData_ShouldGenerateEmptyArray() throws Exception {
                // Given
                List<AttendanceSummary> emptySummaries = Collections.emptyList();
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);

                // When
                attendanceSummaryService.exportSummariesToJSON(emptySummaries, printWriter);
                printWriter.flush();

                // Then
                String jsonOutput = stringWriter.toString();
                assertNotNull(jsonOutput);
                assertTrue(jsonOutput.contains("["));
                assertTrue(jsonOutput.contains("]"));

                // 空の配列のみ
                String trimmed = jsonOutput.trim();
                assertTrue(trimmed.startsWith("["));
                assertTrue(trimmed.endsWith("]"));
        }

        // ========== 月別統計情報テスト ==========

        @Test
        void testGetMonthlyStatistics_WithValidData_ShouldCalculateCorrectly() {
                // Given
                List<AttendanceSummary> summaries = Arrays.asList(
                                // 2月のデータ
                                createAttendanceSummary(1L, TEST_USER_ID, LocalDate.of(2025, 2, 1), "8.00", "1.00",
                                                "0.50", "0.00",
                                                "daily"),
                                createAttendanceSummary(2L, TEST_USER_ID, LocalDate.of(2025, 2, 2), "7.50", "0.00",
                                                "0.00", "0.00",
                                                "daily"),
                                // 3月のデータ
                                createAttendanceSummary(3L, TEST_USER_ID, LocalDate.of(2025, 3, 1), "9.00", "2.00",
                                                "1.00", "0.00",
                                                "daily"));

                when(attendanceSummaryRepository.findByTargetDateBetween(
                                LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 31)))
                                .thenReturn(summaries);

                // When
                Map<String, Object> result = attendanceSummaryService.getMonthlyStatistics(
                                LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 31));

                // Then
                assertNotNull(result);

                @SuppressWarnings("unchecked")
                Map<String, Double> monthlyHours = (Map<String, Double>) result.get("monthlyHours");
                assertNotNull(monthlyHours);
                assertEquals(15.5, monthlyHours.get("2025-02-01"), 0.01); // 2月: 8.00 + 7.50
                assertEquals(9.0, monthlyHours.get("2025-03-01"), 0.01); // 3月: 9.00

                Double averageDailyHours = (Double) result.get("averageDailyHours");
                assertEquals(8.17, averageDailyHours, 0.01); // (8.00 + 7.50 + 9.00) / 3

                verify(attendanceSummaryRepository).findByTargetDateBetween(
                                LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 31));
        }

        // ========== 個人別統計情報テスト ==========

        @Test
        void testGetPersonalAttendanceStatistics_WithValidData_ShouldCalculateCorrectly() {
                // Given
                List<AttendanceSummary> personalSummaries = Arrays.asList(
                                createAttendanceSummary(1L, TEST_USER_ID, TEST_DATE, "8.00", "1.00", "0.50", "0.00",
                                                "daily"),
                                createAttendanceSummary(2L, TEST_USER_ID, TEST_DATE.plusDays(1), "7.50", "0.00", "0.00",
                                                "2.00",
                                                "daily"),
                                createAttendanceSummary(3L, TEST_USER_ID, TEST_DATE.plusDays(2), "9.00", "2.00", "1.00",
                                                "0.00",
                                                "daily"));

                when(attendanceSummaryRepository.findByUserIdAndTargetDateBetween(TEST_USER_ID, START_DATE, END_DATE))
                                .thenReturn(personalSummaries);

                // When
                Map<String, Object> result = attendanceSummaryService.getPersonalAttendanceStatistics(
                                TEST_USER_ID_LONG, START_DATE, END_DATE);

                // Then
                assertNotNull(result);
                assertEquals(TEST_USER_ID_LONG, result.get("userId"));
                assertEquals(3, result.get("totalRecords"));
                assertEquals(24.5, (Double) result.get("totalHours"), 0.01); // 8.00 + 7.50 + 9.00
                assertEquals(3.0, (Double) result.get("overtimeHours"), 0.01); // 1.00 + 0.00 + 2.00
                assertEquals(1.5, (Double) result.get("lateNightHours"), 0.01); // 0.50 + 0.00 + 1.00
                assertEquals(2.0, (Double) result.get("holidayHours"), 0.01); // 0.00 + 2.00 + 0.00
                assertEquals(START_DATE, result.get("startDate"));
                assertEquals(END_DATE, result.get("endDate"));

                verify(attendanceSummaryRepository).findByUserIdAndTargetDateBetween(TEST_USER_ID, START_DATE,
                                END_DATE);
        }

        @Test
        void testGetPersonalAttendanceStatistics_WithNoData_ShouldReturnZeroValues() {
                // Given
                when(attendanceSummaryRepository.findByUserIdAndTargetDateBetween(TEST_USER_ID, START_DATE, END_DATE))
                                .thenReturn(Collections.emptyList());

                // When
                Map<String, Object> result = attendanceSummaryService.getPersonalAttendanceStatistics(
                                TEST_USER_ID_LONG, START_DATE, END_DATE);

                // Then
                assertNotNull(result);
                assertEquals(TEST_USER_ID_LONG, result.get("userId"));
                assertEquals(0, result.get("totalRecords"));
                assertEquals(0.0, (Double) result.get("totalHours"), 0.01);
                assertEquals(0.0, (Double) result.get("overtimeHours"), 0.01);
                assertEquals(0.0, (Double) result.get("lateNightHours"), 0.01);
                assertEquals(0.0, (Double) result.get("holidayHours"), 0.01);

                verify(attendanceSummaryRepository).findByUserIdAndTargetDateBetween(TEST_USER_ID, START_DATE,
                                END_DATE);
        }

        // ========== 部門別統計情報テスト ==========

        @Test
        void testGetDepartmentAttendanceStatistics_WithValidData_ShouldCalculateCorrectly() {
                // Given
                List<User> departmentUsers = Arrays.asList(
                                createUser(1L, TEST_DEPARTMENT_ID),
                                createUser(2L, TEST_DEPARTMENT_ID),
                                createUser(3L, TEST_DEPARTMENT_ID));

                List<AttendanceSummary> user1Summaries = Arrays.asList(
                                createAttendanceSummary(1L, 1, TEST_DATE, "8.00", "1.00", "0.50", "0.00", "daily"),
                                createAttendanceSummary(2L, 1, TEST_DATE.plusDays(1), "7.50", "0.00", "0.00", "0.00",
                                                "daily"));

                List<AttendanceSummary> user2Summaries = Arrays.asList(
                                createAttendanceSummary(3L, 2, TEST_DATE, "9.00", "2.00", "1.00", "0.00", "daily"));

                List<AttendanceSummary> user3Summaries = Collections.emptyList();

                when(userRepository.findByDepartmentId(TEST_DEPARTMENT_ID)).thenReturn(departmentUsers);
                when(attendanceSummaryRepository.findByUserIdAndTargetDateBetween(1, START_DATE, END_DATE))
                                .thenReturn(user1Summaries);
                when(attendanceSummaryRepository.findByUserIdAndTargetDateBetween(2, START_DATE, END_DATE))
                                .thenReturn(user2Summaries);
                when(attendanceSummaryRepository.findByUserIdAndTargetDateBetween(3, START_DATE, END_DATE))
                                .thenReturn(user3Summaries);

                // When
                Map<String, Object> result = attendanceSummaryService.getDepartmentAttendanceStatistics(
                                TEST_DEPARTMENT_ID, START_DATE, END_DATE);

                // Then
                assertNotNull(result);
                assertEquals(TEST_DEPARTMENT_ID, result.get("departmentId"));
                assertEquals(3, result.get("userCount"));
                assertEquals(3, result.get("totalRecords")); // user1: 2件, user2: 1件, user3: 0件
                assertEquals(24.5, (Double) result.get("totalHours"), 0.01); // 8.00 + 7.50 + 9.00
                assertEquals(8.17, (Double) result.get("averageHoursPerUser"), 0.01); // 24.5 / 3
                assertEquals(3.0, (Double) result.get("overtimeHours"), 0.01); // 1.00 + 0.00 + 2.00
                assertEquals(1.5, (Double) result.get("lateNightHours"), 0.01); // 0.50 + 0.00 + 1.00
                assertEquals(0.0, (Double) result.get("holidayHours"), 0.01); // 0.00 + 0.00 + 0.00

                verify(userRepository).findByDepartmentId(TEST_DEPARTMENT_ID);
                verify(attendanceSummaryRepository).findByUserIdAndTargetDateBetween(1, START_DATE, END_DATE);
                verify(attendanceSummaryRepository).findByUserIdAndTargetDateBetween(2, START_DATE, END_DATE);
                verify(attendanceSummaryRepository).findByUserIdAndTargetDateBetween(3, START_DATE, END_DATE);
        }

        @Test
        void testGetDepartmentAttendanceStatistics_WithNoUsers_ShouldReturnZeroValues() {
                // Given
                when(userRepository.findByDepartmentId(TEST_DEPARTMENT_ID)).thenReturn(Collections.emptyList());

                // When
                Map<String, Object> result = attendanceSummaryService.getDepartmentAttendanceStatistics(
                                TEST_DEPARTMENT_ID, START_DATE, END_DATE);

                // Then
                assertNotNull(result);
                assertEquals(TEST_DEPARTMENT_ID, result.get("departmentId"));
                assertEquals(0, result.get("userCount"));
                assertEquals(0, result.get("totalRecords"));
                assertEquals(0.0, (Double) result.get("totalHours"), 0.01);
                assertEquals(0.0, (Double) result.get("averageHoursPerUser"), 0.01);
                assertEquals(0.0, (Double) result.get("overtimeHours"), 0.01);

                verify(userRepository).findByDepartmentId(TEST_DEPARTMENT_ID);
        }

        // ========== ヘルパーメソッド ==========

        private AttendanceSummary createAttendanceSummary(Long id, Integer userId, LocalDate targetDate,
                        String totalHours, String overtimeHours,
                        String lateNightHours, String holidayHours,
                        String summaryType) {
                AttendanceSummary summary = new AttendanceSummary();
                summary.setId(id);
                summary.setUserId(userId);
                summary.setTargetDate(targetDate);
                summary.setTotalHours(new BigDecimal(totalHours));
                summary.setOvertimeHours(new BigDecimal(overtimeHours));
                summary.setLateNightHours(new BigDecimal(lateNightHours));
                summary.setHolidayHours(new BigDecimal(holidayHours));
                summary.setSummaryType(summaryType);
                summary.setCreatedAt(OffsetDateTime.now(ZoneOffset.ofHours(9)));
                return summary;
        }

        private AttendanceSummary createAttendanceSummaryWithNulls(Long id, Integer userId, LocalDate targetDate,
                        String summaryType) {
                AttendanceSummary summary = new AttendanceSummary();
                summary.setId(id);
                summary.setUserId(userId);
                summary.setTargetDate(targetDate);
                summary.setTotalHours(null);
                summary.setOvertimeHours(null);
                summary.setLateNightHours(null);
                summary.setHolidayHours(null);
                summary.setSummaryType(summaryType);
                summary.setCreatedAt(OffsetDateTime.now(ZoneOffset.ofHours(9)));
                return summary;
        }

        private User createUser(Long id, Integer departmentId) {
                User user = new User();
                user.setId(id);
                user.setDepartmentId(departmentId);
                user.setUsername("user" + id);
                user.setEmail("user" + id + "@example.com");
                return user;
        }
}