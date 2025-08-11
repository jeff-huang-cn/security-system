import React, { useState, useEffect } from 'react';
import { TokenManager } from '../../services/tokenManager';
import { authService } from '../../services/authService';

interface ProtectedRouteProps {
  children: React.ReactNode;
  permission?: string | string[]; // 可选的权限要求
}

/**
 * 路由保护组件
 * 检查用户是否已登录，可选检查是否拥有特定权限
 * 当用户未登录时，先尝试刷新token，刷新成功则继续访问，否则跳转登录页
 */
const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, permission }) => {
  const [isChecking, setIsChecking] = useState(true);
  const [isAuthorized, setIsAuthorized] = useState(false);
  const [hasRequiredPermission, setHasRequiredPermission] = useState(true);
  
  useEffect(() => {
    const checkAuthentication = async () => {
      try {
        // 检查是否已登录
        const isAuthenticated = TokenManager.isAuthenticated();
        
        if (isAuthenticated) {
          setIsAuthorized(true);
        } else {
          // 尝试刷新token
          console.log('ProtectedRoute: 用户未认证，尝试刷新token');
          
          try {
            await authService.refreshToken();
            // 刷新后再次检查认证状态
            const refreshSuccessful = TokenManager.isAuthenticated();
            
            if (refreshSuccessful) {
              console.log('ProtectedRoute: token刷新成功，继续访问');
              setIsAuthorized(true);
            } else {
              console.log('ProtectedRoute: token刷新后仍未认证，需要登录');
              setIsAuthorized(false);
            }
          } catch (error) {
            console.error('ProtectedRoute: token刷新失败', error);
            setIsAuthorized(false);
          }
        }
        
        // 检查权限（如果需要）
        if (permission && isAuthorized) {
          const { PermissionUtil } = require('../../utils/permissionUtil');
          
          const hasPermission = Array.isArray(permission)
            ? permission.some(p => PermissionUtil.hasPermission(p)) 
            : PermissionUtil.hasPermission(permission);
          
          setHasRequiredPermission(hasPermission);
        }
      } finally {
        setIsChecking(false);
      }
    };
    
    checkAuthentication();
  }, [permission]);
  
  // 加载中状态
  if (isChecking) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <p>验证中，请稍候...</p>
      </div>
    );
  }
  
  // 未认证状态
  if (!isAuthorized) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <h2>登录已过期</h2>
        <p>您的登录已过期，请重新登录</p>
        <button 
          onClick={() => window.location.replace('/login')}
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

  // 权限不足状态
  if (!hasRequiredPermission) {
    console.log('ProtectedRoute: 权限不足，显示权限提示');
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

  // 已认证且有权限
  return <>{children}</>;
};

export default ProtectedRoute;