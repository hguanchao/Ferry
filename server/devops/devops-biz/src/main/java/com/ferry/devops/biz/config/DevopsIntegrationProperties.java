package com.ferry.devops.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 交付域外部集成配置。
 *
 * <p>所有对外部系统（Jenkins / 大模型 / 部署目标）的调用均通过这里配置；
 * 端点为空时对应实现走本地模拟，保证无外部环境时主流程仍可编译运行。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ferry.devops")
public class DevopsIntegrationProperties {

    /** Jenkins 集成 */
    private Jenkins jenkins = new Jenkins();

    /** 大模型集成 */
    private Ai ai = new Ai();

    /** 部署执行器集成 */
    private Deploy deploy = new Deploy();

    @Data
    public static class Jenkins {
        /** Jenkins 服务基址，如 http://jenkins:8080；为空时下发走模拟 */
        private String baseUrl = "";
        /** 触发 Job 的 API 路径模板，{job} 占位；默认 Jenkins 标准 buildWithParameters */
        private String buildPath = "/job/{job}/buildWithParameters";
        /** Job 不存在时是否按模板自动创建；关闭后缺失 Job 直接判失败 */
        private boolean autoCreateJob = true;
        /** 签出代码使用的 Jenkins 凭据 ID；为空时按公开仓库访问 */
        private String gitCredentialsId = "";
        /** API 用户名 */
        private String user = "";
        /** API Token */
        private String token = "";
        /** 回调验签密钥（复用现有 webhook 配置，这里给出默认对接项） */
        private String callbackSecret = "";
        /**
         * Ferry 状态回调完整地址（须含接口路径），Jenkins 容器必须能访问到，
         * 如 http://172.21.0.1:9100/ferry-devops/callback/jenkins/status（WSL 原生 docker 用 Windows 侧 vEthernet 网关 IP）；
         * 为空时构建参数 CALLBACK_URL 为空，生成的脚本自动跳过回调，状态同步退化为纯轮询兜底
         */
        private String callbackUrl = "";
    }

    @Data
    public static class Ai {
        /** OpenAI 兼容端点（含 /v1），如 http://llm:8080/v1 或 https://api.openai.com/v1 */
        private String baseUrl = "";
        /** 模型名 */
        private String model = "gpt-4o-mini";
        /** API Key */
        private String apiKey = "";
        /** 超时秒数 */
        private Integer timeoutSeconds = 30;
    }

    @Data
    public static class Deploy {
        /** 是否真实执行部署命令；false 时执行器只记日志返回模拟成功 */
        private boolean execute = false;
        /** 部署脚本目录，真实执行时从此目录按类型取脚本 */
        private String scriptDir = "./deploy-scripts";
        /** 命令超时秒数 */
        private Integer timeoutSeconds = 600;
    }
}
