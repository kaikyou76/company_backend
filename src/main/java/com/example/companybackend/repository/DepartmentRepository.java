package com.example.companybackend.repository;

import com.example.companybackend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 部署リポジトリ
 * comsys_dump.sql departmentsテーブル完全対応
 * 
 * テーブル構造:
 * - id (SERIAL PRIMARY KEY)
 * - name (TEXT NOT NULL)
 * - manager_id (INTEGER REFERENCES users(id))
 * - created_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 * - updated_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    /**
     * 部署名による検索
     * @param name 部署名
     * @return 部署情報
     */
    Optional<Department> findByName(String name);

    /**
     * 部署名による存在確認
     * @param name 部署名
     * @return 存在するかどうか
     */
    boolean existsByName(String name);

    /**
     * マネージャーIDによる部署検索
     * @param managerId マネージャーID
     * @return 部署リスト
     */
    List<Department> findByManagerId(Integer managerId);

    /**
     * マネージャーが設定されている部署一覧取得
     * @return マネージャー設定済み部署リスト
     */
    @Query(nativeQuery = true, value = "SELECT d.* FROM departments d WHERE d.manager_id IS NOT NULL")
    List<Department> findDepartmentsWithManager();

    /**
     * マネージャーが未設定の部署一覧取得
     * @return マネージャー未設定部署リスト
     */
    @Query(nativeQuery = true, value = "SELECT d.* FROM departments d WHERE d.manager_id IS NULL")
    List<Department> findDepartmentsWithoutManager();

    /**
     * 部署名部分一致検索
     * @param namePattern 部署名パターン
     * @return 該当部署リスト
     */
    @Query(nativeQuery = true, value = "SELECT d.* FROM departments d WHERE d.name LIKE CONCAT('%', :namePattern, '%')")
    List<Department> findByNameContaining(@Param("namePattern") String namePattern);

    /**
     * 部署統計情報取得
     * @return 部署統計（部署数、マネージャー設定済み数、未設定数）
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            COUNT(d) as totalDepartments,
            COUNT(d.manager_id) as departmentsWithManager,
            COUNT(d) - COUNT(d.manager_id) as departmentsWithoutManager
        FROM departments d
        """)
    java.util.Map<String, Object> getDepartmentStatistics();

    /**
     * 作成日時範囲による部署検索
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 該当期間に作成された部署リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM departments d WHERE d.created_at BETWEEN :startDate AND :endDate")
    List<Department> findByCreatedAtBetween(
        @Param("startDate") java.time.OffsetDateTime startDate,
        @Param("endDate") java.time.OffsetDateTime endDate
    );

    /**
     * 最近更新された部署一覧取得
     * @param days 過去何日以内
     * @return 最近更新された部署リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM departments d WHERE d.updated_at >= CURRENT_TIMESTAMP - INTERVAL ':days days'")
    List<Department> findRecentlyUpdated(@Param("days") int days);

    /**
     * 部署名でソートした全部署取得
     * @return 部署名順ソート済み部署リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM departments d ORDER BY d.name ASC")
    List<Department> findAllOrderByName();

    /**
     * 特定マネージャーの管理部署数取得
     * @param managerId マネージャーID
     * @return 管理部署数
     */
    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM departments d WHERE d.manager_id = :managerId")
    long countByManagerId(@Param("managerId") Integer managerId);

    /**
     * バッチ処理用：大量データ部門取得
     * 処理効率を考慮したページング対応
     * @param offset オフセット
     * @param limit 取得件数
     * @return 部署リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM departments d ORDER BY d.id ASC LIMIT :limit OFFSET :offset")
    List<Department> findForBatchProcessing(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * データ整合性チェック：孤立マネージャー検索
     * 存在しないユーザーIDを参照している部署を検索
     * @return 孤立参照のある部署リスト
     */
    @Query(nativeQuery = true, value = """
        SELECT * FROM departments d 
        WHERE d.manager_id IS NOT NULL 
        AND NOT EXISTS (
            SELECT 1 FROM users u WHERE u.id = d.manager_id
        )
        """)
    List<Department> findDepartmentsWithOrphanedManagerId();
}