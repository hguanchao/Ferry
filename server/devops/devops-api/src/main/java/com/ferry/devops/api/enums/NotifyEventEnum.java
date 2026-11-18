package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 通知事件
 */
@Getter
@AllArgsConstructor
public enum NotifyEventEnum {
    RUN_FAILED("RUN_FAILED", "运行失败"),
    GATE_BLOCKED("GATE_BLOCKED", "门禁阻塞"),
    GATE_FETCH_FAILED("GATE_FETCH_FAILED", "门禁数据获取失败"),
    EXEMPTION_EXPIRED("EXEMPTION_EXPIRED", "豁免到期"),
    RC_PENDING_APPROVAL("RC_PENDING_APPROVAL", "发布单待审批"),
    RELEASE_STARTED("RELEASE_STARTED", "发布开始"),
    RELEASE_CIRCUIT("RELEASE_CIRCUIT", "发布熔断"),
    ROLLBACK_DONE("ROLLBACK_DONE", "回滚完成"),
    ACTION_SUCCEEDED("ACTION_SUCCEEDED", "动作执行成功");

    private final String code;
    private final String desc;

    public static NotifyEventEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(String code) {
        return of(code) != null;
    }
}
