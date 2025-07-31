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
    @Query(nativeQuery = true, value = "SELECT wl.* FROM work_locations wl WHERE wl.created_at BETWEEN :startDate AND :endDate ORDER BY wl.created_at DESC")
    List<WorkLocation> findByCreatedAtBetween(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    /**
     * 最近更新された勤務地取得
     * @param days 過去何日以内
     * @return 最近更新された勤務地リスト
     */
    @Query(nativeQuery = true, value = "SELECT wl.* FROM work_locations wl WHERE wl.updated_at >= CURRENT_TIMESTAMP - INTERVAL ':days days' ORDER BY wl.updated_at DESC")
    List<WorkLocation> findRecentlyUpdated(@Param("days") int days);

    /**
     * 都道府県別勤務地統計
     * 住所から都道府県を抽出して統計
     * @return 都道府県別統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            SUBSTRING(wl.address, 1, POSITION('県' IN wl.address)) as prefecture,
            COUNT(wl) as locationCount
        FROM work_locations wl 
        WHERE wl.address IS NOT NULL 
        AND wl.address LIKE '%県%'
        GROUP BY SUBSTRING(wl.address, 1, POSITION('県' IN wl.address))
        ORDER BY COUNT(wl) DESC
        """)
    List<java.util.Map<String, Object>> getPrefectureStatistics();

    /**
     * 市区町村別勤務地統計
     * @return 市区町村別統計
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            CASE 
                WHEN wl.address LIKE '%市%' THEN SUBSTRING(wl.address, 1, POSITION('市' IN wl.address))
                WHEN wl.address LIKE '%区%' THEN SUBSTRING(wl.address, 1, POSITION('区' IN wl.address))
                WHEN wl.address LIKE '%町%' THEN SUBSTRING(wl.address, 1, POSITION('町' IN wl.address))
                ELSE 'その他'
            END as city,
            COUNT(wl) as locationCount
        FROM work_locations wl 
        WHERE wl.address IS NOT NULL
        GROUP BY CASE 
            WHEN wl.address LIKE '%市%' THEN SUBSTRING(wl.address, 1, POSITION('市' IN wl.address))
            WHEN wl.address LIKE '%区%' THEN SUBSTRING(wl.address, 1, POSITION('区' IN wl.address))
            WHEN wl.address LIKE '%町%' THEN SUBSTRING(wl.address, 1, POSITION('町' IN wl.address))
            ELSE 'その他'
        END
        ORDER BY COUNT(wl) DESC
        """)
    List<java.util.Map<String, Object>> getCityStatistics();

    /**
     * 重複名チェック
     * @param name 勤務地名
     * @param excludeId 除外するID（更新時）
     * @return 重複する勤務地があるかどうか
     */
    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(wl) > 0 THEN true ELSE false END FROM work_locations wl WHERE wl.name = :name AND (:excludeId IS NULL OR wl.id != :excludeId)")
    boolean existsByNameExcludingId(@Param("name") String name, @Param("excludeId") Integer excludeId);

    /**
     * 重複座標チェック
     * @param coordinates 座標（WKT形式）
     * @param excludeId 除外するID（更新時）
     * @return 重複する座標があるかどうか
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM work_locations WHERE ST_Equals(coordinates, ST_GeomFromText(:coordinates, 4326)) AND (:excludeId IS NULL OR id != :excludeId)", nativeQuery = true)
    boolean existsByCoordinatesExcludingId(@Param("coordinates") String coordinates, @Param("excludeId") Integer excludeId);

    /**
     * 地域クラスター分析
     * 座標がある勤務地を地域ごとにグループ化
     * @param clusterDistance クラスター判定距離（メートル）
     * @return 地域クラスター情報
     */
    @Query(value = """
        SELECT 
            ST_ClusterKMeans(coordinates, 5) OVER() as cluster_id,
            id,
            name,
            ST_X(coordinates) as longitude,
            ST_Y(coordinates) as latitude
        FROM work_locations 
        WHERE coordinates IS NOT NULL 
        AND is_active = true
        ORDER BY cluster_id, name
        """, nativeQuery = true)
    List<Object[]> getLocationClusters();

    /**
     * バッチ処理用：大量データ勤務地取得
     * @param offset オフセット
     * @param limit 取得件数
     * @return 勤務地リスト
     */
    @Query(nativeQuery = true, value = "SELECT wl.* FROM work_locations wl ORDER BY wl.id ASC LIMIT :limit OFFSET :offset")
    List<WorkLocation> findForBatchProcessing(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 勤務地名一覧取得（アクティブのみ）
     * @return アクティブな勤務地名リスト
     */
    @Query(nativeQuery = true, value = "SELECT wl.name FROM work_locations wl WHERE wl.is_active = true ORDER BY wl.name")
    List<String> findActiveWorkLocationNames();

    /**
     * 座標の妥当性チェック
     * 日本国内の緯度経度範囲をチェック
     * @return 日本国外の座標を持つ勤務地リスト
     */
    @Query(value = """
        SELECT * FROM work_locations 
        WHERE coordinates IS NOT NULL 
        AND (ST_X(coordinates) < 129.0 
          OR ST_X(coordinates) > 146.0 
          OR ST_Y(coordinates) < 24.0 
          OR ST_Y(coordinates) > 46.0)
        """, nativeQuery = true)
    List<WorkLocation> findWorkLocationsWithInvalidCoordinates();

    /**
     * アクセス頻度統計準備
     * 勤務地別の利用統計を取得するための基礎データ
     * @return 勤務地基本情報
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            wl.id as locationId,
            wl.name as locationName,
            wl.is_active as isActive,
            wl.created_at as createdAt
        FROM work_locations wl 
        ORDER BY wl.name
        """)
    List<java.util.Map<String, Object>> getLocationBasicInfo();
}