import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import WechatCallback from './views/login/WechatCallback';

/**
 * 微信登录路由配置
 * 这个文件只包含微信登录相关的路由
 * 需要在主应用的路由配置中添加这些路由
 */
const WechatRoutes = [
  {
    path: "/oauth2/wechat/callback",
    element: <WechatCallback />
  }
];

export default WechatRoutes; 