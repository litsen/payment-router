package com.company.payrouter.modules.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.infrastructure.crypto.AesCryptoService;
import com.company.payrouter.modules.merchant.dto.MerchantAccountDtos.MerchantAccountCreateRequest;
import com.company.payrouter.modules.merchant.dto.MerchantAccountDtos.MerchantAccountResponse;
import com.company.payrouter.modules.merchant.dto.MerchantAccountDtos.MerchantAccountUpdateRequest;
import com.company.payrouter.modules.merchant.entity.PayMerchantAccount;
import com.company.payrouter.modules.merchant.entity.PayMerchantAccountSecret;
import com.company.payrouter.modules.merchant.entity.PayMerchantPool;
import com.company.payrouter.modules.merchant.mapper.PayMerchantAccountMapper;
import com.company.payrouter.modules.merchant.mapper.PayMerchantAccountSecretMapper;
import com.company.payrouter.modules.system.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class MerchantAccountService {
    private final PayMerchantAccountMapper accountMapper;
    private final PayMerchantAccountSecretMapper secretMapper;
    private final MerchantPoolService poolService;
    private final AesCryptoService cryptoService;
    private final OperationLogService operationLogService;

    public MerchantAccountService(
            PayMerchantAccountMapper accountMapper,
            PayMerchantAccountSecretMapper secretMapper,
            MerchantPoolService poolService,
            AesCryptoService cryptoService,
            OperationLogService operationLogService
    ) {
        this.accountMapper = accountMapper;
        this.secretMapper = secretMapper;
        this.poolService = poolService;
        this.cryptoService = cryptoService;
        this.operationLogService = operationLogService;
    }

    public PageResult<MerchantAccountResponse> pageAccounts(long current, long size, String keyword, Long poolId, String status) {
        LambdaQueryWrapper<PayMerchantAccount> wrapper = new LambdaQueryWrapper<PayMerchantAccount>()
                .orderByAsc(PayMerchantAccount::getPriority)
                .orderByDesc(PayMerchantAccount::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(PayMerchantAccount::getAccountName, keyword).or().like(PayMerchantAccount::getChannelCode, keyword));
        }
        if (poolId != null) {
            wrapper.eq(PayMerchantAccount::getPoolId, poolId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PayMerchantAccount::getStatus, status);
        }
        Page<PayMerchantAccount> page = accountMapper.selectPage(Page.of(current, size), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toResponse).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public MerchantAccountResponse getAccount(Long id) {
        return toResponse(requireAccount(id));
    }

    @Transactional
    public MerchantAccountResponse createAccount(MerchantAccountCreateRequest request) {
        PayMerchantPool pool = poolService.requirePool(request.poolId());
        PayMerchantAccount account = new PayMerchantAccount();
        account.setTenantId(StringUtils.hasText(request.tenantId()) ? request.tenantId() : pool.getTenantId());
        applyCreateFields(account, request);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.insert(account);

        PayMerchantAccountSecret secret = new PayMerchantAccountSecret();
        secret.setAccountId(account.getId());
        applySecretFields(secret, request.apiKey(), request.privateKey(), request.publicKey(), request.certPath(), request.certPassword(), request.extraConfigJson(), true);
        secret.setCreatedAt(LocalDateTime.now());
        secret.setUpdatedAt(LocalDateTime.now());
        secretMapper.insert(secret);

        operationLogService.record("CREATE", "MERCHANT_ACCOUNT", account.getId(), "Create merchant account " + account.getAccountName());
        return toResponse(account);
    }

    @Transactional
    public MerchantAccountResponse updateAccount(Long id, MerchantAccountUpdateRequest request) {
        PayMerchantAccount account = requireAccount(id);
        poolService.requirePool(request.poolId());
        account.setPoolId(request.poolId());
        account.setAccountName(request.accountName());
        account.setChannelCode(StringUtils.hasText(request.channelCode()) ? request.channelCode() : "DEFAULT");
        if (StringUtils.hasText(request.apiKey())) {
            account.setApiKeyEncrypted(cryptoService.encrypt(request.apiKey()));
        }
        if (StringUtils.hasText(request.signKey())) {
            account.setSignKeyEncrypted(cryptoService.encrypt(request.signKey()));
        }
        applyCommonFields(account, request.supportPayMethods(), request.priority(), request.weight(), request.dailyAmountLimit(),
                request.monthlyAmountLimit(), request.singleMinAmount(), request.singleMaxAmount(), request.availableStartDate(),
                request.availableEndDate(), request.availableStartTime(), request.availableEndTime(), request.status(), request.remark());
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);

        PayMerchantAccountSecret secret = getOrCreateSecret(id);
        applySecretFields(secret, request.apiKey(), request.privateKey(), request.publicKey(), request.certPath(), request.certPassword(), request.extraConfigJson(), false);
        secret.setUpdatedAt(LocalDateTime.now());
        if (secret.getId() == null) {
            secret.setCreatedAt(LocalDateTime.now());
            secretMapper.insert(secret);
        } else {
            secretMapper.updateById(secret);
        }

        String content = sensitiveChanged(request) ? "Update merchant account and sensitive parameters " : "Update merchant account ";
        operationLogService.record("UPDATE", "MERCHANT_ACCOUNT", id, content + account.getAccountName());
        return toResponse(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        PayMerchantAccount account = requireAccount(id);
        secretMapper.delete(new LambdaQueryWrapper<PayMerchantAccountSecret>().eq(PayMerchantAccountSecret::getAccountId, id));
        accountMapper.deleteById(id);
        operationLogService.record("DELETE", "MERCHANT_ACCOUNT", id, "Delete merchant account " + account.getAccountName());
    }

    @Transactional
    public MerchantAccountResponse enableAccount(Long id) {
        return changeStatus(id, "ENABLED");
    }

    @Transactional
    public MerchantAccountResponse disableAccount(Long id) {
        return changeStatus(id, "DISABLED");
    }

    private MerchantAccountResponse changeStatus(Long id, String status) {
        PayMerchantAccount account = requireAccount(id);
        account.setStatus(status);
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);
        operationLogService.record(status, "MERCHANT_ACCOUNT", id, status + " merchant account " + account.getAccountName());
        return toResponse(account);
    }

    private void applyCreateFields(PayMerchantAccount account, MerchantAccountCreateRequest request) {
        account.setPoolId(request.poolId());
        account.setAccountName(request.accountName());
        account.setChannelCode(StringUtils.hasText(request.channelCode()) ? request.channelCode() : "DEFAULT");
        account.setApiKeyEncrypted(cryptoService.encrypt(request.apiKey()));
        account.setSignKeyEncrypted(cryptoService.encrypt(request.signKey()));
        applyCommonFields(account, request.supportPayMethods(), request.priority(), request.weight(), request.dailyAmountLimit(),
                request.monthlyAmountLimit(), request.singleMinAmount(), request.singleMaxAmount(), request.availableStartDate(),
                request.availableEndDate(), request.availableStartTime(), request.availableEndTime(), request.status(), request.remark());
        account.setFailCount(0);
    }

    private void applyCommonFields(
            PayMerchantAccount account,
            String supportPayMethods,
            Integer priority,
            Integer weight,
            java.math.BigDecimal dailyAmountLimit,
            java.math.BigDecimal monthlyAmountLimit,
            java.math.BigDecimal singleMinAmount,
            java.math.BigDecimal singleMaxAmount,
            java.time.LocalDate availableStartDate,
            java.time.LocalDate availableEndDate,
            java.time.LocalTime availableStartTime,
            java.time.LocalTime availableEndTime,
            String status,
            String remark
    ) {
        account.setSupportPayMethods(StringUtils.hasText(supportPayMethods) ? supportPayMethods : "ALL");
        account.setPriority(priority == null ? 100 : priority);
        account.setWeight(weight == null ? 1 : weight);
        account.setDailyAmountLimit(dailyAmountLimit);
        account.setMonthlyAmountLimit(monthlyAmountLimit);
        account.setSingleMinAmount(singleMinAmount);
        account.setSingleMaxAmount(singleMaxAmount);
        account.setAvailableStartDate(availableStartDate);
        account.setAvailableEndDate(availableEndDate);
        account.setAvailableStartTime(availableStartTime);
        account.setAvailableEndTime(availableEndTime);
        account.setStatus(StringUtils.hasText(status) ? status : "ENABLED");
        account.setRemark(remark);
    }

    private void applySecretFields(
            PayMerchantAccountSecret secret,
            String apiKey,
            String privateKey,
            String publicKey,
            String certPath,
            String certPassword,
            String extraConfigJson,
            boolean create
    ) {
        if (create || StringUtils.hasText(apiKey)) {
            secret.setApiKeyEncrypted(cryptoService.encrypt(apiKey));
        }
        if (create || StringUtils.hasText(privateKey)) {
            secret.setPrivateKeyEncrypted(cryptoService.encrypt(privateKey));
        }
        if (create || StringUtils.hasText(publicKey)) {
            secret.setPublicKeyEncrypted(cryptoService.encrypt(publicKey));
        }
        if (create || certPath != null) {
            secret.setCertPath(certPath);
        }
        if (create || StringUtils.hasText(certPassword)) {
            secret.setCertPasswordEncrypted(cryptoService.encrypt(certPassword));
        }
        if (create || extraConfigJson != null) {
            secret.setExtraConfigJson(extraConfigJson);
        }
    }

    private boolean sensitiveChanged(MerchantAccountUpdateRequest request) {
        return StringUtils.hasText(request.apiKey())
                || StringUtils.hasText(request.signKey())
                || StringUtils.hasText(request.privateKey())
                || StringUtils.hasText(request.publicKey())
                || StringUtils.hasText(request.certPassword());
    }

    private PayMerchantAccount requireAccount(Long id) {
        PayMerchantAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw new BizException("Merchant account does not exist");
        }
        return account;
    }

    private PayMerchantAccountSecret getOrCreateSecret(Long accountId) {
        PayMerchantAccountSecret secret = secretMapper.selectOne(new LambdaQueryWrapper<PayMerchantAccountSecret>().eq(PayMerchantAccountSecret::getAccountId, accountId));
        if (secret == null) {
            secret = new PayMerchantAccountSecret();
            secret.setAccountId(accountId);
        }
        return secret;
    }

    private MerchantAccountResponse toResponse(PayMerchantAccount account) {
        PayMerchantPool pool = poolService.requirePool(account.getPoolId());
        PayMerchantAccountSecret secret = secretMapper.selectOne(new LambdaQueryWrapper<PayMerchantAccountSecret>().eq(PayMerchantAccountSecret::getAccountId, account.getId()));
        return new MerchantAccountResponse(
                account.getId(),
                account.getTenantId(),
                account.getPoolId(),
                pool.getPoolName(),
                account.getAccountName(),
                account.getChannelCode(),
                cryptoService.maskEncrypted(account.getApiKeyEncrypted()),
                cryptoService.maskEncrypted(account.getSignKeyEncrypted()),
                secret == null ? null : cryptoService.maskEncrypted(secret.getPrivateKeyEncrypted()),
                secret == null ? null : cryptoService.maskEncrypted(secret.getPublicKeyEncrypted()),
                secret == null ? null : secret.getCertPath(),
                secret == null ? null : cryptoService.maskEncrypted(secret.getCertPasswordEncrypted()),
                secret == null ? null : secret.getExtraConfigJson(),
                account.getSupportPayMethods(),
                account.getPriority(),
                account.getWeight(),
                account.getDailyAmountLimit(),
                account.getMonthlyAmountLimit(),
                account.getSingleMinAmount(),
                account.getSingleMaxAmount(),
                account.getAvailableStartDate(),
                account.getAvailableEndDate(),
                account.getAvailableStartTime(),
                account.getAvailableEndTime(),
                account.getStatus(),
                account.getFailCount(),
                account.getLastFailTime(),
                account.getRemark(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
