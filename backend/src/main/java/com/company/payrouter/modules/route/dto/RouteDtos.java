package com.company.payrouter.modules.route.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class RouteDtos {
    private RouteDtos() {
    }

    public record RouteRuleResponse(
            Long id,
            String tenantId,
            String ruleName,
            String ruleCode,
            Long poolId,
            String poolName,
            String payMethod,
            String ruleType,
            String ruleConfigJson,
            Integer priority,
            Boolean enabled,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record RouteRuleCreateRequest(
            String tenantId,
            @NotBlank String ruleName,
            String ruleCode,
            @NotNull Long poolId,
            @NotBlank String payMethod,
            @NotBlank String ruleType,
            String ruleConfigJson,
            Integer priority,
            Boolean enabled
    ) {
    }

    public record RouteRuleUpdateRequest(
            @NotBlank String ruleName,
            @NotNull Long poolId,
            @NotBlank String payMethod,
            @NotBlank String ruleType,
            String ruleConfigJson,
            Integer priority,
            Boolean enabled
    ) {
    }

    public record RouteTestRequest(
            @NotNull Long poolId,
            @NotBlank String payMethod,
            @NotNull BigDecimal amount,
            String merchantOrderNo,
            Boolean simulateFailure
    ) {
    }

    public record RouteResultResponse(
            Long recordId,
            Long accountId,
            String accountName,
            Long poolId,
            Long routeRuleId,
            String routeType,
            String merchantOrderNo,
            BigDecimal amount,
            Boolean simulatedFailure,
            String message
    ) {
    }

    public record RouteRecordResponse(
            Long id,
            String tenantId,
            Long orderId,
            String merchantOrderNo,
            Long poolId,
            String poolName,
            Long accountId,
            String accountName,
            Long routeRuleId,
            String routeRuleName,
            String routeType,
            String routeSnapshotJson,
            BigDecimal amount,
            LocalDateTime createdAt
    ) {
    }

    public record RoutePayTestRequest(
            @NotNull Long poolId,
            @NotBlank String payMethod,
            @NotNull BigDecimal amount,
            String authCode,
            @NotBlank String subject,
            @NotBlank String merchantOrderNo,
            String notifyUrl,
            String channel,
            String returnUrl,
            String subAppId,
            String payerId
    ) {
    }

    public record RouteQueryTestRequest(
            @NotNull Long poolId,
            @NotBlank String payMethod,
            @NotBlank String merchantOrderNo
    ) {
    }
}
