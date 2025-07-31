package com.example.companybackend.repository;

import com.example.companybackend.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 役職リポジトリ
 * comsys_dump.sql positionsテーブル完全対応
 * 
 * テーブル構造:
 * - id (SERIAL PRIMARY KEY)
 * - name (TEXT NOT NULL)
 * - level (INTEGER)
 * - created_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 * - updated_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, Integer> {

    /**
     * 役職名による検索
     * @param name 役職名
     * @return 役職情報
     */
    Optional<Position> findByName(String name);

    /**
     * 役職名による存在確認
     * @param name 役職名
     * @return 存在するかどうか
     */
    boolean existsByName(String name);

    /**
     * レベルによる役職検索
     * @param level 役職レベル
     * @return 該当レベルの役職リスト
     */
    List<Position> findByLevel(Integer level);

    /**
     * レベル範囲による役職検索
     * @param minLevel 最小レベル
     * @param maxLevel 最大レベル
     * @return 指定レベル範囲の役職リスト
     */
    List<Position> findByLevelBetween(Integer minLevel, Integer maxLevel);

    /**
     * 管理職レベル以上の役職取得
     * @return 管理職レベル（レベル4以上）の役職リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM positions p WHERE p.level >= 4")
    List<Position> findManagementPositions();

    /**
     * 幹部レベル以上の役職取得
     * @return 幹部レベル（レベル6以上）の役職リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM positions p WHERE p.level >= 6")
    List<Position> findExecutivePositions();

    /**
     * 一般職レベルの役職取得
     * @return 一般職レベル（レベル3以下）の役職リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM positions p WHERE p.level <= 3")
    List<Position> findStaffPositions();

    /**
     * 役職名部分一致検索
     * @param namePattern 役職名パターン
     * @return 該当役職リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM positions p WHERE p.name LIKE CONCAT('%', :namePattern, '%')")
    List<Position> findByNameContaining(@Param("namePattern") String namePattern);

    /**
     * レベル順ソートした全役職取得
     * @return レベル順ソート済み役職リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM positions p ORDER BY p.level DESC, p.name ASC")
    List<Position> findAllOrderByLevelDesc();

    /**
     * 役職名順ソートした全役職取得
     * @return 役職名順ソート済み役職リスト
     */
    @Query(nativeQuery = true, value = "SELECT p.* FROM positions p ORDER BY p.name ASC")
    List<Position> findAllOrderByName();

    /**
     * 最高レベルの役職取得
     * @return 最高レベルの役職
     */
    @Query(nativeQuery = true, value = "SELECT p.* FROM positions p WHERE p.level = (SELECT MAX(level) FROM positions)")
    List<Position> findTopLevelPositions();

    /**
     * 特定レベル以上の役職数取得
     * @param level 基準レベル
     * @return 該当役職数
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(p) FROM positions p WHERE p.level >= :level")
    long countByLevelGreaterThanEqual(@Param("level") Integer level);

    /**
     * 役職統計情報取得
     * @return 役職統計（総数、最高レベル、最低レベル、平均レベル）
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            COUNT(p) as totalPositions,
            MAX(p.level) as maxLevel,
            MIN(p.level) as minLevel,
            AVG(p.level) as avgLevel,
            COUNT(CASE WHEN p.level >= 4 THEN 1 END) as managementCount,
            COUNT(CASE WHEN p.level >= 6 THEN 1 END) as executiveCount
        FROM positions p
        """)
    java.util.Map<String, Object> getPositionStatistics();

    /**
     * 作成日時範囲による役職検索
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 該当期間に作成された役職リスト
     */
    @Query(nativeQuery = true, value = "SELECT p.* FROM positions p WHERE p.created_at BETWEEN :startDate AND :endDate")
    List<Position> findByCreatedAtBetween(
        @Param("startDate") java.time.OffsetDateTime startDate,
        @Param("endDate") java.time.OffsetDateTime endDate
    );

    /**
     * 最近更新された役職一覧取得
     * @param days 過去何日以内
     * @return 最近更新された役職リスト
     */
    @Query(nativeQuery = true, value = "SELECT p.* FROM positions p WHERE p.updated_at >= CURRENT_TIMESTAMP - INTERVAL ':days days'")
    List<Position> findRecentlyUpdated(@Param("days") int days);

    /**
     * レベル未設定の役職検索
     * @return レベルがNULLの役職リスト
     */
    @Query(nativeQuery = true, value = "SELECT p.* FROM positions p WHERE p.level IS NULL")
    List<Position> findPositionsWithoutLevel();

    /**
     * 特定レベルの役職存在確認
     * @param level 確認するレベル
     * @return 該当レベルの役職が存在するかどうか
     */
    boolean existsByLevel(Integer level);

    /**
     * バッチ処理用：大量データ役職取得
     * 処理効率を考慮したページング対応
     * @param offset オフセット
     * @param limit 取得件数
     * @return 役職リスト
     */
    @Query(nativeQuery = true, value = "SELECT p.* FROM positions p ORDER BY p.id ASC LIMIT :limit OFFSET :offset")
    List<Position> findForBatchProcessing(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * レベル昇進パス取得
     * 現在のレベルより1つ上のレベルの役職を取得
     * @param currentLevel 現在のレベル
     * @return 次のレベルの役職リスト
     */
    @Query(nativeQuery = true, value = """
        SELECT p.* FROM positions p 
        WHERE p.level = (
            SELECT MIN(level) FROM positions 
            WHERE level > :currentLevel
        )
        """)
    List<Position> findNextLevelPositions(@Param("currentLevel") Integer currentLevel);
}