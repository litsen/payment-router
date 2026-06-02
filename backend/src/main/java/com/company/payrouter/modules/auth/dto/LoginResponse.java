package com.company.payrouter.modules.auth.dto;

import java.util.List;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        CurrentUserResponse user
) {
}
