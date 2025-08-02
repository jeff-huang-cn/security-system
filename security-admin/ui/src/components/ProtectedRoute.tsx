import React from 'react';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const token = localStorage.getItem('access_token');
  
  if (!token) {
    return null; // 如果没有token，返回null，由App组件处理重定向到登录页
  }

  return <>{children}</>;
};

export default ProtectedRoute;