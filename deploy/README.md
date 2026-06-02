# Payment Router Docker Compose 部署

本目录提供私有化部署的一期 Compose 编排，包含 MySQL、Redis、后端、前端和统一入口 Nginx。

## 目录说明

```text
deploy/
├── docker-compose.yml
├── .env.example
├── .env.production.example
├── nginx.conf
├── mysql-init/
│   ├── 001-init.sql
│   ├── 002-auth-system.sql
│   ├── 003-merchant-config.sql
│   ├── 004-pay-method.sql
│   ├── 005-route-engine.sql
│   └── 006-payment-gateway.sql
└── README.md
```

MySQL 容器首次创建数据卷时会自动执行 `mysql-init/*.sql`。如果已经启动过并生成了 `mysql-data` 卷，后续修改初始化 SQL 不会自动重新执行。

默认 MySQL 镜像是 `mysql:8.0`。如果已有数据卷来自更高版本 MySQL，例如 `8.4`，不要直接降级启动；应先备份迁移数据，或在 `.env` 中临时设置 `MYSQL_IMAGE=mysql:8.4` 复用旧数据卷。

## 快速启动

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

## 配置说明

所有敏感参数通过 `.env` 配置。生产部署前必须替换：

```text
MYSQL_PASSWORD
MYSQL_ROOT_PASSWORD
SPRING_DATASOURCE_PASSWORD
REDIS_PASSWORD
SPRING_DATA_REDIS_PASSWORD
PAY_ROUTER_JWT_SECRET
PAY_ROUTER_AES_KEY
```

生产环境建议从 `.env.production.example` 复制：

```bash
cp .env.production.example .env
```

并确认：

```text
PAY_ROUTER_ENFORCE_PRODUCTION_SECRETS=true
PAY_ROUTER_CHANNEL_ADAPTER=lfwin
PAY_ROUTER_LFWIN_BASE_URL=https://api2.lfwin.com
```

如果暂时使用 Mock 通道联调，可保持：

```text
PAY_ROUTER_CHANNEL_ADAPTER=mock
```

## 数据与日志

Compose 使用命名卷持久化数据：

```text
mysql-data -> /var/lib/mysql
redis-data -> /data
```

后端日志挂载到宿主机：

```text
deploy/logs/backend/payment-router-backend.log
```

日志默认保留 30 天，单文件 100MB 滚动，总量上限 3GB。

## 常用命令

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f nginx
docker compose restart backend
docker compose down
```

如需清空数据库和 Redis 数据后重新执行初始化 SQL：

```bash
docker compose down -v
docker compose up -d
```

## LFWin production deployment checklist

Before publishing a production build, copy `.env.production.example` to `.env` and replace all placeholder secrets. Confirm these values are set for the real LFWin channel:

```text
PAY_ROUTER_CHANNEL_ADAPTER=lfwin
PAY_ROUTER_LFWIN_BASE_URL=https://api2.lfwin.com
PAY_ROUTER_LFWIN_TIMEOUT_SECONDS=15
PAY_QUERY_ENABLED=true
```

After code changes, rebuild the backend and frontend images so the latest channel adapter and admin pages are included:

```bash
docker compose up -d --build backend frontend nginx
```

Then verify the unified entry point:

```bash
curl http://localhost/api/health
curl http://localhost/
```

Runtime `.env`, logs, root handover documents, local test pages, and local skill folders are development artifacts and should not be committed with the deployment package.
