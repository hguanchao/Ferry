package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 运行阶段
 */
@Data
@TableName(value = "dev_run_stage")
@EqualsAndHashCode(callSuper = true)
public class RunStage extends BaseEntity implements Serializable {

    /** 主键id */
    @TableId
    private Long id;

    /** 运行id */
    private Long runId;

    /** 阶段类型 0=预检 1=构建 2=单元测试 3=静态扫描 4=制品入库 5=部署非生产 6=集成测试 7=准入 8=生产发布 */
    private Integer type;

    /** 阶段名称 */
    private String name;

    /** 状态 1=未开始 2=进行中 3=成功 4=失败 5=阻塞 6=跳过 7=超时 8=已复用 9=已取消 10=待门禁 */
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
