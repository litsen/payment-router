package com.company.payrouter.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.RefundResponse;
import com.company.payrouter.modules.merchant.entity.PayMerchantAccount;
import com.company.payrouter.modules.merchant.entity.PayMerchantPool;
import com.company.payrouter.modules.merchant.mapper.PayMerchantAccountMapper;
import com.company.payrouter.modules.merchant.service.MerchantPoolService;
import com.company.payrouter.modules.order.dto.OrderDtos.RefundOrderResponse;
import com.company.payrouter.modules.order.entity.PayRefundOrder;
import com.company.payrouter.modules.order.mapper.PayRefundOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class RefundOrderAdminService {
    private final PayRefundOrderMapper refundOrderMapper;
    private final MerchantPoolService poolService;
    private final PayMerchantAccountMapper accountMapper;
    private final PaymentGatewayService paymentGatewayService;

    public RefundOrderAdminService(
            PayRefundOrderMapper refundOrderMapper,
            MerchantPoolService poolService,
            PayMerchantAccountMapper accountMapper,
            PaymentGatewayService paymentGatewayService
    ) {
        this.refundOrderMapper = refundOrderMapper;
        this.poolService = poolService;
        this.accountMapper = accountMapper;
        this.paymentGatewayService = paymentGatewayService;
    }

    public PageResult<RefundOrderResponse> pageRefunds(long current, long size, String merchantOrderNo, String merchantRefundNo,
                                                       String platformOrderNo, String upstreamRefundNo, String status,
                                                       Long poolId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<PayRefundOrder> wrapper = new LambdaQueryWrapper<PayRefundOrder>().orderByDesc(PayRefundOrder::getId);
        if (StringUtils.hasText(merchantOrderNo)) wrapper.like(PayRefundOrder::getMerchantOrderNo, merchantOrderNo);
        if (StringUtils.hasText(merchantRefundNo)) wrapper.like(PayRefundOrder::getMerchantRefundNo, merchantRefundNo);
        if (StringUtils.hasText(platformOrderNo)) wrapper.like(PayRefundOrder::getPlatformOrderNo, platformOrderNo);
        if (StringUtils.hasText(upstreamRefundNo)) wrapper.like(PayRefundOrder::getUpstreamRefundNo, upstreamRefundNo);
        if (StringUtils.hasText(status)) wrapper.eq(PayRefundOrder::getStatus, status);
        if (poolId != null) wrapper.eq(PayRefundOrder::getPoolId, poolId);
        if (startTime != null) wrapper.ge(PayRefundOrder::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(PayRefundOrder::getCreatedAt, endTime);
        Page<PayRefundOrder> page = refundOrderMapper.selectPage(Page.of(current, size), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toResponse).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public RefundOrderResponse getRefund(Long id) {
        return toResponse(requireRefund(id));
    }

    public RefundResponse queryRefund(Long id) {
        return paymentGatewayService.queryRefundOrder(requireRefund(id));
    }

    private PayRefundOrder requireRefund(Long id) {
        PayRefundOrder refundOrder = refundOrderMapper.selectById(id);
        if (refundOrder == null) throw new BizException("Refund order does not exist");
        return refundOrder;
    }

    private RefundOrderResponse toResponse(PayRefundOrder refundOrder) {
        PayMerchantPool pool = poolService.requirePool(refundOrder.getPoolId());
        PayMerchantAccount account = refundOrder.getAccountId() == null ? null : accountMapper.selectById(refundOrder.getAccountId());
        return new RefundOrderResponse(refundOrder.getId(), refundOrder.getTenantId(), refundOrder.getOrderId(),
                refundOrder.getPoolId(), pool.getPoolName(), refundOrder.getAccountId(),
                account == null ? null : account.getAccountName(), refundOrder.getAppId(),
                refundOrder.getMerchantOrderNo(), refundOrder.getMerchantRefundNo(),
                refundOrder.getPlatformOrderNo(), refundOrder.getChannelOrderNo(), refundOrder.getUpstreamRefundNo(),
                refundOrder.getOrderAmount(), refundOrder.getRefundAmount(), refundOrder.getReason(),
                refundOrder.getNotifyUrl(), refundOrder.getStatus(), refundOrder.getUpstreamResponseCode(),
                refundOrder.getUpstreamResponseMsg(), refundOrder.getUpstreamRawResponse(),
                refundOrder.getRefundSuccessTime(), refundOrder.getLastQueryTime(), refundOrder.getQueryCount(),
                refundOrder.getCreatedAt(), refundOrder.getUpdatedAt());
    }
}
