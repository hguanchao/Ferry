package com.ferry.devops.biz.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ferry.devops.biz.service.IGateExemptionService;
import com.ferry.devops.biz.service.impl.RunStageGateEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 阶段门禁调度：拉取待门禁阶段的报告并裁决，同时处理已到期豁免。
 *
 * <p>评估按唯一键 upsert，多实例重复执行是幂等的；这里用进程内标记避免单实例重入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GateEvaluationScheduler {

    private final RunStageGateEvaluator runStageGateEvaluator;
    private final IGateExemptionService gateExemptionService;
    private final AtomicBoolean evaluating = new AtomicBoolean(false);
    private final AtomicBoolean expiring = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 15000, initialDelay = 20000)
    public void evaluatePendingGates() {
        if (!evaluating.compareAndSet(false, true)) {
            return;
        }
        try {
            runStageGateEvaluator.evaluatePending();
        } catch (RuntimeException e) {
            log.error("stage gate evaluation failed", e);
        } finally {
            evaluating.set(false);
        }
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void expireExemptions() {
        if (!expiring.compareAndSet(false, true)) {
            return;
        }
        try {
            gateExemptionService.expireExemptions();
        } catch (RuntimeException e) {
            log.error("gate exemption expiry failed", e);
        } finally {
            expiring.set(false);
        }
    }
}
