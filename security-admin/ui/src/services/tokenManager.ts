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
      const expiryTime = Date.now() + (expiresIn * 1000);
      localStorage.setItem(this.TOKEN_EXPIRY_KEY, expiryTime.toString());
      console.log(`Token will expire at: ${new Date(expiryTime).toLocaleString()}`);
    } else {
      // 如果没有提供有效的过期时间，使用默认值（30分钟）
      const defaultExpiry = Date.now() + (30 * 60 * 1000);
      localStorage.setItem(this.TOKEN_EXPIRY_KEY, defaultExpiry.toString());
      console.log(`Using default expiry time: ${new Date(defaultExpiry).toLocaleString()}`);
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
    
    return (expiry - now) <= fiveMinutes;
  }

  /**
   * 检查是否已认证
   */
  static isAuthenticated(): boolean {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();
    return !!(accessToken && refreshToken);
  }

  /**
   * 清除所有token
   */
  static clearTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.TOKEN_EXPIRY_KEY);
  }

  /**
   * 获取token的剩余有效时间（毫秒）
   */
  static getTokenRemainingTime(): number {
    const expiryTime = localStorage.getItem(this.TOKEN_EXPIRY_KEY);
    if (!expiryTime) {
      return 0;
    }
    
    const expiry = parseInt(expiryTime, 10);
    if (isNaN(expiry)) {
      return 0;
    }
    
    const now = Date.now();
    const remainingTime = Math.max(0, expiry - now);
    
    console.log(`Token remaining time: ${Math.round(remainingTime / 1000)} seconds`);
    return remainingTime;
  }
}