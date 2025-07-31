package com.example.companybackend.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
// 添加必要的导入
import org.springframework.batch.core.job.JobKeyGenerator;
import org.springframework.batch.core.job.DefaultJobKeyGenerator;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("batch-task-");
        executor.initialize();
        return executor;
    }

    @Bean
    public DataFieldMaxValueIncrementerFactory incrementerFactory(DataSource dataSource) {
        return new DefaultDataFieldMaxValueIncrementerFactory(dataSource);
    }

    // 添加JobKeyGenerator Bean
    @Bean
    public JobKeyGenerator jobKeyGenerator() {
        return new DefaultJobKeyGenerator();
    }

    @Bean
    public JobRepository jobRepository(DataSource dataSource, PlatformTransactionManager transactionManager,
                                      DataFieldMaxValueIncrementerFactory incrementerFactory) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setIsolationLevelForCreate("ISOLATION_DEFAULT");
        factory.setTablePrefix("BATCH_");
        factory.setIncrementerFactory(incrementerFactory);
        factory.setDatabaseType("POSTGRES"); // 明确指定数据库类型为POSTGRES
        // 设置JobKeyGenerator
        factory.setJobKeyGenerator(jobKeyGenerator());
        return factory.getObject();
    }
}