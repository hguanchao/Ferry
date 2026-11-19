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

    /** 主键id */
    @TableId
    private Long id;

    /** 所属团队id，关联 sys_dept.id */
    private Long deptId;

    /** 应用负责人id，关联 sys_user.id */
    private Long ownerId;

    /** 应用编码 */
    private String code;

    /** 应用名称 */
    private String name;

    /** 应用描述 */
    private String description;

    /** 核心链路 0否 1是 */
    private Integer coreLink;

    /** 代码平台 1GitLab 2GitHub 3Gitee */
    private Integer repoType;

    /** 仓库分支 */
    private String repoBranch;

    /** 仓库提交id */
    private String repoCommitId;

    /** 仓库git地址 */
    private String repoUrl;

    /** 仓库访问令牌 */
    private String repoToken;

    /** 应用状态 0停用 1启用 2草稿 3完善 */
    private Integer status;
}
