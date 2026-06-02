package com.company.payrouter.modules.system.service;

import com.company.payrouter.modules.system.entity.SysOperationLog;
import com.company.payrouter.modules.system.mapper.SysOperationLogMapper;
import com.company.payrouter.security.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperationLogService {
    private final SysOperationLogMapper operationLogMapper;

    public OperationLogService(SysOperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void record(String operationType, String targetType, Object targetId, String content) {
        SysOperationLog log = new SysOperationLog();
        AuthUser currentUser = currentUser();
        if (currentUser != null) {
            log.setOperatorId(currentUser.userId());
            log.setOperatorName(currentUser.username());
        }
        log.setOperationType(operationType);
        log.setTargetType(targetType);
        log.setTargetId(targetId == null ? null : String.valueOf(targetId));
        log.setContent(content);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    private AuthUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser user) {
            return user;
        }
        return null;
    }
}
