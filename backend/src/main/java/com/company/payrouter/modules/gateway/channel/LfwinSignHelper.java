package com.company.payrouter.modules.gateway.channel;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class LfwinSignHelper {
    public String md5Sign(Map<String, String> params, String signKey) {
        String text = canonical(params) + "&signkey=" + signKey;
        return digest("MD5", text);
    }

    public boolean verifyMd5(Map<String, String> params, String signKey) {
        String sign = params.get("sign");
        return sign != null && sign.equalsIgnoreCase(md5Sign(params, signKey));
    }

    public String canonical(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(entry -> !"sign".equals(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + (entry.getValue() == null ? "" : entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String digest(String algorithm, String text) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            byte[] bytes = messageDigest.digest(text.getBytes(StandardCharsets.UTF_8));
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
