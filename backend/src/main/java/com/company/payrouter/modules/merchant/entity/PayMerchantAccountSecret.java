package com.company.payrouter.modules.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("pay_merchant_account_secret")
public class PayMerchantAccountSecret {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private String apiKeyEncrypted;
    private String privateKeyEncrypted;
    private String publicKeyEncrypted;
    private String certPath;
    private String certPasswordEncrypted;
    private String extraConfigJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
