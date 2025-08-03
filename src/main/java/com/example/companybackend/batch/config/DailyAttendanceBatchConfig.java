package com.example.companybackend.batch.config;

import com.example.companybackend.batch.listener.EnhancedJobExecutionListener;
import com.example.companybackend.batch.listener.EnhancedStepExecutionListener;
import com.example.companybackend.batch.processor.DailyWorkTimeProcessor;
import com.example.companybackend.batch.processor.MonthlyWorkTimeProcessor;
import com.example.companybackend.batch.processor.OvertimeMonitoringProcessor;
import com.example.companybackend.batch.reader.AttendanceRecordReader;
import com.example.companybackend.batch.reader.MonthlySummaryReader;
import com.example.companybackend.batch.reader.OvertimeMonitoringReader;
import com.example.companybackend.batch.writer.AttendanceSummaryWriter;
import com.example.companybackend.batch.writer.OvertimeReportWriter;
import com.example.companybackend.batch.service.BatchValidationService;
import com.example.companybackend.batch.service.BatchRecoveryService;
import com.example.companybackend.batch.service.BatchValidationServiceResult;
import com.example.companybackend.service.BatchMonitoringService;
import com.example.companybackend.batch.util.BatchDiagnosticLogger;
import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.entity.OvertimeReport;
import com.example.companybackend.repository.AttendanceRecordRepository;
import com.example.companybackend.repository.AttendanceSummaryRepository;
import com.example.companybackend.repository.HolidayRepository;
import com.example.companybackend.repository.OvertimeReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DailyAttendanceBatchConfig {

    private static final Logger logger = LoggerFactory.getLogger(DailyAttendanceBatchConfig.class);

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 追加: AttendanceRecordRepositoryの注入
    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    // 追加: HolidayRepositoryの注入
    @Autowired
    private HolidayRepository holidayRepository;

    // 追加: AttendanceSummaryRepositoryの注入
    @Autowired
    private AttendanceSummaryRepository attendanceSummaryRepository;

    // 追加: OvertimeReportRepositoryの注入
    @Autowired
    private OvertimeReportRepository overtimeReportRepository;

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
    public Job paidLeaveUpdateJob() {
        return new JobBuilder("paidLeaveUpdateJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(enhancedJobExecutionListener())
                .start(preValidationStep())
                .next(recoveryCheckStep())
                .next(dataInitializationStep())
                .next(attendanceProcessingStep()) // 可以根据需要替换为专用的处理步骤
                .next(postValidationStep())
                .next(thresholdCheckStep())
                .build();
    }

    // dataCleanupJobはDataCleanupBatchConfigで定義されているため、ここでは削除

    @Bean
    public Job dataRepairJob() {
        return new JobBuilder("dataRepairJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(enhancedJobExecutionListener())
                .start(preValidationStep())
                .next(recoveryCheckStep())
                .next(dataInitializationStep())
                .next(attendanceProcessingStep()) // 可以根据需要替换为专用的处理步骤
                .next(postValidationStep())
                .next(thresholdCheckStep())
                .build();
    }

    @Bean
    public Job overtimeMonitoringBatchJob() {
        return new JobBuilder("overtimeMonitoringBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(enhancedJobExecutionListener())
                .start(preValidationStep())
                .next(recoveryCheckStep())
                .next(overtimeDataInitializationStep())
                .next(overtimeMonitoringProcessingStep())
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
                .tasklet(preValidationTasklet(), transactionManager)
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    public Step recoveryCheckStep() {
        return new StepBuilder("recoveryCheckStep", jobRepository)
                .tasklet(recoveryCheckTasklet(), transactionManager)
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    public Step dataInitializationStep() {
        return new StepBuilder("dataInitializationStep", jobRepository)
                .tasklet(dataInitializationTasklet(), transactionManager)
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

    @Bean
    public Step monthlyAttendanceProcessingStep() {
        return new StepBuilder("monthlyAttendanceProcessingStep", jobRepository)
                .<AttendanceRecord, AttendanceSummary>chunk(10, transactionManager)
                .reader(monthlySummaryItemReader())
                .processor(monthlyWorkTimeProcessor())
                .writer(attendanceSummaryWriter())
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    public Step overtimeMonitoringProcessingStep() {
        return new StepBuilder("overtimeMonitoringProcessingStep", jobRepository)
                .<AttendanceSummary, OvertimeReport>chunk(10, transactionManager)
                .reader(overtimeMonitoringItemReader())
                .processor(overtimeMonitoringProcessor())
                .writer(overtimeReportWriter())
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    public Step overtimeDataInitializationStep() {
        return new StepBuilder("overtimeDataInitializationStep", jobRepository)
                .tasklet(overtimeDataInitializationTasklet(), transactionManager)
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    public Step postValidationStep() {
        return new StepBuilder("postValidationStep", jobRepository)
                .tasklet(postValidationTasklet(), transactionManager)
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    public Step thresholdCheckStep() {
        return new StepBuilder("thresholdCheckStep", jobRepository)
                .tasklet(thresholdCheckTasklet(), transactionManager)
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
        DailyWorkTimeProcessor processor = new DailyWorkTimeProcessor();
        // 手动注入依赖
        processor.setAttendanceRecordRepository(attendanceRecordRepository);
        processor.setHolidayRepository(holidayRepository);
        return processor;
    }

    @Bean
    @StepScope
    public ItemReader<AttendanceRecord> monthlySummaryItemReader() {
        return new MonthlySummaryReader(attendanceRecordRepository).reader();
    }

    @Bean
    @StepScope
    public ItemProcessor<AttendanceRecord, AttendanceSummary> monthlyWorkTimeProcessor() {
        MonthlyWorkTimeProcessor processor = new MonthlyWorkTimeProcessor();
        // 手动注入依赖
        processor.setAttendanceSummaryRepository(attendanceSummaryRepository);
        return processor;
    }

    @Bean
    @StepScope
    public ItemReader<AttendanceSummary> overtimeMonitoringItemReader() {
        return new OvertimeMonitoringReader(attendanceSummaryRepository).reader();
    }

    @Bean
    @StepScope
    public ItemProcessor<AttendanceSummary, OvertimeReport> overtimeMonitoringProcessor() {
        OvertimeMonitoringProcessor processor = new OvertimeMonitoringProcessor();
        // 手动注入依赖
        processor.setOvertimeReportRepository(overtimeReportRepository);
        return processor;
    }

    @Bean
    @StepScope
    public ItemWriter<AttendanceSummary> attendanceSummaryWriter() {
        return new AttendanceSummaryWriter();
    }

    @Bean
    @StepScope
    public ItemWriter<OvertimeReport> overtimeReportWriter() {
        return new OvertimeReportWriter();
    }

    // 事前検証処理
    @Bean
    public Tasklet preValidationTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== 事前検証開始 =====");

            try {
                // バッチ設定の検証
                BatchValidationServiceResult configResult = validationService.validateBatchConfiguration();
                if (!configResult.isValid()) {
                    logger.error("バッチ設定検証失敗");
                    configResult.getErrors()
                            .forEach(error -> logger.error("設定エラー: {} - {}", error.getErrorCode(), error.getMessage()));
                    throw new RuntimeException("バッチ設定が無効です");
                }

                // データベース接続性検証
                BatchValidationServiceResult dbResult = validationService.validateDatabaseConnectivity();
                if (!dbResult.isValid()) {
                    logger.error("データベース接続検証失敗");
                    throw new RuntimeException("データベースに接続できません");
                }

                logger.info("===== 事前検証完了 =====");
                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                logger.error("事前検証中にエラーが発生しました", e);
                diagnosticLogger.logError("preValidationStep", null, e);
                throw e;
            }
        };
    }

    // 復旧チェック処理
    @Bean
    public Tasklet recoveryCheckTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== 復旧チェック開始 =====");

            try {
                // 前回の失敗したジョブのクリーンアップ
                recoveryService.cleanupFailedJobs();

                // 再開安全性チェック
                boolean isSafeToRestart = recoveryService.isRestartSafe();
                if (!isSafeToRestart) {
                    logger.warn("再開が安全ではありません。手動確認が必要です。");
                    // 警告として記録するが、処理は継続
                }

                // ロックファイルのクリーンアップ
                recoveryService.cleanupLockFiles();

                logger.info("===== 復旧チェック完了 =====");
                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                logger.error("復旧チェック中にエラーが発生しました", e);
                diagnosticLogger.logError("recoveryCheckStep", null, e);
                throw e;
            }
        };
    }

    // データ初期化処理
    @Bean
    public Tasklet dataInitializationTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== データ初期化開始 =====");

            try {
                String jobName = contribution.getStepExecution().getJobExecution().getJobInstance().getJobName();

                if ("dailyAttendanceSummaryJob".equals(jobName)) {
                    // 日次処理：既存の勤怠集計データをクリア（当日分のみ）
                    jdbcTemplate.update(
                            "DELETE FROM attendance_summaries WHERE target_date = CURRENT_DATE AND summary_type = 'daily'");
                    logger.info("日次集計データを初期化しました");
                } else if ("monthlyAttendanceSummaryJob".equals(jobName)) {
                    // 月次処理：既存の月次集計データをクリア（当月分のみ）
                    jdbcTemplate.update(
                            "DELETE FROM attendance_summaries WHERE DATE_TRUNC('month', target_date) = DATE_TRUNC('month', CURRENT_DATE) AND summary_type = 'monthly'");
                    logger.info("月次集計データを初期化しました");
                }

                logger.info("===== データ初期化完了 =====");
                return RepeatStatus.FINISHED;
            } catch (DataAccessException e) {
                logger.error("データアクセスエラー（再試行可能）", e);
                throw new RuntimeException("データベースエラー", e);
            } catch (Exception e) {
                logger.error("初期化処理失敗（致命的）", e);
                throw e;
            }
        };
    }

    // 事後検証処理
    @Bean
    public Tasklet postValidationTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== 事後検証開始 =====");

            try {
                // データ整合性チェック
                BatchValidationServiceResult integrityResult = validationService.validateDataIntegrity();
                if (!integrityResult.isValid()) {
                    logger.error("データ整合性検証失敗");
                    integrityResult.getErrors().forEach(
                            error -> logger.error("整合性エラー: {} - {}", error.getErrorCode(), error.getMessage()));
                    throw new RuntimeException("データ整合性チェックに失敗しました");
                }

                // ビジネスルール検証
                BatchValidationServiceResult businessRuleResult = validationService.validateBusinessRules();
                if (!businessRuleResult.isValid()) {
                    logger.error("ビジネスルール検証失敗");
                    businessRuleResult.getErrors().forEach(
                            error -> logger.error("ビジネスルールエラー: {} - {}", error.getErrorCode(), error.getMessage()));
                    throw new RuntimeException("ビジネスルール検証に失敗しました");
                }

                // 警告がある場合はログに記録
                if (integrityResult.hasWarnings() || businessRuleResult.hasWarnings()) {
                    logger.warn("検証で警告が発生しました");
                    integrityResult.getWarnings().forEach(
                            warning -> logger.warn("整合性警告: {} - {}", warning.getWarningCode(), warning.getMessage()));
                    businessRuleResult.getWarnings().forEach(warning -> logger.warn("ビジネスルール警告: {} - {}",
                            warning.getWarningCode(), warning.getMessage()));
                }

                logger.info("===== 事後検証完了 =====");
                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                logger.error("事後検証中にエラーが発生しました", e);
                diagnosticLogger.logError("postValidationStep", null, e);
                throw e;
            }
        };
    }

    // 閾値チェック処理
    @Bean
    public Tasklet thresholdCheckTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== 閾値チェック開始 =====");

            try {
                // 閾値チェック実行
                boolean thresholdExceeded = checkThresholds();

                if (thresholdExceeded) {
                    logger.error("閾値超過エラーが発生しました");
                    throw new RuntimeException("閾値超過");
                }

                logger.info("===== チェック完了 =====");
                return RepeatStatus.FINISHED;
            } catch (Exception e) {
                logger.error("閾値チェック中にエラーが発生しました", e);
                throw e;
            }
        };
    }

    // 残業データ初期化処理
    @Bean
    public Tasklet overtimeDataInitializationTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== 残業データ初期化開始 =====");

            try {
                // 既存の残業レポートデータをクリア（当月分のみ）
                jdbcTemplate.update(
                        "DELETE FROM overtime_reports WHERE DATE_TRUNC('month', target_month) = DATE_TRUNC('month', CURRENT_DATE)");
                logger.info("残業レポートデータを初期化しました");

                logger.info("===== 残業データ初期化完了 =====");
                return RepeatStatus.FINISHED;
            } catch (DataAccessException e) {
                logger.error("残業データアクセスエラー（再試行可能）", e);
                throw new RuntimeException("残業データベースエラー", e);
            } catch (Exception e) {
                logger.error("残業初期化処理失敗（致命的）", e);
                throw e;
            }
        };
    }

    // 閾値チェック実装
    private boolean checkThresholds() {
        try {
            // 集計された勤怠レコード数チェック
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM attendance_summaries WHERE date = CURRENT_DATE", Integer.class);

            // 仮の閾値: 10000件
            return count != null && count > 10000;
        } catch (Exception e) {
            logger.error("閾値チェック中にエラーが発生しました", e);
            return true; // エラー時は閾値超過として扱う
        }
    }
}