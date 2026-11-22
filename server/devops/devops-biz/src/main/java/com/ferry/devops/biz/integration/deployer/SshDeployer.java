package com.ferry.devops.biz.integration.deployer;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.devops.biz.config.DevopsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SSH 部署执行器。
 *
 * <p>配置 {@code ferry.devops.deploy.execute=true} 时用 {@code ProcessBuilder} 调本机 ssh/脚本真实执行；
 * 未开启时保留原模拟日志与模拟成功结果（降级不变）。
 *
 * <p>脚本约定（位于 script-dir）：
 * <ul>
 *   <li>ssh-deploy.sh   参数：host artifactDigest targetSnapshotJson batchNo —— 摘流量/停服/备份/解包/渲染/启动/自检；</li>
 *   <li>ssh-health.sh   参数：host —— 健康自检，退出码 0 通过；</li>
 *   <li>ssh-rollback.sh 参数：host artifactDigest —— 按备份恢复；</li>
 *   <li>ssh-status.sh   参数：host —— 输出当前版本标识。</li>
 * </ul>
 */
@Slf4j
@Component
public class SshDeployer extends AbstractDeployer {

    public SshDeployer(DevopsProperties props) {
        super(props);
    }

    @Override
    public Integer deployType() {
        return 1;
    }

    @Override
    public void prepare(DeployContext context) {
        if (!enabled()) {
            log.info("[SSH] prepare deployId={} envId={} artifactId={} target={}（模拟：逐台探活+备份校验）",
                    context.getDeployId(), context.getEnvId(), context.getArtifactId(), context.getTargetConfig());
            return;
        }
        List<String> hosts = parseHosts(context.getTargetConfig());
        if (hosts.isEmpty()) {
            throw new ServiceException("[SSH] 目标配置缺少 hosts 主机清单，无法探活");
        }
        String user = parseTarget(context.getTargetConfig(), "user");
        int port = parsePort(context.getTargetConfig());
        for (String host : hosts) {
            String target = (user == null || user.isBlank() ? "" : user + "@") + host;
            // 备份/变更前逐台探活：任一失联立即中止，不进入停服阶段（对应备份失败立即中止的严谨度）
            log.info("[SSH] prepare 探活 host={}", host);
            runCommand("ssh", "-o", "BatchMode=yes", "-o", "StrictHostKeyChecking=accept-new",
                    "-o", "ConnectTimeout=5", "-p", String.valueOf(port), target, "true");
        }
    }

    @Override
    public void deploy(DeployContext context, DeployBatch batch) {
        if (!enabled()) {
            log.info("[SSH] deploy batchNo={} targetCount={} digest={}（模拟：摘流量→停服务→备份→解包→渲染→启动→自检→挂回）",
                    batch.getBatchNo(), batch.getTargetCount(), context.getArtifactDigest());
            return;
        }
        List<String> hosts = parseHosts(context.getTargetConfig());
        if (hosts.isEmpty()) {
            throw new ServiceException("[SSH] 目标配置缺少 hosts 主机清单，无法部署");
        }
        String deployScript = script("ssh-deploy.sh");
        String digest = nullToEmpty(context.getArtifactDigest());
        for (String host : hosts) {
            log.info("[SSH] deploy batchNo={} host={} digest={}", batch.getBatchNo(), host, digest);
            runCommand(deployScript, host, digest, targetsOf(batch), String.valueOf(batch.getBatchNo()));
        }
    }

    @Override
    public HealthResult observe(DeployContext context, DeployBatch batch) {
        if (!enabled()) {
            log.info("[SSH] observe batchNo={} -> 模拟健康通过", batch.getBatchNo());
            return HealthResult.ok();
        }
        List<String> hosts = parseHosts(context.getTargetConfig());
        String healthScript = script("ssh-health.sh");
        StringBuilder detail = new StringBuilder();
        boolean pass = true;
        for (String host : hosts) {
            CommandResult r = runCommandRaw(healthScript, host);
            boolean hostOk = r.getExitCode() == 0;
            pass = pass && hostOk;
            detail.append(host).append(hostOk ? ":OK" : ":FAIL").append(' ');
        }
        if (hosts.isEmpty()) {
            return health(false, "[SSH] 目标配置无主机，健康检查无数据按不通过");
        }
        log.info("[SSH] observe batchNo={} pass={} detail={}", batch.getBatchNo(), pass, detail);
        return health(pass, "[SSH] " + detail.toString().trim());
    }

    @Override
    public void rollback(DeployContext context, RollbackTarget target) {
        if (!enabled()) {
            log.info("[SSH] rollback envId={} targetArtifactId={} digest={}（模拟：按备份恢复）",
                    target.getEnvId(), target.getArtifactId(), target.getArtifactDigest());
            return;
        }
        List<String> hosts = parseHosts(context.getTargetConfig());
        String rollbackScript = script("ssh-rollback.sh");
        String digest = nullToEmpty(target.getArtifactDigest());
        for (String host : hosts) {
            log.info("[SSH] rollback host={} digest={}", host, digest);
            runCommand(rollbackScript, host, digest);
        }
    }

    @Override
    public DriftStatus status(DeployContext context) {
        if (!enabled()) {
            log.info("[SSH] status envId={} -> 模拟无漂移", context.getEnvId());
            return DriftStatus.none();
        }
        List<String> hosts = parseHosts(context.getTargetConfig());
        String statusScript = script("ssh-status.sh");
        String expect = nullToEmpty(context.getArtifactDigest());
        StringBuilder detail = new StringBuilder();
        boolean drift = false;
        for (String host : hosts) {
            String version = runCommand(statusScript, host).trim();
            boolean match = version.equals(expect);
            drift = drift || !match;
            detail.append(host).append('=').append(version).append(' ');
        }
        String d = detail.toString().trim();
        log.info("[SSH] status envId={} drift={} detail={}", context.getEnvId(), drift, d);
        return drift(drift, "[SSH] " + (drift ? "版本不一致: " : "版本一致: ") + d);
    }

    private int parsePort(String targetConfig) {
        String p = parseTarget(targetConfig, "port");
        if (p == null || p.isBlank()) {
            return 22;
        }
        try {
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            throw new ServiceException("[SSH] target_config.port 非法: " + p);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
