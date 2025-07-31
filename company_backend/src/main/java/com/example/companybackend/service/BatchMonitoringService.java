import com.example.companybackend.batch.util.BatchSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * エンタープライズ級バッチ監視サービス
 * 既存Spring Batchテーブル（comsys_dump.sql）完全活用
 * リアルタイム監視・診断・アラート機能搭載
 * 
 * 活用テーブル：
 * - batch_job_execution（ジョブ実行履歴・リアルタイム監視）
 * - batch_job_instance（ジョブインスタンス管理）
 * - batch_step_execution（ステップ詳細監視）
 * - batch_job_execution_context（実行コンテキスト分析）
 * - batch_step_execution_context（ステップコンテキスト分析）
 */
@Service
@RequiredArgsConstructor
public class BatchMonitoringService {
    
    // 手动添加logger
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BatchMonitoringService.class);

    private final JobExplorer jobExplorer;
    private final JobRepository jobRepository;
    private final BatchSettings batchSettings;
