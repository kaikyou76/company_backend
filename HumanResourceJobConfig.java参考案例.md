package com.example.orgchart_api.batch.job;

import com.example.orgchart_api.batch.step.writer.LoadStaffInfoWriter;
import com.example.orgchart_api.batch.util.BatchSettings;
import com.example.orgchart_api.batch.service.BatchValidationService;
import com.example.orgchart_api.batch.service.BatchRecoveryService;
import com.example.orgchart_api.batch.service.DatabaseHealthCheckService;
import com.example.orgchart_api.batch.monitoring.BatchMonitoringService;
import com.example.orgchart_api.batch.monitoring.BatchDiagnosticLogger;
import com.example.orgchart_api.batch.exception.BatchValidationException;
import com.example.orgchart_api.batch.exception.BatchDatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
public class HumanResourceJobConfig {
    private static final Logger logger = LoggerFactory.getLogger(HumanResourceJobConfig.class);

    private final JobRepository jobRepository;
    private final BatchSettings batchSettings;
    private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbcTemplate;
    private final LoadStaffInfoWriter loadStaffInfoWriter;
    private final BatchValidationService validationService;
    private final BatchRecoveryService recoveryService;
    private final DatabaseHealthCheckService healthCheckService;
    private final BatchMonitoringService monitoringService;
    private final BatchDiagnosticLogger diagnosticLogger;

    public HumanResourceJobConfig(
            JobRepository jobRepository,
            BatchSettings batchSettings,
            PlatformTransactionManager transactionManager,
            JdbcTemplate jdbcTemplate,
            LoadStaffInfoWriter loadStaffInfoWriter,
            BatchValidationService validationService,
            BatchRecoveryService recoveryService,
            DatabaseHealthCheckService healthCheckService,
            BatchMonitoringService monitoringService,
            BatchDiagnosticLogger diagnosticLogger) {
        this.jobRepository = jobRepository;
        this.batchSettings = batchSettings;
        this.transactionManager = transactionManager;
        this.jdbcTemplate = jdbcTemplate;
        this.loadStaffInfoWriter = loadStaffInfoWriter;
        this.validationService = validationService;
        this.recoveryService = recoveryService;
        this.healthCheckService = healthCheckService;
        this.monitoringService = monitoringService;
        this.diagnosticLogger = diagnosticLogger;
    }

