package com.company.payrouter.modules.route.service;

import com.company.payrouter.modules.merchant.entity.PayMerchantAccount;
import com.company.payrouter.modules.route.entity.PayAccountLimitBucket;
import com.company.payrouter.modules.route.entity.PayAccountLimitFlow;
import com.company.payrouter.modules.route.mapper.PayAccountLimitBucketMapper;
import com.company.payrouter.modules.route.mapper.PayAccountLimitFlowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
public class LimitReservationService {
    private static final String DAY = "DAY";
    private static final String MONTH = "MONTH";
    private static final String RESERVED = "RESERVED";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String RELEASED = "RELEASED";

    private final LimitReservationProperties properties;
    private final PayAccountLimitBucketMapper bucketMapper;
    private final PayAccountLimitFlowMapper flowMapper;

    public LimitReservationService(
            LimitReservationProperties properties,
            PayAccountLimitBucketMapper bucketMapper,
            PayAccountLimitFlowMapper flowMapper
    ) {
        this.properties = properties;
        this.bucketMapper = bucketMapper;
        this.flowMapper = flowMapper;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Transactional
    public boolean reserve(Long orderId, String merchantOrderNo, PayMerchantAccount account, BigDecimal amount) {
        if (!isEnabled() || orderId == null || amount == null) {
            return true;
        }
        List<PayAccountLimitFlow> existing = flowMapper.selectByOrderId(orderId);
        if (!existing.isEmpty()) {
            return existing.stream().noneMatch(flow -> RELEASED.equals(flow.getFlowStatus()));
        }

        List<PeriodLimit> limits = periodLimits(account);
        List<PayAccountLimitBucket> reserved = new ArrayList<>();
        for (PeriodLimit limit : limits) {
            PayAccountLimitBucket bucket = ensureBucket(account, limit);
            if (bucketMapper.reserve(bucket.getId(), amount, limit.limitAmount()) != 1) {
                for (PayAccountLimitBucket item : reserved) {
                    bucketMapper.release(item.getId(), amount);
                }
                return false;
            }
            reserved.add(bucket);
        }
        for (PayAccountLimitBucket bucket : reserved) {
            flowMapper.insertIgnoreFlow(
                    account.getTenantId(),
                    orderId,
                    merchantOrderNo,
                    account.getId(),
                    bucket.getId(),
                    bucket.getPeriodType(),
                    bucket.getPeriodStart(),
                    amount,
                    LocalDateTime.now()
            );
        }
        return true;
    }

    @Transactional
    public void confirm(Long orderId) {
        if (!isEnabled() || orderId == null) {
            return;
        }
        for (PayAccountLimitFlow flow : flowMapper.selectByOrderId(orderId)) {
            if (CONFIRMED.equals(flow.getFlowStatus())) {
                continue;
            }
            if (!RESERVED.equals(flow.getFlowStatus())) {
                continue;
            }
            if (bucketMapper.confirm(flow.getBucketId(), flow.getAmount()) == 1) {
                flowMapper.markConfirmed(flow.getId());
            }
        }
    }

    @Transactional
    public void release(Long orderId, String reason) {
        if (!isEnabled() || orderId == null) {
            return;
        }
        for (PayAccountLimitFlow flow : flowMapper.selectByOrderId(orderId)) {
            if (!RESERVED.equals(flow.getFlowStatus())) {
                continue;
            }
            if (bucketMapper.release(flow.getBucketId(), flow.getAmount()) == 1) {
                flowMapper.markReleased(flow.getId(), reason);
            }
        }
    }

    private PayAccountLimitBucket ensureBucket(PayMerchantAccount account, PeriodLimit limit) {
        bucketMapper.insertIgnoreBucket(
                account.getTenantId(),
                account.getId(),
                limit.periodType(),
                limit.periodStart(),
                limit.periodEnd(),
                limit.limitAmount()
        );
        return bucketMapper.selectBucket(account.getId(), limit.periodType(), limit.periodStart());
    }

    private List<PeriodLimit> periodLimits(PayMerchantAccount account) {
        LocalDate today = LocalDate.now();
        List<PeriodLimit> limits = new ArrayList<>();
        if (account.getDailyAmountLimit() != null) {
            LocalDateTime start = today.atStartOfDay();
            limits.add(new PeriodLimit(DAY, start, start.plusDays(1), account.getDailyAmountLimit()));
        }
        if (account.getMonthlyAmountLimit() != null) {
            LocalDateTime start = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
            limits.add(new PeriodLimit(MONTH, start, start.plusMonths(1), account.getMonthlyAmountLimit()));
        }
        return limits;
    }

    private record PeriodLimit(String periodType, LocalDateTime periodStart, LocalDateTime periodEnd, BigDecimal limitAmount) {
    }
}
