package com.company.payrouter.modules.auth.service;

import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.modules.auth.dto.CurrentUserResponse;
import com.company.payrouter.modules.auth.dto.LoginRequest;
import com.company.payrouter.modules.auth.dto.LoginResponse;
import com.company.payrouter.modules.system.entity.SysUser;
import com.company.payrouter.modules.system.service.OperationLogService;
import com.company.payrouter.modules.system.service.RoleService;
import com.company.payrouter.modules.system.service.UserService;
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

    public AuthService(
            UserService userService,
            RoleService roleService,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            JwtProperties jwtProperties,
            OperationLogService operationLogService
    ) {
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
        this.operationLogService = operationLogService;
    }

    public LoginResponse login(LoginRequest request) {
        SysUser user = userService.findByUsername(request.username());
        if (user == null || !"ENABLED".equals(user.getStatus()) || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        AuthUser authUser = buildAuthUser(user);
        String token = tokenProvider.createToken(authUser);
        userService.updateLastLoginTime(user.getId());
        operationLogService.record("LOGIN", "AUTH", user.getId(), "用户登录 " + user.getUsername());
        return new LoginResponse(token, "Bearer", jwtProperties.expireMinutes() * 60, toCurrentUser(authUser));
    }

    public void logout(AuthUser user) {
        if (user != null) {
            operationLogService.record("LOGOUT", "AUTH", user.userId(), "用户退出 " + user.username());
        }
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
}
