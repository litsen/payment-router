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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO pay_method (tenant_id, method_code, method_name, enabled, sort_order, remark) VALUES
('default', 'PRE_ORDER', '统一收银台', 0, 5, '/index/Payment/pre_order'),
('default', 'BARCODE_PAY', '条码支付：商户扫顾客付款码', 1, 10, '/payapi/pay/barcode'),
('default', 'DECODE_BAR', '条码支付前解码', 0, 15, '/payapi/pay/decode_bar'),
('default', 'SCAN_PAY', '聚合扫码支付接口', 0, 20, '/payapi/trans/kxpay service=pay.comm.jspay'),
('default', 'QRCODE_PAY', '扫码支付接口', 0, 25, '/payapi/pay/qrcode service=pay.alipay.qrcode/pay.wxpay.qrcode/pay.unpay.qrcode'),
('default', 'H5_PAY', 'H5/链接跳转支付', 0, 30, '/payapi/pay/jspay3'),
('default', 'WECHAT_JSAPI_PAY', '微信公众号和小程序支付', 0, 40, '/payapi/mini/wxpay'),
('default', 'ALIPAY_JSAPI_PAY', '支付宝生活号和小程序支付', 0, 50, '/payapi/trade/alipay');

INSERT IGNORE INTO sys_permission (permission_code, permission_name) VALUES
('paymethod:view', 'View payment methods'),
('paymethod:manage', 'Manage payment methods');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code IN ('paymethod:view', 'paymethod:manage');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code = 'OPERATOR'
  AND p.permission_code IN ('paymethod:view', 'paymethod:manage');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code = 'AUDITOR'
  AND p.permission_code IN ('paymethod:view');
