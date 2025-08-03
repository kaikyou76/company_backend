package com.example.companybackend.batch.reader;

import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.repository.AttendanceSummaryRepository;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/**
 * 残業監視用のリーダー
 * 月次集計データから残業監視対象のデータを読み取る
 */
public class OvertimeMonitoringReader {

    private final AttendanceSummaryRepository attendanceSummaryRepository;

    public OvertimeMonitoringReader(AttendanceSummaryRepository attendanceSummaryRepository) {
        this.attendanceSummaryRepository = attendanceSummaryRepository;
    }

    public RepositoryItemReader<AttendanceSummary> reader() {
        RepositoryItemReader<AttendanceSummary> reader = new RepositoryItemReader<>();

        // ソート設定（ユーザーID、対象日順）
        Map<String, Sort.Direction> sorts = new HashMap<>();
        sorts.put("userId", Sort.Direction.ASC);
        sorts.put("targetDate", Sort.Direction.ASC);

        reader.setRepository(attendanceSummaryRepository);
        reader.setMethodName("findBySummaryTypeAndTargetDateBetween");
        reader.setPageSize(100);
        reader.setName("overtimeMonitoringReader");
        reader.setSort(sorts);

        // 残業監視のため、当月の月次集計データを対象とする
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        // パラメータ設定（'monthly'タイプのレコードのみ、当月分）
        reader.setArguments(java.util.Arrays.asList(
                "monthly",
                monthStart,
                monthEnd));

        return reader;
    }
}