package com.example.companybackend.service;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.entity.User;
import com.example.companybackend.entity.WorkLocation;
import com.example.companybackend.repository.AttendanceRecordRepository;
import com.example.companybackend.repository.AttendanceSummaryRepository;
import com.example.companybackend.repository.UserRepository;
import com.example.companybackend.repository.WorkLocationRepository;
import com.example.companybackend.service.AttendanceService.ClockInRequest;
import com.example.companybackend.service.AttendanceService.ClockOutRequest;
import com.example.companybackend.service.AttendanceService.ClockInResponse;
import com.example.companybackend.service.AttendanceService.ClockOutResponse;
import com.example.companybackend.service.AttendanceService.DailySummaryData;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private AttendanceSummaryRepository attendanceSummaryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkLocationRepository workLocationRepository;

    private AttendanceService attendanceService;

    // テスト用定数
    private static final Integer TEST_USER_ID = 1;
    private static final Long TEST_USER_ID_LONG = 1L;
    private static final Double VALID_LATITUDE = 35.6812;
    private static final Double VALID_LONGITUDE = 139.7671;
    private static final Double OFFICE_LATITUDE = 35.6812;
    private static final Double OFFICE_LONGITUDE = 139.7671;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(
                attendanceRecordRepository,
                attendanceSummaryRepository,
                userRepository,
                workLocationRepository);
    }

    // ========== 出勤打刻テスト ==========

    @Test
    void testClockIn_WithValidData_ShouldCreateRecord() {
        // Given
        User user = createTestUser(TEST_USER_ID_LONG, "office", false);
        WorkLocation officeLocation = createWorkLocation("office", OFFICE_LATITUDE, OFFICE_LONGITUDE, 100);

        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("office")).thenReturn(Arrays.asList(officeLocation));
        when(attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findRecentRecordsByUserIdAndType(eq(TEST_USER_ID), eq("in"),
                any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> {
            AttendanceRecord record = invocation.getArgument(0);
            record.setId(1L);
            return record;
        });

        // When
        AttendanceRecord result = attendanceService.clockIn(TEST_USER_ID, VALID_LATITUDE, VALID_LONGITUDE);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals("in", result.getType());
        assertEquals(VALID_LATITUDE, result.getLatitude());
        assertEquals(VALID_LONGITUDE, result.getLongitude());
        assertNotNull(result.getTimestamp());

        verify(attendanceRecordRepository).save(any(AttendanceRecord.class));
    }

    @Test
    void testClockIn_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            attendanceService.clockIn(TEST_USER_ID, VALID_LATITUDE, VALID_LONGITUDE);
        });

        assertEquals("ユーザーが見つかりません: " + TEST_USER_ID, exception.getMessage());
        verify(attendanceRecordRepository, never()).save(any(AttendanceRecord.class));
    }

    @Test
    void testClockIn_WithNullLocation_ShouldThrowException() {
        // Given
        User user = createTestUser(TEST_USER_ID_LONG, "office", false);
        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            attendanceService.clockIn(TEST_USER_ID, null, VALID_LONGITUDE);
        });

        assertEquals("位置情報が必要です", exception.getMessage());
    }

    @Test
    void testClockIn_WithInvalidLatitude_ShouldThrowException() {
        // Given
        User user = createTestUser(TEST_USER_ID_LONG, "office", false);
        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            attendanceService.clockIn(TEST_USER_ID, 91.0, VALID_LONGITUDE); // 緯度の範囲外
        });

        assertEquals("位置情報が無効です", exception.getMessage());
    }

    @Test
    void testClockIn_WithAlreadyClockedIn_ShouldThrowException() {
        // Given
        User user = createTestUser(TEST_USER_ID_LONG, "office", false);
        AttendanceRecord existingRecord = createAttendanceRecord(1L, TEST_USER_ID, "in", OffsetDateTime.now());

        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("office")).thenReturn(Arrays.asList(
                createWorkLocation("office", OFFICE_LATITUDE, OFFICE_LONGITUDE, 100)));
        when(attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID))
                .thenReturn(Arrays.asList(existingRecord));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            attendanceService.clockIn(TEST_USER_ID, VALID_LATITUDE, VALID_LONGITUDE);
        });

        assertEquals("既に出勤打刻済みです", exception.getMessage());
    }

    @Test
    void testClockIn_WithRecentDuplicateRecord_ShouldThrowException() {
        // Given
        User user = createTestUser(TEST_USER_ID_LONG, "office", false);
        AttendanceRecord recentRecord = createAttendanceRecord(1L, TEST_USER_ID, "in",
                OffsetDateTime.now().minusMinutes(2));

        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("office")).thenReturn(Arrays.asList(
                createWorkLocation("office", OFFICE_LATITUDE, OFFICE_LONGITUDE, 100)));
        when(attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findRecentRecordsByUserIdAndType(eq(TEST_USER_ID), eq("in"),
                any(OffsetDateTime.class)))
                .thenReturn(Arrays.asList(recentRecord));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            attendanceService.clockIn(TEST_USER_ID, VALID_LATITUDE, VALID_LONGITUDE);
        });

        assertEquals("5分以内に重複する出勤打刻があります", exception.getMessage());
    }

    @Test
    void testClockIn_WithSkipLocationCheck_ShouldSucceed() {
        // Given
        User user = createTestUser(TEST_USER_ID_LONG, "office", true); // 位置チェックスキップ

        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));
        when(attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findRecentRecordsByUserIdAndType(eq(TEST_USER_ID), eq("in"),
                any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> {
            AttendanceRecord record = invocation.getArgument(0);
            record.setId(1L);
            return record;
        });

        // When
        AttendanceRecord result = attendanceService.clockIn(TEST_USER_ID, 0.0, 0.0); // 無効な位置でもOK

        // Then
        assertNotNull(result);
        assertEquals("in", result.getType());
        verify(workLocationRepository, never()).findByType(anyString()); // 位置チェックがスキップされる
    }

    @Test
    void testClockIn_WithClientLocation_ShouldValidateClientRadius() {
        // Given
        User user = createTestUser(TEST_USER_ID_LONG, "client", false);
        WorkLocation clientLocation = createWorkLocation("client", VALID_LATITUDE, VALID_LONGITUDE, 500);

        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("client")).thenReturn(Arrays.asList(clientLocation));
        when(attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findRecentRecordsByUserIdAndType(eq(TEST_USER_ID), eq("in"),
                any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> {
            AttendanceRecord record = invocation.getArgument(0);
            record.setId(1L);
            return record;
        });

        // When
        AttendanceRecord result = attendanceService.clockIn(TEST_USER_ID, VALID_LATITUDE, VALID_LONGITUDE);

        // Then
        assertNotNull(result);
        assertEquals("in", result.getType());
    }

    // ========== 退勤打刻テスト ==========

    @Test
    void testClockOut_WithValidData_ShouldCreateRecordAndUpdateSummary() {
        // Given
        User user = createTestUser(TEST_USER_ID_LONG, "office", false);
        WorkLocation officeLocation = createWorkLocation("office", OFFICE_LATITUDE, OFFICE_LONGITUDE, 100);
        AttendanceRecord clockInRecord = createAttendanceRecord(1L, TEST_USER_ID, "in",
                OffsetDateTime.now().minusHours(8));

        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("office")).thenReturn(Arrays.asList(officeLocation));
        when(attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID))
                .thenReturn(Arrays.asList(clockInRecord));
        when(attendanceRecordRepository.findRecentRecordsByUserIdAndType(eq(TEST_USER_ID), eq("out"),
                any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> {
            AttendanceRecord record = invocation.getArgument(0);
            record.setId(2L);
            return record;
        });
        when(attendanceRecordRepository.findByUserIdAndDate(eq(TEST_USER_ID), any(LocalDate.class)))
                .thenReturn(Arrays.asList(clockInRecord,
                        createAttendanceRecord(2L, TEST_USER_ID, "out", OffsetDateTime.now())));
        when(attendanceSummaryRepository.findByUserIdAndTargetDate(eq(TEST_USER_ID), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // When
        AttendanceRecord result = attendanceService.clockOut(TEST_USER_ID, VALID_LATITUDE, VALID_LONGITUDE);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals("out", result.getType());
        assertEquals(VALID_LATITUDE, result.getLatitude());
        assertEquals(VALID_LONGITUDE, result.getLongitude());

        verify(attendanceRecordRepository).save(any(AttendanceRecord.class));
        verify(attendanceSummaryRepository).save(any(AttendanceSummary.class)); // サマリー更新の確認
    }

    @Test
    void testClockOut_WithoutClockIn_ShouldThrowException() {
        // Given
        User user = createTestUser(TEST_USER_ID_LONG, "office", false);
        WorkLocation officeLocation = createWorkLocation("office", OFFICE_LATITUDE, OFFICE_LONGITUDE, 100);

        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("office")).thenReturn(Arrays.asList(officeLocation));
        when(attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            attendanceService.clockOut(TEST_USER_ID, VALID_LATITUDE, VALID_LONGITUDE);
        });

        assertEquals("出勤打刻がありません", exception.getMessage());
    }

    @Test
    void testClockOut_WithAlreadyClockedOut_ShouldThrowException() {
        // Given
        User user = createTestUser(TEST_USER_ID_LONG, "office", false);
        WorkLocation officeLocation = createWorkLocation("office", OFFICE_LATITUDE, OFFICE_LONGITUDE, 100);
        AttendanceRecord clockInRecord = createAttendanceRecord(1L, TEST_USER_ID, "in",
                OffsetDateTime.now().minusHours(8));
        AttendanceRecord clockOutRecord = createAttendanceRecord(2L, TEST_USER_ID, "out",
                OffsetDateTime.now().minusHours(1));

        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("office")).thenReturn(Arrays.asList(officeLocation));
        when(attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID))
                .thenReturn(Arrays.asList(clockInRecord, clockOutRecord));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            attendanceService.clockOut(TEST_USER_ID, VALID_LATITUDE, VALID_LONGITUDE);
        });

        assertEquals("既に退勤打刻済みです", exception.getMessage());
    }

    // ========== リクエストオブジェクト版テスト ==========

    @Test
    void testClockInWithRequest_WithValidData_ShouldReturnSuccessResponse() {
        // Given
        User user = createTestUser(TEST_USER_ID_LONG, "office", false);
        WorkLocation officeLocation = createWorkLocation("office", OFFICE_LATITUDE, OFFICE_LONGITUDE, 100);
        ClockInRequest request = new ClockInRequest();
        request.setLatitude(VALID_LATITUDE);
        request.setLongitude(VALID_LONGITUDE);

        // 出勤記録を作成
        AttendanceRecord savedRecord = new AttendanceRecord();
        savedRecord.setId(1L);
        savedRecord.setUserId(TEST_USER_ID);
        savedRecord.setType("in");
        savedRecord.setTimestamp(OffsetDateTime.now());

        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("office")).thenReturn(Arrays.asList(officeLocation));
        when(attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID))
                .thenReturn(Collections.emptyList(), Arrays.asList(savedRecord));
        when(attendanceRecordRepository.findRecentRecordsByUserIdAndType(eq(TEST_USER_ID), eq("in"),
                any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> {
            AttendanceRecord record = invocation.getArgument(0);
            record.setId(1L);
            record.setType("in");
            record.setTimestamp(OffsetDateTime.now());
            return record;
        });

        // When
        ClockInResponse response = attendanceService.clockIn(request, TEST_USER_ID_LONG);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("出勤打刻が完了しました", response.getMessage());
        assertNotNull(response.getRecord());
        assertEquals("in", response.getStatus());
    }

    @Test
    void testClockInWithRequest_WithError_ShouldReturnErrorResponse() {
        // Given
        ClockInRequest request = new ClockInRequest();
        request.setLatitude(VALID_LATITUDE);
        request.setLongitude(VALID_LONGITUDE);

        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.empty());

        // When
        ClockInResponse response = attendanceService.clockIn(request, TEST_USER_ID_LONG);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("ユーザーが見つかりません: " + TEST_USER_ID, response.getMessage());
        assertNull(response.getRecord());
        assertNull(response.getStatus());
    }

    // ========== 勤怠状況取得テスト ==========

    @Test
    void testGetCurrentAttendanceStatus_WithNoRecords_ShouldReturnNone() {
        // Given
        when(attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());

        // When
        String status = attendanceService.getCurrentAttendanceStatus(TEST_USER_ID);

        // Then
        assertEquals("none", status);
    }

    @Test
    void testGetCurrentAttendanceStatus_WithClockInRecord_ShouldReturnIn() {
        // Given
        AttendanceRecord clockInRecord = createAttendanceRecord(1L, TEST_USER_ID, "in", OffsetDateTime.now());
        when(attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID))
                .thenReturn(Arrays.asList(clockInRecord));

        // When
        String status = attendanceService.getCurrentAttendanceStatus(TEST_USER_ID);

        // Then
        assertEquals("in", status);
    }

    @Test
    void testGetCurrentAttendanceStatus_WithClockOutRecord_ShouldReturnOut() {
        // Given
        AttendanceRecord clockInRecord = createAttendanceRecord(1L, TEST_USER_ID, "in",
                OffsetDateTime.now().minusHours(8));
        AttendanceRecord clockOutRecord = createAttendanceRecord(2L, TEST_USER_ID, "out", OffsetDateTime.now());
        when(attendanceRecordRepository.findTodayRecordsByUserId(TEST_USER_ID))
                .thenReturn(Arrays.asList(clockInRecord, clockOutRecord));

        // When
        String status = attendanceService.getCurrentAttendanceStatus(TEST_USER_ID);

        // Then
        assertEquals("out", status);
    }

    // ========== 日次サマリー取得テスト ==========

    @Test
    void testGetDailySummary_WithCompleteRecords_ShouldCalculateCorrectly() {
        // Given
        LocalDate testDate = LocalDate.now();
        OffsetDateTime clockInTime = OffsetDateTime.now().minusHours(9);
        OffsetDateTime clockOutTime = OffsetDateTime.now();

        AttendanceRecord clockInRecord = createAttendanceRecord(1L, TEST_USER_ID, "in", clockInTime);
        AttendanceRecord clockOutRecord = createAttendanceRecord(2L, TEST_USER_ID, "out", clockOutTime);

        when(attendanceRecordRepository.findByUserIdAndDate(TEST_USER_ID, testDate))
                .thenReturn(Arrays.asList(clockInRecord, clockOutRecord));

        // When
        DailySummaryData summary = attendanceService.getDailySummary(TEST_USER_ID_LONG, testDate);

        // Then
        assertNotNull(summary);
        assertEquals(testDate, summary.getDate());
        assertEquals(new BigDecimal("9.00"), summary.getTotalHours());
        assertEquals(new BigDecimal("1.00"), summary.getOvertimeHours()); // 8時間超過分
        assertEquals("completed", summary.getStatus());
        assertNotNull(summary.getClockInRecord());
        assertNotNull(summary.getClockOutRecord());
    }

    @Test
    void testGetDailySummary_WithOnlyClockIn_ShouldReturnInProgress() {
        // Given
        LocalDate testDate = LocalDate.now();
        AttendanceRecord clockInRecord = createAttendanceRecord(1L, TEST_USER_ID, "in",
                OffsetDateTime.now().minusHours(4));

        when(attendanceRecordRepository.findByUserIdAndDate(TEST_USER_ID, testDate))
                .thenReturn(Arrays.asList(clockInRecord));

        // When
        DailySummaryData summary = attendanceService.getDailySummary(TEST_USER_ID_LONG, testDate);

        // Then
        assertNotNull(summary);
        assertEquals("in_progress", summary.getStatus());
        assertEquals(BigDecimal.ZERO, summary.getTotalHours());
        assertEquals(BigDecimal.ZERO, summary.getOvertimeHours());
    }

    @Test
    void testGetDailySummary_WithNoRecords_ShouldReturnNone() {
        // Given
        LocalDate testDate = LocalDate.now();
        when(attendanceRecordRepository.findByUserIdAndDate(TEST_USER_ID, testDate))
                .thenReturn(Collections.emptyList());

        // When
        DailySummaryData summary = attendanceService.getDailySummary(TEST_USER_ID_LONG, testDate);

        // Then
        assertNotNull(summary);
        assertEquals("none", summary.getStatus());
        assertEquals(BigDecimal.ZERO, summary.getTotalHours());
        assertEquals(BigDecimal.ZERO, summary.getOvertimeHours());
    }

    // ========== 統計情報取得テスト ==========

    @Test
    void testGetTodayStatistics_ShouldReturnCorrectData() {
        // Given
        when(attendanceRecordRepository.countTodayRecords()).thenReturn(100L);
        when(attendanceRecordRepository.countTodayClockInUsers()).thenReturn(50L);

        // When
        var statistics = attendanceService.getTodayStatistics();

        // Then
        assertEquals(100L, statistics.get("totalRecords"));
        assertEquals(50L, statistics.get("clockedInUsers"));
        assertEquals(LocalDate.now(), statistics.get("date"));
    }

    // ========== 最新記録取得テスト ==========

    @Test
    void testGetLatestRecord_WithExistingRecord_ShouldReturnRecord() {
        // Given
        AttendanceRecord latestRecord = createAttendanceRecord(1L, TEST_USER_ID, "in", OffsetDateTime.now());
        when(attendanceRecordRepository.findTopByUserIdOrderByTimestampDesc(TEST_USER_ID))
                .thenReturn(Arrays.asList(latestRecord));

        // When
        Optional<AttendanceRecord> result = attendanceService.getLatestRecord(TEST_USER_ID_LONG);

        // Then
        assertTrue(result.isPresent());
        assertEquals(latestRecord.getId(), result.get().getId());
    }

    @Test
    void testGetLatestRecord_WithNoRecords_ShouldReturnEmpty() {
        // Given
        when(attendanceRecordRepository.findTopByUserIdOrderByTimestampDesc(TEST_USER_ID))
                .thenReturn(Collections.emptyList());

        // When
        Optional<AttendanceRecord> result = attendanceService.getLatestRecord(TEST_USER_ID_LONG);

        // Then
        assertFalse(result.isPresent());
    }

    // ========== 位置検証テスト ==========

    @Test
    void testClockIn_WithFarFromOffice_ShouldThrowException() {
        // Given
        User user = createTestUser(TEST_USER_ID_LONG, "office", false);
        WorkLocation officeLocation = createWorkLocation("office", OFFICE_LATITUDE, OFFICE_LONGITUDE, 100);

        // オフィスから200m離れた位置
        Double farLatitude = 35.6830; // 約200m北
        Double farLongitude = 139.7671;

        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("office")).thenReturn(Arrays.asList(officeLocation));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            attendanceService.clockIn(TEST_USER_ID, farLatitude, farLongitude);
        });

        assertEquals("オフィスから100m以上離れた場所での打刻はできません", exception.getMessage());
    }

    @Test
    void testClockIn_WithFarFromClient_ShouldThrowException() {
        // Given
        User user = createTestUser(TEST_USER_ID_LONG, "client", false);
        WorkLocation clientLocation = createWorkLocation("client", VALID_LATITUDE, VALID_LONGITUDE, 500);

        // 客先から600m離れた位置
        Double farLatitude = 35.6870; // 約600m北
        Double farLongitude = 139.7671;

        when(userRepository.findById(TEST_USER_ID_LONG)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("client")).thenReturn(Arrays.asList(clientLocation));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            attendanceService.clockIn(TEST_USER_ID, farLatitude, farLongitude);
        });

        assertEquals("指定された客先から500m以上離れた場所での打刻はできません", exception.getMessage());
    }

    // ========== ヘルパーメソッド ==========

    private User createTestUser(Long id, String locationType, boolean skipLocationCheck) {
        User user = new User();
        user.setId(id);
        user.setLocationType(locationType);
        user.setSkipLocationCheck(skipLocationCheck);
        return user;
    }

    private WorkLocation createWorkLocation(String type, Double latitude, Double longitude, Integer radius) {
        WorkLocation location = new WorkLocation();
        location.setType(type);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setRadius(radius);
        return location;
    }

    private AttendanceRecord createAttendanceRecord(Long id, Integer userId, String type, OffsetDateTime timestamp) {
        AttendanceRecord record = new AttendanceRecord();
        record.setId(id);
        record.setUserId(userId);
        record.setType(type);
        record.setTimestamp(timestamp);
        record.setLatitude(VALID_LATITUDE);
        record.setLongitude(VALID_LONGITUDE);
        return record;
    }
}