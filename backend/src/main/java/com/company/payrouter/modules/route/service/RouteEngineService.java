package com.company.payrouter.modules.route.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.common.exception.BusinessErrorCode;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.modules.merchant.entity.PayMerchantAccount;
import com.company.payrouter.modules.merchant.entity.PayMerchantPool;
import com.company.payrouter.modules.merchant.mapper.PayMerchantAccountMapper;
import com.company.payrouter.modules.merchant.service.MerchantPoolService;
import com.company.payrouter.modules.order.mapper.PayOrderMapper;
import com.company.payrouter.modules.paymethod.service.PayMethodService;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteRecordResponse;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteResultResponse;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteTestRequest;
import com.company.payrouter.modules.route.entity.PayRouteRecord;
import com.company.payrouter.modules.route.entity.PayRouteRule;
import com.company.payrouter.modules.route.mapper.PayRouteRecordMapper;
import com.company.payrouter.modules.route.mapper.PayRouteRuleMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class RouteEngineService {
    private static final int DEFAULT_FAIL_THRESHOLD = 3;
    private static final int DEFAULT_BREAK_MINUTES = 30;

    private final PayRouteRuleMapper ruleMapper;
    private final PayRouteRecordMapper recordMapper;
    private final PayOrderMapper orderMapper;
    private final PayMerchantAccountMapper accountMapper;
    private final MerchantPoolService poolService;
    private final PayMethodService payMethodService;
    private final LimitReservationService limitReservationService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public RouteEngineService(
            PayRouteRuleMapper ruleMapper,
            PayRouteRecordMapper recordMapper,
            PayOrderMapper orderMapper,
            PayMerchantAccountMapper accountMapper,
            MerchantPoolService poolService,
            PayMethodService payMethodService,
            LimitReservationService limitReservationService,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.ruleMapper = ruleMapper;
        this.recordMapper = recordMapper;
        this.orderMapper = orderMapper;
        this.accountMapper = accountMapper;
        this.poolService = poolService;
        this.payMethodService = payMethodService;
        this.limitReservationService = limitReservationService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RouteResultResponse testRoute(RouteTestRequest request) {
        RouteDecision decision = route(null, request.merchantOrderNo(), request.poolId(), request.payMethod(), request.amount());
        boolean simulatedFailure = Boolean.TRUE.equals(request.simulateFailure());
        if (simulatedFailure) {
            recordFailure(decision.account().getId(), decision.rule());
        } else {
            clearFailure(decision.account().getId());
        }
        return new RouteResultResponse(
                decision.record().getId(),
                decision.account().getId(),
                decision.account().getAccountName(),
                decision.account().getPoolId(),
                decision.rule() == null ? null : decision.rule().getId(),
                decision.record().getRouteType(),
                decision.record().getMerchantOrderNo(),
                decision.record().getAmount(),
                simulatedFailure,
                simulatedFailure ? "Route selected and simulated failure recorded" : "Route selected"
        );
    }

    @Transactional(noRollbackFor = BizException.class)
    public RouteDecision route(Long orderId, String merchantOrderNo, Long poolId, String payMethod, BigDecimal amount) {
        payMethodService.ensureEnabled(payMethod);
        PayMerchantPool pool = poolService.requirePool(poolId);
        List<PayMerchantAccount> candidates = accountMapper.selectList(new LambdaQueryWrapper<PayMerchantAccount>()
                .eq(PayMerchantAccount::getPoolId, poolId)
                .orderByAsc(PayMerchantAccount::getPriority)
                .orderByDesc(PayMerchantAccount::getWeight)
                .orderByAsc(PayMerchantAccount::getId));
        List<PayMerchantAccount> available = candidates.stream()
                .filter(account -> isAvailable(account, payMethod, amount))
                .toList();
        if (available.isEmpty()) {
            throw new BizException(BusinessErrorCode.NO_AVAILABLE_ACCOUNT, "当前没有可用支付参数，请检查支付参数状态、支付方式、可用时间、金额限额和熔断状态");
        }

        PayRouteRule rule = chooseRule(poolId, payMethod);
        PayMerchantAccount selected = selectAccountWithReservation(orderId, merchantOrderNo, available, rule, poolId, payMethod, amount);
        PayRouteRecord record = saveRecord(orderId, merchantOrderNo, pool, selected, rule, amount, available);
        return new RouteDecision(selected, rule, record);
    }

    public PageResult<RouteRecordResponse> pageRecords(long current, long size, String merchantOrderNo, Long poolId, Long accountId) {
        LambdaQueryWrapper<PayRouteRecord> wrapper = new LambdaQueryWrapper<PayRouteRecord>()
                .orderByDesc(PayRouteRecord::getId);
        if (StringUtils.hasText(merchantOrderNo)) {
            wrapper.like(PayRouteRecord::getMerchantOrderNo, merchantOrderNo);
        }
        if (poolId != null) {
            wrapper.eq(PayRouteRecord::getPoolId, poolId);
        }
        if (accountId != null) {
            wrapper.eq(PayRouteRecord::getAccountId, accountId);
        }
        Page<PayRouteRecord> page = recordMapper.selectPage(Page.of(current, size), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toRecordResponse).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public void recordFailure(Long accountId, PayRouteRule rule) {
        String failKey = "pay:route:fail_count:" + accountId;
        Long count = redisTemplate.opsForValue().increment(failKey);
        redisTemplate.expire(failKey, 1, TimeUnit.DAYS);
        int threshold = failThreshold(rule);
        if (count != null && count >= threshold) {
            redisTemplate.opsForValue().set(circuitBreakKey(accountId), "1", breakMinutes(rule), TimeUnit.MINUTES);
        }
    }

    public void clearFailure(Long accountId) {
        redisTemplate.delete("pay:route:fail_count:" + accountId);
        redisTemplate.delete(circuitBreakKey(accountId));
    }

    private PayRouteRule chooseRule(Long poolId, String payMethod) {
        return ruleMapper.selectList(new LambdaQueryWrapper<PayRouteRule>()
                        .eq(PayRouteRule::getPoolId, poolId)
                        .eq(PayRouteRule::getEnabled, true)
                        .orderByAsc(PayRouteRule::getPriority)
                        .orderByAsc(PayRouteRule::getId))
                .stream()
                .filter(rule -> ruleMatchesPayMethod(rule, payMethod))
                .filter(this::ruleMatchesNow)
                .findFirst()
                .orElse(null);
    }

    private boolean ruleMatchesPayMethod(PayRouteRule rule, String payMethod) {
        if (!StringUtils.hasText(rule.getPayMethod()) || "ALL".equals(rule.getPayMethod())) {
            return true;
        }
        return List.of(rule.getPayMethod().split(",")).stream()
                .map(String::trim)
                .anyMatch(payMethod::equals);
    }

    private PayMerchantAccount selectAccount(List<PayMerchantAccount> available, PayRouteRule rule, Long poolId, String payMethod) {
        List<PayMerchantAccount> topPriority = firstPriorityGroup(available);
        String routeType = rule == null ? RouteRuleService.ROUND_ROBIN : rule.getRuleType();
        if (RouteRuleService.WEIGHT_RANDOM.equals(routeType)) {
            return weightedRandom(topPriority);
        }
        return roundRobin(topPriority, poolId, payMethod);
    }

    private PayMerchantAccount selectAccountWithReservation(Long orderId, String merchantOrderNo, List<PayMerchantAccount> available, PayRouteRule rule, Long poolId, String payMethod, BigDecimal amount) {
        if (!limitReservationService.isEnabled() || orderId == null) {
            return selectAccount(available, rule, poolId, payMethod);
        }
        List<PayMerchantAccount> ordered = orderedReservationCandidates(available, rule, poolId, payMethod);
        for (PayMerchantAccount account : ordered) {
            if (limitReservationService.reserve(orderId, merchantOrderNo, account, amount)) {
                return account;
            }
        }
        throw new BizException(BusinessErrorCode.NO_AVAILABLE_ACCOUNT, "No merchant account has enough reserved limit");
    }

    private List<PayMerchantAccount> orderedReservationCandidates(List<PayMerchantAccount> available, PayRouteRule rule, Long poolId, String payMethod) {
        PayMerchantAccount first = selectAccount(available, rule, poolId, payMethod);
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(first),
                        available.stream()
                                .filter(account -> !account.getId().equals(first.getId()))
                                .sorted(Comparator.comparing((PayMerchantAccount account) -> account.getPriority() == null ? 100 : account.getPriority())
                                        .thenComparing(PayMerchantAccount::getId))
                )
                .toList();
    }

    private List<PayMerchantAccount> firstPriorityGroup(List<PayMerchantAccount> available) {
        int priority = available.stream().map(PayMerchantAccount::getPriority).filter(value -> value != null).min(Integer::compareTo).orElse(100);
        return available.stream()
                .filter(account -> (account.getPriority() == null ? 100 : account.getPriority()) == priority)
                .sorted(Comparator.comparing(PayMerchantAccount::getId))
                .toList();
    }

    private PayMerchantAccount roundRobin(List<PayMerchantAccount> accounts, Long poolId, String payMethod) {
        Long value = redisTemplate.opsForValue().increment("pay:route:round_robin:" + poolId + ":" + payMethod);
        int index = Math.floorMod(value == null ? 0 : value.intValue() - 1, accounts.size());
        return accounts.get(index);
    }

    private PayMerchantAccount weightedRandom(List<PayMerchantAccount> accounts) {
        int totalWeight = accounts.stream().mapToInt(account -> Math.max(account.getWeight() == null ? 1 : account.getWeight(), 1)).sum();
        int cursor = random.nextInt(totalWeight);
        for (PayMerchantAccount account : accounts) {
            cursor -= Math.max(account.getWeight() == null ? 1 : account.getWeight(), 1);
            if (cursor < 0) {
                return account;
            }
        }
        return accounts.get(0);
    }

    private boolean isAvailable(PayMerchantAccount account, String payMethod, BigDecimal amount) {
        return "ENABLED".equals(account.getStatus())
                && supportsPayMethod(account, payMethod)
                && inDateRange(account)
                && inTimeRange(account)
                && inSingleAmountRange(account, amount)
                && withinLimit(account, amount, true)
                && withinLimit(account, amount, false)
                && !isCircuitBreaking(account.getId());
    }

    private boolean supportsPayMethod(PayMerchantAccount account, String payMethod) {
        return StringUtils.hasText(account.getSupportPayMethods())
                && List.of(account.getSupportPayMethods().split(",")).stream()
                .map(String::trim)
                .anyMatch(item -> "ALL".equals(item) || payMethod.equals(item));
    }

    private boolean inDateRange(PayMerchantAccount account) {
        LocalDate today = LocalDate.now();
        return (account.getAvailableStartDate() == null || !today.isBefore(account.getAvailableStartDate()))
                && (account.getAvailableEndDate() == null || !today.isAfter(account.getAvailableEndDate()));
    }

    private boolean inTimeRange(PayMerchantAccount account) {
        if (account.getAvailableStartTime() == null || account.getAvailableEndTime() == null) {
            return true;
        }
        LocalTime now = LocalTime.now();
        LocalTime start = account.getAvailableStartTime();
        LocalTime end = account.getAvailableEndTime();
        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }
        return !now.isBefore(start) || !now.isAfter(end);
    }

    private boolean inSingleAmountRange(PayMerchantAccount account, BigDecimal amount) {
        return (account.getSingleMinAmount() == null || amount.compareTo(account.getSingleMinAmount()) >= 0)
                && (account.getSingleMaxAmount() == null || amount.compareTo(account.getSingleMaxAmount()) <= 0);
    }

    private boolean withinLimit(PayMerchantAccount account, BigDecimal amount, boolean daily) {
        BigDecimal limit = daily ? account.getDailyAmountLimit() : account.getMonthlyAmountLimit();
        if (limit == null) {
            return true;
        }
        LocalDateTime startAt = daily ? LocalDate.now().atStartOfDay() : LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endAt = daily ? startAt.plusDays(1) : startAt.plusMonths(1);
        BigDecimal used = orderMapper.sumSuccessAmount(account.getId(), startAt, endAt);
        return used.add(amount).compareTo(limit) <= 0;
    }

    private boolean isCircuitBreaking(Long accountId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(circuitBreakKey(accountId)));
    }

    private boolean ruleMatchesNow(PayRouteRule rule) {
        if (!RouteRuleService.TIME_RANGE.equals(rule.getRuleType()) || !StringUtils.hasText(rule.getRuleConfigJson())) {
            return true;
        }
        JsonNode config = readConfig(rule);
        String startText = config.path("startTime").asText("");
        String endText = config.path("endTime").asText("");
        if (!StringUtils.hasText(startText) || !StringUtils.hasText(endText)) {
            return true;
        }
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse(startText);
        LocalTime end = LocalTime.parse(endText);
        return start.isBefore(end) ? (!now.isBefore(start) && !now.isAfter(end)) : (!now.isBefore(start) || !now.isAfter(end));
    }

    private PayRouteRecord saveRecord(Long orderId, String merchantOrderNo, PayMerchantPool pool, PayMerchantAccount account, PayRouteRule rule, BigDecimal amount, List<PayMerchantAccount> available) {
        PayRouteRecord record = new PayRouteRecord();
        record.setTenantId(pool.getTenantId());
        record.setOrderId(orderId);
        record.setMerchantOrderNo(StringUtils.hasText(merchantOrderNo) ? merchantOrderNo : "TEST" + System.currentTimeMillis());
        record.setPoolId(pool.getId());
        record.setAccountId(account.getId());
        record.setRouteRuleId(rule == null ? null : rule.getId());
        record.setRouteType(rule == null ? RouteRuleService.ROUND_ROBIN : rule.getRuleType());
        record.setRouteSnapshotJson(snapshot(account, rule, available));
        record.setAmount(amount);
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);
        return record;
    }

    private String snapshot(PayMerchantAccount account, PayRouteRule rule, List<PayMerchantAccount> available) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "accountId", account.getId(),
                    "accountName", account.getAccountName(),
                    "routeRuleId", rule == null ? "" : rule.getId(),
                    "routeType", rule == null ? RouteRuleService.ROUND_ROBIN : rule.getRuleType(),
                    "availableAccountIds", available.stream().map(PayMerchantAccount::getId).toList()
            ));
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private RouteRecordResponse toRecordResponse(PayRouteRecord record) {
        PayMerchantPool pool = poolService.requirePool(record.getPoolId());
        PayMerchantAccount account = accountMapper.selectById(record.getAccountId());
        PayRouteRule rule = record.getRouteRuleId() == null ? null : ruleMapper.selectById(record.getRouteRuleId());
        return new RouteRecordResponse(
                record.getId(),
                record.getTenantId(),
                record.getOrderId(),
                record.getMerchantOrderNo(),
                record.getPoolId(),
                pool.getPoolName(),
                record.getAccountId(),
                account == null ? null : account.getAccountName(),
                record.getRouteRuleId(),
                rule == null ? null : rule.getRuleName(),
                record.getRouteType(),
                record.getRouteSnapshotJson(),
                record.getAmount(),
                record.getCreatedAt()
        );
    }

    private int failThreshold(PayRouteRule rule) {
        if (rule == null || !RouteRuleService.FAILOVER.equals(rule.getRuleType())) {
            return DEFAULT_FAIL_THRESHOLD;
        }
        return readConfig(rule).path("failThreshold").asInt(DEFAULT_FAIL_THRESHOLD);
    }

    private int breakMinutes(PayRouteRule rule) {
        if (rule == null || !RouteRuleService.FAILOVER.equals(rule.getRuleType())) {
            return DEFAULT_BREAK_MINUTES;
        }
        return readConfig(rule).path("breakMinutes").asInt(DEFAULT_BREAK_MINUTES);
    }

    private JsonNode readConfig(PayRouteRule rule) {
        try {
            return objectMapper.readTree(StringUtils.hasText(rule.getRuleConfigJson()) ? rule.getRuleConfigJson() : "{}");
        } catch (JsonProcessingException e) {
            throw new BizException(BusinessErrorCode.ROUTE_FAILED, "Invalid route rule config JSON");
        }
    }

    private String circuitBreakKey(Long accountId) {
        return "pay:route:circuit_break:" + accountId;
    }

    public record RouteDecision(PayMerchantAccount account, PayRouteRule rule, PayRouteRecord record) {
    }
}
