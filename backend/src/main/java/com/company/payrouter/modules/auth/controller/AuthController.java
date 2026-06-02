package com.company.payrouter.modules.auth.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.modules.auth.dto.CurrentUserResponse;
import com.company.payrouter.modules.auth.dto.LoginRequest;
import com.company.payrouter.modules.auth.dto.LoginResponse;
import com.company.payrouter.modules.auth.service.AuthService;
import com.company.payrouter.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@AuthenticationPrincipal AuthUser user) {
        authService.logout(user);
        return ApiResult.success();
    }

    @GetMapping("/me")
    public ApiResult<CurrentUserResponse> me(@AuthenticationPrincipal AuthUser user) {
        return ApiResult.success(authService.toCurrentUser(user));
    }
}
