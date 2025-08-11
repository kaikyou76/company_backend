package com.example.companybackend.controller;

import com.example.companybackend.entity.Position;
import com.example.companybackend.service.PositionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    /**
     * 获取所有职位列表
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<?> getAllPositions() {
        try {
            List<Position> positions = positionService.getAllPositions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", positions);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("获取职位列表失败"));
        }
    }

    /**
     * 根据ID获取特定职位
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getPositionById(@PathVariable Integer id) {
        try {
            Optional<Position> position = positionService.getPositionById(id);
            
            if (position.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("职位不存在"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", position.get());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("获取职位信息失败"));
        }
    }

    /**
     * 创建新的职位
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createPosition(@RequestBody Position position) {
        try {
            // 验证必填字段
            if (position.getName() == null || position.getName().isBlank()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "职位名称不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // 检查职位名称是否已存在
            if (positionService.existsByName(position.getName())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "职位名称已存在");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            Position createdPosition = positionService.createPosition(position);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", createdPosition.getId());
            responseData.put("name", createdPosition.getName());
            responseData.put("level", createdPosition.getLevel());
            responseData.put("createdAt", createdPosition.getCreatedAt());
            responseData.put("updatedAt", createdPosition.getUpdatedAt());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "职位创建成功");
            response.put("data", responseData);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("创建职位失败"));
        }
    }

    /**
     * 更新职位信息
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePosition(@PathVariable Integer id, @RequestBody Position positionDetails) {
        try {
            // 检查职位是否存在
            Optional<Position> existingPosition = positionService.getPositionById(id);
            if (existingPosition.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("职位不存在"));
            }

            // 如果名称被修改，检查新名称是否已存在
            if (positionDetails.getName() != null && 
                !positionDetails.getName().equals(existingPosition.get().getName()) &&
                positionService.existsByName(positionDetails.getName())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "职位名称已存在");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            Position updatedPosition = positionService.updatePosition(id, positionDetails);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", updatedPosition.getId());
            responseData.put("name", updatedPosition.getName());
            responseData.put("level", updatedPosition.getLevel());
            responseData.put("createdAt", updatedPosition.getCreatedAt());
            responseData.put("updatedAt", updatedPosition.getUpdatedAt());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "职位信息更新成功");
            response.put("data", responseData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("更新职位信息失败"));
        }
    }

    /**
     * 删除职位
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePosition(@PathVariable Integer id) {
        try {
            Optional<Position> position = positionService.getPositionById(id);
            if (position.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("职位不存在"));
            }

            positionService.deletePosition(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "职位删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("删除职位失败"));
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