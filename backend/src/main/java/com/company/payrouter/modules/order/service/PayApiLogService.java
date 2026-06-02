package com.company.payrouter.modules.order.service;

import com.company.payrouter.common.exception.BusinessErrorCode;
import com.company.payrouter.common.logging.SensitiveLogMasker;
import com.company.payrouter.modules.order.entity.PayApiLog;
import com.company.payrouter.modules.order.mapper.PayApiLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PayApiLogService {
    private final PayApiLogMapper logMapper;
    private final ObjectMapper objectMapper;

    public PayApiLogService(PayApiLogMapper logMapper, ObjectMapper objectMapper) {
        this.logMapper = logMapper;
        this.objectMapper = objectMapper;
    }

    public void record(Long orderId, String merchantOrderNo, String direction, String apiType, Object request, Object response, String status, String error) {
        record(orderId, merchantOrderNo, direction, apiType, request, response, status, error, null);
    }

    public void record(Long orderId, String merchantOrderNo, String direction, String apiType, Object request, Object response, String status, String error, BusinessErrorCode errorCode) {
        PayApiLog log = new PayApiLog();
        log.setOrderId(orderId);
        log.setMerchantOrderNo(merchantOrderNo);
        log.setDirection(direction);
        log.setApiType(apiType);
        log.setRequestUrl(apiType);
        log.setRequestBody(json(request));
        log.setResponseBody(json(response));
        log.setHttpStatus(error == null ? 200 : 500);
        log.setSuccess(error == null);
        log.setErrorCode(errorCode == null ? null : String.valueOf(errorCode.code()));
        log.setResultStatus(status);
        log.setErrorMessage(error);
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
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
