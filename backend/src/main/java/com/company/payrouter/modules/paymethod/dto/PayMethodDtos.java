package com.company.payrouter.modules.paymethod.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class PayMethodDtos {
    private PayMethodDtos() {
    }

    public record PayMethodResponse(
            Long id,
            String tenantId,
            String methodCode,
            String methodName,
            Boolean enabled,
            Boolean reserved,
            Integer sortOrder,
            String remark,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record PayMethodUpdateRequest(
            @NotBlank String methodName,
            @NotNull Boolean enabled,
            Integer sortOrder,
            String remark
    ) {
    }
}
