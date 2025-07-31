package com.example.companybackend.service;

import com.example.companybackend.dto.request.CreateTimeCorrectionRequest;
import com.example.companybackend.dto.response.CreateTimeCorrectionResponse;
import com.example.companybackend.dto.response.ApproveTimeCorrectionResponse;
import com.example.companybackend.dto.response.RejectTimeCorrectionResponse;
import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.entity.TimeCorrection;
import com.example.companybackend.entity.User;
import com.example.companybackend.repository.AttendanceRecordRepository;
import com.example.companybackend.repository.TimeCorrectionRepository;
import com.example.companybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 打刻修正申請サービス
 * TIME-CORR-SVC-001: 打刻修正申請Service実装
 * 
 * comsys_dump.sql完全準拠:
 * - Enum使用禁止 - 全てString型で処理
 * - Database First原則
 * - 単純なエンティティ設計
 * 
 * 機能:
 * - 申請作成
 * - 申請承認
 * - 申請拒否
 * - 申請一覧取得
 * - 申請詳細取得
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TimeCorrectionService {

    private final TimeCorrectionRepository timeCorrectionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;

    /**
     * 打刻修正申請作成
     */
    public CreateTimeCorrectionResponse createTimeCorrection(CreateTimeCorrectionRequest request, Long userId) {
        log.info("打刻修正申請作成開始: userId={}, requestType={}", userId, request.getRequestType());
        
        try {
            // ユーザー存在確認
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + userId));

            // 対象となる打刻記録の存在確認
            AttendanceRecord attendanceRecord = attendanceRecordRepository.findById(request.getAttendanceId())
                .orElseThrow(() -> new IllegalArgumentException("対象となる打刻記録が見つかりません"));
            
            // 打刻記録がユーザー自身のものか確認
            if (!attendanceRecord.getUserId().equals(userId.intValue())) {
                throw new IllegalArgumentException("他人の打刻記録は修正できません");
            }

            // 申請タイプのバリデーション
            validateCorrectionRequest(request);
            
            // 申請作成
            TimeCorrection timeCorrection = new TimeCorrection();
            timeCorrection.setUserId(userId.intValue());
            timeCorrection.setAttendanceId(request.getAttendanceId());
            timeCorrection.setRequestType(request.getRequestType());
            timeCorrection.setBeforeTime(attendanceRecord.getTimestamp());
            timeCorrection.setCurrentType(request.getCurrentType());
            timeCorrection.setRequestedTime(request.getRequestedTime());
            timeCorrection.setRequestedType(request.getRequestedType());
            timeCorrection.setReason(request.getReason());
            timeCorrection.setStatus("pending");
            timeCorrection.setCreatedAt(OffsetDateTime.now());

            TimeCorrection savedCorrection = timeCorrectionRepository.save(timeCorrection);
            log.info("打刻修正申請作成完了: correctionId={}", savedCorrection.getId());

            return CreateTimeCorrectionResponse.success(savedCorrection);
            
        } catch (Exception e) {
            log.error("打刻修正申請作成エラー: userId={}, error={}", userId, e.getMessage(), e);
            return CreateTimeCorrectionResponse.error(e.getMessage());
        }
    }

    /**
     * 申請のバリデーション
     * @param request 申請リクエスト
     */
    private void validateCorrectionRequest(CreateTimeCorrectionRequest request) {
        // 必須項目チェック
        if (request.getRequestType() == null || request.getRequestType().isEmpty()) {
            throw new IllegalArgumentException("申請タイプは必須です");
        }

        if (request.getCurrentType() == null || request.getCurrentType().isEmpty()) {
            throw new IllegalArgumentException("現在の打刻タイプは必須です");
        }

        if (request.getReason() == null || request.getReason().isEmpty()) {
            throw new IllegalArgumentException("修正理由は必須です");
        }

        // 申請タイプごとのバリデーション
        switch (request.getRequestType()) {
            case "time":
                if (request.getRequestedTime() == null) {
                    throw new IllegalArgumentException("時刻修正の場合は修正時刻の指定が必要です");
                }
                break;
                
            case "type":
                if (request.getRequestedType() == null || request.getRequestedType().isEmpty()) {
                    throw new IllegalArgumentException("タイプ修正の場合は修正タイプの指定が必要です");
                }
                break;
                
            case "both":
                if (request.getRequestedTime() == null) {
                    throw new IllegalArgumentException("時刻・タイプ修正の場合は修正時刻の指定が必要です");
                }
                if (request.getRequestedType() == null || request.getRequestedType().isEmpty()) {
                    throw new IllegalArgumentException("時刻・タイプ修正の場合は修正タイプの指定が必要です");
                }
                break;
                
            default:
                throw new IllegalArgumentException("無効な申請タイプです: " + request.getRequestType());
        }
    }

    /**
     * 打刻修正申請承認
     */
    public ApproveTimeCorrectionResponse approveTimeCorrection(Long correctionId, Long approverId) {
        log.info("打刻修正申請承認開始: correctionId={}, approverId={}", correctionId, approverId);
        
        try {
            // 申請の存在確認
            TimeCorrection correction = timeCorrectionRepository.findById(correctionId)
                .orElseThrow(() -> new IllegalArgumentException("申請が見つかりません"));

            // 申請が承認可能な状態か確認
            if (!"pending".equals(correction.getStatus())) {
                throw new IllegalStateException("申請は既に処理済みです");
            }

            // 承認者情報取得
            User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new IllegalArgumentException("承認者が見つかりません"));
            
            // 承認処理
            correction.setStatus("approved");
            correction.setApproverId(approverId.intValue());
            correction.setApprovedAt(OffsetDateTime.now());

            timeCorrectionRepository.save(correction);
            log.info("打刻修正申請承認完了: correctionId={}", correctionId);

            return new ApproveTimeCorrectionResponse(true, "申請を承認しました", correction);

        } catch (Exception e) {
            log.error("打刻修正申請承認エラー: correctionId={}, error={}", correctionId, e.getMessage(), e);
            return new ApproveTimeCorrectionResponse(false, e.getMessage(), null);
        }
    }

    /**
     * 打刻修正申請拒否
     */
    public RejectTimeCorrectionResponse rejectTimeCorrection(Long correctionId, Long approverId) {
        log.info("打刻修正申請拒否開始: correctionId={}, approverId={}", correctionId, approverId);
        
        try {
            // 申請の存在確認
            TimeCorrection correction = timeCorrectionRepository.findById(correctionId)
                .orElseThrow(() -> new IllegalArgumentException("申請が見つかりません"));

            // 申請が承認可能な状態か確認
            if (!"pending".equals(correction.getStatus())) {
                throw new IllegalStateException("申請は既に処理済みです");
            }

            // 承認者情報取得
            User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new IllegalArgumentException("承認者が見つかりません"));
            
            // 拒否処理
            correction.setStatus("rejected");
            correction.setApproverId(approverId.intValue());
            correction.setApprovedAt(OffsetDateTime.now());

            timeCorrectionRepository.save(correction);
            log.info("打刻修正申請拒否完了: correctionId={}", correctionId);

            return new RejectTimeCorrectionResponse(true, "申請を拒否しました", correction);

        } catch (Exception e) {
            log.error("打刻修正申請拒否エラー: correctionId={}, error={}", correctionId, e.getMessage(), e);
            return new RejectTimeCorrectionResponse(false, e.getMessage(), null);
        }
    }

    /**
     * ユーザーの打刻修正申請一覧取得
     */
    @Transactional(readOnly = true)
    public List<TimeCorrection> getUserTimeCorrections(Long userId) {
        return timeCorrectionRepository.findByUserId(userId.intValue());
    }

    /**
     * 承認待ち申請一覧取得
     */
    @Transactional(readOnly = true)
    public List<TimeCorrection> getPendingTimeCorrections() {
        return timeCorrectionRepository.findByStatus("pending");
    }

    /**
     * 申請詳細取得
     */
    @Transactional(readOnly = true)
    public Optional<TimeCorrection> getTimeCorrectionById(Long correctionId) {
        return timeCorrectionRepository.findById(correctionId);
    }

    /**
     * ユーザーの承認待ち申請数取得
     */
    @Transactional(readOnly = true)
    public long getUserPendingCount(Long userId) {
        return timeCorrectionRepository.countByUserIdAndStatus(userId.intValue(), "pending");
    }

    /**
     * 全体の承認待ち申請数取得
     */
    @Transactional(readOnly = true)
    public long getAllPendingCount() {
        return timeCorrectionRepository.countByStatus("pending");
    }
}
