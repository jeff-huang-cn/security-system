
# OPENAPI 客户端认证与权限管理系统设计文档

## 一、设计背景与目标

### 1.1 背景

为支持外部系统安全对接内部服务，需提供一套标准化的 OPENAPI 认证授权机制，实现“客户端身份校验、接口权限管控、操作可追溯”的核心能力，同时兼容现有微服务架构和 Admin 权限体系。

### 1.2 目标

- 支持外部客户端通过`appid/appsecret`获取访问令牌（Token）；
- 实现客户端对 OPENAPI 的细粒度权限控制（可配置允许访问的接口）；
- 关联客户端创建者信息（`userid/username`），支持操作审计与业务追溯；
- 兼容 Spring Security OAuth2 标准，保障系统安全性与可扩展性。

## 二、核心流程设计

### 2.1 整体流程概览

```mermaid
graph TD
    A[客户端创建] --> B[配置API权限];
    B --> C[获取访问令牌(Token)];
    C --> D[携带Token调用OPENAPI];
    D --> E[Token校验+权限校验];
    E --> F[调用业务微服务(传递创建者信息)];
```

### 2.2 详细流程说明

#### （1）客户端创建与权限配置（Admin 系统操作）

- **操作人**：内部管理员（`sys_user`系统用户）；
- **流程**：
  1. 管理员在 Admin 界面填写客户端信息（名称、联系人等），系统自动生成`appid`（`client_id`）和`appsecret`（`client_secret`）；
  2. 系统将基础信息存入`oauth2_registered_client`表（符合 Spring Security OAuth2 标准）；
  3. 系统将创建者信息（`userid`）及业务扩展信息存入`oauth2_client_extension`表；
  4. 管理员在界面为客户端配置可访问的 OPENAPI 权限（选择`sys_resource`中的资源），关联关系存入`oauth2_client_resource_rel`表。

#### （2）令牌获取流程（外部客户端操作）

- **接口**：`POST /oauth2/token`（OAuth2 标准端点）；
- **请求参数**：

  ```http
  Content-Type: application/x-www-form-urlencoded
  Authorization: Basic Base64(appid:appsecret)  // 客户端凭证

  grant_type=client_credentials&scope=openapi
  ```

- **校验逻辑**：
  1. 系统解析`Authorization`头，获取`appid`和`appsecret`；
  2. 查询`oauth2_registered_client`表，比对`appsecret`是否匹配（加密校验）；
  3. 校验通过后，生成访问令牌（`access_token`），存入`oauth2_authorization`表；
- **响应示例**：
  ```json
  {
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 3600
  }
  ```

#### （3）OPENAPI 调用流程（外部客户端操作）

- **接口**：业务微服务提供的 OPENAPI（如`GET /api/v1/users`）；
- **请求头**：
  ```http
  Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...  // 步骤2获取的token
  ```
- **校验与转发逻辑**：
  1. **Token 校验**：OPENAPI 网关/认证服务解析`access_token`，提取`appid`，查询`oauth2_authorization`表验证令牌有效性（未过期、未吊销）；
  2. **客户端状态校验**：通过`appid`查询`oauth2_client_extension`表，校验客户端是否启用（`client_status=1`）；
  3. **权限校验**：解析当前请求的接口路径，匹配`sys_resource`中的资源，通过`oauth2_client_resource_rel`表校验客户端是否有权访问；
  4. **传递创建者信息**：校验通过后，通过`appid`查询`oauth2_client_extension`获取创建者`userid`和`username`，以请求头（如`X-Creator-Id`、`X-Creator-Name`）或内部上下文形式传递给业务微服务；
  5. **业务处理**：业务微服务接收请求，基于创建者信息进行审计或业务逻辑处理。

## 三、表结构设计

### 3.1 核心表清单

