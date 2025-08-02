# 前端Token自动刷新机制详解

## 概述

本系统实现了一套完整的JWT token自动刷新机制，旨在提供无感知的用户体验，避免用户因token过期而频繁重新登录。

## 设计目标

1. **无感知体验**：用户在正常使用过程中不会感知到token的刷新过程
2. **安全性**：支持双token机制（access_token + refresh_token）
3. **并发控制**：防止多个请求同时触发token刷新
4. **错误处理**：优雅处理刷新失败的情况
5. **循环依赖避免**：认证API和业务API分离，避免循环调用

## 核心组件

### 1. 双API实例架构

#### authApi (认证API实例)
- **用途**：处理认证相关请求（登录、登出、刷新token）
- **特点**：不包含token自动刷新逻辑，避免循环依赖
- **标识**：请求会被标记为 `_isAuthApi = true`

#### businessApi (业务API实例)
- **用途**：处理所有业务请求（用户管理、角色管理等）
- **特点**：包含完整的token自动刷新机制
- **拦截器**：自动添加Authorization头，处理401错误

### 2. Token刷新机制

#### 核心变量
```typescript
let isRefreshing = false;           // 防止并发刷新的标志
let failedQueue: FailedRequest[] = []; // 失败请求队列
```

#### 执行流程

1. **请求发送**
   - businessApi自动添加Authorization头
   - 发送请求到后端

2. **401错误处理**
   - 检查是否为authApi请求（避免循环）
   - 检查是否正在刷新token

3. **并发控制**
   - 如果正在刷新：将请求加入队列等待
   - 如果未在刷新：启动刷新流程

4. **Token刷新**
   - 使用authApi调用 `/api/auth/refresh`
   - 更新localStorage中的token
   - 处理队列中的所有请求

5. **请求重试**
   - 使用新token重试原始请求
   - 返回最终结果

## 详细实现

### 1. 请求拦截器
```typescript
const requestInterceptor = (config: any) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
};
```

### 2. 响应错误拦截器
```typescript
const responseErrorInterceptor = async (error: AxiosError) => {
  const originalRequest = error.config as any;
  
  // 只处理401错误，且不是来自authApi的请求
  if (error.response?.status === 401 && !originalRequest._isAuthApi) {
    
    // 并发控制逻辑
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then(token => {
        originalRequest.headers.Authorization = `Bearer ${token}`;
        return axios(originalRequest);
      });
    }

    // 执行token刷新
    isRefreshing = true;
    try {
      const newToken = await refreshToken();
      processQueue(null, newToken);
      originalRequest.headers.Authorization = `Bearer ${newToken}`;
      return axios(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);
      // 清除token并跳转登录页
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      window.location.href = '/login';
      return Promise.reject(refreshError);
    }
  }

  return Promise.reject(error);
};
```

### 3. 队列处理机制
```typescript
const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token);
    }
  });
  
  failedQueue = [];
  isRefreshing = false;
};
```

## 后端接口要求

### 1. 登录接口 `/api/auth/login`
**请求**：
```json
{
  "username": "admin",
  "password": "password"
}
```

**响应**：
```json
{
  "success": true,
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 7200,
  "refresh_expires_in": 604800
}
```

### 2. 刷新接口 `/api/auth/refresh`
**请求**：
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIs..."
}
```

**响应**：
```json
{
  "success": true,
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 7200,
  "refresh_expires_in": 604800
}
```

### 3. 登出接口 `/api/auth/logout`
**请求**：
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIs..."
}
```

**响应**：
```json
{
  "success": true,
  "message": "登出成功"
}
```

## 使用示例

### 1. 在组件中使用
```typescript
import { userService, authService } from '../services';

// 业务API调用（自动处理token）
const users = await userService.getUsers();

// 认证API调用
const loginResult = await authService.login('admin', 'password');

// 检查认证状态
if (authService.isAuthenticated()) {
  // 用户已登录
}
```

### 2. 错误处理
```typescript
try {
  const result = await userService.createUser(userData);
} catch (error) {
  if (error.response?.status === 401) {
    // token已过期且刷新失败，用户会被自动跳转到登录页
  } else {
    // 其他业务错误
  }
}
```

## 优势特点

1. **用户体验优化**
   - 无感知的token刷新
   - 避免频繁登录

2. **技术优势**
   - 防重复刷新机制
   - 请求队列管理
   - 自动重试机制
   - 循环依赖避免

3. **安全性**
   - 双token机制
   - 服务端token注销
   - 自动清理过期token

4. **可维护性**
   - 模块化设计
   - 清晰的职责分离
   - 详细的注释说明

## 注意事项

1. **环境配置**
   - 确保 `REACT_APP_AUTH_API_URL` 和 `REACT_APP_BUSINESS_API_URL` 正确配置
   - 后端需要支持CORS跨域请求

2. **错误处理**
   - 刷新失败时会自动跳转到 `/login`
   - 可根据实际路由配置调整跳转路径

3. **性能考虑**
   - 并发请求会被合理排队处理
   - 避免了重复的token刷新请求

4. **调试建议**
   - 可通过浏览器开发者工具查看localStorage中的token
   - 网络面板可以观察token刷新的时机和过程