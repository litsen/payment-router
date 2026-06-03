package com.company.payrouter.infrastructure.mysql;

import com.company.payrouter.infrastructure.crypto.AesCryptoService;
import com.company.payrouter.modules.merchant.service.MerchantAppService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AesCryptoService cryptoService;
    private final boolean initSchema;

    public AuthSchemaInitializer(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            AesCryptoService cryptoService,
            @Value("${pay-router.init-schema:true}") boolean initSchema
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.cryptoService = cryptoService;
        this.initSchema = initSchema;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!initSchema) {
            return;
        }
        createTables();
        seedSystemSettings();
        seedPermissions();
        seedRoles();
        seedRolePermissions();
        seedDefaultMerchantApps();
        seedPayMethods();
        seedAdminUser();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_user (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(64) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    real_name VARCHAR(64) NOT NULL,
                    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
                    last_login_time DATETIME NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_role (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    role_code VARCHAR(64) NOT NULL UNIQUE,
                    role_name VARCHAR(64) NOT NULL,
                    description VARCHAR(255) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_permission (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    permission_code VARCHAR(128) NOT NULL UNIQUE,
                    permission_name VARCHAR(64) NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_user_role (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    role_id BIGINT NOT NULL,
                    UNIQUE KEY uk_user_role (user_id, role_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_role_permission (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    role_id BIGINT NOT NULL,
                    permission_id BIGINT NOT NULL,
                    UNIQUE KEY uk_role_permission (role_id, permission_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_operation_log (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    operator_id BIGINT NULL,
                    operator_name VARCHAR(64) NULL,
                    operation_type VARCHAR(64) NOT NULL,
                    target_type VARCHAR(64) NOT NULL,
                    target_id VARCHAR(64) NULL,
                    content VARCHAR(512) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_setting (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    setting_key VARCHAR(128) NOT NULL UNIQUE,
                    setting_value TEXT NULL,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pay_merchant_pool (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
                    pool_name VARCHAR(128) NOT NULL,
                    pool_code VARCHAR(64) NOT NULL UNIQUE,
                    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
                    remark VARCHAR(512) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pay_merchant_account (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
                    pool_id BIGINT NOT NULL,
                    account_name VARCHAR(128) NOT NULL,
                    channel_code VARCHAR(64) NOT NULL,
                    api_key_encrypted TEXT NULL,
                    sign_key_encrypted TEXT NULL,
                    support_pay_methods VARCHAR(255) NOT NULL DEFAULT 'BARCODE_PAY',
                    priority INT NOT NULL DEFAULT 100,
                    weight INT NOT NULL DEFAULT 1,
                    daily_amount_limit DECIMAL(18,2) NULL,
                    monthly_amount_limit DECIMAL(18,2) NULL,
                    single_min_amount DECIMAL(18,2) NULL,
                    single_max_amount DECIMAL(18,2) NULL,
                    available_start_date DATE NULL,
                    available_end_date DATE NULL,
                    available_start_time TIME NULL,
                    available_end_time TIME NULL,
                    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
                    fail_count INT NOT NULL DEFAULT 0,
                    last_fail_time DATETIME NULL,
                    remark VARCHAR(512) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    KEY idx_pay_merchant_account_pool (pool_id),
                    KEY idx_pay_merchant_account_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        addColumnIfMissing("pay_merchant_account", "available_start_date", "DATE NULL");
        addColumnIfMissing("pay_merchant_account", "available_end_date", "DATE NULL");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pay_merchant_account_secret (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    account_id BIGINT NOT NULL UNIQUE,
                    api_key_encrypted TEXT NULL,
                    private_key_encrypted TEXT NULL,
                    public_key_encrypted TEXT NULL,
                    cert_path VARCHAR(512) NULL,
                    cert_password_encrypted TEXT NULL,
                    extra_config_json TEXT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pay_merchant_app (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
                    pool_id BIGINT NOT NULL,
                    app_id VARCHAR(64) NOT NULL UNIQUE,
                    app_name VARCHAR(128) NOT NULL,
                    secret_encrypted TEXT NULL,
                    notify_url_whitelist TEXT NULL,
                    rate_limit_per_minute INT NOT NULL DEFAULT 60,
                    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
                    remark VARCHAR(512) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    KEY idx_pay_merchant_app_pool (pool_id),
                    KEY idx_pay_merchant_app_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pay_method (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
                    method_code VARCHAR(64) NOT NULL UNIQUE,
                    method_name VARCHAR(64) NOT NULL,
                    enabled TINYINT(1) NOT NULL DEFAULT 0,
                    sort_order INT NOT NULL DEFAULT 100,
                    remark VARCHAR(512) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        addColumnIfMissing("pay_order", "platform_order_no", "VARCHAR(128) NULL");
        addColumnIfMissing("pay_order", "channel_order_no", "VARCHAR(128) NULL");
        addColumnIfMissing("pay_order", "pay_success_time", "DATETIME NULL");
        addColumnIfMissing("pay_order", "expired_time", "DATETIME NULL");
        addColumnIfMissing("pay_order", "last_query_time", "DATETIME NULL");
        addColumnIfMissing("pay_order", "query_count", "INT NOT NULL DEFAULT 0");
        addIndexIfMissing("pay_order", "idx_pay_order_success_account_time", "status, account_id, pay_success_time");
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }

    private void seedPermissions() {
        insertPermission("dashboard:view", "View dashboard");
        insertPermission("system:user:view", "View users");
        insertPermission("system:user:manage", "Manage users");
        insertPermission("system:role:view", "View roles");
        insertPermission("system:role:manage", "Manage roles");
        insertPermission("system:settings:view", "View system settings");
        insertPermission("system:settings:manage", "Manage system settings");
        insertPermission("merchant:pool:view", "View merchant pools");
        insertPermission("merchant:pool:manage", "Manage merchant pools");
        insertPermission("merchant:account:view", "View merchant accounts");
        insertPermission("merchant:account:manage", "Manage merchant accounts");
        insertPermission("merchant:app:view", "View merchant apps");
        insertPermission("merchant:app:manage", "Manage merchant apps");
        insertPermission("paymethod:view", "View payment methods");
        insertPermission("paymethod:manage", "Manage payment methods");
        insertPermission("route:rule:view", "View route rules");
        insertPermission("route:rule:manage", "Manage route rules");
        insertPermission("route:record:view", "View route records");
        insertPermission("route:test", "Test route engine");
        insertPermission("order:view", "View orders");
        insertPermission("order:manage", "Manage orders");
        insertPermission("order:log:view", "View order logs");
        insertPermission("refund:view", "View refunds");
        insertPermission("refund:manage", "Manage refunds");
    }

    private void seedRoles() {
        insertRole("SUPER_ADMIN", "Super Admin", "Full backend permissions");
        insertRole("OPERATOR", "Operator", "Daily operation permissions");
        insertRole("AUDITOR", "Auditor", "Read-only audit permissions");
    }

    private void seedRolePermissions() {
        grant("SUPER_ADMIN", "dashboard:view");
        grant("SUPER_ADMIN", "system:user:view");
        grant("SUPER_ADMIN", "system:user:manage");
        grant("SUPER_ADMIN", "system:role:view");
        grant("SUPER_ADMIN", "system:role:manage");
        grant("SUPER_ADMIN", "system:settings:view");
        grant("SUPER_ADMIN", "system:settings:manage");
        grant("SUPER_ADMIN", "merchant:pool:view");
        grant("SUPER_ADMIN", "merchant:pool:manage");
        grant("SUPER_ADMIN", "merchant:account:view");
        grant("SUPER_ADMIN", "merchant:account:manage");
        grant("SUPER_ADMIN", "merchant:app:view");
        grant("SUPER_ADMIN", "merchant:app:manage");
        grant("SUPER_ADMIN", "paymethod:view");
        grant("SUPER_ADMIN", "paymethod:manage");
        grant("SUPER_ADMIN", "route:rule:view");
        grant("SUPER_ADMIN", "route:rule:manage");
        grant("SUPER_ADMIN", "route:record:view");
        grant("SUPER_ADMIN", "route:test");
        grant("SUPER_ADMIN", "order:view");
        grant("SUPER_ADMIN", "order:manage");
        grant("SUPER_ADMIN", "order:log:view");
        grant("SUPER_ADMIN", "refund:view");
        grant("SUPER_ADMIN", "refund:manage");
        grant("OPERATOR", "dashboard:view");
        grant("OPERATOR", "merchant:pool:view");
        grant("OPERATOR", "merchant:pool:manage");
        grant("OPERATOR", "merchant:account:view");
        grant("OPERATOR", "merchant:account:manage");
        grant("OPERATOR", "merchant:app:view");
        grant("OPERATOR", "merchant:app:manage");
        grant("OPERATOR", "paymethod:view");
        grant("OPERATOR", "paymethod:manage");
        grant("OPERATOR", "route:rule:view");
        grant("OPERATOR", "route:rule:manage");
        grant("OPERATOR", "route:record:view");
        grant("OPERATOR", "route:test");
        grant("OPERATOR", "order:view");
        grant("OPERATOR", "order:manage");
        grant("OPERATOR", "order:log:view");
        grant("OPERATOR", "refund:view");
        grant("OPERATOR", "refund:manage");
        grant("AUDITOR", "dashboard:view");
        grant("AUDITOR", "merchant:pool:view");
        grant("AUDITOR", "merchant:account:view");
        grant("AUDITOR", "merchant:app:view");
        grant("AUDITOR", "paymethod:view");
        grant("AUDITOR", "route:rule:view");
        grant("AUDITOR", "route:record:view");
        grant("AUDITOR", "order:view");
        grant("AUDITOR", "order:log:view");
        grant("AUDITOR", "refund:view");
    }

    private void seedPayMethods() {
        insertPayMethod("PRE_ORDER", "统一收银台", false, 5, "LFWin /index/Payment/pre_order");
        insertPayMethod("BARCODE_PAY", "条码支付：商户扫顾客付款码", true, 10, "LFWin /payapi/pay/barcode");
        insertPayMethod("DECODE_BAR", "条码支付前解码", false, 15, "LFWin /payapi/pay/decode_bar");
        insertPayMethod("SCAN_PAY", "聚合扫码支付接口", false, 20, "LFWin /payapi/trans/kxpay service=pay.comm.jspay");
        insertPayMethod("QRCODE_PAY", "扫码支付接口", false, 25, "LFWin /payapi/pay/qrcode service=pay.alipay.qrcode/pay.wxpay.qrcode/pay.unpay.qrcode");
        insertPayMethod("H5_PAY", "H5/链接跳转支付", false, 30, "LFWin /payapi/pay/jspay3");
        insertPayMethod("WECHAT_JSAPI_PAY", "微信公众号和小程序支付", false, 40, "LFWin /payapi/mini/wxpay");
        insertPayMethod("ALIPAY_JSAPI_PAY", "支付宝生活号和小程序支付", false, 50, "LFWin /payapi/trade/alipay");
        jdbcTemplate.update("UPDATE pay_method SET method_name = ?, remark = ? WHERE method_code = ?", "条码支付：商户扫顾客付款码", "LFWin /payapi/pay/barcode", "BARCODE_PAY");
        jdbcTemplate.update("UPDATE pay_method SET method_name = ?, remark = ? WHERE method_code = ?", "聚合扫码支付接口", "LFWin /payapi/trans/kxpay service=pay.comm.jspay", "SCAN_PAY");
        jdbcTemplate.update("UPDATE pay_method SET method_name = ?, remark = ? WHERE method_code = ?", "扫码支付接口", "LFWin /payapi/pay/qrcode service=pay.alipay.qrcode/pay.wxpay.qrcode/pay.unpay.qrcode", "QRCODE_PAY");
        jdbcTemplate.update("UPDATE pay_method SET method_name = ?, remark = ? WHERE method_code = ?", "H5/链接跳转支付", "LFWin /payapi/pay/jspay3", "H5_PAY");
        jdbcTemplate.update("UPDATE pay_method SET method_name = ?, remark = ? WHERE method_code = ?", "微信公众号和小程序支付（旧编码，建议改用 WECHAT_JSAPI_PAY）", "Legacy alias", "JSAPI_PAY");
    }

    private void seedDefaultMerchantApps() {
        jdbcTemplate.queryForList("""
                SELECT id, tenant_id, pool_name, pool_code, status
                FROM pay_merchant_pool
                """).forEach(pool -> {
            String poolCode = String.valueOf(pool.get("pool_code"));
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_merchant_app WHERE app_id = ?", Integer.class, poolCode);
            if (count == null || count == 0) {
                jdbcTemplate.update("""
                        INSERT INTO pay_merchant_app (
                            tenant_id, pool_id, app_id, app_name, secret_encrypted,
                            notify_url_whitelist, rate_limit_per_minute, status, remark
                        ) VALUES (?, ?, ?, ?, ?, NULL, 60, ?, ?)
                        """,
                        pool.get("tenant_id"),
                        pool.get("id"),
                        poolCode,
                        pool.get("pool_name") + " 接口凭证",
                        cryptoService.encrypt(MerchantAppService.generateSecret()),
                        pool.get("status"),
                        "System-generated merchant access credential"
                );
            } else {
                jdbcTemplate.update("""
                        UPDATE pay_merchant_app
                        SET secret_encrypted = ?,
                            remark = 'System-generated merchant access credential'
                        WHERE app_id = ?
                          AND (
                              secret_encrypted IS NULL
                              OR secret_encrypted = ''
                              OR remark = 'Auto-created for legacy poolCode appId compatibility'
                          )
                        """, cryptoService.encrypt(MerchantAppService.generateSecret()), poolCode);
            }
        });
    }

    private void seedSystemSettings() {
        insertSetting("siteName", "支付路由后台");
        insertSetting("copyrightText", "Copyright © xxx公司");
        insertSetting("logoUrl", "");
        insertSetting("loginBackgroundUrl", "");
        insertSetting("faviconUrl", "");
    }

    private void seedAdminUser() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user WHERE username = 'admin'", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO sys_user (username, password, real_name, status) VALUES (?, ?, ?, ?)",
                "admin",
                passwordEncoder.encode("admin123"),
                "System Admin",
                "ENABLED"
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = 'admin'", Long.class);
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE role_code = 'SUPER_ADMIN'", Long.class);
        jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    private void insertPermission(String code, String name) {
        jdbcTemplate.update("INSERT IGNORE INTO sys_permission (permission_code, permission_name) VALUES (?, ?)", code, name);
    }

    private void insertPayMethod(String code, String name, boolean enabled, int sortOrder, String remark) {
        jdbcTemplate.update(
                "INSERT IGNORE INTO pay_method (tenant_id, method_code, method_name, enabled, sort_order, remark) VALUES (?, ?, ?, ?, ?, ?)",
                "default",
                code,
                name,
                enabled,
                sortOrder,
                remark
        );
    }

    private void insertSetting(String key, String value) {
        jdbcTemplate.update("INSERT IGNORE INTO sys_setting (setting_key, setting_value) VALUES (?, ?)", key, value);
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void addIndexIfMissing(String tableName, String indexName, String columns) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD INDEX " + indexName + " (" + columns + ")");
        }
    }

    private void insertRole(String code, String name, String description) {
        jdbcTemplate.update("INSERT IGNORE INTO sys_role (role_code, role_name, description) VALUES (?, ?, ?)", code, name, description);
    }

    private void grant(String roleCode, String permissionCode) {
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE role_code = ?", Long.class, roleCode);
        Long permissionId = jdbcTemplate.queryForObject("SELECT id FROM sys_permission WHERE permission_code = ?", Long.class, permissionCode);
        jdbcTemplate.update("INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)", roleId, permissionId);
    }
}
