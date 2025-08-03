package com.example.companybackend.batch.config;

import com.example.companybackend.batch.listener.EnhancedJobExecutionListener;
import com.example.companybackend.batch.listener.EnhancedStepExecutionListener;
import com.example.companybackend.batch.processor.DataCleanupProcessor;
import com.example.companybackend.batch.reader.DataCleanupReader;
import com.example.companybackend.batch.writer.DataCleanupWriter;
import com.example.companybackend.batch.service.BatchValidationService;
import com.example.companybackend.batch.service.BatchRecoveryService;
import com.example.companybackend.batch.service.BatchValidationServiceResult;
import com.example.companybackend.batch.util.BatchDiagnosticLogger;
import com.example.companybackend.entity.SystemLog;
import com.example.companybackend.repository.SystemLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
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

import javax.sql.DataSource;
import java.time.OffsetDateTime;

@Configuration
@EnableBatchProcessing
public class DataCleanupBatchConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataCleanupBatchConfig.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private BatchValidationService validationService;

    @Autowired
    private BatchRecoveryService recoveryService;

    @Autowired
    private BatchDiagnosticLogger diagnosticLogger;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SystemLogRepository systemLogRepository;

    @Autowired
    private DataSource dataSource;

    @Bean
    public Job dataCleanupJob() {
        return new JobBuilder("dataCleanupJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(enhancedJobExecutionListener())
                .start(preValidationStep())
                .next(recoveryCheckStep())
                .next(dataCleanupInitializationStep())
                .next(dataCleanupProcessingStep())
                .next(postValidationStep())
                .next(cleanupStatisticsStep())
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
    public Step dataCleanupInitializationStep() {
        return new StepBuilder("dataCleanupInitializationStep", jobRepository)
                .tasklet(dataCleanupInitializationTasklet(), transactionManager)
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    public Step dataCleanupProcessingStep() {
        return new StepBuilder("dataCleanupProcessingStep", jobRepository)
                .<SystemLog, SystemLog>chunk(100, transactionManager)
                .reader(dataCleanupItemReader())
                .processor(dataCleanupProcessor())
                .writer(dataCleanupWriter())
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
    public Step cleanupStatisticsStep() {
        return new StepBuilder("cleanupStatisticsStep", jobRepository)
                .tasklet(cleanupStatisticsTasklet(), transactionManager)
                .listener(enhancedStepExecutionListener())
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SystemLog> dataCleanupItemReader() {
        return new DataCleanupReader(systemLogRepository, dataSource).reader();
    }

    @Bean
    @StepScope
    public ItemProcessor<SystemLog, SystemLog> dataCleanupProcessor() {
        DataCleanupProcessor processor = new DataCleanupProcessor();
        processor.setSystemLogRepository(systemLogRepository);
        return processor;
    }

    @Bean
    @StepScope
    public ItemWriter<SystemLog> dataCleanupWriter() {
        return new DataCleanupWriter();
    }

    // 事前検証処理
    @Bean
    public Tasklet preValidationTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== データクリーンアップ事前検証開始 =====");

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

                // 削除対象データ数の確認
                OffsetDateTime cutoffDate = OffsetDateTime.now().minusMonths(12);
                long deleteTargetCount = systemLogRepository.countByCreatedAtBefore(cutoffDate);
                logger.info("削除対象データ数: {} 件 (カットオフ日時: {})", deleteTargetCount, cutoffDate);

                if (deleteTargetCount > 100000) {
                    logger.warn("削除対象データが大量です: {} 件", deleteTargetCount);
                }

                logger.info("===== データクリーンアップ事前検証完了 =====");
                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                logger.error("データクリーンアップ事前検証中にエラーが発生しました", e);
                diagnosticLogger.logError("dataCleanupPreValidationStep", null, e);
                throw e;
            }
        };
    }

    // 復旧チェック処理
    @Bean
    public Tasklet recoveryCheckTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== データクリーンアップ復旧チェック開始 =====");

            try {
                // 前回の失敗したジョブのクリーンアップ
                recoveryService.cleanupFailedJobs();

                // 再開安全性チェック
                boolean isSafeToRestart = recoveryService.isRestartSafe();
                if (!isSafeToRestart) {
                    logger.warn("再開が安全ではありません。手動確認が必要です。");
                }

                // ロックファイルのクリーンアップ
                recoveryService.cleanupLockFiles();

                logger.info("===== データクリーンアップ復旧チェック完了 =====");
                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                logger.error("データクリーンアップ復旧チェック中にエラーが発生しました", e);
                diagnosticLogger.logError("dataCleanupRecoveryCheckStep", null, e);
                throw e;
            }
        };
    }

    // データクリーンアップ初期化処理
    @Bean
    public Tasklet dataCleanupInitializationTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== データクリーンアップ初期化開始 =====");

            try {
                // 削除対象データの統計情報を取得
                OffsetDateTime cutoffDate = OffsetDateTime.now().minusMonths(12);
                long totalCount = systemLogRepository.count();
                long deleteTargetCount = systemLogRepository.countByCreatedAtBefore(cutoffDate);
                long retainCount = totalCount - deleteTargetCount;

                logger.info("システムログ統計情報:");
                logger.info("  総レコード数: {} 件", totalCount);
                logger.info("  削除対象数: {} 件", deleteTargetCount);
                logger.info("  保持対象数: {} 件", retainCount);
                logger.info("  カットオフ日時: {}", cutoffDate);

                // バックアップテーブルの作成（必要に応じて）
                try {
                    jdbcTemplate.execute("""
                            CREATE TABLE IF NOT EXISTS system_logs_backup AS
                            SELECT * FROM system_logs WHERE 1=0
                            """);
                    logger.info("バックアップテーブルを準備しました");
                } catch (Exception e) {
                    logger.warn("バックアップテーブルの作成に失敗しました（継続します）", e);
                }

                logger.info("===== データクリーンアップ初期化完了 =====");
                return RepeatStatus.FINISHED;

            } catch (DataAccessException e) {
                logger.error("データアクセスエラー（再試行可能）", e);
                throw new RuntimeException("データベースエラー", e);
            } catch (Exception e) {
                logger.error("データクリーンアップ初期化処理失敗（致命的）", e);
                throw e;
            }
        };
    }

    // 事後検証処理
    @Bean
    public Tasklet postValidationTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== データクリーンアップ事後検証開始 =====");

            try {
                // データ整合性チェック
                BatchValidationServiceResult integrityResult = validationService.validateDataIntegrity();
                if (!integrityResult.isValid()) {
                    logger.error("データ整合性検証失敗");
                    integrityResult.getErrors().forEach(
                            error -> logger.error("整合性エラー: {} - {}", error.getErrorCode(), error.getMessage()));
                    throw new RuntimeException("データ整合性チェックに失敗しました");
                }

                // クリーンアップ後の統計確認
                long remainingCount = systemLogRepository.count();
                OffsetDateTime cutoffDate = OffsetDateTime.now().minusMonths(12);
                long oldDataCount = systemLogRepository.countByCreatedAtBefore(cutoffDate);

                logger.info("クリーンアップ後統計:");
                logger.info("  残存レコード数: {} 件", remainingCount);
                logger.info("  古いデータ残存数: {} 件", oldDataCount);

                if (oldDataCount > 0) {
                    logger.warn("古いデータが {} 件残存しています", oldDataCount);
                }

                logger.info("===== データクリーンアップ事後検証完了 =====");
                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                logger.error("データクリーンアップ事後検証中にエラーが発生しました", e);
                diagnosticLogger.logError("dataCleanupPostValidationStep", null, e);
                throw e;
            }
        };
    }

    // クリーンアップ統計処理
    @Bean
    public Tasklet cleanupStatisticsTasklet() {
        return (contribution, chunkContext) -> {
            logger.info("===== データクリーンアップ統計出力開始 =====");

            try {
                // 最終統計の出力
                long finalCount = systemLogRepository.count();
                OffsetDateTime processingTime = OffsetDateTime.now();

                logger.info("=== データクリーンアップ完了統計 ===");
                logger.info("処理完了時刻: {}", processingTime);
                logger.info("最終レコード数: {} 件", finalCount);
                logger.info("保持期間: 12ヶ月");
                logger.info("=====================================");

                // 統計情報をコンテキストに保存
                chunkContext.getStepContext().getStepExecution().getExecutionContext()
                        .put("finalRecordCount", finalCount);
                chunkContext.getStepContext().getStepExecution().getExecutionContext()
                        .put("processingCompletedAt", processingTime.toString());

                logger.info("===== データクリーンアップ統計出力完了 =====");
                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                logger.error("データクリーンアップ統計出力中にエラーが発生しました", e);
                throw e;
            }
        };
    }
}