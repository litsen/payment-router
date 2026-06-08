# Payment Router 支付路由后台

Payment Router 是一个面向私有化部署的多商户支付路由与限额分流后台，用于统一承接下游商户的支付、查单、退款、退款查询等请求，并按照商户配置、支付方式、路由规则、限额、可用时间和通道状态选择可用支付参数。

## 功能概览

- 商户管理、支付参数配置、支付方式配置
- 下游商户 `appId/appSecret` 接入与签名校验
- 条码支付、聚合扫码支付、扫码支付（指定 service）、H5 支付、微信 JSAPI、支付宝 JSAPI
- 查单、退款、退款查询、支付通知处理
- 路由规则、路由记录和路由测试
- 订单流水、退款流水、请求日志、通知日志
- 首页统计看板
- RBAC 用户、角色、权限管理
- 后端驱动接口文档与 Knife4j/Swagger 页面
- 敏感日志脱敏、业务错误码、生产安全配置校验
- Docker Compose 一键私有化部署

## 技术栈

后端：

- Java 21
- Spring Boot 3.3.5
- Spring Security + JWT
- MyBatis-Plus
- MySQL 8
- Redis
- Maven

前端：

- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios
- ECharts

部署：

- Docker Compose
- Nginx
- MySQL
- Redis

## 目录结构

```text
payment-router/
├── backend/              # Spring Boot 后端
├── frontend/             # Vue 3 管理后台
├── deploy/               # Docker Compose 私有化部署配置
│   ├── docker-compose.yml
│   ├── .env.example
│   ├── .env.production.example
│   ├── nginx.conf
│   ├── README.md
│   └── mysql-init/
└── README.md
```

## 快速部署

部署前请确认已经安装 Docker 和 Docker Compose。

```bash
cd deploy
cp .env.example .env
docker compose up -d
```

启动后访问：

```text
后台地址：http://localhost
后端接口：http://localhost/api
Swagger：http://localhost/api/doc.html
健康检查：http://localhost/api/health
```

默认后台账号：

```text
admin / admin123
```

## 部署服务说明

`deploy/docker-compose.yml` 包含以下服务：

```text
mysql    MySQL 8 数据库
redis    Redis 缓存
backend  Spring Boot 后端服务
frontend Vue 前端静态资源服务
nginx    统一反向代理入口
```

Nginx 默认暴露 `80` 端口：

- `/` 代理到前端
- `/api/` 代理到后端开放接口
- `/admin/` 代理到后端管理接口
- `/api/doc.html` 代理到后端 Knife4j/Swagger 页面

## 环境变量

开发或演示环境可以从 `.env.example` 复制：

```bash
cp deploy/.env.example deploy/.env
```

生产环境建议从 `.env.production.example` 复制，并替换所有敏感参数：

```bash
cp deploy/.env.production.example deploy/.env
```

生产环境必须重点修改：

```text
MYSQL_PASSWORD
MYSQL_ROOT_PASSWORD
SPRING_DATASOURCE_PASSWORD
REDIS_PASSWORD
SPRING_DATA_REDIS_PASSWORD
PAY_ROUTER_JWT_SECRET
PAY_ROUTER_AES_KEY
```

建议生产环境开启安全校验：

```text
PAY_ROUTER_ENFORCE_PRODUCTION_SECRETS=true
```

如果接入真实上游通道，请按生产环境模板配置对应的通道适配器、上游地址和超时时间。

如果暂时使用 Mock 通道联调：

```text
PAY_ROUTER_CHANNEL_ADAPTER=mock
```

## 数据持久化与日志

Compose 使用命名卷持久化数据：

```text
mysql-data -> /var/lib/mysql
redis-data -> /data
```

后端日志挂载到宿主机：

```text
deploy/logs/backend/payment-router-backend.log
```

日志默认按日期和大小滚动，保留 30 天，单文件上限 100MB，总量上限 3GB。

## 数据库初始化

首次创建 MySQL 数据卷时，容器会自动执行：

```text
deploy/mysql-init/*.sql
```

如果数据库卷已经存在，MySQL 官方镜像不会重复执行初始化 SQL。如需清空本地部署数据并重新初始化：

```bash
cd deploy
docker compose down -v
docker compose up -d
```

注意：`down -v` 会删除数据库和 Redis 数据卷，生产环境不要直接执行。

## MySQL 版本注意事项

默认镜像为：

```text
MYSQL_IMAGE=mysql:8.0
```

如果已有数据卷来自更高版本 MySQL，例如 `8.4`，不要直接用 `8.0` 启动旧数据卷，否则 MySQL 会拒绝降级启动。可以先备份迁移数据，或在 `.env` 中临时指定：

```text
MYSQL_IMAGE=mysql:8.4
```

## 常用部署命令

```bash
cd deploy

docker compose ps
docker compose logs -f backend
docker compose logs -f nginx
docker compose restart backend
docker compose down
```

重新构建并启动：

```bash
docker compose up -d --build
```

## 本地开发

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

常用本地地址：

```text
前端：http://127.0.0.1:5174
后端：http://127.0.0.1:18081
健康检查：http://127.0.0.1:18081/api/health
```

实际端口以本地配置和启动日志为准。

## 接口安全摘要

对外支付接口要求请求中携带：

```text
appId
timestamp
nonce
sign
```

签名摘要：

1. 排除 `sign` 字段
2. 忽略 `null` 值
3. 参数名按 ASCII 升序排序
4. 拼接为 `key=value&key=value`
5. 末尾直接拼接商户 `appSecret`
6. SHA256 后转小写十六进制

`appSecret` 由系统在商户管理中生成，明文只在创建或重置时展示一次。

## 许可证

本项目使用 MIT License，详见 [LICENSE](./LICENSE)。
