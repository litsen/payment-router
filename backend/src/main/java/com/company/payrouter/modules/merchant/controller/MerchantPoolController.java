package com.company.payrouter.modules.merchant.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.modules.merchant.dto.MerchantPoolDtos.MerchantPoolCreateRequest;
import com.company.payrouter.modules.merchant.dto.MerchantPoolDtos.MerchantPoolResponse;
import com.company.payrouter.modules.merchant.dto.MerchantPoolDtos.MerchantPoolUpdateRequest;
import com.company.payrouter.modules.merchant.service.MerchantPoolService;
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
@RequestMapping("/admin/merchant-pools")
public class MerchantPoolController {
    private final MerchantPoolService poolService;

    public MerchantPoolController(MerchantPoolService poolService) {
        this.poolService = poolService;
    }

    @GetMapping
    public ApiResult<PageResult<MerchantPoolResponse>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ApiResult.success(poolService.pagePools(current, size, keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResult<MerchantPoolResponse> detail(@PathVariable Long id) {
        return ApiResult.success(poolService.getPool(id));
    }

    @PostMapping
    public ApiResult<MerchantPoolResponse> create(@Valid @RequestBody MerchantPoolCreateRequest request) {
        return ApiResult.success(poolService.createPool(request));
    }

    @PutMapping("/{id}")
    public ApiResult<MerchantPoolResponse> update(@PathVariable Long id, @Valid @RequestBody MerchantPoolUpdateRequest request) {
        return ApiResult.success(poolService.updatePool(id, request));
    }

    @PostMapping("/{id}/reset-app-secret")
    public ApiResult<MerchantPoolResponse> resetAppSecret(@PathVariable Long id) {
        return ApiResult.success(poolService.resetAppSecret(id));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        poolService.deletePool(id);
        return ApiResult.success();
    }
}
