package com.company.payrouter.modules.system.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.modules.system.dto.UserDtos.UserCreateRequest;
import com.company.payrouter.modules.system.dto.UserDtos.UserResponse;
import com.company.payrouter.modules.system.dto.UserDtos.UserUpdateRequest;
import com.company.payrouter.modules.system.service.UserService;
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
@RequestMapping("/admin/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResult<PageResult<UserResponse>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResult.success(userService.pageUsers(current, size, keyword));
    }

    @PostMapping
    public ApiResult<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResult.success(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResult<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResult.success(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResult.success();
    }

    @PostMapping("/{id}/login-lock/unlock")
    public ApiResult<Void> unlockLoginLimit(@PathVariable Long id) {
        userService.unlockLoginLimit(id);
        return ApiResult.success();
    }
}
