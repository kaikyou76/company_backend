package com.example.companybackend.controller;

import com.example.companybackend.entity.Department;
import com.example.companybackend.service.DepartmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * 获取所有部门列表
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<?> getAllDepartments() {
        try {
            List<Department> departments = departmentService.getAllDepartments();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", departments);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("获取部门列表失败"));
        }
    }

    /**
     * 根据ID获取特定部门
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getDepartmentById(@PathVariable Integer id) {
        try {
            Optional<Department> department = departmentService.getDepartmentById(id);
            
            if (department.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("部门不存在"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", department.get());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("获取部门信息失败"));
        }
    }

    /**
     * 创建新的部门
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody Department department) {
        try {
            // 验证必填字段
            if (department.getName() == null || department.getName().isBlank()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "部门名称不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            if (department.getCode() == null || department.getCode().isBlank()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "部门代码不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // 检查部门名称是否已存在
            if (departmentService.existsByName(department.getName())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "部门名称已存在");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // 检查部门代码是否已存在
            if (departmentService.existsByCode(department.getCode())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "部门代码已存在");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            Department createdDepartment = departmentService.createDepartment(department);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", createdDepartment.getId());
            responseData.put("name", createdDepartment.getName());
            responseData.put("code", createdDepartment.getCode());
            responseData.put("managerId", createdDepartment.getManagerId());
            responseData.put("createdAt", createdDepartment.getCreatedAt());
            responseData.put("updatedAt", createdDepartment.getUpdatedAt());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "部门创建成功");
            response.put("data", responseData);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("创建部门失败"));
        }
    }

    /**
     * 更新部门信息
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable Integer id, @RequestBody Department departmentDetails) {
        try {
            // 检查部门是否存在
            Optional<Department> existingDepartment = departmentService.getDepartmentById(id);
            if (existingDepartment.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("部门不存在"));
            }

            // 如果名称被修改，检查新名称是否已存在
            if (departmentDetails.getName() != null && 
                !departmentDetails.getName().equals(existingDepartment.get().getName()) &&
                departmentService.existsByName(departmentDetails.getName())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "部门名称已存在");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // 如果代码被修改，检查新代码是否已存在
            if (departmentDetails.getCode() != null && 
                !departmentDetails.getCode().equals(existingDepartment.get().getCode()) &&
                departmentService.existsByCode(departmentDetails.getCode())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "部门代码已存在");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            Department updatedDepartment = departmentService.updateDepartment(id, departmentDetails);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", updatedDepartment.getId());
            responseData.put("name", updatedDepartment.getName());
            responseData.put("code", updatedDepartment.getCode());
            responseData.put("managerId", updatedDepartment.getManagerId());
            responseData.put("createdAt", updatedDepartment.getCreatedAt());
            responseData.put("updatedAt", updatedDepartment.getUpdatedAt());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "部门信息更新成功");
            response.put("data", responseData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("更新部门信息失败"));
        }
    }

    /**
     * 删除部门
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable Integer id) {
        try {
            Optional<Department> department = departmentService.getDepartmentById(id);
            if (department.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("部门不存在"));
            }

            departmentService.deleteDepartment(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "部门删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("删除部门失败"));
        }
    }

    // 辅助方法
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
}