package com.example.companybackend.batch.processor;

import com.example.companybackend.entity.SystemLog;
import com.example.companybackend.repository.SystemLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * データクリーンアップ処理プロセッサー
 * 古いシステムログデータを削除対象として識別する
 */
@Slf4j
public class DataCleanupProcessor implements ItemProcessor<SystemLog, SystemLog> {

    private SystemLogRepository systemLogRepository;

    // 保持期間（月数）
    private static final int RETENTION_MONTHS = 12;

    public void setSystemLogRepository(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    @Override
    public SystemLog process(SystemLog systemLog) throws Exception {
        log.debug("データクリーンアップ処理開始: システムログID={}", systemLog.getId());

        try {
            // 保持期間を超えたデータかチェック
            OffsetDateTime cutoffDate = OffsetDateTime.now().minusMonths(RETENTION_MONTHS);

            if (systemLog.getCreatedAt().isBefore(cutoffDate)) {
                log.debug("削除対象データ: ID={}, 作成日時={}",
                        systemLog.getId(), systemLog.getCreatedAt());
                return systemLog; // 削除対象として返す
            }

            log.debug("保持対象データ: ID={}, 作成日時={}",
                    systemLog.getId(), systemLog.getCreatedAt());
            return null; // 削除対象外はnullを返す

        } catch (Exception e) {
            log.error("データクリーンアップ処理中にエラーが発生しました: システムログID={}",
                    systemLog.getId(), e);
            throw e;
        }
    }

    /**
     * 削除対象データの統計情報を取得
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

    /**
     * 保持期間（月数）を取得
     */
    public int getRetentionMonths() {
        return RETENTION_MONTHS;
    }

    /**
     * カットオフ日付を取得
     */
    public LocalDate getCutoffDate() {
        return OffsetDateTime.now().minusMonths(RETENTION_MONTHS).toLocalDate();
    }
}