CREATE TABLE IF NOT EXISTS sys_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(128) NOT NULL UNIQUE,
    setting_value TEXT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO sys_setting (setting_key, setting_value) VALUES
('siteName', '支付路由后台'),
('copyrightText', 'Copyright © xxx公司'),
('logoUrl', ''),
('loginBackgroundUrl', ''),
('faviconUrl', '');

INSERT IGNORE INTO sys_permission (permission_code, permission_name) VALUES
('system:settings:view', 'View system settings'),
('system:settings:manage', 'Manage system settings');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code IN ('system:settings:view', 'system:settings:manage');
