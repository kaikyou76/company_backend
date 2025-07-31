package com.example.companybackend.query;

import com.example.companybackend.entity.User;
import com.example.companybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 従業員クエリ処理サービス
 * QRY-001: Employee Query Models実装
 * 
 * comsys_dump.sql完全準拠:
 * - Enum使用禁止 - 全てString型で処理
 * - Database First原則
 * - 単純なエンティティ設計
 * 
 * 機能:
 * - 従業員検索・フィルタリング
 * - 統計情報取得
 * - レポート生成
 * - ページネーション対応
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class EmployeeQueries {

    private final UserRepository userRepository;

    /**
     * 従業員詳細取得
     * @param userId ユーザーID
     * @return 従業員詳細
     */
    public Optional<EmployeeDetailView> getEmployeeDetail(Long userId) {
        log.debug("従業員詳細取得: userId={}", userId);
        
        return userRepository.findById(userId)
            .map(this::mapToEmployeeDetailView);
    }

    /**
     * 従業員ID別詳細取得
     * @param employeeId 従業員ID
     * @return 従業員詳細
     */
    public Optional<EmployeeDetailView> getEmployeeDetailByEmployeeId(String employeeId) {
        log.debug("従業員詳細取得: employeeId={}", employeeId);
        
        return userRepository.findByEmployeeId(employeeId)
            .map(this::mapToEmployeeDetailView);
    }

    /**
     * 全従業員一覧取得
     * @param pageable ページング情報
     * @return 従業員一覧
     */
    public Page<EmployeeListView> getAllEmployees(Pageable pageable) {
        log.debug("全従業員一覧取得: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        return userRepository.findAll(pageable)
            .map(this::mapToEmployeeListView);
    }

    /**
     * アクティブ従業員一覧取得
     * @param pageable ページング情報
     * @return アクティブ従業員一覧
     */
    public Page<EmployeeListView> getActiveEmployees(Pageable pageable) {
        log.debug("アクティブ従業員一覧取得: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        return userRepository.findByIsActiveTrue(pageable)
            .map(this::mapToEmployeeListView);
    }

    /**
     * 非アクティブ従業員一覧取得
     * @param pageable ページング情報
     * @return 非アクティブ従業員一覧
     */
    public Page<EmployeeListView> getInactiveEmployees(Pageable pageable) {
        log.debug("非アクティブ従業員一覧取得: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        return userRepository.findByIsActiveFalse(pageable)
            .map(this::mapToEmployeeListView);
    }

    /**
     * 部署別従業員一覧取得
     * @param departmentId 部署ID
     * @param pageable ページング情報
     * @return 部署別従業員一覧
     */
    public Page<EmployeeListView> getEmployeesByDepartment(Integer departmentId, Pageable pageable) {
        log.debug("部署別従業員一覧取得: departmentId={}, page={}, size={}", 
                departmentId, pageable.getPageNumber(), pageable.getPageSize());
        
        return userRepository.findByDepartmentId(departmentId, pageable)
            .map(this::mapToEmployeeListView);
    }

    /**
     * 役職別従業員一覧取得
     * @param role 役職
     * @param pageable ページング情報
     * @return 役職別従業員一覧
     */
    public Page<EmployeeListView> getEmployeesByRole(String role, Pageable pageable) {
        log.debug("役職別従業員一覧取得: role={}, page={}, size={}", 
                role, pageable.getPageNumber(), pageable.getPageSize());
        
        return userRepository.findByRole(role, pageable)
            .map(this::mapToEmployeeListView);
    }

    /**
     * 勤務地タイプ別従業員一覧取得
     * @param locationType 勤務地タイプ
     * @param pageable ページング情報
     * @return 勤務地タイプ別従業員一覧
     */
    public Page<EmployeeListView> getEmployeesByLocationType(String locationType, Pageable pageable) {
        log.debug("勤務地タイプ別従業員一覧取得: locationType={}, page={}, size={}", 
                locationType, pageable.getPageNumber(), pageable.getPageSize());
        
        return userRepository.findByLocationType(locationType, pageable)
            .map(this::mapToEmployeeListView);
    }

    /**
     * 従業員検索
     * @param keyword 検索キーワード
     * @param pageable ページング情報
     * @return 検索結果
     */
    public Page<EmployeeListView> searchEmployees(String keyword, Pageable pageable) {
        log.debug("従業員検索: keyword={}, page={}, size={}", 
                keyword, pageable.getPageNumber(), pageable.getPageSize());
        
        return userRepository.findByFullNameContainingOrUsernameContainingOrEmployeeIdContaining(
                keyword, keyword, keyword, pageable)
            .map(this::mapToEmployeeListView);
    }

    /**
     * 複合条件検索
     * @param filter 検索フィルター
     * @param pageable ページング情報
     * @return 検索結果
     */
    public Page<EmployeeListView> searchEmployeesWithFilter(EmployeeSearchFilter filter, Pageable pageable) {
        log.debug("複合条件検索: filter={}", filter);
        
        return userRepository.findEmployeesWithFilter(
                filter.getKeyword(),
                filter.getDepartmentId(),
                filter.getRole(),
                filter.getLocationType(),
                filter.getIsActive(),
                pageable)
            .map(this::mapToEmployeeListView);
    }

    /**
     * 従業員統計情報取得
     * @return 統計情報
     */
    public EmployeeStatistics getEmployeeStatistics() {
        log.debug("従業員統計情報取得");
        
        EmployeeStatistics stats = new EmployeeStatistics();
        
        // 基本統計
        stats.setTotalEmployees(userRepository.count());
        stats.setActiveEmployees(userRepository.countByIsActiveTrue());
        stats.setInactiveEmployees(userRepository.countByIsActiveFalse());
        
        // 役職別統計
        stats.setAdminCount(userRepository.countByRole("admin"));
        stats.setManagerCount(userRepository.countByRole("manager"));
        stats.setEmployeeCount(userRepository.countByRole("employee"));
        
        // 勤務地別統計
        stats.setOfficeEmployees(userRepository.countByLocationType("office"));
        stats.setClientEmployees(userRepository.countByLocationType("client"));
        
        return stats;
    }

    /**
     * 部署別統計取得
     * @return 部署別統計リスト
     */
    public List<DepartmentStatistics> getDepartmentStatistics() {
        log.debug("部署別統計取得");
        
        List<Object[]> results = userRepository.getDepartmentStatistics();
        return results.stream()
            .map(obj -> {
                DepartmentStatistics stat = new DepartmentStatistics();
                stat.setDepartmentId((Integer) obj[0]);
                stat.setEmployeeCount((Long) obj[1]);
                return stat;
            })
            .collect(Collectors.toList());
    }

    /**
     * 最近作成された従業員取得
     * @param days 過去何日以内
     * @return 最近の従業員リスト
     */
    public List<EmployeeListView> getRecentEmployees(int days) {
        log.debug("最近作成された従業員取得: days={}", days);
        
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(days);
        return userRepository.findByCreatedAtAfterOrderByCreatedAtDesc(cutoffDate)
            .stream()
            .map(this::mapToEmployeeListView)
            .toList();
    }

    /**
     * 最近更新された従業員取得
     * @param days 過去何日以内
     * @return 最近更新された従業員リスト
     */
    public List<EmployeeListView> getRecentlyUpdatedEmployees(int days) {
        log.debug("最近更新された従業員取得: days={}", days);
        
        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(days);
        return userRepository.findByUpdatedAtAfterOrderByUpdatedAtDesc(cutoffDate)
            .stream()
            .map(this::mapToEmployeeListView)
            .toList();
    }

    /**
     * 誕生日の従業員取得（今月）
     * @return 今月誕生日の従業員リスト
     */
    public List<EmployeeListView> getBirthdayEmployeesThisMonth() {
        log.debug("今月誕生日の従業員取得");
        
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        
        return userRepository.findEmployeesWithBirthdayInMonth(currentMonth)
            .stream()
            .map(this::mapToEmployeeListView)
            .toList();
    }

    /**
     * 管理者権限を持つ従業員取得
     * @return 管理者リスト
     */
    public List<EmployeeListView> getAdminEmployees() {
        log.debug("管理者権限従業員取得");
        
        return userRepository.findByRole("admin")
            .stream()
            .map(this::mapToEmployeeListView)
            .toList();
    }

    /**
     * マネージャー権限を持つ従業員取得
     * @return マネージャーリスト
     */
    public List<EmployeeListView> getManagerEmployees() {
        log.debug("マネージャー権限従業員取得");
        
        return userRepository.findByRole("manager")
            .stream()
            .map(this::mapToEmployeeListView)
            .toList();
    }

    /**
     * UserエンティティをEmployeeDetailViewにマップ
     */
    private EmployeeDetailView mapToEmployeeDetailView(User user) {
        EmployeeDetailView view = new EmployeeDetailView();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setEmail(user.getEmail());
        view.setEmployeeId(user.getEmployeeId());
        view.setFullName(user.getFullName());
        view.setDepartmentId(user.getDepartmentId());
        view.setRole(user.getRole());
        view.setRoleDisplayName(user.getRoleDisplayName());
        view.setLocationType(user.getLocationType());
        view.setLocationTypeDisplayName(user.getLocationTypeDisplayName());
        view.setIsActive(user.getIsActive());
        view.setCreatedAt(user.getCreatedAt());
        view.setUpdatedAt(user.getUpdatedAt());
        return view;
    }

    /**
     * UserエンティティをEmployeeListViewにマップ
     */
    private EmployeeListView mapToEmployeeListView(User user) {
        EmployeeListView view = new EmployeeListView();
        view.setId(user.getId());
        view.setEmployeeId(user.getEmployeeId());
        view.setFullName(user.getFullName());
        view.setDepartmentId(user.getDepartmentId());
        view.setRole(user.getRole());
        view.setRoleDisplayName(user.getRoleDisplayName());
        view.setLocationType(user.getLocationType());
        view.setLocationTypeDisplayName(user.getLocationTypeDisplayName());
        view.setIsActive(user.getIsActive());
        return view;
    }

    /**
     * 従業員詳細ビューモデル
     */
    public static class EmployeeDetailView {
        private Long id;
        private String username;
        private String email;
        private String employeeId;
        private String fullName;
        private Integer departmentId;
        private String role;
        private String roleDisplayName;
        private String locationType;
        private String locationTypeDisplayName;
        private Boolean isActive;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        
        public Integer getDepartmentId() { return departmentId; }
        public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getRoleDisplayName() { return roleDisplayName; }
        public void setRoleDisplayName(String roleDisplayName) { this.roleDisplayName = roleDisplayName; }
        
        public String getLocationType() { return locationType; }
        public void setLocationType(String locationType) { this.locationType = locationType; }
        
        public String getLocationTypeDisplayName() { return locationTypeDisplayName; }
        public void setLocationTypeDisplayName(String locationTypeDisplayName) { this.locationTypeDisplayName = locationTypeDisplayName; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    /**
     * 従業員リストビューモデル
     */
    public static class EmployeeListView {
        private Long id;
        private String employeeId;
        private String fullName;
        private Integer departmentId;
        private String role;
        private String roleDisplayName;
        private String locationType;
        private String locationTypeDisplayName;
        private Boolean isActive;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        
        public Integer getDepartmentId() { return departmentId; }
        public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getRoleDisplayName() { return roleDisplayName; }
        public void setRoleDisplayName(String roleDisplayName) { this.roleDisplayName = roleDisplayName; }
        
        public String getLocationType() { return locationType; }
        public void setLocationType(String locationType) { this.locationType = locationType; }
        
        public String getLocationTypeDisplayName() { return locationTypeDisplayName; }
        public void setLocationTypeDisplayName(String locationTypeDisplayName) { this.locationTypeDisplayName = locationTypeDisplayName; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }

    /**
     * 従業員検索フィルター
     */
    public static class EmployeeSearchFilter {
        private String keyword;
        private Integer departmentId;
        private String role;
        private String locationType;
        private Boolean isActive;

        // Getters and Setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        
        public Integer getDepartmentId() { return departmentId; }
        public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getLocationType() { return locationType; }
        public void setLocationType(String locationType) { this.locationType = locationType; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }

        @Override
        public String toString() {
            return String.format("EmployeeSearchFilter{keyword='%s', departmentId=%d, role='%s', locationType='%s', isActive=%s}",
                    keyword, departmentId, role, locationType, isActive);
        }
    }

    /**
     * 従業員統計情報
     */
    public static class EmployeeStatistics {
        private Long totalEmployees;
        private Long activeEmployees;
        private Long inactiveEmployees;
        private Long adminCount;
        private Long managerCount;
        private Long employeeCount;
        private Long officeEmployees;
        private Long clientEmployees;

        // Getters and Setters
        public Long getTotalEmployees() { return totalEmployees; }
        public void setTotalEmployees(Long totalEmployees) { this.totalEmployees = totalEmployees; }
        
        public Long getActiveEmployees() { return activeEmployees; }
        public void setActiveEmployees(Long activeEmployees) { this.activeEmployees = activeEmployees; }
        
        public Long getInactiveEmployees() { return inactiveEmployees; }
        public void setInactiveEmployees(Long inactiveEmployees) { this.inactiveEmployees = inactiveEmployees; }
        
        public Long getAdminCount() { return adminCount; }
        public void setAdminCount(Long adminCount) { this.adminCount = adminCount; }
        
        public Long getManagerCount() { return managerCount; }
        public void setManagerCount(Long managerCount) { this.managerCount = managerCount; }
        
        public Long getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(Long employeeCount) { this.employeeCount = employeeCount; }
        
        public Long getOfficeEmployees() { return officeEmployees; }
        public void setOfficeEmployees(Long officeEmployees) { this.officeEmployees = officeEmployees; }
        
        public Long getClientEmployees() { return clientEmployees; }
        public void setClientEmployees(Long clientEmployees) { this.clientEmployees = clientEmployees; }
    }

    /**
     * 部署別統計情報
     */
    public static class DepartmentStatistics {
        private Integer departmentId;
        private Long employeeCount;
        private Long activeCount;
        private Long inactiveCount;

        // Getters and Setters
        public Integer getDepartmentId() { return departmentId; }
        public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
        
        public Long getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(Long employeeCount) { this.employeeCount = employeeCount; }
        
        public Long getActiveCount() { return activeCount; }
        public void setActiveCount(Long activeCount) { this.activeCount = activeCount; }
        
        public Long getInactiveCount() { return inactiveCount; }
        public void setInactiveCount(Long inactiveCount) { this.inactiveCount = inactiveCount; }
    }
}