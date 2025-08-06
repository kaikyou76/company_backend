package com.example.companybackend.controller;

import com.example.companybackend.entity.LeaveRequest;
import com.example.companybackend.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 休暇申請管理コントローラー
 * API Endpoints:
 * - POST /api/leave/request
 * - GET /api/leave/my-requests
 * - POST /api/leave/{id}/approve
 * - GET /api/leave/balance
 * - GET /api/leave/calendar
 */
@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveRequestController {

    private static final Logger log = LoggerFactory.getLogger(LeaveRequestController.class);

    private final LeaveService leaveService;

    /**
     * 休暇申請 API 実装
     * POST /api/leave/request
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestLeave(
            @RequestBody LeaveRequestRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("休暇申請API呼び出し: userId={}, type={}, startDate={}, endDate={}", 
                userId, request.getLeaveType(), request.getStartDate(), request.getEndDate());
        
        try {
            LeaveRequest leaveRequest = leaveService.createLeaveRequest(
                    userId, 
                    request.getLeaveType(), 
                    request.getStartDate(), 
                    request.getEndDate(), 
                    request.getReason());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "休暇申請を作成しました");
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", leaveRequest.getId());
            data.put("leaveType", leaveRequest.getType());
            data.put("startDate", leaveRequest.getStartDate());
            data.put("endDate", leaveRequest.getEndDate());
            data.put("days", leaveRequest.getLeaveDays());
            data.put("approvalStatus", leaveRequest.getStatus().toUpperCase());
            // TODO: 残日数は後で計算する
            data.put("remainingPaidLeaveDays", 0);
            
            result.put("data", data);
            
            log.info("休暇申請API成功: userId={}, leaveRequestId={}", userId, leaveRequest.getId());
            return ResponseEntity.ok(result);
            
        } catch (IllegalStateException e) {
            log.warn("休暇申請API失敗（重複期間）: userId={}, error={}", userId, e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
            
        } catch (Exception e) {
            log.error("休暇申請API例外: userId={}", userId, e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "システムエラーが発生しました");
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 休暇申請一覧取得 API 実装
     * GET /api/leave/my-requests
     */
    @GetMapping("/my-requests")
    public ResponseEntity<Map<String, Object>> getLeaveRequests(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String leaveType,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("休暇申請一覧取得API呼び出し: userId={}, startDate={}, endDate={}, leaveType={}", 
                userId, startDate, endDate, leaveType);
        
        try {
            List<LeaveRequest> leaveRequests = leaveService.getUserLeaveRequests(userId, startDate, endDate, leaveType);
            
            List<Map<String, Object>> requestList = leaveRequests.stream().map(request -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", request.getId());
                item.put("leaveType", request.getType());
                item.put("leaveTypeDisplayName", getLeaveTypeDisplayName(request.getType()));
                item.put("startDate", request.getStartDate());
                item.put("endDate", request.getEndDate());
                item.put("days", request.getLeaveDays());
                item.put("approvalStatus", request.getStatus().toUpperCase());
                item.put("approvalStatusDisplayName", getStatusDisplayName(request.getStatus()));
                return item;
            }).collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            Map<String, Object> data = new HashMap<>();
            data.put("requests", requestList);
            data.put("totalCount", requestList.size());
            // TODO: 残日数は後で計算する
            data.put("remainingPaidLeaveDays", 0);
            
            result.put("data", data);
            
            log.info("休暇申請一覧取得API成功: userId={}, count={}", userId, requestList.size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("休暇申請一覧取得API例外: userId={}", userId, e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "システムエラーが発生しました");
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 休暇申請承認・却下 API 実装
     * POST /api/leave/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveLeaveRequest(
            @PathVariable Long id,
            @RequestBody LeaveApprovalRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("休暇申請承認API呼び出し: requestId={}, userId={}, action={}", 
                id, userId, request.getAction());
        
        try {
            LeaveRequest leaveRequest;
            if ("approve".equals(request.getAction())) {
                leaveRequest = leaveService.approveLeaveRequest(id, userId.intValue());
            } else if ("reject".equals(request.getAction())) {
                leaveRequest = leaveService.rejectLeaveRequest(id, userId);
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "無効なアクションです");
                return ResponseEntity.badRequest().body(result);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "approve".equals(request.getAction()) ? "休暇申請を承認しました" : "休暇申請を却下しました");
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", leaveRequest.getId());
            data.put("approvalStatus", leaveRequest.getStatus().toUpperCase());
            data.put("approvedBy", leaveRequest.getApproverId());
            data.put("approvedAt", leaveRequest.getApprovedAt());
            data.put("approvalComment", request.getComment());
            
            result.put("data", data);
            
            log.info("休暇申請承認API成功: requestId={}, userId={}, status={}", 
                    id, userId, leaveRequest.getStatus());
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("休暇申請承認API失敗（申請が見つからない）: requestId={}, userId={}, error={}", 
                    id, userId, e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
            
        } catch (Exception e) {
            log.error("休暇申請承認API例外: requestId={}, userId={}", id, userId, e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "システムエラーが発生しました");
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 休暇残日数取得 API 実装
     * GET /api/leave/balance
     */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getLeaveBalance(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("休暇残日数取得API呼び出し: userId={}", userId);
        
        try {
            // TODO: 実際の残日数計算ロジックを実装
            long remainingPaidLeaveDays = leaveService.calculateRemainingPaidLeaveDays(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            Map<String, Object> data = new HashMap<>();
            data.put("remainingPaidLeaveDays", remainingPaidLeaveDays);
            
            result.put("data", data);
            
            log.info("休暇残日数取得API成功: userId={}, remainingDays={}", userId, remainingPaidLeaveDays);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("休暇残日数取得API例外: userId={}", userId, e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "システムエラーが発生しました");
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 休暇カレンダー表示 API 実装
     * GET /api/leave/calendar
     */
    @GetMapping("/calendar")
    public ResponseEntity<Map<String, Object>> getLeaveCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("休暇カレンダー表示API呼び出し: userId={}, startDate={}, endDate={}", 
                userId, startDate, endDate);
        
        try {
            List<LeaveRequest> leaveRequests = leaveService.getUserLeaveRequests(userId, startDate, endDate, null);
            
            List<Map<String, Object>> calendarEvents = leaveRequests.stream().map(request -> {
                Map<String, Object> event = new HashMap<>();
                event.put("id", request.getId());
                event.put("title", getLeaveTypeDisplayName(request.getType()));
                event.put("startDate", request.getStartDate());
                event.put("endDate", request.getEndDate());
                event.put("type", request.getType());
                event.put("status", request.getStatus().toUpperCase());
                return event;
            }).collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            Map<String, Object> data = new HashMap<>();
            data.put("events", calendarEvents);
            data.put("startDate", startDate);
            data.put("endDate", endDate);
            
            result.put("data", data);
            
            log.info("休暇カレンダー表示API成功: userId={}, eventCount={}", userId, calendarEvents.size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("休暇カレンダー表示API例外: userId={}", userId, e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "システムエラーが発生しました");
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ヘルパーメソッド
    private String getLeaveTypeDisplayName(String type) {
        switch (type) {
            case "paid": return "有給休暇";
            case "sick": return "病欠";
            case "special": return "特別休暇";
            default: return type;
        }
    }

    private String getStatusDisplayName(String status) {
        switch (status) {
            case "pending": return "承認待ち";
            case "approved": return "承認済み";
            case "rejected": return "却下";
            default: return status;
        }
    }

    // リクエストDTO
    public static class LeaveRequestRequest {
        private String leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;
        private Long substituteUserId;
        private String emergencyContact;

        // Getters and Setters
        public String getLeaveType() {
            return leaveType;
        }

        public void setLeaveType(String leaveType) {
            this.leaveType = leaveType;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public Long getSubstituteUserId() {
            return substituteUserId;
        }

        public void setSubstituteUserId(Long substituteUserId) {
            this.substituteUserId = substituteUserId;
        }

        public String getEmergencyContact() {
            return emergencyContact;
        }

        public void setEmergencyContact(String emergencyContact) {
            this.emergencyContact = emergencyContact;
        }
    }

    public static class LeaveApprovalRequest {
        private String action; // "approve" or "reject"
        private String comment;

        // Getters and Setters
        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}