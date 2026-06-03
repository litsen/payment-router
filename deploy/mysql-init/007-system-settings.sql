CREATE TABLE IF NOT EXISTS sys_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(128) NOT NULL UNIQUE,
    setting_value TEXT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO sys_setting (setting_key, setting_value) VALUES
('siteName', '支付路由后台'),
('copyrightText', 'Copyright © xxx公司'),
('logoUrl', '/brand/logo.png'),
('loginBackgroundUrl', '/brand/login-bg.png'),
('faviconUrl', '/brand/logo.png');

UPDATE sys_setting SET setting_value = '/brand/logo.png'
WHERE setting_key = 'logoUrl' AND (setting_value IS NULL OR setting_value = '');
UPDATE sys_setting SET setting_value = '/brand/login-bg.png'
WHERE setting_key = 'loginBackgroundUrl' AND (setting_value IS NULL OR setting_value = '');
UPDATE sys_setting SET setting_value = '/brand/logo.png'
WHERE setting_key = 'faviconUrl' AND (setting_value IS NULL OR setting_value = '');

INSERT IGNORE INTO sys_permission (permission_code, permission_name) VALUES
('system:settings:view', 'View system settings'),
('system:settings:manage', 'Manage system settings');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code IN ('system:settings:view', 'system:settings:manage');
