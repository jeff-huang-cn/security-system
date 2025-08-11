import { create } from 'zustand';
import axios from 'axios';

// 请求队列项类型
interface QueueItem {
  config: any;
  resolve: (value: any) => void;
  reject: (error: any) => void;
}

// 定义状态接口
interface AuthState {
  // 状态属性
  accessToken: string | null;
  refreshToken: string | null;
  tokenExpiry: number | null;
  isRefreshing: boolean;
  requestQueue: QueueItem[];
  
  // 方法
  setTokens: (accessToken: string, refreshToken: string, expiresIn: number) => void;
  clearTokens: () => void;
  isTokenExpired: () => boolean;
  isTokenExpiringSoon: (bufferMinutes?: number) => boolean;
  refreshTokenAsync: () => Promise<any>;
  processQueue: (newToken: string) => void;
  clearQueue: (error?: any) => void;
}

// 创建store
export const useAuthStore = create<AuthState>((set, get) => ({
  // 初始状态
  accessToken: localStorage.getItem('access_token'),
  refreshToken: localStorage.getItem('refresh_token'),
  tokenExpiry: Number(localStorage.getItem('token_expiry')) || null,
  isRefreshing: false,
  requestQueue: [],
  
  // 设置tokens
  setTokens: (accessToken, refreshToken, expiresIn) => {
    const expiryTime = Date.now() + (expiresIn * 1000);
    
    localStorage.setItem('access_token', accessToken);
    localStorage.setItem('refresh_token', refreshToken);
    localStorage.setItem('token_expiry', expiryTime.toString());
    
    set({ 
      accessToken, 
      refreshToken, 
      tokenExpiry: expiryTime 
    });
  },
  
  // 清除tokens
  clearTokens: () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('token_expiry');
    
    set({ 
      accessToken: null, 
      refreshToken: null, 
      tokenExpiry: null 
    });
  },
  
  // 检查token是否已过期
  isTokenExpired: () => {
    const { tokenExpiry } = get();
    if (!tokenExpiry) return true;
    return Date.now() >= tokenExpiry;
  },
  
  // 检查token是否即将过期（默认2分钟）
  isTokenExpiringSoon: (bufferMinutes = 2) => {
    const { tokenExpiry } = get();
    if (!tokenExpiry) return true;
    
    const bufferMs = bufferMinutes * 60 * 1000;
    return (tokenExpiry - Date.now()) <= bufferMs;
  },
  
  // 刷新token
  refreshTokenAsync: async () => {
    const { refreshToken: currentRefreshToken, isRefreshing } = get();
    
    // 如果没有refreshToken，直接失败
    if (!currentRefreshToken) {
      return Promise.reject(new Error('No refresh token available'));
    }
    
    // 如果已经在刷新中，不重复刷新
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        const checkRefreshing = setInterval(() => {
          if (!get().isRefreshing) {
            clearInterval(checkRefreshing);
            resolve(get().accessToken);
          }
        }, 100);
        
        setTimeout(() => {
          clearInterval(checkRefreshing);
          reject(new Error('Token refresh timeout'));
        }, 10000);
      });
    }
    
    try {
      set({ isRefreshing: true });
      
      const response = await axios.post('/oauth2/refresh', { 
        refreshToken: currentRefreshToken 
      });
      
      const { access_token, refresh_token, expires_in } = response.data;
      
      get().setTokens(access_token, refresh_token, expires_in);
      get().processQueue(access_token);
      
      return response.data;
    } catch (error) {
      get().clearTokens();
      get().clearQueue(error);
      throw error;
    } finally {
      set({ isRefreshing: false });
    }
  },
  
  // 处理队列中的请求
  processQueue: (newToken) => {
    const { requestQueue } = get();
    
    requestQueue.forEach(({ config, resolve, reject }) => {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${newToken}`;
      
      axios(config)
        .then(response => resolve(response))
        .catch(error => reject(error));
    });
    
    set({ requestQueue: [] });
  },
  
  // 清空队列并拒绝所有请求
  clearQueue: (error) => {
    const { requestQueue } = get();
    
    requestQueue.forEach(({ reject }) => {
      reject(error || new Error('Authentication failed'));
    });
    
    set({ requestQueue: [] });
  }
}));