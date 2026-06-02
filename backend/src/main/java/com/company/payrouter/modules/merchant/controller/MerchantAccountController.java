package com.company.payrouter.modules.merchant.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.modules.merchant.dto.MerchantAccountDtos.MerchantAccountCreateRequest;
import com.company.payrouter.modules.merchant.dto.MerchantAccountDtos.MerchantAccountResponse;
import com.company.payrouter.modules.merchant.dto.MerchantAccountDtos.MerchantAccountUpdateRequest;
import com.company.payrouter.modules.merchant.service.MerchantAccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/merchant-accounts")
public class MerchantAccountController {
    private final MerchantAccountService accountService;

    public MerchantAccountController(MerchantAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ApiResult<PageResult<MerchantAccountResponse>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long poolId,
            @RequestParam(required = false) String status
    ) {
        return ApiResult.success(accountService.pageAccounts(current, size, keyword, poolId, status));
    }

    @GetMapping("/{id}")
    public ApiResult<MerchantAccountResponse> detail(@PathVariable Long id) {
        return ApiResult.success(accountService.getAccount(id));
    }

    @PostMapping
    public ApiResult<MerchantAccountResponse> create(@Valid @RequestBody MerchantAccountCreateRequest request) {
        return ApiResult.success(accountService.createAccount(request));
    }

    @PutMapping("/{id}")
    public ApiResult<MerchantAccountResponse> update(@PathVariable Long id, @Valid @RequestBody MerchantAccountUpdateRequest request) {
        return ApiResult.success(accountService.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ApiResult.success();
    }

    @PostMapping("/{id}/enable")
    public ApiResult<MerchantAccountResponse> enable(@PathVariable Long id) {
        return ApiResult.success(accountService.enableAccount(id));
    }

    @PostMapping("/{id}/disable")
    public ApiResult<MerchantAccountResponse> disable(@PathVariable Long id) {
        return ApiResult.success(accountService.disableAccount(id));
    }
}
