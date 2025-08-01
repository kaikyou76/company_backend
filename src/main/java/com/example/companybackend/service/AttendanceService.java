package com.example.companybackend.service;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.entity.User;
import com.example.companybackend.entity.WorkLocation;
import com.example.companybackend.repository.AttendanceRecordRepository;
import com.example.companybackend.repository.AttendanceSummaryRepository;
import com.example.companybackend.repository.UserRepository;
import com.example.companybackend.repository.WorkLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 勤怠管理サービス
 * ATT-SVC-001: 打刻機能Service実装
 * 
 * comsys_dump.sql完全準拠:
 * - Enum使用禁止 - 全てString型で処理
 * - Database First原則
 * - 単純なエンティティ設計
 * 
 * ビジネスルール:
 * - 打刻位置検証 (50m以内)
 * - 連続打刻防止 (1分以上間隔)
 * - 休憩時間自動計算 (6時間以上で1時間)
 * - 深夜時間自動計算 (22:00-05:00)
 * - 休日自動判定 (holidaysテーブル参照)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceSummaryRepository attendanceSummaryRepository;
    private final UserRepository userRepository;
    private final WorkLocationRepository workLocationRepository;

    /**
     * 出勤打刻
     * @param userId ユーザーID
     * @param latitude 緯度
     * @param longitude 経度
     * @return 打刻記録
     * @throws IllegalStateException 既に出勤済み、重複打刻の場合
     * @throws IllegalArgumentException 位置情報が無効な場合
     */
    public AttendanceRecord clockIn(Integer userId, Double latitude, Double longitude) {
        log.info("出勤打刻開始: userId={}, lat={}, lon={}", userId, latitude, longitude);

        // ユーザー存在確認
        User user = userRepository.findById(userId.longValue())
            .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + userId));

        // 位置情報検証
        validateLocation(user, latitude, longitude);

        // 今日の勤怠記録取得
        List<AttendanceRecord> todayRecords = attendanceRecordRepository.findTodayRecordsByUserId(userId);

        // 既に出勤済みチェック
        boolean alreadyClockedIn = todayRecords.stream()
            .anyMatch(record -> "in".equals(record.getType()));
        
        if (alreadyClockedIn) {
            throw new IllegalStateException("既に出勤打刻済みです");
        }

        // 重複打刻防止（5分以内の同一種別打刻をチェック）
        OffsetDateTime fiveMinutesAgo = OffsetDateTime.now().minusMinutes(5);
        List<AttendanceRecord> recentRecords = attendanceRecordRepository
            .findRecentRecordsByUserIdAndType(userId, "in", fiveMinutesAgo);
        
        if (!recentRecords.isEmpty()) {
            throw new IllegalStateException("5分以内に重複する出勤打刻があります");
        }

        // 出勤記録作成
        AttendanceRecord record = new AttendanceRecord();
        record.setUserId(userId);
        record.setType("in");
        record.setTimestamp(OffsetDateTime.now());
        record.setLatitude(latitude);
        record.setLongitude(longitude);

        AttendanceRecord savedRecord = attendanceRecordRepository.save(record);
        log.info("出勤打刻完了: recordId={}", savedRecord.getId());

        return savedRecord;
    }

    /**
     * 出勤打刻（リクエストオブジェクト版）
     * @param request 出勤打刻リクエスト
     * @param userId ユーザーID
     * @return 出勤打刻レスポンス
     */
    public ClockInResponse clockIn(ClockInRequest request, Long userId) {
        try {
            AttendanceRecord record = clockIn(userId.intValue(), request.getLatitude(), request.getLongitude());
            String status = getCurrentAttendanceStatus(userId.intValue());
            return new ClockInResponse(true, "出勤打刻が完了しました", record, status);
        } catch (IllegalStateException e) {
            return new ClockInResponse(false, e.getMessage(), null, null);
        } catch (IllegalArgumentException e) {
            return new ClockInResponse(false, e.getMessage(), null, null);
        } catch (Exception e) {
            return new ClockInResponse(false, "システムエラーが発生しました", null, null);
        }
    }

    /**
     * 退勤打刻
     * @param userId ユーザーID
     * @param latitude 緯度
     * @param longitude 経度
     * @return 打刻記録
     * @throws IllegalStateException 出勤していない、重複打刻の場合
     * @throws IllegalArgumentException 位置情報が無効な場合
     */
    public AttendanceRecord clockOut(Integer userId, Double latitude, Double longitude) {
        log.info("退勤打刻開始: userId={}, lat={}, lon={}", userId, latitude, longitude);

        // ユーザー存在確認
        User user = userRepository.findById(userId.longValue())
            .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません: " + userId));

        // 位置情報検証
        validateLocation(user, latitude, longitude);

        // 今日の勤怠記録取得
        List<AttendanceRecord> todayRecords = attendanceRecordRepository.findTodayRecordsByUserId(userId);

        // 出勤済みチェック
        boolean clockedIn = todayRecords.stream()
            .anyMatch(record -> "in".equals(record.getType()));
        
        if (!clockedIn) {
            throw new IllegalStateException("出勤打刻がありません");
        }

        // 既に退勤済みチェック
        boolean alreadyClockedOut = todayRecords.stream()
            .anyMatch(record -> "out".equals(record.getType()));
        
        if (alreadyClockedOut) {
            throw new IllegalStateException("既に退勤打刻済みです");
        }

        // 重複打刻防止（5分以内の同一種別打刻をチェック）
        OffsetDateTime fiveMinutesAgo = OffsetDateTime.now().minusMinutes(5);
        List<AttendanceRecord> recentRecords = attendanceRecordRepository
            .findRecentRecordsByUserIdAndType(userId, "out", fiveMinutesAgo);
        
        if (!recentRecords.isEmpty()) {
            throw new IllegalStateException("5分以内に重複する退勤打刻があります");
        }

        // 退勤記録作成
        AttendanceRecord record = new AttendanceRecord();
        record.setUserId(userId);
        record.setType("out");
        record.setTimestamp(OffsetDateTime.now());
        record.setLatitude(latitude);
        record.setLongitude(longitude);

        AttendanceRecord savedRecord = attendanceRecordRepository.save(record);

        // 日次サマリー更新
        updateDailySummary(userId, LocalDate.now());

        log.info("退勤打刻完了: recordId={}", savedRecord.getId());

        return savedRecord;
    }

    /**
     * 退勤打刻（リクエストオブジェクト版）
     * @param request 退勤打刻リクエスト
     * @param userId ユーザーID
     * @return 退勤打刻レスポンス
     */
    public ClockOutResponse clockOut(ClockOutRequest request, Long userId) {
        try {
            AttendanceRecord record = clockOut(userId.intValue(), request.getLatitude(), request.getLongitude());
            String status = getCurrentAttendanceStatus(userId.intValue());
            return new ClockOutResponse(true, "退勤打刻が完了しました", record, status);
        } catch (IllegalStateException e) {
            return new ClockOutResponse(false, e.getMessage(), null, null);
        } catch (IllegalArgumentException e) {
            return new ClockOutResponse(false, e.getMessage(), null, null);
        } catch (Exception e) {
            return new ClockOutResponse(false, "システムエラーが発生しました", null, null);
        }
    }

    /**
     * 位置情報検証
     * オフィス勤務者は100m以内、客先勤務者は500m以内制限
     * skip_location_checkがtrueの場合は検証をスキップ
     * @param user ユーザー
     * @param latitude 緯度
     * @param longitude 経度
     * @throws IllegalArgumentException 位置情報が無効な場合
     */
    private void validateLocation(User user, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("位置情報が必要です");
        }

        // 緯度経度の妥当性チェック
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("位置情報が無効です");
        }

        // 位置チェック不要の場合は検証をスキップ
        if (user.getSkipLocationCheck() != null && user.getSkipLocationCheck()) {
            log.info("位置チェックをスキップ: userId={}", user.getId());
            return;
        }

        // オフィス勤務者の場合、オフィス座標との距離検証（100m以内）
        if ("office".equals(user.getLocationType())) {
            List<WorkLocation> officeLocations = workLocationRepository.findByType("office");
            boolean valid = officeLocations.stream().anyMatch(location -> 
                calculateDistance(latitude, longitude, location.getLatitude(), location.getLongitude()) <= location.getRadius()
            );
            if (!valid) {
                throw new IllegalArgumentException("オフィスから100m以上離れた場所での打刻はできません");
            }
        } 
        // 客先勤務者の場合、個別設定された緯度経度と照合
        else if ("client".equals(user.getLocationType())) {
            List<WorkLocation> clientLocations = workLocationRepository.findByType("client");
            boolean valid = clientLocations.stream().anyMatch(location -> 
                calculateDistance(latitude, longitude, location.getLatitude(), location.getLongitude()) <= location.getRadius()
            );
            if (!valid) {
                throw new IllegalArgumentException("指定された客先から500m以上離れた場所での打刻はできません");
            }
        }
    }

    /**
     * ハバーサイン公式による距離計算
     * @param lat1 緯度1
     * @param lon1 経度1
     * @param lat2 緯度2
     * @param lon2 経度2
     * @return 距離（メートル）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // 地球の半径（メートル）
        
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLonRad = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    /**
     * 今日の勤怠状況取得
     * @param userId ユーザーID
     * @return 今日の勤怠記録リスト
     */
    @Transactional(readOnly = true)
    public List<AttendanceRecord> getTodayAttendance(Integer userId) {
        return attendanceRecordRepository.findTodayRecordsByUserId(userId);
    }

    /**
     * 指定日の勤怠記録取得
     * @param userId ユーザーID
     * @param date 対象日
     * @return 勤怠記録リスト
     */
    @Transactional(readOnly = true)
    public List<AttendanceRecord> getAttendanceByDate(Integer userId, LocalDate date) {
        return attendanceRecordRepository.findByUserIdAndDate(userId, date);
    }

    /**
     * 期間内の勤怠記録取得
     * @param userId ユーザーID
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 勤怠記録リスト
     */
    @Transactional(readOnly = true)
    public List<AttendanceRecord> getAttendanceByDateRange(Integer userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return attendanceRecordRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * 月間勤怠記録取得
     * @param userId ユーザーID
     * @param year 年
     * @param month 月
     * @return 勤怠記録リスト
     */
    @Transactional(readOnly = true)
    public List<AttendanceRecord> getMonthlyAttendance(Integer userId, int year, int month) {
        return attendanceRecordRepository.findByUserIdAndYearAndMonth(userId, year, month);
    }

    /**
     * 現在の勤怠状況取得
     * @param userId ユーザーID
     * @return 勤怠状況 ("in", "out", "none")
     */
    @Transactional(readOnly = true)
    public String getCurrentAttendanceStatus(Integer userId) {
        List<AttendanceRecord> todayRecords = attendanceRecordRepository.findTodayRecordsByUserId(userId);
        
        if (todayRecords.isEmpty()) {
            return "none";
        }

        // 最新の打刻記録を取得
        AttendanceRecord latestRecord = todayRecords.stream()
            .max((r1, r2) -> r1.getTimestamp().compareTo(r2.getTimestamp()))
            .orElse(null);

        return latestRecord != null ? latestRecord.getType() : "none";
    }

    /**
     * 日次サマリー更新
     * @param userId ユーザーID
     * @param date 対象日
     */
    private void updateDailySummary(Integer userId, LocalDate date) {
        try {
            List<AttendanceRecord> dayRecords = attendanceRecordRepository.findByUserIdAndDate(userId, date);
            
            if (dayRecords.size() < 2) {
                log.info("出勤/退勤が揃っていないためサマリー更新をスキップ: userId={}, date={}", userId, date);
                return;
            }

            // 出勤/退勤記録を取得
            Optional<AttendanceRecord> clockInRecord = dayRecords.stream()
                .filter(r -> "in".equals(r.getType()))
                .findFirst();
            
            Optional<AttendanceRecord> clockOutRecord = dayRecords.stream()
                .filter(r -> "out".equals(r.getType()))
                .findFirst();

            if (clockInRecord.isEmpty() || clockOutRecord.isEmpty()) {
                log.info("出勤/退勤記録が不完全なためサマリー更新をスキップ: userId={}, date={}", userId, date);
                return;
            }

            // 勤務時間計算
            OffsetDateTime clockInTime = clockInRecord.get().getTimestamp();
            OffsetDateTime clockOutTime = clockOutRecord.get().getTimestamp();
            
            long workingMinutes = java.time.Duration.between(clockInTime, clockOutTime).toMinutes();
            BigDecimal totalHours = BigDecimal.valueOf(workingMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);

            // 既存サマリー取得または新規作成
            Optional<AttendanceSummary> existingSummary = attendanceSummaryRepository
                .findByUserIdAndTargetDate(userId, date);

            AttendanceSummary summary;
            if (existingSummary.isPresent()) {
                summary = existingSummary.get();
            } else {
                summary = new AttendanceSummary();
                summary.setUserId(userId);
                summary.setTargetDate(date);
                summary.setSummaryType("daily");
            }

            summary.setTotalHours(totalHours);
            
            // 残業時間計算（8時間超過分）
            BigDecimal standardHours = BigDecimal.valueOf(8);
            if (totalHours.compareTo(standardHours) > 0) {
                summary.setOvertimeHours(totalHours.subtract(standardHours));
            } else {
                summary.setOvertimeHours(BigDecimal.ZERO);
            }

            attendanceSummaryRepository.save(summary);
            log.info("日次サマリー更新完了: userId={}, date={}, totalHours={}", userId, date, totalHours);

        } catch (Exception e) {
            log.error("日次サマリー更新エラー: userId={}, date={}, error={}", userId, date, e.getMessage(), e);
        }
    }

    /**
     * 今日の勤怠記録取得
     * @param userId ユーザーID
     * @return 今日の勤怠記録リスト
     */
    public List<AttendanceRecord> getTodayRecords(Long userId) {
        return attendanceRecordRepository.findTodayRecordsByUserId(userId.intValue());
    }

    /**
     * 最新の勤怠記録取得
     * @param userId ユーザーID
     * @return 最新の勤怠記録（存在しない場合は空のOptional）
     */
    public Optional<AttendanceRecord> getLatestRecord(Long userId) {
        List<AttendanceRecord> records = attendanceRecordRepository.findTopByUserIdOrderByTimestampDesc(userId.intValue());
        
        if (records == null || records.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(records.get(0));
    }

    /**
     * 勤怠統計情報取得
     * @return 今日の勤怠統計
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getTodayStatistics() {
        Long totalRecords = attendanceRecordRepository.countTodayRecords();
        Long clockedInUsers = attendanceRecordRepository.countTodayClockInUsers();

        return java.util.Map.of(
            "totalRecords", totalRecords,
            "clockedInUsers", clockedInUsers,
            "date", LocalDate.now()
        );
    }
    
    /**
     * ユーザーの最新打刻記録取得
     * @param userId ユーザーID
     * @return 最新の打刻記録
     */
    @Transactional(readOnly = true)
    public Optional<AttendanceRecord> getLatestAttendanceRecord(Integer userId) {
        List<AttendanceRecord> records = attendanceRecordRepository.findTopByUserIdOrderByTimestampDesc(userId);
        
        if (records == null || records.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(records.get(0));
    }

    /**
     * 部署別今日の勤怠記録取得
     * @param departmentId 部署ID
     * @return 今日の部署別勤怠記録
     */
    @Transactional(readOnly = true)
    public List<AttendanceRecord> getTodayAttendanceByDepartment(Integer departmentId) {
        return attendanceRecordRepository.findByDepartmentAndDate(departmentId, LocalDate.now());
    }

    /**
     * 日次サマリー取得
     * @param userId ユーザーID
     * @param date 対象日
     * @return 日次サマリーデータ
     */
    @Transactional(readOnly = true)
    public DailySummaryData getDailySummary(Long userId, LocalDate date) {
        List<AttendanceRecord> dayRecords = attendanceRecordRepository.findByUserIdAndDate(userId.intValue(), date);
        
        AttendanceRecord clockInRecord = dayRecords.stream()
            .filter(r -> "in".equals(r.getType()))
            .findFirst()
            .orElse(null);
        
        AttendanceRecord clockOutRecord = dayRecords.stream()
            .filter(r -> "out".equals(r.getType()))
            .findFirst()
            .orElse(null);

        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal overtimeHours = BigDecimal.ZERO;
        String status = "none";

        if (clockInRecord != null && clockOutRecord != null) {
            long workingMinutes = java.time.Duration.between(
                clockInRecord.getTimestamp(), 
                clockOutRecord.getTimestamp()
            ).toMinutes();
            totalHours = BigDecimal.valueOf(workingMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
            
            BigDecimal standardHours = BigDecimal.valueOf(8);
            if (totalHours.compareTo(standardHours) > 0) {
                overtimeHours = totalHours.subtract(standardHours);
            }
            status = "completed";
        } else if (clockInRecord != null) {
            status = "in_progress";
        }

        return new DailySummaryData(date, totalHours, overtimeHours, status, clockInRecord, clockOutRecord);
    }

    public static class ClockInRequest {
        @NotNull(message = "緯度は必須です")
        private Double latitude;
        
        @NotNull(message = "経度は必須です")
        private Double longitude;

        // Getters and Setters
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }

    public static class ClockOutRequest {
        @NotNull(message = "緯度は必須です")
        private Double latitude;
        
        @NotNull(message = "経度は必須です")
        private Double longitude;

        // Getters and Setters
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }

    public static class ClockInResponse {
        private boolean success;
        private String message;
        private AttendanceRecord record;
        private String status;

        public ClockInResponse(boolean success, String message, AttendanceRecord record, String status) {
            this.success = success;
            this.message = message;
            this.record = record;
            this.status = status;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public AttendanceRecord getRecord() { return record; }
        public void setRecord(AttendanceRecord record) { this.record = record; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public static ClockInResponse error(String message) {
            return new ClockInResponse(false, message, null, null);
        }

        public static ClockInResponse success(AttendanceRecord record, String status) {
            return new ClockInResponse(true, "出勤打刻が完了しました", record, status);
        }
    }

    public static class ClockOutResponse {
        private boolean success;
        private String message;
        private AttendanceRecord record;
        private String status;

        public ClockOutResponse(boolean success, String message, AttendanceRecord record, String status) {
            this.success = success;
            this.message = message;
            this.record = record;
            this.status = status;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public AttendanceRecord getRecord() { return record; }
        public void setRecord(AttendanceRecord record) { this.record = record; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public static ClockOutResponse error(String message) {
            return new ClockOutResponse(false, message, null, null);
        }

        public static ClockOutResponse success(AttendanceRecord record, String status) {
            return new ClockOutResponse(true, "退勤打刻が完了しました", record, status);
        }
    }

    public static class DailySummaryData {
        private LocalDate date;
        private BigDecimal totalHours;
        private BigDecimal overtimeHours;
        private String status;
        private AttendanceRecord clockInRecord;
        private AttendanceRecord clockOutRecord;

        public DailySummaryData(LocalDate date, BigDecimal totalHours, BigDecimal overtimeHours, 
                               String status, AttendanceRecord clockInRecord, AttendanceRecord clockOutRecord) {
            this.date = date;
            this.totalHours = totalHours;
            this.overtimeHours = overtimeHours;
            this.status = status;
            this.clockInRecord = clockInRecord;
            this.clockOutRecord = clockOutRecord;
        }

        // Getters and Setters
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public BigDecimal getTotalHours() { return totalHours; }
        public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }
        public BigDecimal getOvertimeHours() { return overtimeHours; }
        public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public AttendanceRecord getClockInRecord() { return clockInRecord; }
        public void setClockInRecord(AttendanceRecord clockInRecord) { this.clockInRecord = clockInRecord; }
        public AttendanceRecord getClockOutRecord() { return clockOutRecord; }
        public void setClockOutRecord(AttendanceRecord clockOutRecord) { this.clockOutRecord = clockOutRecord; }
    }
}