import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { authService } from './authService';
import { TokenManager } from './tokenManager';

// ===== API实例创建和配置 =====
/**
 * 认证API实例 (authApi)
 * 
 * 用途：专门处理认证相关的请求（登录、登出、刷新token等）
 * 特点：
 * 1. 直接对接SSO服务进行用户认证
 * 2. 不包含token自动刷新逻辑，避免循环依赖
 * 3. 标记请求为_isAuthApi=true，让业务API实例识别并跳过token刷新
 */
export const authApi: AxiosInstance = axios.create({
  baseURL: process.env.REACT_APP_AUTH_BASE_URL || 'http://localhost:9000',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'X-Client-Id': 'webapp-client'  // SSO服务需要的客户端ID
  },
});

// 为authApi的请求添加标识，避免触发token刷新逻辑
authApi.interceptors.request.use((config: any) => {
  config._isAuthApi = true;  // 标记为认证API请求
  return config;
});

/**
 * 业务API实例 (businessApi)
 * 
 * 用途：处理所有业务相关的请求（用户管理、角色管理、权限管理等）
 * 特点：
 * 1. 自动添加Authorization头
 * 2. 处理401错误并自动刷新token
 * 3. 提供无感知的用户体验
 */
export const businessApi: AxiosInstance = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:9001',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 简化后不再需要这些变量和函数
// 只需要一个简单的标志来防止短时间内重复刷新
let tokenRefreshTriggered = false;
// 防抖计时器
let tokenRefreshDebounceTimer: NodeJS.Timeout | null = null;

// ===== 请求拦截器配置 =====
/**
 * 请求拦截器：自动添加Authorization头
 * 并在token即将过期时刷新（只由一个请求触发，其他请求不受影响）
 * 
 * 优化: 
 * 1. 同一用户的并发请求中，只有第一个请求会触发刷新
 * 2. 其他请求继续使用旧但仍有效的token，不会等待刷新完成
 */
const requestInterceptor = async (config: any) => {
  // 如果是认证API的请求，直接返回配置
  if (config._isAuthApi) {
    return config;
  }
  
  // 每次请求都重新获取最新token，确保使用的是最新刷新的token
  const latestToken = TokenManager.getAccessToken();
  if (latestToken) {
    config.headers.Authorization = `Bearer ${latestToken}`;
    console.log(`【请求】使用token: ${latestToken}`);
  }
  // 检查是否需要刷新token（标志位优先判断，提高执行效率）
  if (!tokenRefreshTriggered && TokenManager.isAuthenticated() && TokenManager.isTokenExpiringSoon()) {
    // 设置防抖标志，5秒内不会重复刷新
    tokenRefreshTriggered = true;
    
    // 清除之前的定时器
    if (tokenRefreshDebounceTimer) {
      clearTimeout(tokenRefreshDebounceTimer);
    }
    
    // 5秒后重置防抖标志
    tokenRefreshDebounceTimer = setTimeout(() => {
      tokenRefreshTriggered = false;
      tokenRefreshDebounceTimer = null;
    }, 5000);
    
    console.log('Token is expiring soon, triggering background refresh');
    
    // 在后台刷新token，不阻塞当前请求
    authService.refreshToken()
      .then(response => {
        console.log('Token refreshed successfully in background');
        
        // 强制重置防抖标志，表明刷新已完成，其他请求可以使用新token
        tokenRefreshTriggered = false;
        
        // 清除之前的定时器
        if (tokenRefreshDebounceTimer) {
          clearTimeout(tokenRefreshDebounceTimer);
          tokenRefreshDebounceTimer = null;
        }
      })
      .catch(error => {
        console.error('Background token refresh failed:', error);
        // 刷新失败不影响当前请求，因为旧token仍然有效
      });
  }
  
  // 无论刷新是否触发，都直接返回请求配置
  return config;
};

// ===== 响应错误拦截器配置 =====
/**
 * 响应拦截器：处理返回数据
 */
const responseInterceptor = (response: any) => {  
  // 检查HTTP状态码
  if(response.status !== 200) {
    const error = new Error(response.data?.message || `HTTP错误: ${response.status}`);
    return Promise.reject(error);
  }

  // 检查响应数据中的code字段
  if (response.data && typeof response.data === 'object') {
    // 检查是否有code字段
    if ('code' in response.data) {
      if (response.data.code !== 'success') {
        // 如果code不是success，抛出业务逻辑错误
        const error = new Error(response.data.message || '操作失败');
        // 添加额外信息以便区分业务逻辑错误
        (error as any).isBusinessError = true;
        (error as any).code = response.data.code;
        return Promise.reject(error);
      }
      
      // 成功情况下，直接返回data字段内容
      return response.data.data;
    }
  }
  
  // 如果响应数据不符合预期格式，直接返回原始响应数据
  return response.data;
};

