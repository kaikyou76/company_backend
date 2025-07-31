//
// import jakarta.persistence.*;
// 
// @Entity
// @Table(name = "batch_step_execution_context")
// public class BatchStepExecutionContext {
//     @Id
//     @Column(name = "step_execution_id")
//     private Long stepExecutionId;
// 
//     @Column(name = "short_context", nullable = false)
//     private String shortContext;
// 
//     @Column(name = "serialized_context")
//     private String serializedContext;
// 
//     // Constructors
//     public BatchStepExecutionContext() {}
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
