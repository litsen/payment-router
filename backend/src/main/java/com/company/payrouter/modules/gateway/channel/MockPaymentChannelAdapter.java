package com.company.payrouter.modules.gateway.channel;

import com.company.payrouter.modules.gateway.channel.ChannelDtos.BarcodeChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.ChannelContext;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.ChannelResponse;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.AlipayJsapiChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.DecodeBarChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.H5ChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.PreOrderChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.QrcodeChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.QueryChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.RefundChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.RefundQueryChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.ScanChannelRequest;
import com.company.payrouter.modules.gateway.channel.ChannelDtos.WechatJsapiChannelRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "pay-router.channel", name = "adapter", havingValue = "mock", matchIfMissing = true)
public class MockPaymentChannelAdapter implements PaymentChannelAdapter {
    @Override
    public ChannelResponse barcodePay(BarcodeChannelRequest request, ChannelContext context) {
        String status = decideStatus(request.authCode());
        String upstreamOrderId = "MOCK" + System.currentTimeMillis();
        return new ChannelResponse(
                status,
                upstreamOrderId,
                "TRADE" + upstreamOrderId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "10000",
                "Mock payment " + status,
                null,
                "{\"adapter\":\"MOCK\",\"status\":\"" + status + "\",\"orderid\":\"" + upstreamOrderId + "\"}"
        );
    }

    @Override
    public ChannelResponse preOrder(PreOrderChannelRequest request, ChannelContext context) {
        String upstreamOrderId = "MOCKPRE" + System.currentTimeMillis();
        return processing(upstreamOrderId, "Mock unified cashier created", Map.of("cashierUrl", "https://mock.pay.local/cashier/" + request.merchantOrderNo()));
    }

    @Override
    public ChannelResponse decodeBar(DecodeBarChannelRequest request, ChannelContext context) {
        return new ChannelResponse(
                "SUCCESS",
                null,
                null,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "10000",
                "Mock decode bar success",
                Map.of("userId", "mock-user", "openid", "mock-openid"),
                "{\"adapter\":\"MOCK\",\"decode\":\"success\"}"
        );
    }

    @Override
    public ChannelResponse scanPay(ScanChannelRequest request, ChannelContext context) {
        String upstreamOrderId = "MOCKSCAN" + System.currentTimeMillis();
        String qrCode = "mock://scan-pay/" + request.merchantOrderNo();
        return processing(upstreamOrderId, "Mock scan payment created", Map.of("qrCode", qrCode));
    }

    @Override
    public ChannelResponse h5Pay(H5ChannelRequest request, ChannelContext context) {
        String upstreamOrderId = "MOCKH5" + System.currentTimeMillis();
        String payUrl = "https://mock.pay.local/h5/" + request.merchantOrderNo();
        return processing(upstreamOrderId, "Mock H5 payment created", Map.of("payUrl", payUrl));
    }

    @Override
    public ChannelResponse qrcodePay(QrcodeChannelRequest request, ChannelContext context) {
        String upstreamOrderId = "MOCKQRCODE" + System.currentTimeMillis();
        return processing(upstreamOrderId, "Mock qrcode payment created", Map.of("qrCode", "mock://qrcode-pay/" + request.channel() + "/" + request.merchantOrderNo()));
    }

    @Override
    public ChannelResponse wechatJsapiPay(WechatJsapiChannelRequest request, ChannelContext context) {
        String upstreamOrderId = "MOCKWXJSAPI" + System.currentTimeMillis();
        return processing(upstreamOrderId, "Mock JSAPI payment created", Map.of(
                "appId", request.subAppId() == null ? "mock-app" : request.subAppId(),
                "timeStamp", String.valueOf(System.currentTimeMillis() / 1000),
                "nonceStr", "mockNonce",
                "package", "prepay_id=" + upstreamOrderId,
                "signType", "MD5",
                "paySign", "mockPaySign"
        ));
    }

    @Override
    public ChannelResponse alipayJsapiPay(AlipayJsapiChannelRequest request, ChannelContext context) {
        String upstreamOrderId = "MOCKALIPAYJSAPI" + System.currentTimeMillis();
        return processing(upstreamOrderId, "Mock Alipay JSAPI payment created", Map.of(
                "tradeNo", "TRADE" + upstreamOrderId,
                "orderId", upstreamOrderId
        ));
    }

    @Override
    public ChannelResponse queryPay(QueryChannelRequest request, ChannelContext context) {
        return new ChannelResponse(
                "SUCCESS",
                request.upstreamOrderId(),
                null,
                request.upstreamOrderTime(),
                "10000",
                "Mock query success",
                null,
                "{\"adapter\":\"MOCK\",\"paystatus\":\"1\"}"
        );
    }

    @Override
    public ChannelResponse refund(RefundChannelRequest request, ChannelContext context) {
        return new ChannelResponse(
                "PAYING",
                request.upstreamOrderId(),
                "REFUND" + System.currentTimeMillis(),
                request.upstreamOrderTime(),
                "10000",
                "Mock refund accepted",
                Map.of("refundStatus", "PROCESSING", "merchantRefundNo", request.merchantRefundNo() == null ? "" : request.merchantRefundNo()),
                "{\"adapter\":\"MOCK\",\"refund_status\":\"0\"}"
        );
    }

    @Override
    public ChannelResponse queryRefund(RefundQueryChannelRequest request, ChannelContext context) {
        return new ChannelResponse(
                "SUCCESS",
                request.upstreamOrderId(),
                request.merchantRefundNo(),
                request.upstreamOrderTime(),
                "10000",
                "Mock refund success",
                Map.of("refundStatus", "SUCCESS", "merchantRefundNo", request.merchantRefundNo() == null ? "" : request.merchantRefundNo()),
                "{\"adapter\":\"MOCK\",\"refund_status\":\"1\"}"
        );
    }

    private ChannelResponse processing(String upstreamOrderId, String message, Object payData) {
        return new ChannelResponse(
                "PAYING",
                upstreamOrderId,
                "TRADE" + upstreamOrderId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "10000",
                message,
                payData,
                "{\"adapter\":\"MOCK\",\"status\":\"PAYING\",\"orderid\":\"" + upstreamOrderId + "\"}"
        );
    }

    private String decideStatus(String authCode) {
        if (authCode == null) {
            return "PAYING";
        }
        String normalized = authCode.toUpperCase();
        if (normalized.endsWith("1") || normalized.contains("SUCCESS")) {
            return "SUCCESS";
        }
        if (normalized.endsWith("2") || normalized.contains("FAIL")) {
            return "FAILED";
        }
        if (normalized.endsWith("3") || normalized.contains("PAYING")) {
            return "PAYING";
        }
        return "PAYING";
    }
}
