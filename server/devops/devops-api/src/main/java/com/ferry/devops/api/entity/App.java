package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用档案
 */
@Data
@TableName(value = "dev_app")
@EqualsAndHashCode(callSuper = true)
public class App extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 应用编码 */
    private String code;

    /** 应用名称 */
    private String name;

    /** 所属团队 */
    private Long teamId;

    /** 核心链路 0 否 1 是 */
    private Integer isCore;

    /** 代码平台 1 GitLab 2 GitHub 3 Gitee */
    private Integer platform;

    /** 默认分支 */
    private String defaultBranch;

    /** 默认提交 */
    private String defaultCommitId;

    /** 对象存储 bucket */
    private String storageBucket;

    /** 对象存储路径前缀 */
    private String storagePath;

    /** OCI Registry 仓库名 */
    private String registryRepo;

    /** 监控系统类型 */
    private String monitorType;

    /** 监控大盘地址 */
    private String monitorDashboard;

    /** 告警规则组 */
    private String alertGroup;

    /** 值班时段 */
    private String oncallWindow;

    /** 接入完善度 1 待完善 2 已完善 */
    private Integer setupStatus;

    /** 描述 */
    private String description;
}