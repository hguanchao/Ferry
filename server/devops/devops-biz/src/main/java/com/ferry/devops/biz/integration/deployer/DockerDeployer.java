package com.ferry.devops.biz.integration.deployer;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.devops.biz.config.DevopsIntegrationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Docker 部署执行器。
 *
 * <p>配置 {@code ferry.devops.deploy.execute=true} 时用 {@code ProcessBuilder} 调 docker CLI 真实执行；
 * 未开启时保留原模拟日志与模拟成功结果（降级不变）。
 *
 * <p>关键约束：<b>禁止 latest 标签</b> —— 镜像名或制品摘要含 latest 一律在 prepare/deploy/rollback 拒绝，
 * 只按摘要（digest）拉取与启动。target_config 字段约定：image（镜像名）、host（可选宿主机）、container（容器名）。
 */
@Slf4j
@Component
public class DockerDeployer extends AbstractDeployer {

    public DockerDeployer(DevopsIntegrationProperties props) {
        super(props);
    }

    @Override
    public Integer deployType() {
        return 2;
    }

    @Override
    public void prepare(DeployContext context) {
        if (!enabled()) {
            log.info("[Docker] prepare deployId={} envId={} digest={} target={}（模拟：守护进程/摘要校验）",
                    context.getDeployId(), context.getEnvId(), context.getArtifactDigest(), context.getTargetConfig());
            return;
        }
        rejectLatest(context.getArtifactDigest(), "artifactDigest");
        String image = imageOf(context.getTargetConfig());
        rejectLatest(image, "image");
        // 守护进程可用性探测 + 摘要可拉取校验（dry pull 由 CLI 完成）
        runCommand("docker", "version", "--format", "{{.Server.Version}}");
    }

    @Override
    public void deploy(DeployContext context, DeployBatch batch) {
        if (!enabled()) {
            log.info("[Docker] deploy batchNo={} targetCount={} digest={}（模拟：按摘要拉取→停删旧容器→原参数启新→端口探测）",
                    batch.getBatchNo(), batch.getTargetCount(), context.getArtifactDigest());
            return;
        }
        String digest = rejectLatest(context.getArtifactDigest(), "artifactDigest");
        String image = imageOf(context.getTargetConfig());
        rejectLatest(image, "image");
        if (image == null || image.isBlank()) {
            throw new ServiceException("[Docker] target_config.image 未配置，无法按摘要部署");
        }
        String imageRef = image.contains("@") ? image : image + "@" + digest;
        // 按摘要拉取，按批次启停：由 docker-deploy.sh 封装 stop/rm/run，参数含实例数与清单快照
        String deployScript = script("docker-deploy.sh");
        log.info("[Docker] deploy batchNo={} image={} instances={}", batch.getBatchNo(), imageRef, batch.getTargetCount());
        runCommand(deployScript, imageRef, String.valueOf(batch.getTargetCount()),
                targetsOf(batch), String.valueOf(batch.getBatchNo()));
    }

    @Override
    public HealthResult observe(DeployContext context, DeployBatch batch) {
        if (!enabled()) {
            log.info("[Docker] observe batchNo={} -> 模拟健康通过", batch.getBatchNo());
            return HealthResult.ok();
        }
        String container = parseTarget(context.getTargetConfig(), "container");
        if (container == null || container.isBlank()) {
            return health(false, "[Docker] target_config.container 未配置，健康检查无数据按不通过");
        }
        // docker inspect 退出码 0 且 Running=true 判通过；失败按不通过返回（不抛异常中断主流程）
        CommandResult r = runCommandRaw("docker", "inspect", "--format", "{{.State.Running}}", container);
        boolean pass = r.getExitCode() == 0 && Boolean.parseBoolean(r.getOutput().trim());
        log.info("[Docker] observe batchNo={} pass={} detail={}", batch.getBatchNo(), pass, r.getOutput());
        return health(pass, "[Docker] inspect " + container + " -> " + (pass ? "运行中" : "未运行或不存在"));
    }

    @Override
    public void rollback(DeployContext context, RollbackTarget target) {
        if (!enabled()) {
            log.info("[Docker] rollback envId={} targetArtifactId={} digest={}（模拟：原参数+旧摘要重建）",
                    target.getEnvId(), target.getArtifactId(), target.getArtifactDigest());
            return;
        }
        String digest = rejectLatest(target.getArtifactDigest(), "artifactDigest");
        String image = imageOf(context.getTargetConfig());
        rejectLatest(image, "image");
        if (image == null || image.isBlank()) {
            throw new ServiceException("[Docker] target_config.image 未配置，无法回滚");
        }
        String imageRef = image.contains("@") ? image : image + "@" + digest;
        String rollbackScript = script("docker-rollback.sh");
        log.info("[Docker] rollback envId={} image={}", target.getEnvId(), imageRef);
        runCommand(rollbackScript, imageRef, nullToEmpty(target.getArtifactDigest()));
    }

    @Override
    public DriftStatus status(DeployContext context) {
        if (!enabled()) {
            log.info("[Docker] status envId={} -> 模拟无漂移", context.getEnvId());
            return DriftStatus.none();
        }
        String container = parseTarget(context.getTargetConfig(), "container");
        String expect = nullToEmpty(context.getArtifactDigest());
        if (container == null || container.isBlank()) {
            return drift(true, "[Docker] target_config.container 未配置，无法比对镜像摘要");
        }
        // 当前运行镜像摘要
        String current = runCommand("docker", "ps", "--filter", "name=" + container,
                "--format", "{{.Image}}").trim();
        boolean match = current.contains(expect) && !expect.isBlank();
        log.info("[Docker] status envId={} drift={} current={} expect={}", context.getEnvId(), !match, current, expect);
        return drift(!match, "[Docker] 运行镜像=" + current + " 期望摘要=" + expect);
    }

    private String imageOf(String targetConfig) {
        return parseTarget(targetConfig, "image");
    }

    /** 禁止 latest：镜像名或摘要任一含 latest 即抛异常，其余原样返回 */
    private String rejectLatest(String value, String field) {
        if (value != null && value.toLowerCase().contains("latest")) {
            throw new ServiceException("[Docker] 禁止使用 latest 标签（" + field + "=" + value + "），请改用摘要部署");
        }
        return value;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
