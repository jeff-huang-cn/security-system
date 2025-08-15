import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, message, Spin, Tabs, Result } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { tokenStoreManager } from '../../stores/tokenStoreManager';
import axios from 'axios';

// API基础URL
const API_BASE_URL = process.env.REACT_APP_API_URL || '';

const WechatCallback: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [bindLoading, setBindLoading] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [encryptedOpenId, setEncryptedOpenId] = useState('');
  const [nickname, setNickname] = useState('');
  const [headimgurl, setHeadimgurl] = useState('');
  const [step, setStep] = useState<'processing' | 'choice' | 'success' | 'error'>('processing');
  const [errorMessage, setErrorMessage] = useState('');

  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const queryParams = new URLSearchParams(location.search);
    const code = queryParams.get('code');
    const state = queryParams.get('state');

    if (!code || !state) {
      setStep('error');
      setErrorMessage('缺少必要的参数');
      setLoading(false);
      return;
    }

    // 处理微信授权回调
    axios.get(`${API_BASE_URL}/oauth2/wechat/callback`, {
      params: { code, state }
    })
      .then(response => {
        const data = response.data;
        
        if (data.access_token) {
          // 已关联账号，直接登录成功
          tokenStoreManager.setToken(data.access_token);
          setStep('success');
          setTimeout(() => {
            navigate('/dashboard');
          }, 1500);
        } else if (data.encryptedOpenId) {
          // 未关联账号，需要选择绑定已有账号或创建新账号
          setEncryptedOpenId(data.encryptedOpenId);
          setNickname(data.nickname || '微信用户');
          setHeadimgurl(data.headimgurl || '');
          setStep('choice');
        } else {
          throw new Error('未知的响应格式');
        }
      })
      .catch(error => {
        console.error('微信回调处理失败', error);
        setStep('error');
        setErrorMessage(error.response?.data?.error || '微信登录失败，请重试');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [location, navigate]);

  // 绑定已有账号
  const handleBindExistingAccount = (values: { username: string; password: string }) => {
    setBindLoading(true);
    
    const formData = new URLSearchParams();
    formData.append('username', values.username);
    formData.append('password', values.password);
    formData.append('encryptedOpenId', encryptedOpenId);

    axios.post(`${API_BASE_URL}/oauth2/wechat/bind`, formData.toString(), {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    })
      .then(response => {
        const data = response.data;
        if (data.access_token) {
          tokenStoreManager.setToken(data.access_token);
          setStep('success');
          setTimeout(() => {
            navigate('/dashboard');
          }, 1500);
        } else {
          throw new Error('未知的响应格式');
        }
      })
      .catch(error => {
        console.error('绑定账号失败', error);
        message.error(error.response?.data?.error || '绑定账号失败，请重试');
      })
      .finally(() => {
        setBindLoading(false);
      });
  };

  // 创建新账号
  const handleCreateNewAccount = () => {
    setCreateLoading(true);
    
    const formData = new URLSearchParams();
    formData.append('encryptedOpenId', encryptedOpenId);
    formData.append('nickname', nickname);
    if (headimgurl) {
      formData.append('headimgurl', headimgurl);
    }

    axios.post(`${API_BASE_URL}/oauth2/wechat/create`, formData.toString(), {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    })
      .then(response => {
        const data = response.data;
        if (data.access_token) {
          tokenStoreManager.setToken(data.access_token);
          setStep('success');
          setTimeout(() => {
            navigate('/dashboard');
          }, 1500);
        } else {
          throw new Error('未知的响应格式');
        }
      })
      .catch(error => {
        console.error('创建账号失败', error);
        message.error(error.response?.data?.error || '创建账号失败，请重试');
      })
      .finally(() => {
        setCreateLoading(false);
      });
  };

  // 渲染加载中状态
  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" tip="处理微信登录中..." />
      </div>
    );
  }

  // 渲染错误状态
  if (step === 'error') {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Result
          status="error"
          title="微信登录失败"
          subTitle={errorMessage}
          extra={[
            <Button type="primary" key="back" onClick={() => navigate('/login')}>
              返回登录
            </Button>
          ]}
        />
      </div>
    );
  }

  // 渲染成功状态
  if (step === 'success') {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Result
          status="success"
          title="微信登录成功"
          subTitle="正在跳转到系统首页..."
        />
      </div>
    );
  }

  // 渲染选择状态
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
      <Card title="微信登录" style={{ width: 400 }}>
        <div style={{ textAlign: 'center', marginBottom: 20 }}>
          <img 
            src={headimgurl || '/default-avatar.png'} 
            alt="微信头像" 
            style={{ width: 64, height: 64, borderRadius: '50%' }} 
          />
          <h3 style={{ marginTop: 10 }}>欢迎，{nickname}</h3>
          <p>请选择登录方式</p>
        </div>

        <Tabs defaultActiveKey="bind">
          <Tabs.TabPane tab="绑定已有账号" key="bind">
            <Form onFinish={handleBindExistingAccount}>
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
                <Button type="primary" htmlType="submit" block loading={bindLoading}>
                  绑定并登录
                </Button>
              </Form.Item>
            </Form>
          </Tabs.TabPane>
          
          <Tabs.TabPane tab="创建新账号" key="create">
            <p>将使用您的微信信息创建新账号</p>
            <Button 
              type="primary" 
              block 
              onClick={handleCreateNewAccount} 
              loading={createLoading}
            >
              创建新账号并登录
            </Button>
          </Tabs.TabPane>
        </Tabs>
      </Card>
    </div>
  );
};

export default WechatCallback; 