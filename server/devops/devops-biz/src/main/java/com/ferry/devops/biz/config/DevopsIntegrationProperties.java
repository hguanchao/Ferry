package com.ferry.devops.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 交付域外部集成配置。
 *
 * <p>所有对外部系统（Jenkins / 代码平台 / 大模型 / 部署目标）的调用均通过这里配置；
 * 端点为空时对应实现走本地模拟，保证无外部环境时主流程仍可编译运行。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ferry.devops")
public class DevopsIntegrationProperties {

    /** Jenkins 集成 */
    private Jenkins jenkins = new Jenkins();

    /** 代码平台集成 */
    private Repo repo = new Repo();

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
        /** API 用户名 */
        private String user = "";
        /** API Token */
        private String token = "";
        /** 回调验签密钥（复用现有 webhook 配置，这里给出默认对接项） */
        private String callbackSecret = "";
    }

    @Data
    public static class Repo {
        /** 代码平台 API 基址（如 GitLab 的 http://gitlab/api/v4），为空时预校验只做格式校验 */
        private String apiBase = "";
        /** 平台访问令牌 */
        private String token = "";
        /** 私有化平台域名，用于仓库地址与平台匹配校验 */
        private String selfHostedDomain = "";
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
