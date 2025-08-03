package com.example.companybackend.repository;

import com.example.companybackend.entity.Holiday;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 祝日リポジトリテストクラス
 * 
 * テスト対象：
 * - テストファイル：HolidayRepository.java
 * - テストクラス：com.example.companybackend.repository.HolidayRepository
 * 
 * テスト規約とテクニック：
 * 1. @DataJpaTestを使用してJPA層のみをテスト
 * 2. TestEntityManagerを使用してテストデータを管理
 * 3. 各テストメソッドは独立して実行可能
 * 4. Given-When-Thenパターンに従う
 * 5. 重要な戻り値と副作用を検証
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class HolidayRepositoryTest {

    /**
     * HolidayRepositoryのテスト対象オブジェクト
     * 実際のデータベース操作をテストするために使用
     */
    @Autowired
    private HolidayRepository holidayRepository;

    /**
     * TestEntityManager
     * テストデータのセットアップと検証に使用
     */
    @Autowired
    private TestEntityManager entityManager;

    /**
     * テスト前処理：テストデータクリーンアップ
     */
    @BeforeEach
    void setUp() {
        // テスト用日付範囲のデータを削除（2023年から2027年まで）
        entityManager.getEntityManager().createNativeQuery(
            "DELETE FROM holidays WHERE EXTRACT(YEAR FROM date) BETWEEN 2023 AND 2027"
        ).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * テスト後処理：テストデータクリーンアップ
     */
    @AfterEach
    void tearDown() {
        // テスト用日付範囲のデータを削除（2023年から2027年まで）
        entityManager.getEntityManager().createNativeQuery(
            "DELETE FROM holidays WHERE EXTRACT(YEAR FROM date) BETWEEN 2023 AND 2027"
        ).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * テストデータ作成ヘルパーメソッド
     */
    private Holiday createTestHoliday(LocalDate date, String name) {
        Holiday holiday = new Holiday();
        holiday.setDate(date);
        holiday.setName(name);
        holiday.setIsRecurring(false);
        holiday.setCreatedAt(OffsetDateTime.now());
        return holiday;
    }

    /**
     * テストケース：日付による祝日検索の成功
     * 
     * テスト対象メソッド：
     * - HolidayRepository.findByDate()
     * 
     * テストシナリオ：
     * - 存在する日付で祝日を検索
     * - データベースに該当する祝日が存在する
     * 
     * 期待結果：
     * - 該当する祝日がOptionalで返される
     */
    @Test
    void testFindByDate_Exists() {
        // Given
        LocalDate testDate = LocalDate.of(2023, 1, 1);
        Holiday holiday = createTestHoliday(testDate, "テスト元日");
        entityManager.persistAndFlush(holiday);

        // When
        Optional<Holiday> result = holidayRepository.findByDate(testDate);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getDate()).isEqualTo(testDate);
        assertThat(result.get().getName()).isEqualTo("テスト元日");
    }

    /**
     * テストケース：日付による祝日検索の失敗（該当なし）
     * 
     * テスト対象メソッド：
     * - HolidayRepository.findByDate()
     * 
     * テストシナリオ：
     * - 存在しない日付で祝日を検索
     * - データベースに該当する祝日が存在しない
     * 
     * 期待結果：
     * - 空のOptionalが返される
     */
    @Test
    void testFindByDate_NotExists() {
        // Given
        LocalDate testDate = LocalDate.of(2023, 1, 2);

        // When
        Optional<Holiday> result = holidayRepository.findByDate(testDate);

        // Then
        assertThat(result).isNotPresent();
    }

    /**
     * テストケース：祝日名による検索の成功
     * 
     * テスト対象メソッド：
     * - HolidayRepository.findByName()
     * 
     * テストシナリオ：
     * - 存在する祝日名で祝日を検索
     * - データベースに該当する祝日が存在する
     * 
     * 期待結果：
     * - 該当する祝日がOptionalで返される
     */
    @Test
    void testFindByName_Exists() {
        // Given
        String testName = "テスト建国記念の日";
        Holiday holiday = createTestHoliday(LocalDate.of(2023, 2, 11), testName);
        entityManager.persistAndFlush(holiday);

        // When
        Optional<Holiday> result = holidayRepository.findByName(testName);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(testName);
        assertThat(result.get().getDate()).isEqualTo(LocalDate.of(2023, 2, 11));
    }

    /**
     * テストケース：祝日名による検索の失敗（該当なし）
     * 
     * テスト対象メソッド：
     * - HolidayRepository.findByName()
     * 
     * テストシナリオ：
     * - 存在しない祝日名で祝日を検索
     * - データベースに該当する祝日が存在しない
     * 
     * 期待結果：
     * - 空のOptionalが返される
     */
    @Test
    void testFindByName_NotExists() {
        // Given
        String testName = "テスト存在しない祝日";

        // When
        Optional<Holiday> result = holidayRepository.findByName(testName);

        // Then
        assertThat(result).isNotPresent();
    }

    /**
     * テストケース：日付による祝日存在確認（存在する場合）
     * 
     * テスト対象メソッド：
     * - HolidayRepository.existsByDate()
     * 
     * テストシナリオ：
     * - 存在する日付で祝日の存在を確認
     * - データベースに該当する祝日が存在する
     * 
     * 期待結果：
     * - trueが返される
     */
    @Test
    void testExistsByDate_Exists() {
        // Given
        LocalDate testDate = LocalDate.of(2023, 1, 3);
        Holiday holiday = createTestHoliday(testDate, "テスト元日");
        entityManager.persistAndFlush(holiday);

        // When
        boolean result = holidayRepository.existsByDate(testDate);

        // Then
        assertThat(result).isTrue();
    }

    /**
     * テストケース：日付による祝日存在確認（存在しない場合）
     * 
     * テスト対象メソッド：
     * - HolidayRepository.existsByDate()
     * 
     * テストシナリオ：
     * - 存在しない日付で祝日の存在を確認
     * - データベースに該当する祝日が存在しない
     * 
     * 期待結果：
     * - falseが返される
     */
    @Test
    void testExistsByDate_NotExists() {
        // Given
        LocalDate testDate = LocalDate.of(2023, 1, 4);

        // When
        boolean result = holidayRepository.existsByDate(testDate);

        // Then
        assertThat(result).isFalse();
    }

    /**
     * テストケース：年別祝日取得
     * 
     * テスト対象メソッド：
     * - HolidayRepository.findByYear()
     * 
     * テストシナリオ：
     * - 特定の年における祝日を取得
     * - データベースに複数の祝日が存在する
     * 
     * 期待結果：
     * - 指定年の祝日リストが日付順に返される
     */
    @Test
    void testFindByYear() {
        // Given
        int testYear = 2023;
        
        // 2023年の祝日をいくつか作成
        Holiday holiday1 = createTestHoliday(LocalDate.of(2023, 1, 1), "テスト元日");
        Holiday holiday2 = createTestHoliday(LocalDate.of(2023, 2, 11), "テスト建国記念の日");
        Holiday holiday3 = createTestHoliday(LocalDate.of(2023, 12, 25), "テストクリスマス");
        
        // 異なる年の祝日（これは結果に含まれるべきではない）
        Holiday holiday4 = createTestHoliday(LocalDate.of(2024, 1, 1), "テスト元日");
        
        entityManager.persist(holiday1);
        entityManager.persist(holiday2);
        entityManager.persist(holiday3);
        entityManager.persist(holiday4);
        entityManager.flush();

        // When
        List<Holiday> result = holidayRepository.findByYear(testYear);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Holiday::getDate)
                .containsExactly(
                    LocalDate.of(2023, 1, 1),
                    LocalDate.of(2023, 2, 11),
                    LocalDate.of(2023, 12, 25)
                );
    }

    /**
     * テストケース：月別祝日取得
     * 
     * テスト対象メソッド：
     * - HolidayRepository.findByYearAndMonth()
     * 
     * テストシナリオ：
     * - 特定の年月における祝日を取得
     * - データベースに複数の祝日が存在する
     * 
     * 期待結果：
     * - 指定年月の祝日リストが日付順に返される
     */
    @Test
    void testFindByYearAndMonth() {
        // Given
        int testYear = 2023;
        int testMonth = 1;
        
        // 2023年1月の祝日をいくつか作成
        Holiday holiday1 = createTestHoliday(LocalDate.of(2023, 1, 1), "テスト元日");
        Holiday holiday2 = createTestHoliday(LocalDate.of(2023, 1, 15), "テスト成人の日");
        
        // 異なる月の祝日（これは結果に含まれるべきではない）
        Holiday holiday3 = createTestHoliday(LocalDate.of(2023, 2, 11), "テスト建国記念の日");
        
        entityManager.persist(holiday1);
        entityManager.persist(holiday2);
        entityManager.persist(holiday3);
        entityManager.flush();

        // When
        List<Holiday> result = holidayRepository.findByYearAndMonth(testYear, testMonth);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Holiday::getDate)
                .containsExactly(
                    LocalDate.of(2023, 1, 1),
                    LocalDate.of(2023, 1, 15)
                );
    }

    /**
     * テストケース：期間内祝日取得
     * 
     * テスト対象メソッド：
     * - HolidayRepository.findByDateRange()
     * 
     * テストシナリオ：
     * - 特定の期間内における祝日を取得
     * - データベースに複数の祝日が存在する
     * 
     * 期待結果：
     * - 指定期間内の祝日リストが日付順に返される
     */
    @Test
    void testFindByDateRange() {
        // Given
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 3, 31);
        
        // 期間内の祝日
        Holiday holiday1 = createTestHoliday(LocalDate.of(2023, 1, 1), "テスト元日");
        Holiday holiday2 = createTestHoliday(LocalDate.of(2023, 2, 11), "テスト建国記念の日");
        
        // 期間外の祝日（これは結果に含まれるべきではない）
        Holiday holiday3 = createTestHoliday(LocalDate.of(2023, 4, 1), "テストエイプリルフール");
        
        entityManager.persist(holiday1);
        entityManager.persist(holiday2);
        entityManager.persist(holiday3);
        entityManager.flush();

        // When
        List<Holiday> result = holidayRepository.findByDateRange(startDate, endDate);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Holiday::getDate)
                .containsExactly(
                    LocalDate.of(2023, 1, 1),
                    LocalDate.of(2023, 2, 11)
                );
    }

    /**
     * テストケース：祝日名部分一致検索
     * 
     * テスト対象メソッド：
     * - HolidayRepository.findByNameContaining()
     * 
     * テストシナリオ：
     * - 祝日名の部分一致で祝日を検索
     * - データベースに複数の祝日が存在する
     * 
     * 期待結果：
     * - 名前に指定文字列を含む祝日リストが日付順に返される
     */
    @Test
    void testFindByNameContaining() {
        // Given
        String pattern = "テスト";
        
        // パターンに一致する祝日
        Holiday holiday1 = createTestHoliday(LocalDate.of(2023, 1, 1), "テスト元日");
        Holiday holiday2 = createTestHoliday(LocalDate.of(2023, 2, 11), "テスト建国記念の日");
        Holiday holiday3 = createTestHoliday(LocalDate.of(2023, 7, 20), "テスト海の日");
        
        // パターンに一致しない祝日
        Holiday holiday4 = createTestHoliday(LocalDate.of(2023, 12, 25), "クリスマス");
        
        entityManager.persist(holiday1);
        entityManager.persist(holiday2);
        entityManager.persist(holiday3);
        entityManager.persist(holiday4);
        entityManager.flush();

        // When
        List<Holiday> result = holidayRepository.findByNameContaining(pattern);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Holiday::getName)
                .contains("テスト元日", "テスト建国記念の日", "テスト海の日");
    }

    /**
     * テストケース：年別祝日数取得
     * 
     * テスト対象メソッド：
     * - HolidayRepository.countByYear()
     * 
     * テストシナリオ：
     * - 特定の年の祝日数を取得
     * - データベースに複数の祝日が存在する
     * 
     * 期待結果：
     * - 指定年の祝日数が正確に返される
     */
    @Test
    void testCountByYear() {
        // Given
        int testYear = 2023;
        
        // 2023年の祝日をいくつか作成
        Holiday holiday1 = createTestHoliday(LocalDate.of(2023, 1, 1), "テスト元日");
        Holiday holiday2 = createTestHoliday(LocalDate.of(2023, 2, 11), "テスト建国記念の日");
        Holiday holiday3 = createTestHoliday(LocalDate.of(2023, 12, 25), "テストクリスマス");
        
        // 異なる年の祝日
        Holiday holiday4 = createTestHoliday(LocalDate.of(2024, 1, 1), "テスト元日");
        
        entityManager.persist(holiday1);
        entityManager.persist(holiday2);
        entityManager.persist(holiday3);
        entityManager.persist(holiday4);
        entityManager.flush();

        // When
        long result = holidayRepository.countByYear(testYear);

        // Then
        assertThat(result).isEqualTo(3);
    }

    /**
     * テストケース：月別祝日数取得
     * 
     * テスト対象メソッド：
     * - HolidayRepository.countByYearAndMonth()
     * 
     * テストシナリオ：
     * - 特定の年月の祝日数を取得
     * - データベースに複数の祝日が存在する
     * 
     * 期待結果：
     * - 指定年月の祝日数が正確に返される
     */
    @Test
    void testCountByYearAndMonth() {
        // Given
        int testYear = 2023;
        int testMonth = 1;
        
        // 2023年1月の祝日をいくつか作成
        Holiday holiday1 = createTestHoliday(LocalDate.of(2023, 1, 1), "テスト元日");
        Holiday holiday2 = createTestHoliday(LocalDate.of(2023, 1, 15), "テスト成人の日");
        
        // 異なる月の祝日
        Holiday holiday3 = createTestHoliday(LocalDate.of(2023, 2, 11), "テスト建国記念の日");
        
        entityManager.persist(holiday1);
        entityManager.persist(holiday2);
        entityManager.persist(holiday3);
        entityManager.flush();

        // When
        long result = holidayRepository.countByYearAndMonth(testYear, testMonth);

        // Then
        assertThat(result).isEqualTo(2);
    }

    /**
     * テストケース：期間内祝日数取得
     * 
     * テスト対象メソッド：
     * - HolidayRepository.countByDateRange()
     * 
     * テストシナリオ：
     * - 特定の期間内の祝日数を取得
     * - データベースに複数の祝日が存在する
     * 
     * 期待結果：
     * - 指定期間内の祝日数が正確に返される
     */
    @Test
    void testCountByDateRange() {
        // Given
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 3, 31);
        
        // 期間内の祝日
        Holiday holiday1 = createTestHoliday(LocalDate.of(2023, 1, 1), "テスト元日");
        Holiday holiday2 = createTestHoliday(LocalDate.of(2023, 2, 11), "テスト建国記念の日");
        
        // 期間外の祝日
        Holiday holiday3 = createTestHoliday(LocalDate.of(2023, 4, 1), "テストエイプリルフール");
        
        entityManager.persist(holiday1);
        entityManager.persist(holiday2);
        entityManager.persist(holiday3);
        entityManager.flush();

        // When
        long result = holidayRepository.countByDateRange(startDate, endDate);

        // Then
        assertThat(result).isEqualTo(2);
    }

    /**
     * テストケース：年別祝日統計取得
     * 
     * テスト対象メソッド：
     * - HolidayRepository.getYearlyStatistics()
     * 
     * テストシナリオ：
     * - 年別の祝日数統計を取得
     * - データベースに複数年の祝日が存在する
     * 
     * 期待結果：
     * - 年別統計情報が年降順で返される
     */
    @Test
    void testGetYearlyStatistics() {
        // Given
        // 複数年の祝日を作成
        Holiday holiday1 = createTestHoliday(LocalDate.of(2024, 1, 1), "テスト元日");
        Holiday holiday2 = createTestHoliday(LocalDate.of(2024, 12, 25), "テストクリスマス");
        Holiday holiday3 = createTestHoliday(LocalDate.of(2023, 1, 1), "テスト元日");
        Holiday holiday4 = createTestHoliday(LocalDate.of(2023, 2, 11), "テスト建国記念の日");
        Holiday holiday5 = createTestHoliday(LocalDate.of(2023, 12, 25), "テストクリスマス");
        
        entityManager.persist(holiday1);
        entityManager.persist(holiday2);
        entityManager.persist(holiday3);
        entityManager.persist(holiday4);
        entityManager.persist(holiday5);
        entityManager.flush();

        // When
        List<Map<String, Object>> result = holidayRepository.getYearlyStatistics();

        // Then
        assertThat(result).isNotEmpty();
        // 結果が年降順になっていることを確認
        assertThat(result).extracting(map -> ((Number) map.get("year")).doubleValue())
                .containsExactly(2024.0, 2023.0); // PostgreSQLはBigDecimalで返す
        
        // 各年の祝日数を確認
        Map<String, Object> stat2024 = result.get(0);
        Map<String, Object> stat2023 = result.get(1);
        
        assertThat(((Number) stat2024.get("year")).doubleValue()).isEqualTo(2024.0);
        assertThat(((Number) stat2024.get("holidayCount")).longValue()).isEqualTo(2L);
        
        assertThat(((Number) stat2023.get("year")).doubleValue()).isEqualTo(2023.0);
        assertThat(((Number) stat2023.get("holidayCount")).longValue()).isEqualTo(3L);
    }

    /**
     * テストケース：月別祝日統計取得
     * 
     * テスト対象メソッド：
     * - HolidayRepository.getMonthlyStatistics()
     * 
     * テストシナリオ：
     * - 特定の年の月別祝日数統計を取得
     * - データベースに指定年の祝日が存在する
     * 
     * 期待結果：
     * - 月別統計情報が月昇順で返される
     */
    @Test
    void testGetMonthlyStatistics() {
        // Given
        int testYear = 2023;
        
        // 2023年の各月の祝日を作成
        Holiday holiday1 = createTestHoliday(LocalDate.of(2023, 1, 1), "テスト元日");
        Holiday holiday2 = createTestHoliday(LocalDate.of(2023, 1, 15), "テスト成人の日");
        Holiday holiday3 = createTestHoliday(LocalDate.of(2023, 2, 11), "テスト建国記念の日");
        Holiday holiday4 = createTestHoliday(LocalDate.of(2023, 12, 25), "テストクリスマス");
        
        entityManager.persist(holiday1);
        entityManager.persist(holiday2);
        entityManager.persist(holiday3);
        entityManager.persist(holiday4);
        entityManager.flush();

        // When
        List<Map<String, Object>> result = holidayRepository.getMonthlyStatistics(testYear);

        // Then
        assertThat(result).isNotEmpty();
        // 結果が月昇順になっていることを確認
        assertThat(result).extracting(map -> ((Number) map.get("month")).doubleValue())
                .containsExactly(1.0, 2.0, 12.0); // PostgreSQLはBigDecimalで返す
        
        // 各月の祝日数を確認
        Map<String, Object> stat1 = result.get(0);
        Map<String, Object> stat2 = result.get(1);
        Map<String, Object> stat12 = result.get(2);
        
        assertThat(((Number) stat1.get("month")).intValue()).isEqualTo(1);
        assertThat(((Number) stat1.get("holidayCount")).longValue()).isEqualTo(2L);
        
        assertThat(((Number) stat2.get("month")).intValue()).isEqualTo(2);
        assertThat(((Number) stat2.get("holidayCount")).longValue()).isEqualTo(1L);
        
        assertThat(((Number) stat12.get("month")).intValue()).isEqualTo(12);
        assertThat(((Number) stat12.get("holidayCount")).longValue()).isEqualTo(1L);
    }
    
    /**
     * 簡単なテストメソッド - 基本的な動作確認
     */
    @Test
    void testBasicFunctionality() {
        // Given
        Holiday holiday = createTestHoliday(LocalDate.of(2025, 1, 1), "元日");
        entityManager.persistAndFlush(holiday);

        // When
        Optional<Holiday> result = holidayRepository.findByDate(LocalDate.of(2025, 1, 1));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("元日");
    }
}