package com.company.payrouter.modules.paymethod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.payrouter.common.exception.BusinessErrorCode;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.modules.paymethod.dto.PayMethodDtos.PayMethodResponse;
import com.company.payrouter.modules.paymethod.dto.PayMethodDtos.PayMethodUpdateRequest;
import com.company.payrouter.modules.paymethod.entity.PayMethod;
import com.company.payrouter.modules.paymethod.mapper.PayMethodMapper;
import com.company.payrouter.modules.system.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PayMethodService {
    public static final String PRE_ORDER = "PRE_ORDER";
    public static final String BARCODE_PAY = "BARCODE_PAY";
    public static final String DECODE_BAR = "DECODE_BAR";
    public static final String SCAN_PAY = "SCAN_PAY";
    public static final String QRCODE_PAY = "QRCODE_PAY";
    public static final String H5_PAY = "H5_PAY";
    public static final String WECHAT_JSAPI_PAY = "WECHAT_JSAPI_PAY";
    public static final String ALIPAY_JSAPI_PAY = "ALIPAY_JSAPI_PAY";
    private static final String LEGACY_JSAPI_PAY = "JSAPI_PAY";
    private static final Set<String> ENABLED_METHODS_IN_PHASE_ONE = Set.of(
            PRE_ORDER,
            BARCODE_PAY,
            DECODE_BAR,
            SCAN_PAY,
            QRCODE_PAY,
            H5_PAY,
            WECHAT_JSAPI_PAY,
            ALIPAY_JSAPI_PAY
    );

    private final PayMethodMapper payMethodMapper;
    private final OperationLogService operationLogService;

    public PayMethodService(PayMethodMapper payMethodMapper, OperationLogService operationLogService) {
        this.payMethodMapper = payMethodMapper;
        this.operationLogService = operationLogService;
    }

    public List<PayMethodResponse> listMethods() {
        return payMethodMapper.selectList(new LambdaQueryWrapper<PayMethod>().orderByAsc(PayMethod::getSortOrder).orderByAsc(PayMethod::getId))
                .stream()
                .filter(method -> !LEGACY_JSAPI_PAY.equals(method.getMethodCode()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PayMethodResponse updateMethod(Long id, PayMethodUpdateRequest request) {
        PayMethod method = requireMethod(id);
        if (Boolean.TRUE.equals(request.enabled()) && !canEnable(method.getMethodCode())) {
            throw new BizException(BusinessErrorCode.UNSUPPORTED_PAY_METHOD);
        }
        method.setMethodName(request.methodName());
        method.setEnabled(request.enabled());
        method.setSortOrder(request.sortOrder() == null ? method.getSortOrder() : request.sortOrder());
        method.setRemark(request.remark());
        method.setUpdatedAt(LocalDateTime.now());
        payMethodMapper.updateById(method);
        operationLogService.record("UPDATE", "PAY_METHOD", id, "Update payment method " + method.getMethodCode());
        return toResponse(method);
    }

    @Transactional
    public PayMethodResponse enableMethod(Long id) {
        PayMethod method = requireMethod(id);
        if (!canEnable(method.getMethodCode())) {
            throw new BizException(BusinessErrorCode.UNSUPPORTED_PAY_METHOD);
        }
        method.setEnabled(true);
        method.setUpdatedAt(LocalDateTime.now());
        payMethodMapper.updateById(method);
        operationLogService.record("ENABLE", "PAY_METHOD", id, "Enable payment method " + method.getMethodCode());
        return toResponse(method);
    }

    @Transactional
    public PayMethodResponse disableMethod(Long id) {
        PayMethod method = requireMethod(id);
        method.setEnabled(false);
        method.setUpdatedAt(LocalDateTime.now());
        payMethodMapper.updateById(method);
        operationLogService.record("DISABLE", "PAY_METHOD", id, "Disable payment method " + method.getMethodCode());
        return toResponse(method);
    }

    public void ensureEnabled(String methodCode) {
        PayMethod method = findByCode(methodCode);
        if (method == null || !Boolean.TRUE.equals(method.getEnabled())) {
            throw new BizException(BusinessErrorCode.PAY_METHOD_DISABLED, "Payment method " + methodCode + " is disabled");
        }
    }

    public PayMethod findByCode(String methodCode) {
        if (!StringUtils.hasText(methodCode)) {
            return null;
        }
        return payMethodMapper.selectOne(new LambdaQueryWrapper<PayMethod>().eq(PayMethod::getMethodCode, methodCode));
    }

    private PayMethod requireMethod(Long id) {
        PayMethod method = payMethodMapper.selectById(id);
        if (method == null) {
            throw new BizException("Payment method does not exist");
        }
        return method;
    }

    private boolean canEnable(String methodCode) {
        return ENABLED_METHODS_IN_PHASE_ONE.contains(methodCode);
    }

    private PayMethodResponse toResponse(PayMethod method) {
        return new PayMethodResponse(
                method.getId(),
                method.getTenantId(),
                method.getMethodCode(),
                method.getMethodName(),
                method.getEnabled(),
                !canEnable(method.getMethodCode()),
                method.getSortOrder(),
                method.getRemark(),
                method.getCreatedAt(),
                method.getUpdatedAt()
        );
    }
}
