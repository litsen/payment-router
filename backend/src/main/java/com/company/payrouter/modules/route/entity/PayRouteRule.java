package com.company.payrouter.modules.route.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("pay_route_rule")
public class PayRouteRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private String ruleName;
    private String ruleCode;
    private Long poolId;
    private String payMethod;
    private String ruleType;
    private String ruleConfigJson;
    private Integer priority;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
