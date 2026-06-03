package com.company.payrouter.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.Set;

public final class UserDtos {
    private UserDtos() {
    }

    public record UserResponse(
            Long id,
            String username,
            String realName,
            String status,
            Set<String> roles,
            Integer loginFailCount,
            Boolean loginLocked,
            String lockedIp,
            LocalDateTime lastFailTime,
            LocalDateTime lastLoginTime,
            LocalDateTime createdAt
    ) {
    }

    public record UserCreateRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String realName,
            @NotEmpty Set<String> roles,
            String status
    ) {
    }

    public record UserUpdateRequest(
            @NotBlank String realName,
            String password,
            @NotEmpty Set<String> roles,
            String status
    ) {
    }
}
