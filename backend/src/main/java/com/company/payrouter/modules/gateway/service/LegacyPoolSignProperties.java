package com.company.payrouter.modules.gateway.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pay-router.gateway-security.legacy-pool-sign")
public class LegacyPoolSignProperties {
    private boolean enabled = true;
    private boolean fallbackAfterDefaultAppSecretFailure = true;
    private boolean warnOnly = true;
    private String allowPoolCodes = "";
    private String denyPoolCodes = "";

    public boolean allows(String poolCode) {
        if (codes(denyPoolCodes).contains(poolCode)) {
            return warnOnly;
        }
        if (enabled) {
            return true;
        }
        Set<String> allowList = codes(allowPoolCodes);
        return allowList.contains(poolCode) || warnOnly;
    }

    public boolean allowsDefaultAppFallback(String poolCode) {
        if (codes(denyPoolCodes).contains(poolCode)) {
            return warnOnly;
        }
        return fallbackAfterDefaultAppSecretFailure || warnOnly;
    }

    public boolean wouldReject(String poolCode, boolean defaultAppFallback) {
        if (codes(denyPoolCodes).contains(poolCode)) {
            return true;
        }
        if (defaultAppFallback) {
            return !fallbackAfterDefaultAppSecretFailure;
        }
        if (enabled) {
            return false;
        }
        return !codes(allowPoolCodes).contains(poolCode);
    }

    private Set<String> codes(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        return Arrays.stream(value.split("[,\\n\\r]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }
}
