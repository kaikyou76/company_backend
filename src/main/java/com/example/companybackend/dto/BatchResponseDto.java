package com.example.companybackend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class BatchResponseDto {
    
    @Data
    public static class MonthlySummaryBatchResponse {
        private boolean success;
        private String message;
        private MonthlySummaryData data;
        private LocalDateTime executedAt;
    }

    @Data
    public static class MonthlySummaryData {
        private String targetMonth;
        private int processedCount;
        private int userCount;
        private int totalWorkDays;
        private int totalWorkTime;
        private int totalOvertimeTime;
    }

    @Data
    public static class PaidLeaveUpdateBatchResponse {
        private boolean success;
        private String message;
        private PaidLeaveUpdateData data;
        private LocalDateTime executedAt;
    }

    @Data
    public static class PaidLeaveUpdateData {
        private int fiscalYear;
        private int updatedCount;
        private int totalUserCount;
        private double successRate;
        private int errorCount;
        private List<String> errorMessages;
    }

    @Data
    public static class DataCleanupBatchResponse {
        private boolean success;
        private String message;
        private DataCleanupResult data;
        private LocalDateTime executedAt;
    }

    @Data
    public static class DataCleanupResult {
        private int retentionMonths;
        private String cutoffDate;
        private int deletedCount;
        private Map<String, Integer> deletedDetails;
    }

    @Data
    public static class DataRepairBatchResponse {
        private boolean success;
        private String message;
        private List<String> repairedItems;
        private LocalDateTime executedAt;
    }

    @Data
    public static class BatchStatusResponse {
        private String systemStatus;
        private LocalDateTime lastChecked;
        private String uptime;
        private DatabaseStatus databaseStatus;
        private DataStatistics dataStatistics;
        private List<BatchExecutionHistory> recentBatchExecutions;
    }

    @Data
    public static class DatabaseStatus {
        private int totalUsers;
        private int activeUsers;
        private int totalAttendanceRecords;
        private String latestRecordDate;
    }

    @Data
    public static class DataStatistics {
        private int currentMonthRecords;
        private int incompleteRecords;
    }

    @Data
    public static class BatchExecutionHistory {
        private String type;
        private LocalDateTime executedAt;
        private String status;
        private String duration;
    }
}