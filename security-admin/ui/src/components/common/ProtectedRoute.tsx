import React, { useEffect, useState } from 'react';
import { TokenManager } from '../../services/tokenManager';
import { authService } from '../../services/authService';
import { Spin } from 'antd';

interface ProtectedRouteProps {
  children: React.ReactNode;
  permission?: string | string[]; // 可选的权限要求
}

/**
 * 路由保护组件
 * 检查用户是否已登录，可选检查是否拥有特定权限
 */
const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, permission }) => {
  const [isLoading, setIsLoading] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(TokenManager.isAuthenticated());

  useEffect(() => {
    const attemptTokenRefresh = async () => {
      // 只有当未认证但有刷新token时尝试刷新
      if (!isAuthenticated && TokenManager.getRefreshToken()) {
        try {
          setIsLoading(true);
          console.log('ProtectedRoute: 尝试刷新token...');
          await authService.refreshToken();
          setIsAuthenticated(true);
          console.log('ProtectedRoute: token刷新成功');
        } catch (error) {
          console.error('ProtectedRoute: token刷新失败，将自动重定向到登录页', error);
          // 刷新失败时，api.ts的拦截器会自动处理重定向
        } finally {
          setIsLoading(false);
        }
      }
    };
    
    attemptTokenRefresh();
  }, [isAuthenticated]);
  
  // 如果正在加载，显示加载状态
  if (isLoading) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin tip="正在重新获取授权...">
          <div style={{ height: '100px' }} />
        </Spin>
      </div>
    );
  }
  
  // 检查是否已登录
  if (!isAuthenticated) {
    console.log('ProtectedRoute: 用户未认证，已经尝试过刷新token，将重定向到登录页');
    // 让api拦截器处理重定向，这里也做一个备用处理
    setTimeout(() => {
      if (window.location.pathname !== '/login') {
        window.location.replace('/login');
      }
    }, 100);
    
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin tip="正在重定向到登录页...">
          <div style={{ height: '100px' }} />
        </Spin>
      </div>
    );
  }

  // 如果指定了权限要求，需要进一步检查
  if (permission) {
    // 导入PermissionUtil
    const { PermissionUtil } = require('../../utils/permissionUtil');
    
    // 检查是否有所需权限
    const hasPermission = Array.isArray(permission)
      ? permission.some(p => PermissionUtil.hasPermission(p)) 
      : PermissionUtil.hasPermission(permission);
    
    if (!hasPermission) {
      console.log('ProtectedRoute: 权限不足，显示权限提示');
      // 无权限访问，可以返回自定义的无权限页面
      return (
        <div style={{ padding: 24, textAlign: 'center' }}>
          <h2>权限不足</h2>
          <p>您没有访问此页面的权限</p>
          <p>所需权限: {Array.isArray(permission) ? permission.join(', ') : permission}</p>
          <button 
            onClick={() => window.location.href = '/dashboard'}
            style={{
              padding: '8px 16px',
              backgroundColor: '#1890ff',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              marginTop: '16px'
            }}
          >
            返回首页
          </button>
        </div>
      );
    }
  }

  return <>{children}</>;
};

export default ProtectedRoute;