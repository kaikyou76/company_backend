package com.example.companybackend.service;

import com.example.companybackend.entity.User;
import com.example.companybackend.repository.UserRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PaidLeaveCalculationService テストクラス
 * 有給休暇計算サービスの包括的なテスト
 * comsys_test_dump.sqlの実データを活用
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PaidLeaveCalculationServiceTest {

    @Autowired
    private PaidLeaveCalculationService paidLeaveCalculationService;

    @Autowired
    private UserRepository userRepository;

    private LocalDate baseDate;

    @BeforeEach
    void setUp() {
        // 基準日を設定（2025年8月2日 - 実データベースの作成日より後）
        baseDate = LocalDate.of(2025, 8, 2);
    }

    /**
     * テスト用Userを作成するヘルパーメソッド
     */
    private User createTestUser(Long id, String username, String email, OffsetDateTime createdAt) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setIsActive(true);
        user.setCreatedAt(createdAt);
        return user;
    }

    // ========== 実データベースユーザーテスト ==========

    @Test
    void testCalculatePaidLeaveDays_WithRealDatabaseUsers_ShouldCalculateCorrectly() {
        // Given - 実データベースからユーザーを取得
        List<User> allUsers = userRepository.findAll();
        assertFalse(allUsers.isEmpty(), "データベースにユーザーが存在すること");

        // 最初の数人のユーザーで有給計算をテスト
        for (int i = 0; i < Math.min(5, allUsers.size()); i++) {
            User user = allUsers.get(i);

            // When
            int paidLeaveDays = paidLeaveCalculationService.calculatePaidLeaveDays(user, baseDate);

            // Then - 実データベースのユーザーは2025年7月24日または8月1日作成なので、
            // 基準日2025年8月2日では全員6ヶ月未満（0日）になる
            assertEquals(0, paidLeaveDays,
                    "ユーザーID " + user.getId() + " は入社6ヶ月未満のため有給は0日");

            System.out.println("ユーザーID: " + user.getId() +
                    ", 作成日: " + user.getCreatedAt() +
                    ", 有給日数: " + paidLeaveDays + "日");
        }
    }

    // ========== 基本的な有給計算テスト（修正版） ==========

    @Test
    void testCalculatePaidLeaveDays_NewEmployee_ShouldReturn0Days() {
        // Given - 入社3ヶ月のユーザー（ChronoUnit.YEARSは0年を返す）
        OffsetDateTime hireDate = baseDate.minusMonths(3).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1000L, "test_new", "new@company.com", hireDate);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);

        // Then
        assertEquals(0, result, "入社6ヶ月未満の場合、有給は0日");
    }

    @Test
    void testCalculatePaidLeaveDays_OneYearEmployee_ShouldReturn10Days() {
        // Given - 入社1年のユーザー
        OffsetDateTime hireDate = baseDate.minusYears(1).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1001L, "test_one_year", "oneyear@company.com", hireDate);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);

        // Then
        assertEquals(10, result, "入社1年以上2年未満の場合、有給は10日");
    }

    @Test
    void testCalculatePaidLeaveDays_TwoYearsEmployee_ShouldReturn11Days() {
        // Given - 入社2年のユーザー
        OffsetDateTime hireDate = baseDate.minusYears(2).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1002L, "test_two_years", "twoyears@company.com", hireDate);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);

        // Then
        assertEquals(11, result, "入社2年以上3年未満の場合、有給は11日");
    }

    @Test
    void testCalculatePaidLeaveDays_ThreeYearsEmployee_ShouldReturn12Days() {
        // Given - 入社3年のユーザー
        OffsetDateTime hireDate = baseDate.minusYears(3).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1003L, "test_three_years", "threeyears@company.com", hireDate);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);

        // Then
        assertEquals(12, result, "入社3年以上4年未満の場合、有給は12日");
    }

    @Test
    void testCalculatePaidLeaveDays_FourYearsEmployee_ShouldReturn13Days() {
        // Given - 入社4年のユーザー
        OffsetDateTime hireDate = baseDate.minusYears(4).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1004L, "test_four_years", "fouryears@company.com", hireDate);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);

        // Then
        assertEquals(13, result, "入社4年以上5年未満の場合、有給は13日");
    }

    @Test
    void testCalculatePaidLeaveDays_FiveYearsEmployee_ShouldReturn14Days() {
        // Given - 入社5年のユーザー
        OffsetDateTime hireDate = baseDate.minusYears(5).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1005L, "test_five_years", "fiveyears@company.com", hireDate);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);

        // Then
        assertEquals(14, result, "入社5年以上6年未満の場合、有給は14日");
    }

    @Test
    void testCalculatePaidLeaveDays_SixYearsEmployee_ShouldReturn15Days() {
        // Given - 入社6年のユーザー
        OffsetDateTime hireDate = baseDate.minusYears(6).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1006L, "test_six_years", "sixyears@company.com", hireDate);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);

        // Then
        assertEquals(15, result, "入社6年以上の場合、有給は15日（上限）");
    }

    @Test
    void testCalculatePaidLeaveDays_TenYearsEmployee_ShouldReturn15Days() {
        // Given - 入社10年のユーザー（上限確認）
        OffsetDateTime hireDate = baseDate.minusYears(10).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1007L, "test_ten_years", "tenyears@company.com", hireDate);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);

        // Then
        assertEquals(15, result, "入社10年の場合でも有給は15日（上限）");
    }

    // ========== 境界値テスト（修正版） ==========

    @Test
    void testCalculatePaidLeaveDays_ExactlyOneYear_ShouldReturn10Days() {
        // Given - 入社ちょうど1年のユーザー
        OffsetDateTime hireDate = baseDate.minusYears(1).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1008L, "test_exact_one_year", "exactoneyear@company.com", hireDate);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);

        // Then
        assertEquals(10, result, "入社ちょうど1年の場合、有給は10日");
    }

    @Test
    void testCalculatePaidLeaveDays_ExactlyTwoYears_ShouldReturn11Days() {
        // Given - 入社ちょうど2年のユーザー
        OffsetDateTime hireDate = baseDate.minusYears(2).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1009L, "test_exact_two_years", "exacttwoyears@company.com", hireDate);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);

        // Then
        assertEquals(11, result, "入社ちょうど2年の場合、有給は11日");
    }

    // ========== 特殊ケーステスト ==========

    @Test
    void testCalculatePaidLeaveDays_FutureHireDate_ShouldReturn0Days() {
        // Given - 未来の入社日のユーザー（異常ケース）
        OffsetDateTime futureHireDate = baseDate.plusMonths(1).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1010L, "test_future", "future@company.com", futureHireDate);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);

        // Then
        assertEquals(0, result, "未来の入社日の場合、有給は0日");
    }

    @Test
    void testCalculatePaidLeaveDays_SameDayHire_ShouldReturn0Days() {
        // Given - 当日入社のユーザー
        OffsetDateTime sameDayHire = baseDate.atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1011L, "test_same_day", "sameday@company.com", sameDayHire);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);

        // Then
        assertEquals(0, result, "当日入社の場合、有給は0日");
    }

    // ========== 複数ユーザーテスト ==========

    @Test
    void testCalculatePaidLeaveDays_MultipleUsers_ShouldReturnCorrectDays() {
        // Given - 異なる勤続年数の複数ユーザー
        User newUser = createTestUser(1012L, "new_user", "new@company.com",
                baseDate.minusMonths(3).atStartOfDay().atOffset(ZoneOffset.ofHours(9))); // 3ヶ月

        User experiencedUser = createTestUser(1013L, "experienced_user", "exp@company.com",
                baseDate.minusYears(3).atStartOfDay().atOffset(ZoneOffset.ofHours(9))); // 3年

        User veteranUser = createTestUser(1014L, "veteran_user", "vet@company.com",
                baseDate.minusYears(8).atStartOfDay().atOffset(ZoneOffset.ofHours(9))); // 8年

        // When
        int newUserDays = paidLeaveCalculationService.calculatePaidLeaveDays(newUser, baseDate);
        int experiencedUserDays = paidLeaveCalculationService.calculatePaidLeaveDays(experiencedUser, baseDate);
        int veteranUserDays = paidLeaveCalculationService.calculatePaidLeaveDays(veteranUser, baseDate);

        // Then
        assertEquals(0, newUserDays, "新入社員（3ヶ月）の有給は0日");
        assertEquals(12, experiencedUserDays, "経験者（3年）の有給は12日");
        assertEquals(15, veteranUserDays, "ベテラン（8年）の有給は15日");
    }

    // ========== 日付計算精度テスト ==========

    @Test
    void testCalculatePaidLeaveDays_LeapYearCalculation_ShouldBeAccurate() {
        // Given - うるう年を含む期間での計算
        LocalDate leapYearDate = LocalDate.of(2024, 2, 29); // うるう年の2月29日
        OffsetDateTime hireDate = LocalDate.of(2022, 2, 28).atStartOfDay().atOffset(ZoneOffset.ofHours(9)); // 2年前
        User testUser = createTestUser(1015L, "leap_year_user", "leap@company.com", hireDate);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, leapYearDate);

        // Then
        assertEquals(11, result, "うるう年を含む2年間の勤続で有給は11日");
    }

    @Test
    void testCalculatePaidLeaveDays_EndOfMonth_ShouldBeAccurate() {
        // Given - 月末日での計算
        LocalDate endOfMonth = LocalDate.of(2025, 1, 31);
        OffsetDateTime hireDate = LocalDate.of(2023, 1, 31).atStartOfDay().atOffset(ZoneOffset.ofHours(9)); // 2年前の同日
        User testUser = createTestUser(1016L, "end_month_user", "endmonth@company.com", hireDate);

        // When
        int result = paidLeaveCalculationService.calculatePaidLeaveDays(testUser, endOfMonth);

        // Then
        assertEquals(11, result, "月末日での2年間勤続で有給は11日");
    }

    // ========== パフォーマンステスト ==========

    @Test
    void testCalculatePaidLeaveDays_Performance_ShouldBeEfficient() {
        // Given - パフォーマンステスト用ユーザー
        OffsetDateTime hireDate = baseDate.minusYears(5).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1017L, "performance_user", "perf@company.com", hireDate);

        // When - 1000回計算を実行
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);
        }
        long endTime = System.currentTimeMillis();

        // Then
        assertTrue(endTime - startTime < 1000, "1000回の計算が1秒以内で完了すること");
    }

    // ========== エラーハンドリングテスト ==========

    @Test
    void testCalculatePaidLeaveDays_NullUser_ShouldThrowException() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            paidLeaveCalculationService.calculatePaidLeaveDays(null, baseDate);
        }, "nullユーザーの場合、NullPointerExceptionが発生すること");
    }

    @Test
    void testCalculatePaidLeaveDays_NullTargetDate_ShouldThrowException() {
        // Given
        OffsetDateTime hireDate = baseDate.minusYears(2).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1018L, "null_date_user", "nulldate@company.com", hireDate);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            paidLeaveCalculationService.calculatePaidLeaveDays(testUser, null);
        }, "null基準日の場合、NullPointerExceptionが発生すること");
    }

    @Test
    void testCalculatePaidLeaveDays_NullCreatedAt_ShouldThrowException() {
        // Given - createdAtがnullのユーザー
        User testUser = createTestUser(1019L, "null_created_user", "nullcreated@company.com", null);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            paidLeaveCalculationService.calculatePaidLeaveDays(testUser, baseDate);
        }, "null入社日の場合、NullPointerExceptionが発生すること");
    }

    // ========== 実用的なシナリオテスト ==========

    @Test
    void testCalculatePaidLeaveDays_TypicalCareerProgression_ShouldShowProgression() {
        // Given - 典型的なキャリア進行のシミュレーション
        OffsetDateTime initialHireDate = LocalDate.of(2020, 4, 1).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        User testUser = createTestUser(1020L, "career_user", "career@company.com", initialHireDate);

        // When & Then - 各年での有給日数を確認
        // 入社1年目（2021年4月）
        LocalDate firstYear = LocalDate.of(2021, 4, 1);
        assertEquals(10, paidLeaveCalculationService.calculatePaidLeaveDays(testUser, firstYear));

        // 入社2年目（2022年4月）
        LocalDate secondYear = LocalDate.of(2022, 4, 1);
        assertEquals(11, paidLeaveCalculationService.calculatePaidLeaveDays(testUser, secondYear));

        // 入社3年目（2023年4月）
        LocalDate thirdYear = LocalDate.of(2023, 4, 1);
        assertEquals(12, paidLeaveCalculationService.calculatePaidLeaveDays(testUser, thirdYear));

        // 入社4年目（2024年4月）
        LocalDate fourthYear = LocalDate.of(2024, 4, 1);
        assertEquals(13, paidLeaveCalculationService.calculatePaidLeaveDays(testUser, fourthYear));

        // 入社5年目（2025年4月）
        LocalDate fifthYear = LocalDate.of(2025, 4, 1);
        assertEquals(14, paidLeaveCalculationService.calculatePaidLeaveDays(testUser, fifthYear));

        // 入社6年目（2026年4月）
        LocalDate sixthYear = LocalDate.of(2026, 4, 1);
        assertEquals(15, paidLeaveCalculationService.calculatePaidLeaveDays(testUser, sixthYear));
    }

    // ========== 実データベースとの統合テスト ==========

    @Test
    void testCalculatePaidLeaveDays_WithDatabaseIntegration_ShouldWorkWithRealData() {
        // Given - 実データベースから特定のユーザーを取得
        Optional<User> userOpt = userRepository.findById(1L);

        if (userOpt.isPresent()) {
            User realUser = userOpt.get();

            // When
            int paidLeaveDays = paidLeaveCalculationService.calculatePaidLeaveDays(realUser, baseDate);

            // Then - 実データベースのユーザーは2025年7月24日作成なので、
            // 基準日2025年8月2日では6ヶ月未満（0日）になる
            assertEquals(0, paidLeaveDays,
                    "実データベースユーザーは入社6ヶ月未満のため有給は0日");

            System.out.println("実データベースユーザー - ID: " + realUser.getId() +
                    ", 作成日: " + realUser.getCreatedAt() +
                    ", 有給日数: " + paidLeaveDays + "日");
        } else {
            System.out.println("ID=1のユーザーが見つかりませんでした。テストをスキップします。");
        }
    }

    @Test
    void testCalculatePaidLeaveDays_WithFutureDate_ShouldCalculateCorrectly() {
        // Given - 実データベースから特定のユーザーを取得し、未来の基準日でテスト
        Optional<User> userOpt = userRepository.findById(1L);

        if (userOpt.isPresent()) {
            User realUser = userOpt.get();
            LocalDate futureDate = LocalDate.of(2026, 8, 1); // 1年後

            // When
            int paidLeaveDays = paidLeaveCalculationService.calculatePaidLeaveDays(realUser, futureDate);

            // Then - 1年後なら10日の有給が付与される
            assertEquals(10, paidLeaveDays,
                    "実データベースユーザーの1年後の有給は10日");

            System.out.println("実データベースユーザー（1年後） - ID: " + realUser.getId() +
                    ", 作成日: " + realUser.getCreatedAt() +
                    ", 基準日: " + futureDate +
                    ", 有給日数: " + paidLeaveDays + "日");
        } else {
            System.out.println("ID=1のユーザーが見つかりませんでした。テストをスキップします。");
        }
    }
}