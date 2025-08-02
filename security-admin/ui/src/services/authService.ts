import { authApi } from './api';

/**
 * 认证服务模块
 * 
 * 职责：
 * 1. 封装所有认证相关的API调用
 * 2. 管理token的存储和获取
 * 3. 提供用户认证状态的判断
 * 4. 处理登录、登出、token刷新等核心认证流程
 */

export const authService = {
  /**
   * 用户登录
   * 
   * @param username 用户名
   * @param password 密码
   * @returns Promise<any> 登录响应数据
   * 
   * 执行流程：
   * 1. 调用后端登录接口
   * 2. 获取access_token和refresh_token
   * 3. 将token保存到localStorage
   * 4. 返回完整的响应数据供组件使用
   */
  login: async (username: string, password: string) => {
    const response = await authApi.post('/api/auth/login', {
      username,
      password
    });

    // 提取token信息
    const { access_token, refresh_token } = response.data;
    
    // 保存access_token到本地存储
    if (access_token) {
      localStorage.setItem('access_token', access_token);
    }
    
    // 保存refresh_token到本地存储（用于后续自动刷新）
    if (refresh_token) {
      localStorage.setItem('refresh_token', refresh_token);
    }

    return response.data;
  },

  /**
   * 刷新访问令牌
   * 
   * @param refreshToken 可选的刷新令牌，如果不提供则从localStorage获取
   * @returns Promise<any> 刷新响应数据
   * 
   * 执行流程：
   * 1. 获取refresh_token（参数传入或从localStorage读取）
   * 2. 调用后端刷新接口
   * 3. 更新localStorage中的token
   * 4. 返回新的token信息
   * 
   * 注意：此函数主要供api.ts中的自动刷新机制调用
   */
  refreshToken: async (refreshToken?: string) => {
    const token = refreshToken || localStorage.getItem('refresh_token');
    if (!token) {
      throw new Error('No refresh token available');
    }

    const response = await authApi.post('/api/auth/refresh', {
      refresh_token: token
    });

    // 更新本地存储的token
    const { access_token, refresh_token: newRefreshToken } = response.data;
    
    if (access_token) {
      localStorage.setItem('access_token', access_token);
    }
    
    if (newRefreshToken) {
      localStorage.setItem('refresh_token', newRefreshToken);
    }

    return response.data;
  },

  /**
   * 获取当前用户信息
   * 
   * @returns Promise<any> 用户信息
   * 
   * 执行流程：
   * 1. 调用后端获取当前用户接口
   * 2. 返回用户详细信息
   * 
   * 注意：此接口会自动携带Authorization头，由api.ts的拦截器处理
   */
  getCurrentUser: async () => {
    const response = await authApi.get('/api/auth/user/current');
    return response.data;
  },

  /**
   * 用户登出
   * 
   * @returns Promise<any> 登出响应数据
   * 
   * 执行流程：
   * 1. 获取当前的refresh_token
   * 2. 调用后端登出接口（传递refresh_token用于服务端注销）
   * 3. 清除本地存储的所有token
   * 4. 返回登出结果
   * 
   * 设计考虑：
   * - 即使后端调用失败，也要清除本地token，确保前端状态正确
   * - 传递refresh_token给后端，让服务端能够将其加入黑名单
   */
  logout: async () => {
    try {
      // 获取refresh_token用于服务端注销
      const refreshToken = localStorage.getItem('refresh_token');
      if (refreshToken) {
        await authApi.post('/api/auth/logout', {
          refresh_token: refreshToken
        });
      }
    } catch (error) {
      // 即使后端登出失败，也要清除本地token
      console.error('Logout API call failed:', error);
    } finally {
      // 清除本地存储的token
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
    }
  },

  /**
   * 验证token有效性
   * 
   * @param token 要验证的token
   * @returns Promise<any> 验证结果
   * 
   * 执行流程：
   * 1. 调用后端token验证接口
   * 2. 返回验证结果
   * 
   * 用途：可用于页面加载时验证用户登录状态
   */
  validateToken: async (token: string) => {
    const response = await authApi.post('/api/auth/validate', { token });
    return response.data;
  },

  // ===== 工具函数 =====
  
  /**
   * 检查用户是否已认证
   * 
   * @returns boolean 是否已认证
   * 
   * 判断逻辑：
   * 1. 检查localStorage中是否存在access_token和refresh_token
   * 2. 两者都存在才认为用户已认证
   * 
   * 注意：这只是前端的简单判断，真正的认证状态需要后端验证
   */
  isAuthenticated: (): boolean => {
    const accessToken = localStorage.getItem('access_token');
    const refreshToken = localStorage.getItem('refresh_token');
    return !!(accessToken && refreshToken);
  },

  /**
   * 获取访问令牌
   * 
   * @returns string | null 访问令牌
   */
  getAccessToken: (): string | null => {
    return localStorage.getItem('access_token');
  },

  /**
   * 获取刷新令牌
   * 
   * @returns string | null 刷新令牌
   */
  getRefreshToken: (): string | null => {
    return localStorage.getItem('refresh_token');
  }
};