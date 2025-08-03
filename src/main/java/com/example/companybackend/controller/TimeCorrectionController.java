package com.example.companybackend.controller;

import com.example.companybackend.dto.request.CreateTimeCorrectionRequest;
import com.example.companybackend.dto.response.CreateTimeCorrectionResponse;
import com.example.companybackend.dto.response.ApproveTimeCorrectionResponse;
import com.example.companybackend.dto.response.RejectTimeCorrectionResponse;
import com.example.companybackend.entity.TimeCorrection;
import com.example.companybackend.service.TimeCorrectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

/**
 * 打刻修正申請コントローラー
 * Spring Boot 3.x対応 - Jakarta EE準拠
 * API Endpoints:
 * - POST /api/v1/time-corrections - 申請作成
 * - PUT /api/v1/time-corrections/{id}/approve - 申請承認
 * - PUT /api/v1/time-corrections/{id}/reject - 申請拒否
 * - GET /api/v1/time-corrections/user - ユーザー申請一覧
 * - GET /api/v1/time-corrections/pending - 承認待ち一覧
 * - GET /api/v1/time-corrections/{id} - 申請詳細
 * - GET /api/v1/time-corrections/user/pending-count - ユーザー承認待ち数
 * - GET /api/v1/time-corrections/pending-count - 全体承認待ち数
 */
@RestController
@RequestMapping("/api/v1/time-corrections")
@RequiredArgsConstructor
@Slf4j
public class TimeCorrectionController {

    private final TimeCorrectionService timeCorrectionService;

