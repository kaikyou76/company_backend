package com.example.companybackend.batch.config;

import com.example.companybackend.entity.User;
import com.example.companybackend.repository.UserRepository;
import com.example.companybackend.service.PaidLeaveCalculationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.Map;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class PaidLeaveUpdateBatchConfig {

    private static final Logger log = LoggerFactory.getLogger(PaidLeaveUpdateBatchConfig.class);
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final UserRepository userRepository;
    private final PaidLeaveCalculationService paidLeaveCalculationService;

    @Bean
    public Job paidLeaveUpdateJob() {
        return new JobBuilder("paidLeaveUpdateJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(updatePaidLeaveStep())
                .build();
    }

    @Bean
    public Step updatePaidLeaveStep() {
        return new StepBuilder("updatePaidLeaveStep", jobRepository)
                .<User, User>chunk(10, transactionManager)
                .reader(userReader())
                .processor(paidLeaveProcessor())
                .writer(paidLeaveWriter())
                .build();
    }

    @Bean
    public ItemReader<User> userReader() {
        return new RepositoryItemReaderBuilder<User>()
                .name("userReader")
                .repository(userRepository)
                .methodName("findAll")
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<User, User> paidLeaveProcessor() {
        return user -> {
            log.info("有給休暇日数更新処理: userId={}", user.getId());

            // 今年の有給休暇日数を計算
            int paidLeaveDays = paidLeaveCalculationService.calculatePaidLeaveDays(
                    user, LocalDate.now());

            // 実際のシステムでは、ここでユーザーの有給休暇日数を更新するフィールドに値を設定します
            // ただし、現在のデータベース構造にはそのようなフィールドがないため、
            // ログ出力のみ行います

            log.info("ユーザーID {} の今年度有給休暇日数: {}日", user.getId(), paidLeaveDays);
            return user;
        };
    }

    @Bean
    public ItemWriter<User> paidLeaveWriter() {
        return users -> {
            log.info("有給休暇日数更新結果書き込み: {}件", users.size());
            // 実際のシステムでは、ここでデータベースに更新結果を保存します
            // 現在のデータベース構造には有給休暇日数を保存するフィールドがないため、
            // ログ出力のみ行います
            users.forEach(user -> log.info("ユーザーID {} の有給休暇日数更新完了", user.getId()));
        };
    }
}