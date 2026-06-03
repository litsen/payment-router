CREATE TABLE IF NOT EXISTS sys_login_security (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    fail_count INT NOT NULL DEFAULT 0,
    captcha_id VARCHAR(64) NULL,
    captcha_code VARCHAR(16) NULL,
    captcha_expires_at DATETIME NULL,
    last_fail_ip VARCHAR(64) NULL,
    locked_ip VARCHAR(64) NULL,
    ip_locked TINYINT(1) NOT NULL DEFAULT 0,
    user_locked TINYINT(1) NOT NULL DEFAULT 0,
    last_fail_time DATETIME NULL,
    locked_at DATETIME NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_login_security_locked_ip (ip_locked, locked_ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
