package com.company.payrouter.modules.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.modules.merchant.dto.MerchantPoolDtos.MerchantPoolCreateRequest;
import com.company.payrouter.modules.merchant.dto.MerchantPoolDtos.MerchantPoolResponse;
import com.company.payrouter.modules.merchant.dto.MerchantPoolDtos.MerchantPoolUpdateRequest;
import com.company.payrouter.modules.merchant.entity.PayMerchantAccount;
import com.company.payrouter.modules.merchant.entity.PayMerchantApp;
import com.company.payrouter.modules.merchant.entity.PayMerchantPool;
import com.company.payrouter.modules.merchant.mapper.PayMerchantAccountMapper;
import com.company.payrouter.modules.merchant.mapper.PayMerchantAppMapper;
import com.company.payrouter.modules.merchant.mapper.PayMerchantPoolMapper;
import com.company.payrouter.modules.system.service.OperationLogService;
import com.company.payrouter.infrastructure.crypto.AesCryptoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class MerchantPoolService {
    private final PayMerchantPoolMapper poolMapper;
    private final PayMerchantAccountMapper accountMapper;
    private final PayMerchantAppMapper appMapper;
    private final AesCryptoService cryptoService;
    private final OperationLogService operationLogService;

    public MerchantPoolService(
            PayMerchantPoolMapper poolMapper,
            PayMerchantAccountMapper accountMapper,
            PayMerchantAppMapper appMapper,
            AesCryptoService cryptoService,
            OperationLogService operationLogService
    ) {
        this.poolMapper = poolMapper;
        this.accountMapper = accountMapper;
        this.appMapper = appMapper;
        this.cryptoService = cryptoService;
        this.operationLogService = operationLogService;
    }

    public PageResult<MerchantPoolResponse> pagePools(long current, long size, String keyword, String status) {
        LambdaQueryWrapper<PayMerchantPool> wrapper = new LambdaQueryWrapper<PayMerchantPool>()
                .orderByDesc(PayMerchantPool::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(PayMerchantPool::getPoolName, keyword).or().like(PayMerchantPool::getPoolCode, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PayMerchantPool::getStatus, status);
        }
        Page<PayMerchantPool> page = poolMapper.selectPage(Page.of(current, size), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toResponse).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public MerchantPoolResponse getPool(Long id) {
        return toResponse(requirePool(id));
    }

    @Transactional
    public MerchantPoolResponse createPool(MerchantPoolCreateRequest request) {
        String poolCode = StringUtils.hasText(request.poolCode()) ? request.poolCode() : nextPoolCode();
        if (findByPoolCode(poolCode) != null) {
            throw new BizException("Merchant pool code already exists");
        }
        PayMerchantPool pool = new PayMerchantPool();
        pool.setTenantId(StringUtils.hasText(request.tenantId()) ? request.tenantId() : "default");
        pool.setPoolName(request.poolName());
        pool.setPoolCode(poolCode);
        pool.setStatus(StringUtils.hasText(request.status()) ? request.status() : "ENABLED");
        pool.setRemark(request.remark());
        pool.setCreatedAt(LocalDateTime.now());
        pool.setUpdatedAt(LocalDateTime.now());
        poolMapper.insert(pool);
        String plainSecret = createDefaultApp(pool);
        operationLogService.record("CREATE", "MERCHANT_POOL", pool.getId(), "Create merchant pool " + pool.getPoolCode());
        return toResponse(pool, plainSecret);
    }

    @Transactional
    public MerchantPoolResponse updatePool(Long id, MerchantPoolUpdateRequest request) {
        PayMerchantPool pool = requirePool(id);
        pool.setPoolName(request.poolName());
        pool.setStatus(StringUtils.hasText(request.status()) ? request.status() : "ENABLED");
        pool.setRemark(request.remark());
        pool.setUpdatedAt(LocalDateTime.now());
        poolMapper.updateById(pool);
        operationLogService.record("UPDATE", "MERCHANT_POOL", id, "Update merchant pool " + pool.getPoolCode());
        return toResponse(pool);
    }

    @Transactional
    public void deletePool(Long id) {
        PayMerchantPool pool = requirePool(id);
        Long accountCount = accountMapper.selectCount(new LambdaQueryWrapper<PayMerchantAccount>().eq(PayMerchantAccount::getPoolId, id));
        if (accountCount != null && accountCount > 0) {
            throw new BizException("Merchant pool has accounts and cannot be deleted");
        }
        appMapper.delete(new LambdaQueryWrapper<PayMerchantApp>().eq(PayMerchantApp::getPoolId, id));
        poolMapper.deleteById(id);
        operationLogService.record("DELETE", "MERCHANT_POOL", id, "Delete merchant pool " + pool.getPoolCode());
    }

    @Transactional
    public MerchantPoolResponse resetAppSecret(Long id) {
        PayMerchantPool pool = requirePool(id);
        PayMerchantApp app = requireDefaultApp(pool);
        String plainSecret = MerchantAppService.generateSecret();
        app.setSecretEncrypted(cryptoService.encrypt(plainSecret));
        app.setUpdatedAt(LocalDateTime.now());
        appMapper.updateById(app);
        operationLogService.record("RESET_SECRET", "MERCHANT_POOL", id, "Reset merchant app secret " + pool.getPoolCode());
        return toResponse(pool, plainSecret);
    }

    public PayMerchantPool requirePool(Long id) {
        PayMerchantPool pool = poolMapper.selectById(id);
        if (pool == null) {
            throw new BizException("Merchant pool does not exist");
        }
        return pool;
    }

    public MerchantPoolResponse toResponse(PayMerchantPool pool) {
        return toResponse(pool, null);
    }

    private MerchantPoolResponse toResponse(PayMerchantPool pool, String plainSecret) {
        PayMerchantApp app = defaultApp(pool);
        return new MerchantPoolResponse(
                pool.getId(),
                pool.getTenantId(),
                pool.getPoolName(),
                pool.getPoolCode(),
                app == null ? pool.getPoolCode() : app.getAppId(),
                maskAppSecret(app, plainSecret),
                plainSecret,
                pool.getStatus(),
                pool.getRemark(),
                pool.getCreatedAt(),
                pool.getUpdatedAt()
        );
    }

    private PayMerchantPool findByPoolCode(String poolCode) {
        return poolMapper.selectOne(new LambdaQueryWrapper<PayMerchantPool>().eq(PayMerchantPool::getPoolCode, poolCode));
    }

    private String nextPoolCode() {
        return "MCH" + System.currentTimeMillis();
    }

    private String createDefaultApp(PayMerchantPool pool) {
        String plainSecret = MerchantAppService.generateSecret();
        PayMerchantApp app = new PayMerchantApp();
        app.setTenantId(pool.getTenantId());
        app.setPoolId(pool.getId());
        app.setAppId(pool.getPoolCode());
        app.setAppName(pool.getPoolName() + " 接口凭证");
        app.setSecretEncrypted(cryptoService.encrypt(plainSecret));
        app.setRateLimitPerMinute(60);
        app.setStatus(pool.getStatus());
        app.setRemark("System-generated merchant access credential");
        app.setCreatedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        appMapper.insert(app);
        return plainSecret;
    }

    private PayMerchantApp requireDefaultApp(PayMerchantPool pool) {
        PayMerchantApp app = defaultApp(pool);
        if (app != null) {
            return app;
        }
        createDefaultApp(pool);
        return defaultApp(pool);
    }

    private PayMerchantApp defaultApp(PayMerchantPool pool) {
        return appMapper.selectOne(new LambdaQueryWrapper<PayMerchantApp>().eq(PayMerchantApp::getAppId, pool.getPoolCode()));
    }

    private String maskAppSecret(PayMerchantApp app, String plainSecret) {
        if (StringUtils.hasText(plainSecret)) {
            return cryptoService.mask(plainSecret);
        }
        if (app == null || !StringUtils.hasText(app.getSecretEncrypted())) {
            return null;
        }
        try {
            return cryptoService.maskEncrypted(app.getSecretEncrypted());
        } catch (RuntimeException exception) {
            return "****";
        }
    }
}
