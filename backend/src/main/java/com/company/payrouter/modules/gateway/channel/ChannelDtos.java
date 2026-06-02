package com.company.payrouter.modules.gateway.channel;

import com.company.payrouter.modules.merchant.entity.PayMerchantAccount;

import java.math.BigDecimal;

public final class ChannelDtos {
    private ChannelDtos() {
    }

    public record BarcodeChannelRequest(
            String merchantOrderNo,
            BigDecimal amount,
            String authCode,
            String subject,
            String notifyUrl
    ) {
    }

    public record PreOrderChannelRequest(
            String merchantOrderNo,
            BigDecimal amount,
            String subject,
            String notifyUrl,
            String successUrl,
            String errorUrl
    ) {
    }

    public record DecodeBarChannelRequest(
            String authCode,
            String sceneNo,
            String subAppId
    ) {
    }

    public record ScanChannelRequest(
            String merchantOrderNo,
            BigDecimal amount,
            String subject,
            String notifyUrl
    ) {
    }

    public record H5ChannelRequest(
            String merchantOrderNo,
            BigDecimal amount,
            String subject,
            String notifyUrl,
            String returnUrl
    ) {
    }

    public record QrcodeChannelRequest(
            String merchantOrderNo,
            BigDecimal amount,
            String subject,
            String channel,
            String notifyUrl
    ) {
    }

    public record WechatJsapiChannelRequest(
            String merchantOrderNo,
            BigDecimal amount,
            String subject,
            String notifyUrl,
            String subAppId,
            String subOpenId
    ) {
    }

    public record AlipayJsapiChannelRequest(
            String merchantOrderNo,
            BigDecimal amount,
            String subject,
            String notifyUrl,
            String subAppId,
            String buyerId,
            String buyerOpenId
    ) {
    }

    public record QueryChannelRequest(
            String merchantOrderNo,
            String upstreamOrderId,
            String upstreamOrderTime
    ) {
    }

    public record ChannelResponse(
            String status,
            String upstreamOrderId,
            String upstreamTradeNo,
            String upstreamOrderTime,
            String responseCode,
            String responseMessage,
            Object payData,
            String rawResponse
    ) {
    }

    public record ChannelContext(PayMerchantAccount account, String apiKey, String signKey) {
    }

    public record RefundChannelRequest(
            String merchantOrderNo,
            String upstreamOrderId,
            String upstreamOrderTime,
            String merchantRefundNo,
            BigDecimal refundAmount,
            String reason,
            String notifyUrl
    ) {
    }

    public record RefundQueryChannelRequest(
            String merchantOrderNo,
            String upstreamOrderId,
            String upstreamOrderTime,
            String merchantRefundNo
    ) {
    }
}
