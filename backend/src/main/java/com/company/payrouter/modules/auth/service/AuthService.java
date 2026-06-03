package com.company.payrouter.modules.auth.service;

import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.modules.auth.dto.CaptchaResponse;
import com.company.payrouter.modules.auth.dto.ChangePasswordRequest;
import com.company.payrouter.modules.auth.dto.CurrentUserResponse;
import com.company.payrouter.modules.auth.dto.LoginRequest;
import com.company.payrouter.modules.auth.dto.LoginResponse;
import com.company.payrouter.modules.auth.dto.LoginSecurityStatusResponse;
import com.company.payrouter.modules.system.entity.SysUser;
import com.company.payrouter.modules.system.service.OperationLogService;
import com.company.payrouter.modules.system.service.RoleService;
import com.company.payrouter.modules.system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import com.company.payrouter.security.AuthUser;
import com.company.payrouter.security.JwtProperties;
import com.company.payrouter.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {
    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final OperationLogService operationLogService;
    private final LoginSecurityService loginSecurityService;

    public AuthService(
            UserService userService,
            RoleService roleService,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            JwtProperties jwtProperties,
            OperationLogService operationLogService,
            LoginSecurityService loginSecurityService
    ) {
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
        this.operationLogService = operationLogService;
        this.loginSecurityService = loginSecurityService;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        String username = request.username().trim();
        String ip = clientIp(servletRequest);
        loginSecurityService.assertLoginAllowed(username, ip);
        loginSecurityService.validateCaptchaIfRequired(username, request.captchaId(), request.captchaCode());
        SysUser user = userService.findByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginSecurityService.recordFailure(username, ip, user);
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw new BizException(401, "用户已禁用");
        }
        AuthUser authUser = buildAuthUser(user);
        String token = tokenProvider.createToken(authUser);
        userService.updateLastLoginTime(user.getId());
        loginSecurityService.recordSuccess(username);
        operationLogService.record("LOGIN", "AUTH", user.getId(), "用户登录 " + user.getUsername());
        return new LoginResponse(token, "Bearer", jwtProperties.expireMinutes() * 60, toCurrentUser(authUser));
    }

    public LoginSecurityStatusResponse loginStatus(String username, HttpServletRequest request) {
        return loginSecurityService.status(username, clientIp(request));
    }

    public CaptchaResponse captcha(String username) {
        return loginSecurityService.captcha(username);
    }

    public void logout(AuthUser user) {
        if (user != null) {
            operationLogService.record("LOGOUT", "AUTH", user.userId(), "用户退出 " + user.username());
        }
    }

    public void changePassword(AuthUser authUser, ChangePasswordRequest request) {
        if (authUser == null) {
            throw new BizException(401, "未登录");
        }
        SysUser user = userService.requireUser(authUser.userId());
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BizException("原密码错误");
        }
        userService.changePassword(user.getId(), request.newPassword());
        operationLogService.record("CHANGE_PASSWORD", "AUTH", user.getId(), "修改密码 " + user.getUsername());
    }

    public AuthUser loadAuthUser(Long userId) {
        SysUser user = userService.requireUser(userId);
        if (!"ENABLED".equals(user.getStatus())) {
            throw new BizException(401, "用户已禁用");
        }
        return buildAuthUser(user);
    }

    public CurrentUserResponse toCurrentUser(AuthUser user) {
        return new CurrentUserResponse(user.userId(), user.username(), user.realName(), user.roles(), user.permissions());
    }

    private AuthUser buildAuthUser(SysUser user) {
        Set<String> roles = roleService.roleCodesByUserId(user.getId());
        Set<String> permissions = roleService.permissionCodesByRoleCodes(roles);
        return new AuthUser(user.getId(), user.getUsername(), user.getRealName(), roles, permissions);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
