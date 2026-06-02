package com.company.payrouter.infrastructure.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pay-router.crypto")
public class CryptoProperties {
    private String aesKey = "payment-router-local-dev-aes-key-change-me";

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
}
