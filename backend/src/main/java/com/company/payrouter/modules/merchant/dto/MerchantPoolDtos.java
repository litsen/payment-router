package com.company.payrouter.modules.merchant.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public final class MerchantPoolDtos {
    private MerchantPoolDtos() {
    }

    public record MerchantPoolResponse(
            Long id,
            String tenantId,
            String poolName,
            String poolCode,
            String appId,
            String appSecretMasked,
            String plainAppSecret,
            String status,
            String remark,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record MerchantPoolCreateRequest(
            String tenantId,
            @NotBlank String poolName,
            String poolCode,
            String status,
            String remark
    ) {
    }

    public record MerchantPoolUpdateRequest(
            @NotBlank String poolName,
            String status,
            String remark
    ) {
    }
}
