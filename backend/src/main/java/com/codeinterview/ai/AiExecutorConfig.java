package com.codeinterview.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * AI 工作线程池（05 §3.4）：bean 名 aiExecutor，core4/max8/queue20，AbortPolicy（拒绝 → 42902），
 * 线程名 ai- 前缀、停机不等待任务完成。
 */
@Configuration
public class AiExecutorConfig {

    public static final String AI_EXECUTOR = "aiExecutor";

    @Bean(name = AI_EXECUTOR)
    public ThreadPoolTaskExecutor aiExecutor(AiProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getPool().getCoreSize());
        executor.setMaxPoolSize(props.getPool().getMaxSize());
        executor.setQueueCapacity(props.getPool().getQueueCapacity());
        executor.setThreadNamePrefix("ai-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
