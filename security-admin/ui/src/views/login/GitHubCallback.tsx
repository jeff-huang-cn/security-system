import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Tabs, message, Spin, Typography, Avatar } from 'antd';
import { UserOutlined, LockOutlined, GithubOutlined } from '@ant-design/icons';
import axios from 'axios';
import ErrorPage from './ErrorPage';

const { Title, Text } = Typography;
const { TabPane } = Tabs;

// API基础URL
const API_BASE_URL = process.env.REACT_APP_AUTH_BASE_URL || 'http://localhost:9000';

interface GitHubCallbackProps {
  onLogin: (userData: any, token: string) => void;
}

const GitHubCallback: React.FC<GitHubCallbackProps> = ({ onLogin }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [githubData, setGithubData] = useState<any>(null);
  const [activeTab, setActiveTab] = useState('bind');
  const [bindForm] = Form.useForm();
  const [registerForm] = Form.useForm();

  useEffect(() => {
    // 解析URL中的参数
    const params = new URLSearchParams(location.search);
    const code = params.get('code');
    const state = params.get('state');
    const errorMsg = params.get('error');
    
    if (errorMsg) {
      setError(decodeURIComponent(errorMsg));
      setLoading(false);
      return;
    }
    
    if (!code || !state) {
      setError('缺少必要的参数');
      setLoading(false);
      return;
    }

    // 获取GitHub用户数据
    fetchGitHubData(code, state);
  }, [location.search]);

  const fetchGitHubData = async (code: string, state: string) => {
    try {
      // 调用后端接口获取GitHub用户数据
      const response = await axios.get(`${API_BASE_URL}/auth/github/callback?code=${code}&state=${state}`);
      
      // 检查是否是令牌响应（已绑定账号）
      if (response.data && response.data.access_token) {
        // 已绑定账号，直接登录
        handleLoginSuccess(response.data.access_token);
      } 
      // 检查是否是ChoiceResponse格式（未绑定账号）
      else if (response.data && response.data.encryptedOpenId) {
        // 未绑定账号，显示绑定界面
        setGithubData({
          encryptedGithubId: response.data.encryptedOpenId,
          login: response.data.nickname,
          avatarUrl: response.data.headimgurl
        });
        setLoading(false);
      } else {
        setError('获取GitHub用户数据失败');
        setLoading(false);
      }
    } catch (error: any) {
      console.error('获取GitHub用户数据失败:', error);
      setError(error.response?.data?.error || '获取GitHub用户数据失败');
      setLoading(false);
    }
  };

  const handleLoginSuccess = (token: string) => {
    // 存储token
    localStorage.setItem('token', token);
    
    // 调用登录回调
    onLogin({ username: 'github_user' }, token);
    
    // 跳转到首页
    navigate('/dashboard');
    message.success('登录成功');
  };

  const handleBindAccount = async (values: any) => {
    try {
      setLoading(true);
      // 调用绑定已有账号接口
      const params = new URLSearchParams();
      params.append('githubId', githubData.githubId);
      params.append('username', values.username);
      params.append('password', values.password);
      
      const response = await axios.post(`${API_BASE_URL}/auth/github/bind`, params);
      
      if (response.data && response.data.success) {
        message.success('账号绑定成功');
        handleLoginSuccess(response.data.token.access_token);
      } else {
        message.error(response.data.message || '账号绑定失败');
      }
    } catch (error: any) {
      console.error('绑定账号失败:', error);
      message.error(error.response?.data?.message || '绑定账号失败');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateAccount = async (values: any) => {
    try {
      setLoading(true);
      // 调用创建新账号接口
      const params = new URLSearchParams();
      params.append('githubId', githubData.githubId);
      params.append('username', values.username);
      params.append('password', values.password);
      
      const response = await axios.post(`${API_BASE_URL}/auth/github/create`, params);
      
      if (response.data && response.data.success) {
        message.success('账号创建成功');
        handleLoginSuccess(response.data.token.access_token);
      } else {
        message.error(response.data.message || '账号创建失败');
      }
    } catch (error: any) {
      console.error('创建账号失败:', error);
      message.error(error.response?.data?.message || '创建账号失败');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh',
        background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
      }}>
        <Spin size="large" tip="正在处理GitHub登录..." />
      </div>
    );
  }

  if (error) {
    return <ErrorPage title="GitHub登录失败" error={error} />;
  }

  if (!githubData) {
    return null;
  }

  const githubUser = githubData.githubInfo || {};

  return (
    <div style={{
      minHeight: "100vh",
      background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      padding: "20px"
    }}>
      <Card
        style={{
          width: 450,
          boxShadow: "0 4px 12px rgba(0, 0, 0, 0.15)",
          borderRadius: 8
        }}
      >
        <div style={{ textAlign: "center", marginBottom: 24 }}>
          <Avatar 
            size={64} 
            src={githubUser.avatarUrl} 
            icon={<GithubOutlined />}
            style={{ marginBottom: 16 }}
          />
          <Title level={3} style={{ margin: 0 }}>
            GitHub账号关联
          </Title>
          <Text type="secondary">
            欢迎 {githubUser.name || githubUser.login}，请选择关联方式
          </Text>
        </div>

        <Tabs activeKey={activeTab} onChange={setActiveTab} centered>
          <TabPane tab="绑定已有账号" key="bind">
            <Form
              form={bindForm}
              name="bind_account"
              onFinish={handleBindAccount}
              layout="vertical"
            >
              <Form.Item
                name="username"
                rules={[{ required: true, message: '请输入用户名' }]}
              >
                <Input prefix={<UserOutlined />} placeholder="用户名" />
              </Form.Item>

              <Form.Item
                name="password"
                rules={[{ required: true, message: '请输入密码' }]}
              >
                <Input.Password prefix={<LockOutlined />} placeholder="密码" />
              </Form.Item>

              <Form.Item>
                <Button type="primary" htmlType="submit" block loading={loading}>
                  绑定账号
                </Button>
              </Form.Item>
            </Form>
          </TabPane>

          <TabPane tab="创建新账号" key="register">
            <Form
              form={registerForm}
              name="register_account"
              onFinish={handleCreateAccount}
              layout="vertical"
            >
              <Form.Item
                name="username"
                rules={[
                  { required: true, message: '请输入用户名' },
                  { min: 3, message: '用户名至少3个字符' }
                ]}
              >
                <Input prefix={<UserOutlined />} placeholder="设置用户名" />
              </Form.Item>

              <Form.Item
                name="password"
                rules={[
                  { required: true, message: '请输入密码' },
                  { min: 6, message: '密码至少6个字符' }
                ]}
              >
                <Input.Password prefix={<LockOutlined />} placeholder="设置密码" />
              </Form.Item>

              <Form.Item
                name="confirmPassword"
                dependencies={['password']}
                rules={[
                  { required: true, message: '请确认密码' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue('password') === value) {
                        return Promise.resolve();
                      }
                      return Promise.reject(new Error('两次输入的密码不一致'));
                    },
                  }),
                ]}
              >
                <Input.Password prefix={<LockOutlined />} placeholder="确认密码" />
              </Form.Item>

              <Form.Item>
                <Button type="primary" htmlType="submit" block loading={loading}>
                  创建并关联账号
                </Button>
              </Form.Item>
            </Form>
          </TabPane>
        </Tabs>

        <div style={{ textAlign: 'center', marginTop: 16 }}>
          <Button type="link" onClick={() => navigate('/login')}>
            返回登录页
          </Button>
        </div>
      </Card>
    </div>
  );
};

export default GitHubCallback; 