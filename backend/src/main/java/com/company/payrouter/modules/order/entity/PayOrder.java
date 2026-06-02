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
@TableName("pay_order")
public class PayOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long poolId;
    private Long accountId;
    private String appId;
    private String payMethod;
    private String merchantOrderNo;
    private String platformOrderNo;
    private String channelOrderNo;
    private String upstreamOrderTime;
    private BigDecimal amount;
    private String subject;
    private String authCodeMasked;
    private String notifyUrl;
    private String status;
    private String routeType;
    private Long routeRecordId;
    private String upstreamResponseCode;
    private String upstreamResponseMsg;
    private LocalDateTime paySuccessTime;
    private LocalDateTime expiredTime;
    private LocalDateTime lastQueryTime;
    private Integer queryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
