package com.company.payrouter.modules.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("pay_refund_order")
public class PayRefundOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long orderId;
    private Long poolId;
    private Long accountId;
    private String appId;
    private String merchantOrderNo;
    private String merchantRefundNo;
    private String platformOrderNo;
    private String channelOrderNo;
    private String upstreamRefundNo;
    private BigDecimal orderAmount;
    private BigDecimal refundAmount;
    private String reason;
    private String notifyUrl;
    private String status;
    private String upstreamResponseCode;
    private String upstreamResponseMsg;
    private String upstreamRawResponse;
    private LocalDateTime refundSuccessTime;
    private LocalDateTime lastQueryTime;
    private Integer queryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
