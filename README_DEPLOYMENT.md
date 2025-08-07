# 🚀 安全系统 Docker 部署

## 快速开始

### Windows 用户
1. 确保 Docker Desktop 已启动
2. 双击运行 `start.bat`
3. 等待部署完成
4. 访问 http://localhost

### Linux/Mac 用户
1. 确保 Docker 和 docker-compose 已安装
2. 运行 `./start.sh`
3. 等待部署完成
4. 访问 http://localhost

## 手动部署

### 1. 环境准备
```bash
# 复制环境变量文件
cp env.example .env

# 创建日志目录
mkdir logs
```

### 2. 启动服务
```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps
```

### 3. 访问服务
- **前端管理界面**: http://localhost
- **Admin API**: http://localhost:9000
- **SSO服务**: http://localhost:8080

## 默认账户
- **用户名**: admin
- **密码**: admin123

## 常用命令

```bash
# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 重新构建
docker-compose up -d --build
```

## 服务架构

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Browser   │───▶│  security-ui│───▶│security-admin│
│             │    │   (nginx)   │    │   (Spring)  │
└─────────────┘    └─────────────┘    └─────────────┘
                           │                   │
                           ▼                   ▼
                   ┌─────────────┐    ┌─────────────┐
                   │security-sso │    │    MySQL    │
                   │  (Spring)   │    │  (Database) │
                   └─────────────┘    └─────────────┘
                           │                   │
                           ▼                   ▼
                   ┌─────────────┐    ┌─────────────┐
                   │    Redis    │    │    Logs     │
                   │   (Cache)   │    │  (Volume)   │
                   └─────────────┘    └─────────────┘
```

## 端口说明

| 服务 | 端口 | 说明 |
|------|------|------|
| security-ui | 80 | 前端管理界面 |
| security-admin | 9000 | 权限管理API |
| security-sso | 8080 | OAuth2认证服务 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |

## 故障排除

### 端口冲突
修改 `docker-compose.yml` 中的端口映射

### 服务启动失败
```bash
# 查看详细日志
docker-compose logs security-sso
docker-compose logs security-admin
```

### 数据库连接失败
检查 MySQL 服务是否正常启动

## 生产环境建议

1. 修改默认密码
2. 配置 HTTPS
3. 设置防火墙规则
4. 配置监控和备份 