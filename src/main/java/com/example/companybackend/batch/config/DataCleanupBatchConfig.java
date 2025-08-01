package com.example.companybackend.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
public class DataCleanupBatchConfig {
    
    
    @Bean
    public Job dataCleanupJob(JobRepository jobRepository) {
        return new JobBuilder("dataCleanupJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cleanupOldDataStep(jobRepository, null))
                .build();
    }
    
    @Bean
    public Step cleanupOldDataStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("cleanupOldDataStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // 実際のデータクリーンアップロジックをここに実装
                    // 例えば、3ヶ月以上前の古いログデータを削除する
                    // systemLogRepository.deleteByCreatedAtBefore(threeMonthsAgo);
                    
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}