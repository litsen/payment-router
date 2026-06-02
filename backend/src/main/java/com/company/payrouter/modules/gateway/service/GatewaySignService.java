package com.company.payrouter.modules.gateway.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class GatewaySignService {
    public String sha256Sign(Map<String, ?> params, String apiSecret) {
        String canonical = new TreeMap<>(params).entrySet().stream()
                .filter(entry -> !"sign".equals(entry.getKey()))
                .filter(entry -> entry.getValue() != null)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        return sha256(canonical + apiSecret);
    }

    public boolean verify(Map<String, ?> params, String apiSecret, String sign) {
        return sign != null && sign.equalsIgnoreCase(sha256Sign(params, apiSecret));
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Sign digest failed", exception);
        }
    }
}
