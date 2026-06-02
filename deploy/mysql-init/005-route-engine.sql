CREATE TABLE IF NOT EXISTS pay_route_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    rule_name VARCHAR(128) NOT NULL,
    rule_code VARCHAR(64) NOT NULL UNIQUE,
    pool_id BIGINT NOT NULL,
    pay_method VARCHAR(64) NOT NULL,
    rule_type VARCHAR(64) NOT NULL,
    rule_config_json TEXT NULL,
    priority INT NOT NULL DEFAULT 100,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pay_route_rule_pool (pool_id),
    KEY idx_pay_route_rule_pool_method (pool_id, pay_method),
    KEY idx_pay_route_rule_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pay_route_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    order_id BIGINT NULL,
    merchant_order_no VARCHAR(128) NULL,
    pool_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    route_rule_id BIGINT NULL,
    route_type VARCHAR(64) NOT NULL,
    route_snapshot_json TEXT NULL,
    amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_pay_route_record_order (merchant_order_no),
    KEY idx_pay_route_record_account_time (account_id, created_at),
    KEY idx_pay_route_record_pool_time (pool_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pay_account_limit_bucket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    account_id BIGINT NOT NULL,
    period_type VARCHAR(16) NOT NULL,
    period_start DATETIME NOT NULL,
    period_end DATETIME NOT NULL,
    limit_amount DECIMAL(18,2) NOT NULL,
    used_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    reserved_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_limit_bucket_account_period (account_id, period_type, period_start),
    KEY idx_limit_bucket_period (period_type, period_start, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pay_account_limit_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    order_id BIGINT NOT NULL,
    merchant_order_no VARCHAR(128) NOT NULL,
    account_id BIGINT NOT NULL,
    bucket_id BIGINT NOT NULL,
    period_type VARCHAR(16) NOT NULL,
    period_start DATETIME NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    flow_status VARCHAR(16) NOT NULL,
    reserved_at DATETIME NOT NULL,
    confirmed_at DATETIME NULL,
    released_at DATETIME NULL,
    release_reason VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_limit_flow_order_period (order_id, period_type),
    KEY idx_limit_flow_account_status (account_id, flow_status),
    KEY idx_limit_flow_status_reserved (flow_status, reserved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO sys_permission (permission_code, permission_name) VALUES
('route:rule:view', 'View route rules'),
('route:rule:manage', 'Manage route rules'),
('route:record:view', 'View route records'),
('route:test', 'Test route engine');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code IN ('route:rule:view', 'route:rule:manage', 'route:record:view', 'route:test');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code = 'OPERATOR'
  AND p.permission_code IN ('route:rule:view', 'route:rule:manage', 'route:record:view', 'route:test');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code = 'AUDITOR'
  AND p.permission_code IN ('route:rule:view', 'route:record:view');
