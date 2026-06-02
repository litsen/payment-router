package com.company.payrouter.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.payrouter.modules.order.entity.PayRefundOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayRefundOrderMapper extends BaseMapper<PayRefundOrder> {
}
