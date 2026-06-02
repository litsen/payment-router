package com.company.payrouter.modules.route.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteRuleCreateRequest;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteRuleResponse;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteRuleUpdateRequest;
import com.company.payrouter.modules.route.service.RouteRuleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/route-rules")
public class RouteRuleController {
    private final RouteRuleService routeRuleService;

    public RouteRuleController(RouteRuleService routeRuleService) {
        this.routeRuleService = routeRuleService;
    }

    @GetMapping
    public ApiResult<PageResult<RouteRuleResponse>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long poolId,
            @RequestParam(required = false) String payMethod,
            @RequestParam(required = false) Boolean enabled
    ) {
        return ApiResult.success(routeRuleService.pageRules(current, size, keyword, poolId, payMethod, enabled));
    }

    @PostMapping
    public ApiResult<RouteRuleResponse> create(@Valid @RequestBody RouteRuleCreateRequest request) {
        return ApiResult.success(routeRuleService.createRule(request));
    }

    @PutMapping("/{id}")
    public ApiResult<RouteRuleResponse> update(@PathVariable Long id, @Valid @RequestBody RouteRuleUpdateRequest request) {
        return ApiResult.success(routeRuleService.updateRule(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        routeRuleService.deleteRule(id);
        return ApiResult.success();
    }

    @PostMapping("/{id}/enable")
    public ApiResult<RouteRuleResponse> enable(@PathVariable Long id) {
        return ApiResult.success(routeRuleService.enableRule(id));
    }

    @PostMapping("/{id}/disable")
    public ApiResult<RouteRuleResponse> disable(@PathVariable Long id) {
        return ApiResult.success(routeRuleService.disableRule(id));
    }
}