| 表名                         | 作用                                   | 类型       |
| ---------------------------- | -------------------------------------- | ---------- |
| `oauth2_registered_client`   | 存储客户端基础信息（符合 OAuth2 标准） | 框架标准表 |
| `oauth2_client_extension`    | 扩展客户端业务信息（关联创建者）       | 业务扩展表 |
| `sys_resource`               | 定义可开放的 API 资源                  | 业务基础表 |
| `oauth2_client_resource_rel` | 记录客户端与 API 资源的授权关系        | 关联关系表 |
| `oauth2_authorization`       | 存储令牌信息（框架自动维护）           | 框架标准表 |

### 3.2 详细表结构

#### （1）`oauth2_registered_client`（框架标准表）

```sql
CREATE TABLE oauth2_registered_client (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    client_id VARCHAR(100) NOT NULL UNIQUE COMMENT '客户端标识（appid）',
    client_secret VARCHAR(200) NOT NULL COMMENT '客户端密钥（appsecret，加密存储）',
    client_name VARCHAR(200) NOT NULL COMMENT '客户端名称',
    client_authentication_methods VARCHAR(1000) NOT NULL COMMENT '认证方式（如client_secret_basic）',
    authorization_grant_types VARCHAR(1000) NOT NULL COMMENT '授权类型（如client_credentials）',
    redirect_uris VARCHAR(1000) COMMENT '重定向URI（客户端凭证模式可不填）',
    scopes VARCHAR(1000) NOT NULL COMMENT '权限范围（如openapi）',
    client_settings VARCHAR(2000) NOT NULL COMMENT '客户端配置（JSON格式）',
    token_settings VARCHAR(2000) NOT NULL COMMENT '令牌配置（JSON格式，如过期时间）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2客户端标准表';
```

#### （2）`oauth2_client_extension`（业务扩展表）

```sql
CREATE TABLE oauth2_client_extension (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    client_id VARCHAR(100) NOT NULL UNIQUE COMMENT '关联oauth2_registered_client.client_id',
    user_id BIGINT NOT NULL COMMENT '创建者用户ID（关联sys_user.user_id）',
    client_status TINYINT DEFAULT 1 COMMENT '客户端状态（1-启用，0-禁用）',
    contact_name VARCHAR(100) COMMENT '联系人姓名',
    contact_phone VARCHAR(20) COMMENT '联系人电话',
    remark VARCHAR(500) COMMENT '备注信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (client_id) REFERENCES oauth2_registered_client(client_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2客户端扩展信息表';
```

#### （3）`sys_resource`（API 资源定义表）

```sql
CREATE TABLE sys_resource (
    resource_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    resource_code VARCHAR(100) NOT NULL UNIQUE COMMENT '资源编码（如user:query）',
    resource_name VARCHAR(100) NOT NULL COMMENT '资源名称（如用户查询接口）',
    resource_path VARCHAR(500) NOT NULL COMMENT '接口路径（如/api/v1/users/**）',
    method VARCHAR(10) NOT NULL COMMENT 'HTTP方法（GET/POST/PUT/DELETE）',
    status TINYINT DEFAULT 1 COMMENT '资源状态（1-启用，0-禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_resource_path (resource_path, method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OPENAPI资源定义表';
```

#### （4）`oauth2_client_resource_rel`（客户端-资源关联表）

```sql
CREATE TABLE oauth2_client_resource_rel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    client_id VARCHAR(100) NOT NULL COMMENT '关联oauth2_registered_client.client_id',
    resource_id BIGINT NOT NULL COMMENT '关联sys_resource.resource_id',
    grant_user_id BIGINT NOT NULL COMMENT '授权人ID（关联sys_user.user_id）',
    grant_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    UNIQUE KEY uk_client_resource (client_id, resource_id) COMMENT '避免重复授权',
    FOREIGN KEY (client_id) REFERENCES oauth2_registered_client(client_id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES sys_resource(resource_id) ON DELETE CASCADE,
    FOREIGN KEY (grant_user_id) REFERENCES sys_user(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端-API资源授权关系表';
```

### 3.3 表关系图

