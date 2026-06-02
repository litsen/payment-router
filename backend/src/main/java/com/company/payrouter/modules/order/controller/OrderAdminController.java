package com.company.payrouter.modules.order.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.PayResponse;
import com.company.payrouter.modules.order.dto.OrderDtos.NotifyLogResponse;
import com.company.payrouter.modules.order.dto.OrderDtos.OrderLogResponse;
import com.company.payrouter.modules.order.dto.OrderDtos.OrderResponse;
import com.company.payrouter.modules.order.service.OrderAdminService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin")
public class OrderAdminController {
    private final OrderAdminService orderAdminService;

    public OrderAdminController(OrderAdminService orderAdminService) {
        this.orderAdminService = orderAdminService;
    }

    @GetMapping("/orders")
    public ApiResult<PageResult<OrderResponse>> orders(@RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       String merchantOrderNo,
                                                       String platformOrderNo,
                                                       String channelOrderNo,
                                                       String payMethod,
                                                       String status,
                                                       Long poolId,
                                                       BigDecimal minAmount,
                                                       BigDecimal maxAmount,
                                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResult.success(orderAdminService.pageOrders(current, size, merchantOrderNo, platformOrderNo, channelOrderNo, payMethod, status, poolId, minAmount, maxAmount, startTime, endTime));
    }

    @GetMapping("/orders/{id}")
    public ApiResult<OrderResponse> order(@PathVariable Long id) {
        return ApiResult.success(orderAdminService.getOrder(id));
    }

    @PostMapping("/orders/{id}/query")
    public ApiResult<PayResponse> queryOrder(@PathVariable Long id,
                                             @RequestParam(defaultValue = "false") boolean force) {
        return ApiResult.success(orderAdminService.queryOrder(id, force));
    }

    @GetMapping("/order-logs")
    public ApiResult<PageResult<OrderLogResponse>> logs(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        Long orderId,
                                                        String merchantOrderNo,
                                                        String direction,
                                                        String apiType) {
        return ApiResult.success(orderAdminService.pageLogs(current, size, orderId, merchantOrderNo, direction, apiType));
    }

    @GetMapping("/order-logs/{id}")
    public ApiResult<OrderLogResponse> log(@PathVariable Long id) {
        return ApiResult.success(orderAdminService.getLog(id));
    }

    @GetMapping("/notify-logs")
    public ApiResult<PageResult<NotifyLogResponse>> notifyLogs(@RequestParam(defaultValue = "1") long current,
                                                               @RequestParam(defaultValue = "10") long size,
                                                               Long orderId,
                                                               String merchantOrderNo) {
        return ApiResult.success(orderAdminService.pageNotifyLogs(current, size, orderId, merchantOrderNo));
    }
}
