package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 应用档案
 */
@Data
@TableName(value = "dev_app")
@EqualsAndHashCode(callSuper = true)
public class App extends BaseEntity implements Serializable {

    /**
     * 数据id
     */
    @TableId
    private Long id;

    /**
     * 应用编码
     */
    private String code;

    /**
     * 应用名称
     */
    private String name;

    /**
     * 所属团队（部门id）
     */
    private Long deptId;

    /**
     * 应用负责人id，1-3 人，逗号分隔
     *
     * <p>落库是字符串而不是关联表：当前只按应用维度读写，没有按人反查的场景，
     * 等审批流需要「我是谁负责的应用」时再拆表。
     */
    private String ownerIds;

    /**
     * 应用负责人名称，与 ownerIds 顺序一致，逗号分隔
     *
     * <p>冗余存名称，列表和详情直接随应用读出，不必为展示姓名去用户域回表。
     * 用户改名后不回溯刷新，以应用档案保存时的名称为准。
     */
    private String ownerNames;

    /**
     * 应用描述
     */
    private String description;

    /**
     * 核心链路 0：否 1：是
     */
    @TableField("is_core")
    private Integer isCore;

    /**
     * 状态 0：停用 1：正常
     */
    private Integer status;

    /**
     * 代码托管平台
     */
    private String repoType;

    /**
     * 仓库 Git 地址
     */
    private String repoUrl;

    /**
     * 默认分支
     */
    private String repoBranch;

    /**
     * 指定提交id，留空取默认分支最新提交
     */
    private String repoCommitId;

    /**
     * 平台访问令牌，AES 加密存储
     */
    private String repoToken;
}
