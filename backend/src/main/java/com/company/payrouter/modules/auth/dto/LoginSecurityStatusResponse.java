package com.company.payrouter.modules.auth.dto;

public record LoginSecurityStatusResponse(
        boolean captchaRequired,
        boolean locked
) {
}
