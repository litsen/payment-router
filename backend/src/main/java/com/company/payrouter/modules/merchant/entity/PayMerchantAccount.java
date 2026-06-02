package com.company.payrouter.modules.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@TableName("pay_merchant_account")
public class PayMerchantAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long poolId;
    private String accountName;
    private String channelCode;
    private String apiKeyEncrypted;
    private String signKeyEncrypted;
    private String supportPayMethods;
    private Integer priority;
    private Integer weight;
    private BigDecimal dailyAmountLimit;
    private BigDecimal monthlyAmountLimit;
    private BigDecimal singleMinAmount;
    private BigDecimal singleMaxAmount;
    private LocalDate availableStartDate;
    private LocalDate availableEndDate;
    private LocalTime availableStartTime;
    private LocalTime availableEndTime;
    private String status;
    private Integer failCount;
    private LocalDateTime lastFailTime;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
