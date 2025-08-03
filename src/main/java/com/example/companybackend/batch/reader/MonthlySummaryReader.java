package com.example.companybackend.batch.reader;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.repository.AttendanceRecordRepository;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/**
 * 月次集計用のリーダー
 * 日次処理とは異なり、月次処理では各ユーザーの月次データを処理するため、
 * 月の最初の日の出勤記録を基準として各ユーザーを識別する
 */
public class MonthlySummaryReader {

    private final AttendanceRecordRepository attendanceRecordRepository;

    public MonthlySummaryReader(AttendanceRecordRepository attendanceRecordRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    public RepositoryItemReader<AttendanceRecord> reader() {
        RepositoryItemReader<AttendanceRecord> reader = new RepositoryItemReader<>();

        // ソート設定（ユーザーID、日付順）
        Map<String, Sort.Direction> sorts = new HashMap<>();
        sorts.put("userId", Sort.Direction.ASC);
        sorts.put("timestamp", Sort.Direction.ASC);

        reader.setRepository(attendanceRecordRepository);
        reader.setMethodName("findByTypeAndTimestampBetween");
        reader.setPageSize(100);
        reader.setName("monthlySummaryReader");
        reader.setSort(sorts);

        // 月次処理のため、現在月の範囲を設定
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        // パラメータ設定（'in'タイプのレコードのみ、当月分）
        reader.setArguments(java.util.Arrays.asList(
                "in",
                monthStart.atStartOfDay().atOffset(java.time.ZoneOffset.UTC),
                monthEnd.atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC)));

        return reader;
    }
}