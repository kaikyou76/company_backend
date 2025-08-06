package com.example.companybackend.batch.reader;

import com.example.companybackend.entity.SystemLog;
import com.example.companybackend.repository.SystemLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.PostgresPagingQueryProvider;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * データクリーンアップ用リーダー
 * 古いシステムログデータを読み込む
 */
public class DataCleanupReader {

    private static final Logger log = LoggerFactory.getLogger(DataCleanupReader.class);

    private final SystemLogRepository systemLogRepository;
    private final DataSource dataSource;

    // 保持期間（月数）
    private static final int RETENTION_MONTHS = 12;

    public DataCleanupReader(SystemLogRepository systemLogRepository, DataSource dataSource) {
        this.systemLogRepository = systemLogRepository;
        this.dataSource = dataSource;
    }

    public ItemReader<SystemLog> reader() {
        log.info("データクリーンアップリーダーを初期化します");

        try {
            JdbcPagingItemReader<SystemLog> reader = new JdbcPagingItemReader<>();
            reader.setDataSource(dataSource);
            reader.setPageSize(100);
            reader.setRowMapper(new BeanPropertyRowMapper<>(SystemLog.class));

            // PostgreSQL用のクエリプロバイダーを設定
            PostgresPagingQueryProvider queryProvider = new PostgresPagingQueryProvider();
            queryProvider
                    .setSelectClause("SELECT id, user_id, action, status, ip_address, user_agent, details, created_at");
            queryProvider.setFromClause("FROM system_logs");

            // 保持期間を超えたデータのみを対象とする
            OffsetDateTime cutoffDate = OffsetDateTime.now().minusMonths(RETENTION_MONTHS);
            queryProvider.setWhereClause("WHERE created_at < :cutoffDate");

            // ソート条件を設定
            Map<String, Order> sortKeys = new HashMap<>();
            sortKeys.put("id", Order.ASCENDING);
            queryProvider.setSortKeys(sortKeys);

            reader.setQueryProvider(queryProvider);

            // パラメータを設定
            Map<String, Object> parameterValues = new HashMap<>();
            parameterValues.put("cutoffDate", cutoffDate);
            reader.setParameterValues(parameterValues);

            reader.setName("dataCleanupReader");
            reader.afterPropertiesSet();

            log.info("データクリーンアップリーダーの初期化が完了しました。カットオフ日時: {}", cutoffDate);
            return reader;

        } catch (Exception e) {
            log.error("データクリーンアップリーダーの初期化中にエラーが発生しました", e);
            throw new RuntimeException("データクリーンアップリーダーの初期化に失敗しました", e);
        }
    }

    /**
     * 削除対象データ数を取得
     */
    public long getDeleteTargetCount() {
        try {
            OffsetDateTime cutoffDate = OffsetDateTime.now().minusMonths(RETENTION_MONTHS);
            return systemLogRepository.countByCreatedAtBefore(cutoffDate);
        } catch (Exception e) {
            log.error("削除対象データ数の取得中にエラーが発生しました", e);
            return 0;
        }
    }
}