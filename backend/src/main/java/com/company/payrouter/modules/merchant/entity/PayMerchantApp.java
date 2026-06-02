package com.company.payrouter.modules.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("pay_merchant_app")
public class PayMerchantApp {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long poolId;
    private String appId;
    private String appName;
    private String secretEncrypted;
    private String notifyUrlWhitelist;
    private Integer rateLimitPerMinute;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
