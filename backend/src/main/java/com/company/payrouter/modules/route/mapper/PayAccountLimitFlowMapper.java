package com.company.payrouter.modules.route.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.payrouter.modules.route.entity.PayAccountLimitFlow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PayAccountLimitFlowMapper extends BaseMapper<PayAccountLimitFlow> {
    @Insert("""
            INSERT IGNORE INTO pay_account_limit_flow (
                tenant_id, order_id, merchant_order_no, account_id, bucket_id,
                period_type, period_start, amount, flow_status, reserved_at, created_at, updated_at
            ) VALUES (
                #{tenantId}, #{orderId}, #{merchantOrderNo}, #{accountId}, #{bucketId},
                #{periodType}, #{periodStart}, #{amount}, 'RESERVED', #{reservedAt}, NOW(), NOW()
            )
            """)
    int insertIgnoreFlow(
            @Param("tenantId") String tenantId,
            @Param("orderId") Long orderId,
            @Param("merchantOrderNo") String merchantOrderNo,
            @Param("accountId") Long accountId,
            @Param("bucketId") Long bucketId,
            @Param("periodType") String periodType,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("amount") BigDecimal amount,
            @Param("reservedAt") LocalDateTime reservedAt
    );

    @Select("""
            SELECT *
            FROM pay_account_limit_flow
            WHERE order_id = #{orderId}
            ORDER BY id ASC
            """)
    List<PayAccountLimitFlow> selectByOrderId(@Param("orderId") Long orderId);

    @Select("""
            SELECT *
            FROM pay_account_limit_flow
            WHERE flow_status = 'RESERVED'
              AND reserved_at < #{reservedBefore}
            ORDER BY reserved_at ASC
            LIMIT #{limit}
            """)
    List<PayAccountLimitFlow> selectReservedBefore(@Param("reservedBefore") LocalDateTime reservedBefore, @Param("limit") int limit);

    @Update("""
            UPDATE pay_account_limit_flow
            SET flow_status = 'CONFIRMED',
                confirmed_at = NOW(),
                updated_at = NOW()
            WHERE id = #{id}
              AND flow_status = 'RESERVED'
            """)
    int markConfirmed(@Param("id") Long id);

    @Update("""
            UPDATE pay_account_limit_flow
            SET flow_status = 'RELEASED',
                released_at = NOW(),
                release_reason = #{reason},
                updated_at = NOW()
            WHERE id = #{id}
              AND flow_status = 'RESERVED'
            """)
    int markReleased(@Param("id") Long id, @Param("reason") String reason);
}
