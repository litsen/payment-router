package com.company.payrouter.modules.paymethod.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.modules.paymethod.dto.PayMethodDtos.PayMethodResponse;
import com.company.payrouter.modules.paymethod.dto.PayMethodDtos.PayMethodUpdateRequest;
import com.company.payrouter.modules.paymethod.service.PayMethodService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/pay-methods")
public class PayMethodController {
    private final PayMethodService payMethodService;

    public PayMethodController(PayMethodService payMethodService) {
        this.payMethodService = payMethodService;
    }

    @GetMapping
    public ApiResult<List<PayMethodResponse>> list() {
        return ApiResult.success(payMethodService.listMethods());
    }

    @PutMapping("/{id}")
    public ApiResult<PayMethodResponse> update(@PathVariable Long id, @Valid @RequestBody PayMethodUpdateRequest request) {
        return ApiResult.success(payMethodService.updateMethod(id, request));
    }

    @PostMapping("/{id}/enable")
    public ApiResult<PayMethodResponse> enable(@PathVariable Long id) {
        return ApiResult.success(payMethodService.enableMethod(id));
    }

    @PostMapping("/{id}/disable")
    public ApiResult<PayMethodResponse> disable(@PathVariable Long id) {
        return ApiResult.success(payMethodService.disableMethod(id));
    }
}
