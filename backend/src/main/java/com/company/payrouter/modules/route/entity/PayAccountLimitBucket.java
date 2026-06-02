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
@TableName("pay_account_limit_bucket")
public class PayAccountLimitBucket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long accountId;
    private String periodType;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private BigDecimal limitAmount;
    private BigDecimal usedAmount;
    private BigDecimal reservedAmount;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
