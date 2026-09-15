package com.safetylaw.monitor.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("safety-law-sched-");
        // 정기 작업은 한국 시각 기준으로 돈다. 서버 시간대가 UTC 로 맞춰진
        // 환경에서도 "새벽 1시"가 한국 시각 새벽 1시가 되도록 명시한다.
        scheduler.setAwaitTerminationSeconds(20);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * 전체 법령 본문 캐시 전용 실행기.
     *
     * <p>수천 건을 상세 조회하는 긴 작업이라 화면 요청을 붙잡지 않도록
     * 따로 돌린다. 스레드가 하나라서 동시에 두 번 돌지 않는다.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService fullCacheExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "safety-law-full-cache");
            thread.setDaemon(true);
            return thread;
        });
    }
}
