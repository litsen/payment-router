package com.company.payrouter.modules.route.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.modules.merchant.entity.PayMerchantPool;
import com.company.payrouter.modules.merchant.service.MerchantPoolService;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteRuleCreateRequest;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteRuleResponse;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteRuleUpdateRequest;
import com.company.payrouter.modules.route.entity.PayRouteRule;
import com.company.payrouter.modules.route.mapper.PayRouteRuleMapper;
import com.company.payrouter.modules.system.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class RouteRuleService {
    public static final String ROUND_ROBIN = "ROUND_ROBIN";
    public static final String WEIGHT_RANDOM = "WEIGHT_RANDOM";
    public static final String TIME_RANGE = "TIME_RANGE";
    public static final String LIMIT_AMOUNT = "LIMIT_AMOUNT";
    public static final String FAILOVER = "FAILOVER";

    private static final Set<String> SUPPORTED_TYPES = Set.of(ROUND_ROBIN, WEIGHT_RANDOM, TIME_RANGE, LIMIT_AMOUNT, FAILOVER);

    private final PayRouteRuleMapper ruleMapper;
    private final MerchantPoolService poolService;
    private final OperationLogService operationLogService;

    public RouteRuleService(PayRouteRuleMapper ruleMapper, MerchantPoolService poolService, OperationLogService operationLogService) {
        this.ruleMapper = ruleMapper;
        this.poolService = poolService;
        this.operationLogService = operationLogService;
    }

    public PageResult<RouteRuleResponse> pageRules(long current, long size, String keyword, Long poolId, String payMethod, Boolean enabled) {
        LambdaQueryWrapper<PayRouteRule> wrapper = new LambdaQueryWrapper<PayRouteRule>()
                .orderByAsc(PayRouteRule::getPriority)
                .orderByDesc(PayRouteRule::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(PayRouteRule::getRuleName, keyword).or().like(PayRouteRule::getRuleCode, keyword));
        }
        if (poolId != null) {
            wrapper.eq(PayRouteRule::getPoolId, poolId);
        }
        if (StringUtils.hasText(payMethod)) {
            wrapper.and(query -> query.eq(PayRouteRule::getPayMethod, "ALL")
                    .or()
                    .eq(PayRouteRule::getPayMethod, payMethod)
                    .or()
                    .like(PayRouteRule::getPayMethod, payMethod));
        }
        if (enabled != null) {
            wrapper.eq(PayRouteRule::getEnabled, enabled);
        }
        Page<PayRouteRule> page = ruleMapper.selectPage(Page.of(current, size), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toResponse).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public RouteRuleResponse createRule(RouteRuleCreateRequest request) {
        PayMerchantPool pool = poolService.requirePool(request.poolId());
        validateRuleType(request.ruleType());
        assertNoOtherRuleForPool(request.poolId(), null);
        PayRouteRule rule = new PayRouteRule();
        rule.setTenantId(StringUtils.hasText(request.tenantId()) ? request.tenantId() : pool.getTenantId());
        rule.setRuleName(request.ruleName());
        rule.setRuleCode(StringUtils.hasText(request.ruleCode()) ? request.ruleCode() : "ROUTE" + System.currentTimeMillis());
        rule.setPoolId(request.poolId());
        rule.setPayMethod(request.payMethod());
        rule.setRuleType(request.ruleType());
        rule.setRuleConfigJson(request.ruleConfigJson());
        rule.setPriority(request.priority() == null ? 100 : request.priority());
        rule.setEnabled(request.enabled() == null || request.enabled());
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.insert(rule);
        operationLogService.record("CREATE", "ROUTE_RULE", rule.getId(), "Create route rule " + rule.getRuleName());
        return toResponse(rule);
    }

    @Transactional
    public RouteRuleResponse updateRule(Long id, RouteRuleUpdateRequest request) {
        validateRuleType(request.ruleType());
        PayRouteRule rule = requireRule(id);
        poolService.requirePool(request.poolId());
        assertNoOtherRuleForPool(request.poolId(), id);
        rule.setRuleName(request.ruleName());
        rule.setPoolId(request.poolId());
        rule.setPayMethod(request.payMethod());
        rule.setRuleType(request.ruleType());
        rule.setRuleConfigJson(request.ruleConfigJson());
        rule.setPriority(request.priority() == null ? 100 : request.priority());
        rule.setEnabled(request.enabled() == null || request.enabled());
        rule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.updateById(rule);
        operationLogService.record("UPDATE", "ROUTE_RULE", id, "Update route rule " + rule.getRuleName());
        return toResponse(rule);
    }

    @Transactional
    public void deleteRule(Long id) {
        PayRouteRule rule = requireRule(id);
        ruleMapper.deleteById(id);
        operationLogService.record("DELETE", "ROUTE_RULE", id, "Delete route rule " + rule.getRuleName());
    }

    @Transactional
    public RouteRuleResponse enableRule(Long id) {
        return changeEnabled(id, true);
    }

    @Transactional
    public RouteRuleResponse disableRule(Long id) {
        return changeEnabled(id, false);
    }

    public PayRouteRule requireRule(Long id) {
        PayRouteRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BizException("Route rule does not exist");
        }
        return rule;
    }

    private RouteRuleResponse changeEnabled(Long id, boolean enabled) {
        PayRouteRule rule = requireRule(id);
        rule.setEnabled(enabled);
        rule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.updateById(rule);
        operationLogService.record(enabled ? "ENABLE" : "DISABLE", "ROUTE_RULE", id, (enabled ? "Enable " : "Disable ") + rule.getRuleName());
        return toResponse(rule);
    }

    private void validateRuleType(String ruleType) {
        if (!SUPPORTED_TYPES.contains(ruleType)) {
            throw new BizException("Unsupported route rule type");
        }
    }

    private void assertNoOtherRuleForPool(Long poolId, Long currentRuleId) {
        LambdaQueryWrapper<PayRouteRule> wrapper = new LambdaQueryWrapper<PayRouteRule>()
                .eq(PayRouteRule::getPoolId, poolId);
        if (currentRuleId != null) {
            wrapper.ne(PayRouteRule::getId, currentRuleId);
        }
        Long count = ruleMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException("One merchant can only configure one route rule");
        }
    }

    private RouteRuleResponse toResponse(PayRouteRule rule) {
        PayMerchantPool pool = poolService.requirePool(rule.getPoolId());
        return new RouteRuleResponse(
                rule.getId(),
                rule.getTenantId(),
                rule.getRuleName(),
                rule.getRuleCode(),
                rule.getPoolId(),
                pool.getPoolName(),
                rule.getPayMethod(),
                rule.getRuleType(),
                rule.getRuleConfigJson(),
                rule.getPriority(),
                rule.getEnabled(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}
