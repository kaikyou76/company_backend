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
@CrossOrigin(origins = "*", maxAge = 3600)
public class TimeCorrectionController {

    private final TimeCorrectionService timeCorrectionService;

    /**
     * 打刻修正申請作成 API
     * POST /api/v1/time-corrections
     */
    @PostMapping
    public ResponseEntity<CreateTimeCorrectionResponse> createTimeCorrection(
            @Valid @RequestBody CreateTimeCorrectionRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("打刻修正申請作成リクエスト受信: requestType={}", request.getRequestType());
        
        try {
            CreateTimeCorrectionResponse response = timeCorrectionService.createTimeCorrection(request, userId);
            
            if (response.isSuccess()) {
                log.info("打刻修正申請作成成功: correctionId={}", response.getTimeCorrection().getId());
                return ResponseEntity.ok(response);
            } else {
                log.warn("打刻修正申請作成失敗: reason={}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("打刻修正申請作成中にエラーが発生: error={}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                CreateTimeCorrectionResponse.error("打刻修正申請の作成中にエラーが発生しました")
            );
        }
    }

    /**
     * 打刻修正申請承認 API
     * PUT /api/v1/time-corrections/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApproveTimeCorrectionResponse> approveTimeCorrection(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long approverId) {
        
        log.info("打刻修正申請承認API呼び出し: correctionId={}, approverId={}", id, approverId);
        
        try {
            ApproveTimeCorrectionResponse response = 
                    timeCorrectionService.approveTimeCorrection(id, approverId);
            
            if (response.isSuccess()) {
                log.info("打刻修正申請承認API成功: correctionId={}, approverId={}", id, approverId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("打刻修正申請承認API失敗: correctionId={}, approverId={}, error={}", 
                        id, approverId, response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("打刻修正申請承認API例外: correctionId={}, approverId={}", id, approverId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 打刻修正申請拒否 API
     * PUT /api/v1/time-corrections/{id}/reject
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<RejectTimeCorrectionResponse> rejectTimeCorrection(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long approverId) {
        
        log.info("打刻修正申請拒否API呼び出し: correctionId={}, approverId={}", id, approverId);
        
        try {
            RejectTimeCorrectionResponse response = 
                    timeCorrectionService.rejectTimeCorrection(id, approverId);
            
            if (response.isSuccess()) {
                log.info("打刻修正申請拒否API成功: correctionId={}, approverId={}", id, approverId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("打刻修正申請拒否API失敗: correctionId={}, approverId={}, error={}", 
                        id, approverId, response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("打刻修正申請拒否API例外: correctionId={}, approverId={}", id, approverId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ユーザーの打刻修正申請一覧取得 API
     * GET /api/v1/time-corrections/user
     */
    @GetMapping("/user")
    public ResponseEntity<UserTimeCorrectionListResponse> getUserTimeCorrections(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.debug("ユーザー打刻修正申請一覧API呼び出し: userId={}", userId);
        
        try {
            List<TimeCorrection> corrections = timeCorrectionService.getUserTimeCorrections(userId);
            
            UserTimeCorrectionListResponse response = new UserTimeCorrectionListResponse();
            response.setUserId(userId);
            response.setCorrections(corrections);
            response.setTotalCount(corrections.size());
            
            log.debug("ユーザー打刻修正申請一覧API成功: userId={}, count={}", userId, corrections.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("ユーザー打刻修正申請一覧API例外: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 承認待ち申請一覧取得 API
     * GET /api/v1/time-corrections/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<PendingTimeCorrectionListResponse> getPendingTimeCorrections() {
        
        log.debug("承認待ち申請一覧API呼び出し");
        
        try {
            List<TimeCorrection> corrections = timeCorrectionService.getPendingTimeCorrections();
            
            PendingTimeCorrectionListResponse response = new PendingTimeCorrectionListResponse();
            response.setCorrections(corrections);
            response.setTotalCount(corrections.size());
            
            log.debug("承認待ち申請一覧API成功: count={}", corrections.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("承認待ち申請一覧API例外", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 申請詳細取得 API
     * GET /api/v1/time-corrections/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TimeCorrection> getTimeCorrectionById(@PathVariable Long id) {
        
        log.debug("申請詳細取得API呼び出し: correctionId={}", id);
        
        try {
            Optional<TimeCorrection> correction = timeCorrectionService.getTimeCorrectionById(id);
            
            if (correction.isPresent()) {
                log.debug("申請詳細取得API成功: correctionId={}", id);
                return ResponseEntity.ok(correction.get());
            } else {
                log.warn("申請詳細取得API失敗: correctionId={} - 申請が見つかりません", id);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("申請詳細取得API例外: correctionId={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ユーザーの承認待ち申請数取得 API
     * GET /api/v1/time-corrections/user/pending-count
     */
    @GetMapping("/user/pending-count")
    public ResponseEntity<PendingCountResponse> getUserPendingCount(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.debug("ユーザー承認待ち申請数取得API呼び出し: userId={}", userId);
        
        try {
            long count = timeCorrectionService.getUserPendingCount(userId);
            
            PendingCountResponse response = new PendingCountResponse();
            response.setUserId(userId);
            response.setPendingCount(count);
            
            log.debug("ユーザー承認待ち申請数取得API成功: userId={}, count={}", userId, count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("ユーザー承認待ち申請数取得API例外: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 全体の承認待ち申請数取得 API
     * GET /api/v1/time-corrections/pending-count
     */
    @GetMapping("/pending-count")
    public ResponseEntity<AllPendingCountResponse> getAllPendingCount() {
        
        log.debug("全体承認待ち申請数取得API呼び出し");
        
        try {
            long count = timeCorrectionService.getAllPendingCount();
            
            AllPendingCountResponse response = new AllPendingCountResponse();
            response.setPendingCount(count);
            
            log.debug("全体承認待ち申請数取得API成功: count={}", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("全体承認待ち申請数取得API例外", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // レスポンスDTOクラス
    public static class UserTimeCorrectionListResponse {
        private Long userId;
        private List<TimeCorrection> corrections;
        private int totalCount;

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public List<TimeCorrection> getCorrections() { return corrections; }
        public void setCorrections(List<TimeCorrection> corrections) { this.corrections = corrections; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    }

    public static class PendingTimeCorrectionListResponse {
        private List<TimeCorrection> corrections;
        private int totalCount;

        // Getters and Setters
        public List<TimeCorrection> getCorrections() { return corrections; }
        public void setCorrections(List<TimeCorrection> corrections) { this.corrections = corrections; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    }

    public static class PendingCountResponse {
        private Long userId;
        private long pendingCount;

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public long getPendingCount() { return pendingCount; }
        public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
    }

    public static class AllPendingCountResponse {
        private long pendingCount;

        // Getters and Setters
        public long getPendingCount() { return pendingCount; }
        public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
    }
}