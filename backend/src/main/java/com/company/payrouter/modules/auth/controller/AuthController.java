package com.company.payrouter.modules.auth.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.modules.auth.dto.CaptchaResponse;
import com.company.payrouter.modules.auth.dto.ChangePasswordRequest;
import com.company.payrouter.modules.auth.dto.CurrentUserResponse;
import com.company.payrouter.modules.auth.dto.LoginRequest;
import com.company.payrouter.modules.auth.dto.LoginResponse;
import com.company.payrouter.modules.auth.dto.LoginSecurityStatusResponse;
import com.company.payrouter.modules.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import com.company.payrouter.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResult.success(authService.login(request, servletRequest));
    }

    @GetMapping("/login-status")
    public ApiResult<LoginSecurityStatusResponse> loginStatus(@RequestParam(required = false) String username, HttpServletRequest request) {
        return ApiResult.success(authService.loginStatus(username, request));
    }

    @GetMapping("/captcha")
    public ApiResult<CaptchaResponse> captcha(@RequestParam String username) {
        return ApiResult.success(authService.captcha(username));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@AuthenticationPrincipal AuthUser user) {
        authService.logout(user);
        return ApiResult.success();
    }

    @PostMapping("/change-password")
    public ApiResult<Void> changePassword(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user, request);
        return ApiResult.success();
    }

    @GetMapping("/me")
    public ApiResult<CurrentUserResponse> me(@AuthenticationPrincipal AuthUser user) {
        return ApiResult.success(authService.toCurrentUser(user));
    }
}
