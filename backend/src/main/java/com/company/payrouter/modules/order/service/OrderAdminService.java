package com.company.payrouter.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.infrastructure.crypto.AesCryptoService;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.PayResponse;
import com.company.payrouter.modules.merchant.entity.PayMerchantAccount;
import com.company.payrouter.modules.merchant.entity.PayMerchantPool;
import com.company.payrouter.modules.merchant.mapper.PayMerchantAccountMapper;
import com.company.payrouter.modules.merchant.service.MerchantPoolService;
import com.company.payrouter.modules.order.dto.OrderDtos.NotifyLogResponse;
import com.company.payrouter.modules.order.dto.OrderDtos.OrderLogResponse;
import com.company.payrouter.modules.order.dto.OrderDtos.OrderResponse;
import com.company.payrouter.modules.order.entity.PayApiLog;
import com.company.payrouter.modules.order.entity.PayOrder;
import com.company.payrouter.modules.order.entity.PayOrderNotifyLog;
import com.company.payrouter.modules.order.mapper.PayApiLogMapper;
import com.company.payrouter.modules.order.mapper.PayOrderMapper;
import com.company.payrouter.modules.order.mapper.PayOrderNotifyLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class OrderAdminService {
    private final PayOrderMapper orderMapper;
    private final PayApiLogMapper logMapper;
    private final PayOrderNotifyLogMapper notifyLogMapper;
    private final MerchantPoolService poolService;
    private final PayMerchantAccountMapper accountMapper;
    private final PaymentGatewayService paymentGatewayService;
    private final AesCryptoService cryptoService;

    public OrderAdminService(
            PayOrderMapper orderMapper,
            PayApiLogMapper logMapper,
            PayOrderNotifyLogMapper notifyLogMapper,
            MerchantPoolService poolService,
            PayMerchantAccountMapper accountMapper,
            PaymentGatewayService paymentGatewayService,
            AesCryptoService cryptoService
    ) {
        this.orderMapper = orderMapper;
        this.logMapper = logMapper;
        this.notifyLogMapper = notifyLogMapper;
        this.poolService = poolService;
        this.accountMapper = accountMapper;
        this.paymentGatewayService = paymentGatewayService;
        this.cryptoService = cryptoService;
    }

    public PageResult<OrderResponse> pageOrders(long current, long size, String merchantOrderNo, String platformOrderNo, String channelOrderNo,
                                                String payMethod, String status, Long poolId, BigDecimal minAmount, BigDecimal maxAmount,
                                                LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<PayOrder>().orderByDesc(PayOrder::getId);
        if (StringUtils.hasText(merchantOrderNo)) wrapper.like(PayOrder::getMerchantOrderNo, merchantOrderNo);
        if (StringUtils.hasText(platformOrderNo)) wrapper.like(PayOrder::getPlatformOrderNo, platformOrderNo);
        if (StringUtils.hasText(channelOrderNo)) wrapper.like(PayOrder::getChannelOrderNo, channelOrderNo);
        if (StringUtils.hasText(payMethod)) wrapper.eq(PayOrder::getPayMethod, payMethod);
        if (StringUtils.hasText(status)) wrapper.eq(PayOrder::getStatus, status);
        if (poolId != null) wrapper.eq(PayOrder::getPoolId, poolId);
        if (minAmount != null) wrapper.ge(PayOrder::getAmount, minAmount);
        if (maxAmount != null) wrapper.le(PayOrder::getAmount, maxAmount);
        if (startTime != null) wrapper.ge(PayOrder::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(PayOrder::getCreatedAt, endTime);
        Page<PayOrder> page = orderMapper.selectPage(Page.of(current, size), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toOrderResponse).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public OrderResponse getOrder(Long id) {
        return toOrderResponse(requireOrder(id));
    }

    public PayResponse queryOrder(Long id) {
        return paymentGatewayService.queryOrder(requireOrder(id));
    }

    public PageResult<OrderLogResponse> pageLogs(long current, long size, Long orderId, String merchantOrderNo, String direction, String apiType) {
        LambdaQueryWrapper<PayApiLog> wrapper = new LambdaQueryWrapper<PayApiLog>().orderByDesc(PayApiLog::getId);
        if (orderId != null) wrapper.eq(PayApiLog::getOrderId, orderId);
        if (StringUtils.hasText(merchantOrderNo)) wrapper.like(PayApiLog::getMerchantOrderNo, merchantOrderNo);
        if (StringUtils.hasText(direction)) wrapper.eq(PayApiLog::getDirection, direction);
        if (StringUtils.hasText(apiType)) wrapper.eq(PayApiLog::getApiType, apiType);
        Page<PayApiLog> page = logMapper.selectPage(Page.of(current, size), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toLogResponse).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public OrderLogResponse getLog(Long id) {
        PayApiLog log = logMapper.selectById(id);
        if (log == null) throw new BizException("Order log does not exist");
        return toLogResponse(log);
    }

    public PageResult<NotifyLogResponse> pageNotifyLogs(long current, long size, Long orderId, String merchantOrderNo) {
        LambdaQueryWrapper<PayOrderNotifyLog> wrapper = new LambdaQueryWrapper<PayOrderNotifyLog>().orderByDesc(PayOrderNotifyLog::getId);
        if (orderId != null) wrapper.eq(PayOrderNotifyLog::getOrderId, orderId);
        if (StringUtils.hasText(merchantOrderNo)) wrapper.like(PayOrderNotifyLog::getMerchantOrderNo, merchantOrderNo);
        Page<PayOrderNotifyLog> page = notifyLogMapper.selectPage(Page.of(current, size), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toNotifyResponse).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    private PayOrder requireOrder(Long id) {
        PayOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException("Order does not exist");
        return order;
    }

    private OrderResponse toOrderResponse(PayOrder order) {
        PayMerchantPool pool = poolService.requirePool(order.getPoolId());
        PayMerchantAccount account = order.getAccountId() == null ? null : accountMapper.selectById(order.getAccountId());
        return new OrderResponse(order.getId(), order.getTenantId(), order.getAppId(), order.getMerchantOrderNo(),
                order.getPlatformOrderNo(), order.getChannelOrderNo(), order.getPayMethod(), order.getAmount(),
                order.getSubject(), order.getAuthCodeMasked(), order.getUpstreamOrderTime(), order.getRouteType(),
                order.getRouteRecordId(), order.getPoolId(), pool.getPoolName(), order.getAccountId(),
                account == null ? null : account.getAccountName(),
                account == null ? null : cryptoService.maskEncrypted(account.getApiKeyEncrypted()),
                order.getStatus(), order.getNotifyUrl(),
                order.getPaySuccessTime(), order.getExpiredTime(), order.getLastQueryTime(), order.getQueryCount(),
                order.getUpstreamResponseCode(), order.getUpstreamResponseMsg(), order.getCreatedAt(), order.getUpdatedAt());
    }

    private OrderLogResponse toLogResponse(PayApiLog log) {
        return new OrderLogResponse(log.getId(), log.getTenantId(), log.getOrderId(), log.getMerchantOrderNo(),
                log.getDirection(), log.getApiType(), log.getRequestUrl(), log.getRequestHeadersJson(), log.getRequestBody(),
                log.getResponseBody(), log.getHttpStatus(), log.getCostMs(), log.getSuccess(), log.getErrorCode(),
                log.getResultStatus(), log.getErrorMessage(), log.getCreatedAt());
    }

    private NotifyLogResponse toNotifyResponse(PayOrderNotifyLog log) {
        return new NotifyLogResponse(log.getId(), log.getTenantId(), log.getOrderId(), log.getMerchantOrderNo(),
                log.getNotifyBody(), log.getVerified(), log.getSuccess(), log.getErrorMessage(), log.getCreatedAt());
    }
}
