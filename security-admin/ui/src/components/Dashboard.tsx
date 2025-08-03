import React, { useState, useEffect } from 'react';
import {
  Layout,
  Menu,
  Button,
  Avatar,
  Dropdown,
  Space,
  Breadcrumb,
  Card,
  Row,
  Col,
  Statistic,
  message
} from 'antd';
import {
  UserOutlined,
  TeamOutlined,
  SafetyOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  DashboardOutlined,
  SettingOutlined
} from '@ant-design/icons';
import UserManagement from './UserManagement';
import RoleManagement from './RoleManagement';
import PermissionManagement from './PermissionManagement';
import { authService } from '../services/authService';

const { Header, Sider, Content } = Layout;

interface DashboardProps {
  user: any;
  onLogout: () => void;
}

const Dashboard: React.FC<DashboardProps> = ({ user, onLogout }) => {
  const [collapsed, setCollapsed] = useState(false);
  const [selectedKey, setSelectedKey] = useState('dashboard');
  const [userInfo, setUserInfo] = useState<any>(null);

  useEffect(() => {
    // 从localStorage获取用户信息
    const token = localStorage.getItem('token');
    const user = localStorage.getItem('user');
    if (user) {
      setUserInfo(JSON.parse(user));
    }
  }, []);

  const handleLogout = async () => {
    try {
      await authService.logout();
      onLogout();
    } catch (error) {
      console.error('Logout failed:', error);
      // 即使登出失败，也清除本地存储并跳转到登录页
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      onLogout();
    }
  };

  const menuItems = [
    {
      key: 'dashboard',
      icon: <DashboardOutlined />,
      label: '仪表盘',
    },
    {
      key: 'system',
      icon: <SettingOutlined />,
      label: '系统管理',
      children: [
        {
          key: 'users',
          icon: <UserOutlined />,
          label: '用户管理',
        },
        {
          key: 'roles',
          icon: <TeamOutlined />,
          label: '角色管理',
        },
        {
          key: 'permissions',
          icon: <SafetyOutlined />,
          label: '权限管理',
        },
      ],
    },

  ];

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人信息',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ];

  const getBreadcrumbItems = () => {
    const breadcrumbMap: { [key: string]: string[] } = {
      'dashboard': ['仪表盘'],
      'users': ['系统管理', '用户管理'],
      'roles': ['系统管理', '角色管理'],
      'permissions': ['系统管理', '权限管理'],
    };
    
    return breadcrumbMap[selectedKey] || ['仪表盘'];
  };

  const renderContent = () => {
    switch (selectedKey) {
      case 'users':
        return <UserManagement />;
      case 'roles':
        return <RoleManagement />;
      case 'permissions':
        return <PermissionManagement />;
      default:
        return (
          <div style={{ padding: 24 }}>
            <Row gutter={16} style={{ marginBottom: 24 }}>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="总用户数"
                    value={1128}
                    prefix={<UserOutlined />}
                    valueStyle={{ color: '#3f8600' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="总角色数"
                    value={12}
                    prefix={<TeamOutlined />}
                    valueStyle={{ color: '#1890ff' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="总权限数"
                    value={48}
                    prefix={<SafetyOutlined />}
                    valueStyle={{ color: '#722ed1' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="在线用户"
                    value={93}
                    prefix={<UserOutlined />}
                    valueStyle={{ color: '#cf1322' }}
                  />
                </Card>
              </Col>
            </Row>
            
            <Card title="系统概览" style={{ marginBottom: 24 }}>
              <p>欢迎使用权限管理系统！</p>
              <p>这是一个基于 Spring Boot + React + Ant Design 构建的权限管理系统。</p>
              <p>您可以通过左侧菜单进行用户、角色、权限的管理操作。</p>
            </Card>

            <Card title="快速操作">
              <Space>
                <Button type="primary" icon={<UserOutlined />} onClick={() => setSelectedKey('users')}>
                  用户管理
                </Button>
                <Button icon={<TeamOutlined />} onClick={() => setSelectedKey('roles')}>
                  角色管理
                </Button>
                <Button icon={<SafetyOutlined />} onClick={() => setSelectedKey('permissions')}>
                  权限管理
                </Button>
              </Space>
            </Card>
          </div>
        );
    }
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider 
        trigger={null} 
        collapsible 
        collapsed={collapsed} 
        theme="light" 
        width={220}
        style={{ boxShadow: '2px 0 8px 0 rgba(29,35,41,.05)' }}
      >
        {/* 系统Logo和名称 */}
        <div style={{ 
          padding: collapsed ? '22px 0' : '16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: collapsed ? 'center' : 'flex-start',
          backgroundColor: '#ffffff',
          height: 64,
          boxShadow: '0 1px 2px rgba(0, 0, 0, 0.1)',
          margin: '0 0 1px 0'
        }}>
          <div style={{ 
            background: 'linear-gradient(135deg, #1890ff 0%, #722ed1 100%)', 
            width: 36, 
            height: 36, 
            borderRadius: '8px', 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            marginRight: collapsed ? 0 : 12
          }}>
            <SafetyOutlined style={{ color: 'white', fontSize: 20 }} />
          </div>
          {!collapsed && (
            <div>
              <div style={{ 
                color: '#000000', 
                fontSize: 16, 
                fontWeight: 'bold', 
                lineHeight: '20px',
                letterSpacing: '0.5px'
              }}>
                安全权限系统
              </div>
              <div style={{ color: 'rgba(0,0,0,0.65)', fontSize: 12, marginTop: 2 }}>
                Security Admin
              </div>
            </div>
          )}
        </div>

        <Menu
          theme="light"
          mode="inline"
          selectedKeys={[selectedKey]}
          defaultOpenKeys={['system']}
          items={menuItems}
          onClick={({ key }) => setSelectedKey(key)}
          style={{ 
            borderRight: 0, 
            padding: '8px 0',
            fontSize: '14px'
          }}
        />
      </Sider>
      
      <Layout>
        <Header style={{ 
          padding: '0 24px', 
          background: '#fff', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'space-between',
          boxShadow: '0 1px 4px rgba(0,21,41,.08)',
          zIndex: 10,
          position: 'sticky',
          top: 0
        }}>
          <Space>
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              style={{ fontSize: '16px' }}
            />
            <Breadcrumb style={{ marginLeft: 12 }}>
              {getBreadcrumbItems().map((item, index) => (
                <Breadcrumb.Item key={index}>{item}</Breadcrumb.Item>
              ))}
            </Breadcrumb>
          </Space>
          
          <Dropdown
            menu={{ items: userMenuItems }}
            placement="bottomRight"
          >
            <Space style={{ cursor: 'pointer' }}>
              <Avatar 
                style={{ 
                  backgroundColor: '#1890ff',
                  boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)'
                }} 
                icon={<UserOutlined />} 
              />
              <span style={{ fontSize: '14px' }}>{userInfo?.username || '管理员'}</span>
            </Space>
          </Dropdown>
        </Header>
        
        <Content style={{ 
          margin: '16px', 
          minHeight: 280,
          background: '#f0f2f5'
        }}>
          <div style={{ 
            padding: '24px', 
            background: '#fff', 
            borderRadius: '4px',
            minHeight: 'calc(100vh - 140px)',
            boxShadow: '0 1px 4px rgba(0,21,41,.08)'
          }}>
            {renderContent()}
          </div>
        </Content>
      </Layout>
    </Layout>
  );
};

export default Dashboard;