//
// import jakarta.persistence.*;
// 
// @Entity
// @Table(name = "batch_job_execution_context")
// public class BatchJobExecutionContext {
//     @Id
//     @Column(name = "job_execution_id")
//     private Long jobExecutionId;
// 
//     @Column(name = "short_context", nullable = false)
//     private String shortContext;
// 
//     @Column(name = "serialized_context")
//     private String serializedContext;
// 
//     // Constructors
//     public BatchJobExecutionContext() {}
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
//     public String getShortContext() {
//         return shortContext;
//     }
// 
//     public void setShortContext(String shortContext) {
//         this.shortContext = shortContext;
//     }
// 
//     public String getSerializedContext() {
//         return serializedContext;
//     }
// 
//     public void setSerializedContext(String serializedContext) {
//         this.serializedContext = serializedContext;
//     }
// }
