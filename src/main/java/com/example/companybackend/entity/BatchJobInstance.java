//
// import jakarta.persistence.*;
// 
// import java.time.LocalDateTime;
// 
// @Entity
// @Table(name = "batch_job_instance")
// public class BatchJobInstance {
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     @Column(name = "job_instance_id")
//     private Long jobInstanceId;
// 
//     @Column(name = "version")
//     private Long version;
// 
//     @Column(name = "job_name", nullable = false)
//     private String jobName;
// 
//     @Column(name = "job_key", nullable = false)
//     private String jobKey;
// 
//     // Constructors
//     public BatchJobInstance() {}
// 
//     // Getters and Setters
//     public Long getJobInstanceId() {
//         return jobInstanceId;
//     }
// 
//     public void setJobInstanceId(Long jobInstanceId) {
//         this.jobInstanceId = jobInstanceId;
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
//     public String getJobName() {
//         return jobName;
//     }
// 
//     public void setJobName(String jobName) {
//         this.jobName = jobName;
//     }
// 
//     public String getJobKey() {
//         return jobKey;
//     }
// 
//     public void setJobKey(String jobKey) {
//         this.jobKey = jobKey;
//     }
// }
