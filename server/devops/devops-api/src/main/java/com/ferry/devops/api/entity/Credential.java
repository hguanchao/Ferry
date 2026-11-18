package com.ferry.devops.api.entity;
import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 凭据
 */
@Data
@TableName(value = "dev_credential")
@EqualsAndHashCode(callSuper = true)
public class Credential extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 凭据名称 */
    private String name;

    /** 凭据类型 1SSH密钥 2对象存储AK/SK 3Registry账号 4K8s凭据 5第三方Token */
    private Integer type;

    /** AES密文 */
    private String cipherText;

    /** 引用数 */
    private Integer refCount;

    /** 最近轮换时间 */
    private java.util.Date lastRotateTime;
}