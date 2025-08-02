# Web应用认证授权系统

基于Spring Boot + Spring Security + OAuth2的统一认证授权系统。

## 项目结构

```
webapp-auth-system/
├── auth-common/          # 公共模块
│   ├── entity/          # 实体类
│   ├── mapper/          # MyBatis映射器
│   └── service/         # 服务接口和实现
├── auth-service/        # 统一认证授权服务
│   ├── config/          # 配置类
│   ├── controller/      # 控制器
│   ├── security/        # 安全配置
│   └── service/         # 认证授权服务
├── auth-backend/        # 后台管理系统
│   ├── src/             # 后端代码
│   └── ui/              # 前端React应用
└── pom.xml              # 主项目配置
```

## 功能特性

- **统一认证授权**: 将认证和授权功能整合到单一服务中，简化架构
- **OAuth2认证服务器**: 支持授权码、密码、客户端凭证、刷新令牌等授权模式
- **JWT令牌**: 使用RSA签名的JWT令牌，包含用户信息和权限
- **RBAC权限模型**: 基于角色的访问控制，支持用户-角色-权限三级关联
- **资源保护**: 基于JWT令牌的API权限校验
- **数据库支持**: 使用MySQL存储用户、角色、权限数据
- **前后端分离**: React前端 + Spring Boot后端
- **现代化UI**: 基于Ant Design的管理界面

## 快速开始

### 1. 数据库准备

创建MySQL数据库 `auth_system`，并执行以下SQL脚本：

```sql
-- 创建数据库
CREATE DATABASE auth_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE auth_system;

-- 用户表
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    avatar VARCHAR(200) COMMENT '头像',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 角色表
CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(200) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 权限表
CREATE TABLE sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perm_name VARCHAR(50) NOT NULL COMMENT '权限名称',
    perm_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    perm_type TINYINT NOT NULL COMMENT '权限类型：1-菜单，2-按钮',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    path VARCHAR(200) COMMENT '路径',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id)
);

-- 角色权限关联表
CREATE TABLE sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_permission (role_id, permission_id)
);

-- 插入测试数据
-- 插入用户
INSERT INTO sys_user (username, password, real_name, email, phone) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '管理员', 'admin@example.com', '13800138000'),
('user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '普通用户', 'user@example.com', '13800138001');

-- 插入角色
INSERT INTO sys_role (role_name, role_code, description) VALUES
('超级管理员', 'ADMIN', '系统超级管理员'),
('普通用户', 'USER', '普通用户角色');

-- 插入权限
INSERT INTO sys_permission (perm_name, perm_code, perm_type, parent_id, path, sort_order) VALUES
('用户管理', 'user', 1, 0, '/user', 1),
('用户列表', 'user:list', 2, 1, '', 1),
('用户添加', 'user:add', 2, 1, '', 2),
('用户编辑', 'user:edit', 2, 1, '', 3),
('用户删除', 'user:delete', 2, 1, '', 4),
('角色管理', 'role', 1, 0, '/role', 2),
('角色列表', 'role:list', 2, 6, '', 1),
('角色添加', 'role:add', 2, 6, '', 2),
('角色编辑', 'role:edit', 2, 6, '', 3),
('角色删除', 'role:delete', 2, 6, '', 4),
('角色权限管理', 'role:manage', 2, 6, '', 5);

-- 分配用户角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), -- admin用户分配管理员角色
(2, 2); -- user用户分配普通用户角色

-- 分配角色权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
-- 管理员拥有所有权限
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
(1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11),
-- 普通用户只有查看权限
(2, 1), (2, 2), (2, 6), (2, 7);
```

### 2. 启动服务

1. **启动统一认证授权服务**:
   ```bash
   cd auth-service
   mvn spring-boot:run
   ```
   服务将在 http://localhost:9001 启动

2. **启动后台管理系统**:
   ```bash
   cd auth-backend
   mvn spring-boot:run
   ```
   后端服务将在 http://localhost:8080 启动

3. **启动前端管理界面**:
   ```bash
   cd auth-backend/ui
   npm install
   npm start
   ```
   前端界面将在 http://localhost:3000 启动

### 3. 测试认证流程

#### 获取访问令牌

