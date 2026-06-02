package com.company.payrouter.modules.route.service;

import com.company.payrouter.modules.order.entity.PayOrder;
import com.company.payrouter.modules.order.mapper.PayOrderMapper;
import com.company.payrouter.modules.route.entity.PayAccountLimitFlow;
import com.company.payrouter.modules.route.mapper.PayAccountLimitFlowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LimitReservationCompensationJob {
    private static final Logger log = LoggerFactory.getLogger(LimitReservationCompensationJob.class);
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final LimitReservationProperties properties;
    private final PayAccountLimitFlowMapper flowMapper;
    private final PayOrderMapper orderMapper;
    private final LimitReservationService reservationService;

    public LimitReservationCompensationJob(
            LimitReservationProperties properties,
            PayAccountLimitFlowMapper flowMapper,
            PayOrderMapper orderMapper,
            LimitReservationService reservationService
    ) {
        this.properties = properties;
        this.flowMapper = flowMapper;
        this.orderMapper = orderMapper;
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "${pay-router.limit-reservation.compensation-interval-seconds:60}000")
    public void compensate() {
        if (!properties.isEnabled()) {
            return;
        }
        LocalDateTime reservedBefore = LocalDateTime.now().minusSeconds(properties.getReservationCheckDelaySeconds());
        for (PayAccountLimitFlow flow : flowMapper.selectReservedBefore(reservedBefore, properties.getCompensationBatchSize())) {
            PayOrder order = orderMapper.selectById(flow.getOrderId());
            if (order == null) {
                log.warn("event=limit_reservation_compensation orderId={} result=SKIPPED reason=ORDER_NOT_FOUND", flow.getOrderId());
                continue;
            }
            if (STATUS_SUCCESS.equals(order.getStatus())) {
                reservationService.confirm(order.getId());
            } else if (STATUS_FAILED.equals(order.getStatus())) {
                reservationService.release(order.getId(), "COMPENSATE_FAILED_ORDER");
            } else {
                log.info("event=limit_reservation_compensation orderId={} status={} result=WAITING", order.getId(), order.getStatus());
            }
        }
    }
}
