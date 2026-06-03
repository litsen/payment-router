package com.company.payrouter.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.payrouter.common.api.PageResult;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.modules.auth.entity.SysLoginSecurity;
import com.company.payrouter.modules.auth.service.LoginSecurityService;
import com.company.payrouter.modules.system.dto.UserDtos.UserCreateRequest;
import com.company.payrouter.modules.system.dto.UserDtos.UserResponse;
import com.company.payrouter.modules.system.dto.UserDtos.UserUpdateRequest;
import com.company.payrouter.modules.system.entity.SysRole;
import com.company.payrouter.modules.system.entity.SysUser;
import com.company.payrouter.modules.system.entity.SysUserRole;
import com.company.payrouter.modules.system.mapper.SysRoleMapper;
import com.company.payrouter.modules.system.mapper.SysUserMapper;
import com.company.payrouter.modules.system.mapper.SysUserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class UserService {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final RoleService roleService;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;
    private final LoginSecurityService loginSecurityService;

    public UserService(
            SysUserMapper userMapper,
            SysRoleMapper roleMapper,
            SysUserRoleMapper userRoleMapper,
            RoleService roleService,
            OperationLogService operationLogService,
            PasswordEncoder passwordEncoder,
            LoginSecurityService loginSecurityService
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleService = roleService;
        this.operationLogService = operationLogService;
        this.passwordEncoder = passwordEncoder;
        this.loginSecurityService = loginSecurityService;
    }

    public PageResult<UserResponse> pageUsers(long current, long size, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByAsc(SysUser::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
        }
        Page<SysUser> page = userMapper.selectPage(Page.of(current, size), wrapper);
        List<UserResponse> records = page.getRecords().stream().map(this::toResponse).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (findByUsername(request.username()) != null) {
            throw new BizException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName());
        user.setStatus(StringUtils.hasText(request.status()) ? request.status() : "ENABLED");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        roleService.assignRoles(user.getId(), request.roles());
        operationLogService.record("CREATE", "USER", user.getId(), "创建用户 " + user.getUsername());
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        SysUser user = requireUser(id);
        boolean removingSuperAdmin = roleService.roleCodesByUserId(id).contains("SUPER_ADMIN")
                && !request.roles().contains("SUPER_ADMIN");
        if (removingSuperAdmin) {
            ensureNotLastSuperAdmin(id);
        }
        user.setRealName(request.realName());
        user.setStatus(StringUtils.hasText(request.status()) ? request.status() : "ENABLED");
        if (StringUtils.hasText(request.password())) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        roleService.assignRoles(id, request.roles());
        operationLogService.record("UPDATE", "USER", id, "更新用户 " + user.getUsername());
        return toResponse(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        SysUser user = requireUser(id);
        if (roleService.roleCodesByUserId(id).contains("SUPER_ADMIN")) {
            ensureNotLastSuperAdmin(id);
        }
        loginSecurityService.clearByUsername(user.getUsername());
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        userMapper.deleteById(id);
        operationLogService.record("DELETE", "USER", id, "删除用户 " + user.getUsername());
    }

    public SysUser findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    public SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    public void updateLastLoginTime(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public void changePassword(Long userId, String password) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(passwordEncoder.encode(password));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public void unlockLoginLimit(Long userId) {
        SysUser user = requireUser(userId);
        loginSecurityService.unlockUser(userId);
        operationLogService.record("UNLOCK_LOGIN", "USER", userId, "解除登录限制 " + user.getUsername());
    }

    public UserResponse toResponse(SysUser user) {
        SysLoginSecurity security = loginSecurityService.findByUsername(user.getUsername());
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getStatus(),
                roleService.roleCodesByUserId(user.getId()),
                security == null ? 0 : security.getFailCount(),
                security != null && (Boolean.TRUE.equals(security.getUserLocked()) || Boolean.TRUE.equals(security.getIpLocked())),
                security == null ? null : security.getLockedIp(),
                security == null ? null : security.getLastFailTime(),
                user.getLastLoginTime(),
                user.getCreatedAt()
        );
    }

    private void ensureNotLastSuperAdmin(Long changedUserId) {
        SysRole superAdmin = roleService.findByCode("SUPER_ADMIN");
        if (superAdmin == null) {
            throw new BizException("超级管理员角色不存在");
        }
        List<SysUserRole> superAdminRelations = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, superAdmin.getId())
        );
        long remaining = superAdminRelations.stream()
                .map(SysUserRole::getUserId)
                .filter(userId -> !userId.equals(changedUserId))
                .count();
        if (remaining <= 0) {
            throw new BizException("禁止删除或移除最后一个超级管理员");
        }
    }
}
