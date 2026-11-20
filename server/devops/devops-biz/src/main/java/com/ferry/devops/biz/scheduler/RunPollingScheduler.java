package com.ferry.devops.biz.scheduler;

import com.ferry.devops.biz.service.IRunPollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Jenkins 构建状态轮询任务：每 5 秒同步一次活动运行。
 *
 * <p>多实例部署时各实例都会轮询，状态更新是幂等的；如需选主可追加分布式锁。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RunPollingScheduler {

    private final IRunPollService runPollService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void poll() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            runPollService.pollActiveRuns();
        } catch (RuntimeException e) {
            log.error("run polling failed", e);
        } finally {
            running.set(false);
        }
    }
}
