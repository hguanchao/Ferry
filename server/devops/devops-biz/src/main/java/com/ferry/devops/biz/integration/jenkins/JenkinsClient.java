package com.ferry.devops.biz.integration.jenkins;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Jenkins 客户端（最小实现：仅记录下发日志，不发起真实 HTTP 请求）。
 *
 * <p>真实环境接入时替换 {@link JenkinsClientImpl} 即可，事务提交后再调用，避免外部超时回滚运行记录。
 */
public interface JenkinsClient {

    /**
     * 下发 CI 阶段 Job
     *
     * @param runId    运行id
     * @param stageId  阶段id
     * @param jobId    Job 标识
     * @param snapshot 版本快照 JSON
     */
    void dispatchStage(Long runId, Long stageId, String jobId, String snapshot);

    /**
     * 中止构建
     *
     * @param runId   运行id
     * @param buildNo 构建号
     */
    void abortBuild(Long runId, Long buildNo);
}

/**
 * Jenkins 客户端最小实现：只记日志，不真实发 HTTP（无 Jenkins 环境）。
 */
@Slf4j
@Component
class JenkinsClientImpl implements JenkinsClient {

    @Override
    public void dispatchStage(Long runId, Long stageId, String jobId, String snapshot) {
        log.info("Jenkins dispatch (mock): runId={}, stageId={}, jobId={}, snapshotLength={}",
                runId, stageId, jobId, snapshot == null ? 0 : snapshot.length());
    }

    @Override
    public void abortBuild(Long runId, Long buildNo) {
        log.info("Jenkins abort (mock): runId={}, buildNo={}", runId, buildNo);
    }
}
