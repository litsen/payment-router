package com.company.payrouter.modules.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.infrastructure.crypto.AesCryptoService;
import com.company.payrouter.modules.merchant.dto.MerchantAppDtos.MerchantAppCreateRequest;
import com.company.payrouter.modules.merchant.dto.MerchantAppDtos.MerchantAppResponse;
import com.company.payrouter.modules.merchant.dto.MerchantAppDtos.MerchantAppUpdateRequest;
import com.company.payrouter.modules.merchant.entity.PayMerchantApp;
import com.company.payrouter.modules.merchant.entity.PayMerchantPool;
import com.company.payrouter.modules.merchant.mapper.PayMerchantAppMapper;
import com.company.payrouter.modules.system.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class MerchantAppService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PayMerchantAppMapper appMapper;
    private final MerchantPoolService poolService;
    private final AesCryptoService cryptoService;
    private final OperationLogService operationLogService;

    public MerchantAppService(
            PayMerchantAppMapper appMapper,
            MerchantPoolService poolService,
            AesCryptoService cryptoService,
            OperationLogService operationLogService
    ) {
        this.appMapper = appMapper;
        this.poolService = poolService;
        this.cryptoService = cryptoService;
        this.operationLogService = operationLogService;
    }

    public PageResult<MerchantAppResponse> pageApps(long current, long size, String keyword, Long poolId, String status) {
        LambdaQueryWrapper<PayMerchantApp> wrapper = new LambdaQueryWrapper<PayMerchantApp>()
                .orderByDesc(PayMerchantApp::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(PayMerchantApp::getAppName, keyword).or().like(PayMerchantApp::getAppId, keyword));
        }
        if (poolId != null) {
            wrapper.eq(PayMerchantApp::getPoolId, poolId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PayMerchantApp::getStatus, status);
        }
        Page<PayMerchantApp> page = appMapper.selectPage(Page.of(current, size), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toResponse).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public MerchantAppResponse getApp(Long id) {
        return toResponse(requireApp(id));
    }

    @Transactional
    public MerchantAppResponse createApp(MerchantAppCreateRequest request) {
        PayMerchantPool pool = poolService.requirePool(request.poolId());
        if (findByAppId(request.appId()) != null) {
            throw new BizException("AppId already exists");
        }
        String plainSecret = generateSecret();
        PayMerchantApp app = new PayMerchantApp();
        app.setTenantId(StringUtils.hasText(request.tenantId()) ? request.tenantId() : pool.getTenantId());
        app.setPoolId(request.poolId());
        app.setAppId(request.appId());
        app.setAppName(request.appName());
        app.setSecretEncrypted(cryptoService.encrypt(plainSecret));
        app.setNotifyUrlWhitelist(request.notifyUrlWhitelist());
        app.setRateLimitPerMinute(request.rateLimitPerMinute() == null ? 60 : request.rateLimitPerMinute());
        app.setStatus(StringUtils.hasText(request.status()) ? request.status() : "ENABLED");
        app.setRemark(request.remark());
        app.setCreatedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        appMapper.insert(app);
        operationLogService.record("CREATE", "MERCHANT_APP", app.getId(), "Create merchant app " + app.getAppId());
        return toResponse(app, plainSecret);
    }

    @Transactional
    public MerchantAppResponse updateApp(Long id, MerchantAppUpdateRequest request) {
        PayMerchantApp app = requireApp(id);
        poolService.requirePool(request.poolId());
        PayMerchantApp duplicated = findByAppId(app.getAppId());
        if (duplicated != null && !duplicated.getId().equals(id)) {
            throw new BizException("AppId already exists");
        }
        app.setPoolId(request.poolId());
        app.setAppName(request.appName());
        app.setNotifyUrlWhitelist(request.notifyUrlWhitelist());
        app.setRateLimitPerMinute(request.rateLimitPerMinute() == null ? 60 : request.rateLimitPerMinute());
        app.setStatus(StringUtils.hasText(request.status()) ? request.status() : "ENABLED");
        app.setRemark(request.remark());
        app.setUpdatedAt(LocalDateTime.now());
        appMapper.updateById(app);
        operationLogService.record("UPDATE", "MERCHANT_APP", app.getId(), "Update merchant app " + app.getAppId());
        return toResponse(app);
    }

    @Transactional
    public MerchantAppResponse resetSecret(Long id) {
        PayMerchantApp app = requireApp(id);
        String plainSecret = generateSecret();
        app.setSecretEncrypted(cryptoService.encrypt(plainSecret));
        app.setUpdatedAt(LocalDateTime.now());
        appMapper.updateById(app);
        operationLogService.record("RESET_SECRET", "MERCHANT_APP", app.getId(), "Reset merchant app secret " + app.getAppId());
        return toResponse(app, plainSecret);
    }

    @Transactional
    public void deleteApp(Long id) {
        PayMerchantApp app = requireApp(id);
        appMapper.deleteById(id);
        operationLogService.record("DELETE", "MERCHANT_APP", app.getId(), "Delete merchant app " + app.getAppId());
    }

    public PayMerchantApp requireApp(Long id) {
        PayMerchantApp app = appMapper.selectById(id);
        if (app == null) {
            throw new BizException("Merchant app does not exist");
        }
        return app;
    }

    private PayMerchantApp findByAppId(String appId) {
        return appMapper.selectOne(new LambdaQueryWrapper<PayMerchantApp>().eq(PayMerchantApp::getAppId, appId));
    }

    private MerchantAppResponse toResponse(PayMerchantApp app) {
        return toResponse(app, null);
    }

    private MerchantAppResponse toResponse(PayMerchantApp app, String plainSecret) {
        PayMerchantPool pool = poolService.requirePool(app.getPoolId());
        return new MerchantAppResponse(
                app.getId(),
                app.getTenantId(),
                app.getPoolId(),
                pool.getPoolName(),
                app.getAppId(),
                app.getAppName(),
                maskAppSecret(app, plainSecret),
                plainSecret,
                app.getNotifyUrlWhitelist(),
                app.getRateLimitPerMinute(),
                app.getStatus(),
                app.getRemark(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }

    public static String generateSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String maskAppSecret(PayMerchantApp app, String plainSecret) {
        if (StringUtils.hasText(plainSecret)) {
            return cryptoService.mask(plainSecret);
        }
        if (!StringUtils.hasText(app.getSecretEncrypted())) {
            return null;
        }
        try {
            return cryptoService.maskEncrypted(app.getSecretEncrypted());
        } catch (RuntimeException exception) {
            return "****";
        }
    }
}
