package com.company.payrouter.modules.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class OrderDtos {
    private OrderDtos() {
    }

    public record OrderResponse(
            Long id,
            String tenantId,
            String appId,
            String merchantOrderNo,
            String platformOrderNo,
            String channelOrderNo,
            String payMethod,
            BigDecimal amount,
            String subject,
            String authCodeMasked,
            String upstreamOrderTime,
            String routeType,
            Long routeRecordId,
            Long poolId,
            String poolName,
            Long accountId,
            String accountName,
            String accountApiKeyMasked,
            String accountApiKey,
            String status,
            String notifyUrl,
            LocalDateTime paySuccessTime,
            LocalDateTime expiredTime,
            LocalDateTime lastQueryTime,
            Integer queryCount,
            String upstreamResponseCode,
            String upstreamResponseMsg,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record OrderLogResponse(
            Long id,
            String tenantId,
            Long orderId,
            String merchantOrderNo,
            String direction,
            String apiType,
            String requestUrl,
            String requestHeadersJson,
            String requestBody,
            String responseBody,
            Integer httpStatus,
            Long costMs,
            Boolean success,
            String errorCode,
            String resultStatus,
            String errorMessage,
            LocalDateTime createdAt
    ) {
    }

    public record NotifyLogResponse(
            Long id,
            String tenantId,
            Long orderId,
            String merchantOrderNo,
            String notifyBody,
            Boolean verified,
            Boolean success,
            String errorMessage,
            LocalDateTime createdAt
    ) {
    }

    public record RefundOrderResponse(
            Long id,
            String tenantId,
            Long orderId,
            Long poolId,
            String poolName,
            Long accountId,
            String accountName,
            String appId,
            String merchantOrderNo,
            String merchantRefundNo,
            String platformOrderNo,
            String channelOrderNo,
            String upstreamRefundNo,
            BigDecimal orderAmount,
            BigDecimal refundAmount,
            String reason,
            String notifyUrl,
            String status,
            String upstreamResponseCode,
            String upstreamResponseMsg,
            String upstreamRawResponse,
            LocalDateTime refundSuccessTime,
            LocalDateTime lastQueryTime,
            Integer queryCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
