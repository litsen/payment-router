package com.company.payrouter.modules.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class MerchantAccountDtos {
    private MerchantAccountDtos() {
    }

    public record MerchantAccountResponse(
            Long id,
            String tenantId,
            Long poolId,
            String poolName,
            String accountName,
            String channelCode,
            String apiKeyMasked,
            String signKeyMasked,
            String privateKeyMasked,
            String publicKeyMasked,
            String certPath,
            String certPasswordMasked,
            String extraConfigJson,
            String supportPayMethods,
            Integer priority,
            Integer weight,
            BigDecimal dailyAmountLimit,
            BigDecimal monthlyAmountLimit,
            BigDecimal singleMinAmount,
            BigDecimal singleMaxAmount,
            LocalDate availableStartDate,
            LocalDate availableEndDate,
            LocalTime availableStartTime,
            LocalTime availableEndTime,
            String status,
            Integer failCount,
            LocalDateTime lastFailTime,
            String remark,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record MerchantAccountCreateRequest(
            String tenantId,
            @NotNull Long poolId,
            @NotBlank String accountName,
            String channelCode,
            @NotBlank String apiKey,
            @NotBlank String signKey,
            String privateKey,
            String publicKey,
            String certPath,
            String certPassword,
            String extraConfigJson,
            String supportPayMethods,
            Integer priority,
            Integer weight,
            BigDecimal dailyAmountLimit,
            BigDecimal monthlyAmountLimit,
            BigDecimal singleMinAmount,
            BigDecimal singleMaxAmount,
            LocalDate availableStartDate,
            LocalDate availableEndDate,
            LocalTime availableStartTime,
            LocalTime availableEndTime,
            String status,
            String remark
    ) {
    }

    public record MerchantAccountUpdateRequest(
            @NotNull Long poolId,
            @NotBlank String accountName,
            String channelCode,
            String apiKey,
            String signKey,
            String privateKey,
            String publicKey,
            String certPath,
            String certPassword,
            String extraConfigJson,
            String supportPayMethods,
            Integer priority,
            Integer weight,
            BigDecimal dailyAmountLimit,
            BigDecimal monthlyAmountLimit,
            BigDecimal singleMinAmount,
            BigDecimal singleMaxAmount,
            LocalDate availableStartDate,
            LocalDate availableEndDate,
            LocalTime availableStartTime,
            LocalTime availableEndTime,
            String status,
            String remark
    ) {
    }
}
