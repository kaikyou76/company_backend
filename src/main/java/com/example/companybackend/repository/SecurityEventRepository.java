package com.example.companybackend.repository;

import com.example.companybackend.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * セキュリティイベントリポジトリ
 */
@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

        /**
         * 指定期間内の特定IPアドレスからのイベント数を取得
         */
        @Query("SELECT COUNT(se) FROM SecurityEvent se WHERE se.ipAddress = :ipAddress " +
                        "AND se.eventType = :eventType AND se.createdAt >= :since")
        long countByIpAddressAndEventTypeAndCreatedAtAfter(
                        @Param("ipAddress") String ipAddress,
                        @Param("eventType") String eventType,
                        @Param("since") LocalDateTime since);

        /**
         * 指定期間内の特定イベントタイプのイベントを取得
         */
        List<SecurityEvent> findByEventTypeAndCreatedAtAfterOrderByCreatedAtDesc(
                        String eventType, LocalDateTime since);

        /**
         * 特定IPアドレスの最近のイベントを取得
         */
        List<SecurityEvent> findTop10ByIpAddressOrderByCreatedAtDesc(String ipAddress);

        /**
         * 重要度レベル別のイベント数を取得
         */
        @Query("SELECT se.severityLevel, COUNT(se) FROM SecurityEvent se " +
                        "WHERE se.createdAt >= :since GROUP BY se.severityLevel")
        List<Object[]> countBySeverityLevelAndCreatedAtAfter(@Param("since") LocalDateTime since);

        /**
         * 古いセキュリティイベントを削除（データ保持期間管理）
         */
        void deleteByCreatedAtBefore(LocalDateTime before);
}