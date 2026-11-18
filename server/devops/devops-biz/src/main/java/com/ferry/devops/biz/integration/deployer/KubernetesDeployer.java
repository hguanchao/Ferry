package com.ferry.devops.biz.integration.deployer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Kubernetes 部署执行器（最小实现）。
 *
 * <p>真实场景需 fabric8 依赖完成凭据鉴权、按摘要更新工作负载、rollout 监听与 revision 回滚，
 * 本期不引入远程客户端依赖，仅打印各阶段日志并返回模拟成功结果。
 */
@Slf4j
@Component
public class KubernetesDeployer implements Deployer {

    @Override
    public Integer deployType() {
        return 3;
    }

    @Override
    public void prepare(DeployContext context) {
        log.info("[K8s] prepare deployId={} envId={} digest={} target={}",
                context.getDeployId(), context.getEnvId(), context.getArtifactDigest(), context.getTargetConfig());
    }

    @Override
    public void deploy(DeployContext context, DeployBatch batch) {
        log.info("[K8s] deploy batchNo={} targetCount={} digest={}（模拟：按摘要更新工作负载→监听 rollout→按批次调副本/权重）",
                batch.getBatchNo(), batch.getTargetCount(), context.getArtifactDigest());
    }

    @Override
    public HealthResult observe(DeployContext context, DeployBatch batch) {
        log.info("[K8s] observe batchNo={} -> 模拟健康通过", batch.getBatchNo());
        return HealthResult.ok();
    }

    @Override
    public void rollback(DeployContext context, RollbackTarget target) {
        log.info("[K8s] rollback envId={} targetArtifactId={} digest={}（模拟：回上一 revision，保留现场副本）",
                target.getEnvId(), target.getArtifactId(), target.getArtifactDigest());
    }

    @Override
    public DriftStatus status(DeployContext context) {
        log.info("[K8s] status envId={}", context.getEnvId());
        return DriftStatus.none();
    }
}
