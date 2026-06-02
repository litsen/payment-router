package com.company.payrouter.modules.gateway.channel;

import com.company.payrouter.common.exception.BusinessErrorCode;
import com.company.payrouter.common.exception.BizException;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "pay-router.channel", name = "adapter", havingValue = "lfwin")
public class LfwinPaymentChannelAdapter implements PaymentChannelAdapter {
    private final LfwinSignHelper signHelper;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;

    public LfwinPaymentChannelAdapter(
            LfwinSignHelper signHelper,
            ObjectMapper objectMapper,
            @Value("${pay-router.channel.lfwin.base-url:${PAY_ROUTER_LFWIN_BASE_URL:https://api2.lfwin.com}}") String baseUrl,
            @Value("${pay-router.channel.lfwin.timeout-seconds:${PAY_ROUTER_LFWIN_TIMEOUT_SECONDS:15}}") int timeoutSeconds
    ) {
        this.signHelper = signHelper;
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Override
    public ChannelResponse preOrder(PreOrderChannelRequest request, ChannelContext context) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("apikey", context.apiKey());
        params.put("money", money(request.amount()));
        params.put("mch_orderid", request.merchantOrderNo());
        params.put("nonce_str", nonce());
        if (StringUtils.hasText(request.notifyUrl())) params.put("notify_url", request.notifyUrl());
        if (StringUtils.hasText(request.successUrl())) params.put("succ_url", request.successUrl());
        if (StringUtils.hasText(request.errorUrl())) params.put("err_url", request.errorUrl());
        return call("/index/Payment/pre_order", params, context.signKey());
    }

