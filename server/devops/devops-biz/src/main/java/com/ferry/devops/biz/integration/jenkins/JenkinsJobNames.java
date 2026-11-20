package com.ferry.devops.biz.integration.jenkins;

import com.ferry.common.core.utils.StrUtil;

import java.util.regex.Pattern;

/**
 * Jenkins Job 命名规则：{@code <应用编码>-pipeline-<流水线id>}。
 *
 * <p>应用编码创建后不可修改、流水线 id 不变，因此 Job 名可实时拼接、无需落库；
 * 一个应用的多条流水线按 id 天然隔离。白名单校验防止路径注入（仅字母/数字/中划线）。
 */
public final class JenkinsJobNames {

    /** 应用编码与流水线 id 之间的分隔段 */
    private static final String PIPELINE_SEGMENT = "-pipeline-";

    /** Jenkins Job 名白名单：字母/数字/中划线，总长上限 119（留足 URL 与文件名余量） */
    private static final Pattern JOB_NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,118}");

    private JenkinsJobNames() {
    }

    /**
     * 拼接流水线 Job 名。
     *
     * @param appCode    应用编码
     * @param pipelineId 流水线 id
     * @return 合法 Job 名
     * @throws JenkinsDispatchException 应用编码为空、流水线 id 为空或拼接结果不合法
     */
    public static String pipelineJobName(String appCode, Long pipelineId) {
        String code = appCode == null ? "" : appCode.trim();
        String jobName = code + PIPELINE_SEGMENT + pipelineId;
        if (StrUtil.isBlank(code) || pipelineId == null || !JOB_NAME_PATTERN.matcher(jobName).matches()) {
            throw new JenkinsDispatchException("应用编码或流水线id不合法，无法生成 Jenkins Job 名：" + jobName, false);
        }
        return jobName;
    }
}
