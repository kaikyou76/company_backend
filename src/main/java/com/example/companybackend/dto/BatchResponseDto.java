package com.example.companybackend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class BatchResponseDto {

    public static class MonthlySummaryBatchResponse {
        private boolean success;
        private String message;
        private MonthlySummaryData data;
        private LocalDateTime executedAt;

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public MonthlySummaryData getData() {
            return data;
        }

        public void setData(MonthlySummaryData data) {
            this.data = data;
        }

        public LocalDateTime getExecutedAt() {
            return executedAt;
        }

        public void setExecutedAt(LocalDateTime executedAt) {
            this.executedAt = executedAt;
        }
    }

    public static class MonthlySummaryData {
        private int year;
        private int month;
        private List<DepartmentMonthlyData> departments;
        private LocalDateTime generatedAt;
        private String targetMonth;
        private int processedCount;
        private int userCount;
        private int totalWorkDays;
        private int totalWorkTime;
        private int totalOvertimeTime;

        // Getters and Setters
        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public List<DepartmentMonthlyData> getDepartments() {
            return departments;
        }

        public void setDepartments(List<DepartmentMonthlyData> departments) {
            this.departments = departments;
        }

        public LocalDateTime getGeneratedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(LocalDateTime generatedAt) {
            this.generatedAt = generatedAt;
        }

        public String getTargetMonth() {
            return targetMonth;
        }

        public void setTargetMonth(String targetMonth) {
            this.targetMonth = targetMonth;
        }

        public int getProcessedCount() {
            return processedCount;
        }

        public void setProcessedCount(int processedCount) {
            this.processedCount = processedCount;
        }

        public int getUserCount() {
            return userCount;
        }

        public void setUserCount(int userCount) {
            this.userCount = userCount;
        }

        public int getTotalWorkDays() {
            return totalWorkDays;
        }

        public void setTotalWorkDays(int totalWorkDays) {
            this.totalWorkDays = totalWorkDays;
        }

        public int getTotalWorkTime() {
            return totalWorkTime;
        }

        public void setTotalWorkTime(int totalWorkTime) {
            this.totalWorkTime = totalWorkTime;
        }

        public int getTotalOvertimeTime() {
            return totalOvertimeTime;
        }

        public void setTotalOvertimeTime(int totalOvertimeTime) {
            this.totalOvertimeTime = totalOvertimeTime;
        }
    }

    public static class DepartmentMonthlyData {
        private Integer departmentId;
        private String departmentName;
        private List<EmployeeMonthlyData> employees;
        private double totalWorkedHours;
        private double totalLeaveHours;
        private int totalEmployees;
        private LocalDateTime processedAt;

        // Getters and Setters
        public Integer getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(Integer departmentId) {
            this.departmentId = departmentId;
        }

        public String getDepartmentName() {
            return departmentName;
        }

        public void setDepartmentName(String departmentName) {
            this.departmentName = departmentName;
        }

        public List<EmployeeMonthlyData> getEmployees() {
            return employees;
        }

        public void setEmployees(List<EmployeeMonthlyData> employees) {
            this.employees = employees;
        }

        public double getTotalWorkedHours() {
            return totalWorkedHours;
        }

        public void setTotalWorkedHours(double totalWorkedHours) {
            this.totalWorkedHours = totalWorkedHours;
        }

        public double getTotalLeaveHours() {
            return totalLeaveHours;
        }

        public void setTotalLeaveHours(double totalLeaveHours) {
            this.totalLeaveHours = totalLeaveHours;
        }

        public int getTotalEmployees() {
            return totalEmployees;
        }

        public void setTotalEmployees(int totalEmployees) {
            this.totalEmployees = totalEmployees;
        }

        public LocalDateTime getProcessedAt() {
            return processedAt;
        }

        public void setProcessedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
        }
    }

    public static class EmployeeMonthlyData {
        private Long employeeId;
        private String employeeName;
        private String employeeCode;
        private double totalWorkedHours;
        private double regularWorkedHours;
        private double overtimeHours;
        private double holidayWorkedHours;
        private double totalLeaveHours;
        private double paidLeaveHours;
        private double specialLeaveHours;
        private Map<String, Object> details;
        private LocalDateTime processedAt;

        // Getters and Setters
        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Long employeeId) {
            this.employeeId = employeeId;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        public void setEmployeeName(String employeeName) {
            this.employeeName = employeeName;
        }

        public String getEmployeeCode() {
            return employeeCode;
        }

        public void setEmployeeCode(String employeeCode) {
            this.employeeCode = employeeCode;
        }

        public double getTotalWorkedHours() {
            return totalWorkedHours;
        }

        public void setTotalWorkedHours(double totalWorkedHours) {
            this.totalWorkedHours = totalWorkedHours;
        }

        public double getRegularWorkedHours() {
            return regularWorkedHours;
        }

        public void setRegularWorkedHours(double regularWorkedHours) {
            this.regularWorkedHours = regularWorkedHours;
        }

        public double getOvertimeHours() {
            return overtimeHours;
        }

        public void setOvertimeHours(double overtimeHours) {
            this.overtimeHours = overtimeHours;
        }

        public double getHolidayWorkedHours() {
            return holidayWorkedHours;
        }

        public void setHolidayWorkedHours(double holidayWorkedHours) {
            this.holidayWorkedHours = holidayWorkedHours;
        }

        public double getTotalLeaveHours() {
            return totalLeaveHours;
        }

        public void setTotalLeaveHours(double totalLeaveHours) {
            this.totalLeaveHours = totalLeaveHours;
        }

        public double getPaidLeaveHours() {
            return paidLeaveHours;
        }

        public void setPaidLeaveHours(double paidLeaveHours) {
            this.paidLeaveHours = paidLeaveHours;
        }

        public double getSpecialLeaveHours() {
            return specialLeaveHours;
        }

        public void setSpecialLeaveHours(double specialLeaveHours) {
            this.specialLeaveHours = specialLeaveHours;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }

        public LocalDateTime getProcessedAt() {
            return processedAt;
        }

        public void setProcessedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
        }
    }

    public static class DailySummaryBatchResponse {
        private boolean success;
        private String message;
        private DailySummaryData data;
        private LocalDateTime executedAt;

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public DailySummaryData getData() {
            return data;
        }

        public void setData(DailySummaryData data) {
            this.data = data;
        }

        public LocalDateTime getExecutedAt() {
            return executedAt;
        }

        public void setExecutedAt(LocalDateTime executedAt) {
            this.executedAt = executedAt;
        }
    }

    public static class DailySummaryData {
        private String targetDate;
        private int processedCount;
        private int userCount;
        private int totalWorkTime;
        private int totalOvertimeTime;
        private int totalLateNightTime;
        private int totalHolidayTime;
        private double averageWorkHours;
        private List<String> processingWarnings = new ArrayList<>();

        // Getters and Setters
        public String getTargetDate() {
            return targetDate;
        }

        public void setTargetDate(String targetDate) {
            this.targetDate = targetDate;
        }

        public int getProcessedCount() {
            return processedCount;
        }

        public void setProcessedCount(int processedCount) {
            this.processedCount = processedCount;
        }

        public int getUserCount() {
            return userCount;
        }

        public void setUserCount(int userCount) {
            this.userCount = userCount;
        }

        public int getTotalWorkTime() {
            return totalWorkTime;
        }

        public void setTotalWorkTime(int totalWorkTime) {
            this.totalWorkTime = totalWorkTime;
        }

        public int getTotalOvertimeTime() {
            return totalOvertimeTime;
        }

        public void setTotalOvertimeTime(int totalOvertimeTime) {
            this.totalOvertimeTime = totalOvertimeTime;
        }

        public int getTotalLateNightTime() {
            return totalLateNightTime;
        }

        public void setTotalLateNightTime(int totalLateNightTime) {
            this.totalLateNightTime = totalLateNightTime;
        }

        public int getTotalHolidayTime() {
            return totalHolidayTime;
        }

        public void setTotalHolidayTime(int totalHolidayTime) {
            this.totalHolidayTime = totalHolidayTime;
        }

        public double getAverageWorkHours() {
            return averageWorkHours;
        }

        public void setAverageWorkHours(double averageWorkHours) {
            this.averageWorkHours = averageWorkHours;
        }

        public List<String> getProcessingWarnings() {
            return processingWarnings;
        }

        public void setProcessingWarnings(List<String> processingWarnings) {
            this.processingWarnings = processingWarnings;
        }
    }

    public static class PaidLeaveUpdateBatchResponse {
        private boolean success;
        private String message;
        private PaidLeaveUpdateData data;
        private LocalDateTime executedAt;
        
        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public PaidLeaveUpdateData getData() {
            return data;
        }

        public void setData(PaidLeaveUpdateData data) {
            this.data = data;
        }

        public LocalDateTime getExecutedAt() {
            return executedAt;
        }

        public void setExecutedAt(LocalDateTime executedAt) {
            this.executedAt = executedAt;
        }
    }

    public static class PaidLeaveUpdateData {
        private int fiscalYear;
        private int updatedCount;
        private int totalUserCount;
        private double successRate;
        private int errorCount;
        private List<String> errorMessages;
        
        // Getters and Setters
        public int getFiscalYear() {
            return fiscalYear;
        }

        public void setFiscalYear(int fiscalYear) {
            this.fiscalYear = fiscalYear;
        }

        public int getUpdatedCount() {
            return updatedCount;
        }

        public void setUpdatedCount(int updatedCount) {
            this.updatedCount = updatedCount;
        }

        public int getTotalUserCount() {
            return totalUserCount;
        }

        public void setTotalUserCount(int totalUserCount) {
            this.totalUserCount = totalUserCount;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(double successRate) {
            this.successRate = successRate;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public void setErrorCount(int errorCount) {
            this.errorCount = errorCount;
        }

        public List<String> getErrorMessages() {
            return errorMessages;
        }

        public void setErrorMessages(List<String> errorMessages) {
            this.errorMessages = errorMessages;
        }
    }

    public static class DataCleanupBatchResponse {
        private boolean success;
        private String message;
        private DataCleanupResult data;
        private LocalDateTime executedAt;
        
        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public DataCleanupResult getData() {
            return data;
        }

        public void setData(DataCleanupResult data) {
            this.data = data;
        }

        public LocalDateTime getExecutedAt() {
            return executedAt;
        }

        public void setExecutedAt(LocalDateTime executedAt) {
            this.executedAt = executedAt;
        }
    }

    public static class DataCleanupResult {
        private int retentionMonths;
        private String cutoffDate;
        private int deletedCount;
        private Map<String, Integer> deletedDetails;
        
        // Getters and Setters
        public int getRetentionMonths() {
            return retentionMonths;
        }

        public void setRetentionMonths(int retentionMonths) {
            this.retentionMonths = retentionMonths;
        }

        public String getCutoffDate() {
            return cutoffDate;
        }

        public void setCutoffDate(String cutoffDate) {
            this.cutoffDate = cutoffDate;
        }

        public int getDeletedCount() {
            return deletedCount;
        }

        public void setDeletedCount(int deletedCount) {
            this.deletedCount = deletedCount;
        }

        public Map<String, Integer> getDeletedDetails() {
            return deletedDetails;
        }

        public void setDeletedDetails(Map<String, Integer> deletedDetails) {
            this.deletedDetails = deletedDetails;
        }
    }

    public static class DataRepairBatchResponse {
        private boolean success;
        private String message;
        private List<String> repairedItems;
        private LocalDateTime executedAt;
        
        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<String> getRepairedItems() {
            return repairedItems;
        }

        public void setRepairedItems(List<String> repairedItems) {
            this.repairedItems = repairedItems;
        }

        public LocalDateTime getExecutedAt() {
            return executedAt;
        }

        public void setExecutedAt(LocalDateTime executedAt) {
            this.executedAt = executedAt;
        }
    }

    public static class OvertimeMonitoringBatchResponse {
        private boolean success;
        private String message;
        private OvertimeMonitoringData data;
        private LocalDateTime executedAt;
        
        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public OvertimeMonitoringData getData() {
            return data;
        }

        public void setData(OvertimeMonitoringData data) {
            this.data = data;
        }

        public LocalDateTime getExecutedAt() {
            return executedAt;
        }

        public void setExecutedAt(LocalDateTime executedAt) {
            this.executedAt = executedAt;
        }
    }

    public static class OvertimeMonitoringData {
        private String targetMonth;
        private int processedCount;
        private int userCount;
        private int overtimeReportsGenerated;
        private int highOvertimeAlerts;
        private int confirmedReports;
        private int draftReports;
        private int approvedReports;
        private List<String> processingWarnings = new ArrayList<>();
        
        // Getters and Setters
        public String getTargetMonth() {
            return targetMonth;
        }

        public void setTargetMonth(String targetMonth) {
            this.targetMonth = targetMonth;
        }

        public int getProcessedCount() {
            return processedCount;
        }

        public void setProcessedCount(int processedCount) {
            this.processedCount = processedCount;
        }

        public int getUserCount() {
            return userCount;
        }

        public void setUserCount(int userCount) {
            this.userCount = userCount;
        }

        public int getOvertimeReportsGenerated() {
            return overtimeReportsGenerated;
        }

        public void setOvertimeReportsGenerated(int overtimeReportsGenerated) {
            this.overtimeReportsGenerated = overtimeReportsGenerated;
        }

        public int getHighOvertimeAlerts() {
            return highOvertimeAlerts;
        }

        public void setHighOvertimeAlerts(int highOvertimeAlerts) {
            this.highOvertimeAlerts = highOvertimeAlerts;
        }

        public int getConfirmedReports() {
            return confirmedReports;
        }

        public void setConfirmedReports(int confirmedReports) {
            this.confirmedReports = confirmedReports;
        }

        public int getDraftReports() {
            return draftReports;
        }

        public void setDraftReports(int draftReports) {
            this.draftReports = draftReports;
        }

        public int getApprovedReports() {
            return approvedReports;
        }

        public void setApprovedReports(int approvedReports) {
            this.approvedReports = approvedReports;
        }

        public List<String> getProcessingWarnings() {
            return processingWarnings;
        }

        public void setProcessingWarnings(List<String> processingWarnings) {
            this.processingWarnings = processingWarnings;
        }
    }

    public static class BatchStatusResponse {
        private String systemStatus;
        private LocalDateTime lastChecked;
        private String uptime;
        private DatabaseStatus databaseStatus;
        private DataStatistics dataStatistics;
        private List<BatchExecutionHistory> recentBatchExecutions;
        
        // Getters and Setters
        public String getSystemStatus() {
            return systemStatus;
        }

        public void setSystemStatus(String systemStatus) {
            this.systemStatus = systemStatus;
        }

        public LocalDateTime getLastChecked() {
            return lastChecked;
        }

        public void setLastChecked(LocalDateTime lastChecked) {
            this.lastChecked = lastChecked;
        }

        public String getUptime() {
            return uptime;
        }

        public void setUptime(String uptime) {
            this.uptime = uptime;
        }

        public DatabaseStatus getDatabaseStatus() {
            return databaseStatus;
        }

        public void setDatabaseStatus(DatabaseStatus databaseStatus) {
            this.databaseStatus = databaseStatus;
        }

        public DataStatistics getDataStatistics() {
            return dataStatistics;
        }

        public void setDataStatistics(DataStatistics dataStatistics) {
            this.dataStatistics = dataStatistics;
        }

        public List<BatchExecutionHistory> getRecentBatchExecutions() {
            return recentBatchExecutions;
        }

        public void setRecentBatchExecutions(List<BatchExecutionHistory> recentBatchExecutions) {
            this.recentBatchExecutions = recentBatchExecutions;
        }
    }

    public static class DatabaseStatus {
        private int totalUsers;
        private int activeUsers;
        private int totalAttendanceRecords;
        private String latestRecordDate;
        
        // Getters and Setters
        public int getTotalUsers() {
            return totalUsers;
        }

        public void setTotalUsers(int totalUsers) {
            this.totalUsers = totalUsers;
        }

        public int getActiveUsers() {
            return activeUsers;
        }

        public void setActiveUsers(int activeUsers) {
            this.activeUsers = activeUsers;
        }

        public int getTotalAttendanceRecords() {
            return totalAttendanceRecords;
        }

        public void setTotalAttendanceRecords(int totalAttendanceRecords) {
            this.totalAttendanceRecords = totalAttendanceRecords;
        }

        public String getLatestRecordDate() {
            return latestRecordDate;
        }

        public void setLatestRecordDate(String latestRecordDate) {
            this.latestRecordDate = latestRecordDate;
        }
    }

    public static class DataStatistics {
        private int currentMonthRecords;
        private int incompleteRecords;
        
        // Getters and Setters
        public int getCurrentMonthRecords() {
            return currentMonthRecords;
        }

        public void setCurrentMonthRecords(int currentMonthRecords) {
            this.currentMonthRecords = currentMonthRecords;
        }

        public int getIncompleteRecords() {
            return incompleteRecords;
        }

        public void setIncompleteRecords(int incompleteRecords) {
            this.incompleteRecords = incompleteRecords;
        }
    }

    public static class BatchExecutionHistory {
        private String type;
        private LocalDateTime executedAt;
        private String status;
        private String duration;
        
        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public LocalDateTime getExecutedAt() {
            return executedAt;
        }

        public void setExecutedAt(LocalDateTime executedAt) {
            this.executedAt = executedAt;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }
    }
}