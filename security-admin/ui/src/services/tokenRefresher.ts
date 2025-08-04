import { authService } from './authService';
import { TokenManager } from './tokenManager';

/**
 * Token自动刷新管理器
 * 提供Token的自动刷新功能，避免用户会话中断
 */
export class TokenRefresher {
  private static refreshTimeout: NodeJS.Timeout | null = null;
  private static readonly EXPIRY_BUFFER = 100 * 1000; // 提前100秒刷新token
  
  /**
   * 初始化Token刷新机制
   */
  static initialize(): void {
    // 清除可能存在的旧定时器
    this.stopRefreshCycle();
    
    // 设置下一次刷新时间
    this.scheduleNextRefresh();
    
    // 当页面从后台恢复时重新计算刷新时间
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        this.scheduleNextRefresh();
      }
    });
    
    console.log('Token auto-refresh mechanism initialized');
  }
  
  /**
   * 停止Token刷新循环
   */
  static stopRefreshCycle(): void {
    if (this.refreshTimeout) {
      clearTimeout(this.refreshTimeout);
      this.refreshTimeout = null;
      console.log('Token refresh cycle stopped');
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