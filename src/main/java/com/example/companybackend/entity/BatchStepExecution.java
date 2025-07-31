//
// import jakarta.persistence.*;
// 
// import java.time.LocalDateTime;
// 
// @Entity
// @Table(name = "batch_step_execution")
// public class BatchStepExecution {
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     @Column(name = "step_execution_id")
//     private Long stepExecutionId;
// 
//     @Column(name = "version", nullable = false)
//     private Long version;
// 
//     @Column(name = "step_name", nullable = false)
//     private String stepName;
// 
//     @Column(name = "job_execution_id", nullable = false)
//     private Long jobExecutionId;
// 
//     @Column(name = "create_time", nullable = false)
//     private LocalDateTime createTime;
// 
//     @Column(name = "start_time")
//     private LocalDateTime startTime;
// 
//     @Column(name = "end_time")
//     private LocalDateTime endTime;
// 
//     @Column(name = "status")
//     private String status;
// 
//     @Column(name = "commit_count")
//     private Long commitCount;
// 
//     @Column(name = "read_count")
//     private Long readCount;
// 
//     @Column(name = "filter_count")
//     private Long filterCount;
// 
//     @Column(name = "write_count")
//     private Long writeCount;
// 
//     @Column(name = "read_skip_count")
//     private Long readSkipCount;
// 
//     @Column(name = "write_skip_count")
//     private Long writeSkipCount;
// 
//     @Column(name = "process_skip_count")
//     private Long processSkipCount;
// 
//     @Column(name = "rollback_count")
//     private Long rollbackCount;
// 
//     @Column(name = "exit_code")
//     private String exitCode;
// 
//     @Column(name = "exit_message")
//     private String exitMessage;
// 
//     @Column(name = "last_updated")
//     private LocalDateTime lastUpdated;
// 
//     // Constructors
//     public BatchStepExecution() {}
// 
//     // Getters and Setters
//     public Long getStepExecutionId() {
//         return stepExecutionId;
//     }
// 
//     public void setStepExecutionId(Long stepExecutionId) {
//         this.stepExecutionId = stepExecutionId;
//     }
// 
//     public Long getVersion() {
//         return version;
//     }
// 
//     public void setVersion(Long version) {
//         this.version = version;
//     }
// 
//     public String getStepName() {
//         return stepName;
//     }
// 
//     public void setStepName(String stepName) {
//         this.stepName = stepName;
//     }
// 
//     public Long getJobExecutionId() {
//         return jobExecutionId;
//     }
// 
//     public void setJobExecutionId(Long jobExecutionId) {
//         this.jobExecutionId = jobExecutionId;
//     }
// 
//     public LocalDateTime getCreateTime() {
//         return createTime;
//     }
// 
//     public void setCreateTime(LocalDateTime createTime) {
//         this.createTime = createTime;
//     }
// 
//     public LocalDateTime getStartTime() {
//         return startTime;
//     }
// 
//     public void setStartTime(LocalDateTime startTime) {
//         this.startTime = startTime;
//     }
// 
//     public LocalDateTime getEndTime() {
//         return endTime;
//     }
// 
//     public void setEndTime(LocalDateTime endTime) {
//         this.endTime = endTime;
//     }
// 
//     public String getStatus() {
//         return status;
//     }
// 
//     public void setStatus(String status) {
//         this.status = status;
//     }
// 
//     public Long getCommitCount() {
//         return commitCount;
//     }
// 
//     public void setCommitCount(Long commitCount) {
//         this.commitCount = commitCount;
//     }
// 
//     public Long getReadCount() {
//         return readCount;
//     }
// 
//     public void setReadCount(Long readCount) {
//         this.readCount = readCount;
//     }
// 
//     public Long getFilterCount() {
//         return filterCount;
//     }
// 
//     public void setFilterCount(Long filterCount) {
//         this.filterCount = filterCount;
//     }
// 
//     public Long getWriteCount() {
//         return writeCount;
//     }
// 
//     public void setWriteCount(Long writeCount) {
//         this.writeCount = writeCount;
//     }
// 
//     public Long getReadSkipCount() {
//         return readSkipCount;
//     }
// 
//     public void setReadSkipCount(Long readSkipCount) {
//         this.readSkipCount = readSkipCount;
//     }
// 
//     public Long getWriteSkipCount() {
//         return writeSkipCount;
//     }
// 
//     public void setWriteSkipCount(Long writeSkipCount) {
//         this.writeSkipCount = writeSkipCount;
//     }
// 
//     public Long getProcessSkipCount() {
//         return processSkipCount;
//     }
// 
//     public void setProcessSkipCount(Long processSkipCount) {
//         this.processSkipCount = processSkipCount;
//     }
// 
//     public Long getRollbackCount() {
//         return rollbackCount;
//     }
// 
//     public void setRollbackCount(Long rollbackCount) {
//         this.rollbackCount = rollbackCount;
//     }
// 
//     public String getExitCode() {
//         return exitCode;
//     }
// 
//     public void setExitCode(String exitCode) {
//         this.exitCode = exitCode;
//     }
// 
//     public String getExitMessage() {
//         return exitMessage;
//     }
// 
//     public void setExitMessage(String exitMessage) {
//         this.exitMessage = exitMessage;
//     }
// 
//     public LocalDateTime getLastUpdated() {
//         return lastUpdated;
//     }
// 
//     public void setLastUpdated(LocalDateTime lastUpdated) {
//         this.lastUpdated = lastUpdated;
//     }
// }
