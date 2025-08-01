package com.example.companybackend.controller;

import com.example.companybackend.entity.SystemLog;
import com.example.companybackend.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * システムログ管理コントローラー
 * comsys_dump.sql system_logsテーブル完全対応
 * 
 * エンドポイント：
 * - GET /api/system-logs - システムログ一覧取得
 * - GET /api/system-logs/{id} - システムログ詳細取得
 * - DELETE /api/system-logs/{id} - システムログ削除
 * - GET /api/system-logs/export/csv - システムログCSVエクスポート
 * - GET /api/system-logs/export/json - システムログJSONエクスポート
 * - GET /api/system-logs/statistics - システムログ統計情報取得
 * - GET /api/system-logs/search - システムログ検索
 */
@RestController
@RequestMapping("/api/system-logs")
@RequiredArgsConstructor
@Slf4j
public class SystemLogController {

    private final SystemLogRepository systemLogRepository;

    /**
     * システムログ一覧取得
     * 
     * @param page ページ番号 (デフォルト: 0)
     * @param size ページサイズ (デフォルト: 20)
     * @param action アクションフィルター (オプション)
     * @param status ステータスフィルター (オプション)
     * @param startDate 開始日時 (オプション)
     * @param endDate 終了日時 (オプション)
     * @return システムログ一覧
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        
        try {
            log.info("システムログ一覧取得: page={}, size={}, action={}, status={}, startDate={}, endDate={}", 
                    page, size, action, status, startDate, endDate);
            
            // ページパラメータのバリデーション
            int validPage = Math.max(0, page);
            int validSize = Math.max(1, Math.min(size, 100)); // サイズは1-100の範囲に制限
            
            Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<SystemLog> logsPage;
            
            // フィルター条件に基づいてログを取得
            if (action != null || status != null || startDate != null || endDate != null) {
                logsPage = systemLogRepository.findByFilters(action, status, startDate, endDate, pageable);
            } else {
                logsPage = systemLogRepository.findAll(pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "システムログ一覧の取得が完了しました");
            response.put("data", logsPage.getContent());
            response.put("currentPage", logsPage.getNumber());
            response.put("totalItems", logsPage.getTotalElements());
            response.put("totalPages", logsPage.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("システムログ一覧取得中にエラーが発生しました", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "システムログ一覧の取得に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * システムログ詳細取得
     * 
     * @param id システムログID
     * @return システムログ詳細情報
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemLogById(@PathVariable Long id) {
        try {
            log.info("システムログ詳細取得: id={}", id);
            
            Optional<SystemLog> systemLog = systemLogRepository.findById(id.intValue());
            
            if (systemLog.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "システムログ詳細の取得が完了しました");
                response.put("data", systemLog.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "指定されたシステムログが見つかりません");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (Exception e) {
            log.error("システムログ詳細取得中にエラーが発生しました: id={}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "システムログ詳細の取得に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * システムログ削除
     * 
     * @param id システムログID
     * @return 削除結果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteSystemLog(@PathVariable Long id) {
        try {
            log.info("システムログ削除: id={}", id);
            
            Optional<SystemLog> systemLog = systemLogRepository.findById(id.intValue());
            
            if (systemLog.isPresent()) {
                systemLogRepository.deleteById(id.intValue());
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "システムログを削除しました");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "指定されたシステムログが見つかりません");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (Exception e) {
            log.error("システムログ削除中にエラーが発生しました: id={}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "システムログの削除に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * システムログCSVエクスポート
     * 
     * @param action アクションフィルター (オプション)
     * @param status ステータスフィルター (オプション)
     * @param startDate 開始日時 (オプション)
     * @param endDate 終了日時 (オプション)
     * @return CSV形式のシステムログ
     */
    @GetMapping(value = "/export/csv", produces = "text/csv; charset=UTF-8")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> exportSystemLogsCsv(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        
        try {
            log.info("システムログCSVエクスポート: action={}, status={}, startDate={}, endDate={}", 
                    action, status, startDate, endDate);
            
            List<SystemLog> logs = systemLogRepository.findForExport(action, status, startDate, endDate);
            
            StringBuilder csvContent = new StringBuilder();
            // CSVヘッダー
            csvContent.append("ID,ユーザーID,アクション,ステータス,IPアドレス,ユーザーエージェント,詳細,作成日時\n");
            
            // CSVデータ
            for (SystemLog log : logs) {
                csvContent.append(String.format("%d,%s,%s,%s,%s,%s,%s,%s\n",
                        log.getId(),
                        log.getUserId() != null ? log.getUserId().toString() : "",
                        log.getAction(),
                        log.getStatus(),
                        log.getIpAddress() != null ? log.getIpAddress() : "",
                        log.getUserAgent() != null ? log.getUserAgent() : "",
                        log.getDetails() != null ? log.getDetails().replace(",", "、") : "",
                        log.getCreatedAt()));
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", "system_logs.csv");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvContent.toString());
        } catch (Exception e) {
            log.error("システムログCSVエクスポート中にエラーが発生しました", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("システムログのエクスポートに失敗しました");
        }
    }

    /**
     * システムログJSONエクスポート
     * 
     * @param action アクションフィルター (オプション)
     * @param status ステータスフィルター (オプション)
     * @param startDate 開始日時 (オプション)
     * @param endDate 終了日時 (オプション)
     * @return JSON形式のシステムログ
     */
    @GetMapping(value = "/export/json", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> exportSystemLogsJson(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        
        try {
            log.info("システムログJSONエクスポート: action={}, status={}, startDate={}, endDate={}", 
                    action, status, startDate, endDate);
            
            List<SystemLog> logs = systemLogRepository.findForExport(action, status, startDate, endDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "システムログのエクスポートが完了しました");
            response.put("data", logs);
            response.put("exportedAt", OffsetDateTime.now());
            response.put("count", logs.size());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", "system_logs.json");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(response);
        } catch (Exception e) {
            log.error("システムログJSONエクスポート中にエラーが発生しました", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "システムログのエクスポートに失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * システムログ統計情報取得
     * 
     * @param startDate 開始日時 (オプション)
     * @param endDate 終了日時 (オプション)
     * @return システムログ統計情報
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemLogStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        
        try {
            log.info("システムログ統計情報取得: startDate={}, endDate={}", startDate, endDate);
            
            // デフォルト期間を設定 (過去30日)
            OffsetDateTime now = OffsetDateTime.now();
            if (startDate == null) {
                startDate = now.minusDays(30);
            }
            if (endDate == null) {
                endDate = now;
            }
            
            Map<String, Object> statistics = new HashMap<>();
            
            // アクション別統計
            List<Map<String, Object>> actionStats = systemLogRepository.countByActionGrouped();
            statistics.put("actionStats", actionStats);
            
            // ステータス別統計
            List<Map<String, Object>> statusStats = systemLogRepository.countByStatusGrouped();
            statistics.put("statusStats", statusStats);
            
            // ユーザー別統計
            List<Map<String, Object>> userStats = systemLogRepository.countByUserGrouped();
            statistics.put("userStats", userStats);
            
            // 日別統計
            List<Map<String, Object>> dateStats = systemLogRepository.countByDateGrouped();
            statistics.put("dateStats", dateStats);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "システムログ統計情報の取得が完了しました");
            response.put("data", statistics);
            response.put("period", Map.of(
                "startDate", startDate,
                "endDate", endDate
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("システムログ統計情報取得中にエラーが発生しました", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "システムログ統計情報の取得に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * システムログ検索
     * 
     * @param keyword 検索キーワード
     * @param page ページ番号 (デフォルト: 0)
     * @param size ページサイズ (デフォルト: 20)
     * @return 検索結果
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> searchSystemLogs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            log.info("システムログ検索: keyword={}, page={}, size={}", keyword, page, size);
            
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<SystemLog> logsPage = systemLogRepository.searchByKeyword(keyword, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "システムログの検索が完了しました");
            response.put("data", logsPage.getContent());
            response.put("currentPage", logsPage.getNumber());
            response.put("totalItems", logsPage.getTotalElements());
            response.put("totalPages", logsPage.getTotalPages());
            response.put("keyword", keyword);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("システムログ検索中にエラーが発生しました: keyword={}", keyword, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "システムログの検索に失敗しました");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}