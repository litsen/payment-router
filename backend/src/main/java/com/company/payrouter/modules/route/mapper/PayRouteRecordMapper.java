package com.company.payrouter.modules.route.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.payrouter.modules.route.entity.PayRouteRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface PayRouteRecordMapper extends BaseMapper<PayRouteRecord> {
    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM pay_route_record
            WHERE account_id = #{accountId}
              AND created_at >= #{startAt}
              AND created_at < #{endAt}
            """)
    BigDecimal sumAmount(@Param("accountId") Long accountId, @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);
}
