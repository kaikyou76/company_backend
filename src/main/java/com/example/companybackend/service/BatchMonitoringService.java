package com.example.companybackend.service;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * バッチ監視サービス（簡略版）
 */
@Service
public class BatchMonitoringService {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BatchMonitoringService.class);

    private final JobExplorer jobExplorer;

    // リアルタイム監視状態管理
    private final AtomicBoolean isMonitoringActive = new AtomicBoolean(false);

    // 修复：使用显式构造函数替代@RequiredArgsConstructor
    public BatchMonitoringService(JobExplorer jobExplorer) {
        this.jobExplorer = jobExplorer;
    }

    /**
     * 監視開始
     */
    public void startMonitoring() {
        if (isMonitoringActive.compareAndSet(false, true)) {
            log.info("バッチ監視を開始しました");
        }
    }

    /**
     * 監視停止
     */
    public void stopMonitoring() {
        if (isMonitoringActive.compareAndSet(true, false)) {
            log.info("バッチ監視を停止しました");
        }
    }

    /**
     * 全ジョブ名取得
     */
    public List<String> getJobNames() {
        return jobExplorer.getJobNames();
    }

    /**
     * 全ジョブインスタンス取得
     */
    public List<Map<String, Object>> getAllJobInstances() {
        List<String> jobNames = jobExplorer.getJobNames();
        List<Map<String, Object>> allInstances = new ArrayList<>();
        
        for (String jobName : jobNames) {
            List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, Integer.MAX_VALUE);
            for (JobInstance instance : instances) {
                Map<String, Object> instanceInfo = new HashMap<>();
                instanceInfo.put("jobInstanceId", instance.getInstanceId());
                instanceInfo.put("jobName", instance.getJobName());
                allInstances.add(instanceInfo);
            }
        }
        
        return allInstances;
    }

    /**
     * ジョブ実行履歴取得
     */
    public List<Map<String, Object>> getJobExecutionHistory(String jobName, int page, int size) {
        List<JobInstance> instances = jobExplorer.getJobInstances(jobName, page * size, size);
        List<Map<String, Object>> executions = new ArrayList<>();
        
        for (JobInstance instance : instances) {
            List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(instance);
            for (JobExecution execution : jobExecutions) {
                Map<String, Object> executionInfo = new HashMap<>();
                executionInfo.put("jobExecutionId", execution.getId());
                executionInfo.put("jobInstanceId", execution.getJobInstance().getInstanceId());
                executionInfo.put("startTime", execution.getStartTime());
                executionInfo.put("endTime", execution.getEndTime());
                executionInfo.put("status", execution.getStatus().toString());
                executionInfo.put("exitCode", execution.getExitStatus().getExitCode());
                executions.add(executionInfo);
            }
        }
        
        return executions;
    }

    /**
     * ステップ実行履歴取得
     */
    public List<Map<String, Object>> getStepExecutionHistory(Long jobExecutionId) {
        List<Map<String, Object>> steps = new ArrayList<>();
        
        try {
            JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
            if (jobExecution != null) {
                for (org.springframework.batch.core.StepExecution stepExecution : jobExecution.getStepExecutions()) {
                    Map<String, Object> stepInfo = new HashMap<>();
                    stepInfo.put("stepExecutionId", stepExecution.getId());
                    stepInfo.put("stepName", stepExecution.getStepName());
                    stepInfo.put("startTime", stepExecution.getStartTime());
                    stepInfo.put("endTime", stepExecution.getEndTime());
                    stepInfo.put("status", stepExecution.getStatus().toString());
                    stepInfo.put("commitCount", stepExecution.getCommitCount());
                    stepInfo.put("readCount", stepExecution.getReadCount());
                    stepInfo.put("writeCount", stepExecution.getWriteCount());
                    stepInfo.put("exitCode", stepExecution.getExitStatus().getExitCode());
                    steps.add(stepInfo);
                }
            }
        } catch (Exception e) {
            log.error("ステップ実行履歴取得エラー: jobExecutionId={}", jobExecutionId, e);
        }
        
        return steps;
    }

    /**
     * 実行中ジョブ取得
     */
    public List<Map<String, Object>> getRunningJobs() {
        List<String> jobNames = jobExplorer.getJobNames();
        List<Map<String, Object>> runningJobs = new ArrayList<>();

        for (String jobName : jobNames) {
            Set<JobExecution> runningExecutions = jobExplorer.findRunningJobExecutions(jobName);
            for (JobExecution execution : runningExecutions) {
                Map<String, Object> jobInfo = new HashMap<>();
                jobInfo.put("jobExecutionId", execution.getId());
                jobInfo.put("jobName", execution.getJobInstance().getJobName());
                jobInfo.put("status", execution.getStatus().toString());
                runningJobs.add(jobInfo);
            }
        }

        return runningJobs;
    }

    /**
     * バッチ実行統計取得
     */
    public Map<String, Object> getBatchExecutionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<String> jobNames = jobExplorer.getJobNames();
        stats.put("totalJobs", jobNames.size());
        stats.put("successRate", 100.0);
        stats.put("errorRate", 0.0);
        
        return stats;
    }

    /**
     * 最新ジョブ実行情報取得
     */
    public Map<String, Object> getLatestJobExecution(String jobName) {
        List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, 1);
        if (instances.isEmpty()) {
            return Collections.emptyMap();
        }

        JobInstance latestInstance = instances.get(0);
        List<JobExecution> executions = jobExplorer.getJobExecutions(latestInstance);
        if (executions.isEmpty()) {
            return Collections.emptyMap();
        }

        JobExecution latestExecution = executions.get(0);
        Map<String, Object> info = new HashMap<>();
        info.put("jobExecutionId", latestExecution.getId());
        info.put("jobName", latestExecution.getJobInstance().getJobName());
        info.put("status", latestExecution.getStatus().toString());
        
        return info;
    }
}