```mermaid
erDiagram
    oauth2_registered_client ||--o{ oauth2_client_extension : "1对1关联"
    oauth2_registered_client ||--o{ oauth2_client_resource_rel : "1对多关联"
    sys_resource ||--o{ oauth2_client_resource_rel : "1对多关联"
    sys_user ||--o{ oauth2_client_extension : "1对多关联"
    sys_user ||--o{ oauth2_client_resource_rel : "1对多关联"
```

## 四、权限控制设计

### 4.1 资源权限粒度

- **资源定义**：以“接口路径+HTTP 方法”为最小粒度（如`GET /api/v1/users`和`POST /api/v1/users`为两个不同资源）；
- **权限配置**：管理员为客户端勾选可访问的资源（`sys_resource`），关联关系存入`oauth2_client_resource_rel`表。

### 4.2 权限校验逻辑

在 OPENAPI 网关/认证服务中实现拦截器，核心逻辑：

```java
// 伪代码
public boolean checkPermission(HttpServletRequest request, String clientId) {
    // 1. 获取当前请求的路径和方法
    String path = request.getRequestURI();
    String method = request.getMethod();

    // 2. 匹配对应的资源ID
    Long resourceId = sysResourceMapper.matchResource(path, method);
    if (resourceId == null) {
        return false; // 未定义的资源，拒绝访问
    }

    // 3. 校验客户端是否有权访问该资源
    return clientResourceRelMapper.exists(clientId, resourceId);
}
```

## 五、接口规范

### 5.1 令牌获取接口（`/oauth2/token`）

- **请求方式**：POST
- **请求头**：`Authorization: Basic Base64(appid:appsecret)`
- **请求参数**：`grant_type=client_credentials&scope=openapi`
- **响应格式**：
  ```json
  {
    "access_token": "字符串令牌",
    "token_type": "Bearer",
    "expires_in": 3600 // 过期时间（秒）
  }
  ```

### 5.2 OPENAPI 调用规范

- **请求头**：`Authorization: Bearer {access_token}`
- **创建者信息传递**：网关自动添加请求头
  ```http
  X-Creator-Id: 10086  // 客户端创建者userid
  X-Creator-Name: "张三"  // 客户端创建者username
  ```

### 5.3 错误码定义

| 错误码 | 描述           | 场景示例                        |
| ------ | -------------- | ------------------------------- |
| 401    | 未授权         | Token 无效、过期或未携带        |
| 403    | 权限不足       | 客户端未配置当前接口的访问权限  |
| 400    | 无效请求       | 缺少 appid/appsecret 或参数错误 |
| 500    | 服务器内部错误 | 服务端处理异常                  |

## 六、安全设计

1. **传输安全**：所有接口强制使用 HTTPS，防止`appsecret`和 Token 被窃听；
2. **凭证安全**：`appsecret`存储时使用 BCrypt 加密，禁止明文存储；
3. **令牌安全**：
   - 令牌采用 JWT 格式，包含签名（防止篡改）和过期时间（默认 1 小时）；
   - 支持令牌吊销（通过`oauth2_authorization`表标记状态）；
4. **权限隔离**：客户端仅能访问已授权的 API 资源，未授权资源直接拦截；
5. **审计日志**：记录客户端调用记录（包含`appid`、`userid`、接口路径、时间等），支持追溯。

## 七、与现有系统集成点

1. **Admin 系统**：
   - 新增“客户端管理”模块，支持创建、编辑、禁用客户端；
   - 新增“资源授权”模块，支持为客户端配置 API 权限；
2. **业务微服务**：
   - 从请求头获取`X-Creator-Id`和`X-Creator-Name`，用于业务逻辑或日志记录；
3. **认证服务**：
   - 复用 Spring Security OAuth2 组件，扩展拦截器实现自定义权限校验。

## 八、部署与扩展建议

1. **部署架构**：认证服务与 OPENAPI 网关独立部署，业务微服务按需扩展；
2. **性能优化**：
   - 缓存客户端信息和权限配置（如 Redis），减少数据库查询；
   - 令牌校验逻辑异步化，提升接口响应速度；
3. **扩展性**：未来支持多环境（测试/生产）客户端隔离，可通过`oauth2_client_extension`表增加`env`字段实现。

```

```
