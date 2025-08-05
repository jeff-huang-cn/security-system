import React, { useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import Dashboard from './components/Dashboard';
import Login from './components/Login';
import { authService } from './services/authService';

function App() {
  const [user, setUser] = useState<any>(null);
  
  // 组件挂载时检查用户登录状态
  useEffect(() => {
    // 检查用户是否已登录
    if (authService.isAuthenticated()) {
      // 从本地存储获取用户信息，或设置默认值
      const userString = localStorage.getItem('user');
      if (userString) {
        try {
          setUser(JSON.parse(userString));
        } catch (e) {
          setUser({ username: 'admin' }); // 默认用户
        }
      } else {
        setUser({ username: 'admin' }); // 默认用户
      }
    }
  }, []);
  
  // 处理登录成功
  const handleLogin = (token: string, userData: any) => {
    setUser(userData);
    // 保存用户信息到本地存储
    localStorage.setItem('user', JSON.stringify(userData));
    // 重定向到仪表盘
    window.location.href = '/dashboard';
  };
  
  // 处理登出
  const handleLogout = () => {
    setUser(null);
    localStorage.removeItem('user');
    // 重定向到登录页
    window.location.href = '/login';
  };

  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login onLogin={handleLogin} />} />
        <Route
          path="/dashboard/*"
          element={
            authService.isAuthenticated() ? (
              <Dashboard user={user || { username: 'admin' }} onLogout={handleLogout} />
            ) : (
              <Navigate to="/login" />
            )
          }
        />
        <Route path="/" element={<Navigate to="/dashboard" />} />
      </Routes>
    </Router>
  );
}

export default App;