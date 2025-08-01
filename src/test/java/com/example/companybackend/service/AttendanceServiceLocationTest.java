package com.example.companybackend.service;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.entity.User;
import com.example.companybackend.entity.WorkLocation;
import com.example.companybackend.repository.AttendanceRecordRepository;
import com.example.companybackend.repository.UserRepository;
import com.example.companybackend.repository.WorkLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class AttendanceServiceLocationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private WorkLocationRepository workLocationRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testOfficeLocationValidation_Success() {
        // テストデータの準備
        User user = new User();
        user.setId(1L);
        user.setLocationType("office");
        user.setSkipLocationCheck(false);

        WorkLocation officeLocation = new WorkLocation();
        officeLocation.setId(1L);
        officeLocation.setType("office");
        officeLocation.setLatitude(35.6812);
        officeLocation.setLongitude(139.7671);
        officeLocation.setRadius(100);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("office")).thenReturn(Arrays.asList(officeLocation));

        // テスト実行と検証
        assertDoesNotThrow(() -> {
            attendanceService.clockIn(1, 35.6812, 139.7671);
        });
    }

    @Test
    void testOfficeLocationValidation_OutOfRange() {
        // テストデータの準備
        User user = new User();
        user.setId(1L);
        user.setLocationType("office");
        user.setSkipLocationCheck(false);

        WorkLocation officeLocation = new WorkLocation();
        officeLocation.setId(1L);
        officeLocation.setType("office");
        officeLocation.setLatitude(35.6812);
        officeLocation.setLongitude(139.7671);
        officeLocation.setRadius(100); // 100m以内

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("office")).thenReturn(Arrays.asList(officeLocation));

        // テスト実行と検証 (200m離れた位置)
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            attendanceService.clockIn(1, 35.6800, 139.7650);
        });

        assertEquals("オフィスから100m以上離れた場所での打刻はできません", exception.getMessage());
    }

    @Test
    void testClientLocationValidation_Success() {
        // テストデータの準備
        User user = new User();
        user.setId(1L);
        user.setLocationType("client");
        user.setSkipLocationCheck(false);

        WorkLocation clientLocation = new WorkLocation();
        clientLocation.setId(1L);
        clientLocation.setType("client");
        clientLocation.setLatitude(35.6762);
        clientLocation.setLongitude(139.6503);
        clientLocation.setRadius(500); // 500m以内

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("client")).thenReturn(Arrays.asList(clientLocation));

        // テスト実行と検証
        assertDoesNotThrow(() -> {
            attendanceService.clockIn(1, 35.6762, 139.6503);
        });
    }

    @Test
    void testClientLocationValidation_OutOfRange() {
        // テストデータの準備
        User user = new User();
        user.setId(1L);
        user.setLocationType("client");
        user.setSkipLocationCheck(false);

        WorkLocation clientLocation = new WorkLocation();
        clientLocation.setId(1L);
        clientLocation.setType("client");
        clientLocation.setLatitude(35.6762);
        clientLocation.setLongitude(139.6503);
        clientLocation.setRadius(500); // 500m以内

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workLocationRepository.findByType("client")).thenReturn(Arrays.asList(clientLocation));

        // テスト実行と検証 (600m離れた位置)
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            attendanceService.clockIn(1, 35.6800, 139.6500);
        });

        assertEquals("指定された客先から500m以上離れた場所での打刻はできません", exception.getMessage());
    }

    @Test
    void testSkipLocationCheck() {
        // テストデータの準備
        User user = new User();
        user.setId(1L);
        user.setLocationType("office");
        user.setSkipLocationCheck(true); // 位置チェックをスキップ

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // テスト実行と検証
        assertDoesNotThrow(() -> {
            attendanceService.clockIn(1, 35.0000, 139.0000); // 任意の位置
        });
    }

    @Test
    void testDuplicateClockInCheck() {
        // テストデータの準備
        User user = new User();
        user.setId(1L);
        user.setLocationType("office");
        user.setSkipLocationCheck(false);

        AttendanceRecord existingRecord = new AttendanceRecord();
        existingRecord.setId(1L);
        existingRecord.setUserId(1);
        existingRecord.setType("in");
        existingRecord.setTimestamp(OffsetDateTime.now().minusHours(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRecordRepository.findTodayRecordsByUserId(1)).thenReturn(Arrays.asList(existingRecord));

        WorkLocation officeLocation = new WorkLocation();
        officeLocation.setId(1L);
        officeLocation.setType("office");
        officeLocation.setLatitude(35.6812);
        officeLocation.setLongitude(139.7671);
        officeLocation.setRadius(100);

        when(workLocationRepository.findByType("office")).thenReturn(Arrays.asList(officeLocation));

        // テスト実行と検証
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            attendanceService.clockIn(1, 35.6812, 139.7671);
        });

        assertEquals("既に出勤打刻済みです", exception.getMessage());
    }

    @Test
    void testDuplicateClockOutCheck() {
        // テストデータの準備
        User user = new User();
        user.setId(1L);
        user.setLocationType("office");
        user.setSkipLocationCheck(false);

        AttendanceRecord inRecord = new AttendanceRecord();
        inRecord.setId(1L);
        inRecord.setUserId(1);
        inRecord.setType("in");
        inRecord.setTimestamp(OffsetDateTime.now().minusHours(8));

        AttendanceRecord outRecord = new AttendanceRecord();
        outRecord.setId(2L);
        outRecord.setUserId(1);
        outRecord.setType("out");
        outRecord.setTimestamp(OffsetDateTime.now().minusHours(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRecordRepository.findTodayRecordsByUserId(1)).thenReturn(Arrays.asList(inRecord, outRecord));

        WorkLocation officeLocation = new WorkLocation();
        officeLocation.setId(1L);
        officeLocation.setType("office");
        officeLocation.setLatitude(35.6812);
        officeLocation.setLongitude(139.7671);
        officeLocation.setRadius(100);

        when(workLocationRepository.findByType("office")).thenReturn(Arrays.asList(officeLocation));

        // テスト実行と検証
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            attendanceService.clockOut(1, 35.6812, 139.7671);
        });

        assertEquals("既に退勤打刻済みです", exception.getMessage());
    }
}