/**
 * 响应错误拦截器：处理401错误并自动刷新token
 */
const responseErrorInterceptor = async (error: any) => {
  // 获取原始请求配置
  const originalRequest = error.config;
  
  // 处理HTTP错误
  if (error.response) {
    const { status } = error.response;
    
          // 处理401错误 - 未授权
      if (status === 401) {
        // 如果是认证API的请求，不进行刷新尝试
        if (originalRequest._isAuthApi) {
          console.log('Auth API request unauthorized (401), redirecting to login page');
          TokenManager.clearTokens();
          window.location.href = '/login';
          return Promise.reject(error);
        }
        
        // 如果请求已经重试过一次，不要继续刷新，直接清除token并跳转登录
        if (originalRequest._retry) {
          console.log('已重试过请求但仍失败(401)，清除token并跳转登录');
          TokenManager.clearTokens();
          window.location.href = '/login';
          return Promise.reject(error);
        }
        
        console.log('Request unauthorized (401), attempting to refresh token');
        
        // 标记请求已经重试过，避免无限循环
        originalRequest._retry = true;
        
        try {
          // 刷新token
          const response = await authService.refreshToken();
          
          // 刷新成功后立即重置标志，避免其他请求重复刷新
          tokenRefreshTriggered = false;
          if (tokenRefreshDebounceTimer) {
            clearTimeout(tokenRefreshDebounceTimer);
            tokenRefreshDebounceTimer = null;
          }
          
          // 更新失败请求的Authorization头，总是使用最新的token
          const newToken = TokenManager.getAccessToken(); // 从TokenManager获取刷新后的token
          if (!newToken) {
            throw new Error('Failed to get new token after refresh');
          }
          
          // 设置新的认证头
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          console.log(`【重试】使用新token: ${newToken}`);
          
          // 确保其他可能的配置都是最新的
          
          // 重新发送原始请求
          console.log('Token refreshed, retrying original request');
          return axios(originalRequest);
        } catch (refreshError) {
          console.error('Token refresh failed during 401 handling:', refreshError);
          
          // 如果刷新token失败，清除token并跳转到登录页
          TokenManager.clearTokens();
          window.location.href = '/login';
          
          return Promise.reject(refreshError);
        }
    } 
    // 处理403错误 - 禁止访问
    else if (status === 403) {
      console.log('Access forbidden (403)');
      // 可以显示无权限提示或跳转到无权限页面
    }
    // 处理400错误 - 可能是因为token问题
    else if (status === 400) {
      console.log('Bad request (400) - may be due to token issues:', error);
      
      // 如果错误消息表明与token相关，可以尝试刷新或直接登出
      const errorMessage = error.response?.data?.message || '';
      if (errorMessage.toLowerCase().includes('token') || 
          errorMessage.toLowerCase().includes('auth') || 
          errorMessage.toLowerCase().includes('unauthorized')) {
        console.log('Token可能无效，尝试清除并跳转登录页');
        TokenManager.clearTokens();
        window.location.href = '/login';
      }
    }
    // 处理404错误 - 资源不存在
    else if (status === 404) {
      console.log('Resource not found (404)');
    }
    // 处理500错误 - 服务器错误
    else if (status >= 500) {
      console.log('Server error:', status);
    }
  } else if (error.request) {
    // 请求已发送但没有收到响应
    console.log('No response received:', error.request);
  } else {
    // 请求配置出错
    console.log('Request error:', error.message);
    
    // 检查是否与token相关的错误
    if (error.code === 'ERR_BAD_REQUEST' && !error._tokenErrorHandled) {
      console.log('ERR_BAD_REQUEST可能是token问题，尝试刷新token后重试一次');
      error._tokenErrorHandled = true;
      
      // 获取原始请求
      const originalRequest = error.config;
      
      // 避免无限循环
      if (!originalRequest._badRequestRetried) {
        originalRequest._badRequestRetried = true;
        
        try {
          // 尝试刷新token
          console.log('尝试刷新token后重新发送请求');
          const refreshResponse = await authService.refreshToken();
          
          // 使用新token重试
          const newToken = TokenManager.getAccessToken();
          if (newToken) {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return axios(originalRequest);
          }
        } catch (refreshError) {
          console.error('Token刷新失败，跳转到登录页:', refreshError);
          TokenManager.clearTokens();
          window.location.href = '/login';
        }
      } else {
        // 如果已经尝试过一次，就清除token并重定向
        console.log('多次尝试失败，可能是token无效，跳转到登录页');
        TokenManager.clearTokens();
        window.location.href = '/login';
      }
    }
  }

  return Promise.reject(error);
};

// 配置业务API的拦截器
businessApi.interceptors.request.use(requestInterceptor);
businessApi.interceptors.response.use(responseInterceptor, responseErrorInterceptor);

// 导出默认的业务API实例
export default businessApi;