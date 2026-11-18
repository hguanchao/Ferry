package com.ferry.devops.biz.integration.deployer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SSH 部署执行器（最小实现）。
 *
 * <p>真实场景需 jsch 依赖完成逐台连通、指纹校验、备份、解包、渲染配置、启停与自检，
 * 本期不引入远程客户端依赖，仅打印各阶段日志并返回模拟成功结果，保持调度主流程完整。
 */
@Slf4j
@Component
public class SshDeployer implements Deployer {

    @Override
    public Integer deployType() {
        return 1;
    }

    @Override
    public void prepare(DeployContext context) {
        log.info("[SSH] prepare deployId={} envId={} artifactId={} target={}",
                context.getDeployId(), context.getEnvId(), context.getArtifactId(), context.getTargetConfig());
    }

    @Override
    public void deploy(DeployContext context, DeployBatch batch) {
        log.info("[SSH] deploy batchNo={} targetCount={} digest={}（模拟：摘流量→停服务→备份→解包→渲染→启动→自检→挂回）",
                batch.getBatchNo(), batch.getTargetCount(), context.getArtifactDigest());
    }

    @Override
    public HealthResult observe(DeployContext context, DeployBatch batch) {
        log.info("[SSH] observe batchNo={} -> 模拟健康通过", batch.getBatchNo());
        return HealthResult.ok();
    }

    @Override
    public void rollback(DeployContext context, RollbackTarget target) {
        log.info("[SSH] rollback envId={} targetArtifactId={} digest={}（模拟：按备份恢复）",
                target.getEnvId(), target.getArtifactId(), target.getArtifactDigest());
    }

    @Override
    public DriftStatus status(DeployContext context) {
        log.info("[SSH] status envId={}", context.getEnvId());
        return DriftStatus.none();
    }
}
