import React from 'react';
import { TokenManager } from '../../services/tokenManager';

interface ProtectedRouteProps {
  children: React.ReactNode;
  permission?: string | string[]; // 可选的权限要求
}

/**
 * 路由保护组件
 * 检查用户是否已登录，可选检查是否拥有特定权限
 */
const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, permission }) => {
  // 检查是否已登录
  const isAuthenticated = TokenManager.isAuthenticated();
  
  if (!isAuthenticated) {
    console.log('ProtectedRoute: 用户未认证，显示登录提示');
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <h2>登录已过期</h2>
        <p>您的登录已过期，请重新登录</p>
        <button 
          onClick={() => window.location.replace('/dashboard')}
          style={{
            padding: '8px 16px',
            backgroundColor: '#1890ff',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          重新登录
        </button>
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
            onClick={() => window.location.replace('/dashboard')}
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