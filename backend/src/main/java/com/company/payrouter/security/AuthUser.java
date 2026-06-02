package com.company.payrouter.security;

import java.util.Set;

public record AuthUser(
        Long userId,
        String username,
        String realName,
        Set<String> roles,
        Set<String> permissions
) {
    public boolean hasRole(String roleCode) {
        return roles.contains(roleCode);
    }
}
