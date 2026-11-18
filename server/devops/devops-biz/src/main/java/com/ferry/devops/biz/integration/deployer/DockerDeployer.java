package com.ferry.devops.biz.integration.deployer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Docker 部署执行器（最小实现）。
 *
 * <p>真实场景需 docker-java 依赖完成守护进程探测、按摘要拉取、旧容器参数留存与重建，
 * 本期不引入远程客户端依赖，仅打印各阶段日志并返回模拟成功结果。
 */
@Slf4j
@Component
public class DockerDeployer implements Deployer {

    @Override
    public Integer deployType() {
        return 2;
    }

    @Override
    public void prepare(DeployContext context) {
        log.info("[Docker] prepare deployId={} envId={} digest={} target={}",
                context.getDeployId(), context.getEnvId(), context.getArtifactDigest(), context.getTargetConfig());
    }

    @Override
    public void deploy(DeployContext context, DeployBatch batch) {
        log.info("[Docker] deploy batchNo={} targetCount={} digest={}（模拟：按摘要拉取→停删旧容器→原参数启新→端口探测）",
                batch.getBatchNo(), batch.getTargetCount(), context.getArtifactDigest());
    }

    @Override
    public HealthResult observe(DeployContext context, DeployBatch batch) {
        log.info("[Docker] observe batchNo={} -> 模拟健康通过", batch.getBatchNo());
        return HealthResult.ok();
    }

    @Override
    public void rollback(DeployContext context, RollbackTarget target) {
        log.info("[Docker] rollback envId={} targetArtifactId={} digest={}（模拟：原参数+旧摘要重建）",
                target.getEnvId(), target.getArtifactId(), target.getArtifactDigest());
    }

    @Override
    public DriftStatus status(DeployContext context) {
        log.info("[Docker] status envId={}", context.getEnvId());
        return DriftStatus.none();
    }
}
