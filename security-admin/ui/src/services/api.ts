import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';
import { authService } from './authService';

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
 * 2. 处理401错误并跳转到登录页
 * 3. 提供无感知的用户体验
 */
export const businessApi: AxiosInstance = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:9001',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});
  
// ===== 请求拦截器配置 =====
/**
 * 请求拦截器：自动添加Authorization头
 * 
 * 执行流程：
 * 1. 从localStorage获取access_token
 * 2. 如果token存在，自动添加到请求头的Authorization字段
 * 3. 使用Bearer token格式：'Bearer <access_token>'
 */
const requestInterceptor = (config: any) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
    console.log('Adding Authorization header to request');
  }
  return config;
};

// ===== 响应错误拦截器配置 =====
/**
 * 响应错误拦截器：处理401错误
 * 
 * 设计用意：
 * 1. 当API返回401错误时，直接跳转到登录页
 * 2. 简化错误处理逻辑，提高可维护性
 * 
 * 执行流程：
 * 1. 检查错误状态码是否为401
 * 2. 如果是401，清除本地token并跳转到登录页
 * 3. 其他错误正常返回，由调用方处理
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

const responseErrorInterceptor = async (error: any) => {
  // 处理HTTP错误
  if (error.response) {
    const { status } = error.response;
    
    // 处理401错误 - 未授权
    if (status === 401) {
      console.log('Unauthorized access detected (401), redirecting to login page');
      
      // 清除本地token
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      localStorage.removeItem('token_expiry');
      
      // 跳转到登录页
      window.location.href = '/login';
    } 
    // 处理403错误 - 禁止访问
    else if (status === 403) {
      console.log('Access forbidden (403)');
      // 可以显示无权限提示或跳转到无权限页面
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
  }

  return Promise.reject(error);
};

// 配置业务API的拦截器
businessApi.interceptors.request.use(requestInterceptor);
businessApi.interceptors.response.use(responseInterceptor, responseErrorInterceptor);

// 导出默认的业务API实例
export default businessApi;