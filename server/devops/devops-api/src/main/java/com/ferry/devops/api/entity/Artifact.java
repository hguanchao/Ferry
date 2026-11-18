package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 制品元数据。
 * 特殊说明：本表用 delete_seq 代替 is_deleted 做逻辑删除，故不继承 BaseEntity，
 * 自行声明 create_user/create_time/update_user/update_time 四个公共字段。
 * 所有查询都必须带 delete_seq = 0 条件。
 */
@Data
@TableName(value = "dev_artifact")
public class Artifact implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 应用id */
    private Long appId;

    /** 版本号 */
    private String version;

    /** 制品形态 1 包制品 2 镜像制品 */
    private Integer form;

    /** sha256 摘要 */
    private String digest;

    /** 字节数 */
    private Long size;

    /** 产出运行 */
    private Long runId;

    /** Jenkins 构建号 */
    private Long buildNo;

    /** 构建镜像 */
    private String buildImage;

    /** 分支 */
    private String branch;

    /** 提交 */
    private String commitId;

    /** 构建人 */
    private Long builder;

    /** 门禁汇总结论 */
    private Integer gateConclusion;

    /** 生产使用状态 1 当前生效 2 历史版本 3 从未使用 */
    private Integer prodUseStatus;

    /** 确认状态 1 正常 2 待确认 */
    private Integer confirmStatus;

    /** 删除序号，0 表示未删；软删时置为 id */
    private Long deleteSeq;

    /** 软删除时间 */
    private Date deletedAt;

    /** 删除来源 1 人工 2 清理任务 */
    private Integer deleteSource;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新人 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
