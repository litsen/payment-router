package com.company.payrouter.modules.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class MerchantAppDtos {
    private MerchantAppDtos() {
    }

    public record MerchantAppResponse(
            Long id,
            String tenantId,
            Long poolId,
            String poolName,
            String appId,
            String appName,
            String secretMasked,
            String plainSecret,
            String notifyUrlWhitelist,
            Integer rateLimitPerMinute,
            String status,
            String remark,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record MerchantAppCreateRequest(
            String tenantId,
            @NotNull Long poolId,
            @NotBlank String appId,
            @NotBlank String appName,
            String notifyUrlWhitelist,
            Integer rateLimitPerMinute,
            String status,
            String remark
    ) {
    }

    public record MerchantAppUpdateRequest(
            @NotNull Long poolId,
            @NotBlank String appName,
            String notifyUrlWhitelist,
            Integer rateLimitPerMinute,
            String status,
            String remark
    ) {
    }
}
