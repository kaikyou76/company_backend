//
// import jakarta.persistence.*;
// 
// import java.time.LocalDateTime;
// 
// @Entity
// @Table(name = "batch_job_execution")
// public class BatchJobExecution {
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     @Column(name = "job_execution_id")
//     private Long jobExecutionId;
// 
//     @Column(name = "version")
//     private Long version;
// 
//     @Column(name = "job_instance_id", nullable = false)
//     private Long jobInstanceId;
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
//     public BatchJobExecution() {}
// 
//     // Getters and Setters
//     public Long getJobExecutionId() {
//         return jobExecutionId;
//     }
// 
//     public void setJobExecutionId(Long jobExecutionId) {
//         this.jobExecutionId = jobExecutionId;
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
//     public Long getJobInstanceId() {
//         return jobInstanceId;
//     }
// 
//     public void setJobInstanceId(Long jobInstanceId) {
//         this.jobInstanceId = jobInstanceId;
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
