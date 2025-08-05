/**
 * @deprecated 该文件已被废弃，不再使用
 * 
 * 新的token刷新机制已经移到api.ts中，通过axios拦截器实现：
 * 1. 请求拦截器 - 检查token是否即将过期，在发送请求前刷新
 * 2. 响应拦截器 - 处理401错误，自动刷新token并重试请求
 * 
 * 这种基于拦截器的方式提供了更好的无感刷新体验，避免了定时器的问题。
 */

import { authService } from './authService';
import { TokenManager } from './tokenManager';

/**
 * Token自动刷新管理器
 * 提供Token的自动刷新功能，避免用户会话中断
 * @deprecated 已被api.ts中的拦截器方案取代
 */
export class TokenRefresher {
  private static refreshTimeout: NodeJS.Timeout | null = null;
  private static readonly EXPIRY_BUFFER = 100 * 1000; // 提前100秒刷新token
  
  /**
   * 初始化Token刷新机制
   * @deprecated 已被api.ts中的拦截器方案取代
   */
  static initialize(): void {
    console.warn('TokenRefresher.initialize() is deprecated. Using interceptor-based refresh mechanism instead.');
    
    // 原实现保持不变，但实际上不再使用
    // 清除可能存在的旧定时器
    this.stopRefreshCycle();
    
    // 立即检查一次token状态
    this.checkAndRefreshTokenIfNeeded();
    
    // 设置下一次刷新时间
    this.scheduleNextRefresh();
    
    // 当页面从后台恢复时重新计算刷新时间
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        this.checkAndRefreshTokenIfNeeded();
        this.scheduleNextRefresh();
      }
    });
    
    console.log('Token auto-refresh mechanism initialized');
  }
  
  /**
   * 停止Token刷新循环
   * @deprecated 已被api.ts中的拦截器方案取代
   */
  static stopRefreshCycle(): void {
    console.warn('TokenRefresher.stopRefreshCycle() is deprecated. Using interceptor-based refresh mechanism instead.');
    
    if (this.refreshTimeout) {
      clearTimeout(this.refreshTimeout);
      this.refreshTimeout = null;
      console.log('Token refresh cycle stopped');
    }
  }

  /**
   * 立即检查并在需要时刷新token
   */
  static async checkAndRefreshTokenIfNeeded(): Promise<void> {
    if (!authService.isAuthenticated()) {
      return;
    }

    // 获取token剩余有效时间
    const remainingTime = TokenManager.getTokenRemainingTime();
    
    // 如果剩余时间小于缓冲区，立即刷新
    if (remainingTime <= this.EXPIRY_BUFFER) {
      console.log(`Token will expire soon (${Math.round(remainingTime / 1000)}s remaining), refreshing now...`);
      try {
        await this.refreshToken();
      } catch (error) {
        console.error('Failed to refresh token during immediate check:', error);
      }
    } else {
      console.log(`Token still valid for ${Math.round(remainingTime / 1000)}s, no need to refresh yet`);
    }
  }
  
  /**
   * 安排下一次token刷新
   */
  static scheduleNextRefresh(): void {
    // 清除现有的定时器
    this.stopRefreshCycle();
    
    // 如果用户未认证，不设置刷新
    if (!authService.isAuthenticated()) {
      console.log('User not authenticated, skipping token refresh scheduling');
      return;
    }
    
    // 获取token剩余有效时间
    const remainingTime = TokenManager.getTokenRemainingTime();
    console.log(`Current token remaining time: ${Math.round(remainingTime / 1000)} seconds`);
    
    // 如果无法获取有效时间或时间为0，使用默认值（30分钟）
    const defaultExpiryTime = 30 * 60 * 1000; // 30分钟
    
    // 计算下次刷新时间：剩余时间减去缓冲区，如果小于0则立即刷新
    const timeUntilRefresh = remainingTime > this.EXPIRY_BUFFER 
      ? remainingTime - this.EXPIRY_BUFFER 
      : 0;
    
    // 如果剩余时间无效，使用默认值减去缓冲区
    const refreshDelay = timeUntilRefresh || (defaultExpiryTime - this.EXPIRY_BUFFER);
    
    const refreshTime = new Date(Date.now() + refreshDelay);
    console.log(`Scheduling next token refresh in ${Math.round(refreshDelay / 1000)} seconds at ${refreshTime.toLocaleString()}`);
    
    // 设置定时器
    this.refreshTimeout = setTimeout(() => {
      this.refreshToken();
    }, refreshDelay);
  }
  
  /**
   * 刷新Token并安排下一次刷新
   */
  static async refreshToken(): Promise<void> {
    try {
      // 只有在用户已认证时刷新
      if (authService.isAuthenticated()) {
        console.log('Refreshing token at', new Date().toLocaleString());
        const response = await authService.refreshToken();
        console.log('Token refreshed successfully');
      } else {
        console.log('User not authenticated, skipping token refresh');
      }
    } catch (error) {
      console.error('Failed to refresh token:', error);
      // 刷新失败，可能是refresh_token已过期
      // 不自动重定向，让API响应拦截器处理
    } finally {
      // 无论成功失败，都安排下一次刷新
      this.scheduleNextRefresh();
    }
  }
} 