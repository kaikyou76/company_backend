package com.example.companybackend.batch.service;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class BatchJobService {

    @Autowired
    private JobLauncher jobLauncher;

    /**
     * ジョブ実行
     */
    public JobExecution runJob(Job job, Map<String, Object> parameters) throws Exception {
        JobParametersBuilder builder = new JobParametersBuilder();
        
        // パラメータを追加
        if (parameters != null) {
            parameters.forEach((key, value) -> {
                if (value instanceof String) {
                    builder.addString(key, (String) value);
                } else if (value instanceof Long) {
                    builder.addLong(key, (Long) value);
                } else if (value instanceof Double) {
                    builder.addDouble(key, (Double) value);
                } else {
                    builder.addString(key, value.toString());
                }
            });
        }
        
        // 一意性のためのタイムスタンプ追加
        builder.addString("timestamp", LocalDateTime.now().toString());
        
        JobParameters jobParameters = builder.toJobParameters();
        return jobLauncher.run(job, jobParameters);
    }

    /**
     * ジョブ実行（パラメータなし）
     */
    public JobExecution runJob(Job job) throws Exception {
        return runJob(job, null);
    }

    /**
     * ジョブ停止
     */
    public boolean stopJob(Long jobExecutionId) {
        // 実装は必要に応じて追加
        return false;
    }
}