    @Override
    public ChannelResponse decodeBar(DecodeBarChannelRequest request, ChannelContext context) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "pay.alipay.decode_bar");
        params.put("apikey", context.apiKey());
        params.put("dynamic_id", request.authCode());
        params.put("nonce_str", nonce());
        if (StringUtils.hasText(request.sceneNo())) params.put("sence_no", request.sceneNo());
        if (StringUtils.hasText(request.subAppId())) params.put("sub_appid", request.subAppId());
        return call("/payapi/pay/decode_bar", params, context.signKey());
    }

    @Override
    public ChannelResponse barcodePay(BarcodeChannelRequest request, ChannelContext context) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "pay.comm.barcode");
        params.put("apikey", context.apiKey());
        params.put("money", money(request.amount()));
        params.put("dynamic_id", request.authCode());
        params.put("mch_orderid", request.merchantOrderNo());
        params.put("nonce_str", nonce());
        params.put("good_name", request.subject());
        if (StringUtils.hasText(request.notifyUrl())) {
            params.put("notify_url", request.notifyUrl());
        }
        return call("/payapi/pay/barcode", params, context.signKey());
    }

    @Override
    public ChannelResponse scanPay(ScanChannelRequest request, ChannelContext context) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "pay.comm.jspay");
        params.put("apikey", context.apiKey());
        params.put("money", money(request.amount()));
        params.put("mch_orderid", request.merchantOrderNo());
        params.put("nonce_str", nonce());
        params.put("remarks", request.subject());
        if (StringUtils.hasText(request.notifyUrl())) {
            params.put("notify_url", request.notifyUrl());
        }
        return call("/payapi/trans/kxpay", params, context.signKey());
    }

    @Override
    public ChannelResponse h5Pay(H5ChannelRequest request, ChannelContext context) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "wxpay.comm.jspay");
        params.put("apikey", context.apiKey());
        params.put("money", money(request.amount()));
        params.put("mch_orderid", request.merchantOrderNo());
        params.put("nonce_str", nonce());
        params.put("remarks", request.subject());
        if (StringUtils.hasText(request.notifyUrl())) {
            params.put("notify_url", request.notifyUrl());
        }
        if (StringUtils.hasText(request.returnUrl())) {
            params.put("succ_url", request.returnUrl());
        }
        return call("/payapi/pay/jspay3", params, context.signKey());
    }

    @Override
    public ChannelResponse qrcodePay(QrcodeChannelRequest request, ChannelContext context) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", qrcodeService(request.channel()));
        params.put("apikey", context.apiKey());
        params.put("money", money(request.amount()));
        params.put("mch_orderid", request.merchantOrderNo());
        params.put("nonce_str", nonce());
        if (StringUtils.hasText(request.notifyUrl())) params.put("notify_url", request.notifyUrl());
        return call("/payapi/pay/qrcode", params, context.signKey());
    }

    @Override
    public ChannelResponse wechatJsapiPay(WechatJsapiChannelRequest request, ChannelContext context) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "comm.js.pay");
        params.put("apikey", context.apiKey());
        params.put("money", money(request.amount()));
        params.put("mch_orderid", request.merchantOrderNo());
        params.put("nonce_str", nonce());
        params.put("sub_appid", request.subAppId());
        params.put("sub_openid", request.subOpenId());
        params.put("good_name", request.subject());
        if (StringUtils.hasText(request.notifyUrl())) {
            params.put("notify_url", request.notifyUrl());
        }
        return call("/payapi/mini/wxpay", params, context.signKey());
    }

    @Override
    public ChannelResponse alipayJsapiPay(AlipayJsapiChannelRequest request, ChannelContext context) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "comm.js.pay");
        params.put("apikey", context.apiKey());
        params.put("money", money(request.amount()));
        params.put("mch_orderid", request.merchantOrderNo());
        params.put("nonce_str", nonce());
        params.put("remarks", request.subject());
        if (StringUtils.hasText(request.subAppId())) params.put("sub_appid", request.subAppId());
        if (StringUtils.hasText(request.buyerId())) params.put("buyer_id", request.buyerId());
        if (StringUtils.hasText(request.buyerOpenId())) params.put("buyer_open_id", request.buyerOpenId());
        if (StringUtils.hasText(request.notifyUrl())) params.put("notify_url", request.notifyUrl());
        return call("/payapi/trade/alipay", params, context.signKey());
    }

    @Override
    public ChannelResponse queryPay(QueryChannelRequest request, ChannelContext context) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "pay.comm.query_order");
        params.put("apikey", context.apiKey());
        params.put("nonce_str", nonce());
        if (StringUtils.hasText(request.upstreamOrderId())) {
            params.put("orderid", request.upstreamOrderId());
        } else {
            params.put("mch_orderid", request.merchantOrderNo());
            if (StringUtils.hasText(request.upstreamOrderTime())) {
                params.put("order_time", request.upstreamOrderTime());
            }
        }
        return call("/payapi/pay/query_order", params, context.signKey());
    }

    @Override
    public ChannelResponse refund(RefundChannelRequest request, ChannelContext context) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "pay.comm.refund_order");
        params.put("apikey", context.apiKey());
        params.put("nonce_str", nonce());
        params.put("refundmoney", money(request.refundAmount()));
        params.put("version", "4.0");
        if (StringUtils.hasText(request.upstreamOrderId())) {
            params.put("orderid", request.upstreamOrderId());
        } else {
            params.put("mch_orderid", request.merchantOrderNo());
            if (StringUtils.hasText(request.upstreamOrderTime())) params.put("order_time", request.upstreamOrderTime());
        }
        if (StringUtils.hasText(request.merchantRefundNo())) params.put("mch_refund_no", request.merchantRefundNo());
        if (StringUtils.hasText(request.reason())) params.put("reason", request.reason());
        if (StringUtils.hasText(request.notifyUrl())) params.put("notify_url", request.notifyUrl());
        return call("/payapi/pay/refund_order", params, context.signKey());
    }

    @Override
    public ChannelResponse queryRefund(RefundQueryChannelRequest request, ChannelContext context) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "pay.comm.query_refund");
        params.put("apikey", context.apiKey());
        params.put("nonce_str", nonce());
        params.put("version", "4.0");
        if (StringUtils.hasText(request.upstreamOrderId())) {
            params.put("orderid", request.upstreamOrderId());
        } else {
            params.put("mch_orderid", request.merchantOrderNo());
            if (StringUtils.hasText(request.upstreamOrderTime())) params.put("order_time", request.upstreamOrderTime());
        }
        if (StringUtils.hasText(request.merchantRefundNo())) params.put("mch_refund_no", request.merchantRefundNo());
        return call("/payapi/pay/query_refund", params, context.signKey());
    }

    private ChannelResponse call(String path, Map<String, String> params, String signKey) {
        if (!StringUtils.hasText(signKey)) {
            throw new BizException(BusinessErrorCode.CHANNEL_ERROR, "LFWin signKey is not configured");
        }
        params.put("sign", signHelper.md5Sign(params, signKey));
        String responseBody = postForm(path, params);
        Map<String, String> values = parseResponse(responseBody);
        boolean signVerified = verifyResponse(values, signKey, responseBody);
        values.put("_signVerified", String.valueOf(signVerified));
        return toChannelResponse(values, responseBody);
    }

    private String postForm(String path, Map<String, String> params) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody(params), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(BusinessErrorCode.CHANNEL_ERROR, "LFWin HTTP request failed: " + response.statusCode());
            }
            return response.body();
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(BusinessErrorCode.CHANNEL_ERROR, "LFWin HTTP request failed: " + exception.getMessage());
        }
    }

    private Map<String, String> parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            Map<String, Object> values = objectMapper.convertValue(root, new TypeReference<>() {
            });
            Map<String, String> flat = values.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() == null ? "" : String.valueOf(entry.getValue()), (left, right) -> right, LinkedHashMap::new));
            JsonNode data = root.path("data");
            if (data.isObject()) {
                flat.remove("data");
                data.fields().forEachRemaining(entry -> flat.putIfAbsent(entry.getKey(), entry.getValue().isValueNode() ? entry.getValue().asText() : entry.getValue().toString()));
            } else if (data.isTextual()) {
                flat.putIfAbsent("data", data.asText());
            }
            return flat;
        } catch (Exception ignored) {
            Map<String, String> values = new LinkedHashMap<>();
            for (String part : responseBody.split("&")) {
                int index = part.indexOf('=');
                if (index > 0) {
                    values.put(part.substring(0, index), part.substring(index + 1));
                }
            }
            if (values.isEmpty()) {
                throw new BizException(BusinessErrorCode.CHANNEL_ERROR, "LFWin response is not JSON or form data");
            }
            return values;
        }
    }

    private boolean verifyResponse(Map<String, String> values, String signKey, String rawResponse) {
        String status = values.get("status");
        if (!"10000".equals(status) || !StringUtils.hasText(values.get("sign"))) {
            return true;
        }
        boolean verified = signHelper.verifyMd5(values, signKey);
        if (!verified && !hasPaymentPayload(values)) {
            throw new ChannelException("LFWin response sign verification failed", rawResponse);
        }
        return verified;
    }

    private ChannelResponse toChannelResponse(Map<String, String> values, String rawResponse) {
        String status = values.get("status");
        String payStatus = values.get("paystatus");
        String normalized = "PAYING";
        if ("10000".equals(status) && "1".equals(payStatus)) {
            normalized = "SUCCESS";
        } else if ("10000".equals(status) && "2".equals(payStatus)) {
            normalized = "FAILED";
        } else if (!"10000".equals(status)) {
            normalized = "FAILED";
        }
        return new ChannelResponse(
                normalized,
                firstText(values, "orderid", "order_id"),
                firstText(values, "trade_no", "out_trade_no", "dis_name"),
                firstText(values, "order_time", "ordertime"),
                status,
                firstText(values, "message", "msg", "errmsg", "return_msg"),
                payData(values),
                rawResponse
        );
    }

    private Object payData(Map<String, String> values) {
        Map<String, String> data = new LinkedHashMap<>();
        copyAs(values, data, "pay_url", "payUrl");
        copyIfPresent(values, data, "pay_url");
        copyIfPresent(values, data, "url");
        copyIfPresent(values, data, "qr_code");
        copyIfPresent(values, data, "code_url");
        copyIfPresent(values, data, "appId");
        copyIfPresent(values, data, "timeStamp");
        copyIfPresent(values, data, "nonceStr");
        copyIfPresent(values, data, "signType");
        copyIfPresent(values, data, "package");
        copyIfPresent(values, data, "paySign");
        copyIfPresent(values, data, "data");
        copyIfPresent(values, data, "user_id");
        copyIfPresent(values, data, "openid");
        copyIfPresent(values, data, "sub_openid");
        copyIfPresent(values, data, "refund_status");
        copyIfPresent(values, data, "refund_orderid");
        if ("false".equals(values.get("_signVerified"))) {
            data.put("signVerified", "false");
        }
        return data.isEmpty() ? null : data;
    }

    private boolean hasPaymentPayload(Map<String, String> values) {
        return StringUtils.hasText(values.get("pay_url"))
                || StringUtils.hasText(values.get("url"))
                || StringUtils.hasText(values.get("qr_code"))
                || StringUtils.hasText(values.get("code_url"))
                || StringUtils.hasText(values.get("appId"))
                || StringUtils.hasText(values.get("package"))
                || StringUtils.hasText(values.get("paySign"))
                || StringUtils.hasText(values.get("trade_no"))
                || StringUtils.hasText(values.get("orderid"));
    }

    private String qrcodeService(String channel) {
        String normalized = channel == null ? "" : channel.trim().toUpperCase();
        return switch (normalized) {
            case "ALIPAY" -> "pay.alipay.qrcode";
            case "UNIONPAY", "UNPAY" -> "pay.unpay.qrcode";
            default -> "pay.wxpay.qrcode";
        };
    }

    private void copyIfPresent(Map<String, String> source, Map<String, String> target, String key) {
        if (StringUtils.hasText(source.get(key))) {
            target.put(key, source.get(key));
        }
    }

    private void copyAs(Map<String, String> source, Map<String, String> target, String sourceKey, String targetKey) {
        if (StringUtils.hasText(source.get(sourceKey))) {
            target.put(targetKey, source.get(sourceKey));
        }
    }

    private String firstText(Map<String, String> values, String... keys) {
        for (String key : keys) {
            if (StringUtils.hasText(values.get(key))) {
                return values.get(key);
            }
        }
        return null;
    }

    private String formBody(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String money(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private String nonce() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
