package com.company.payrouter.modules.paymethod.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.payrouter.modules.paymethod.entity.PayMethod;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayMethodMapper extends BaseMapper<PayMethod> {
}
