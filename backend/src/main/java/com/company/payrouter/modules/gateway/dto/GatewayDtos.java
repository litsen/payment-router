package com.company.payrouter.modules.gateway.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class GatewayDtos {
    private GatewayDtos() {
    }

    public interface PayCreateRequest {
        String appId();

        String merchantOrderNo();

        BigDecimal amount();

        String subject();

        String notifyUrl();

        Long timestamp();

        String nonce();

        String sign();
    }

    public record BarcodePayRequest(
            @NotBlank String appId,
            @NotBlank String merchantOrderNo,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String authCode,
            @NotBlank String subject,
            String notifyUrl,
            @NotNull Long timestamp,
            @NotBlank String nonce,
            @NotBlank String sign
    ) implements PayCreateRequest {
    }

    public record PreOrderRequest(
            @NotBlank String appId,
            @NotBlank String merchantOrderNo,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String subject,
            String notifyUrl,
            String successUrl,
            String errorUrl,
            @NotNull Long timestamp,
            @NotBlank String nonce,
            @NotBlank String sign
    ) implements PayCreateRequest {
    }

    public record DecodeBarRequest(
            @NotBlank String appId,
            @NotBlank String authCode,
            String sceneNo,
            String subAppId,
            @NotNull Long timestamp,
            @NotBlank String nonce,
            @NotBlank String sign
    ) {
    }

    public record ScanPayRequest(
            @NotBlank String appId,
            @NotBlank String merchantOrderNo,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String subject,
            String notifyUrl,
            @NotNull Long timestamp,
            @NotBlank String nonce,
            @NotBlank String sign
    ) implements PayCreateRequest {
    }

    public record H5PayRequest(
            @NotBlank String appId,
            @NotBlank String merchantOrderNo,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String subject,
            String notifyUrl,
            String returnUrl,
            @NotNull Long timestamp,
            @NotBlank String nonce,
            @NotBlank String sign
    ) implements PayCreateRequest {
    }

    public record QrcodePayRequest(
            @NotBlank String appId,
            @NotBlank String merchantOrderNo,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String subject,
            @NotBlank String service,
            String notifyUrl,
            @NotNull Long timestamp,
            @NotBlank String nonce,
            @NotBlank String sign
    ) implements PayCreateRequest {
    }

    public record WechatJsapiPayRequest(
            @NotBlank String appId,
            @NotBlank String merchantOrderNo,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String subject,
            @NotBlank String subAppId,
            @NotBlank String subOpenId,
            String notifyUrl,
            @NotNull Long timestamp,
            @NotBlank String nonce,
            @NotBlank String sign
    ) implements PayCreateRequest {
    }

    public record AlipayJsapiPayRequest(
            @NotBlank String appId,
            @NotBlank String merchantOrderNo,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String subject,
            String subAppId,
            String buyerId,
            String buyerOpenId,
            String notifyUrl,
            @NotNull Long timestamp,
            @NotBlank String nonce,
            @NotBlank String sign
    ) implements PayCreateRequest {
    }

    public record RefundRequest(
            @NotBlank String appId,
            @NotBlank String merchantOrderNo,
            String merchantRefundNo,
            @NotNull @DecimalMin(value = "0.01") BigDecimal refundAmount,
            String reason,
            String notifyUrl,
            @NotNull Long timestamp,
            @NotBlank String nonce,
            @NotBlank String sign
    ) {
    }

    public record RefundQueryRequest(
            @NotBlank String appId,
            @NotBlank String merchantOrderNo,
            String merchantRefundNo,
            @NotNull Long timestamp,
            @NotBlank String nonce,
            @NotBlank String sign,
            Boolean forceQuery
    ) {
    }

    public record QueryPayRequest(
            @NotBlank String appId,
            @NotBlank String merchantOrderNo,
            @NotNull Long timestamp,
            @NotBlank String nonce,
            @NotBlank String sign
    ) {
    }

    public record PayResponse(
            String appId,
            String merchantOrderNo,
            BigDecimal amount,
            String payMethod,
            String status,
            String platformOrderNo,
            String channelOrderNo,
            Object payData,
            String message
    ) {
    }

    public record DecodeBarResponse(
            String appId,
            Object data,
            String message
    ) {
    }

    public record RefundResponse(
            String appId,
            String merchantOrderNo,
            String merchantRefundNo,
            BigDecimal refundAmount,
            String status,
            Object data,
            String message
    ) {
    }
}
