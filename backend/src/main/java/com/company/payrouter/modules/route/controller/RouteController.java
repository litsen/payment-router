package com.company.payrouter.modules.route.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.PayResponse;
import com.company.payrouter.modules.order.service.PaymentGatewayService;
import com.company.payrouter.modules.route.dto.RouteDtos.RoutePayTestRequest;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteQueryTestRequest;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteRecordResponse;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteResultResponse;
import com.company.payrouter.modules.route.dto.RouteDtos.RouteTestRequest;
import com.company.payrouter.modules.route.service.RouteEngineService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class RouteController {
    private final RouteEngineService routeEngineService;
    private final PaymentGatewayService paymentGatewayService;

    public RouteController(RouteEngineService routeEngineService, PaymentGatewayService paymentGatewayService) {
        this.routeEngineService = routeEngineService;
        this.paymentGatewayService = paymentGatewayService;
    }

    @PostMapping("/route/test")
    public ApiResult<RouteResultResponse> test(@Valid @RequestBody RouteTestRequest request) {
        return ApiResult.success(routeEngineService.testRoute(request));
    }

    @PostMapping("/route/pay-test")
    public ApiResult<PayResponse> payTest(@Valid @RequestBody RoutePayTestRequest request) {
        return ApiResult.success(paymentGatewayService.debugBarcodePay(request));
    }

    @PostMapping("/route/query-test")
    public ApiResult<PayResponse> queryTest(@Valid @RequestBody RouteQueryTestRequest request) {
        return ApiResult.success(paymentGatewayService.debugQueryPay(request));
    }

    @GetMapping("/route-records")
    public ApiResult<PageResult<RouteRecordResponse>> records(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String merchantOrderNo,
            @RequestParam(required = false) Long poolId,
            @RequestParam(required = false) Long accountId
    ) {
        return ApiResult.success(routeEngineService.pageRecords(current, size, merchantOrderNo, poolId, accountId));
    }
}
