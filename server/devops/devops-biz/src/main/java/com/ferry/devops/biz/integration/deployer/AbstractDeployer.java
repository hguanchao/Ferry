package com.ferry.devops.biz.integration.deployer;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.devops.biz.config.DevopsIntegrationProperties;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 部署执行器通用基类。
 *
 * <p>集中处理三件复用逻辑：
 * <ol>
 *   <li>{@link #enabled()} 按配置开关决定是否真实执行；</li>
 *   <li>{@link #runCommand(String...)} 用 {@code ProcessBuilder} 执行本地命令，错误流合并、带超时、
 *       失败收敛为 {@link ServiceException}；另提供 {@link #runCommandRaw(String...)} 供健康探测按退出码判定；</li>
 *   <li>{@link #parseTarget(String, String)} / {@link #parseHosts(String)} 解析环境目标资源 JSON，
 *       {@link #script(String)} 定位外部脚本。</li>
 * </ol>
 *
 * <p>不引入 jsch / docker-java / fabric8 等第三方依赖，真实能力全部委托给本机 shell/CLI。
 * 配置关闭时各子类保持原模拟日志与模拟成功结果，保证无外部环境仍可编译运行。
 */
@Slf4j
public abstract class AbstractDeployer implements Deployer {

    protected final DevopsIntegrationProperties props;

    protected AbstractDeployer(DevopsIntegrationProperties props) {
        this.props = props;
    }

    /** 是否真实执行部署命令 */
    protected boolean enabled() {
        return props != null && props.getDeploy() != null && props.getDeploy().isExecute();
    }

    /** 命令超时秒数（配置缺省时用 600 秒） */
    private int timeoutSeconds() {
        Integer t = props == null || props.getDeploy() == null ? null : props.getDeploy().getTimeoutSeconds();
        return t == null || t <= 0 ? 600 : t;
    }

    /**
     * 执行命令并返回合并后的 stdout/stderr；退出码非 0 或超时抛 {@link ServiceException}。
     */
    protected String runCommand(String... cmd) {
        return runCommand(Arrays.asList(cmd));
    }

    /** 列表形式，便于按条件拼参数（如可选的 --context） */
    protected String runCommand(List<String> cmd) {
        CommandResult r = doRun(cmd);
        if (r.exitCode != 0) {
            throw new ServiceException("部署命令执行失败(exit=" + r.exitCode + "): "
                    + commandText(cmd) + (r.output.isBlank() ? "" : " -> " + truncate(r.output)));
        }
        return r.output;
    }

    /**
     * 执行命令但不因非 0 退出码抛异常，供健康探测按退出码判定。仅超时/无法启动收敛为异常。
     */
    protected CommandResult runCommandRaw(String... cmd) {
        return runCommandRaw(Arrays.asList(cmd));
    }

    /** 列表形式，便于按条件拼参数 */
    protected CommandResult runCommandRaw(List<String> cmd) {
        return doRun(cmd);
    }

    /** 定位脚本：scriptDir/name，不存在抛清晰异常 */
    protected String script(String name) {
        String dir = props == null || props.getDeploy() == null
                ? "./deploy-scripts" : props.getDeploy().getScriptDir();
        if (dir == null || dir.isBlank()) {
            dir = "./deploy-scripts";
        }
        File file = new File(dir, name);
        if (!file.isFile()) {
            throw new ServiceException("部署脚本不存在: " + file.getPath()
                    + "（请配置 ferry.devops.deploy.script-dir 并放置 " + name + "）");
        }
        return file.getPath();
    }

    /**
     * 从 target_config JSON 中按 key 取文本值，如 host/port/user/namespace/cluster/image。
     */
    protected String parseTarget(String targetConfig, String key) {
        if (targetConfig == null || targetConfig.isBlank()) {
            return null;
        }
        JsonNode root = JsonUtil.readTree(targetConfig);
        if (root == null) {
            return null;
        }
        JsonNode node = root.path(key);
        return node == null || node.isMissingNode() ? null : node.asText();
    }

    /**
     * 从 target_config 解析主机清单（hosts 字段）。兼容字符串数组与对象数组（取其中 host 字段）。
     */
    protected List<String> parseHosts(String targetConfig) {
        List<String> hosts = new ArrayList<>();
        if (targetConfig == null || targetConfig.isBlank()) {
            return hosts;
        }
        JsonNode root = JsonUtil.readTree(targetConfig);
        if (root == null) {
            return hosts;
        }
        JsonNode arr = root.path("hosts");
        if (arr != null && arr.isArray()) {
            for (JsonNode item : arr) {
                String host = item.isTextual() ? item.asText()
                        : (item.path("host").isMissingNode() ? null : item.path("host").asText());
                if (host != null && !host.isBlank()) {
                    hosts.add(host);
                }
            }
        }
        if (hosts.isEmpty()) {
            String single = parseTarget(targetConfig, "host");
            if (single != null && !single.isBlank()) {
                hosts.add(single);
            }
        }
        return hosts;
    }

    /** 健康结论工厂 */
    protected HealthResult health(boolean pass, String detail) {
        return HealthResult.of(pass, detail);
    }

    /** 漂移结论工厂 */
    protected DriftStatus drift(boolean hasDrift, String detail) {
        return DriftStatus.of(hasDrift, detail);
    }

    /** 执行命令：错误流合并到输入流避免管道缓冲区写满导致死锁，带超时强制终止 */
    private CommandResult doRun(List<String> cmd) {
        if (cmd == null || cmd.isEmpty()) {
            throw new ServiceException("部署命令为空");
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        int timeout = timeoutSeconds();
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new ServiceException("部署命令无法启动: " + commandText(cmd) + " -> " + e.getMessage());
        }
        StringBuilder out = new StringBuilder();
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    out.append(line).append('\n');
                }
            } catch (IOException ignored) {
                // 进程被强制终止时流关闭属预期，忽略
            }
        }, "deploy-cmd-reader");
        reader.setDaemon(true);
        reader.start();
        try {
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                reader.join(2000);
                throw new ServiceException("部署命令执行超时(" + timeout + "s): " + commandText(cmd));
            }
            reader.join(2000);
            return new CommandResult(process.exitValue(), out.toString().trim());
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new ServiceException("部署命令执行被中断: " + commandText(cmd));
        }
    }

    private static String commandText(List<String> cmd) {
        return String.join(" ", cmd);
    }

    private static String truncate(String s) {
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }

    /** 命令执行原始结果：退出码 + 合并输出 */
    protected static class CommandResult {
        private final int exitCode;
        private final String output;

        protected CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }
    }

    /** 便于子类拼接批次实例清单快照（数组 JSON 或占位清单） */
    protected String targetsOf(DeployBatch batch) {
        String snap = batch == null ? null : batch.getTargetSnapshot();
        return snap == null || snap.isBlank() ? "[]" : snap;
    }
}
