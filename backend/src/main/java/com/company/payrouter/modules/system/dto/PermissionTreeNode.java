package com.company.payrouter.modules.system.dto;

import java.util.List;

public record PermissionTreeNode(
        String label,
        String value,
        List<PermissionTreeNode> children
) {
}
