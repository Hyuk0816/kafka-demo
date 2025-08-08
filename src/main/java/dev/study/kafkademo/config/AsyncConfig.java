package dev.study.kafkademo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 * Event-Driven Architecture의 비동기 이벤트 처리를 위한 설정
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 이벤트 처리용 비동기 실행자 설정
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);              // 최소 스레드 수
        executor.setMaxPoolSize(10);              // 최대 스레드 수
        executor.setQueueCapacity(50);            // 큐 용량
        executor.setThreadNamePrefix("async-event-");
        executor.setKeepAliveSeconds(60);         // 유휴 스레드 유지 시간
        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.warn("비동기 작업 거부됨: 큐가 가득참. 현재 활성 스레드: {}, 큐 크기: {}", 
                    threadPoolExecutor.getActiveCount(), 
                    threadPoolExecutor.getQueue().size());
        });
        executor.initialize();
        return executor;
    }
}