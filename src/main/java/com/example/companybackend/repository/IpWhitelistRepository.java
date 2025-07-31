package com.example.companybackend.repository;

import com.example.companybackend.entity.IpWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * IPホワイトリストリポジトリ
 * comsys_dump.sql ip_whitelistテーブル完全対応
 * 
 * テーブル構造:
 * - id (SERIAL PRIMARY KEY)
 * - ip_address (INET NOT NULL UNIQUE)
 * - description (TEXT)
 * - is_active (BOOLEAN DEFAULT TRUE)
 * - created_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 * - updated_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 */
@Repository
public interface IpWhitelistRepository extends JpaRepository<IpWhitelist, Integer> {

    /**
     * IPアドレスによる検索
     * @param ipAddress IPアドレス
     * @return IPホワイトリスト情報
     */
    Optional<IpWhitelist> findByIpAddress(String ipAddress);

    /**
     * IPアドレスによる存在確認
     * @param ipAddress IPアドレス
     * @return 存在するかどうか
     */
    boolean existsByIpAddress(String ipAddress);

    /**
     * アクティブなIPアドレス一覧取得
     * @return アクティブなIPホワイトリスト
     */
    @Query(nativeQuery = true, value = "SELECT iw.* FROM ip_whitelist iw WHERE iw.is_active = true ORDER BY iw.ip_address")
    List<IpWhitelist> findActiveIpAddresses();

    /**
     * 非アクティブなIPアドレス一覧取得
     * @return 非アクティブなIPホワイトリスト
     */
    @Query(nativeQuery = true, value = "SELECT iw.* FROM ip_whitelist iw WHERE iw.is_active = false ORDER BY iw.ip_address")
    List<IpWhitelist> findInactiveIpAddresses();

    /**
     * IPアドレス範囲検索（CIDR記法対応）
     * PostgreSQLのINET型機能を活用
     * @param ipAddress 検索対象IPアドレス
     * @return 該当するIPホワイトリストエントリ
     */
    @Query(value = "SELECT * FROM ip_whitelist WHERE is_active = true AND :ipAddress <<= ip_address", nativeQuery = true)
    List<IpWhitelist> findByIpAddressRange(@Param("ipAddress") String ipAddress);

    /**
     * IPアドレス許可チェック
     * @param ipAddress 確認対象IPアドレス
     * @return 許可されているかどうか
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM ip_whitelist WHERE is_active = true AND (:ipAddress <<= ip_address OR ip_address = :ipAddress)", nativeQuery = true)
    boolean isIpAddressAllowed(@Param("ipAddress") String ipAddress);

    /**
     * 説明文部分一致検索
     * @param descriptionPattern 説明文パターン
     * @return 該当IPホワイトリスト
     */
    @Query(nativeQuery = true, value = "SELECT iw.* FROM ip_whitelist iw WHERE iw.description LIKE CONCAT('%', :descriptionPattern, '%') ORDER BY iw.ip_address")
    List<IpWhitelist> findByDescriptionContaining(@Param("descriptionPattern") String descriptionPattern);

    /**
     * 作成日時範囲によるIPホワイトリスト検索
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 該当期間に作成されたIPホワイトリスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM ip_whitelist iw WHERE iw.created_at BETWEEN :startDate AND :endDate ORDER BY iw.created_at DESC")
    List<IpWhitelist> findByCreatedAtBetween(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    /**
     * 最近更新されたIPホワイトリスト取得
     * @param days 過去何日以内
     * @return 最近更新されたIPホワイトリスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM ip_whitelist iw WHERE iw.updated_at >= CURRENT_TIMESTAMP - INTERVAL ':days days' ORDER BY iw.updated_at DESC")
    List<IpWhitelist> findRecentlyUpdated(@Param("days") int days);

    /**
     * IPアドレス統計情報取得
     * @return IPホワイトリスト統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            COUNT(*) as totalEntries,
            COUNT(CASE WHEN is_active = true THEN 1 END) as activeEntries,
            COUNT(CASE WHEN is_active = false THEN 1 END) as inactiveEntries
        FROM ip_whitelist
        """)
    java.util.Map<String, Object> getIpWhitelistStatistics();

    /**
     * プライベートIPアドレス検索
     * RFC 1918準拠のプライベートIP範囲
     * @return プライベートIPアドレスのエントリ
     */
    @Query(value = """
        SELECT * FROM ip_whitelist 
        WHERE (ip_address <<= '10.0.0.0/8'::inet 
           OR ip_address <<= '172.16.0.0/12'::inet 
           OR ip_address <<= '192.168.0.0/16'::inet)
        ORDER BY ip_address
        """, nativeQuery = true)
    List<IpWhitelist> findPrivateIpAddresses();

