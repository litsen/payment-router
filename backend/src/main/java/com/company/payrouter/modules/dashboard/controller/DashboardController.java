package com.company.payrouter.modules.dashboard.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.modules.dashboard.dto.DashboardDtos.AccountStatsResponse;
import com.company.payrouter.modules.dashboard.dto.DashboardDtos.DashboardSummaryResponse;
import com.company.payrouter.modules.dashboard.dto.DashboardDtos.HourlyTrendResponse;
import com.company.payrouter.modules.dashboard.dto.DashboardDtos.StatusDistributionResponse;
import com.company.payrouter.modules.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResult<DashboardSummaryResponse> summary() {
        return ApiResult.success(dashboardService.summary());
    }

    @GetMapping("/hourly-trend")
    public ApiResult<List<HourlyTrendResponse>> hourlyTrend() {
        return ApiResult.success(dashboardService.hourlyTrend());
    }

    @GetMapping("/account-stats")
    public ApiResult<List<AccountStatsResponse>> accountStats() {
        return ApiResult.success(dashboardService.accountStats());
    }

    @GetMapping("/status-distribution")
    public ApiResult<List<StatusDistributionResponse>> statusDistribution() {
        return ApiResult.success(dashboardService.statusDistribution());
    }
}
