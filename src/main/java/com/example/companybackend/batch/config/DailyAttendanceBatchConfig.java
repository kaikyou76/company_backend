package com.example.companybackend.batch.config;

import com.example.companybackend.batch.listener.EnhancedJobExecutionListener;
import com.example.companybackend.batch.listener.EnhancedStepExecutionListener;
import com.example.companybackend.batch.processor.DailyWorkTimeProcessor;
import com.example.companybackend.batch.reader.AttendanceRecordReader;
import com.example.companybackend.batch.writer.AttendanceSummaryWriter;
import com.example.companybackend.batch.service.BatchValidationService;
import com.example.companybackend.batch.service.BatchRecoveryService;
import com.example.companybackend.service.BatchMonitoringService;
import com.example.companybackend.batch.util.BatchDiagnosticLogger;
import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.repository.AttendanceRecordRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DailyAttendanceBatchConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private BatchValidationService validationService;

    @Autowired
    private BatchRecoveryService recoveryService;

    @Autowired
    private BatchMonitoringService monitoringService;

    @Autowired
    private BatchDiagnosticLogger diagnosticLogger;
    
    // 追加: AttendanceRecordRepositoryの注入
    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Bean
    public Job dailyAttendanceSummaryJob() {
        return new JobBuilder("dailyAttendanceSummaryJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(enhancedJobExecutionListener())
                .start(preValidationStep())
                .next(recoveryCheckStep())
                .next(dataInitializationStep())
                .next(attendanceProcessingStep())
                .next(postValidationStep())
                .next(thresholdCheckStep())
                .build();
    }

    // Monthly attendance summary job for F-302
    @Bean
    public Job monthlyAttendanceSummaryJob() {
        return new JobBuilder("monthlyAttendanceSummaryJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(enhancedJobExecutionListener())
                .start(preValidationStep())
                .next(recoveryCheckStep())
                .next(dataInitializationStep())
                .next(monthlyAttendanceProcessingStep())
                .next(postValidationStep())
                .next(thresholdCheckStep())
                .build();
    }

    @Bean
    public EnhancedJobExecutionListener enhancedJobExecutionListener() {
        return new EnhancedJobExecutionListener();
    }

    @Bean
    public EnhancedStepExecutionListener enhancedStepExecutionListener() {
        return new EnhancedStepExecutionListener();
    }

    @Bean
    public Step preValidationStep() {
        return new StepBuilder("preValidationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // 事前検証ロジック
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    public Step recoveryCheckStep() {
        return new StepBuilder("recoveryCheckStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // 復旧チェックロジック
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    public Step dataInitializationStep() {
        return new StepBuilder("dataInitializationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // データ初期化ロジック
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    public Step attendanceProcessingStep() {
        return new StepBuilder("attendanceProcessingStep", jobRepository)
                .<AttendanceRecord, AttendanceSummary>chunk(10, transactionManager)
                .reader(attendanceRecordItemReader())
                .processor(dailyWorkTimeProcessor())
                .writer(attendanceSummaryWriter())
                .listener(enhancedStepExecutionListener())
                .build();
    }

    // Monthly processing step for F-302
    @Bean
    public Step monthlyAttendanceProcessingStep() {
        return new StepBuilder("monthlyAttendanceProcessingStep", jobRepository)
                .<AttendanceRecord, AttendanceSummary>chunk(10, transactionManager)
                .reader(attendanceRecordItemReader())
                .processor(dailyWorkTimeProcessor()) // Can be modified for monthly processing
                .writer(attendanceSummaryWriter())
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    public Step postValidationStep() {
        return new StepBuilder("postValidationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // 事後検証ロジック
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    public Step thresholdCheckStep() {
        return new StepBuilder("thresholdCheckStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // 閾値チェックロジック
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<AttendanceRecord> attendanceRecordItemReader() {
        return new AttendanceRecordReader(attendanceRecordRepository).reader();
    }

    @Bean
    @StepScope
    public ItemProcessor<AttendanceRecord, AttendanceSummary> dailyWorkTimeProcessor() {
        return new DailyWorkTimeProcessor();
    }

    @Bean
    @StepScope
    public ItemWriter<AttendanceSummary> attendanceSummaryWriter() {
        return new AttendanceSummaryWriter();
    }
}