package com.example.companybackend.batch.writer;

import com.example.companybackend.entity.SystemLog;
import com.example.companybackend.repository.SystemLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * データクリーンアップ用ライター
 * 古いシステムログデータを削除する
 */
@Slf4j
@Component
public class DataCleanupWriter implements ItemWriter<SystemLog> {

    @Autowired
    private SystemLogRepository systemLogRepository;

    private long deletedCount = 0;

    @Override
    public void write(Chunk<? extends SystemLog> chunk) throws Exception {
        List<? extends SystemLog> items = chunk.getItems();

        if (items.isEmpty()) {
            log.debug("削除対象データがありません");
            return;
        }

        log.info("データクリーンアップ処理開始: 削除対象件数={}", items.size());

        try {
            // IDリストを作成
            List<Long> idsToDelete = items.stream()
                    .map(SystemLog::getId)
                    .collect(Collectors.toList());

            // バッチ削除実行
            int deletedInThisBatch = systemLogRepository.deleteByIdIn(idsToDelete);
            deletedCount += deletedInThisBatch;

            log.info("データクリーンアップ処理完了: 今回削除件数={}, 累計削除件数={}",
                    deletedInThisBatch, deletedCount);

            // 削除されたデータの詳細をデバッグログに出力
            if (log.isDebugEnabled()) {
                items.forEach(item -> log.debug("削除完了: ID={}, 作成日時={}, アクション={}",
                        item.getId(), item.getCreatedAt(), item.getAction()));
            }

        } catch (Exception e) {
            log.error("データクリーンアップ処理中にエラーが発生しました: 対象件数={}", items.size(), e);

            // エラー詳細をログに記録
            items.forEach(item -> log.error("削除失敗データ: ID={}, 作成日時={}", item.getId(), item.getCreatedAt()));

            throw new RuntimeException("データクリーンアップ処理に失敗しました", e);
        }
    }

    /**
     * 削除件数を取得
     */
    public long getDeletedCount() {
        return deletedCount;
    }

    /**
     * 削除件数をリセット
     */
    public void resetDeletedCount() {
        this.deletedCount = 0;
    }

    /**
     * 削除処理の統計情報を出力
     */
    public void logStatistics() {
        log.info("=== データクリーンアップ統計情報 ===");
        log.info("総削除件数: {}", deletedCount);
        log.info("削除完了時刻: {}", OffsetDateTime.now());
        log.info("=====================================");
    }
}