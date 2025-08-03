package com.example.orgchart_api.config;

import com.example.orgchart_api.batch.util.BatchSettings;
import com.example.orgchart_api.batch.util.ErrorFileManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Properties;

@Configuration
public class BatchConfig {

    private final Environment env;

    public BatchConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public BatchSettings batchSettings() {
        return new BatchSettings(collectAllProperties());
    }

    @Bean
    @Qualifier("batchProperties")
    public Properties applicationProperties() {
        return collectAllProperties();
    }

    @Bean
    public ErrorFileManager errorFileManager(BatchSettings batchSettings) {
        String errorDir = batchSettings.getOutPutErrFileDir(); // プロパティから取得
        String errorPrefix = batchSettings.getOutPutErrFileNm(); // プロパティから取得
        return new ErrorFileManager(errorDir, errorPrefix);
    }

    // ★ 追加：非同期バッチ処理用のTaskExecutor Bean
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);       // 最小スレッド数
        executor.setMaxPoolSize(10);       // 最大スレッド数
        executor.setQueueCapacity(25);     // 待機キューの長さ
        executor.setThreadNamePrefix("batch-task-");
        executor.initialize();
        return executor;
    }

    private Properties collectAllProperties() {
        Properties props = new Properties();
        MutablePropertySources propertySources = ((AbstractEnvironment) env).getPropertySources();

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String osSuffix = isWindows ? ".Win32" : ".Linux";

        propertySources.stream()
                .filter(ps -> ps instanceof org.springframework.core.env.EnumerablePropertySource)
                .forEach(ps -> {
                    org.springframework.core.env.EnumerablePropertySource<?> enumerablePs =
                            (org.springframework.core.env.EnumerablePropertySource<?>) ps;

                    for (String propName : enumerablePs.getPropertyNames()) {
                        Object value = enumerablePs.getProperty(propName);
                        if (value != null) {
                            props.setProperty(propName, value.toString());
                        }
                    }
                });

        return props;
    }
}
