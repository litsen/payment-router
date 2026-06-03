package com.company.payrouter.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.payrouter.infrastructure.crypto.AesCryptoService;
import com.company.payrouter.modules.gateway.service.GatewaySignService;
import com.company.payrouter.modules.merchant.entity.PayMerchantApp;
import com.company.payrouter.modules.merchant.mapper.PayMerchantAppMapper;
import com.company.payrouter.modules.order.entity.PayOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MerchantNotifyService {
    private final PayMerchantAppMapper appMapper;
    private final AesCryptoService cryptoService;
    private final GatewaySignService signService;
    private final PayOrderNotifyLogService notifyLogService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public MerchantNotifyService(
            PayMerchantAppMapper appMapper,
            AesCryptoService cryptoService,
            GatewaySignService signService,
            PayOrderNotifyLogService notifyLogService,
            ObjectMapper objectMapper
    ) {
        this.appMapper = appMapper;
        this.cryptoService = cryptoService;
        this.signService = signService;
        this.notifyLogService = notifyLogService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public void notifyPaymentResult(PayOrder order) {
        if (order == null || !StringUtils.hasText(order.getNotifyUrl())) {
            return;
        }
        Map<String, Object> payload = buildPayload(order);
        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(order.getNotifyUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            notifyLogService.record(
                    order.getTenantId(),
                    order.getId(),
                    order.getMerchantOrderNo(),
                    outboundLog(order.getNotifyUrl(), payload, response.statusCode(), response.body()),
                    true,
                    success,
                    success ? null : "Merchant notify returned HTTP " + response.statusCode()
            );
        } catch (Exception exception) {
            notifyLogService.record(
                    order.getTenantId(),
                    order.getId(),
                    order.getMerchantOrderNo(),
                    outboundLog(order.getNotifyUrl(), payload, null, null),
                    true,
                    false,
                    exception.getMessage()
            );
        }
    }

    private Map<String, Object> buildPayload(PayOrder order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appId", order.getAppId());
        payload.put("merchantOrderNo", order.getMerchantOrderNo());
        payload.put("amount", order.getAmount());
        payload.put("payMethod", order.getPayMethod());
        payload.put("status", order.getStatus());
        payload.put("platformOrderNo", order.getPlatformOrderNo());
        payload.put("channelOrderNo", order.getChannelOrderNo());
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("nonce", UUID.randomUUID().toString().replace("-", ""));
        payload.put("sign", signService.sha256Sign(payload, appSecret(order.getAppId())));
        return payload;
    }

    private String appSecret(String appId) {
        PayMerchantApp app = appMapper.selectOne(new LambdaQueryWrapper<PayMerchantApp>()
                .eq(PayMerchantApp::getAppId, appId));
        if (app == null) {
            return "";
        }
        return cryptoService.decrypt(app.getSecretEncrypted());
    }

    private Map<String, Object> outboundLog(String notifyUrl, Map<String, Object> requestBody, Integer httpStatus, String responseBody) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("direction", "OUTBOUND");
        log.put("notifyUrl", notifyUrl);
        log.put("requestBody", requestBody);
        log.put("httpStatus", httpStatus);
        log.put("responseBody", responseBody);
        return log;
    }
}
