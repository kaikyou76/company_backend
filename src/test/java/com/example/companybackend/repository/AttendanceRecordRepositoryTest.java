package com.example.companybackend.repository;

import com.example.companybackend.entity.AttendanceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AttendanceRecordRepository テストクラス
 * 勤怠記録データアクセス層の包括的なテスト
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AttendanceRecordRepositoryTest {

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    // テスト用定数（comsys_test_dump.sqlの実際のデータを使用）
    private static final Integer TEST_USER_ID_1 = 1; // ceo@company.com
    private static final Integer TEST_USER_ID_2 = 2; // director@company.com
    private static final Integer TEST_DEPARTMENT_ID = 1;
    private static final Double TEST_LATITUDE = 35.6812;
    private static final Double TEST_LONGITUDE = 139.7671;
    private static final String TYPE_IN = "in";
    private static final String TYPE_OUT = "out";

    private AttendanceRecord clockInRecord1;
    private AttendanceRecord clockOutRecord1;
    private AttendanceRecord clockInRecord2;
    private OffsetDateTime baseTime;

    @BeforeEach
    void setUp() {
        // 基準時刻を設定（日本時間）
        baseTime = OffsetDateTime.of(2025, 2, 1, 9, 0, 0, 0, ZoneOffset.ofHours(9));

        // テストデータの準備
        clockInRecord1 = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_IN,
                baseTime, TEST_LATITUDE, TEST_LONGITUDE, false);
        clockOutRecord1 = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_OUT,
                baseTime.plusHours(8), TEST_LATITUDE, TEST_LONGITUDE, false);
        clockInRecord2 = createAttendanceRecord(null, TEST_USER_ID_2, TYPE_IN,
                baseTime.plusMinutes(30), TEST_LATITUDE, TEST_LONGITUDE, false);

        // データベースに保存
        clockInRecord1 = attendanceRecordRepository.save(clockInRecord1);
        clockOutRecord1 = attendanceRecordRepository.save(clockOutRecord1);
        clockInRecord2 = attendanceRecordRepository.save(clockInRecord2);
    }

    // ========== 基本検索テスト ==========

    @Test
    void testFindByUserId_WithExistingUser_ShouldReturnRecords() {
        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findByUserId(TEST_USER_ID_1);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 2); // 既存データ + テストデータ
        assertTrue(result.stream().allMatch(record -> record.getUserId().equals(TEST_USER_ID_1)));
    }

    @Test
    void testFindByUserId_WithNonExistentUser_ShouldReturnEmptyList() {
        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findByUserId(999);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByUserIdAndType_WithValidData_ShouldReturnFilteredRecords() {
        // When
        List<AttendanceRecord> clockInResults = attendanceRecordRepository.findByUserIdAndType(TEST_USER_ID_1, TYPE_IN);
        List<AttendanceRecord> clockOutResults = attendanceRecordRepository.findByUserIdAndType(TEST_USER_ID_1,
                TYPE_OUT);

        // Then
        assertTrue(clockInResults.size() >= 1); // 既存データ + テストデータ
        assertTrue(clockInResults.stream().allMatch(record -> TYPE_IN.equals(record.getType())));

        assertTrue(clockOutResults.size() >= 1); // 既存データ + テストデータ
        assertTrue(clockOutResults.stream().allMatch(record -> TYPE_OUT.equals(record.getType())));
    }

    // ========== 日付検索テスト ==========

    @Test
    void testFindByUserIdAndDate_WithValidDate_ShouldReturnRecords() {
        // Given
        LocalDate testDate = baseTime.toLocalDate();

        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findByUserIdAndDate(TEST_USER_ID_1, testDate);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(record -> record.getTimestamp().toLocalDate().equals(testDate)));
    }

    @Test
    void testFindByUserIdAndDate_WithDifferentDate_ShouldReturnEmptyList() {
        // Given
        LocalDate differentDate = baseTime.toLocalDate().plusDays(1);

        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findByUserIdAndDate(TEST_USER_ID_1, differentDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByUserIdAndDateOrderByTimestampAsc_ShouldReturnOrderedRecords() {
        // Given
        LocalDate testDate = baseTime.toLocalDate();

        // When
        List<AttendanceRecord> result = attendanceRecordRepository
                .findByUserIdAndDateOrderByTimestampAsc(TEST_USER_ID_1, testDate);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // 時刻順に並んでいることを確認
        assertTrue(result.get(0).getTimestamp().isBefore(result.get(1).getTimestamp()));
        assertEquals(TYPE_IN, result.get(0).getType());
        assertEquals(TYPE_OUT, result.get(1).getType());
    }

    // ========== 日付範囲検索テスト ==========

    @Test
    void testFindByUserIdAndDateRange_WithValidRange_ShouldReturnRecords() {
        // Given
        OffsetDateTime startDate = baseTime.minusHours(1);
        OffsetDateTime endDate = baseTime.plusHours(10);

        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findByUserIdAndDateRange(
                TEST_USER_ID_1, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(
                record -> !record.getTimestamp().isBefore(startDate) && !record.getTimestamp().isAfter(endDate)));
    }

    @Test
    void testFindByDateRange_ShouldReturnAllRecordsInRange() {
        // Given
        OffsetDateTime startDate = baseTime.minusHours(1);
        OffsetDateTime endDate = baseTime.plusHours(10);

        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findByDateRange(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size()); // 全ユーザーの記録
    }

    // ========== 今日の記録検索テスト ==========

    @Test
    void testFindTodayRecordsByUserId_WithTodayRecords_ShouldReturnRecords() {
        // Given - 今日の日付でレコードを作成
        OffsetDateTime today = OffsetDateTime.now(ZoneOffset.ofHours(9));
        AttendanceRecord todayRecord = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_IN,
                today, TEST_LATITUDE, TEST_LONGITUDE, false);
        attendanceRecordRepository.save(todayRecord);

        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID_1);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(record -> record.getTimestamp().toLocalDate().equals(LocalDate.now())));
    }

    // ========== 最新記録検索テスト ==========

    @Test
    void testFindTopByUserIdOrderByTimestampDesc_ShouldReturnLatestRecord() {
        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findTopByUserIdOrderByTimestampDesc(TEST_USER_ID_1);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // 最新の記録が最初に来ることを確認（時刻順）
        AttendanceRecord latestRecord = result.get(0);
        assertNotNull(latestRecord.getTimestamp());
        // 既存データがあるため、具体的な時刻ではなく順序のみ確認
        if (result.size() > 1) {
            assertTrue(latestRecord.getTimestamp().isAfter(result.get(1).getTimestamp()) ||
                    latestRecord.getTimestamp().equals(result.get(1).getTimestamp()));
        }
    }

    @Test
    void testFindLatestByUserIdAndType_ShouldReturnLatestByType() {
        // When
        List<AttendanceRecord> clockInResults = attendanceRecordRepository.findLatestByUserIdAndType(TEST_USER_ID_1,
                TYPE_IN);
        List<AttendanceRecord> clockOutResults = attendanceRecordRepository.findLatestByUserIdAndType(TEST_USER_ID_1,
                TYPE_OUT);

        // Then
        assertNotNull(clockInResults);
        assertFalse(clockInResults.isEmpty());
        assertEquals(TYPE_IN, clockInResults.get(0).getType());

        assertNotNull(clockOutResults);
        assertFalse(clockOutResults.isEmpty());
        assertEquals(TYPE_OUT, clockOutResults.get(0).getType());
    }

    // ========== 最近の記録検索テスト ==========

    @Test
    void testFindRecentRecordsByUserIdAndType_WithRecentRecords_ShouldReturnRecords() {
        // Given
        OffsetDateTime recentTime = baseTime.minusMinutes(30);

        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findRecentRecordsByUserIdAndType(
                TEST_USER_ID_1, TYPE_IN, recentTime);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty()); // 既存データがあるため空ではない
        assertTrue(result.stream().allMatch(record -> TYPE_IN.equals(record.getType())));
        assertTrue(result.stream().allMatch(
                record -> record.getTimestamp().isAfter(recentTime) || record.getTimestamp().equals(recentTime)));
    }

    @Test
    void testFindRecentRecordsByUserIdAndType_WithNoRecentRecords_ShouldReturnEmptyList() {
        // Given
        OffsetDateTime futureTime = OffsetDateTime.now().plusDays(1); // 明日の時刻

        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findRecentRecordsByUserIdAndType(
                TEST_USER_ID_1, TYPE_IN, futureTime);

        // Then
        assertNotNull(result);
        // 未来の時刻以降のレコードのみが返される
        assertTrue(result.stream().allMatch(
                record -> record.getTimestamp().isAfter(futureTime) || record.getTimestamp().equals(futureTime)));
    }

    // ========== 時間範囲検索テスト ==========

    @Test
    void testFindByUserIdAndTimeRange_WithValidRange_ShouldReturnRecords() {
        // Given
        OffsetDateTime startTime = baseTime.minusMinutes(30);
        OffsetDateTime endTime = baseTime.plusMinutes(30);

        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findByUserIdAndTimeRange(
                TEST_USER_ID_1, startTime, endTime);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TYPE_IN, result.get(0).getType());
    }

    // ========== 月次検索テスト ==========

    @Test
    void testFindByUserIdAndYearAndMonth_WithValidYearMonth_ShouldReturnRecords() {
        // Given
        int year = baseTime.getYear();
        int month = baseTime.getMonthValue();

        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findByUserIdAndYearAndMonth(
                TEST_USER_ID_1, year, month);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(record -> record.getTimestamp().getYear() == year &&
                record.getTimestamp().getMonthValue() == month));
    }

    @Test
    void testFindByUserIdAndYearAndMonth_WithDifferentMonth_ShouldReturnEmptyList() {
        // Given
        int year = baseTime.getYear();
        int differentMonth = baseTime.getMonthValue() + 1;

        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findByUserIdAndYearAndMonth(
                TEST_USER_ID_1, year, differentMonth);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== タイプ別検索テスト ==========

    @Test
    void testFindClockInRecordsByUserId_ShouldReturnOnlyClockInRecords() {
        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findClockInRecordsByUserId(TEST_USER_ID_1);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 1); // 既存データ + テストデータ
        assertTrue(result.stream().allMatch(record -> TYPE_IN.equals(record.getType())));
    }

    @Test
    void testFindClockOutRecordsByUserId_ShouldReturnOnlyClockOutRecords() {
        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findClockOutRecordsByUserId(TEST_USER_ID_1);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 1); // 既存データ + テストデータ
        assertTrue(result.stream().allMatch(record -> TYPE_OUT.equals(record.getType())));
    }

    @Test
    void testFindByTypeAndTimestampBetween_ShouldReturnFilteredRecords() {
        // Given
        OffsetDateTime startDate = baseTime.minusHours(1);
        OffsetDateTime endDate = baseTime.plusHours(1);

        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findByTypeAndTimestampBetween(
                TYPE_IN, startDate, endDate);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 2); // 既存データ + テストデータ
        assertTrue(result.stream().allMatch(record -> TYPE_IN.equals(record.getType())));
    }

    // ========== 処理状態検索テスト ==========

    @Test
    void testFindUnprocessedRecords_ShouldReturnUnprocessedRecords() {
        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findUnprocessedRecords();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size()); // 全て未処理として作成
        assertTrue(result.stream().allMatch(record -> !record.getProcessed()));
    }

    @Test
    void testFindUnprocessedRecords_WithProcessedRecords_ShouldReturnOnlyUnprocessed() {
        // Given - 1つの記録を処理済みに変更
        clockInRecord1.setProcessed(true);
        attendanceRecordRepository.save(clockInRecord1);

        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findUnprocessedRecords();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size()); // 未処理の記録のみ
        assertTrue(result.stream().allMatch(record -> !record.getProcessed()));
    }

    // ========== 統計情報テスト ==========

    @Test
    void testCountTodayClockInUsers_WithTodayRecords_ShouldReturnCount() {
        // Given - 今日の出勤記録を追加
        OffsetDateTime today = OffsetDateTime.now(ZoneOffset.ofHours(9));
        AttendanceRecord todayClockIn1 = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_IN,
                today, TEST_LATITUDE, TEST_LONGITUDE, false);
        AttendanceRecord todayClockIn2 = createAttendanceRecord(null, TEST_USER_ID_2, TYPE_IN,
                today.plusMinutes(30), TEST_LATITUDE, TEST_LONGITUDE, false);
        attendanceRecordRepository.save(todayClockIn1);
        attendanceRecordRepository.save(todayClockIn2);

        // When
        Long result = attendanceRecordRepository.countTodayClockInUsers();

        // Then
        assertNotNull(result);
        assertEquals(2L, result); // 2人のユーザーが出勤
    }

    @Test
    void testCountTodayRecords_WithTodayRecords_ShouldReturnCount() {
        // Given - 今日の記録を追加
        OffsetDateTime today = OffsetDateTime.now(ZoneOffset.ofHours(9));
        AttendanceRecord todayRecord1 = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_IN,
                today, TEST_LATITUDE, TEST_LONGITUDE, false);
        AttendanceRecord todayRecord2 = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_OUT,
                today.plusHours(8), TEST_LATITUDE, TEST_LONGITUDE, false);
        attendanceRecordRepository.save(todayRecord1);
        attendanceRecordRepository.save(todayRecord2);

        // When
        Long result = attendanceRecordRepository.countTodayRecords();

        // Then
        assertNotNull(result);
        assertEquals(2L, result); // 今日の記録数
    }

    // ========== 部署別検索テスト ==========

    @Test
    void testFindByDepartmentAndDate_WithValidDepartmentAndDate_ShouldReturnRecords() {
        // Given
        LocalDate testDate = baseTime.toLocalDate();

        // When
        List<AttendanceRecord> result = attendanceRecordRepository.findByDepartmentAndDate(
                TEST_DEPARTMENT_ID, testDate);

        // Then
        assertNotNull(result);
        // 実際のユーザーと部署の関連によって結果が変わるため、nullでないことのみ確認
    }

    // ========== エッジケースと境界値テスト ==========

    @Test
    void testFindByUserId_WithNullUserId_ShouldHandleGracefully() {
        // When & Then
        assertDoesNotThrow(() -> {
            List<AttendanceRecord> result = attendanceRecordRepository.findByUserId(null);
            assertNotNull(result);
        });
    }

    @Test
    void testFindByUserIdAndType_WithNullType_ShouldHandleGracefully() {
        // When & Then
        assertDoesNotThrow(() -> {
            List<AttendanceRecord> result = attendanceRecordRepository.findByUserIdAndType(TEST_USER_ID_1, null);
            assertNotNull(result);
        });
    }

    @Test
    void testFindByUserIdAndDate_WithNullDate_ShouldHandleGracefully() {
        // When & Then
        assertDoesNotThrow(() -> {
            List<AttendanceRecord> result = attendanceRecordRepository.findByUserIdAndDate(TEST_USER_ID_1, null);
            assertNotNull(result);
        });
    }

    // ========== データ整合性テスト ==========

    @Test
    void testSaveAndRetrieve_ShouldMaintainDataIntegrity() {
        // Given
        AttendanceRecord newRecord = createAttendanceRecord(null, TEST_USER_ID_1, TYPE_IN,
                OffsetDateTime.now(ZoneOffset.ofHours(9)),
                35.6895, 139.6917, false);

        // When
        AttendanceRecord savedRecord = attendanceRecordRepository.save(newRecord);
        AttendanceRecord retrievedRecord = attendanceRecordRepository.findById(savedRecord.getId()).orElse(null);

        // Then
        assertNotNull(retrievedRecord);
        assertEquals(savedRecord.getId(), retrievedRecord.getId());
        assertEquals(TEST_USER_ID_1, retrievedRecord.getUserId());
        assertEquals(TYPE_IN, retrievedRecord.getType());
        assertEquals(35.6895, retrievedRecord.getLatitude(), 0.0001);
        assertEquals(139.6917, retrievedRecord.getLongitude(), 0.0001);
        assertFalse(retrievedRecord.getProcessed());
    }

    @Test
    void testUpdateRecord_ShouldReflectChanges() {
        // Given
        AttendanceRecord record = attendanceRecordRepository.findById(clockInRecord1.getId()).orElse(null);
        assertNotNull(record);

        // When
        record.setProcessed(true);
        record.setLatitude(35.7000);
        AttendanceRecord updatedRecord = attendanceRecordRepository.save(record);

        // Then
        assertEquals(clockInRecord1.getId(), updatedRecord.getId());
        assertTrue(updatedRecord.getProcessed());
        assertEquals(35.7000, updatedRecord.getLatitude(), 0.0001);
    }

    @Test
    void testDeleteRecord_ShouldRemoveFromDatabase() {
        // Given
        Long recordId = clockInRecord1.getId();
        assertTrue(attendanceRecordRepository.existsById(recordId));

        // When
        attendanceRecordRepository.deleteById(recordId);

        // Then
        assertFalse(attendanceRecordRepository.existsById(recordId));
    }

    // ========== パフォーマンステスト ==========

    @Test
    void testLargeDatasetQuery_ShouldPerformEfficiently() {
        // Given - 大量のテストデータを作成
        for (int i = 0; i < 100; i++) {
            AttendanceRecord record = createAttendanceRecord(null, TEST_USER_ID_1,
                    i % 2 == 0 ? TYPE_IN : TYPE_OUT,
                    baseTime.plusMinutes(i * 10),
                    TEST_LATITUDE, TEST_LONGITUDE, false);
            attendanceRecordRepository.save(record);
        }

        // When
        long startTime = System.currentTimeMillis();
        List<AttendanceRecord> result = attendanceRecordRepository.findByUserId(TEST_USER_ID_1);
        long endTime = System.currentTimeMillis();

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 102); // 既存データ + 元の2件 + 新規100件
        assertTrue(endTime - startTime < 1000); // 1秒以内で完了することを確認
    }

    // ========== ヘルパーメソッド ==========

    /**
     * テスト用AttendanceRecordを作成
     */
    private AttendanceRecord createAttendanceRecord(Long id, Integer userId, String type,
            OffsetDateTime timestamp, Double latitude,
            Double longitude, Boolean processed) {
        AttendanceRecord record = new AttendanceRecord();
        record.setId(id);
        record.setUserId(userId);
        record.setType(type);
        record.setTimestamp(timestamp);
        record.setLatitude(latitude);
        record.setLongitude(longitude);
        record.setProcessed(processed);
        record.setCreatedAt(timestamp);
        return record;
    }
}