package com.company.payrouter.modules.route.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.payrouter.modules.route.entity.PayAccountLimitBucket;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface PayAccountLimitBucketMapper extends BaseMapper<PayAccountLimitBucket> {
    @Insert("""
            INSERT IGNORE INTO pay_account_limit_bucket (
                tenant_id, account_id, period_type, period_start, period_end,
                limit_amount, used_amount, reserved_amount, version, created_at, updated_at
            ) VALUES (
                #{tenantId}, #{accountId}, #{periodType}, #{periodStart}, #{periodEnd},
                #{limitAmount},
                (
                    SELECT COALESCE(SUM(amount), 0)
                    FROM pay_order
                    WHERE account_id = #{accountId}
                      AND status = 'SUCCESS'
                      AND pay_success_time >= #{periodStart}
                      AND pay_success_time < #{periodEnd}
                ),
                0, 0, NOW(), NOW()
            )
            """)
    int insertIgnoreBucket(
            @Param("tenantId") String tenantId,
            @Param("accountId") Long accountId,
            @Param("periodType") String periodType,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd,
            @Param("limitAmount") BigDecimal limitAmount
    );

    @Select("""
            SELECT *
            FROM pay_account_limit_bucket
            WHERE account_id = #{accountId}
              AND period_type = #{periodType}
              AND period_start = #{periodStart}
            LIMIT 1
            """)
    PayAccountLimitBucket selectBucket(
            @Param("accountId") Long accountId,
            @Param("periodType") String periodType,
            @Param("periodStart") LocalDateTime periodStart
    );

    @Update("""
            UPDATE pay_account_limit_bucket
            SET limit_amount = #{limitAmount},
                reserved_amount = reserved_amount + #{amount},
                version = version + 1,
                updated_at = NOW()
            WHERE id = #{bucketId}
              AND used_amount + reserved_amount + #{amount} <= #{limitAmount}
            """)
    int reserve(
            @Param("bucketId") Long bucketId,
            @Param("amount") BigDecimal amount,
            @Param("limitAmount") BigDecimal limitAmount
    );

    @Update("""
            UPDATE pay_account_limit_bucket
            SET reserved_amount = reserved_amount - #{amount},
                used_amount = used_amount + #{amount},
                version = version + 1,
                updated_at = NOW()
            WHERE id = #{bucketId}
              AND reserved_amount >= #{amount}
            """)
    int confirm(@Param("bucketId") Long bucketId, @Param("amount") BigDecimal amount);

    @Update("""
            UPDATE pay_account_limit_bucket
            SET reserved_amount = reserved_amount - #{amount},
                version = version + 1,
                updated_at = NOW()
            WHERE id = #{bucketId}
              AND reserved_amount >= #{amount}
            """)
    int release(@Param("bucketId") Long bucketId, @Param("amount") BigDecimal amount);
}
