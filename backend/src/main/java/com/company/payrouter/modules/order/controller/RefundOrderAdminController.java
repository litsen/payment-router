package com.company.payrouter.modules.order.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.modules.gateway.dto.GatewayDtos.RefundResponse;
import com.company.payrouter.modules.order.dto.OrderDtos.RefundOrderResponse;
import com.company.payrouter.modules.order.service.RefundOrderAdminService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/refunds")
public class RefundOrderAdminController {
    private final RefundOrderAdminService refundOrderAdminService;

    public RefundOrderAdminController(RefundOrderAdminService refundOrderAdminService) {
        this.refundOrderAdminService = refundOrderAdminService;
    }

    @GetMapping
    public ApiResult<PageResult<RefundOrderResponse>> refunds(@RequestParam(defaultValue = "1") long current,
                                                              @RequestParam(defaultValue = "10") long size,
                                                              String merchantOrderNo,
                                                              String merchantRefundNo,
                                                              String platformOrderNo,
                                                              String upstreamRefundNo,
                                                              String status,
                                                              Long poolId,
                                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResult.success(refundOrderAdminService.pageRefunds(current, size, merchantOrderNo, merchantRefundNo,
                platformOrderNo, upstreamRefundNo, status, poolId, startTime, endTime));
    }

    @GetMapping("/{id}")
    public ApiResult<RefundOrderResponse> refund(@PathVariable Long id) {
        return ApiResult.success(refundOrderAdminService.getRefund(id));
    }

    @PostMapping("/{id}/query")
    public ApiResult<RefundResponse> queryRefund(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "false") boolean force) {
        return ApiResult.success(refundOrderAdminService.queryRefund(id, force));
    }
}
