/**
 * Token管理工具类
 * 提供token的存储、获取、清除等功能
 */
export class TokenManager {
  private static readonly ACCESS_TOKEN_KEY = 'access_token';
  private static readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private static readonly TOKEN_EXPIRY_KEY = 'token_expiry';

  /**
   * 保存token信息
   * @param accessToken 访问令牌
   * @param refreshToken 刷新令牌
   * @param expiresIn 过期时间（秒）
   */
  static saveTokens(accessToken: string, refreshToken?: string, expiresIn?: number): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, accessToken);
    
    if (refreshToken) {
      localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
    }
    
    if (expiresIn && !isNaN(expiresIn)) {
      // 确保 expiresIn 是一个正数
      const validExpiresIn = Math.max(0, expiresIn);
      const expiryTime = Date.now() + (validExpiresIn * 1000);
      localStorage.setItem(this.TOKEN_EXPIRY_KEY, expiryTime.toString());
      console.log(`Token will expire at: ${new Date(expiryTime).toLocaleString()} (in ${validExpiresIn} seconds)`);
    } else {
      // 如果没有提供有效的过期时间，使用默认值（30分钟）
      const defaultExpiry = Date.now() + (30 * 60 * 1000);
      localStorage.setItem(this.TOKEN_EXPIRY_KEY, defaultExpiry.toString());
      console.log(`Using default expiry time: ${new Date(defaultExpiry).toLocaleString()} (in 30 minutes)`);
    }
  }

  /**
   * 获取访问令牌
   */
  static getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  /**
   * 获取刷新令牌
   */
  static getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  /**
   * 检查token是否即将过期（提前5分钟）
   */
  static isTokenExpiringSoon(): boolean {
    const expiryTime = localStorage.getItem(this.TOKEN_EXPIRY_KEY);
    if (!expiryTime) {
      return false;
    }
    
    const expiry = parseInt(expiryTime, 10);
    if (isNaN(expiry)) {
      return false;
    }
    
    const now = Date.now();
    const fiveMinutes = 5 * 60 * 1000; // 5分钟
    
    const isExpiring = (expiry - now) <= fiveMinutes;
    if (isExpiring) {
      console.log(`Token is expiring soon. Expires at: ${new Date(expiry).toLocaleString()}, now: ${new Date(now).toLocaleString()}`);
    }
    return isExpiring;
  }

  /**
   * 检查token是否已过期
   */
  static isTokenExpired(): boolean {
    const expiryTime = localStorage.getItem(this.TOKEN_EXPIRY_KEY);
    if (!expiryTime) {
      return true; // 如果没有过期时间，视为已过期
    }
    
    const expiry = parseInt(expiryTime, 10);
    if (isNaN(expiry)) {
      return true; // 如果过期时间无效，视为已过期
    }
    
    const now = Date.now();
    const isExpired = now >= expiry;
    
    if (isExpired) {
      console.log(`Token has expired. Expired at: ${new Date(expiry).toLocaleString()}, now: ${new Date(now).toLocaleString()}`);
    }
    
    return isExpired;
  }

  /**
   * 检查是否已认证
   */
  static isAuthenticated(): boolean {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();
    
    // 检查token是否存在且未过期
    const hasTokens = !!(accessToken && refreshToken);
    const notExpired = !this.isTokenExpired();
    
    return hasTokens && notExpired;
  }

  /**
   * 清除所有token
   */
  static clearTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.TOKEN_EXPIRY_KEY);
    console.log('All tokens cleared from localStorage');
  }

  /**
   * 获取token的剩余有效时间（毫秒）
   */
  static getTokenRemainingTime(): number {
    const expiryTime = localStorage.getItem(this.TOKEN_EXPIRY_KEY);
    if (!expiryTime) {
      console.log('No token expiry time found');
      return 0;
    }
    
    const expiry = parseInt(expiryTime, 10);
    if (isNaN(expiry)) {
      console.log('Invalid token expiry time');
      return 0;
    }
    
    const now = Date.now();
    const remainingTime = Math.max(0, expiry - now);
    
    console.log(`Token remaining time: ${Math.round(remainingTime / 1000)} seconds (expires at ${new Date(expiry).toLocaleString()})`);
    return remainingTime;
  }
}