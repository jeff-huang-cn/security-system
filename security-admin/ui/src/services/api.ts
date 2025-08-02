import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';

// ===== API实例创建和配置 =====
/**
 * 认证API实例 (authApi)
 * 
 * 用途：专门处理认证相关的请求（登录、登出、刷新token等）
 * 特点：
 * 1. 不包含token自动刷新逻辑，避免循环依赖
 * 2. 用于token刷新时调用后端接口
 * 3. 标记请求为_isAuthApi=true，让业务API实例识别并跳过token刷新
 */
export const authApi: AxiosInstance = axios.create({
  baseURL: process.env.REACT_APP_AUTH_API_URL || 'http://localhost:9001',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 为authApi的请求添加标识，避免触发token刷新逻辑
authApi.interceptors.request.use((config) => {
  config._isAuthApi = true;  // 标记为认证API请求
  return config;
});

/**
 * 业务API实例 (businessApi)
 * 
 * 用途：处理所有业务相关的请求（用户管理、角色管理、权限管理等）
 * 特点：
 * 1. 自动添加Authorization头
 * 2. 自动处理401错误和token刷新
 * 3. 提供无感知的用户体验
 * 4. 支持请求队列和并发控制
 */
export const businessApi: AxiosInstance = axios.create({
  baseURL: process.env.REACT_APP_BUSINESS_API_URL || 'http://localhost:9002',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ===== Token自动刷新机制配置 =====
/**
 * Token自动刷新机制的核心变量
 * 
 * 设计用意：
 * 1. 防止多个并发请求同时触发token刷新，造成资源浪费和潜在的竞态条件
 * 2. 当token刷新进行中时，将后续的失败请求排队等待，刷新完成后统一重试
 * 3. 提供无感知的用户体验，避免用户频繁重新登录
 */

// 标记当前是否正在刷新token，防止并发刷新
let isRefreshing = false;

// 失败请求队列：存储在token刷新期间失败的请求，等待刷新完成后重试
interface FailedRequest {
  resolve: (value?: any) => void;  // 请求成功时的回调
  reject: (reason?: any) => void;  // 请求失败时的回调
}
let failedQueue: FailedRequest[] = [];

/**
 * 处理失败请求队列
 * 
 * @param error 如果token刷新失败，传入错误对象；如果成功，传入null
 * @param token 刷新成功后的新token
 * 
 * 执行流程：
 * 1. 遍历失败请求队列
 * 2. 如果token刷新成功，resolve所有排队的请求
 * 3. 如果token刷新失败，reject所有排队的请求
 * 4. 清空队列，重置刷新状态
 */
const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token);
    }
  });
  
  failedQueue = [];
};

/**
 * Token刷新函数
 * 
 * 执行流程：
 * 1. 从localStorage获取refresh_token
 * 2. 调用后端的/api/auth/refresh接口
 * 3. 更新localStorage中的access_token和refresh_token
 * 4. 返回新的access_token供后续请求使用
 * 
 * 注意：使用authApi实例避免循环依赖（authApi不会触发token刷新逻辑）
 */
const refreshToken = async (): Promise<string> => {
  const refreshToken = localStorage.getItem('refresh_token');
  if (!refreshToken) {
    throw new Error('No refresh token available');
  }

  try {
    // 使用authApi避免触发拦截器的token刷新逻辑
    const response = await authApi.post('/api/auth/refresh', {
      refresh_token: refreshToken
    });
    
    const { access_token, refresh_token: newRefreshToken } = response.data;
    
    // 更新本地存储的token
    localStorage.setItem('access_token', access_token);
    if (newRefreshToken) {
      localStorage.setItem('refresh_token', newRefreshToken);
    }
    
    return access_token;
  } catch (error) {
    // 刷新失败，清除所有token并跳转到登录页
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    window.location.href = '/login';
    throw error;
  }
};

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
  }
  return config;
};

// ===== 响应错误拦截器配置 =====
/**
 * 响应错误拦截器：处理401错误和自动token刷新
 * 
 * 设计用意：
 * 1. 当API返回401错误时，自动尝试刷新token
 * 2. 避免用户感知到token过期，提供无缝的用户体验
 * 3. 处理并发请求的token刷新，避免重复刷新
 * 
 * 执行流程：
 * 1. 检查错误状态码是否为401
 * 2. 检查当前请求是否来自authApi（避免循环刷新）
 * 3. 如果正在刷新token，将当前请求加入队列等待
 * 4. 如果未在刷新，启动token刷新流程
 * 5. 刷新成功后，使用新token重试原始请求
 * 6. 刷新失败后，清除本地token并跳转到登录页
 */
const responseInterceptor = (response: any) => response;

const responseErrorInterceptor = async (error: any) => {
  const originalRequest = error.config;
  
  // 只处理401错误，且不是来自authApi的请求（避免循环）
  if (error.response?.status === 401 && !originalRequest._isAuthApi) {
    
    // 如果正在刷新token，将请求加入队列
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then(token => {
        // 使用新token重试请求
        originalRequest.headers.Authorization = `Bearer ${token}`;
        return axios(originalRequest);
      }).catch(err => {
        return Promise.reject(err);
      });
    }

    // 标记为正在刷新，防止并发刷新
    originalRequest._retry = true;
    isRefreshing = true;

    try {
      // 执行token刷新
      const newToken = await refreshToken();
      
      // 刷新成功，处理队列中的请求
      processQueue(null, newToken);
      
      // 使用新token重试原始请求
      originalRequest.headers.Authorization = `Bearer ${newToken}`;
      return axios(originalRequest);
    } catch (refreshError) {
      // 刷新失败，处理队列中的请求
      processQueue(refreshError, null);
      
      // 清除本地token
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      
      // 跳转到登录页（可根据实际路由配置调整）
      window.location.href = '/login';
      
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }

  return Promise.reject(error);
};

// 配置业务API的拦截器
businessApi.interceptors.request.use(requestInterceptor);
businessApi.interceptors.response.use(responseInterceptor, responseErrorInterceptor);

// 导出默认的业务API实例
export default businessApi;