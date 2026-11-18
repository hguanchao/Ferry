package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 部署锁镜像
 */
@Data
@TableName("dev_deploy_lock")
public class DeployLock implements Serializable {

    @TableId
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    private Long appId;

    private Long envId;

    private String owner;

    private String ownerIp;

    private Date acquiredAt;

    private Date heartbeatAt;

    private Date expireAt;

    private Integer isActive;
}
