package com.company.payrouter.modules.auth.dto;

public record CaptchaResponse(
        boolean required,
        String captchaId,
        String imageBase64
) {
}
