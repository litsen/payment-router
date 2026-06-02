package com.company.payrouter.modules.dashboard.service;

import com.company.payrouter.modules.dashboard.dto.DashboardDtos.AccountStatsResponse;
import com.company.payrouter.modules.dashboard.dto.DashboardDtos.DashboardSummaryResponse;
import com.company.payrouter.modules.dashboard.dto.DashboardDtos.HourlyTrendResponse;
import com.company.payrouter.modules.dashboard.dto.DashboardDtos.StatusDistributionResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DashboardService {
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:00");

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    public DashboardSummaryResponse summary() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                    COALESCE(SUM(amount), 0) AS total_amount,
                    COUNT(*) AS total_count,
                    SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
                    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count,
                    SUM(CASE WHEN status = 'UNKNOWN' THEN 1 ELSE 0 END) AS unknown_count
                FROM pay_order
                WHERE created_at >= ? AND created_at < ?
                """, start, end);
        long totalCount = asLong(row.get("total_count"));
        long successCount = asLong(row.get("success_count"));
        return new DashboardSummaryResponse(
                asBigDecimal(row.get("total_amount")),
                totalCount,
                successCount,
                asLong(row.get("failed_count")),
                rate(successCount, totalCount),
                asLong(row.get("unknown_count")),
                availableAccountCount(),
                circuitBrokenAccountCount()
        );
    }

    public List<HourlyTrendResponse> hourlyTrend() {
        LocalDateTime end = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1);
        LocalDateTime start = end.minusHours(24);
        Map<String, HourlyTrendResponse> values = new HashMap<>();
        jdbcTemplate.query("""
                SELECT
                    DATE_FORMAT(created_at, '%Y-%m-%d %H:00:00') AS hour_key,
                    COALESCE(SUM(amount), 0) AS total_amount,
                    COUNT(*) AS total_count,
                    SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
                    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count,
                    SUM(CASE WHEN status = 'UNKNOWN' THEN 1 ELSE 0 END) AS unknown_count
                FROM pay_order
                WHERE created_at >= ? AND created_at < ?
                GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d %H:00:00')
                ORDER BY hour_key
                """, rs -> {
            String hourKey = rs.getString("hour_key");
            LocalDateTime hour = Timestamp.valueOf(hourKey).toLocalDateTime();
            values.put(hourKey, new HourlyTrendResponse(
                    hour.format(HOUR_FORMATTER),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("total_count"),
                    rs.getLong("success_count"),
                    rs.getLong("failed_count"),
                    rs.getLong("unknown_count")
            ));
        }, start, end);

        List<HourlyTrendResponse> result = new ArrayList<>();
        for (LocalDateTime cursor = start; cursor.isBefore(end); cursor = cursor.plusHours(1)) {
            String key = cursor.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00:00"));
            result.add(values.getOrDefault(key, new HourlyTrendResponse(
                    cursor.format(HOUR_FORMATTER),
                    BigDecimal.ZERO,
                    0,
                    0,
                    0,
                    0
            )));
        }
        return result;
    }

    public List<AccountStatsResponse> accountStats() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return jdbcTemplate.query("""
                SELECT
                    o.account_id,
                    COALESCE(a.account_name, '未路由') AS account_name,
                    o.pool_id,
                    COALESCE(p.pool_name, '-') AS pool_name,
                    COALESCE(SUM(o.amount), 0) AS total_amount,
                    COUNT(*) AS total_count,
                    SUM(CASE WHEN o.status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
                    SUM(CASE WHEN o.status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count
                FROM pay_order o
                LEFT JOIN pay_merchant_account a ON a.id = o.account_id
                LEFT JOIN pay_merchant_pool p ON p.id = o.pool_id
                WHERE o.created_at >= ? AND o.created_at < ?
                GROUP BY o.account_id, a.account_name, o.pool_id, p.pool_name
                ORDER BY total_amount DESC, total_count DESC
                """, (rs, rowNum) -> {
            long totalCount = rs.getLong("total_count");
            long successCount = rs.getLong("success_count");
            return new AccountStatsResponse(
                    rs.getObject("account_id", Long.class),
                    rs.getString("account_name"),
                    rs.getObject("pool_id", Long.class),
                    rs.getString("pool_name"),
                    rs.getBigDecimal("total_amount"),
                    totalCount,
                    successCount,
                    rs.getLong("failed_count"),
                    rate(successCount, totalCount)
            );
        }, start, end);
    }

    public List<StatusDistributionResponse> statusDistribution() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return jdbcTemplate.query("""
                SELECT status, COUNT(*) AS total_count, COALESCE(SUM(amount), 0) AS total_amount
                FROM pay_order
                WHERE created_at >= ? AND created_at < ?
                GROUP BY status
                ORDER BY total_count DESC
                """, (rs, rowNum) -> new StatusDistributionResponse(
                rs.getString("status"),
                rs.getLong("total_count"),
                rs.getBigDecimal("total_amount")
        ), start, end);
    }

    private long availableAccountCount() {
        Set<String> brokenKeys = redisTemplate.keys("pay:route:circuit_break:*");
        return jdbcTemplate.queryForList(
                        "SELECT id FROM pay_merchant_account WHERE status = 'ENABLED'",
                        Long.class
                ).stream()
                .filter(accountId -> brokenKeys == null || !brokenKeys.contains("pay:route:circuit_break:" + accountId))
                .count();
    }

    private long circuitBrokenAccountCount() {
        Set<String> keys = redisTemplate.keys("pay:route:circuit_break:*");
        return keys == null ? 0 : keys.size();
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }
}
