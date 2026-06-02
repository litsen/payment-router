package com.company.payrouter.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.payrouter.modules.system.dto.PermissionTreeNode;
import com.company.payrouter.modules.system.entity.SysPermission;
import com.company.payrouter.modules.system.mapper.SysPermissionMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionService {
    private static final List<PermissionGroupDefinition> GROUPS = List.of(
            new PermissionGroupDefinition("首页看板", "group:dashboard", List.of(
                    "dashboard:view"
            )),
            new PermissionGroupDefinition("商户配置", "group:merchant", List.of(
                    "merchant:pool:view",
                    "merchant:pool:manage",
                    "merchant:account:view",
                    "merchant:account:manage",
                    "merchant:app:view",
                    "merchant:app:manage",
                    "paymethod:view",
                    "paymethod:manage",
                    "route:rule:view",
                    "route:rule:manage",
                    "route:record:view",
                    "route:test",
                    "order:view",
                    "order:manage",
                    "refund:view",
                    "refund:manage",
                    "order:log:view"
            )),
            new PermissionGroupDefinition("系统管理", "group:system", List.of(
                    "system:user:view",
                    "system:user:manage",
                    "system:role:view",
                    "system:role:manage"
            ))
    );

    private final SysPermissionMapper permissionMapper;

    public PermissionService(SysPermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public List<PermissionTreeNode> permissionTree() {
        List<SysPermission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getId)
        );
        Map<String, String> labelByCode = permissions.stream()
                .collect(Collectors.toMap(
                        SysPermission::getPermissionCode,
                        SysPermission::getPermissionName,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Set<String> groupedCodes = GROUPS.stream()
                .flatMap(group -> group.permissionCodes().stream())
                .collect(Collectors.toSet());

        List<PermissionTreeNode> tree = new ArrayList<>();
        for (PermissionGroupDefinition group : GROUPS) {
            List<PermissionTreeNode> children = group.permissionCodes().stream()
                    .filter(labelByCode::containsKey)
                    .map(code -> new PermissionTreeNode(displayName(code, labelByCode.get(code)), code, List.of()))
                    .toList();
            tree.add(new PermissionTreeNode(group.label(), group.value(), children));
        }

        List<PermissionTreeNode> otherChildren = permissions.stream()
                .filter(permission -> !groupedCodes.contains(permission.getPermissionCode()))
                .map(permission -> new PermissionTreeNode(displayName(permission.getPermissionCode(), permission.getPermissionName()), permission.getPermissionCode(), List.of()))
                .toList();
        if (!otherChildren.isEmpty()) {
            tree.add(new PermissionTreeNode("其他权限", "group:other", otherChildren));
        }
        return tree;
    }

    private record PermissionGroupDefinition(String label, String value, List<String> permissionCodes) {
    }

    private String displayName(String code, String fallback) {
        return switch (code) {
            case "dashboard:view" -> "查看首页";
            case "merchant:pool:view" -> "查看商户";
            case "merchant:pool:manage" -> "管理商户";
            case "merchant:account:view" -> "查看支付参数";
            case "merchant:account:manage" -> "管理支付参数";
            case "merchant:app:view" -> "查看下游应用";
            case "merchant:app:manage" -> "管理下游应用";
            case "paymethod:view" -> "查看支付方式";
            case "paymethod:manage" -> "管理支付方式";
            case "route:rule:view" -> "查看路由规则";
            case "route:rule:manage" -> "管理路由规则";
            case "route:record:view" -> "查看路由记录";
            case "route:test" -> "路由测试";
            case "order:view" -> "查看订单";
            case "order:manage" -> "管理订单";
            case "refund:view" -> "查看退款流水";
            case "refund:manage" -> "管理退款流水";
            case "order:log:view" -> "查看订单日志";
            case "system:user:view" -> "查看用户";
            case "system:user:manage" -> "管理用户";
            case "system:role:view" -> "查看角色";
            case "system:role:manage" -> "管理角色";
            default -> fallback;
        };
    }
}
