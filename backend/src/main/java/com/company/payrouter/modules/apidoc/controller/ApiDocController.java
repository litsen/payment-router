package com.company.payrouter.modules.apidoc.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.modules.apidoc.dto.ApiDocDtos.ApiDocResponse;
import com.company.payrouter.modules.apidoc.dto.ApiDocDtos.ApiDocSummary;
import com.company.payrouter.modules.apidoc.service.ApiDocService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ApiDocController {
    private final ApiDocService apiDocService;

    public ApiDocController(ApiDocService apiDocService) {
        this.apiDocService = apiDocService;
    }

    @GetMapping({"/admin/api-docs", "/api/docs"})
    public ApiResult<List<ApiDocSummary>> list() {
        return ApiResult.success(apiDocService.listDocs());
    }

    @GetMapping({"/admin/api-docs/{slug}", "/api/docs/{slug}"})
    public ApiResult<ApiDocResponse> detail(@PathVariable String slug) {
        return ApiResult.success(apiDocService.getDoc(slug));
    }
}
