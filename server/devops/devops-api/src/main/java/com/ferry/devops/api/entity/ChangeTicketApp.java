package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 变更单应用
 */
@Data
@TableName("dev_change_ticket_app")
public class ChangeTicketApp implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 变更单id */
    private Long ticketId;

    /** 应用id */
    private Long appId;
}
