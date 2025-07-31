package com.example.companybackend.repository;

import com.example.companybackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByEmployeeId(String employeeId);
    
    Optional<User> findByUsername(String username);
    
    // 添加缺失的方法
    @Query(nativeQuery = true, value = "SELECT u.* FROM users u WHERE u.username = :username")
    Optional<User> findByUsernameForAuthentication(@Param("username") String username);
    
    boolean existsByUsername(String username);
    
    Page<User> findByIsActiveTrue(Pageable pageable);
    
    Page<User> findByIsActiveFalse(Pageable pageable);
    
    Page<User> findByDepartmentId(Integer departmentId, Pageable pageable);
    
    Page<User> findByRole(String role, Pageable pageable);
    
    // 添加不带Pageable的findByRole方法
    List<User> findByRole(String role);
    
    Page<User> findByLocationType(String locationType, Pageable pageable);
    
    Page<User> findByFullNameContainingOrUsernameContainingOrEmployeeIdContaining(
        String fullName, String username, String employeeId, Pageable pageable);
    
    @Query(nativeQuery = true, value = "SELECT u.* FROM users u WHERE " +
           "(:search IS NULL OR u.full_name LIKE CONCAT('%', :search, '%') OR u.username LIKE CONCAT('%', :search, '%') OR u.employee_id LIKE CONCAT('%', :search, '%')) AND " +
           "(:departmentId IS NULL OR u.department_id = :departmentId) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:locationType IS NULL OR u.location_type = :locationType) AND " +
           "(:isActive IS NULL OR u.is_active = :isActive)")
    Page<User> findEmployeesWithFilter(
        @Param("search") String search, 
        @Param("departmentId") Integer departmentId, 
        @Param("role") String role, 
        @Param("locationType") String locationType, 
        @Param("isActive") Boolean isActive, 
        Pageable pageable);
    
    long countByIsActiveTrue();
    
    long countByIsActiveFalse();
    
    long countByRole(String role);
    
    long countByLocationType(String locationType);
    
    @Query(nativeQuery = true, value = "SELECT u.department_id as departmentId, COUNT(u) as count FROM users u WHERE u.department_id IS NOT NULL GROUP BY u.department_id")
    List<Object[]> getDepartmentStatistics();
    
    List<User> findByCreatedAtAfterOrderByCreatedAtDesc(OffsetDateTime dateTime);
    
    List<User> findByUpdatedAtAfterOrderByUpdatedAtDesc(OffsetDateTime dateTime);
    
    @Query(nativeQuery = true, value = "SELECT u.* FROM users u WHERE EXTRACT(MONTH FROM u.created_at) = :month")
    List<User> findEmployeesWithBirthdayInMonth(@Param("month") int month);
}