package com.company.payrouter.modules.gateway.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.payrouter.common.exception.BusinessErrorCode;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.infrastructure.crypto.AesCryptoService;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.AlipayJsapiPayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.BarcodePayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.DecodeBarRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.H5PayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.PayCreateRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.PreOrderRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.QrcodePayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.QueryPayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.RefundQueryRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.RefundRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.ScanPayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.WechatJsapiPayRequest;
import com.company.payrouter.modules.merchant.entity.PayMerchantAccount;
import com.company.payrouter.modules.merchant.entity.PayMerchantApp;
import com.company.payrouter.modules.merchant.entity.PayMerchantPool;
import com.company.payrouter.modules.merchant.mapper.PayMerchantAccountMapper;
import com.company.payrouter.modules.merchant.mapper.PayMerchantAppMapper;
import com.company.payrouter.modules.merchant.mapper.PayMerchantPoolMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class GatewaySecurityService {
    private static final Logger log = LoggerFactory.getLogger(GatewaySecurityService.class);
    private static final long TIMESTAMP_WINDOW_MILLIS = Duration.ofMinutes(5).toMillis();
    private static final int RATE_LIMIT_PER_MINUTE = 60;

    private final PayMerchantPoolMapper poolMapper;
    private final PayMerchantAccountMapper accountMapper;
    private final PayMerchantAppMapper appMapper;
    private final AesCryptoService cryptoService;
    private final GatewaySignService signService;
    private final StringRedisTemplate redisTemplate;
    private final LegacyPoolSignProperties legacyPoolSignProperties;
    private final MeterRegistry meterRegistry;

    public GatewaySecurityService(
            PayMerchantPoolMapper poolMapper,
            PayMerchantAccountMapper accountMapper,
            PayMerchantAppMapper appMapper,
            AesCryptoService cryptoService,
            GatewaySignService signService,
            StringRedisTemplate redisTemplate,
            LegacyPoolSignProperties legacyPoolSignProperties,
            MeterRegistry meterRegistry
    ) {
        this.poolMapper = poolMapper;
        this.accountMapper = accountMapper;
        this.appMapper = appMapper;
        this.cryptoService = cryptoService;
        this.signService = signService;
        this.redisTemplate = redisTemplate;
        this.legacyPoolSignProperties = legacyPoolSignProperties;
        this.meterRegistry = meterRegistry;
    }

    public PayMerchantPool verifyBarcodeRequest(BarcodePayRequest request) {
        return verifyPayRequest(request, toSignParams(request));
    }

    public PayMerchantPool verifyPreOrderRequest(PreOrderRequest request) {
        Map<String, Object> params = toSignParams(request);
        if (request.successUrl() != null) params.put("successUrl", request.successUrl());
        if (request.errorUrl() != null) params.put("errorUrl", request.errorUrl());
        return verifyPayRequest(request, params);
    }

    public PayMerchantPool verifyDecodeBarRequest(DecodeBarRequest request) {
        VerifiedApp verifiedApp = requireEnabledApp(request.appId());
        verifyTimestamp(request.timestamp());
        verifyRateLimit(request.appId(), verifiedApp);
        verifyNonce(request.appId(), request.nonce());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("appId", request.appId());
        params.put("authCode", request.authCode());
        if (request.sceneNo() != null) params.put("sceneNo", request.sceneNo());
        if (request.subAppId() != null) params.put("subAppId", request.subAppId());
        params.put("timestamp", request.timestamp());
        params.put("nonce", request.nonce());
        verifySign(verifiedApp, request.sign(), params, "DECODE_BAR");
        return verifiedApp.pool();
    }

    public PayMerchantPool verifyScanRequest(ScanPayRequest request) {
        return verifyPayRequest(request, toSignParams(request));
    }

    public PayMerchantPool verifyH5Request(H5PayRequest request) {
        Map<String, Object> params = toSignParams(request);
        if (request.returnUrl() != null) {
            params.put("returnUrl", request.returnUrl());
        }
        return verifyPayRequest(request, params);
    }

    public PayMerchantPool verifyQrcodeRequest(QrcodePayRequest request) {
        Map<String, Object> params = toSignParams(request);
        params.put("channel", request.channel());
        return verifyPayRequest(request, params);
    }

    public PayMerchantPool verifyWechatJsapiRequest(WechatJsapiPayRequest request) {
        Map<String, Object> params = toSignParams(request);
        params.put("subAppId", request.subAppId());
        params.put("subOpenId", request.subOpenId());
        return verifyPayRequest(request, params);
    }

    public PayMerchantPool verifyAlipayJsapiRequest(AlipayJsapiPayRequest request) {
        Map<String, Object> params = toSignParams(request);
        if (request.subAppId() != null) {
            params.put("subAppId", request.subAppId());
        }
        if (request.buyerId() != null) params.put("buyerId", request.buyerId());
        if (request.buyerOpenId() != null) params.put("buyerOpenId", request.buyerOpenId());
        return verifyPayRequest(request, params);
    }

    public PayMerchantPool verifyRefundRequest(RefundRequest request) {
        VerifiedApp verifiedApp = requireEnabledApp(request.appId());
        verifyTimestamp(request.timestamp());
        verifyRateLimit(request.appId(), verifiedApp);
        verifyNonce(request.appId(), request.nonce());
        verifyNotifyUrl(request.notifyUrl(), verifiedApp);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("appId", request.appId());
        params.put("merchantOrderNo", request.merchantOrderNo());
        if (request.merchantRefundNo() != null) params.put("merchantRefundNo", request.merchantRefundNo());
        params.put("refundAmount", request.refundAmount());
        if (request.reason() != null) params.put("reason", request.reason());
        if (request.notifyUrl() != null) params.put("notifyUrl", request.notifyUrl());
        params.put("timestamp", request.timestamp());
        params.put("nonce", request.nonce());
        verifySign(verifiedApp, request.sign(), params, "REFUND");
        return verifiedApp.pool();
    }

    public PayMerchantPool verifyRefundQueryRequest(RefundQueryRequest request) {
        VerifiedApp verifiedApp = requireEnabledApp(request.appId());
        verifyTimestamp(request.timestamp());
        verifyRateLimit(request.appId(), verifiedApp);
        verifyNonce(request.appId(), request.nonce());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("appId", request.appId());
        params.put("merchantOrderNo", request.merchantOrderNo());
        if (request.merchantRefundNo() != null) params.put("merchantRefundNo", request.merchantRefundNo());
        params.put("timestamp", request.timestamp());
        params.put("nonce", request.nonce());
        verifySign(verifiedApp, request.sign(), params, "REFUND_QUERY");
        return verifiedApp.pool();
    }

    private PayMerchantPool verifyPayRequest(PayCreateRequest request, Map<String, Object> params) {
        VerifiedApp verifiedApp = requireEnabledApp(request.appId());
        verifyTimestamp(request.timestamp());
        verifyRateLimit(request.appId(), verifiedApp);
        verifyNonce(request.appId(), request.nonce());
        verifyNotifyUrl(request.notifyUrl(), verifiedApp);
        verifySign(verifiedApp, request.sign(), params, requestType(request));
        return verifiedApp.pool();
    }

    private Map<String, Object> toSignParams(PayCreateRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("appId", request.appId());
        params.put("merchantOrderNo", request.merchantOrderNo());
        params.put("amount", request.amount());
        if (request instanceof BarcodePayRequest barcodeRequest) {
            params.put("authCode", barcodeRequest.authCode());
        }
        params.put("subject", request.subject());
        if (request.notifyUrl() != null) {
            params.put("notifyUrl", request.notifyUrl());
        }
        params.put("timestamp", request.timestamp());
        params.put("nonce", request.nonce());
        return params;
    }

    public PayMerchantPool verifyQueryRequest(QueryPayRequest request) {
        VerifiedApp verifiedApp = requireEnabledApp(request.appId());
        verifyTimestamp(request.timestamp());
        verifyRateLimit(request.appId(), verifiedApp);
        verifyNonce(request.appId(), request.nonce());
        verifySign(verifiedApp, request.sign(), Map.of(
                "appId", request.appId(),
                "merchantOrderNo", request.merchantOrderNo(),
                "timestamp", request.timestamp(),
                "nonce", request.nonce()
        ), "QUERY_PAY");
        return verifiedApp.pool();
    }

    private VerifiedApp requireEnabledApp(String appId) {
        PayMerchantApp app = appMapper.selectOne(new LambdaQueryWrapper<PayMerchantApp>().eq(PayMerchantApp::getAppId, appId));
        if (app != null) {
            PayMerchantPool pool = poolMapper.selectById(app.getPoolId());
            if (!"ENABLED".equals(app.getStatus()) || pool == null || !"ENABLED".equals(pool.getStatus())) {
                throw new BizException(BusinessErrorCode.INVALID_APP_ID);
            }
            return new VerifiedApp(app, pool);
        }
        PayMerchantPool pool = poolMapper.selectOne(new LambdaQueryWrapper<PayMerchantPool>().eq(PayMerchantPool::getPoolCode, appId));
        if (pool == null || !"ENABLED".equals(pool.getStatus())) {
            throw new BizException(BusinessErrorCode.INVALID_APP_ID);
        }
        if (!legacyPoolSignProperties.allows(pool.getPoolCode())) {
            recordSignVerify("LEGACY_POOL_SIGN_KEY", appId, pool, null, "UNKNOWN", "FAILED", "LEGACY_DISABLED");
            throw new BizException(BusinessErrorCode.INVALID_APP_ID);
        }
        return new VerifiedApp(null, pool);
    }

    private void verifyTimestamp(Long timestamp) {
        long diff = Math.abs(System.currentTimeMillis() - timestamp);
        if (diff > TIMESTAMP_WINDOW_MILLIS) {
            throw new BizException(BusinessErrorCode.REQUEST_EXPIRED);
        }
    }

    private void verifyRateLimit(String appId, VerifiedApp verifiedApp) {
        int limit = verifiedApp.app() == null || verifiedApp.app().getRateLimitPerMinute() == null
                ? RATE_LIMIT_PER_MINUTE
                : verifiedApp.app().getRateLimitPerMinute();
        String key = "pay:api:rate:" + appId + ":" + (System.currentTimeMillis() / 60000);
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 2, TimeUnit.MINUTES);
        if (count != null && count > limit) {
            throw new BizException(BusinessErrorCode.TOO_MANY_REQUESTS, "Too many payment requests");
        }
    }

    private void verifyNonce(String appId, String nonce) {
        String key = "pay:api:nonce:" + appId + ":" + nonce;
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1", TIMESTAMP_WINDOW_MILLIS, TimeUnit.MILLISECONDS);
        if (!Boolean.TRUE.equals(ok)) {
            throw new BizException(BusinessErrorCode.DUPLICATE_NONCE);
        }
    }

    private void verifySign(VerifiedApp verifiedApp, String sign, Map<String, ?> params, String requestType) {
        if (verifiedApp.app() != null) {
            String appSecret = cryptoService.decrypt(verifiedApp.app().getSecretEncrypted());
            if (StringUtils.hasText(appSecret) && signService.verify(params, appSecret, sign)) {
                String authPath = verifiedApp.app().getAppId().equals(verifiedApp.pool().getPoolCode()) ? "DEFAULT_POOL_APP_SECRET" : "APP_SECRET";
                recordSignVerify(authPath, verifiedApp.app().getAppId(), verifiedApp.pool(), null, requestType, "SUCCESS", "SIGN_MATCH");
                return;
            }
            if (!verifiedApp.app().getAppId().equals(verifiedApp.pool().getPoolCode())) {
                recordSignVerify("APP_SECRET", verifiedApp.app().getAppId(), verifiedApp.pool(), null, requestType, "FAILED", "SIGN_MISMATCH");
                throw new BizException(BusinessErrorCode.INVALID_SIGN);
            }
            if (!legacyPoolSignProperties.allowsDefaultAppFallback(verifiedApp.pool().getPoolCode())) {
                recordSignVerify("LEGACY_POOL_SIGN_KEY_FALLBACK", verifiedApp.app().getAppId(), verifiedApp.pool(), null, requestType, "FAILED", "DEFAULT_APP_SECRET_FAILED_FALLBACK_DISABLED");
                throw new BizException(BusinessErrorCode.INVALID_SIGN);
            }
        }
        verifyLegacyPoolSign(verifiedApp, sign, params, requestType);
    }

    private void verifyLegacyPoolSign(VerifiedApp verifiedApp, String sign, Map<String, ?> params, String requestType) {
        boolean defaultAppFallback = verifiedApp.app() != null;
        String authPath = defaultAppFallback ? "LEGACY_POOL_SIGN_KEY_FALLBACK" : "LEGACY_POOL_SIGN_KEY";
        if (!defaultAppFallback && !legacyPoolSignProperties.allows(verifiedApp.pool().getPoolCode())) {
            recordSignVerify(authPath, paramAppId(params), verifiedApp.pool(), null, requestType, "FAILED", "LEGACY_DISABLED");
            throw new BizException(BusinessErrorCode.INVALID_SIGN);
        }
        List<PayMerchantAccount> accounts = accountMapper.selectList(new LambdaQueryWrapper<PayMerchantAccount>()
                .eq(PayMerchantAccount::getPoolId, verifiedApp.pool().getId())
                .eq(PayMerchantAccount::getStatus, "ENABLED"));
        for (PayMerchantAccount account : accounts) {
            String signKey = cryptoService.decrypt(account.getSignKeyEncrypted());
            if (StringUtils.hasText(signKey) && signService.verify(params, signKey, sign)) {
                String reason = legacyPoolSignProperties.wouldReject(verifiedApp.pool().getPoolCode(), defaultAppFallback)
                        ? "WARN_ONLY_WOULD_REJECT"
                        : "SIGN_MATCH";
                recordSignVerify(authPath, paramAppId(params), verifiedApp.pool(), account.getId(), requestType, "SUCCESS", reason);
                return;
            }
        }
        recordSignVerify(authPath, paramAppId(params), verifiedApp.pool(), null, requestType, "FAILED", "SIGN_MISMATCH");
        throw new BizException(BusinessErrorCode.INVALID_SIGN);
    }

    private String requestType(PayCreateRequest request) {
        if (request instanceof BarcodePayRequest) return "BARCODE_PAY";
        if (request instanceof PreOrderRequest) return "PRE_ORDER";
        if (request instanceof ScanPayRequest) return "SCAN_PAY";
        if (request instanceof QrcodePayRequest) return "QRCODE_PAY";
        if (request instanceof H5PayRequest) return "H5_PAY";
        if (request instanceof WechatJsapiPayRequest) return "WECHAT_JSAPI_PAY";
        if (request instanceof AlipayJsapiPayRequest) return "ALIPAY_JSAPI_PAY";
        return "PAY_CREATE";
    }

    private String paramAppId(Map<String, ?> params) {
        Object appId = params.get("appId");
        return appId == null ? "" : String.valueOf(appId);
    }

    private void recordSignVerify(String authPath, String appId, PayMerchantPool pool, Long merchantAccountId, String requestType, String result, String reason) {
        log.info(
                "event=payment_gateway_sign_verify authPath={} appId={} poolId={} poolCode={} merchantAccountId={} requestType={} result={} reason={}",
                authPath,
                appId,
                pool == null ? null : pool.getId(),
                pool == null ? null : pool.getPoolCode(),
                merchantAccountId,
                requestType,
                result,
                reason
        );
        meterRegistry.counter("payment.gateway.sign.verify.total", "authPath", authPath, "result", result, "requestType", requestType).increment();
        if (authPath.startsWith("LEGACY_POOL_SIGN_KEY") && pool != null) {
            String name = "SUCCESS".equals(result) ? "payment.gateway.sign.legacy.hit.total" : "payment.gateway.sign.legacy.invalid.total";
            meterRegistry.counter(name, "poolCode", pool.getPoolCode(), "requestType", requestType).increment();
        }
        if ("DEFAULT_POOL_APP_SECRET".equals(authPath) && pool != null) {
            meterRegistry.counter("payment.gateway.sign.default_pool_app.hit.total", "poolCode", pool.getPoolCode(), "requestType", requestType).increment();
        }
    }

    private void verifyNotifyUrl(String notifyUrl, VerifiedApp verifiedApp) {
        if (verifiedApp.app() == null || !StringUtils.hasText(verifiedApp.app().getNotifyUrlWhitelist()) || !StringUtils.hasText(notifyUrl)) {
            return;
        }
        boolean allowed = java.util.Arrays.stream(verifiedApp.app().getNotifyUrlWhitelist().split("[,\\n\\r]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .anyMatch(notifyUrl::startsWith);
        if (!allowed) {
            throw new BizException(BusinessErrorCode.INVALID_REQUEST_PARAMETER, "Notify url is not allowed for this app");
        }
    }

    private record VerifiedApp(PayMerchantApp app, PayMerchantPool pool) {
    }

    public Map<String, Object> toParamMap(BarcodePayRequest request) {
        Map<String, Object> values = toParamMap((PayCreateRequest) request);
        values.put("authCode", mask(request.authCode()));
        return values;
    }

    public Map<String, Object> toParamMap(PreOrderRequest request) {
        Map<String, Object> values = toParamMap((PayCreateRequest) request);
        values.put("successUrl", request.successUrl());
        values.put("errorUrl", request.errorUrl());
        return values;
    }

    public Map<String, Object> toParamMap(DecodeBarRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("appId", request.appId());
        values.put("authCode", mask(request.authCode()));
        values.put("sceneNo", request.sceneNo());
        values.put("subAppId", request.subAppId());
        values.put("timestamp", request.timestamp());
        values.put("nonce", request.nonce());
        values.put("sign", "****");
        return values;
    }

    public Map<String, Object> toParamMap(ScanPayRequest request) {
        return toParamMap((PayCreateRequest) request);
    }

    public Map<String, Object> toParamMap(H5PayRequest request) {
        Map<String, Object> values = toParamMap((PayCreateRequest) request);
        values.put("returnUrl", request.returnUrl());
        return values;
    }

    public Map<String, Object> toParamMap(QrcodePayRequest request) {
        Map<String, Object> values = toParamMap((PayCreateRequest) request);
        values.put("channel", request.channel());
        return values;
    }

    public Map<String, Object> toParamMap(WechatJsapiPayRequest request) {
        Map<String, Object> values = toParamMap((PayCreateRequest) request);
        values.put("subAppId", request.subAppId());
        values.put("subOpenId", mask(request.subOpenId()));
        return values;
    }

    public Map<String, Object> toParamMap(AlipayJsapiPayRequest request) {
        Map<String, Object> values = toParamMap((PayCreateRequest) request);
        values.put("subAppId", request.subAppId());
        values.put("buyerId", mask(request.buyerId()));
        values.put("buyerOpenId", mask(request.buyerOpenId()));
        return values;
    }

    public Map<String, Object> toParamMap(RefundRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("appId", request.appId());
        values.put("merchantOrderNo", request.merchantOrderNo());
        values.put("merchantRefundNo", request.merchantRefundNo());
        values.put("refundAmount", request.refundAmount());
        values.put("reason", request.reason());
        values.put("notifyUrl", request.notifyUrl());
        values.put("timestamp", request.timestamp());
        values.put("nonce", request.nonce());
        values.put("sign", "****");
        return values;
    }

    public Map<String, Object> toParamMap(RefundQueryRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("appId", request.appId());
        values.put("merchantOrderNo", request.merchantOrderNo());
        values.put("merchantRefundNo", request.merchantRefundNo());
        values.put("timestamp", request.timestamp());
        values.put("nonce", request.nonce());
        values.put("sign", "****");
        return values;
    }

    private Map<String, Object> toParamMap(PayCreateRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("appId", request.appId());
        values.put("merchantOrderNo", request.merchantOrderNo());
        values.put("amount", request.amount());
        values.put("subject", request.subject());
        values.put("notifyUrl", request.notifyUrl());
        values.put("timestamp", request.timestamp());
        values.put("nonce", request.nonce());
        values.put("sign", "****");
        return values;
    }

    public Map<String, Object> toParamMap(QueryPayRequest request) {
        return Map.of(
                "appId", request.appId(),
                "merchantOrderNo", request.merchantOrderNo(),
                "timestamp", request.timestamp(),
                "nonce", request.nonce(),
                "sign", "****"
        );
    }

    public String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
