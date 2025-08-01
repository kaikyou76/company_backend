package com.example.companybackend.repository;

import com.example.companybackend.entity.WorkLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 勤務地リポジトリ
 * comsys_dump.sql work_locationsテーブル完全対応
 * 
 * テーブル構造:
 * - id (SERIAL PRIMARY KEY)
 * - name (TEXT NOT NULL)
 * - address (TEXT)
 * - coordinates (POINT)
 * - is_active (BOOLEAN DEFAULT TRUE)
 * - created_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 * - updated_at (TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
 */
@Repository
public interface WorkLocationRepository extends JpaRepository<WorkLocation, Integer> {

    /**
     * 勤務地名による検索
     * @param name 勤務地名
     * @return 勤務地情報
     */
    Optional<WorkLocation> findByName(String name);

    /**
     * 勤務地名による存在確認
     * @param name 勤務地名
     * @return 存在するかどうか
     */
    boolean existsByName(String name);

    /**
     * アクティブな勤務地一覧取得
     * @return アクティブな勤務地リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM work_locations wl WHERE wl.is_active = true ORDER BY wl.name")
    List<WorkLocation> findActiveWorkLocations();

    /**
     * 非アクティブな勤務地一覧取得
     * @return 非アクティブな勤務地リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM work_locations wl WHERE wl.is_active = false ORDER BY wl.name")
    List<WorkLocation> findInactiveWorkLocations();

    /**
     * 勤務地名部分一致検索
     * @param namePattern 勤務地名パターン
     * @return 該当勤務地リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM work_locations wl WHERE wl.name LIKE CONCAT('%', :namePattern, '%') ORDER BY wl.name")
    List<WorkLocation> findByNameContaining(@Param("namePattern") String namePattern);

    /**
     * 住所部分一致検索
     * @param addressPattern 住所パターン
     * @return 該当勤務地リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM work_locations wl WHERE wl.address LIKE CONCAT('%', :addressPattern, '%') ORDER BY wl.name")
    List<WorkLocation> findByAddressContaining(@Param("addressPattern") String addressPattern);

    /**
     * 座標が設定された勤務地取得
     * @return 座標設定済み勤務地リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM work_locations wl WHERE wl.coordinates IS NOT NULL ORDER BY wl.name")
    List<WorkLocation> findWorkLocationsWithCoordinates();

    /**
     * 座標が未設定の勤務地取得
     * @return 座標未設定勤務地リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM work_locations wl WHERE wl.coordinates IS NULL ORDER BY wl.name")
    List<WorkLocation> findWorkLocationsWithoutCoordinates();

    /**
     * 指定位置からの距離内にある勤務地検索
     * PostGIS拡張機能を使用した地理的検索
     * @param longitude 経度
     * @param latitude 緯度
     * @param radiusKm 検索半径（キロメートル）
     * @return 指定距離内の勤務地リスト
     */
    @Query(value = """
        SELECT *, ST_Distance(
            ST_Transform(coordinates, 3857), 
            ST_Transform(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326), 3857)
        ) / 1000 as distance_km
        FROM work_locations 
        WHERE coordinates IS NOT NULL 
        AND ST_DWithin(
            ST_Transform(coordinates, 3857), 
            ST_Transform(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326), 3857), 
            :radiusKm * 1000
        )
        ORDER BY distance_km ASC
        """, nativeQuery = true)
    List<Object[]> findWorkLocationsWithinRadius(@Param("longitude") double longitude, @Param("latitude") double latitude, @Param("radiusKm") double radiusKm);

    /**
     * 最寄りの勤務地検索
     * @param longitude 経度
     * @param latitude 緯度
     * @param limit 取得件数
     * @return 最寄りの勤務地リスト
     */
    @Query(value = """
        SELECT *, ST_Distance(
            ST_Transform(coordinates, 3857), 
            ST_Transform(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326), 3857)
        ) / 1000 as distance_km
        FROM work_locations 
        WHERE coordinates IS NOT NULL 
        AND is_active = true
        ORDER BY distance_km ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findNearestWorkLocations(@Param("longitude") double longitude, @Param("latitude") double latitude, @Param("limit") int limit);

    /**
     * 勤務地統計情報取得
     * @return 勤務地統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            COUNT(wl) as totalLocations,
            COUNT(CASE WHEN wl.is_active = true THEN 1 END) as activeLocations,
            COUNT(CASE WHEN wl.is_active = false THEN 1 END) as inactiveLocations,
            COUNT(CASE WHEN wl.coordinates IS NOT NULL THEN 1 END) as locationsWithCoordinates,
            COUNT(CASE WHEN wl.address IS NOT NULL THEN 1 END) as locationsWithAddress
        FROM work_locations wl
        """)
    java.util.Map<String, Object> getWorkLocationStatistics();

    /**
     * 作成日時範囲による勤務地検索
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 該当期間に作成された勤務地リスト
     */
    @Query("SELECT wl FROM WorkLocation wl WHERE wl.createdAt BETWEEN :startDate AND :endDate ORDER BY wl.createdAt DESC")
    List<WorkLocation> findByCreatedAtBetween(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);
    
    /**
     * タイプによる勤務地検索
     * @param type 勤務地タイプ (office, client, other)
     * @return 該当する勤務地リスト
     */
    @Query(nativeQuery = true, value = "SELECT * FROM work_locations wl WHERE wl.type = :type AND wl.is_active = true ORDER BY wl.name")
    List<WorkLocation> findByType(@Param("type") String type);
}