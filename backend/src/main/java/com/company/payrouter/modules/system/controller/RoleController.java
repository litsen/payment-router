package com.company.payrouter.modules.system.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.modules.system.dto.RoleDtos.RoleCreateRequest;
import com.company.payrouter.modules.system.dto.RoleDtos.RoleResponse;
import com.company.payrouter.modules.system.dto.RoleDtos.RoleUpdateRequest;
import com.company.payrouter.modules.system.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResult<List<RoleResponse>> list() {
        return ApiResult.success(roleService.listRoles());
    }

    @PostMapping
    public ApiResult<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResult.success(roleService.createRole(request));
    }

    @PutMapping("/{id}")
    public ApiResult<RoleResponse> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResult.success(roleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResult.success();
    }
}
