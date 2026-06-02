package com.company.payrouter.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.modules.system.dto.RoleDtos.RoleCreateRequest;
import com.company.payrouter.modules.system.dto.RoleDtos.RoleResponse;
import com.company.payrouter.modules.system.dto.RoleDtos.RoleUpdateRequest;
import com.company.payrouter.modules.system.entity.SysPermission;
import com.company.payrouter.modules.system.entity.SysRole;
import com.company.payrouter.modules.system.entity.SysRolePermission;
import com.company.payrouter.modules.system.entity.SysUserRole;
import com.company.payrouter.modules.system.mapper.SysPermissionMapper;
import com.company.payrouter.modules.system.mapper.SysRoleMapper;
import com.company.payrouter.modules.system.mapper.SysRolePermissionMapper;
import com.company.payrouter.modules.system.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoleService {
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final OperationLogService operationLogService;

    public RoleService(
            SysRoleMapper roleMapper,
            SysPermissionMapper permissionMapper,
            SysRolePermissionMapper rolePermissionMapper,
            SysUserRoleMapper userRoleMapper,
            OperationLogService operationLogService
    ) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.operationLogService = operationLogService;
    }

    public List<RoleResponse> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        if (findByCode(request.roleCode()) != null) {
            throw new BizException("角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(request.roleCode());
        role.setRoleName(request.roleName());
        role.setDescription(request.description());
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(role);
        saveRolePermissions(role.getId(), request.permissions());
        operationLogService.record("CREATE", "ROLE", role.getId(), "创建角色 " + role.getRoleCode());
        return toResponse(role);
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleUpdateRequest request) {
        SysRole role = requireRole(id);
        role.setRoleName(request.roleName());
        role.setDescription(request.description());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(role);
        saveRolePermissions(id, request.permissions());
        operationLogService.record("UPDATE", "ROLE", id, "更新角色 " + role.getRoleCode());
        return toResponse(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        SysRole role = requireRole(id);
        long boundUsers = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        if (boundUsers > 0) {
            throw new BizException("角色已绑定用户，不能删除");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        roleMapper.deleteById(id);
        operationLogService.record("DELETE", "ROLE", id, "删除角色 " + role.getRoleCode());
    }

    public SysRole findByCode(String roleCode) {
        return roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
    }

    public List<SysRole> findByCodes(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getRoleCode, roleCodes));
    }

    public Set<String> roleCodesByUserId(Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Set.of();
        }
        Set<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        return roleMapper.selectBatchIds(roleIds).stream().map(SysRole::getRoleCode).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> permissionCodesByRoleCodes(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Set.of();
        }
        List<SysRole> roles = findByCodes(roleCodes);
        if (roles.isEmpty()) {
            return Set.of();
        }
        Set<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toSet());
        List<SysRolePermission> relations = rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds));
        if (relations.isEmpty()) {
            return Set.of();
        }
        Set<Long> permissionIds = relations.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toSet());
        return permissionMapper.selectBatchIds(permissionIds)
                .stream()
                .map(SysPermission::getPermissionCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional
    public void assignRoles(Long userId, Set<String> roleCodes) {
        List<SysRole> roles = findByCodes(roleCodes);
        if (roles.size() != roleCodes.size()) {
            throw new BizException("角色不存在");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        for (SysRole role : roles) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }
    }

    private RoleResponse toResponse(SysRole role) {
        return new RoleResponse(
                role.getId(),
                role.getRoleCode(),
                role.getRoleName(),
                role.getDescription(),
                permissionCodesByRoleCodes(Set.of(role.getRoleCode())),
                role.getCreatedAt()
        );
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        return role;
    }

    private void saveRolePermissions(Long roleId, Set<String> permissionCodes) {
        Map<String, SysPermission> permissionMap = permissionMapper.selectList(null)
                .stream()
                .collect(Collectors.toMap(SysPermission::getPermissionCode, Function.identity()));
        for (String permissionCode : permissionCodes) {
            if (!permissionMap.containsKey(permissionCode)) {
                throw new BizException("权限不存在: " + permissionCode);
            }
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        for (String permissionCode : permissionCodes) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionMap.get(permissionCode).getId());
            rolePermissionMapper.insert(relation);
        }
    }
}
