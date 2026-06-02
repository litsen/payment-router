package com.company.payrouter.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.Set;

public final class RoleDtos {
    private RoleDtos() {
    }

    public record RoleResponse(
            Long id,
            String roleCode,
            String roleName,
            String description,
            Set<String> permissions,
            LocalDateTime createdAt
    ) {
    }

    public record RoleCreateRequest(
            @NotBlank String roleCode,
            @NotBlank String roleName,
            String description,
            @NotEmpty Set<String> permissions
    ) {
    }

    public record RoleUpdateRequest(
            @NotBlank String roleName,
            String description,
            @NotEmpty Set<String> permissions
    ) {
    }
}
