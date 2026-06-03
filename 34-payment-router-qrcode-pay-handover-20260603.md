# Payment Router 扫码支付接口调整交接文档

更新时间：2026-06-03

## 1. 本次目标

根据当前项目对外支付接口实现和 `payment-router-pay-api-skill`，重新区分两个扫码支付入口：

- 【聚合扫码支付接口】：Payment Router `POST /api/pay/scan`，对接 LFWin `/payapi/trans/kxpay`，固定上游 `service=pay.comm.jspay`。
- 【扫码支付接口】：Payment Router `POST /api/pay/qrcode`，对接 LFWin `/payapi/pay/qrcode`，下游请求必须指定 `service`，并参与下游 SHA256 签名。

`/api/pay/qrcode` 当前允许的 service：

```text
pay.alipay.qrcode
pay.wxpay.qrcode
pay.unpay.qrcode
```

## 2. 核心代码改动

### 2.1 后端支付接口

- `backend/src/main/java/com/company/payrouter/modules/gateway/dto/GatewayDtos.java`
  - `QrcodePayRequest` 字段由 `channel` 改为 `service`。

- `backend/src/main/java/com/company/payrouter/modules/gateway/channel/ChannelDtos.java`
  - `QrcodeChannelRequest` 字段由 `channel` 改为 `service`。

- `backend/src/main/java/com/company/payrouter/modules/gateway/service/GatewaySecurityService.java`
  - `/api/pay/qrcode` 验签参数由 `channel` 改为 `service`。
  - 请求日志参数同步记录 `service`。

- `backend/src/main/java/com/company/payrouter/modules/order/service/PaymentGatewayService.java`
  - 调试支付、真实 qrcode 支付链路改为传递 `service`。
  - 新增 qrcode service 白名单校验。
  - 上游日志记录实际请求 service。

- `backend/src/main/java/com/company/payrouter/modules/gateway/channel/LfwinPaymentChannelAdapter.java`
  - qrcode 支付不再根据 channel 推导 service，直接使用请求传入的 `service` 调用 `/payapi/pay/qrcode`。

- `backend/src/main/java/com/company/payrouter/modules/gateway/channel/MockPaymentChannelAdapter.java`
  - Mock qrcode 支付返回内容改为包含 `service`。

- `backend/src/main/java/com/company/payrouter/modules/route/dto/RouteDtos.java`
  - 后台路由支付测试请求字段由 `channel` 改为 `service`。

### 2.2 支付方式初始化

- `backend/src/main/java/com/company/payrouter/infrastructure/mysql/AuthSchemaInitializer.java`
  - `SCAN_PAY` 名称改为【聚合扫码支付接口】。
  - `QRCODE_PAY` 名称改为【扫码支付接口】。
  - 已有库启动时会通过初始化器更新 `pay_method` 名称和备注。

- `deploy/mysql-init/004-pay-method.sql`
  - 新库初始化数据同步上述名称和 LFWin 路径备注。

### 2.3 接口文档

- `backend/src/main/java/com/company/payrouter/modules/apidoc/service/ApiDocService.java`
  - `scan-pay` 标题改为【聚合扫码支付接口】。
  - 新增 `qrcode-pay` 文档，路径 `POST /api/pay/qrcode`。
  - `qrcode-pay` 请求参数新增必填 `service`，并说明参与签名。

- `backend/src/test/java/com/company/payrouter/modules/apidoc/service/ApiDocServiceTest.java`
  - 新增 `qrcode-pay` 文档 slug 覆盖。

注意：`backend/src/test/` 当前在 `.gitignore` 中被忽略。如需把测试文件提交到 GitHub，需要强制 add。

### 2.4 前端后台

- `frontend/src/layouts/BasicLayout.vue`
  - 接口文档菜单新增【扫码支付接口】。
  - 原 `scan-pay` 菜单改为【聚合扫码支付接口】。

- `frontend/src/router/index.ts`
  - 新增 `/api-docs/qrcode-pay` 路由。
  - `scan-pay` 路由标题改为【聚合扫码支付接口】。

- `frontend/src/views/merchant/MerchantAccountFormView.vue`
  - 支付参数配置中的支付方式文案同步。

