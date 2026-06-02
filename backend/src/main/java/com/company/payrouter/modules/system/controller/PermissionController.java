package com.company.payrouter.modules.system.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.modules.system.dto.PermissionTreeNode;
import com.company.payrouter.modules.system.service.PermissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/permissions")
public class PermissionController {
    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/tree")
    public ApiResult<List<PermissionTreeNode>> tree() {
        return ApiResult.success(permissionService.permissionTree());
    }
}
