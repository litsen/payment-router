package com.company.payrouter.config;

import com.company.payrouter.infrastructure.crypto.CryptoProperties;
import com.company.payrouter.security.JwtProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ProductionSecurityValidator implements ApplicationRunner {
    public static final String DEFAULT_JWT_SECRET = "payment-router-jwt-secret-change-me-at-least-32-bytes";
    public static final String DEFAULT_AES_KEY = "payment-router-local-dev-aes-key-change-me";
    public static final String DEFAULT_DATABASE_PASSWORD = "payment_router";

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final CryptoProperties cryptoProperties;
    private final String datasourcePassword;
    private final boolean enforceProductionSecrets;

    public ProductionSecurityValidator(
            Environment environment,
            JwtProperties jwtProperties,
            CryptoProperties cryptoProperties,
            @Value("${spring.datasource.password:}") String datasourcePassword,
            @Value("${pay-router.security.enforce-production-secrets:false}") boolean enforceProductionSecrets
    ) {
        this.environment = environment;
        this.jwtProperties = jwtProperties;
        this.cryptoProperties = cryptoProperties;
        this.datasourcePassword = datasourcePassword;
        this.enforceProductionSecrets = enforceProductionSecrets;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!shouldValidate()) {
            return;
        }
        List<String> errors = validateProductionSecrets(
                jwtProperties.secret(),
                cryptoProperties.getAesKey(),
                datasourcePassword
        );
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Production security configuration is unsafe: " + String.join("; ", errors));
        }
    }

    public static List<String> validateProductionSecrets(String jwtSecret, String aesKey, String datasourcePassword) {
        List<String> errors = new ArrayList<>();
        if (!StringUtils.hasText(jwtSecret)) {
            errors.add("PAY_ROUTER_JWT_SECRET must be configured");
        } else {
            if (jwtSecret.length() < 32) {
                errors.add("PAY_ROUTER_JWT_SECRET must be at least 32 characters");
            }
            if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
                errors.add("PAY_ROUTER_JWT_SECRET must not use the development default");
            }
        }

        if (!StringUtils.hasText(aesKey)) {
            errors.add("PAY_ROUTER_AES_KEY must be configured");
        } else {
            if (aesKey.length() < 16) {
                errors.add("PAY_ROUTER_AES_KEY must be at least 16 characters");
            }
            if (DEFAULT_AES_KEY.equals(aesKey)) {
                errors.add("PAY_ROUTER_AES_KEY must not use the development default");
            }
        }

        if (!StringUtils.hasText(datasourcePassword)) {
            errors.add("SPRING_DATASOURCE_PASSWORD must be configured");
        } else if (DEFAULT_DATABASE_PASSWORD.equals(datasourcePassword)) {
            errors.add("SPRING_DATASOURCE_PASSWORD must not use the development default");
        }
        return errors;
    }

    private boolean shouldValidate() {
        return enforceProductionSecrets || Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
    }
}