- `frontend/src/views/route/RouteRulesView.vue`
  - 路由规则支付方式文案同步。

- `frontend/src/views/route/RouteTestView.vue`
  - `QRCODE_PAY` 测试字段由“指定通道”改为 `service` 下拉。
  - 下拉值直接使用 LFWin service：`pay.wxpay.qrcode`、`pay.alipay.qrcode`、`pay.unpay.qrcode`。

- `frontend/src/api/route.ts`
  - 路由测试 payload 字段由 `channel` 改为 `service`。

### 2.5 支付接口测试工具

- `frontend/public/pay-api-test.html`
  - 接口类型中新增【扫码支付接口】，请求 `/api/pay/qrcode`。
  - 原扫码接口改名为【聚合扫码支付接口】，请求 `/api/pay/scan`。
  - 新增 `service` 下拉字段，并参与签名。

### 2.6 Payment Router 支付 skill、smoke 和接口对接文档

以下文件当前被 `.gitignore` 忽略，但本地内容已经更新：

- `payment-router-pay-api-skill/SKILL.md`
  - 在 workflow 中明确 `/api/pay/scan` 是聚合扫码支付接口，`/api/pay/qrcode` 是必须传 `service` 的扫码支付接口。
  - 在示例生成规则中要求扫码支付优先使用 `/api/pay/qrcode` 并显式传入 `service`。

- `payment-router-pay-api-skill/references/payment-router-pay-api.md`
  - 接口清单新增【扫码支付】`POST /api/pay/qrcode`。
  - 额外字段说明新增 `service` 必填、参与签名，以及允许的 service 枚举。

- `32-payment-router-api-integration.md`
  - 原 `/api/pay/scan` 章节改为【聚合扫码支付接口】。
  - 新增正式【扫码支付接口】章节，路径 `POST /api/pay/qrcode`。
  - 删除附录中旧的 `channel` 版本 qrcode 接口说明。

- `scripts/smoke/04-pay-mock.ps1`
  - `QRCODE_PAY` smoke 参数由 `SMOKE_CHANNEL/channel` 改为 `SMOKE_SERVICE/service`。

- `scripts/smoke/README.md`
  - smoke 示例配置改为 `SMOKE_SERVICE=pay.wxpay.qrcode`。

## 3. 本地部署同步

已同步到本地部署：

```text
http://192.168.2.33/
```

执行命令：

```powershell
cd E:\www\SVN\workplace\payment-router\deploy
docker compose up -d --build backend frontend nginx
```

部署结果：

- `payment-router-backend` 已重建并启动。
- `payment-router-frontend` 已重建并启动。
- `payment-router-nginx` 保持运行，继续反代到新 frontend/backend。
- MySQL、Redis 数据卷未重置。

验证结果：

```powershell
Invoke-RestMethod -Uri 'http://192.168.2.33/api/health'
```

返回：

```text
code=0
data.status=UP
data.mysql=UP
data.redis=UP
```

接口文档验证：

```powershell
Invoke-RestMethod -Uri 'http://192.168.2.33/api/docs/qrcode-pay'
```

确认：

```text
requestPath=/api/pay/qrcode
requestParams 包含 service
service 枚举包含 pay.alipay.qrcode、pay.wxpay.qrcode、pay.unpay.qrcode
```

前端测试工具验证：

```powershell
Invoke-WebRequest -Uri 'http://192.168.2.33/pay-api-test.html'
```

确认页面包含：

```text
聚合扫码支付接口
扫码支付接口
pay.wxpay.qrcode
```

## 4. 构建和测试记录

前端构建：

```powershell
cd E:\www\SVN\workplace\payment-router\frontend
npm run build
```

结果：通过。

说明：仍有既有 Rollup `#__PURE__` 注释提示和 chunk size warning，不影响本次功能。

后端测试：

本机 Java 环境是 Java 8 JRE，缺少 `javac`，直接执行 Maven 测试会失败：

```text
No compiler is provided in this environment. Perhaps you are running on a JRE rather than a JDK?
```

已使用 Docker JDK 21 Maven 容器执行后端测试，容器退出码为 `0`：

