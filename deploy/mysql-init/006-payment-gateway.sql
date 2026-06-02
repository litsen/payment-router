CREATE TABLE IF NOT EXISTS pay_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    pool_id BIGINT NOT NULL,
    account_id BIGINT NULL,
    app_id VARCHAR(64) NOT NULL,
    pay_method VARCHAR(64) NOT NULL,
    merchant_order_no VARCHAR(128) NOT NULL,
    platform_order_no VARCHAR(128) NULL,
    channel_order_no VARCHAR(128) NULL,
    upstream_order_time VARCHAR(64) NULL,
    amount DECIMAL(18,2) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    auth_code_masked VARCHAR(64) NULL,
    notify_url VARCHAR(512) NULL,
    status VARCHAR(32) NOT NULL,
    route_type VARCHAR(64) NULL,
    route_record_id BIGINT NULL,
    upstream_response_code VARCHAR(64) NULL,
    upstream_response_msg VARCHAR(512) NULL,
    pay_success_time DATETIME NULL,
    expired_time DATETIME NULL,
    last_query_time DATETIME NULL,
    query_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pay_order_app_order (app_id, merchant_order_no),
    KEY idx_pay_order_status_time (status, created_at),
    KEY idx_pay_order_pool_time (pool_id, created_at),
    KEY idx_pay_order_account_time (account_id, created_at),
    KEY idx_pay_order_success_account_time (status, account_id, pay_success_time),
    KEY idx_pay_order_platform (platform_order_no),
    KEY idx_pay_order_channel (channel_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pay_order_request_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NULL,
    order_id BIGINT NULL,
    merchant_order_no VARCHAR(128) NULL,
    direction VARCHAR(32) NOT NULL,
    api_type VARCHAR(64) NOT NULL,
    request_url VARCHAR(512) NULL,
    request_headers_json TEXT NULL,
    request_body MEDIUMTEXT NULL,
    response_body MEDIUMTEXT NULL,
    http_status INT NULL,
    cost_ms BIGINT NULL,
    success TINYINT(1) NULL,
    error_code VARCHAR(64) NULL,
    result_status VARCHAR(32) NULL,
    error_message VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_pay_order_request_log_order (order_id),
    KEY idx_pay_order_request_log_merchant_order (merchant_order_no),
    KEY idx_pay_order_request_log_type_time (api_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pay_order_notify_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NULL,
    order_id BIGINT NULL,
    merchant_order_no VARCHAR(128) NULL,
    notify_body MEDIUMTEXT NULL,
    verified TINYINT(1) NULL,
    success TINYINT(1) NULL,
    error_message VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_pay_order_notify_log_order (order_id),
    KEY idx_pay_order_notify_log_merchant_order (merchant_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pay_refund_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    order_id BIGINT NOT NULL,
    pool_id BIGINT NOT NULL,
    account_id BIGINT NULL,
    app_id VARCHAR(64) NOT NULL,
    merchant_order_no VARCHAR(128) NOT NULL,
    merchant_refund_no VARCHAR(128) NOT NULL,
    platform_order_no VARCHAR(128) NULL,
    channel_order_no VARCHAR(128) NULL,
    upstream_refund_no VARCHAR(128) NULL,
    order_amount DECIMAL(18,2) NOT NULL,
    refund_amount DECIMAL(18,2) NOT NULL,
    reason VARCHAR(255) NULL,
    notify_url VARCHAR(512) NULL,
    status VARCHAR(32) NOT NULL,
    upstream_response_code VARCHAR(64) NULL,
    upstream_response_msg VARCHAR(512) NULL,
    upstream_raw_response MEDIUMTEXT NULL,
    refund_success_time DATETIME NULL,
    last_query_time DATETIME NULL,
    query_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pay_refund_app_refund (app_id, merchant_refund_no),
    KEY idx_pay_refund_order_order (order_id),
    KEY idx_pay_refund_order_merchant_order (merchant_order_no),
    KEY idx_pay_refund_order_status_time (status, created_at),
    KEY idx_pay_refund_order_pool_time (pool_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO sys_permission (permission_code, permission_name) VALUES
('refund:view', 'View refunds'),
('refund:manage', 'Manage refunds');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN' AND p.permission_code IN ('refund:view', 'refund:manage');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code = 'OPERATOR' AND p.permission_code IN ('refund:view', 'refund:manage');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code = 'AUDITOR' AND p.permission_code IN ('refund:view');
