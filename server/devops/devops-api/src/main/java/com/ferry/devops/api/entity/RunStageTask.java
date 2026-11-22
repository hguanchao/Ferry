package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 运行阶段任务
 */
@Data
@TableName(value = "dev_run_stage_task")
@EqualsAndHashCode(callSuper = true)
public class RunStageTask extends BaseEntity implements Serializable {

    /** 主键id */
    @TableId
    private Long id;

    /** 运行id */
    private Long runId;

    /** 运行阶段id */
    private Long runStageId;

    /** 任务名称 */
    private String name;

    /** 执行器 */
    private String executor;

    /** 源码脚本路径 */
    private String scriptRef;

    /** 自定义脚本内容 */
    private String scriptContent;

    /** 状态 1=未开始 2=进行中 3=成功 4=失败 5=阻塞 6=跳过 7=超时 8=已复用 9=已取消 */
    private Integer status;

    /** 运行结果信息(JSON) */
    private String result;

    /** 排序号 */
    private Integer sort;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;
}