```powershell
docker run --rm -v "${PWD}:/workspace" -w /workspace maven:3.9.9-eclipse-temurin-21 mvn -B test
```

部署构建命令中的后端镜像构建使用：

```text
mvn -B clean package -DskipTests
```

镜像构建结果：通过。

## 5. GitHub 提交命令整理

### 5.1 查看当前改动

```powershell
cd E:\www\SVN\workplace\payment-router
git status --short
git diff --stat
```

### 5.2 建议创建分支

```powershell
git switch -c codex/qrcode-pay-service
```

如果分支已存在：

```powershell
git switch codex/qrcode-pay-service
```

### 5.3 添加 Git 跟踪文件

```powershell
git add README.md `
  backend/src/main/java/com/company/payrouter/infrastructure/mysql/AuthSchemaInitializer.java `
  backend/src/main/java/com/company/payrouter/modules/apidoc/service/ApiDocService.java `
  backend/src/main/java/com/company/payrouter/modules/gateway/channel/ChannelDtos.java `
  backend/src/main/java/com/company/payrouter/modules/gateway/channel/LfwinPaymentChannelAdapter.java `
  backend/src/main/java/com/company/payrouter/modules/gateway/channel/MockPaymentChannelAdapter.java `
  backend/src/main/java/com/company/payrouter/modules/gateway/dto/GatewayDtos.java `
  backend/src/main/java/com/company/payrouter/modules/gateway/service/GatewaySecurityService.java `
  backend/src/main/java/com/company/payrouter/modules/order/service/PaymentGatewayService.java `
  backend/src/main/java/com/company/payrouter/modules/route/dto/RouteDtos.java `
  deploy/mysql-init/004-pay-method.sql `
  frontend/public/pay-api-test.html `
  frontend/src/api/route.ts `
  frontend/src/layouts/BasicLayout.vue `
  frontend/src/router/index.ts `
  frontend/src/views/merchant/MerchantAccountFormView.vue `
  frontend/src/views/route/RouteRulesView.vue `
  frontend/src/views/route/RouteTestView.vue `
  34-payment-router-qrcode-pay-handover-20260603.md
```

### 5.4 如需提交被忽略文件

以下文件被 `.gitignore` 忽略。如业务要求它们也进入 GitHub，需要使用 `-f`：

```powershell
git add -f `
  backend/src/test/java/com/company/payrouter/modules/apidoc/service/ApiDocServiceTest.java `
  payment-router-pay-api-skill/SKILL.md `
  payment-router-pay-api-skill/references/payment-router-pay-api.md `
  32-payment-router-api-integration.md `
  scripts/smoke/04-pay-mock.ps1 `
  scripts/smoke/README.md
```

如果不希望提交被忽略文件，至少需要在 PR 描述中说明这些本地辅助文档和 smoke 文件已更新但不随仓库提交。

### 5.5 提交

```powershell
git diff --cached --stat
git commit -m "feat: add qrcode pay service endpoint"
```

### 5.6 推送到 GitHub

```powershell
git push -u origin codex/qrcode-pay-service
```

### 5.7 创建 PR

如果本机已安装并登录 GitHub CLI：

```powershell
gh pr create `
  --base main `
  --head codex/qrcode-pay-service `
  --title "feat: add qrcode pay service endpoint" `
  --body "拆分聚合扫码支付接口和扫码支付接口；新增 /api/pay/qrcode service 必填签名参数；同步接口文档、前端菜单、测试工具和部署初始化数据。"
```

如主分支不是 `main`，请把 `--base main` 改成实际默认分支。

## 6. 上线和回归注意事项

- `/api/pay/qrcode` 的下游签名必须包含 `service`。
- 老的 `channel` 字段不再用于 `/api/pay/qrcode`。
- 生产使用 LFWin 时，`PAY_ROUTER_CHANNEL_ADAPTER=lfwin` 已在当前 `deploy/.env` 中配置。
- 若真实通道联调失败，优先检查：
  - 下游签名串是否包含 `service`。
  - service 是否为允许的三个值之一。
  - 支付参数是否支持 `QRCODE_PAY`。
  - LFWin `apikey/signkey` 是否匹配当前商户号。
