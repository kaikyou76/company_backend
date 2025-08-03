package com.example.companybackend.repository;

import com.example.companybackend.entity.AttendanceSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AttendanceSummaryRepository テストクラス
 * 勤怠集計データアクセス層の包括的なテスト
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AttendanceSummaryRepositoryTest {

    @Autowired
    private AttendanceSummaryRepository attendanceSummaryRepository;

    // テスト用定数（comsys_test_dump.sqlの実際のデータを使用）
    private static final Integer TEST_USER_ID_1 = 1; // ceo@company.com
    private static final Integer TEST_USER_ID_2 = 2; // director@company.com
    private static final LocalDate TEST_DATE = LocalDate.of(2025, 2, 1);
    private static final LocalDate START_DATE = LocalDate.of(2025, 2, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 2, 28);
    private static final String SUMMARY_TYPE_DAILY = "daily";
    private static final String SUMMARY_TYPE_MONTHLY = "monthly";

    private AttendanceSummary dailySummary1;
    private AttendanceSummary dailySummary2;
    private AttendanceSummary monthlySummary1;

    @BeforeEach
    void setUp() {
        // テストデータの準備（異なる日付を使用して重複を避ける）
        dailySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE,
                new BigDecimal("8.00"), new BigDecimal("1.00"),
                new BigDecimal("0.50"), new BigDecimal("0.00"),
                SUMMARY_TYPE_DAILY);

        dailySummary2 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE.plusDays(1),
                new BigDecimal("7.50"), new BigDecimal("0.00"),
                new BigDecimal("0.00"), new BigDecimal("2.00"),
                SUMMARY_TYPE_DAILY);

        // 月次サマリーは異なる日付を使用
        monthlySummary1 = createAttendanceSummary(null, TEST_USER_ID_1, TEST_DATE.plusDays(2),
                new BigDecimal("160.00"), new BigDecimal("20.00"),
                new BigDecimal("5.00"), new BigDecimal("8.00"),
                SUMMARY_TYPE_MONTHLY);

        // データベースに保存
        dailySummary1 = attendanceSummaryRepository.save(dailySummary1);
        dailySummary2 = attendanceSummaryRepository.save(dailySummary2);
        monthlySummary1 = attendanceSummaryRepository.save(monthlySummary1);
    }

    // ========== 基本検索テスト ==========

    @Test
    void testFindByUserId_WithExistingUser_ShouldReturnSummaries() {
        // When
        List<AttendanceSummary> result = attendanceSummaryRepository.findByUserId(TEST_USER_ID_1);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size()); // テストデータのみ（データベースは空）
        assertTrue(result.stream().allMatch(summary -> summary.getUserId().equals(TEST_USER_ID_1)));
    }

    @Test
    void testFindByUserId_WithNonExistentUser_ShouldReturnEmptyList() {
        // When
        List<AttendanceSummary> result = attendanceSummaryRepository.findByUserId(999);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindBySummaryType_WithValidType_ShouldReturnFilteredSummaries() {
        // When
        List<AttendanceSummary> dailyResults = attendanceSummaryRepository.findBySummaryType(SUMMARY_TYPE_DAILY);
        List<AttendanceSummary> monthlyResults = attendanceSummaryRepository.findBySummaryType(SUMMARY_TYPE_MONTHLY);

        // Then
        assertNotNull(dailyResults);
        assertEquals(2, dailyResults.size()); // テストデータのみ
        assertTrue(dailyResults.stream().allMatch(summary -> SUMMARY_TYPE_DAILY.equals(summary.getSummaryType())));

        assertNotNull(monthlyResults);
        assertEquals(1, monthlyResults.size()); // テストデータのみ
        assertTrue(monthlyResults.stream().allMatch(summary -> SUMMARY_TYPE_MONTHLY.equals(summary.getSummaryType())));
    }

    // ========== 日付検索テスト ==========

    @Test
    void testFindByTargetDate_WithValidDate_ShouldReturnSummaries() {
        // When
        List<AttendanceSummary> result = attendanceSummaryRepository.findByTargetDate(TEST_DATE);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // dailySummary1のみ（monthlySummary1は異なる日付）
        assertTrue(result.stream().allMatch(summary -> summary.getTargetDate().equals(TEST_DATE)));
    }

    @Test
    void testFindByTargetDate_WithNonExistentDate_ShouldReturnEmptyList() {
        // Given
        LocalDate nonExistentDate = LocalDate.of(2030, 12, 31);

        // When
        List<AttendanceSummary> result = attendanceSummaryRepository.findByTargetDate(nonExistentDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByUserIdAndTargetDate_WithValidData_ShouldReturnSummary() {
        // When
        Optional<AttendanceSummary> result = attendanceSummaryRepository.findByUserIdAndTargetDate(
                TEST_USER_ID_1, TEST_DATE);

        // Then
        assertTrue(result.isPresent());
        assertEquals(TEST_USER_ID_1, result.get().getUserId());
        assertEquals(TEST_DATE, result.get().getTargetDate());
    }

    @Test
    void testFindByUserIdAndTargetDate_WithNonExistentData_ShouldReturnEmpty() {
        // Given
        LocalDate nonExistentDate = LocalDate.of(2030, 12, 31);

        // When
        Optional<AttendanceSummary> result = attendanceSummaryRepository.findByUserIdAndTargetDate(
                TEST_USER_ID_1, nonExistentDate);

        // Then
        assertFalse(result.isPresent());
    }

    // ========== 複合条件検索テスト ==========

    @Test
    void testFindByUserIdAndSummaryType_WithValidData_ShouldReturnFilteredSummaries() {
        // When
        List<AttendanceSummary> dailyResults = attendanceSummaryRepository.findByUserIdAndSummaryType(
                TEST_USER_ID_1, SUMMARY_TYPE_DAILY);
        List<AttendanceSummary> monthlyResults = attendanceSummaryRepository.findByUserIdAndSummaryType(
                TEST_USER_ID_1, SUMMARY_TYPE_MONTHLY);

        // Then
        assertNotNull(dailyResults);
        assertEquals(2, dailyResults.size()); // dailySummary1 + dailySummary2
        assertTrue(dailyResults.stream().allMatch(summary -> summary.getUserId().equals(TEST_USER_ID_1) &&
                SUMMARY_TYPE_DAILY.equals(summary.getSummaryType())));

        assertNotNull(monthlyResults);
        assertEquals(1, monthlyResults.size()); // monthlySummary1
        assertTrue(monthlyResults.stream().allMatch(summary -> summary.getUserId().equals(TEST_USER_ID_1) &&
                SUMMARY_TYPE_MONTHLY.equals(summary.getSummaryType())));
    }

    // ========== 期間検索テスト ==========

    @Test
    void testFindByUserIdAndTargetDateBetween_WithValidRange_ShouldReturnSummaries() {
        // When
        List<AttendanceSummary> result = attendanceSummaryRepository.findByUserIdAndTargetDateBetween(
                TEST_USER_ID_1, START_DATE, END_DATE);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size()); // テストデータのみ
        assertTrue(result.stream().allMatch(summary -> summary.getUserId().equals(TEST_USER_ID_1) &&
                !summary.getTargetDate().isBefore(START_DATE) &&
                !summary.getTargetDate().isAfter(END_DATE)));
    }

    @Test
    void testFindByTargetDateBetween_WithPageable_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AttendanceSummary> result = attendanceSummaryRepository.findByTargetDateBetween(
                START_DATE, END_DATE, pageable);

        // Then
        assertNotNull(result);
        assertNotNull(result.getContent());
        assertEquals(3, result.getContent().size()); // テストデータのみ
        assertTrue(result.getContent().stream().allMatch(summary -> !summary.getTargetDate().isBefore(START_DATE) &&
                !summary.getTargetDate().isAfter(END_DATE)));
    }

    @Test
    void testFindByTargetDateBetween_WithoutPageable_ShouldReturnAllResults() {
        // When
        List<AttendanceSummary> result = attendanceSummaryRepository.findByTargetDateBetween(
                START_DATE, END_DATE);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size()); // テストデータのみ
        assertTrue(result.stream().allMatch(summary -> !summary.getTargetDate().isBefore(START_DATE) &&
                !summary.getTargetDate().isAfter(END_DATE)));
    }

    @Test
    void testFindByUserIdAndSummaryTypeAndTargetDateBetween_WithValidData_ShouldReturnFilteredSummaries() {
        // When
        List<AttendanceSummary> dailyResults = attendanceSummaryRepository
                .findByUserIdAndSummaryTypeAndTargetDateBetween(
                        TEST_USER_ID_1, SUMMARY_TYPE_DAILY, START_DATE, END_DATE);
        List<AttendanceSummary> monthlyResults = attendanceSummaryRepository
                .findByUserIdAndSummaryTypeAndTargetDateBetween(
                        TEST_USER_ID_1, SUMMARY_TYPE_MONTHLY, START_DATE, END_DATE);

        // Then
        assertNotNull(dailyResults);
        assertEquals(2, dailyResults.size()); // dailySummary1 + dailySummary2
        assertTrue(dailyResults.stream().allMatch(summary -> summary.getUserId().equals(TEST_USER_ID_1) &&
                SUMMARY_TYPE_DAILY.equals(summary.getSummaryType()) &&
                !summary.getTargetDate().isBefore(START_DATE) &&
                !summary.getTargetDate().isAfter(END_DATE)));

        assertNotNull(monthlyResults);
        assertEquals(1, monthlyResults.size()); // monthlySummary1
        assertTrue(monthlyResults.stream().allMatch(summary -> summary.getUserId().equals(TEST_USER_ID_1) &&
                SUMMARY_TYPE_MONTHLY.equals(summary.getSummaryType()) &&
                !summary.getTargetDate().isBefore(START_DATE) &&
                !summary.getTargetDate().isAfter(END_DATE)));
    }

    @Test
    void testFindBySummaryTypeAndTargetDateBetween_WithValidData_ShouldReturnOrderedResults() {
        // When
        List<AttendanceSummary> dailyResults = attendanceSummaryRepository.findBySummaryTypeAndTargetDateBetween(
                SUMMARY_TYPE_DAILY, START_DATE, END_DATE);

        // Then
        assertNotNull(dailyResults);
        assertEquals(2, dailyResults.size()); // テストデータのみ
        assertTrue(dailyResults.stream().allMatch(summary -> SUMMARY_TYPE_DAILY.equals(summary.getSummaryType()) &&
                !summary.getTargetDate().isBefore(START_DATE) &&
                !summary.getTargetDate().isAfter(END_DATE)));

        // ソート順の確認（userId ASC, targetDate ASC）
        if (dailyResults.size() > 1) {
            for (int i = 0; i < dailyResults.size() - 1; i++) {
                AttendanceSummary current = dailyResults.get(i);
                AttendanceSummary next = dailyResults.get(i + 1);

                // ユーザーIDが同じ場合は日付順、異なる場合はユーザーID順
                if (current.getUserId().equals(next.getUserId())) {
                    assertTrue(current.getTargetDate().isBefore(next.getTargetDate()) ||
                            current.getTargetDate().equals(next.getTargetDate()));
                } else {
                    assertTrue(current.getUserId() <= next.getUserId());
                }
            }
        }
    }

    // ========== データ整合性テスト ==========

    @Test
    void testSaveAndRetrieve_ShouldMaintainDataIntegrity() {
        // Given
        AttendanceSummary newSummary = createAttendanceSummary(null, TEST_USER_ID_2,
                LocalDate.of(2025, 3, 1),
                new BigDecimal("9.25"), new BigDecimal("1.25"),
                new BigDecimal("0.75"), new BigDecimal("0.50"),
                SUMMARY_TYPE_DAILY);

        // When
        AttendanceSummary savedSummary = attendanceSummaryRepository.save(newSummary);
        AttendanceSummary retrievedSummary = attendanceSummaryRepository.findById(savedSummary.getId()).orElse(null);

        // Then
        assertNotNull(retrievedSummary);
        assertEquals(savedSummary.getId(), retrievedSummary.getId());
        assertEquals(TEST_USER_ID_2, retrievedSummary.getUserId());
        assertEquals(LocalDate.of(2025, 3, 1), retrievedSummary.getTargetDate());
        assertEquals(0, new BigDecimal("9.25").compareTo(retrievedSummary.getTotalHours()));
        assertEquals(0, new BigDecimal("1.25").compareTo(retrievedSummary.getOvertimeHours()));
        assertEquals(0, new BigDecimal("0.75").compareTo(retrievedSummary.getLateNightHours()));
        assertEquals(0, new BigDecimal("0.50").compareTo(retrievedSummary.getHolidayHours()));
        assertEquals(SUMMARY_TYPE_DAILY, retrievedSummary.getSummaryType());
        assertNotNull(retrievedSummary.getCreatedAt());
    }

    @Test
    void testUpdateSummary_ShouldReflectChanges() {
        // Given
        AttendanceSummary summary = attendanceSummaryRepository.findById(dailySummary1.getId()).orElse(null);
        assertNotNull(summary);

        // When
        summary.setTotalHours(new BigDecimal("9.00"));
        summary.setOvertimeHours(new BigDecimal("2.00"));
        AttendanceSummary updatedSummary = attendanceSummaryRepository.save(summary);

        // Then
        assertEquals(dailySummary1.getId(), updatedSummary.getId());
        assertEquals(0, new BigDecimal("9.00").compareTo(updatedSummary.getTotalHours()));
        assertEquals(0, new BigDecimal("2.00").compareTo(updatedSummary.getOvertimeHours()));
    }

    @Test
    void testDeleteSummary_ShouldRemoveFromDatabase() {
        // Given
        Long summaryId = dailySummary1.getId();
        assertTrue(attendanceSummaryRepository.existsById(summaryId));

        // When
        attendanceSummaryRepository.deleteById(summaryId);

        // Then
        assertFalse(attendanceSummaryRepository.existsById(summaryId));
    }

    // ========== BigDecimal精度テスト ==========

    @Test
    void testBigDecimalPrecision_ShouldMaintainAccuracy() {
        // Given
        AttendanceSummary precisionSummary = createAttendanceSummary(null, TEST_USER_ID_1,
                LocalDate.of(2025, 3, 15),
                new BigDecimal("8.33"), new BigDecimal("0.67"),
                new BigDecimal("0.17"), new BigDecimal("0.83"),
                SUMMARY_TYPE_DAILY);

        // When
        AttendanceSummary savedSummary = attendanceSummaryRepository.save(precisionSummary);
        AttendanceSummary retrievedSummary = attendanceSummaryRepository.findById(savedSummary.getId()).orElse(null);

        // Then
        assertNotNull(retrievedSummary);
        assertEquals(0, new BigDecimal("8.33").compareTo(retrievedSummary.getTotalHours()));
        assertEquals(0, new BigDecimal("0.67").compareTo(retrievedSummary.getOvertimeHours()));
        assertEquals(0, new BigDecimal("0.17").compareTo(retrievedSummary.getLateNightHours()));
        assertEquals(0, new BigDecimal("0.83").compareTo(retrievedSummary.getHolidayHours()));
    }

    @Test
    void testBigDecimalZeroValues_ShouldHandleCorrectly() {
        // Given
        AttendanceSummary zeroSummary = createAttendanceSummary(null, TEST_USER_ID_1,
                LocalDate.of(2025, 3, 20),
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                SUMMARY_TYPE_DAILY);

        // When
        AttendanceSummary savedSummary = attendanceSummaryRepository.save(zeroSummary);
        AttendanceSummary retrievedSummary = attendanceSummaryRepository.findById(savedSummary.getId()).orElse(null);

        // Then
        assertNotNull(retrievedSummary);
        assertEquals(0, BigDecimal.ZERO.compareTo(retrievedSummary.getTotalHours()));
        assertEquals(0, BigDecimal.ZERO.compareTo(retrievedSummary.getOvertimeHours()));
        assertEquals(0, BigDecimal.ZERO.compareTo(retrievedSummary.getLateNightHours()));
        assertEquals(0, BigDecimal.ZERO.compareTo(retrievedSummary.getHolidayHours()));
    }

    // ========== エッジケース・境界値テスト ==========

    @Test
    void testFindByUserId_WithNullUserId_ShouldHandleGracefully() {
        // When & Then
        assertDoesNotThrow(() -> {
            List<AttendanceSummary> result = attendanceSummaryRepository.findByUserId(null);
            assertNotNull(result);
        });
    }

    @Test
    void testFindBySummaryType_WithNullType_ShouldHandleGracefully() {
        // When & Then
        assertDoesNotThrow(() -> {
            List<AttendanceSummary> result = attendanceSummaryRepository.findBySummaryType(null);
            assertNotNull(result);
        });
    }

    @Test
    void testFindByTargetDate_WithNullDate_ShouldHandleGracefully() {
        // When & Then
        assertDoesNotThrow(() -> {
            List<AttendanceSummary> result = attendanceSummaryRepository.findByTargetDate(null);
            assertNotNull(result);
        });
    }

    @Test
    void testDateRangeQuery_WithInvalidRange_ShouldReturnEmptyList() {
        // Given - 開始日が終了日より後の無効な範囲
        LocalDate invalidStartDate = LocalDate.of(2025, 3, 1);
        LocalDate invalidEndDate = LocalDate.of(2025, 2, 1);

        // When
        List<AttendanceSummary> result = attendanceSummaryRepository.findByTargetDateBetween(
                invalidStartDate, invalidEndDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== パフォーマンステスト ==========

    @Test
    void testLargeDatasetQuery_ShouldPerformEfficiently() {
        // Given - 大量のテストデータを作成
        for (int i = 0; i < 50; i++) {
            AttendanceSummary summary = createAttendanceSummary(null, TEST_USER_ID_1,
                    TEST_DATE.plusDays(i % 30 + 10), // 既存テストデータと重複しないように調整
                    new BigDecimal("8.00"), new BigDecimal("1.00"),
                    new BigDecimal("0.50"), new BigDecimal("0.00"),
                    SUMMARY_TYPE_DAILY);
            attendanceSummaryRepository.save(summary);
        }

        // When
        long startTime = System.currentTimeMillis();
        List<AttendanceSummary> result = attendanceSummaryRepository.findByUserId(TEST_USER_ID_1);
        long endTime = System.currentTimeMillis();

        // Then
        assertNotNull(result);
        assertEquals(53, result.size()); // 元の3件 + 新規50件
        assertTrue(endTime - startTime < 1000); // 1秒以内で完了することを確認
    }

    // ========== ヘルパーメソッド ==========

    /**
     * テスト用AttendanceSummaryを作成
     */
    private AttendanceSummary createAttendanceSummary(Long id, Integer userId, LocalDate targetDate,
            BigDecimal totalHours, BigDecimal overtimeHours,
            BigDecimal lateNightHours, BigDecimal holidayHours,
            String summaryType) {
        AttendanceSummary summary = new AttendanceSummary();
        summary.setId(id);
        summary.setUserId(userId);
        summary.setTargetDate(targetDate);
        summary.setTotalHours(totalHours);
        summary.setOvertimeHours(overtimeHours);
        summary.setLateNightHours(lateNightHours);
        summary.setHolidayHours(holidayHours);
        summary.setSummaryType(summaryType);
        summary.setCreatedAt(OffsetDateTime.now(ZoneOffset.ofHours(9)));
        return summary;
    }
}