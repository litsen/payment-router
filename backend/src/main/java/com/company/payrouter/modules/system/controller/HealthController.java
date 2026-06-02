package com.company.payrouter.modules.system.controller;

import com.company.payrouter.common.api.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Health")
@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public HealthController(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Operation(summary = "Health check")
    @GetMapping
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "payment-router-backend");
        status.put("status", "UP");
        status.put("mysql", checkMysql());
        status.put("redis", checkRedis());
        return ApiResult.success(status);
    }

    private String checkMysql() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return Integer.valueOf(1).equals(result) ? "UP" : "DOWN";
    }

    private String checkRedis() {
        String pong = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
        return "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN";
    }
}
