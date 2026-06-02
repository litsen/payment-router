package com.company.payrouter.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pay-router.jwt")
public record JwtProperties(String secret, long expireMinutes) {
}
