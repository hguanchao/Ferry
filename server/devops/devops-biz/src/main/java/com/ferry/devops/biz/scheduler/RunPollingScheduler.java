package com.ferry.devops.biz.scheduler;

import com.ferry.devops.biz.service.IRunPollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Jenkins 构建状态轮询任务：状态同步的唯一通道。
 *
 * <p>按运行状态分频：
 *
 * <ul>
 *   <li>排队中/运行中：每 5 秒，用 wfapi 逐阶段同步状态与运行终态（Jenkins wfapi 在运行中即返回 stage 状态，
 *       高频轮询可实现阶段级准实时推进；状态更新幂等）</li>
 *   <li>结果待确认：每 300 秒，等待触发结果收敛，无高频变化需求</li>
 * </ul>
 *
 * <p>多实例部署时各实例都会轮询，状态更新是幂等的；如需选主可追加分布式锁。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RunPollingScheduler {

    private final IRunPollService runPollService;
    /** 两个分频任务共用一把互斥标记：同一时刻最多一个轮询线程访问 Jenkins */
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void pollActiveRuns() {
        runGuarded(runPollService::pollActiveRuns);
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 15000)
    public void pollUnknownRuns() {
        runGuarded(runPollService::pollUnknownRuns);
    }

    /**
     * 串行执行轮询：上一轮未结束时跳过本轮，避免并发访问 Jenkins。
     *
     * @param task 轮询动作
     */
    private void runGuarded(Runnable task) {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            task.run();
        } catch (RuntimeException e) {
            log.error("run polling failed", e);
        } finally {
            running.set(false);
        }
    }
}
