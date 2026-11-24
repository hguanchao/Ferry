package com.ferry.devops.api.enums;

import com.ferry.common.core.utils.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 运行状态
 *
 * <p>编号按生命周期分组，便于判定边界：
 * <ul>
 *   <li>0 待确认：Jenkins 触发结果不确定，等待轮询收敛</li>
 *   <li>1 排队中、2 运行中、3 阻塞：已进入执行流程的进行中状态</li>
 *   <li>4 成功、5 失败、6 超时、7 已取消：终态</li>
 *   <li>8 预检未通过：未进入执行流程，落库仅用于沉淀拒绝原因</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum RunStatusEnum {
    /** 待确认：Jenkins 触发结果不确定，等待轮询收敛 */
    UNKNOWN(0, "待确认"),
    QUEUED(1, "排队中"),
    RUNNING(2, "运行中"),
    BLOCKED(3, "阻塞"),
    SUCCESS(4, "成功"),
    FAILURE(5, "失败"),
    TIMEOUT(6, "超时"),
    CANCELLED(7, "已取消"),
    /** 预检未通过：运行记录已创建但不会下发 Jenkins */
    PRECHECK(8, "预检未通过");

    private final Integer code;
    private final String desc;

    public static RunStatusEnum of(Integer code) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equals(item.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public static boolean contains(Integer code) {
        return of(code) != null;
    }

    /**
     * 是否为终态（成功/失败/超时/已取消）：终态不再接受状态推进与人工干预。
     *
     * @param code 状态码
     * @return 是否终态
     */
    public static boolean isTerminal(Integer code) {
        return ObjUtil.equals(code, SUCCESS.getCode())
                || ObjUtil.equals(code, FAILURE.getCode())
                || ObjUtil.equals(code, TIMEOUT.getCode())
                || ObjUtil.equals(code, CANCELLED.getCode());
    }

    /**
     * 是否为进行中状态（排队/运行/阻塞/结果待确认）：用于判断运行是否仍占用流水线。
     *
     * @param code 状态码
     * @return 是否进行中
     */
    public static boolean isActive(Integer code) {
        return ObjUtil.equals(code, QUEUED.getCode())
                || ObjUtil.equals(code, RUNNING.getCode())
                || ObjUtil.equals(code, BLOCKED.getCode())
                || ObjUtil.equals(code, UNKNOWN.getCode());
    }
}
