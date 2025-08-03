package com.example.companybackend.service;

import com.example.companybackend.dto.request.CreateTimeCorrectionRequest;
import com.example.companybackend.dto.response.ApproveTimeCorrectionResponse;
import com.example.companybackend.dto.response.CreateTimeCorrectionResponse;
import com.example.companybackend.dto.response.RejectTimeCorrectionResponse;
import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.entity.TimeCorrection;
import com.example.companybackend.entity.User;
import com.example.companybackend.repository.AttendanceRecordRepository;
import com.example.companybackend.repository.TimeCorrectionRepository;
import com.example.companybackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeCorrectionService テストクラス
 * 時刻修正申請サービスの包括的なテスト
 * comsys_test_dump.sqlの実データを活用
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TimeCorrectionServiceTest {

        @Autowired
        private TimeCorrectionService timeCorrectionService;

        @Autowired
        private TimeCorrectionRepository timeCorrectionRepository;

        @Autowired
        private AttendanceRecordRepository attendanceRecordRepository;

        @Autowired
        private UserRepository userRepository;

        private OffsetDateTime baseTime;
        private User testUser;
        private User approverUser;
        private AttendanceRecord testAttendanceRecord;

        @BeforeEach
        void setUp() {
                // 基準時刻を設定（JST）
                baseTime = OffsetDateTime.now(ZoneOffset.ofHours(9));

                // テスト用ユーザーの準備
                setupTestUsers();

                // テスト用打刻記録の準備
                setupTestAttendanceRecord();
        }

        private void setupTestUsers() {
                // 実データベースから既存のユーザーを取得
                List<User> allUsers = userRepository.findAll();
                if (allUsers.size() >= 2) {
                        testUser = allUsers.get(0);
                        approverUser = allUsers.get(1);
                } else {
                        // フォールバック: 新しいユーザーを作成（ユニークなusernameを使用）
                        String timestamp = String.valueOf(System.currentTimeMillis());

                        testUser = new User();
                        testUser.setUsername("test_user_" + timestamp);
                        testUser.setPasswordHash("$2a$10$test");
                        testUser.setLocationType("office");
                        testUser.setEmail("test_" + timestamp + "@company.com");
                        testUser.setIsActive(true);
                        testUser.setCreatedAt(baseTime.minusDays(30));
                        testUser.setUpdatedAt(baseTime.minusDays(30));
                        testUser = userRepository.save(testUser);

                        approverUser = new User();
                        approverUser.setUsername("approver_user_" + timestamp);
                        approverUser.setPasswordHash("$2a$10$approver");
                        approverUser.setLocationType("office");
                        approverUser.setEmail("approver_" + timestamp + "@company.com");
                        approverUser.setIsActive(true);
                        approverUser.setCreatedAt(baseTime.minusDays(60));
                        approverUser.setUpdatedAt(baseTime.minusDays(60));
                        approverUser = userRepository.save(approverUser);
                }
        }

        private void setupTestAttendanceRecord() {
                // テスト用打刻記録
                testAttendanceRecord = new AttendanceRecord();
                testAttendanceRecord.setUserId(testUser.getId().intValue());
                testAttendanceRecord.setType("in");
                testAttendanceRecord.setTimestamp(baseTime.minusHours(2));
                testAttendanceRecord.setLatitude(35.6812);
                testAttendanceRecord.setLongitude(139.7671);
                testAttendanceRecord.setCreatedAt(baseTime.minusHours(2));
                testAttendanceRecord = attendanceRecordRepository.save(testAttendanceRecord);
        }

        /**
         * テスト用修正申請リクエストを作成するヘルパーメソッド
         */
        private CreateTimeCorrectionRequest createTimeRequest(String requestType, String currentType,
                        OffsetDateTime requestedTime, String requestedType, String reason) {
                CreateTimeCorrectionRequest request = new CreateTimeCorrectionRequest();
                request.setAttendanceId(testAttendanceRecord.getId());
                request.setRequestType(requestType);
                request.setCurrentType(currentType);
                request.setRequestedTime(requestedTime);
                request.setRequestedType(requestedType);
                request.setReason(reason);
                return request;
        }

        // ========== 実データベース統合テスト ==========

        @Test
        void testCreateTimeCorrection_WithRealDatabaseData_ShouldWorkCorrectly() {
                // Given - 実データベースから打刻記録を取得
                List<AttendanceRecord> realRecords = attendanceRecordRepository.findAll();
                assertFalse(realRecords.isEmpty(), "実データベースに打刻記録が存在すること");

                // 最初の打刻記録を使用
                AttendanceRecord realRecord = realRecords.get(0);

                // 実データベースからユーザーを取得
                Optional<User> realUserOpt = userRepository.findById(realRecord.getUserId().longValue());
                if (realUserOpt.isPresent()) {
                        User realUser = realUserOpt.get();

                        CreateTimeCorrectionRequest request = new CreateTimeCorrectionRequest();
                        request.setAttendanceId(realRecord.getId());
                        request.setRequestType("time");
                        request.setCurrentType(realRecord.getType());
                        request.setRequestedTime(realRecord.getTimestamp().plusMinutes(30));
                        request.setReason("実データベーステスト用の修正申請");

                        // When
                        CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
                                        request, realUser.getId());

                        // Then
                        assertTrue(response.isSuccess(), "実データベースでの申請作成が成功すること");
                        assertNotNull(response.getTimeCorrection(), "作成された申請が返されること");
                        assertEquals("pending", response.getTimeCorrection().getStatus(), "申請ステータスがpendingであること");

                        System.out.println("実データベーステスト - ユーザーID: " + realUser.getId() +
                                        ", 打刻記録ID: " + realRecord.getId() +
                                        ", 申請ID: " + response.getTimeCorrection().getId());
                } else {
                        System.out.println("実データベースのユーザーが見つかりませんでした。テストをスキップします。");
                }
        }

        // ========== 申請作成テスト群 ==========

        @Test
        void testCreateTimeCorrection_TimeOnly_ShouldCreateSuccessfully() {
                // Given - 時刻のみ修正申請
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "time", "in", baseTime.minusHours(1), null, "出勤時刻を1時間早く修正");

                // When
                CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());

                // Then
                assertTrue(response.isSuccess(), "時刻修正申請が成功すること");
                assertNotNull(response.getTimeCorrection(), "作成された申請が返されること");
                assertEquals("time", response.getTimeCorrection().getRequestType(), "申請タイプが正しいこと");
                assertEquals("pending", response.getTimeCorrection().getStatus(), "ステータスがpendingであること");
                assertEquals(testUser.getId().intValue(), response.getTimeCorrection().getUserId(), "ユーザーIDが正しいこと");
                assertNotNull(response.getTimeCorrection().getRequestedTime(), "修正時刻が設定されていること");
        }

        @Test
        void testCreateTimeCorrection_TypeOnly_ShouldCreateSuccessfully() {
                // Given - タイプのみ修正申請
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "type", "in", null, "out", "出勤を退勤に修正");

                // When
                CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());

                // Then
                assertTrue(response.isSuccess(), "タイプ修正申請が成功すること");
                assertNotNull(response.getTimeCorrection(), "作成された申請が返されること");
                assertEquals("type", response.getTimeCorrection().getRequestType(), "申請タイプが正しいこと");
                assertEquals("out", response.getTimeCorrection().getRequestedType(), "修正タイプが正しいこと");
                assertNull(response.getTimeCorrection().getRequestedTime(), "修正時刻は設定されていないこと");
        }

        @Test
        void testCreateTimeCorrection_Both_ShouldCreateSuccessfully() {
                // Given - 時刻・タイプ両方修正申請
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "both", "in", baseTime.minusMinutes(30), "out", "出勤を退勤に変更し時刻も修正");

                // When
                CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());

                // Then
                assertTrue(response.isSuccess(), "時刻・タイプ修正申請が成功すること");
                assertNotNull(response.getTimeCorrection(), "作成された申請が返されること");
                assertEquals("both", response.getTimeCorrection().getRequestType(), "申請タイプが正しいこと");
                assertEquals("out", response.getTimeCorrection().getRequestedType(), "修正タイプが正しいこと");
                assertNotNull(response.getTimeCorrection().getRequestedTime(), "修正時刻が設定されていること");
        }

        // ========== バリデーションテスト群 ==========

        @Test
        void testCreateTimeCorrection_InvalidRequestType_ShouldFail() {
                // Given - 無効な申請タイプ
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "invalid", "in", baseTime, "out", "無効な申請タイプテスト");

                // When
                CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());

                // Then
                assertFalse(response.isSuccess(), "無効な申請タイプで失敗すること");
                assertTrue(response.getMessage().contains("無効な申請タイプです"), "適切なエラーメッセージが返されること");
        }

        @Test
        void testCreateTimeCorrection_MissingRequestedTime_ShouldFail() {
                // Given - 時刻修正で修正時刻が未指定
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "time", "in", null, null, "修正時刻未指定テスト");

                // When
                CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());

                // Then
                assertFalse(response.isSuccess(), "修正時刻未指定で失敗すること");
                assertTrue(response.getMessage().contains("修正時刻の指定が必要です"), "適切なエラーメッセージが返されること");
        }

        @Test
        void testCreateTimeCorrection_MissingRequestedType_ShouldFail() {
                // Given - タイプ修正で修正タイプが未指定
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "type", "in", null, null, "修正タイプ未指定テスト");

                // When
                CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());

                // Then
                assertFalse(response.isSuccess(), "修正タイプ未指定で失敗すること");
                assertTrue(response.getMessage().contains("修正タイプの指定が必要です"), "適切なエラーメッセージが返されること");
        }

        @Test
        void testCreateTimeCorrection_EmptyReason_ShouldFail() {
                // Given - 修正理由が空
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "time", "in", baseTime, null, "");

                // When
                CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());

                // Then
                assertFalse(response.isSuccess(), "修正理由が空で失敗すること");
                assertTrue(response.getMessage().contains("修正理由は必須です"), "適切なエラーメッセージが返されること");
        }

        @Test
        void testCreateTimeCorrection_NonExistentUser_ShouldFail() {
                // Given - 存在しないユーザーID
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "time", "in", baseTime, null, "存在しないユーザーテスト");

                // When
                CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
                                request, 99999L);

                // Then
                assertFalse(response.isSuccess(), "存在しないユーザーで失敗すること");
                assertTrue(response.getMessage().contains("ユーザーが見つかりません"), "適切なエラーメッセージが返されること");
        }

        @Test
        void testCreateTimeCorrection_NonExistentAttendanceRecord_ShouldFail() {
                // Given - 存在しない打刻記録ID
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "time", "in", baseTime, null, "存在しない打刻記録テスト");
                request.setAttendanceId(99999L);

                // When
                CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());

                // Then
                assertFalse(response.isSuccess(), "存在しない打刻記録で失敗すること");
                assertTrue(response.getMessage().contains("対象となる打刻記録が見つかりません"), "適切なエラーメッセージが返されること");
        }

        @Test
        void testCreateTimeCorrection_OtherUserAttendanceRecord_ShouldFail() {
                // Given - 他人の打刻記録を使用
                AttendanceRecord otherUserRecord = new AttendanceRecord();
                otherUserRecord.setUserId(approverUser.getId().intValue());
                otherUserRecord.setType("in");
                otherUserRecord.setTimestamp(baseTime);
                otherUserRecord.setLatitude(35.6812);
                otherUserRecord.setLongitude(139.7671);
                otherUserRecord.setCreatedAt(baseTime);
                otherUserRecord = attendanceRecordRepository.save(otherUserRecord);

                CreateTimeCorrectionRequest request = createTimeRequest(
                                "time", "in", baseTime, null, "他人の打刻記録テスト");
                request.setAttendanceId(otherUserRecord.getId());

                // When
                CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());

                // Then
                assertFalse(response.isSuccess(), "他人の打刻記録で失敗すること");
                assertTrue(response.getMessage().contains("他人の打刻記録は修正できません"), "適切なエラーメッセージが返されること");
        }

        // ========== 承認テスト群 ==========

        @Test
        void testApproveTimeCorrection_ValidRequest_ShouldApproveSuccessfully() {
                // Given - 承認待ちの申請を作成
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "time", "in", baseTime.minusMinutes(30), null, "承認テスト用申請");
                CreateTimeCorrectionResponse createResponse = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());
                assertTrue(createResponse.isSuccess(), "申請作成が成功すること");

                Long correctionId = createResponse.getTimeCorrection().getId();

                // When
                ApproveTimeCorrectionResponse response = timeCorrectionService.approveTimeCorrection(
                                correctionId, approverUser.getId());

                // Then
                assertTrue(response.isSuccess(), "申請承認が成功すること");
                assertNotNull(response.getCorrection(), "承認された申請が返されること");
                assertEquals("approved", response.getCorrection().getStatus(), "ステータスがapprovedに変更されること");
                assertEquals(approverUser.getId().intValue(), response.getCorrection().getApproverId(),
                                "承認者IDが設定されること");
                assertNotNull(response.getCorrection().getApprovedAt(), "承認日時が設定されること");
        }

        @Test
        void testApproveTimeCorrection_NonExistentRequest_ShouldFail() {
                // Given - 存在しない申請ID
                Long nonExistentId = 99999L;

                // When
                ApproveTimeCorrectionResponse response = timeCorrectionService.approveTimeCorrection(
                                nonExistentId, approverUser.getId());

                // Then
                assertFalse(response.isSuccess(), "存在しない申請で失敗すること");
                assertTrue(response.getMessage().contains("申請が見つかりません"), "適切なエラーメッセージが返されること");
        }

        @Test
        void testApproveTimeCorrection_AlreadyProcessed_ShouldFail() {
                // Given - 既に承認済みの申請
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "time", "in", baseTime.minusMinutes(30), null, "重複承認テスト用申請");
                CreateTimeCorrectionResponse createResponse = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());
                Long correctionId = createResponse.getTimeCorrection().getId();

                // 最初の承認
                timeCorrectionService.approveTimeCorrection(correctionId, approverUser.getId());

                // When - 2回目の承認試行
                ApproveTimeCorrectionResponse response = timeCorrectionService.approveTimeCorrection(
                                correctionId, approverUser.getId());

                // Then
                assertFalse(response.isSuccess(), "既に処理済みの申請で失敗すること");
                assertTrue(response.getMessage().contains("申請は既に処理済みです"), "適切なエラーメッセージが返されること");
        }

        // ========== 拒否テスト群 ==========

        @Test
        void testRejectTimeCorrection_ValidRequest_ShouldRejectSuccessfully() {
                // Given - 承認待ちの申請を作成
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "time", "in", baseTime.minusMinutes(30), null, "拒否テスト用申請");
                CreateTimeCorrectionResponse createResponse = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());
                Long correctionId = createResponse.getTimeCorrection().getId();

                // When
                RejectTimeCorrectionResponse response = timeCorrectionService.rejectTimeCorrection(
                                correctionId, approverUser.getId());

                // Then
                assertTrue(response.isSuccess(), "申請拒否が成功すること");
                assertNotNull(response.getCorrection(), "拒否された申請が返されること");
                assertEquals("rejected", response.getCorrection().getStatus(), "ステータスがrejectedに変更されること");
                assertEquals(approverUser.getId().intValue(), response.getCorrection().getApproverId(),
                                "承認者IDが設定されること");
                assertNotNull(response.getCorrection().getApprovedAt(), "処理日時が設定されること");
        }

        @Test
        void testRejectTimeCorrection_NonExistentRequest_ShouldFail() {
                // Given - 存在しない申請ID
                Long nonExistentId = 99999L;

                // When
                RejectTimeCorrectionResponse response = timeCorrectionService.rejectTimeCorrection(
                                nonExistentId, approverUser.getId());

                // Then
                assertFalse(response.isSuccess(), "存在しない申請で失敗すること");
                assertTrue(response.getMessage().contains("申請が見つかりません"), "適切なエラーメッセージが返されること");
        }

        // ========== 一覧取得テスト群 ==========

        @Test
        void testGetUserTimeCorrections_WithMultipleRequests_ShouldReturnAllUserRequests() {
                // Given - 複数の申請を作成
                CreateTimeCorrectionRequest request1 = createTimeRequest(
                                "time", "in", baseTime.minusMinutes(30), null, "申請1");
                CreateTimeCorrectionRequest request2 = createTimeRequest(
                                "type", "in", null, "out", "申請2");
                CreateTimeCorrectionRequest request3 = createTimeRequest(
                                "both", "out", baseTime.plusMinutes(15), "in", "申請3");

                timeCorrectionService.createTimeCorrection(request1, testUser.getId());
                timeCorrectionService.createTimeCorrection(request2, testUser.getId());
                timeCorrectionService.createTimeCorrection(request3, testUser.getId());

                // When
                List<TimeCorrection> userCorrections = timeCorrectionService.getUserTimeCorrections(testUser.getId());

                // Then
                assertEquals(3, userCorrections.size(), "ユーザーの全申請が取得されること");
                assertTrue(userCorrections.stream().allMatch(tc -> tc.getUserId().equals(testUser.getId().intValue())),
                                "全ての申請が対象ユーザーのものであること");
        }

        @Test
        void testGetPendingTimeCorrections_WithMixedStatuses_ShouldReturnOnlyPending() {
                // Given - 異なるステータスの申請を作成
                CreateTimeCorrectionRequest request1 = createTimeRequest(
                                "time", "in", baseTime.minusMinutes(30), null, "承認待ち申請");
                CreateTimeCorrectionRequest request2 = createTimeRequest(
                                "type", "in", null, "out", "承認予定申請");

                CreateTimeCorrectionResponse response1 = timeCorrectionService.createTimeCorrection(request1,
                                testUser.getId());
                CreateTimeCorrectionResponse response2 = timeCorrectionService.createTimeCorrection(request2,
                                testUser.getId());

                // 1つを承認
                timeCorrectionService.approveTimeCorrection(response1.getTimeCorrection().getId(),
                                approverUser.getId());

                // When
                List<TimeCorrection> pendingCorrections = timeCorrectionService.getPendingTimeCorrections();

                // Then
                assertEquals(1, pendingCorrections.size(), "承認待ちの申請のみが取得されること");
                assertEquals("pending", pendingCorrections.get(0).getStatus(), "ステータスがpendingであること");
                assertEquals(response2.getTimeCorrection().getId(), pendingCorrections.get(0).getId(), "正しい申請が取得されること");
        }

        @Test
        void testGetTimeCorrectionById_ExistingRequest_ShouldReturnCorrection() {
                // Given - 申請を作成
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "time", "in", baseTime.minusMinutes(30), null, "詳細取得テスト用申請");
                CreateTimeCorrectionResponse createResponse = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());
                Long correctionId = createResponse.getTimeCorrection().getId();

                // When
                Optional<TimeCorrection> result = timeCorrectionService.getTimeCorrectionById(correctionId);

                // Then
                assertTrue(result.isPresent(), "申請が取得されること");
                assertEquals(correctionId, result.get().getId(), "正しい申請が取得されること");
                assertEquals("time", result.get().getRequestType(), "申請タイプが正しいこと");
        }

        @Test
        void testGetTimeCorrectionById_NonExistentRequest_ShouldReturnEmpty() {
                // Given - 存在しない申請ID
                Long nonExistentId = 99999L;

                // When
                Optional<TimeCorrection> result = timeCorrectionService.getTimeCorrectionById(nonExistentId);

                // Then
                assertFalse(result.isPresent(), "存在しない申請でOptional.emptyが返されること");
        }

        // ========== カウント機能テスト群 ==========

        @Test
        void testGetUserPendingCount_WithMultipleStatuses_ShouldCountOnlyPending() {
                // Given - 異なるステータスの申請を作成
                CreateTimeCorrectionRequest request1 = createTimeRequest(
                                "time", "in", baseTime.minusMinutes(30), null, "承認待ち申請1");
                CreateTimeCorrectionRequest request2 = createTimeRequest(
                                "type", "in", null, "out", "承認待ち申請2");
                CreateTimeCorrectionRequest request3 = createTimeRequest(
                                "both", "out", baseTime.plusMinutes(15), "in", "承認予定申請");

                CreateTimeCorrectionResponse response1 = timeCorrectionService.createTimeCorrection(request1,
                                testUser.getId());
                timeCorrectionService.createTimeCorrection(request2, testUser.getId());
                CreateTimeCorrectionResponse response3 = timeCorrectionService.createTimeCorrection(request3,
                                testUser.getId());

                // 1つを承認
                timeCorrectionService.approveTimeCorrection(response1.getTimeCorrection().getId(),
                                approverUser.getId());

                // When
                long pendingCount = timeCorrectionService.getUserPendingCount(testUser.getId());

                // Then
                assertEquals(2, pendingCount, "ユーザーの承認待ち申請数が正しいこと");
        }

        @Test
        void testGetAllPendingCount_WithMultipleUsers_ShouldCountAllPending() {
                // Given - 複数ユーザーの申請を作成
                CreateTimeCorrectionRequest request1 = createTimeRequest(
                                "time", "in", baseTime.minusMinutes(30), null, "ユーザー1の申請");
                CreateTimeCorrectionRequest request2 = createTimeRequest(
                                "type", "in", null, "out", "ユーザー1の申請2");

                timeCorrectionService.createTimeCorrection(request1, testUser.getId());
                CreateTimeCorrectionResponse response2 = timeCorrectionService.createTimeCorrection(request2,
                                testUser.getId());

                // 承認者の申請も作成
                AttendanceRecord approverRecord = new AttendanceRecord();
                approverRecord.setUserId(approverUser.getId().intValue());
                approverRecord.setType("out");
                approverRecord.setTimestamp(baseTime);
                approverRecord.setLatitude(35.6812);
                approverRecord.setLongitude(139.7671);
                approverRecord.setCreatedAt(baseTime);
                approverRecord = attendanceRecordRepository.save(approverRecord);

                CreateTimeCorrectionRequest request3 = new CreateTimeCorrectionRequest();
                request3.setAttendanceId(approverRecord.getId());
                request3.setRequestType("time");
                request3.setCurrentType("out");
                request3.setRequestedTime(baseTime.plusMinutes(30));
                request3.setReason("承認者の申請");

                timeCorrectionService.createTimeCorrection(request3, approverUser.getId());

                // 1つを承認
                timeCorrectionService.approveTimeCorrection(response2.getTimeCorrection().getId(),
                                approverUser.getId());

                // When
                long allPendingCount = timeCorrectionService.getAllPendingCount();

                // Then
                assertEquals(2, allPendingCount, "全体の承認待ち申請数が正しいこと");
        }

        // ========== 複合シナリオテスト ==========

        @Test
        void testCompleteWorkflow_CreateApproveReject_ShouldWorkCorrectly() {
                // Given - 3つの申請を作成
                CreateTimeCorrectionRequest request1 = createTimeRequest(
                                "time", "in", baseTime.minusMinutes(30), null, "承認予定申請");
                CreateTimeCorrectionRequest request2 = createTimeRequest(
                                "type", "in", null, "out", "拒否予定申請");
                CreateTimeCorrectionRequest request3 = createTimeRequest(
                                "both", "out", baseTime.plusMinutes(15), "in", "承認待ち申請");

                CreateTimeCorrectionResponse response1 = timeCorrectionService.createTimeCorrection(request1,
                                testUser.getId());
                CreateTimeCorrectionResponse response2 = timeCorrectionService.createTimeCorrection(request2,
                                testUser.getId());
                CreateTimeCorrectionResponse response3 = timeCorrectionService.createTimeCorrection(request3,
                                testUser.getId());

                // When - 承認と拒否を実行
                ApproveTimeCorrectionResponse approveResponse = timeCorrectionService.approveTimeCorrection(
                                response1.getTimeCorrection().getId(), approverUser.getId());
                RejectTimeCorrectionResponse rejectResponse = timeCorrectionService.rejectTimeCorrection(
                                response2.getTimeCorrection().getId(), approverUser.getId());

                // Then - 各処理の結果確認
                assertTrue(approveResponse.isSuccess(), "承認が成功すること");
                assertTrue(rejectResponse.isSuccess(), "拒否が成功すること");

                // 最終状態確認
                List<TimeCorrection> userCorrections = timeCorrectionService.getUserTimeCorrections(testUser.getId());
                assertEquals(3, userCorrections.size(), "全申請が取得されること");

                long approvedCount = userCorrections.stream().mapToLong(tc -> "approved".equals(tc.getStatus()) ? 1 : 0)
                                .sum();
                long rejectedCount = userCorrections.stream().mapToLong(tc -> "rejected".equals(tc.getStatus()) ? 1 : 0)
                                .sum();
                long pendingCount = userCorrections.stream().mapToLong(tc -> "pending".equals(tc.getStatus()) ? 1 : 0)
                                .sum();

                assertEquals(1, approvedCount, "承認済み申請が1件であること");
                assertEquals(1, rejectedCount, "拒否済み申請が1件であること");
                assertEquals(1, pendingCount, "承認待ち申請が1件であること");
        }

        // ========== パフォーマンステスト ==========

        @Test
        void testCreateTimeCorrection_BulkOperations_ShouldPerformEfficiently() {
                // Given - 大量申請のシミュレーション
                int requestCount = 100;
                long startTime = System.currentTimeMillis();

                // When - 100件の申請を作成
                for (int i = 0; i < requestCount; i++) {
                        CreateTimeCorrectionRequest request = createTimeRequest(
                                        "time", "in", baseTime.minusMinutes(i), null, "パフォーマンステスト申請" + i);
                        CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(
                                        request, testUser.getId());
                        assertTrue(response.isSuccess(), "申請" + i + "が成功すること");
                }

                long endTime = System.currentTimeMillis();

                // Then
                assertTrue(endTime - startTime < 10000, "100件の申請作成が10秒以内で完了すること");

                // 作成された申請数の確認
                List<TimeCorrection> userCorrections = timeCorrectionService.getUserTimeCorrections(testUser.getId());
                assertEquals(requestCount, userCorrections.size(), "全申請が正しく作成されていること");
        }

        // ========== エラーハンドリングテスト ==========

        @Test
        void testCreateTimeCorrection_NullRequest_ShouldHandleGracefully() {
                // When & Then
                assertThrows(NullPointerException.class, () -> {
                        timeCorrectionService.createTimeCorrection(null, testUser.getId());
                }, "null申請でNullPointerExceptionが発生すること");
        }

        @Test
        void testApproveTimeCorrection_NullApproverId_ShouldFail() {
                // Given - 申請を作成
                CreateTimeCorrectionRequest request = createTimeRequest(
                                "time", "in", baseTime.minusMinutes(30), null, "null承認者テスト");
                CreateTimeCorrectionResponse createResponse = timeCorrectionService.createTimeCorrection(
                                request, testUser.getId());

                // When
                ApproveTimeCorrectionResponse response = timeCorrectionService.approveTimeCorrection(
                                createResponse.getTimeCorrection().getId(), null);

                // Then
                assertFalse(response.isSuccess(), "null承認者で失敗すること");
        }
}