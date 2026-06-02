package com.company.payrouter.modules.auth.dto;

import java.util.Set;

public record CurrentUserResponse(
        Long id,
        String username,
        String realName,
        Set<String> roles,
        Set<String> permissions
) {
}
