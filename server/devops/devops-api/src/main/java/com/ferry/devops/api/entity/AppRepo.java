package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用代码源
 */
@Data
@TableName(value = "dev_app_repo")
@EqualsAndHashCode(callSuper = true)
public class AppRepo extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 应用id */
    private Long appId;

    /** 平台枚举 */
    private Integer platform;

    /** group/project 路径 */
    private String repoPath;

    /** 完整 Git 地址 */
    private String repoUrl;

    /** 仓库访问令牌，明文存储（内部系统使用） */
    private String repoToken;

    /** 是否主仓库 */
    private Integer isMain;

    /** 该仓库默认分支 */
    private String defaultBranch;

    /** 是否公开仓库 */
    private Integer isPublic;
}