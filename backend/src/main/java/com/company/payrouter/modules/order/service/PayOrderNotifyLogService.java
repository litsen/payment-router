package com.company.payrouter.modules.order.service;

import com.company.payrouter.common.logging.SensitiveLogMasker;
import com.company.payrouter.modules.order.entity.PayOrderNotifyLog;
import com.company.payrouter.modules.order.mapper.PayOrderNotifyLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PayOrderNotifyLogService {
    private final PayOrderNotifyLogMapper notifyLogMapper;
    private final ObjectMapper objectMapper;

    public PayOrderNotifyLogService(PayOrderNotifyLogMapper notifyLogMapper, ObjectMapper objectMapper) {
        this.notifyLogMapper = notifyLogMapper;
        this.objectMapper = objectMapper;
    }

    public void record(String tenantId, Long orderId, String merchantOrderNo, Object body, boolean verified, boolean success, String errorMessage) {
        PayOrderNotifyLog log = new PayOrderNotifyLog();
        log.setTenantId(tenantId);
        log.setOrderId(orderId);
        log.setMerchantOrderNo(merchantOrderNo);
        log.setNotifyBody(json(body));
        log.setVerified(verified);
        log.setSuccess(success);
        log.setErrorMessage(errorMessage);
        log.setCreatedAt(LocalDateTime.now());
        notifyLogMapper.insert(log);
    }

    private String json(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return SensitiveLogMasker.mask(text);
        }
        try {
            return SensitiveLogMasker.mask(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException exception) {
            return SensitiveLogMasker.mask(String.valueOf(value));
        }
    }
}
