package com.company.payrouter.modules.route.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("pay_account_limit_flow")
public class PayAccountLimitFlow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long orderId;
    private String merchantOrderNo;
    private Long accountId;
    private Long bucketId;
    private String periodType;
    private LocalDateTime periodStart;
    private BigDecimal amount;
    private String flowStatus;
    private LocalDateTime reservedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime releasedAt;
    private String releaseReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
