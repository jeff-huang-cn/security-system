# OpenAPI 鉴权功能

## 功能概述

本模块实现了OpenAPI鉴权功能，允许客户端使用`app_id`和`app_secret`获取OAuth2访问令牌。该功能将客户端提供的应用凭证转换为标准的OAuth2客户端凭证，然后调用Spring Security OAuth2的token端点生成访问令牌。

## 核心组件

### 1. OpenApiTokenController
- **路径**: `com.webapp.security.sso.api.OpenApiTokenController`
- **功能**: 处理OpenAPI token请求，实现凭证转换和验证
- **端点**: `POST /v2/oauth2/token`

### 2. ApiConfig
- **路径**: `com.webapp.security.sso.api.config.ApiConfig`
- **功能**: 提供API模块所需的Bean配置（如RestTemplate）

## 数据库表结构

### sys_client_credential
存储客户端凭证信息：
- `app_id`: 应用ID
- `app_secret`: 应用密钥（加密存储）
- `client_id`: 关联的OAuth2客户端ID
- `status`: 凭证状态（1=启用，0=禁用）

### sys_resource
存储API资源信息：
- `resource_code`: 资源编码
- `resource_name`: 资源名称
- `resource_path`: API路径
- `http_method`: HTTP方法

### sys_credential_resource_rel
存储凭证与资源的关联关系：
- `credential_id`: 凭证ID
- `resource_id`: 资源ID

## API使用说明

### 请求格式

```bash
curl -XPOST 'http://localhost:8080/v2/oauth2/token' \
  -H 'Authorization: Basic <Base64Encode(app_id:app_secret)>' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=client_credentials'
```

### 请求参数

- **Authorization头**: `Basic <credentials>`
  - credentials = Base64编码的 `app_id:app_secret`
- **Content-Type**: `application/x-www-form-urlencoded`
- **grant_type**: 必须为 `client_credentials`

### 响应格式

#### 成功响应 (200)
```json
{
  "access_token": "dae7*********************e486",
  "expires_in": 7200,
  "token_type": "Bearer"
}
```

#### 错误响应
```json
{
  "error": {
    "code": "40001",
    "message": "Invalid client credentials"
  }
}
```

### 错误码说明

| HTTP状态码 | 错误类型 | 描述 |
|-----------|---------|------|
| 400 | unsupported_grant_type | 不支持的授权类型 |
| 401 | invalid_client | 无效的客户端凭证 |
| 500 | server_error | 服务器内部错误 |

## 实现流程

1. **请求验证**: 验证grant_type和Authorization头格式
2. **凭证解析**: 解码Basic认证，提取app_id和app_secret
3. **凭证验证**: 从数据库查询凭证信息并验证
4. **状态检查**: 检查凭证是否启用
5. **密钥验证**: 使用BCrypt验证app_secret
6. **客户端匹配**: 验证client_id是否为"openapi"
7. **凭证转换**: 将app凭证转换为OAuth2客户端凭证
8. **Token生成**: 调用Spring Security OAuth2端点生成token
9. **响应返回**: 返回标准格式的token响应

## 安全配置

### SecurityConfig更新
在`SecurityConfig.java`中添加了对`/v2/oauth2/**`路径的访问许可：

```java
.antMatchers("/login", "/logout", "/oauth2/**", "/v2/oauth2/**", "/.well-known/jwks.json",
        "/api/token-blacklist/**", "/favicon.ico",
        "/css/**", "/js/**", "/images/**", "/webjars/**", "/error")
.permitAll()
```

### OAuth2客户端配置
固定使用以下OAuth2客户端配置：
- **client_id**: `openapi`
- **client_secret**: `IPSG-YbDDJ4C_tscD-OuYfrfSmVW8UKV`

## 测试

使用提供的HTTP测试文件 `OpenApiTokenController.http` 进行功能测试：

1. 正常token获取
2. 无效grant_type测试
3. 缺少Authorization头测试
4. 无效Base64编码测试
5. 不存在的appId测试

## 注意事项

1. **密钥存储**: app_secret在数据库中使用BCrypt加密存储
2. **客户端限制**: 只支持client_id为"openapi"的客户端
3. **授权类型**: 仅支持client_credentials授权类型
4. **作用域**: 使用客户端配置的默认作用域，不在请求中显式指定
5. **错误处理**: 提供详细的错误信息和状态码
6. **日志记录**: 记录关键操作和错误信息用于调试

## 依赖关系

- Spring Security OAuth2 Authorization Server
- Spring Web
- MyBatis Plus
- BCrypt密码编码器
- RestTemplate HTTP客户端