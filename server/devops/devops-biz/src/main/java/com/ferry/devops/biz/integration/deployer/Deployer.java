package com.ferry.devops.biz.integration.deployer;

import java.util.List;

/**
 * 部署执行器统一接口。
 *
 * <p>三个实现通过 DevopsProperties#getDeploy().isExecute() 控制：
 * 开启时用 {@code ProcessBuilder} 真实执行 shell/CLI 命令，关闭时保留原模拟日志与模拟成功结果。
 */
public interface Deployer {

    /** 部署类型 1SSH 2Docker 3Kubernetes */
    Integer deployType();

    /** 前置准备：可达性、凭据、资源校验 */
    void prepare(DeployContext context);

    /** 部署单个批次 */
    void deploy(DeployContext context, DeployBatch batch);

    /** 观察单个批次健康结论：1 通过 2 不通过 */
    HealthResult observe(DeployContext context, DeployBatch batch);

    /** 回滚到目标制品 */
    void rollback(DeployContext context, RollbackTarget target);

    /** 查询环境当前版本漂移状态 */
    DriftStatus status(DeployContext context);

    /** 部署上下文 */
    class DeployContext {
        private Long deployId;
        private Long appId;
        private Long envId;
        private Long artifactId;
        private String artifactDigest;
        private String targetConfig;

        public Long getDeployId() { return deployId; }
        public void setDeployId(Long deployId) { this.deployId = deployId; }
        public Long getAppId() { return appId; }
        public void setAppId(Long appId) { this.appId = appId; }
        public Long getEnvId() { return envId; }
        public void setEnvId(Long envId) { this.envId = envId; }
        public Long getArtifactId() { return artifactId; }
        public void setArtifactId(Long artifactId) { this.artifactId = artifactId; }
        public String getArtifactDigest() { return artifactDigest; }
        public void setArtifactDigest(String artifactDigest) { this.artifactDigest = artifactDigest; }
        public String getTargetConfig() { return targetConfig; }
        public void setTargetConfig(String targetConfig) { this.targetConfig = targetConfig; }
    }

    /** 批次快照 */
    class DeployBatch {
        private Long batchId;
        private Integer batchNo;
        private Integer targetCount;
        private String targetSnapshot;

        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }
        public Integer getBatchNo() { return batchNo; }
        public void setBatchNo(Integer batchNo) { this.batchNo = batchNo; }
        public Integer getTargetCount() { return targetCount; }
        public void setTargetCount(Integer targetCount) { this.targetCount = targetCount; }
        public String getTargetSnapshot() { return targetSnapshot; }
        public void setTargetSnapshot(String targetSnapshot) { this.targetSnapshot = targetSnapshot; }
    }

    /** 健康观察结果 */
    class HealthResult {
        private boolean pass;
        private String detail;

        public static HealthResult ok() {
            HealthResult r = new HealthResult();
            r.pass = true;
            r.detail = "模拟健康检查通过";
            return r;
        }

        /** 真实执行结果工厂：由执行器按探测结论填充 */
        public static HealthResult of(boolean pass, String detail) {
            HealthResult r = new HealthResult();
            r.pass = pass;
            r.detail = detail;
            return r;
        }

        public boolean isPass() { return pass; }
        public String getDetail() { return detail; }
    }

    /** 回滚目标 */
    class RollbackTarget {
        private Long envId;
        private Long artifactId;
        private String artifactDigest;

        public Long getEnvId() { return envId; }
        public void setEnvId(Long envId) { this.envId = envId; }
        public Long getArtifactId() { return artifactId; }
        public void setArtifactId(Long artifactId) { this.artifactId = artifactId; }
        public String getArtifactDigest() { return artifactDigest; }
        public void setArtifactDigest(String artifactDigest) { this.artifactDigest = artifactDigest; }
    }

    /** 漂移状态 */
    class DriftStatus {
        private boolean drift;
        private String detail;

        public static DriftStatus none() {
            DriftStatus s = new DriftStatus();
            s.drift = false;
            s.detail = "无漂移";
            return s;
        }

        /** 真实执行结果工厂：由执行器按镜像/版本比对结论填充 */
        public static DriftStatus of(boolean drift, String detail) {
            DriftStatus s = new DriftStatus();
            s.drift = drift;
            s.detail = detail;
            return s;
        }

        public boolean isDrift() { return drift; }
        public String getDetail() { return detail; }
    }
}
