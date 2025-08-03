package com.example.companybackend.service;

import com.example.companybackend.entity.LeaveRequest;
import com.example.companybackend.entity.Notification;
import com.example.companybackend.entity.TimeCorrection;
import com.example.companybackend.entity.User;
import com.example.companybackend.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知サービス
 * comsys_test_dump.sqlの既存データベース構造に基づく通知機能
 */
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    // メモリベースの重複防止キャッシュ
    private final Map<String, OffsetDateTime> duplicatePreventionCache = new ConcurrentHashMap<>();

    /**
     * 通知作成の基本メソッド
     * 
     * @param userId    ユーザーID (Integer型)
     * @param title     タイトル
     * @param message   メッセージ
     * @param type      通知タイプ
     * @param relatedId 関連ID
     * @return 作成された通知
     */
    public Notification createNotification(Integer userId, String title, String message, String type,
            Integer relatedId) {
        log.info("通知作成開始: userId={}, type={}, title={}", userId, type, title);

        try {
            // 重複チェック
            if (isDuplicateNotification(title, userId, Duration.ofMinutes(5))) {
                log.warn("重複通知を検出: userId={}, title={}", userId, title);
                return null;
            }

            // 通知作成
            Notification notification = Notification.create(userId, title, message, type, relatedId);
            Notification savedNotification = notificationRepository.save(notification);

            // 重複防止キャッシュに追加
            String cacheKey = userId + ":" + title;
            duplicatePreventionCache.put(cacheKey, OffsetDateTime.now());

            log.info("通知作成完了: notificationId={}", savedNotification.getId());
            return savedNotification;

        } catch (Exception e) {
            log.error("通知作成エラー: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("通知作成に失敗しました", e);
        }
    }

    /**
     * ユーザーの通知一覧取得
     * 
     * @param userId     ユーザーID
     * @param unreadOnly 未読のみ取得するか
     * @return 通知リスト
     */
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Integer userId, boolean unreadOnly) {
        log.info("ユーザー通知取得: userId={}, unreadOnly={}", userId, unreadOnly);

        try {
            if (unreadOnly) {
                return notificationRepository.findUnreadByUserId(userId);
            } else {
                return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
            }
        } catch (Exception e) {
            log.error("ユーザー通知取得エラー: userId={}, error={}", userId, e.getMessage(), e);
            return List.of(); // 空リストを返す
        }
    }

    /**
     * 通知を既読にする
     * 
     * @param notificationId 通知ID
     */
    public void markAsRead(Long notificationId) {
        log.info("通知既読化: notificationId={}", notificationId);

        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new IllegalArgumentException("通知が見つかりません: " + notificationId));

            if (!notification.isUnread()) {
                log.warn("既に既読の通知: notificationId={}", notificationId);
                return;
            }

            notification.markAsRead();
            notificationRepository.save(notification);

            log.info("通知既読化完了: notificationId={}", notificationId);

        } catch (Exception e) {
            log.error("通知既読化エラー: notificationId={}, error={}", notificationId, e.getMessage(), e);
            throw new RuntimeException("通知の既読化に失敗しました", e);
        }
    }

    /**
     * ユーザーの全通知を既読にする
     * 
     * @param userId ユーザーID
     */
    public void markAllAsRead(Integer userId) {
        log.info("全通知既読化: userId={}", userId);

        try {
            int updatedCount = notificationRepository.markAllAsReadByUserId(userId);
            log.info("全通知既読化完了: userId={}, updatedCount={}", userId, updatedCount);

        } catch (Exception e) {
            log.error("全通知既読化エラー: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("全通知の既読化に失敗しました", e);
        }
    }

    /**
     * 古い通知を削除する（30日以上前）
     * 
     * @param daysOld 何日以上古い通知を削除するか
     * @return 削除件数
     */
    public int deleteOldNotifications(int daysOld) {
        log.info("古い通知削除開始: daysOld={}", daysOld);

        try {
            OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(daysOld);
            int deletedCount = notificationRepository.deleteOldNotifications(cutoffDate);

            log.info("古い通知削除完了: deletedCount={}", deletedCount);
            return deletedCount;

        } catch (Exception e) {
            log.error("古い通知削除エラー: daysOld={}, error={}", daysOld, e.getMessage(), e);
            throw new RuntimeException("古い通知の削除に失敗しました", e);
        }
    }

    /**
     * 重複通知チェック
     * 
     * @param title      タイトル
     * @param userId     ユーザーID (Integer)
     * @param timeWindow 時間窓
     * @return 重複の場合true
     */
    public boolean isDuplicateNotification(String title, Integer userId, Duration timeWindow) {
        String cacheKey = userId + ":" + title;
        OffsetDateTime lastSent = duplicatePreventionCache.get(cacheKey);

        if (lastSent != null) {
            OffsetDateTime threshold = OffsetDateTime.now().minus(timeWindow);
            if (lastSent.isAfter(threshold)) {
                return true; // 重複
            } else {
                // 古いエントリを削除
                duplicatePreventionCache.remove(cacheKey);
            }
        }

        // データベースからも確認
        OffsetDateTime afterTime = OffsetDateTime.now().minus(timeWindow);
        List<Notification> duplicates = notificationRepository.findDuplicateNotifications(userId, title, afterTime);
        return !duplicates.isEmpty();
    }

    /**
     * 通知統計取得
     * 
     * @param startDate 開始日
     * @param endDate   終了日
     * @return 統計情報
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationStatistics(LocalDate startDate, LocalDate endDate) {
        log.info("通知統計取得: startDate={}, endDate={}", startDate, endDate);

        try {
            OffsetDateTime startDateTime = startDate.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
            OffsetDateTime endDateTime = endDate.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

            Object[] stats = notificationRepository.getNotificationStatistics(startDateTime, endDateTime);

            Map<String, Object> result = new HashMap<>();
            if (stats != null && stats.length >= 6) {
                result.put("totalNotifications", stats[0]);
                result.put("readCount", stats[1]);
                result.put("unreadCount", stats[2]);
                result.put("leaveNotifications", stats[3]);
                result.put("correctionNotifications", stats[4]);
                result.put("systemNotifications", stats[5]);

                // 既読率計算
                Long total = (Long) stats[0];
                Long read = (Long) stats[1];
                if (total > 0) {
                    result.put("readRate", (double) read / total * 100);
                } else {
                    result.put("readRate", 0.0);
                }
            }

            log.info("通知統計取得完了: result={}", result);
            return result;

        } catch (Exception e) {
            log.error("通知統計取得エラー: startDate={}, endDate={}, error={}", startDate, endDate, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    // ========== 休暇申請通知機能 ==========

    /**
     * 休暇申請作成通知送信
     * 
     * @param leaveRequest 休暇申請
     * @param user         申請者
     */
    public void sendLeaveRequestNotification(LeaveRequest leaveRequest, User user) {
        log.info("休暇申請作成通知送信: leaveRequestId={}, userId={}", leaveRequest.getId(), user.getId());

        try {
            String title = "休暇申請を提出しました";
            String message = String.format("%sの申請を提出しました。期間: %s - %s\n理由: %s",
                    getLeaveTypeDisplayName(leaveRequest.getType()),
                    leaveRequest.getStartDate(),
                    leaveRequest.getEndDate(),
                    leaveRequest.getReason() != null ? leaveRequest.getReason() : "理由なし");

            createNotification(user.getId().intValue(), title, message, "leave", leaveRequest.getId().intValue());

            log.info("休暇申請作成通知送信完了: leaveRequestId={}", leaveRequest.getId());

        } catch (Exception e) {
            log.error("休暇申請作成通知送信エラー: leaveRequestId={}, error={}", leaveRequest.getId(), e.getMessage(), e);
        }
    }

    /**
     * 休暇申請承認通知送信
     * 
     * @param leaveRequest 休暇申請
     * @param user         申請者
     */
    public void sendLeaveApprovalNotification(LeaveRequest leaveRequest, User user) {
        log.info("休暇申請承認通知送信: leaveRequestId={}, userId={}", leaveRequest.getId(), user.getId());

        try {
            String title = getLeaveTypeDisplayName(leaveRequest.getType()) + "申請が承認されました";
            String message = String.format("%sの申請が承認されました。期間: %s - %s\n承認日時: %s",
                    getLeaveTypeDisplayName(leaveRequest.getType()),
                    leaveRequest.getStartDate(),
                    leaveRequest.getEndDate(),
                    leaveRequest.getApprovedAt() != null ? leaveRequest.getApprovedAt().toString() : "承認日時不明");

            createNotification(user.getId().intValue(), title, message, "leave", leaveRequest.getId().intValue());

            log.info("休暇申請承認通知送信完了: leaveRequestId={}", leaveRequest.getId());

        } catch (Exception e) {
            log.error("休暇申請承認通知送信エラー: leaveRequestId={}, error={}", leaveRequest.getId(), e.getMessage(), e);
        }
    }

    /**
     * 休暇申請拒否通知送信
     * 
     * @param leaveRequest 休暇申請
     * @param user         申請者
     */
    public void sendLeaveRejectionNotification(LeaveRequest leaveRequest, User user) {
        log.info("休暇申請拒否通知送信: leaveRequestId={}, userId={}", leaveRequest.getId(), user.getId());

        try {
            String title = getLeaveTypeDisplayName(leaveRequest.getType()) + "申請が拒否されました";
            String message = String.format("%sの申請が拒否されました。期間: %s - %s\n拒否日時: %s",
                    getLeaveTypeDisplayName(leaveRequest.getType()),
                    leaveRequest.getStartDate(),
                    leaveRequest.getEndDate(),
                    leaveRequest.getApprovedAt() != null ? leaveRequest.getApprovedAt().toString() : "拒否日時不明");

            createNotification(user.getId().intValue(), title, message, "leave", leaveRequest.getId().intValue());

            log.info("休暇申請拒否通知送信完了: leaveRequestId={}", leaveRequest.getId());

        } catch (Exception e) {
            log.error("休暇申請拒否通知送信エラー: leaveRequestId={}, error={}", leaveRequest.getId(), e.getMessage(), e);
        }
    }

    // ========== 時刻修正申請通知機能 ==========

    /**
     * 時刻修正申請作成通知送信
     * 
     * @param timeCorrection 時刻修正申請
     * @param user           申請者
     */
    public void sendTimeCorrectionNotification(TimeCorrection timeCorrection, User user) {
        log.info("時刻修正申請作成通知送信: timeCorrectionId={}, userId={}", timeCorrection.getId(), user.getId());

        try {
            String title = "時刻修正申請を提出しました";
            String message = String.format("時刻修正申請を提出しました。\n修正タイプ: %s\n理由: %s",
                    getTimeCorrectionTypeDisplayName(timeCorrection.getRequestType()),
                    timeCorrection.getReason());

            createNotification(user.getId().intValue(), title, message, "correction",
                    timeCorrection.getId().intValue());

            log.info("時刻修正申請作成通知送信完了: timeCorrectionId={}", timeCorrection.getId());

        } catch (Exception e) {
            log.error("時刻修正申請作成通知送信エラー: timeCorrectionId={}, error={}", timeCorrection.getId(), e.getMessage(), e);
        }
    }

    /**
     * 時刻修正申請承認通知送信
     * 
     * @param timeCorrection 時刻修正申請
     * @param user           申請者
     */
    public void sendTimeCorrectionApprovalNotification(TimeCorrection timeCorrection, User user) {
        log.info("時刻修正申請承認通知送信: timeCorrectionId={}, userId={}", timeCorrection.getId(), user.getId());

        try {
            String title = "時刻修正申請が承認されました";
            String message = String.format("時刻修正申請が承認されました。\n修正タイプ: %s\n承認日時: %s",
                    getTimeCorrectionTypeDisplayName(timeCorrection.getRequestType()),
                    timeCorrection.getApprovedAt() != null ? timeCorrection.getApprovedAt().toString() : "承認日時不明");

            createNotification(user.getId().intValue(), title, message, "correction",
                    timeCorrection.getId().intValue());

            log.info("時刻修正申請承認通知送信完了: timeCorrectionId={}", timeCorrection.getId());

        } catch (Exception e) {
            log.error("時刻修正申請承認通知送信エラー: timeCorrectionId={}, error={}", timeCorrection.getId(), e.getMessage(), e);
        }
    }

    /**
     * 時刻修正申請拒否通知送信
     * 
     * @param timeCorrection 時刻修正申請
     * @param user           申請者
     */
    public void sendTimeCorrectionRejectionNotification(TimeCorrection timeCorrection, User user) {
        log.info("時刻修正申請拒否通知送信: timeCorrectionId={}, userId={}", timeCorrection.getId(), user.getId());

        try {
            String title = "時刻修正申請が拒否されました";
            String message = String.format("時刻修正申請が拒否されました。\n修正タイプ: %s\n拒否日時: %s",
                    getTimeCorrectionTypeDisplayName(timeCorrection.getRequestType()),
                    timeCorrection.getApprovedAt() != null ? timeCorrection.getApprovedAt().toString() : "拒否日時不明");

            createNotification(user.getId().intValue(), title, message, "correction",
                    timeCorrection.getId().intValue());

            log.info("時刻修正申請拒否通知送信完了: timeCorrectionId={}", timeCorrection.getId());

        } catch (Exception e) {
            log.error("時刻修正申請拒否通知送信エラー: timeCorrectionId={}, error={}", timeCorrection.getId(), e.getMessage(), e);
        }
    }

    // ========== ヘルパーメソッド ==========

    /**
     * 休暇タイプの表示名取得
     * 
     * @param type 休暇タイプ
     * @return 表示名
     */
    private String getLeaveTypeDisplayName(String type) {
        switch (type) {
            case "paid":
                return "有給休暇";
            case "sick":
                return "病気休暇";
            case "special":
                return "特別休暇";
            default:
                return "休暇";
        }
    }

    /**
     * 時刻修正タイプの表示名取得
     * 
     * @param requestType 修正タイプ
     * @return 表示名
     */
    private String getTimeCorrectionTypeDisplayName(String requestType) {
        switch (requestType) {
            case "time":
                return "時刻修正";
            case "type":
                return "タイプ修正";
            case "both":
                return "時刻・タイプ修正";
            default:
                return "修正";
        }
    }

    /**
     * 通知タイトル作成
     * 
     * @param type      テンプレートタイプ
     * @param variables 変数マップ
     * @return 作成されたタイトル
     */
    public String createNotificationTitle(String type, Map<String, Object> variables) {
        switch (type) {
            case "leave_request_created":
                return String.format("%s申請を提出しました", variables.get("type"));
            case "leave_request_approval_needed":
                return String.format("%s申請の承認が必要です", variables.get("type"));
            case "leave_request_approved":
                return String.format("%s申請が承認されました", variables.get("type"));
            case "leave_request_rejected":
                return String.format("%s申請が拒否されました", variables.get("type"));
            case "time_correction_created":
                return "時刻修正申請を提出しました";
            case "time_correction_approval_needed":
                return "時刻修正申請の承認が必要です";
            case "time_correction_approved":
                return "時刻修正申請が承認されました";
            case "time_correction_rejected":
                return "時刻修正申請が拒否されました";
            default:
                return "通知";
        }
    }

    /**
     * 通知メッセージ作成
     * 
     * @param type      テンプレートタイプ
     * @param variables 変数マップ
     * @return 作成されたメッセージ
     */
    public String createNotificationMessage(String type, Map<String, Object> variables) {
        switch (type) {
            case "leave_request_created":
                return String.format("%sの申請を提出しました。期間: %s - %s\n理由: %s",
                        variables.get("type"),
                        variables.get("startDate"),
                        variables.get("endDate"),
                        variables.get("reason"));
            case "leave_request_approval_needed":
                return String.format("%s さんから%sの申請が提出されました。期間: %s - %s\n理由: %s\n承認をお願いします。",
                        variables.get("username"),
                        variables.get("type"),
                        variables.get("startDate"),
                        variables.get("endDate"),
                        variables.get("reason"));
            case "leave_request_approved":
                return String.format("%sの申請が承認されました。期間: %s - %s\n承認日時: %s",
                        variables.get("type"),
                        variables.get("startDate"),
                        variables.get("endDate"),
                        variables.get("approvedAt"));
            case "leave_request_rejected":
                return String.format("%sの申請が拒否されました。期間: %s - %s\n拒否日時: %s",
                        variables.get("type"),
                        variables.get("startDate"),
                        variables.get("endDate"),
                        variables.get("rejectedAt"));
            case "time_correction_created":
                return String.format("時刻修正申請を提出しました。\n修正タイプ: %s\n理由: %s",
                        variables.get("requestType"),
                        variables.get("reason"));
            case "time_correction_approval_needed":
                return String.format("%s さんから時刻修正申請が提出されました。\n修正タイプ: %s\n理由: %s\n承認をお願いします。",
                        variables.get("username"),
                        variables.get("requestType"),
                        variables.get("reason"));
            case "time_correction_approved":
                return String.format("時刻修正申請が承認されました。\n修正タイプ: %s\n承認日時: %s",
                        variables.get("requestType"),
                        variables.get("approvedAt"));
            case "time_correction_rejected":
                return String.format("時刻修正申請が拒否されました。\n修正タイプ: %s\n拒否日時: %s",
                        variables.get("requestType"),
                        variables.get("rejectedAt"));
            default:
                return "通知メッセージ";
        }
    }
}