    /**
     * パブリックIPアドレス検索
     * @return パブリックIPアドレスのエントリ
     */
    @Query(value = """
        SELECT * FROM ip_whitelist 
        WHERE NOT (ip_address <<= '10.0.0.0/8'::inet 
                OR ip_address <<= '172.16.0.0/12'::inet 
                OR ip_address <<= '192.168.0.0/16'::inet
                OR ip_address <<= '127.0.0.0/8'::inet)
        ORDER BY ip_address
        """, nativeQuery = true)
    List<IpWhitelist> findPublicIpAddresses();

    /**
     * IPv4アドレス検索
     * @return IPv4アドレスのエントリ
     */
    @Query(value = "SELECT * FROM ip_whitelist WHERE family(ip_address) = 4 ORDER BY ip_address", nativeQuery = true)
    List<IpWhitelist> findIpv4Addresses();

    /**
     * IPv6アドレス検索
     * @return IPv6アドレスのエントリ
     */
    @Query(value = "SELECT * FROM ip_whitelist WHERE family(ip_address) = 6 ORDER BY ip_address", nativeQuery = true)
    List<IpWhitelist> findIpv6Addresses();

    /**
     * CIDR記法のネットワーク範囲検索
     * @return ネットワーク範囲として定義されたエントリ
     */
    @Query(value = "SELECT * FROM ip_whitelist WHERE masklen(ip_address) < 32 ORDER BY ip_address", nativeQuery = true)
    List<IpWhitelist> findNetworkRanges();

    /**
     * 単一IPアドレス検索
     * @return 単一IPアドレスとして定義されたエントリ
     */
    @Query(value = "SELECT * FROM ip_whitelist WHERE masklen(ip_address) = 32 OR masklen(ip_address) = 128 ORDER BY ip_address", nativeQuery = true)
    List<IpWhitelist> findSingleIpAddresses();

    /**
     * 重複IPアドレスチェック
     * @param ipAddress IPアドレス
     * @param excludeId 除外するID（更新時）
     * @return 重複するエントリがあるかどうか
     */
    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(iw) > 0 THEN true ELSE false END FROM ip_whitelist iw WHERE iw.ip_address = :ipAddress AND (:excludeId IS NULL OR iw.id != :excludeId)")
    boolean existsByIpAddressExcludingId(@Param("ipAddress") String ipAddress, @Param("excludeId") Integer excludeId);

    /**
     * 期限切れエントリ検索（説明文に期限情報がある場合）
     * @return 削除候補のエントリ
     */
    @Query(nativeQuery = true, value = "SELECT iw.* FROM ip_whitelist iw WHERE iw.description LIKE '%期限%' OR iw.description LIKE '%expire%' ORDER BY iw.updated_at ASC")
    List<IpWhitelist> findExpiredEntries();

    /**
     * アクセス頻度による統計
     * システムログとの連携が必要な場合の準備
     * @return IPアドレス別アクセス統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            iw.ip_address as ipAddress,
            iw.description as description,
            iw.is_active as isActive,
            iw.created_at as createdAt
        FROM ip_whitelist iw 
        WHERE iw.is_active = true
        ORDER BY iw.created_at DESC
        """)
    List<java.util.Map<String, Object>> getActiveIpStatistics();

    /**
     * バッチ処理用：大量データIPホワイトリスト取得
     * @param offset オフセット
     * @param limit 取得件数
     * @return IPホワイトリスト
     */
    @Query(nativeQuery = true, value = "SELECT iw.* FROM ip_whitelist iw ORDER BY iw.id ASC LIMIT :limit OFFSET :offset")
    List<IpWhitelist> findForBatchProcessing(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * IP範囲重複チェック
     * 新規追加時の重複確認用
     * @param ipAddress 確認対象IPアドレス
     * @return 重複する範囲のエントリ
     */
    @Query(value = """
        SELECT * FROM ip_whitelist 
        WHERE :ipAddress <<= ip_address 
           OR ip_address <<= :ipAddress 
           OR ip_address && :ipAddress
        """, nativeQuery = true)
    List<IpWhitelist> findOverlappingRanges(@Param("ipAddress") String ipAddress);

    /**
     * セキュリティ監査用：最近のIPホワイトリスト変更
     * @param hours 過去何時間以内
     * @return 最近変更されたエントリ
     */
    @Query(nativeQuery = true, value = "SELECT iw.* FROM ip_whitelist iw WHERE iw.updated_at >= CURRENT_TIMESTAMP - INTERVAL ':hours hours' OR iw.created_at >= CURRENT_TIMESTAMP - INTERVAL ':hours hours' ORDER BY GREATEST(iw.created_at, iw.updated_at) DESC")
    List<IpWhitelist> findRecentChanges(@Param("hours") int hours);
}