    @Bean
    public Job humanResourceBatchJob() {
        return new JobBuilder("humanResourceBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobExecutionListener())
                .start(preValidationStep())
                .next(recoveryCheckStep())
                .next(stagingTableInitializationStep())
                .next(loadStaffInfoStep())
                .next(postValidationStep())
                .next(thresholdCheckStep())
                .build();
    }

    // ジョブ実行リスナー
    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                logger.info("ジョブ開始: {}", jobExecution.getJobParameters());
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                if (jobExecution.getStatus() == BatchStatus.FAILED) {
                    logger.warn("ジョブ失敗: {}", jobExecution.getExitStatus().getExitDescription());
                }
            }
        };
    }

    // ステージングテーブル初期化ステップ
    public Step stagingTableInitializationStep() {
        return new StepBuilder("stagingTableInitializationStep", jobRepository)
                .tasklet(stagingInitializationTasklet(), transactionManager)
                .allowStartIfComplete(true)
                .startLimit(3)
                .listener(stepExecutionListener())
                .build();
    }

    // ステップ実行リスナー
    @Bean
    public StepExecutionListener stepExecutionListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                logger.info("ステップ開始: {}", stepExecution.getStepName());
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                logger.info("ステップ完了: {}", stepExecution.getStatus());
                return stepExecution.getExitStatus();
            }
        };
    }

    // ステージングテーブル初期化処理
    @Bean
    public Tasklet stagingInitializationTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== ステージングテーブル初期化開始 =====");

            try {
                jdbcTemplate.update("DELETE FROM biz_ad");
                jdbcTemplate.update("DELETE FROM biz_department");
                jdbcTemplate.update("DELETE FROM biz_employee");
                jdbcTemplate.update("DELETE FROM biz_organization");
                jdbcTemplate.update("DELETE FROM biz_shift");

                logger.info("===== 初期化完了 =====");
                return RepeatStatus.FINISHED;
            } catch (DataAccessException e) {
                logger.error("データアクセスエラー（再試行可能）", e);
                throw new RetryableException("データベースエラー", e);
            } catch (Exception e) {
                logger.error("初期化処理失敗（致命的）", e);
                throw e;
            }
        };
    }

    // メイン処理ステップ
    @Bean
    public Step loadStaffInfoStep() {
        return new StepBuilder("loadStaffInfoStep", jobRepository)
                .tasklet(loadStaffInfoWriter, transactionManager)
                .allowStartIfComplete(true)
                .startLimit(3)
                .build();
    }

    // 閾値チェックステップ
    @Bean
    public Step thresholdCheckStep() {
        return new StepBuilder("thresholdCheckStep", jobRepository)
                .tasklet(thresholdCheckTasklet(), transactionManager)
                .allowStartIfComplete(true)
                .build();
    }

    // 統合された閾値チェック処理
    @Bean
    public Tasklet thresholdCheckTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== 閾値チェック開始 =====");

            // 閾値チェック実行
            boolean thresholdExceeded = checkThresholds();

            if (thresholdExceeded) {
                logger.error("閾値超過エラーが発生しました");
                throw new RuntimeException("閾値超過");
            }

            logger.info("===== チェック完了 =====");
            return RepeatStatus.FINISHED;
        };
    }

    // 閾値チェック実装
    private boolean checkThresholds() {
        // 従業員数が許容範囲を超えていないか確認
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_employee", Integer.class);

        return count > batchSettings.getEmployeeThreshold();
    }

    // 再試行可能例外クラス
    private static class RetryableException extends DataAccessException {
        public RetryableException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

} // 事前検証ス
    テップ

    @Bean
    public Step preValidationStep() {
        return new StepBuilder("preValidationStep", jobRepository)
                .tasklet(preValidationTasklet(), transactionManager)
                .allowStartIfComplete(true)
                .listener(stepExecutionListener())
                .build();
    }

    @Bean
    public Tasklet preValidationTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== 事前検証開始 =====");

            try {
                // バッチ設定の検証
                var configResult = validationService.validateBatchConfiguration();
                if (!configResult.isValid()) {
                    logger.error("バッチ設定検証失敗");
                    configResult.getErrors()
                            .forEach(error -> logger.error("設定エラー: {} - {}", error.getErrorCode(), error.getMessage()));
                    throw new BatchValidationException("CONFIG_001", "バッチ設定が無効です");
                }

                // データベース接続性検証
                var dbResult = validationService.validateDatabaseConnectivity();
                if (!dbResult.isValid()) {
                    logger.error("データベース接続検証失敗");
                    throw new BatchDatabaseException("DB_001", "データベースに接続できません");
                }

                // CSVファイル存在確認
                String[] requiredFiles = {
                        batchSettings.getBizAdCsvFileName(),
                        batchSettings.getBizDepartmentCsvFileName(),
                        batchSettings.getBizEmployeeCsvFileName(),
                        batchSettings.getBizOrganizationCsvFileName(),
                        batchSettings.getBizShiftCsvFileName()
                };

                for (String fileName : requiredFiles) {
                    String filePath = batchSettings.getCsvFtpDir() + "/" + fileName;
                    var fileResult = validationService.validateCsvFileAccessibility(filePath);
                    if (!fileResult.isValid()) {
                        logger.error("CSVファイル検証失敗: {}", filePath);
                        throw new BatchValidationException("FILE_001", "必須CSVファイルが見つかりません: " + fileName);
                    }
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

    // 復旧チェックステップ
    @Bean
    public Step recoveryCheckStep() {
        return new StepBuilder("recoveryCheckStep", jobRepository)
                .tasklet(recoveryCheckTasklet(), transactionManager)
                .allowStartIfComplete(true)
                .listener(stepExecutionListener())
                .build();
    }

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

    // 事後検証ステップ
    @Bean
    public Step postValidationStep() {
        return new StepBuilder("postValidationStep", jobRepository)
                .tasklet(postValidationTasklet(), transactionManager)
                .allowStartIfComplete(true)
                .listener(stepExecutionListener())
                .build();
    }

    @Bean
    public Tasklet postValidationTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== 事後検証開始 =====");

            try {
                // データ整合性チェック
                var integrityResult = validationService.validateDataIntegrity();
                if (!integrityResult.isValid()) {
                    logger.error("データ整合性検証失敗");
                    integrityResult.getErrors().forEach(
                            error -> logger.error("整合性エラー: {} - {}", error.getErrorCode(), error.getMessage()));
                    throw new BatchValidationException("INTEGRITY_001", "データ整合性チェックに失敗しました");
                }

                // ビジネスルール検証
                var businessRuleResult = validationService.validateBusinessRules();
                if (!businessRuleResult.isValid()) {
                    logger.error("ビジネスルール検証失敗");
                    businessRuleResult.getErrors().forEach(
                            error -> logger.error("ビジネスルールエラー: {} - {}", error.getErrorCode(), error.getMessage()));
                    throw new BatchValidationException("BUSINESS_001", "ビジネスルール検証に失敗しました");
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

    // 拡張されたジョブ実行リスナー
    @Bean
    public JobExecutionListener enhancedJobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                Long jobId = jobExecution.getJobId();
                logger.info("バッチジョブ開始: ID={}, Parameters={}", jobId, jobExecution.getJobParameters());

                // 監視開始
                monitoringService.startMonitoring();

                // 診断ログ記録
                diagnosticLogger.logJobStart(jobId, jobExecution.getJobInstance().getJobName());
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                Long jobId = jobExecution.getJobId();
                boolean success = jobExecution.getStatus() == BatchStatus.COMPLETED;

                try {
                    // 処理されたレコード数を取得（概算）
                    int totalRecords = getTotalProcessedRecords(jobExecution);

                    // 診断ログ記録
                    diagnosticLogger.logJobEnd(jobId, jobExecution.getJobInstance().getJobName(), success,
                            totalRecords);

                    if (success) {
                        logger.info("バッチジョブ正常完了: ID={}, 処理レコード数={}", jobId, totalRecords);
                    } else {
                        logger.error("バッチジョブ失敗: ID={}, Status={}, ExitDescription={}",
                                jobId, jobExecution.getStatus(), jobExecution.getExitStatus().getExitDescription());

                        // 失敗時の復旧レポート生成
                        try {
                            String recoveryReport = recoveryService.generateRecoveryReport();
                            logger.info("復旧レポート:\n{}", recoveryReport);
                        } catch (Exception e) {
                            logger.error("復旧レポート生成に失敗しました", e);
                        }
                    }

                } finally {
                    // 監視停止
                    monitoringService.stopMonitoring();

                    // リソース使用状況ログ
                    diagnosticLogger.logResourceUsage();
                }
            }
        };
    }

    // 拡張されたステップ実行リスナー
    @Bean
    public StepExecutionListener enhancedStepExecutionListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                String stepName = stepExecution.getStepName();
                logger.info("ステップ開始: {}", stepName);

                // ステップ開始ログ
                diagnosticLogger.logStepStart(stepName, java.util.Map.of(
                        "jobId", stepExecution.getJobExecution().getJobId(),
                        "stepId", stepExecution.getId()));
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                String stepName = stepExecution.getStepName();
                boolean success = stepExecution.getStatus() == BatchStatus.COMPLETED;
                int processedRecords = stepExecution.getReadCount() + stepExecution.getWriteCount();

                // ステップ完了ログ
                diagnosticLogger.logStepEnd(stepName, success, processedRecords, java.util.Map.of(
                        "readCount", stepExecution.getReadCount(),
                        "writeCount", stepExecution.getWriteCount(),
                        "commitCount", stepExecution.getCommitCount(),
                        "rollbackCount", stepExecution.getRollbackCount()));

                logger.info("ステップ完了: {} - Status: {}, 処理レコード数: {}",
                        stepName, stepExecution.getStatus(), processedRecords);

                return stepExecution.getExitStatus();
            }
        };
    }

    // 処理されたレコード数の概算取得
    private int getTotalProcessedRecords(JobExecution jobExecution) {
        return jobExecution.getStepExecutions().stream()
                .mapToInt(step -> step.getReadCount() + step.getWriteCount())
                .sum();
    }

    // 拡張された閾値チェック実装
    private boolean checkThresholds() {
        try {
            // 従業員数チェック
            Integer empCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_employee", Integer.class);
            if (empCount != null && empCount > batchSettings.getEmployeeThreshold()) {
                logger.error("従業員数が閾値を超過: {} > {}", empCount, batchSettings.getEmployeeThreshold());
                return true;
            }

            // 組織数チェック
            Integer orgCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_organization", Integer.class);
            if (orgCount != null && orgCount > batchSettings.getOrganizationThreshold()) {
                logger.error("組織数が閾値を超過: {} > {}", orgCount, batchSettings.getOrganizationThreshold());
                return true;
            }

            // 部署数チェック
            Integer deptCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_department", Integer.class);
            if (deptCount != null && deptCount > batchSettings.getDepartmentThreshold()) {
                logger.error("部署数が閾値を超過: {} > {}", deptCount, batchSettings.getDepartmentThreshold());
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.error("閾値チェック中にエラーが発生しました", e);
            return true; // エラー時は閾値超過として扱う
        }
    }
}