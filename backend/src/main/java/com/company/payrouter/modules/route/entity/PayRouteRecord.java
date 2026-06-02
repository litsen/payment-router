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
@TableName("pay_route_record")
public class PayRouteRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long orderId;
    private String merchantOrderNo;
    private Long poolId;
    private Long accountId;
    private Long routeRuleId;
    private String routeType;
    private String routeSnapshotJson;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
