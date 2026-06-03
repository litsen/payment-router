package com.company.payrouter.modules.dashboard.service;

import com.company.payrouter.common.exception.BizException;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DashboardService {
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:00");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter HOUR_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00:00");
    private static final DateTimeFormatter DAY_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_RANGE_DAYS = 31;

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    public DashboardSummaryResponse summary(LocalDate startDate, LocalDate endDate) {
        DateRange range = normalizeRange(startDate, endDate);
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                    COALESCE(SUM(amount), 0) AS total_amount,
                    COUNT(*) AS total_count,
                    SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
                    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count,
                    SUM(CASE WHEN status = 'UNKNOWN' THEN 1 ELSE 0 END) AS unknown_count
                FROM pay_order
                WHERE created_at >= ? AND created_at < ?
                """, range.start(), range.end());
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

    public List<HourlyTrendResponse> hourlyTrend(LocalDate startDate, LocalDate endDate) {
        DateRange range = normalizeRange(startDate, endDate);
        boolean groupByDay = range.days() > 2;
        String groupExpression = groupByDay
                ? "DATE_FORMAT(created_at, '%Y-%m-%d')"
                : "DATE_FORMAT(created_at, '%Y-%m-%d %H:00:00')";
        Map<String, HourlyTrendResponse> values = new HashMap<>();
        jdbcTemplate.query("""
                SELECT
                    %s AS trend_key,
                    COALESCE(SUM(amount), 0) AS total_amount,
                    COUNT(*) AS total_count,
                    SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
                    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count,
                    SUM(CASE WHEN status = 'UNKNOWN' THEN 1 ELSE 0 END) AS unknown_count
                FROM pay_order
                WHERE created_at >= ? AND created_at < ?
                GROUP BY %s
                ORDER BY trend_key
                """.formatted(groupExpression, groupExpression), rs -> {
            String trendKey = rs.getString("trend_key");
            values.put(trendKey, new HourlyTrendResponse(
                    trendLabel(trendKey, groupByDay),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("total_count"),
                    rs.getLong("success_count"),
                    rs.getLong("failed_count"),
                    rs.getLong("unknown_count")
            ));
        }, range.start(), range.end());

        List<HourlyTrendResponse> result = new ArrayList<>();
        if (groupByDay) {
            for (LocalDate cursor = range.start().toLocalDate(); cursor.isBefore(range.end().toLocalDate()); cursor = cursor.plusDays(1)) {
                String key = cursor.format(DAY_KEY_FORMATTER);
                result.add(values.getOrDefault(key, emptyTrend(cursor.format(DAY_FORMATTER))));
            }
        } else {
            for (LocalDateTime cursor = range.start(); cursor.isBefore(range.end()); cursor = cursor.plusHours(1)) {
                String key = cursor.format(HOUR_KEY_FORMATTER);
                result.add(values.getOrDefault(key, emptyTrend(cursor.format(HOUR_FORMATTER))));
            }
        }
        return result;
    }

    public List<AccountStatsResponse> accountStats(LocalDate startDate, LocalDate endDate) {
        DateRange range = normalizeRange(startDate, endDate);
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
        }, range.start(), range.end());
    }

    public List<StatusDistributionResponse> statusDistribution(LocalDate startDate, LocalDate endDate) {
        DateRange range = normalizeRange(startDate, endDate);
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
        ), range.start(), range.end());
    }

    private DateRange normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now() : startDate;
        LocalDate end = endDate == null ? start : endDate;
        if (end.isBefore(start)) {
            throw new BizException("结束日期不能早于开始日期");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new BizException("首页看板最多支持查询 31 天数据");
        }
        return new DateRange(start.atStartOfDay(), end.plusDays(1).atStartOfDay(), days);
    }

    private String trendLabel(String trendKey, boolean groupByDay) {
        if (groupByDay) {
            return LocalDate.parse(trendKey, DAY_KEY_FORMATTER).format(DAY_FORMATTER);
        }
        return Timestamp.valueOf(trendKey).toLocalDateTime().format(HOUR_FORMATTER);
    }

    private HourlyTrendResponse emptyTrend(String label) {
        return new HourlyTrendResponse(label, BigDecimal.ZERO, 0, 0, 0, 0);
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

    private record DateRange(LocalDateTime start, LocalDateTime end, long days) {
    }
}
