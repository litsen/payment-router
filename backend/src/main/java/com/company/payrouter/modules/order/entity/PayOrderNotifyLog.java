package com.company.payrouter.modules.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("pay_order_notify_log")
public class PayOrderNotifyLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long orderId;
    private String merchantOrderNo;
    private String notifyBody;
    private Boolean verified;
    private Boolean success;
    private String errorMessage;
    private LocalDateTime createdAt;
}
