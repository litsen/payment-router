package com.company.payrouter.modules.dashboard.dto;

import java.math.BigDecimal;

public class DashboardDtos {
    public record DashboardSummaryResponse(
            BigDecimal todayAmount,
            long todayOrderCount,
            long todaySuccessCount,
            long todayFailedCount,
            BigDecimal todaySuccessRate,
            long todayUnknownCount,
            long availableAccountCount,
            long circuitBrokenAccountCount
    ) {
    }

    public record HourlyTrendResponse(
            String hour,
            BigDecimal amount,
            long orderCount,
            long successCount,
            long failedCount,
            long unknownCount
    ) {
    }

    public record AccountStatsResponse(
            Long accountId,
            String accountName,
            Long poolId,
            String poolName,
            BigDecimal amount,
            long orderCount,
            long successCount,
            long failedCount,
            BigDecimal successRate
    ) {
    }

    public record StatusDistributionResponse(
            String status,
            long count,
            BigDecimal amount
    ) {
    }
}
