package com.company.payrouter.modules.paymethod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("pay_method")
public class PayMethod {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private String methodCode;
    private String methodName;
    private Boolean enabled;
    private Integer sortOrder;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
