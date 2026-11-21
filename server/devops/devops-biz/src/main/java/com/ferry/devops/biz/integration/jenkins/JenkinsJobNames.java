package com.ferry.devops.biz.integration.jenkins;

import com.ferry.common.core.utils.StrUtil;

import java.util.regex.Pattern;

/**
 * Jenkins Job 命名规则：{@code <应用编码>}，一个应用一个 Job。
 *
 * <p>应用编码创建后不可修改，因此 Job 名可实时拼接、无需落库；应用与流水线 1:1，
 * 流水线的新增/编辑都按应用编码找到同一 Job 更新脚本。白名单校验防止路径注入
 * （仅字母/数字/中划线）。
 */
public final class JenkinsJobNames {

    /** Jenkins Job 名白名单：字母/数字/中划线，总长上限 119（留足 URL 与文件名余量） */
    private static final Pattern JOB_NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,118}");

    private JenkinsJobNames() {
    }

    /**
     * 拼接应用级 Job 名。
     *
     * @param appCode 应用编码
     * @return 合法 Job 名
     * @throws JenkinsDispatchException 应用编码为空或拼接结果不合法
     */
    public static String appJobName(String appCode) {
        String code = appCode == null ? "" : appCode.trim();
        if (StrUtil.isBlank(code) || !JOB_NAME_PATTERN.matcher(code).matches()) {
            throw new JenkinsDispatchException("应用编码不合法，无法生成 Jenkins Job 名：" + code, false);
        }
        return code;
    }
}
