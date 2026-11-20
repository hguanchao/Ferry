package com.ferry.devops.biz.integration.jenkins;

/**
 * Jenkins 下发异常。
 *
 * <p>{@code uncertain=true} 表示请求可能已经到达 Jenkins（超时、连接中断、5xx），
 * 运行应进入「结果待确认」；{@code false} 表示明确失败（鉴权失败、Job 创建失败、4xx），
 * 运行应直接置失败。
 */
public class JenkinsDispatchException extends RuntimeException {

    private final boolean uncertain;

    public JenkinsDispatchException(String message, boolean uncertain) {
        super(message);
        this.uncertain = uncertain;
    }

    public JenkinsDispatchException(String message, boolean uncertain, Throwable cause) {
        super(message, cause);
        this.uncertain = uncertain;
    }

    public boolean isUncertain() {
        return uncertain;
    }
}
