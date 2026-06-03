package com.company.payrouter.modules.dashboard.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.modules.dashboard.dto.DashboardDtos.AccountStatsResponse;
import com.company.payrouter.modules.dashboard.dto.DashboardDtos.DashboardSummaryResponse;
import com.company.payrouter.modules.dashboard.dto.DashboardDtos.HourlyTrendResponse;
import com.company.payrouter.modules.dashboard.dto.DashboardDtos.StatusDistributionResponse;
import com.company.payrouter.modules.dashboard.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResult<DashboardSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.success(dashboardService.summary(startDate, endDate));
    }

    @GetMapping("/hourly-trend")
    public ApiResult<List<HourlyTrendResponse>> hourlyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.success(dashboardService.hourlyTrend(startDate, endDate));
    }

    @GetMapping("/account-stats")
    public ApiResult<List<AccountStatsResponse>> accountStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.success(dashboardService.accountStats(startDate, endDate));
    }

    @GetMapping("/status-distribution")
    public ApiResult<List<StatusDistributionResponse>> statusDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.success(dashboardService.statusDistribution(startDate, endDate));
    }
}
