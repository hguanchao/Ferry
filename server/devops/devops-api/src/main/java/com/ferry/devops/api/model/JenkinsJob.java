package com.ferry.devops.api.model;

import lombok.Data;

@Data
public class JenkinsJob {

    /**
     * Job名称
     */
    private String name;

    /**
     * 声明式脚本
     */
    private String script;

    /**
     * 应用编码
     */
    private String appCode;

    /**
     * 仓库URL
     */
    private String repoUrl;

    /**
     * 仓库名称
     */
    private String repoName;

    /**
     * 是否禁用
     */
    private boolean disabled;
}
