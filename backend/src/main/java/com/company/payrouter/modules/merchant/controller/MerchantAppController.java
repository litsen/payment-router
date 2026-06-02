package com.company.payrouter.modules.merchant.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.modules.merchant.dto.MerchantAppDtos.MerchantAppCreateRequest;
import com.company.payrouter.modules.merchant.dto.MerchantAppDtos.MerchantAppResponse;
import com.company.payrouter.modules.merchant.dto.MerchantAppDtos.MerchantAppUpdateRequest;
import com.company.payrouter.modules.merchant.service.MerchantAppService;
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
@RequestMapping("/admin/merchant-apps")
public class MerchantAppController {
    private final MerchantAppService appService;

    public MerchantAppController(MerchantAppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public ApiResult<PageResult<MerchantAppResponse>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long poolId,
            @RequestParam(required = false) String status
    ) {
        return ApiResult.success(appService.pageApps(current, size, keyword, poolId, status));
    }

    @GetMapping("/{id}")
    public ApiResult<MerchantAppResponse> detail(@PathVariable Long id) {
        return ApiResult.success(appService.getApp(id));
    }

    @PostMapping
    public ApiResult<MerchantAppResponse> create(@Valid @RequestBody MerchantAppCreateRequest request) {
        return ApiResult.success(appService.createApp(request));
    }

    @PutMapping("/{id}")
    public ApiResult<MerchantAppResponse> update(@PathVariable Long id, @Valid @RequestBody MerchantAppUpdateRequest request) {
        return ApiResult.success(appService.updateApp(id, request));
    }

    @PostMapping("/{id}/reset-secret")
    public ApiResult<MerchantAppResponse> resetSecret(@PathVariable Long id) {
        return ApiResult.success(appService.resetSecret(id));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        appService.deleteApp(id);
        return ApiResult.success();
    }
}