```bash
# PowerShell
Invoke-WebRequest -Uri "http://localhost:9001/api/auth/login" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"username":"admin","password":"123456"}'

# 或使用curl (如果已安装)
curl -X POST http://localhost:9001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "123456"}'
```

#### 验证令牌

```bash
# PowerShell
Invoke-WebRequest -Uri "http://localhost:9001/api/auth/validate" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"token":"YOUR_ACCESS_TOKEN"}'
```

#### 获取当前用户信息

```bash
# PowerShell
Invoke-WebRequest -Uri "http://localhost:9001/api/auth/user/current" `
  -Method GET `
  -Headers @{"Authorization"="Bearer YOUR_ACCESS_TOKEN"}
```

## API接口

### 认证授权服务 (http://localhost:9001)

#### 认证相关
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出
- `POST /api/auth/validate` - 令牌验证
- `GET /api/auth/user/current` - 获取当前用户信息

#### OAuth2端点
- `GET /oauth2/authorize` - 授权端点
- `POST /oauth2/token` - 令牌端点
- `GET /.well-known/oauth-authorization-server` - 服务器元数据

### 后台管理系统 (http://localhost:8080)

#### 用户管理（需要相应权限）
- `GET /api/users` - 获取用户列表（需要 `user:list` 权限）
- `POST /api/users` - 添加用户（需要 `user:add` 权限）
- `PUT /api/users/{id}` - 更新用户（需要 `user:edit` 权限）
- `DELETE /api/users/{id}` - 删除用户（需要 `user:delete` 权限）
- `GET /api/users/me` - 获取当前用户信息

#### 角色管理（需要相应权限）
- `GET /api/roles` - 获取角色列表（需要 `role:list` 权限）
- `POST /api/roles` - 添加角色（需要 `role:add` 权限）
- `PUT /api/roles/{id}` - 更新角色（需要 `role:edit` 权限）
- `DELETE /api/roles/{id}` - 删除角色（需要 `role:delete` 权限）
- `POST /api/roles/{id}/permissions` - 分配角色权限（需要 `role:manage` 权限）

### 前端管理界面 (http://localhost:3000)

提供基于React + Ant Design的现代化管理界面，包括：
- 用户登录页面
- 用户管理界面
- 角色管理界面
- 权限分配界面

## 技术栈

### 后端
- **Spring Boot 2.7.5** - 应用框架
- **Spring Security** - 安全框架
- **Spring Security OAuth2 Authorization Server** - OAuth2认证服务器
- **MyBatis-Plus** - ORM框架
- **MySQL** - 数据库
- **Druid** - 数据库连接池
- **Hutool** - 工具库
- **Lombok** - 代码简化

### 前端
- **React 18** - 前端框架
- **TypeScript** - 类型安全
- **Ant Design** - UI组件库
- **Axios** - HTTP客户端
- **React Router** - 路由管理

## 架构优势

1. **统一服务**: 将认证和授权整合到单一服务中，减少服务间通信开销
2. **简化部署**: 减少了服务数量，降低了部署和运维复杂度
3. **标准化**: 遵循OAuth2和JWT标准，具有良好的兼容性
4. **可扩展**: 模块化设计，便于功能扩展和维护
5. **现代化**: 采用最新的技术栈，提供良好的开发体验

## 开发说明

### 环境要求
- JDK 8+
- Maven 3.6+
- MySQL 5.7+
- Node.js 16+

### 配置说明
- 数据库连接配置在 `auth-service/src/main/resources/application.yml`
- 前端环境变量配置在 `auth-backend/ui/.env.*` 文件中
  - `REACT_APP_AUTH_BASE_URL`: 认证服务地址 (默认: http://localhost:9001)
  - `REACT_APP_API_BASE_URL`: 后台管理服务地址 (默认: http://localhost:8080)
- OAuth2客户端配置在 `OAuth2AuthorizationServerConfig.java` 中

### 默认账户
- 管理员: admin / 123456
- 普通用户: user / 123456

## 注意事项

1. 默认用户密码为 `123456`，生产环境请及时修改
2. JWT令牌有效期为1小时，刷新令牌有效期为7天
3. 数据库连接信息需要根据实际环境修改
4. 生产环境请修改OAuth2客户端密钥和JWT签名密钥
5. 建议启用HTTPS以保护令牌传输安全
6. 确保MySQL服务正在运行并且数据库已正确创建