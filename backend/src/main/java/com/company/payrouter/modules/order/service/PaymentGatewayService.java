package com.company.payrouter.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.payrouter.common.exception.BusinessErrorCode;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.infrastructure.crypto.AesCryptoService;
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
import com.company.payrouter.modules.gateway.channel.ChannelException;
import com.company.payrouter.modules.gateway.channel.LfwinSignHelper;
import com.company.payrouter.modules.gateway.channel.PaymentChannelAdapter;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.AlipayJsapiPayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.BarcodePayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.DecodeBarRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.DecodeBarResponse;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.H5PayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.PayCreateRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.PayResponse;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.PreOrderRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.QrcodePayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.QueryPayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.RefundQueryRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.RefundRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.RefundResponse;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.ScanPayRequest;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.WechatJsapiPayRequest;
import com.company.payrouter.modules.gateway.service.GatewaySecurityService;
import com.company.payrouter.modules.gateway.service.GatewaySignService;
import com.company.payrouter.modules.merchant.entity.PayMerchantAccount;
import com.company.payrouter.modules.merchant.entity.PayMerchantApp;
import com.company.payrouter.modules.merchant.entity.PayMerchantPool;
import com.company.payrouter.modules.merchant.mapper.PayMerchantAccountMapper;
import com.company.payrouter.modules.merchant.mapper.PayMerchantAppMapper;
import com.company.payrouter.modules.merchant.service.MerchantPoolService;
import com.company.payrouter.modules.order.entity.PayOrder;
import com.company.payrouter.modules.order.entity.PayRefundOrder;
import com.company.payrouter.modules.order.mapper.PayOrderMapper;
import com.company.payrouter.modules.order.mapper.PayRefundOrderMapper;
import com.company.payrouter.modules.paymethod.service.PayMethodService;
import com.company.payrouter.modules.route.dto.RouteDtos.RoutePayTestRequest;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteQueryTestRequest;
import com.company.payrouter.modules.route.service.LimitReservationService;
import com.company.payrouter.modules.route.service.RouteEngineService;
import com.company.payrouter.modules.route.service.RouteEngineService.RouteDecision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentGatewayService {
    private static final String STATUS_INIT = "INIT";
    private static final String STATUS_ROUTING = "ROUTING";
    private static final String STATUS_PAYING = "PAYING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final DateTimeFormatter LFWIN_ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PayOrderMapper orderMapper;
    private final PayMerchantAccountMapper accountMapper;
    private final PayMerchantAppMapper appMapper;
    private final GatewaySecurityService securityService;
    private final PayApiLogService apiLogService;
    private final PayOrderNotifyLogService notifyLogService;
    private final RouteEngineService routeEngineService;
    private final PaymentChannelAdapter channelAdapter;
    private final AesCryptoService cryptoService;
    private final LfwinSignHelper lfwinSignHelper;
    private final MerchantPoolService poolService;
    private final GatewaySignService signService;
    private final PayRefundOrderMapper refundOrderMapper;
    private final LimitReservationService limitReservationService;

    public PaymentGatewayService(
            PayOrderMapper orderMapper,
            PayMerchantAccountMapper accountMapper,
            PayMerchantAppMapper appMapper,
            GatewaySecurityService securityService,
            PayApiLogService apiLogService,
            PayOrderNotifyLogService notifyLogService,
            RouteEngineService routeEngineService,
            PaymentChannelAdapter channelAdapter,
            AesCryptoService cryptoService,
            LfwinSignHelper lfwinSignHelper,
            MerchantPoolService poolService,
            GatewaySignService signService,
            PayRefundOrderMapper refundOrderMapper,
            LimitReservationService limitReservationService
    ) {
        this.orderMapper = orderMapper;
        this.accountMapper = accountMapper;
        this.appMapper = appMapper;
        this.securityService = securityService;
        this.apiLogService = apiLogService;
        this.notifyLogService = notifyLogService;
        this.routeEngineService = routeEngineService;
        this.channelAdapter = channelAdapter;
        this.cryptoService = cryptoService;
        this.lfwinSignHelper = lfwinSignHelper;
        this.poolService = poolService;
        this.signService = signService;
        this.refundOrderMapper = refundOrderMapper;
        this.limitReservationService = limitReservationService;
    }

    @Transactional
    public PayResponse debugBarcodePay(RoutePayTestRequest request) {
        PayMerchantPool pool = poolService.requirePool(request.poolId());
        long timestamp = System.currentTimeMillis();
        String nonce = "DEBUG" + timestamp;
        Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("appId", pool.getPoolCode());
        params.put("merchantOrderNo", request.merchantOrderNo());
        params.put("amount", request.amount());
        if (PayMethodService.BARCODE_PAY.equals(request.payMethod())) {
            params.put("authCode", request.authCode());
        }
        params.put("subject", request.subject());
        if (request.notifyUrl() != null) {
            params.put("notifyUrl", request.notifyUrl());
        }
        if (PayMethodService.PRE_ORDER.equals(request.payMethod()) && request.returnUrl() != null) {
            params.put("successUrl", request.returnUrl());
        }
        if (PayMethodService.H5_PAY.equals(request.payMethod()) && request.returnUrl() != null) {
            params.put("returnUrl", request.returnUrl());
        }
        if (PayMethodService.WECHAT_JSAPI_PAY.equals(request.payMethod())) {
            if (request.subAppId() != null) {
                params.put("subAppId", request.subAppId());
            }
            params.put("subOpenId", request.payerId());
        }
        if (PayMethodService.ALIPAY_JSAPI_PAY.equals(request.payMethod())) {
            if (request.subAppId() != null) params.put("subAppId", request.subAppId());
            params.put("buyerId", request.payerId());
        }
        if (PayMethodService.QRCODE_PAY.equals(request.payMethod()) && request.channel() != null) {
            params.put("channel", request.channel());
        }
        params.put("timestamp", timestamp);
        params.put("nonce", nonce);
        String signKey = debugSignKey(pool.getId(), request.payMethod());
        String sign = signService.sha256Sign(params, signKey);
        return switch (request.payMethod()) {
            case PayMethodService.BARCODE_PAY -> barcodePay(new BarcodePayRequest(pool.getPoolCode(), request.merchantOrderNo(), request.amount(), request.authCode(), request.subject(), request.notifyUrl(), timestamp, nonce, sign));
            case PayMethodService.PRE_ORDER -> preOrder(new PreOrderRequest(pool.getPoolCode(), request.merchantOrderNo(), request.amount(), request.subject(), request.notifyUrl(), request.returnUrl(), null, timestamp, nonce, sign));
            case PayMethodService.SCAN_PAY -> scanPay(new ScanPayRequest(pool.getPoolCode(), request.merchantOrderNo(), request.amount(), request.subject(), request.notifyUrl(), timestamp, nonce, sign));
            case PayMethodService.QRCODE_PAY -> qrcodePay(new QrcodePayRequest(pool.getPoolCode(), request.merchantOrderNo(), request.amount(), request.subject(), request.channel(), request.notifyUrl(), timestamp, nonce, sign));
            case PayMethodService.H5_PAY -> h5Pay(new H5PayRequest(pool.getPoolCode(), request.merchantOrderNo(), request.amount(), request.subject(), request.notifyUrl(), request.returnUrl(), timestamp, nonce, sign));
            case PayMethodService.WECHAT_JSAPI_PAY -> wechatJsapiPay(new WechatJsapiPayRequest(pool.getPoolCode(), request.merchantOrderNo(), request.amount(), request.subject(), request.subAppId(), request.payerId(), request.notifyUrl(), timestamp, nonce, sign));
            case PayMethodService.ALIPAY_JSAPI_PAY -> alipayJsapiPay(new AlipayJsapiPayRequest(pool.getPoolCode(), request.merchantOrderNo(), request.amount(), request.subject(), request.subAppId(), request.payerId(), null, request.notifyUrl(), timestamp, nonce, sign));
            default -> throw new BizException(BusinessErrorCode.UNSUPPORTED_PAY_METHOD);
        };
    }

    @Transactional
    public PayResponse debugQueryPay(RouteQueryTestRequest request) {
        PayMerchantPool pool = poolService.requirePool(request.poolId());
        long timestamp = System.currentTimeMillis();
        String nonce = "DEBUG" + timestamp;
        Map<String, Object> params = Map.of(
                "appId", pool.getPoolCode(),
                "merchantOrderNo", request.merchantOrderNo(),
                "timestamp", timestamp,
                "nonce", nonce
        );
        String signKey = debugSignKey(pool.getId(), request.payMethod());
        return queryPay(new QueryPayRequest(
                pool.getPoolCode(),
                request.merchantOrderNo(),
                timestamp,
                nonce,
                signService.sha256Sign(params, signKey)
        ));
    }

    @Transactional
    public PayResponse barcodePay(BarcodePayRequest request) {
        PayMerchantPool pool = securityService.verifyBarcodeRequest(request);
        return createPayment(pool, request, PayMethodService.BARCODE_PAY, securityService.toParamMap(request), request.authCode(), account -> channelAdapter.barcodePay(
                new BarcodeChannelRequest(request.merchantOrderNo(), request.amount(), request.authCode(), request.subject(), request.notifyUrl()),
                context(account)
        ));
    }

    @Transactional
    public PayResponse preOrder(PreOrderRequest request) {
        PayMerchantPool pool = securityService.verifyPreOrderRequest(request);
        return createPayment(pool, request, PayMethodService.PRE_ORDER, securityService.toParamMap(request), null, account -> channelAdapter.preOrder(
                new PreOrderChannelRequest(request.merchantOrderNo(), request.amount(), request.subject(), request.notifyUrl(), request.successUrl(), request.errorUrl()),
                context(account)
        ));
    }

    @Transactional
    public DecodeBarResponse decodeBar(DecodeBarRequest request) {
        PayMerchantPool pool = securityService.verifyDecodeBarRequest(request);
        PayMerchantAccount account = accountMapper.selectList(new LambdaQueryWrapper<PayMerchantAccount>()
                        .eq(PayMerchantAccount::getPoolId, pool.getId())
                        .eq(PayMerchantAccount::getStatus, "ENABLED")
                        .orderByAsc(PayMerchantAccount::getPriority)
                        .orderByAsc(PayMerchantAccount::getId))
                .stream()
                .filter(item -> supportsPayMethod(item, PayMethodService.DECODE_BAR))
                .findFirst()
                .orElseThrow(() -> new BizException(BusinessErrorCode.NO_AVAILABLE_ACCOUNT, "No enabled merchant account supports decode bar"));
        ChannelResponse response = channelAdapter.decodeBar(new DecodeBarChannelRequest(request.authCode(), request.sceneNo(), request.subAppId()), context(account));
        apiLogService.record(null, null, "UPSTREAM", PayMethodService.DECODE_BAR, securityService.toParamMap(request), response.rawResponse(), response.status(), null);
        return new DecodeBarResponse(request.appId(), response.payData(), response.responseMessage());
    }

    @Transactional
    public PayResponse scanPay(ScanPayRequest request) {
        PayMerchantPool pool = securityService.verifyScanRequest(request);
        return createPayment(pool, request, PayMethodService.SCAN_PAY, securityService.toParamMap(request), null, account -> channelAdapter.scanPay(
                new ScanChannelRequest(request.merchantOrderNo(), request.amount(), request.subject(), request.notifyUrl()),
                context(account)
        ));
    }

    @Transactional
    public PayResponse h5Pay(H5PayRequest request) {
        PayMerchantPool pool = securityService.verifyH5Request(request);
        return createPayment(pool, request, PayMethodService.H5_PAY, securityService.toParamMap(request), null, account -> channelAdapter.h5Pay(
                new H5ChannelRequest(request.merchantOrderNo(), request.amount(), request.subject(), request.notifyUrl(), request.returnUrl()),
                context(account)
        ));
    }

    @Transactional
    public PayResponse qrcodePay(QrcodePayRequest request) {
        PayMerchantPool pool = securityService.verifyQrcodeRequest(request);
        return createPayment(pool, request, PayMethodService.QRCODE_PAY, securityService.toParamMap(request), null, account -> channelAdapter.qrcodePay(
                new QrcodeChannelRequest(request.merchantOrderNo(), request.amount(), request.subject(), request.channel(), request.notifyUrl()),
                context(account)
        ));
    }

    @Transactional
    public PayResponse wechatJsapiPay(WechatJsapiPayRequest request) {
        PayMerchantPool pool = securityService.verifyWechatJsapiRequest(request);
        return createPayment(pool, request, PayMethodService.WECHAT_JSAPI_PAY, securityService.toParamMap(request), request.subOpenId(), account -> channelAdapter.wechatJsapiPay(
                new WechatJsapiChannelRequest(request.merchantOrderNo(), request.amount(), request.subject(), request.notifyUrl(), request.subAppId(), request.subOpenId()),
                context(account)
        ));
    }

    @Transactional
    public PayResponse alipayJsapiPay(AlipayJsapiPayRequest request) {
        if (!StringUtils.hasText(request.buyerId()) && !StringUtils.hasText(request.buyerOpenId())) {
            throw new BizException(BusinessErrorCode.INVALID_REQUEST_PARAMETER, "buyerId or buyerOpenId is required");
        }
        PayMerchantPool pool = securityService.verifyAlipayJsapiRequest(request);
        return createPayment(pool, request, PayMethodService.ALIPAY_JSAPI_PAY, securityService.toParamMap(request), firstText(request.buyerId(), request.buyerOpenId()), account -> channelAdapter.alipayJsapiPay(
                new AlipayJsapiChannelRequest(request.merchantOrderNo(), request.amount(), request.subject(), request.notifyUrl(), request.subAppId(), request.buyerId(), request.buyerOpenId()),
                context(account)
        ));
    }

    private PayResponse createPayment(PayMerchantPool pool, PayCreateRequest request, String payMethod, Map<String, Object> inboundLog, String authCode, ChannelInvoker invoker) {
        PayOrder existing = findOrder(request.appId(), request.merchantOrderNo());
        if (existing != null) {
            apiLogService.record(existing.getId(), existing.getMerchantOrderNo(), "INBOUND", payMethod, inboundLog, null, existing.getStatus(), null);
            return idempotentResponse(existing);
        }

        PayOrder order = createOrder(pool, request, payMethod, authCode);
        apiLogService.record(order.getId(), order.getMerchantOrderNo(), "INBOUND", payMethod, inboundLog, null, order.getStatus(), null);
        try {
            order.setStatus(STATUS_ROUTING);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);

            RouteDecision route = routeEngineService.route(order.getId(), order.getMerchantOrderNo(), pool.getId(), payMethod, request.amount());
            order.setAccountId(route.account().getId());
            order.setRouteType(route.record().getRouteType());
            order.setRouteRecordId(route.record().getId());
            order.setStatus(STATUS_PAYING);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);

            ChannelResponse channelResponse = invoker.invoke(route.account());
            apiLogService.record(order.getId(), order.getMerchantOrderNo(), "UPSTREAM", payMethod, upstreamPayLog(request, payMethod, route.account(), authCode), channelResponse.rawResponse(), channelResponse.status(), null);
            applyChannelResponse(order, channelResponse, route.account());
            return toResponse(order, channelResponse.payData());
        } catch (RuntimeException exception) {
            order.setStatus(STATUS_UNKNOWN);
            order.setUpstreamResponseMsg(exception.getMessage());
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            Object responseBody = exception instanceof ChannelException channelException ? channelException.getRawResponse() : null;
            apiLogService.record(order.getId(), order.getMerchantOrderNo(), "UPSTREAM", payMethod, upstreamPayLog(request, payMethod, order.getAccountId() == null ? null : requireAccount(order.getAccountId()), authCode), responseBody, STATUS_UNKNOWN, exception.getMessage());
            return toResponse(order, debugErrorData(exception));
        }
    }

    @Transactional
    public PayResponse queryPay(QueryPayRequest request) {
        securityService.verifyQueryRequest(request);
        PayOrder order = findOrder(request.appId(), request.merchantOrderNo());
        if (order == null) {
            throw new BizException(BusinessErrorCode.ORDER_NOT_FOUND);
        }
        apiLogService.record(order.getId(), order.getMerchantOrderNo(), "INBOUND", "QUERY_PAY", securityService.toParamMap(request), null, order.getStatus(), null);
        if (isTerminal(order.getStatus()) || order.getAccountId() == null) {
            return toResponse(order);
        }

        return queryOrder(order);
    }

    @Transactional
    public String handleNotify(Map<String, String> params) {
        PayOrder order = null;
        PayMerchantAccount account = null;
        String merchantOrderNo = params.get("mch_orderid");
        boolean verified = false;
        try {
            if (!StringUtils.hasText(merchantOrderNo)) {
                throw new BizException(BusinessErrorCode.INVALID_NOTIFY, "Missing mch_orderid");
            }
            order = orderMapper.selectOne(new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getMerchantOrderNo, merchantOrderNo));
            if (order == null) {
                throw new BizException(BusinessErrorCode.ORDER_NOT_FOUND);
            }
            account = order.getAccountId() == null ? null : requireAccount(order.getAccountId());
            if (account != null && StringUtils.hasText(params.get("sign"))) {
                String signKey = cryptoService.decrypt(account.getSignKeyEncrypted());
                if (!lfwinSignHelper.verifyMd5(params, signKey)) {
                    throw new BizException(BusinessErrorCode.INVALID_NOTIFY_SIGN);
                }
            }
            verified = true;
            String payStatus = params.get("paystatus");
            if ("1".equals(payStatus)) {
                markSuccess(order, params.get("orderid"), params.get("trade_no"), params.get("order_time"), "Notify success");
            } else if ("2".equals(payStatus) && !STATUS_SUCCESS.equals(order.getStatus())) {
                markFailed(order, params.get("orderid"), params.get("trade_no"), params.get("order_time"), "Notify failed", account);
            }
            apiLogService.record(order.getId(), order.getMerchantOrderNo(), "INBOUND", "PAY_NOTIFY", params, "success", order.getStatus(), null);
            notifyLogService.record(order.getTenantId(), order.getId(), order.getMerchantOrderNo(), params, true, true, null);
            return "success";
        } catch (RuntimeException exception) {
            BusinessErrorCode errorCode = notifyErrorCode(exception);
            String errorMessage = exception.getMessage();
            String tenantId = order == null ? null : order.getTenantId();
            Long orderId = order == null ? null : order.getId();
            String resolvedMerchantOrderNo = order == null ? merchantOrderNo : order.getMerchantOrderNo();
            apiLogService.record(orderId, resolvedMerchantOrderNo, "INBOUND", "PAY_NOTIFY", params, "fail", STATUS_FAILED, errorMessage, errorCode);
            notifyLogService.record(tenantId, orderId, resolvedMerchantOrderNo, params, verified, false, errorMessage);
            return "fail";
        }
    }

    private PayOrder createOrder(PayMerchantPool pool, PayCreateRequest request, String payMethod, String authCode) {
        PayOrder order = new PayOrder();
        order.setTenantId(pool.getTenantId());
        order.setPoolId(pool.getId());
        order.setAppId(request.appId());
        order.setPayMethod(payMethod);
        order.setMerchantOrderNo(request.merchantOrderNo());
        order.setAmount(request.amount());
        order.setSubject(request.subject());
        order.setAuthCodeMasked(securityService.mask(authCode));
        order.setNotifyUrl(request.notifyUrl());
        order.setStatus(STATUS_INIT);
        order.setQueryCount(0);
        order.setExpiredTime(LocalDateTime.now().plusMinutes(15));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);
        return order;
    }

    private void applyChannelResponse(PayOrder order, ChannelResponse response, PayMerchantAccount account) {
        String status = normalizeStatus(response.status());
        if (STATUS_SUCCESS.equals(status)) {
            markSuccess(order, response.upstreamOrderId(), response.upstreamTradeNo(), response.upstreamOrderTime(), response.responseMessage());
            routeEngineService.clearFailure(account.getId());
        } else if (STATUS_FAILED.equals(status)) {
            markFailed(order, response.upstreamOrderId(), response.upstreamTradeNo(), response.upstreamOrderTime(), response.responseMessage(), account);
        } else {
            order.setStatus(STATUS_UNKNOWN.equals(status) ? STATUS_UNKNOWN : STATUS_PAYING);
            fillUpstream(order, response.upstreamOrderId(), response.upstreamTradeNo(), response.upstreamOrderTime());
            order.setUpstreamResponseCode(response.responseCode());
            order.setUpstreamResponseMsg(response.responseMessage());
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }

    private void markSuccess(PayOrder order, String upstreamOrderId, String upstreamTradeNo, String upstreamOrderTime, String message) {
        fillUpstream(order, upstreamOrderId, upstreamTradeNo, upstreamOrderTime);
        order.setStatus(STATUS_SUCCESS);
        order.setUpstreamResponseCode("10000");
        order.setUpstreamResponseMsg(message);
        order.setPaySuccessTime(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        limitReservationService.confirm(order.getId());
    }

    private void markFailed(PayOrder order, String upstreamOrderId, String upstreamTradeNo, String upstreamOrderTime, String message, PayMerchantAccount account) {
        if (STATUS_SUCCESS.equals(order.getStatus())) {
            return;
        }
        fillUpstream(order, upstreamOrderId, upstreamTradeNo, upstreamOrderTime);
        order.setStatus(STATUS_FAILED);
        order.setUpstreamResponseCode("10000");
        order.setUpstreamResponseMsg(message);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        limitReservationService.release(order.getId(), "ORDER_FAILED");
        if (account != null) {
            routeEngineService.recordFailure(account.getId(), null);
        }
    }

    private void fillUpstream(PayOrder order, String upstreamOrderId, String upstreamTradeNo, String upstreamOrderTime) {
        if (StringUtils.hasText(upstreamOrderId)) {
            order.setPlatformOrderNo(upstreamOrderId);
        }
        if (StringUtils.hasText(upstreamTradeNo)) {
            order.setChannelOrderNo(upstreamTradeNo);
        }
        if (StringUtils.hasText(upstreamOrderTime)) {
            order.setUpstreamOrderTime(upstreamOrderTime);
        }
    }

    private PayResponse idempotentResponse(PayOrder order) {
        if (STATUS_FAILED.equals(order.getStatus())) {
            throw new BizException(BusinessErrorCode.ORDER_ALREADY_FAILED, "Order already failed, please use a new merchantOrderNo");
        }
        return toResponse(order);
    }

    private PayResponse toResponse(PayOrder order) {
        return toResponse(order, null);
    }

    private PayResponse toResponse(PayOrder order, Object payData) {
        return new PayResponse(
                order.getAppId(),
                order.getMerchantOrderNo(),
                order.getAmount(),
                order.getPayMethod(),
                order.getStatus(),
                order.getPlatformOrderNo(),
                order.getChannelOrderNo(),
                payData,
                order.getUpstreamResponseMsg()
        );
    }

    private PayOrder findOrder(String appId, String merchantOrderNo) {
        return orderMapper.selectOne(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getAppId, appId)
                .eq(PayOrder::getMerchantOrderNo, merchantOrderNo));
    }

    private PayMerchantAccount requireAccount(Long accountId) {
        PayMerchantAccount account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(BusinessErrorCode.NO_AVAILABLE_ACCOUNT, "Merchant account does not exist");
        }
        return account;
    }

    private ChannelContext context(PayMerchantAccount account) {
        return new ChannelContext(account, cryptoService.decrypt(account.getApiKeyEncrypted()), cryptoService.decrypt(account.getSignKeyEncrypted()));
    }

    private String debugSignKey(Long poolId, String payMethod) {
        PayMerchantPool pool = poolService.requirePool(poolId);
        PayMerchantApp app = appMapper.selectOne(new LambdaQueryWrapper<PayMerchantApp>()
                .eq(PayMerchantApp::getAppId, pool.getPoolCode())
                .eq(PayMerchantApp::getStatus, "ENABLED"));
        if (app != null) {
            String appSecret = cryptoService.decrypt(app.getSecretEncrypted());
            if (StringUtils.hasText(appSecret)) {
                return appSecret;
            }
        }
        return accountMapper.selectList(new LambdaQueryWrapper<PayMerchantAccount>()
                        .eq(PayMerchantAccount::getPoolId, poolId)
                        .eq(PayMerchantAccount::getStatus, "ENABLED")
                        .orderByAsc(PayMerchantAccount::getPriority)
                        .orderByAsc(PayMerchantAccount::getId))
                .stream()
                .filter(account -> supportsPayMethod(account, payMethod))
                .map(account -> cryptoService.decrypt(account.getSignKeyEncrypted()))
                .filter(StringUtils::hasText)
                .findFirst()
                .orElseThrow(() -> new BizException(BusinessErrorCode.NO_AVAILABLE_ACCOUNT, "No enabled merchant account SignKey supports this debug pay method"));
    }

    private boolean supportsPayMethod(PayMerchantAccount account, String payMethod) {
        return StringUtils.hasText(account.getSupportPayMethods())
                && java.util.Arrays.stream(account.getSupportPayMethods().split(","))
                .map(String::trim)
                .anyMatch(item -> "ALL".equals(item) || payMethod.equals(item));
    }

    private Map<String, Object> upstreamPayLog(PayCreateRequest request, String payMethod, PayMerchantAccount account, String authCode) {
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        values.put("service", lfwinService(payMethod));
        if (account != null) {
            values.put("accountId", account.getId());
        }
        values.put("money", request.amount().toPlainString());
        values.put("mch_orderid", request.merchantOrderNo());
        if (authCode != null) {
            values.put("dynamic_id", securityService.mask(authCode));
        }
        return values;
    }

    private String lfwinService(String payMethod) {
        return switch (payMethod) {
            case PayMethodService.BARCODE_PAY -> "pay.comm.barcode";
            case PayMethodService.SCAN_PAY -> "pay.comm.jspay";
            case PayMethodService.H5_PAY -> "alipay.comm.jspay/wxpay.comm.jspay/unpay.comm.jspay";
            case PayMethodService.PRE_ORDER -> "/index/Payment/pre_order";
            case PayMethodService.DECODE_BAR -> "pay.alipay.decode_bar";
            case PayMethodService.QRCODE_PAY -> "pay.alipay.qrcode/pay.wxpay.qrcode/pay.unpay.qrcode";
            case PayMethodService.WECHAT_JSAPI_PAY -> "comm.js.pay/comm.mini.pay";
            case PayMethodService.ALIPAY_JSAPI_PAY -> "comm.js.pay/comm.mini.pay";
            default -> payMethod;
        };
    }

    @Transactional
    public RefundResponse refund(RefundRequest request) {
        PayMerchantPool pool = securityService.verifyRefundRequest(request);
        PayOrder order = findOrder(request.appId(), request.merchantOrderNo());
        if (order == null) throw new BizException(BusinessErrorCode.ORDER_NOT_FOUND);
        if (!STATUS_SUCCESS.equals(order.getStatus())) throw new BizException(BusinessErrorCode.ORDER_NOT_REFUNDABLE);
        if (request.refundAmount().compareTo(order.getAmount()) > 0) throw new BizException("Refund amount exceeds order amount");
        String merchantRefundNo = StringUtils.hasText(request.merchantRefundNo()) ? request.merchantRefundNo() : generateRefundNo();
        PayRefundOrder existing = findRefund(request.appId(), merchantRefundNo);
        if (existing != null) {
            apiLogService.record(order.getId(), order.getMerchantOrderNo(), "INBOUND", "REFUND", securityService.toParamMap(request), null, existing.getStatus(), null);
            return toRefundResponse(existing, existing.getUpstreamRawResponse());
        }
        PayMerchantAccount account = requireAccount(order.getAccountId());
        PayRefundOrder refundOrder = createRefundOrder(pool, order, account, merchantRefundNo, request);
        ChannelResponse response = channelAdapter.refund(new RefundChannelRequest(order.getMerchantOrderNo(), order.getPlatformOrderNo(), order.getUpstreamOrderTime(), merchantRefundNo, request.refundAmount(), request.reason(), request.notifyUrl()), context(account));
        applyRefundResponse(refundOrder, response, false);
        apiLogService.record(order.getId(), order.getMerchantOrderNo(), "OUTBOUND", "REFUND", securityService.toParamMap(request), response.rawResponse(), response.status(), null);
        return toRefundResponse(refundOrder, response.payData());
    }

    @Transactional
    public RefundResponse queryRefund(RefundQueryRequest request) {
        securityService.verifyRefundQueryRequest(request);
        if (!StringUtils.hasText(request.merchantRefundNo())) {
            throw new BizException("merchantRefundNo is required");
        }
        PayOrder order = findOrder(request.appId(), request.merchantOrderNo());
        if (order == null) throw new BizException(BusinessErrorCode.ORDER_NOT_FOUND);
        PayRefundOrder refundOrder = findRefund(request.appId(), request.merchantRefundNo());
        if (refundOrder == null) throw new BizException("Refund order does not exist");
        if (isRefundTerminal(refundOrder.getStatus()) && !Boolean.TRUE.equals(request.forceQuery())) {
            touchRefundQuery(refundOrder);
            apiLogService.record(order.getId(), order.getMerchantOrderNo(), "INBOUND", "QUERY_REFUND", securityService.toParamMap(request), null, refundOrder.getStatus(), null);
            return toRefundResponse(refundOrder, refundOrder.getUpstreamRawResponse());
        }
        PayMerchantAccount account = requireAccount(order.getAccountId());
        ChannelResponse response = channelAdapter.queryRefund(new RefundQueryChannelRequest(order.getMerchantOrderNo(), order.getPlatformOrderNo(), order.getUpstreamOrderTime(), request.merchantRefundNo()), context(account));
        applyRefundResponse(refundOrder, response, true);
        apiLogService.record(order.getId(), order.getMerchantOrderNo(), "OUTBOUND", "QUERY_REFUND", securityService.toParamMap(request), response.rawResponse(), response.status(), null);
        return toRefundResponse(refundOrder, response.payData());
    }

    @Transactional
    public RefundResponse queryRefundOrder(PayRefundOrder refundOrder) {
        return queryRefundOrder(refundOrder, false);
    }

    @Transactional
    public RefundResponse queryRefundOrder(PayRefundOrder refundOrder, boolean force) {
        if (isRefundTerminal(refundOrder.getStatus()) && !force) {
            touchRefundQuery(refundOrder);
            return toRefundResponse(refundOrder, refundOrder.getUpstreamRawResponse());
        }
        PayOrder order = orderMapper.selectById(refundOrder.getOrderId());
        if (order == null) throw new BizException(BusinessErrorCode.ORDER_NOT_FOUND);
        PayMerchantAccount account = requireAccount(refundOrder.getAccountId());
        ChannelResponse response = channelAdapter.queryRefund(new RefundQueryChannelRequest(order.getMerchantOrderNo(), order.getPlatformOrderNo(), order.getUpstreamOrderTime(), refundOrder.getMerchantRefundNo()), context(account));
        applyRefundResponse(refundOrder, response, true);
        apiLogService.record(order.getId(), order.getMerchantOrderNo(), "OUTBOUND", "QUERY_REFUND", Map.of("merchantRefundNo", refundOrder.getMerchantRefundNo()), response.rawResponse(), response.status(), null);
        return toRefundResponse(refundOrder, response.payData());
    }

    private String normalizeRefundStatus(ChannelResponse response) {
        if (response.payData() instanceof Map<?, ?> data && data.get("refundStatus") != null) {
            return String.valueOf(data.get("refundStatus"));
        }
        return switch (response.status()) {
            case STATUS_SUCCESS -> "SUCCESS";
            case STATUS_FAILED -> "FAILED";
            default -> "PROCESSING";
        };
    }

    private PayRefundOrder createRefundOrder(PayMerchantPool pool, PayOrder order, PayMerchantAccount account, String merchantRefundNo, RefundRequest request) {
        PayRefundOrder refundOrder = new PayRefundOrder();
        refundOrder.setTenantId(pool.getTenantId());
        refundOrder.setOrderId(order.getId());
        refundOrder.setPoolId(order.getPoolId());
        refundOrder.setAccountId(account.getId());
        refundOrder.setAppId(request.appId());
        refundOrder.setMerchantOrderNo(order.getMerchantOrderNo());
        refundOrder.setMerchantRefundNo(merchantRefundNo);
        refundOrder.setPlatformOrderNo(order.getPlatformOrderNo());
        refundOrder.setChannelOrderNo(order.getChannelOrderNo());
        refundOrder.setOrderAmount(order.getAmount());
        refundOrder.setRefundAmount(request.refundAmount());
        refundOrder.setReason(request.reason());
        refundOrder.setNotifyUrl(request.notifyUrl());
        refundOrder.setStatus(STATUS_INIT);
        refundOrder.setQueryCount(0);
        refundOrder.setCreatedAt(LocalDateTime.now());
        refundOrder.setUpdatedAt(LocalDateTime.now());
        refundOrderMapper.insert(refundOrder);
        return refundOrder;
    }

    private void applyRefundResponse(PayRefundOrder refundOrder, ChannelResponse response, boolean query) {
        String status = normalizeRefundStatus(response);
        refundOrder.setStatus(status);
        refundOrder.setUpstreamRefundNo(response.upstreamTradeNo());
        refundOrder.setUpstreamResponseCode(response.responseCode());
        refundOrder.setUpstreamResponseMsg(response.responseMessage());
        refundOrder.setUpstreamRawResponse(response.rawResponse());
        if (query) {
            refundOrder.setLastQueryTime(LocalDateTime.now());
            refundOrder.setQueryCount((refundOrder.getQueryCount() == null ? 0 : refundOrder.getQueryCount()) + 1);
        }
        if ("SUCCESS".equals(status)) {
            refundOrder.setRefundSuccessTime(LocalDateTime.now());
        }
        refundOrder.setUpdatedAt(LocalDateTime.now());
        refundOrderMapper.updateById(refundOrder);
    }

    private void touchRefundQuery(PayRefundOrder refundOrder) {
        refundOrder.setLastQueryTime(LocalDateTime.now());
        refundOrder.setQueryCount((refundOrder.getQueryCount() == null ? 0 : refundOrder.getQueryCount()) + 1);
        refundOrder.setUpdatedAt(LocalDateTime.now());
        refundOrderMapper.updateById(refundOrder);
    }

    private RefundResponse toRefundResponse(PayRefundOrder refundOrder, Object data) {
        return new RefundResponse(refundOrder.getAppId(), refundOrder.getMerchantOrderNo(), refundOrder.getMerchantRefundNo(),
                refundOrder.getRefundAmount(), refundOrder.getStatus(), data, refundOrder.getUpstreamResponseMsg());
    }

    private PayRefundOrder findRefund(String appId, String merchantRefundNo) {
        return refundOrderMapper.selectOne(new LambdaQueryWrapper<PayRefundOrder>()
                .eq(PayRefundOrder::getAppId, appId)
                .eq(PayRefundOrder::getMerchantRefundNo, merchantRefundNo));
    }

    private boolean isRefundTerminal(String status) {
        return "SUCCESS".equals(status) || "FAILED".equals(status);
    }

    private String generateRefundNo() {
        return "RF" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) return value;
        }
        return null;
    }

    private Object debugErrorData(RuntimeException exception) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("error", exception.getMessage());
        if (exception instanceof ChannelException channelException && StringUtils.hasText(channelException.getRawResponse())) {
            values.put("rawResponse", channelException.getRawResponse());
        }
        return values;
    }

    @FunctionalInterface
    private interface ChannelInvoker {
        ChannelResponse invoke(PayMerchantAccount account);
    }

    private Map<String, Object> queryLog(PayOrder order) {
        return Map.of(
                "service", "pay.comm.query_order",
                "mch_orderid", order.getMerchantOrderNo(),
                "order_time", resolveQueryOrderTime(order)
        );
    }

    private String resolveQueryOrderTime(PayOrder order) {
        if (StringUtils.hasText(order.getUpstreamOrderTime())) {
            return order.getUpstreamOrderTime();
        }
        if (order.getCreatedAt() != null) {
            return order.getCreatedAt().format(LFWIN_ORDER_TIME_FORMATTER);
        }
        return "";
    }

    @Transactional
    public PayResponse queryOrder(PayOrder order) {
        return queryOrder(order, false);
    }

    @Transactional
    public PayResponse queryOrder(PayOrder order, boolean force) {
        if ((isTerminal(order.getStatus()) && !force) || order.getAccountId() == null) {
            return toResponse(order);
        }
        PayMerchantAccount account = requireAccount(order.getAccountId());
        ChannelResponse channelResponse = channelAdapter.queryPay(
                new QueryChannelRequest(order.getMerchantOrderNo(), order.getPlatformOrderNo(), resolveQueryOrderTime(order)),
                context(account)
        );
        order.setLastQueryTime(LocalDateTime.now());
        order.setQueryCount((order.getQueryCount() == null ? 0 : order.getQueryCount()) + 1);
        orderMapper.updateById(order);
        apiLogService.record(order.getId(), order.getMerchantOrderNo(), "OUTBOUND", "QUERY", queryLog(order), channelResponse.rawResponse(), channelResponse.status(), null);
        applyChannelResponse(order, channelResponse, account);
        return toResponse(order);
    }

    private boolean isTerminal(String status) {
        return STATUS_SUCCESS.equals(status) || STATUS_FAILED.equals(status);
    }

    private String normalizeStatus(String status) {
        if (STATUS_SUCCESS.equals(status) || STATUS_FAILED.equals(status) || STATUS_PAYING.equals(status) || STATUS_UNKNOWN.equals(status)) {
            return status;
        }
        return STATUS_UNKNOWN;
    }

    private BusinessErrorCode notifyErrorCode(RuntimeException exception) {
        if (exception instanceof BizException bizException) {
            int code = bizException.getCode();
            for (BusinessErrorCode errorCode : BusinessErrorCode.values()) {
                if (errorCode.code() == code) {
                    return errorCode;
                }
            }
        }
        return BusinessErrorCode.INVALID_NOTIFY;
    }
}
