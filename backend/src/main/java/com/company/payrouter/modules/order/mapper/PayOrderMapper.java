package com.company.payrouter.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.payrouter.modules.order.entity.PayOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface PayOrderMapper extends BaseMapper<PayOrder> {
    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM pay_order
            WHERE account_id = #{accountId}
              AND status = 'SUCCESS'
              AND pay_success_time >= #{startAt}
              AND pay_success_time < #{endAt}
            """)
    BigDecimal sumSuccessAmount(@Param("accountId") Long accountId, @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);
}
