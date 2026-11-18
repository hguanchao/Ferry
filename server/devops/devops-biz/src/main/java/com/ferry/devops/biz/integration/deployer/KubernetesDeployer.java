package com.ferry.devops.biz.integration.deployer;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.devops.biz.config.DevopsIntegrationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Kubernetes 部署执行器。
 *
 * <p>配置 {@code ferry.devops.deploy.execute=true} 时用 {@code ProcessBuilder} 调 kubectl 真实执行；
 * 未开启时保留原模拟日志与模拟成功结果（降级不变）。
 *
 * <p>target_config 字段约定：namespace、cluster（context）、deployment（工作负载名）、container（容器名）、image（镜像名）。
 * <ul>
 *   <li>deploy：kubectl set image 按摘要更新 → 按批次 scale 副本；</li>
 *   <li>observe：kubectl rollout status 监听至就绪或超时，失败返回 pass=false；</li>
 *   <li>rollback：kubectl rollout undo 回上一 revision；</li>
 *   <li>status：kubectl get deployment 解析当前镜像摘要与期望比对。</li>
 * </ul>
 */
@Slf4j
@Component
public class KubernetesDeployer extends AbstractDeployer {

    public KubernetesDeployer(DevopsIntegrationProperties props) {
        super(props);
    }

    @Override
    public Integer deployType() {
        return 3;
    }

    @Override
    public void prepare(DeployContext context) {
        if (!enabled()) {
            log.info("[K8s] prepare deployId={} envId={} digest={} target={}（模拟：凭据/命名空间校验）",
                    context.getDeployId(), context.getEnvId(), context.getArtifactDigest(), context.getTargetConfig());
            return;
        }
        String ns = requireNamespace(context.getTargetConfig());
        String deployment = deployment(context.getTargetConfig());
        // kubectl 客户端可用性探测
        runCommand("kubectl", "version", "--client");
        // 命名空间/工作负载存在性与权限校验
        runCommand(kubectl(context.getTargetConfig(), "get", "deployment", deployment, "-n", ns));
    }

    @Override
    public void deploy(DeployContext context, DeployBatch batch) {
        if (!enabled()) {
            log.info("[K8s] deploy batchNo={} targetCount={} digest={}（模拟：按摘要更新工作负载→监听 rollout→按批次调副本/权重）",
                    batch.getBatchNo(), batch.getTargetCount(), context.getArtifactDigest());
            return;
        }
        String ns = requireNamespace(context.getTargetConfig());
        String deployment = deployment(context.getTargetConfig());
        String container = container(context.getTargetConfig());
        String image = parseTarget(context.getTargetConfig(), "image");
        String digest = context.getArtifactDigest();
        if (digest == null || digest.isBlank()) {
            throw new ServiceException("[K8s] 制品摘要为空，无法按摘要更新工作负载");
        }
        String imageRef = (image == null || image.isBlank())
                ? digest : (image.contains("@") ? image : image + "@" + digest);
        // 按摘要更新镜像
        runCommand(kubectl(context.getTargetConfig(), "set", "image", "deployment/" + deployment,
                container + "=" + imageRef, "-n", ns));
        // 按批次调整副本数（灰度批次影响实例数）
        Integer replicas = batch == null ? null : batch.getTargetCount();
        if (replicas != null && replicas > 0) {
            runCommand(kubectl(context.getTargetConfig(), "scale", "deployment/" + deployment,
                    "--replicas=" + replicas, "-n", ns));
        }
        log.info("[K8s] deploy batchNo={} deployment={} image={} done", batch.getBatchNo(), deployment, imageRef);
    }

    @Override
    public HealthResult observe(DeployContext context, DeployBatch batch) {
        if (!enabled()) {
            log.info("[K8s] observe batchNo={} -> 模拟健康通过", batch.getBatchNo());
            return HealthResult.ok();
        }
        String ns = requireNamespace(context.getTargetConfig());
        String deployment = deployment(context.getTargetConfig());
        int timeout = props.getDeploy().getTimeoutSeconds() == null ? 600 : props.getDeploy().getTimeoutSeconds();
        // rollout status 退出码非 0 即判不通过（超时/进度停滞），按业务降级不中断主流程
        CommandResult r = runCommandRaw(kubectl(context.getTargetConfig(), "rollout", "status",
                "deployment/" + deployment, "-n", ns, "--timeout=" + timeout + "s"));
        boolean pass = r.getExitCode() == 0;
        log.info("[K8s] observe batchNo={} pass={} detail={}", batch.getBatchNo(), pass, r.getOutput());
        return health(pass, "[K8s] rollout " + deployment + " -> " + (pass ? "就绪" : "失败/超时"));
    }

    @Override
    public void rollback(DeployContext context, RollbackTarget target) {
        if (!enabled()) {
            log.info("[K8s] rollback envId={} targetArtifactId={} digest={}（模拟：回上一 revision，保留现场副本）",
                    target.getEnvId(), target.getArtifactId(), target.getArtifactDigest());
            return;
        }
        String ns = requireNamespace(context.getTargetConfig());
        String deployment = deployment(context.getTargetConfig());
        log.info("[K8s] rollback envId={} deployment={} -> 上一 revision", target.getEnvId(), deployment);
        runCommand(kubectl(context.getTargetConfig(), "rollout", "undo", "deployment/" + deployment, "-n", ns));
    }

    @Override
    public DriftStatus status(DeployContext context) {
        if (!enabled()) {
            log.info("[K8s] status envId={} -> 模拟无漂移", context.getEnvId());
            return DriftStatus.none();
        }
        String ns = requireNamespace(context.getTargetConfig());
        String deployment = deployment(context.getTargetConfig());
        String container = container(context.getTargetConfig());
        String expect = context.getArtifactDigest() == null ? "" : context.getArtifactDigest();
        String jsonpath = "{.spec.template.spec.containers[?(@.name==\"" + container + "\")].image}";
        String current = runCommand(kubectl(context.getTargetConfig(), "get", "deployment", deployment,
                "-n", ns, "-o", "jsonpath=" + jsonpath)).trim();
        boolean match = !expect.isBlank() && current.contains(expect);
        log.info("[K8s] status envId={} drift={} current={} expect={}", context.getEnvId(), !match, current, expect);
        return drift(!match, "[K8s] 工作负载镜像=" + current + " 期望摘要=" + expect);
    }

    /** 拼 kubectl 命令：配置 cluster 时追加 --context，否则不追加多余空参数 */
    private List<String> kubectl(String targetConfig, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("kubectl");
        cmd.addAll(Arrays.asList(args));
        String cluster = parseTarget(targetConfig, "cluster");
        if (cluster != null && !cluster.isBlank()) {
            cmd.add("--context=" + cluster);
        }
        return cmd;
    }

    private String requireNamespace(String targetConfig) {
        String ns = namespace(targetConfig);
        if (ns == null || ns.isBlank()) {
            throw new ServiceException("[K8s] target_config.namespace 未配置，无法执行部署");
        }
        return ns;
    }

    private String namespace(String targetConfig) {
        return parseTarget(targetConfig, "namespace");
    }

    private String deployment(String targetConfig) {
        String d = parseTarget(targetConfig, "deployment");
        if (d == null || d.isBlank()) {
            throw new ServiceException("[K8s] target_config.deployment 未配置，无法执行部署");
        }
        return d;
    }

    private String container(String targetConfig) {
        String c = parseTarget(targetConfig, "container");
        return c == null || c.isBlank() ? "app" : c;
    }
}