    /**
     * 打刻修正申請作成 API
     * POST /api/v1/time-corrections
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTimeCorrection(
            @Valid @RequestBody CreateTimeCorrectionRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("打刻修正申請作成リクエスト受信: requestType={}", request.getRequestType());
        
        Map<String, Object> response = new HashMap<>();
        try {
            CreateTimeCorrectionResponse serviceResponse = timeCorrectionService.createTimeCorrection(request, userId);
            
            if (serviceResponse.isSuccess()) {
                log.info("打刻修正申請作成成功: correctionId={}", serviceResponse.getTimeCorrection().getId());
                response.put("success", true);
                response.put("message", "打刻修正申請が正常に作成されました");
                response.put("data", serviceResponse.getTimeCorrection());
                return ResponseEntity.ok(response);
            } else {
                log.warn("打刻修正申請作成失敗: reason={}", serviceResponse.getMessage());
                response.put("success", false);
                response.put("message", serviceResponse.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("打刻修正申請作成中にエラーが発生: error={}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "打刻修正申請の作成中にエラーが発生しました");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 打刻修正申請承認 API
     * PUT /api/v1/time-corrections/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveTimeCorrection(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long approverId) {
        
        log.info("打刻修正申請承認API呼び出し: correctionId={}, approverId={}", id, approverId);
        
        Map<String, Object> response = new HashMap<>();
        try {
            ApproveTimeCorrectionResponse serviceResponse = 
                    timeCorrectionService.approveTimeCorrection(id, approverId);
            
            if (serviceResponse.isSuccess()) {
                log.info("打刻修正申請承認API成功: correctionId={}, approverId={}", id, approverId);
                response.put("success", true);
                response.put("message", "打刻修正申請が承認されました");
                response.put("data", serviceResponse.getCorrection());
                return ResponseEntity.ok(response);
            } else {
                log.warn("打刻修正申請承認API失敗: correctionId={}, approverId={}, error={}", 
                        id, approverId, serviceResponse.getMessage());
                response.put("success", false);
                response.put("message", serviceResponse.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("打刻修正申請承認API例外: correctionId={}, approverId={}", id, approverId, e);
            response.put("success", false);
            response.put("message", "打刻修正申請の承認中にエラーが発生しました");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 打刻修正申請拒否 API
     * PUT /api/v1/time-corrections/{id}/reject
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectTimeCorrection(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long approverId) {
        
        log.info("打刻修正申請拒否API呼び出し: correctionId={}, approverId={}", id, approverId);
        
        Map<String, Object> response = new HashMap<>();
        try {
            RejectTimeCorrectionResponse serviceResponse = 
                    timeCorrectionService.rejectTimeCorrection(id, approverId);
            
            if (serviceResponse.isSuccess()) {
                log.info("打刻修正申請拒否API成功: correctionId={}, approverId={}", id, approverId);
                response.put("success", true);
                response.put("message", "打刻修正申請が拒否されました");
                response.put("data", serviceResponse.getCorrection());
                return ResponseEntity.ok(response);
            } else {
                log.warn("打刻修正申請拒否API失敗: correctionId={}, approverId={}, error={}", 
                        id, approverId, serviceResponse.getMessage());
                response.put("success", false);
                response.put("message", serviceResponse.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("打刻修正申請拒否API例外: correctionId={}, approverId={}", id, approverId, e);
            response.put("success", false);
            response.put("message", "打刻修正申請の拒否中にエラーが発生しました");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * ユーザーの打刻修正申請一覧取得 API
     * GET /api/v1/time-corrections/user
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUserTimeCorrections(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.debug("ユーザー打刻修正申請一覧API呼び出し: userId={}", userId);
        
        Map<String, Object> response = new HashMap<>();
        try {
            List<TimeCorrection> corrections = timeCorrectionService.getUserTimeCorrections(userId);
            
            response.put("success", true);
            response.put("message", "ユーザーの打刻修正申請一覧を取得しました");
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("corrections", corrections);
            data.put("totalCount", corrections.size());
            response.put("data", data);
            
            log.debug("ユーザー打刻修正申請一覧API成功: userId={}, count={}", userId, corrections.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("ユーザー打刻修正申請一覧API例外: userId={}", userId, e);
            response.put("success", false);
            response.put("message", "ユーザーの打刻修正申請一覧取得中にエラーが発生しました");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 承認待ち申請一覧取得 API
     * GET /api/v1/time-corrections/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingTimeCorrections() {
        
        log.debug("承認待ち申請一覧API呼び出し");
        
        Map<String, Object> response = new HashMap<>();
        try {
            List<TimeCorrection> corrections = timeCorrectionService.getPendingTimeCorrections();
            
            response.put("success", true);
            response.put("message", "承認待ち申請一覧を取得しました");
            Map<String, Object> data = new HashMap<>();
            data.put("corrections", corrections);
            data.put("totalCount", corrections.size());
            response.put("data", data);
            
            log.debug("承認待ち申請一覧API成功: count={}", corrections.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("承認待ち申請一覧API例外", e);
            response.put("success", false);
            response.put("message", "承認待ち申請一覧取得中にエラーが発生しました");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 申請詳細取得 API
     * GET /api/v1/time-corrections/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTimeCorrectionById(@PathVariable Long id) {
        
        log.debug("申請詳細取得API呼び出し: correctionId={}", id);
        
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<TimeCorrection> correction = timeCorrectionService.getTimeCorrectionById(id);
            
            if (correction.isPresent()) {
                log.debug("申請詳細取得API成功: correctionId={}", id);
                response.put("success", true);
                response.put("message", "申請詳細を取得しました");
                response.put("data", correction.get());
                return ResponseEntity.ok(response);
            } else {
                log.warn("申請詳細取得API失敗: correctionId={} - 申請が見つかりません", id);
                response.put("success", false);
                response.put("message", "申請が見つかりません");
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            log.error("申請詳細取得API例外: correctionId={}", id, e);
            response.put("success", false);
            response.put("message", "申請詳細取得中にエラーが発生しました");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * ユーザーの承認待ち申請数取得 API
     * GET /api/v1/time-corrections/user/pending-count
     */
    @GetMapping("/user/pending-count")
    public ResponseEntity<Map<String, Object>> getUserPendingCount(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.debug("ユーザー承認待ち申請数取得API呼び出し: userId={}", userId);
        
        Map<String, Object> response = new HashMap<>();
        try {
            long count = timeCorrectionService.getUserPendingCount(userId);
            
            response.put("success", true);
            response.put("message", "ユーザーの承認待ち申請数を取得しました");
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("pendingCount", count);
            response.put("data", data);
            
            log.debug("ユーザー承認待ち申請数取得API成功: userId={}, count={}", userId, count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("ユーザー承認待ち申請数取得API例外: userId={}", userId, e);
            response.put("success", false);
            response.put("message", "ユーザーの承認待ち申請数取得中にエラーが発生しました");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 全体の承認待ち申請数取得 API
     * GET /api/v1/time-corrections/pending-count
     */
    @GetMapping("/pending-count")
    public ResponseEntity<Map<String, Object>> getAllPendingCount() {
        
        log.debug("全体承認待ち申請数取得API呼び出し");
        
        Map<String, Object> response = new HashMap<>();
        try {
            long count = timeCorrectionService.getAllPendingCount();
            
            response.put("success", true);
            response.put("message", "全体の承認待ち申請数を取得しました");
            Map<String, Object> data = new HashMap<>();
            data.put("pendingCount", count);
            response.put("data", data);
            
            log.debug("全体承認待ち申請数取得API成功: count={}", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("全体承認待ち申請数取得API例外", e);
            response.put("success", false);
            response.put("message", "全体の承認待ち申請数取得中にエラーが発生しました");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}