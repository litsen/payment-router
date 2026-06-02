package com.company.payrouter.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.payrouter.modules.order.entity.PayOrder;
import com.company.payrouter.modules.order.mapper.PayOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "pay-router.pay-query", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PayQueryCompensationJob {
    private final PayOrderMapper orderMapper;
    private final PaymentGatewayService paymentGatewayService;
    private final int intervalSeconds;
    private final int maxCount;

    public PayQueryCompensationJob(
            PayOrderMapper orderMapper,
            PaymentGatewayService paymentGatewayService,
            @Value("${pay-router.pay-query.interval-seconds:${PAY_QUERY_INTERVAL_SECONDS:30}}") int intervalSeconds,
            @Value("${pay-router.pay-query.max-count:${PAY_QUERY_MAX_COUNT:10}}") int maxCount
    ) {
        this.orderMapper = orderMapper;
        this.paymentGatewayService = paymentGatewayService;
        this.intervalSeconds = intervalSeconds;
        this.maxCount = maxCount;
    }

    @Scheduled(fixedDelayString = "${pay-router.pay-query.interval-seconds:${PAY_QUERY_INTERVAL_SECONDS:30}}000")
    public void compensate() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(intervalSeconds);
        List<PayOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<PayOrder>()
                .in(PayOrder::getStatus, List.of("PAYING", "UNKNOWN"))
                .and(query -> query.isNull(PayOrder::getLastQueryTime).or().le(PayOrder::getLastQueryTime, before))
                .lt(PayOrder::getQueryCount, maxCount)
                .orderByAsc(PayOrder::getId)
                .last("LIMIT 50"));
        for (PayOrder order : orders) {
            try {
                paymentGatewayService.queryOrder(order);
            } catch (Exception exception) {
                log.warn("Pay query compensation failed, orderId={}", order.getId(), exception);
            }
        }
    }
}
