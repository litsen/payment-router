package com.company.payrouter.modules.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("pay_order_request_log")
public class PayApiLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String tenantId;
    private String merchantOrderNo;
    private String direction;
    private String apiType;
    private String requestUrl;
    private String requestHeadersJson;
    private String requestBody;
    private String responseBody;
    private Integer httpStatus;
    private Long costMs;
    private Boolean success;
    private String errorCode;
    private String resultStatus;
    private String errorMessage;
    private LocalDateTime createdAt;
}
