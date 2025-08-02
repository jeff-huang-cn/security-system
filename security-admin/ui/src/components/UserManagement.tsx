import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  Select,
  Space,
  Tag,
  message,
  Popconfirm,
  Card,
  Row,
  Col,
  Checkbox,
  Divider
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
  SearchOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { userService, roleService } from '../services';

const { Option } = Select;
const { Search } = Input;

interface User {
  userId: number;
  username: string;
  realName: string;
  email: string;
  phone: string;
  status: number;
  createTime: string;
  roles?: Role[];
}

interface Role {
  roleId: number;
  roleName: string;
  roleCode: string;
}

const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [showRoleModal, setShowRoleModal] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [selectedRoles, setSelectedRoles] = useState<number[]>([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total: number, range: number[]) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条记录`,
  });

  const [form] = Form.useForm();
  const [roleForm] = Form.useForm();

  useEffect(() => {
    loadUsers();
    loadRoles();
  }, [pagination.current, pagination.pageSize, searchKeyword]);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const response = await userService.getUsers(pagination.current, pagination.pageSize, searchKeyword);
      setUsers(response.data || []);
      setPagination(prev => ({
        ...prev,
        total: response.total || 0
      }));
    } catch (error) {
      message.error('加载用户列表失败');
      console.error('加载用户列表失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadRoles = async () => {
    try {
      const response = await roleService.getRoles(1, 100);
      setRoles(response.data || []);
    } catch (error) {
      message.error('加载角色列表失败');
      console.error('加载角色列表失败:', error);
    }
  };

  const handleCreate = () => {
    setEditingUser(null);
    form.resetFields();
    setShowModal(true);
  };

  const handleEdit = (user: User) => {
    setEditingUser(user);
    form.setFieldsValue({
      username: user.username,
      realName: user.realName,
      email: user.email,
      phone: user.phone,
      status: user.status
    });
    setShowModal(true);
  };

  const handleSubmit = async (values: any) => {
    setLoading(true);
    try {
      if (editingUser) {
        await userService.updateUser(editingUser.userId, values);
        message.success('用户更新成功');
      } else {
        await userService.createUser(values);
        message.success('用户创建成功');
      }
      setShowModal(false);
      loadUsers();
    } catch (error) {
      message.error(editingUser ? '用户更新失败' : '用户创建失败');
      console.error('保存用户失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (userId: number) => {
    try {
      await userService.deleteUser(userId);
      message.success('用户删除成功');
      loadUsers();
    } catch (error) {
      message.error('用户删除失败');
      console.error('删除用户失败:', error);
    }
  };

  const handleToggleStatus = async (userId: number, status: number) => {
    try {
      await userService.toggleUserStatus(userId, status === 1 ? 0 : 1);
      message.success('用户状态更新成功');
      loadUsers();
    } catch (error) {
      message.error('用户状态更新失败');
      console.error('更新用户状态失败:', error);
    }
  };

  const handleAssignRoles = async (userId: number) => {
    setCurrentUserId(userId);
    try {
      const userRoles = await userService.getUserRoles(userId);
      setSelectedRoles(userRoles.map((role: Role) => role.roleId));
      setShowRoleModal(true);
    } catch (error) {
      message.error('获取用户角色失败');
      console.error('获取用户角色失败:', error);
    }
  };

  const handleSaveRoles = async () => {
    if (currentUserId) {
      try {
        await userService.assignUserRoles(currentUserId, selectedRoles);
        message.success('角色分配成功');
        setShowRoleModal(false);
        loadUsers();
      } catch (error) {
        message.error('角色分配失败');
        console.error('分配角色失败:', error);
      }
    }
  };

  const handleSearch = (value: string) => {
    setSearchKeyword(value);
    setPagination(prev => ({ ...prev, current: 1 }));
  };

  const handleTableChange = (paginationConfig: any) => {
    setPagination(prev => ({
      ...prev,
      current: paginationConfig.current,
      pageSize: paginationConfig.pageSize
    }));
  };

  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      render: (text: string) => (
        <Space>
          <UserOutlined />
          {text}
        </Space>
      ),
    },
    {
      title: '真实姓名',
      dataIndex: 'realName',
      key: 'realName',
      width: 120,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 180,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      width: 130,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 120,
      render: (time: string) => new Date(time).toLocaleDateString(),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: User) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => handleToggleStatus(record.userId, record.status)}
          >
            {record.status === 1 ? '禁用' : '启用'}
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => handleAssignRoles(record.userId)}
          >
            分配角色
          </Button>
          <Popconfirm
            title="确定要删除这个用户吗？"
            onConfirm={() => handleDelete(record.userId)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card>
        <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
          <Col>
            <h2 style={{ margin: 0 }}>用户管理</h2>
          </Col>
          <Col>
            <Space>
              <Search
                placeholder="搜索用户名或姓名"
                allowClear
                enterButton={<SearchOutlined />}
                size="middle"
                onSearch={handleSearch}
                style={{ width: 250 }}
              />
              <Button
                type="default"
                icon={<ReloadOutlined />}
                onClick={loadUsers}
              >
                刷新
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleCreate}
              >
                新增用户
              </Button>
            </Space>
          </Col>
        </Row>

        <Table
          columns={columns}
          dataSource={users}
          rowKey="userId"
          loading={loading}
          pagination={pagination}
          onChange={handleTableChange}
          scroll={{ x: 1000 }}
        />
      </Card>

      {/* 用户编辑模态框 */}
      <Modal
        title={editingUser ? '编辑用户' : '新增用户'}
        open={showModal}
        onCancel={() => setShowModal(false)}
        footer={null}
        width={500}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{ status: 1 }}
        >
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入用户名" />
          </Form.Item>

          {!editingUser && (
            <Form.Item
              label="密码"
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password placeholder="请输入密码" />
            </Form.Item>
          )}

          <Form.Item
            label="真实姓名"
            name="realName"
          >
            <Input placeholder="请输入真实姓名" />
          </Form.Item>

          <Form.Item
            label="邮箱"
            name="email"
            rules={[{ type: 'email', message: '请输入有效的邮箱地址' }]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>

          <Form.Item
            label="手机号"
            name="phone"
          >
            <Input placeholder="请输入手机号" />
          </Form.Item>

          <Form.Item
            label="状态"
            name="status"
          >
            <Select>
              <Option value={1}>启用</Option>
              <Option value={0}>禁用</Option>
            </Select>
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setShowModal(false)}>
                取消
              </Button>
              <Button type="primary" htmlType="submit" loading={loading}>
                保存
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* 角色分配模态框 */}
      <Modal
        title="分配角色"
        open={showRoleModal}
        onOk={handleSaveRoles}
        onCancel={() => setShowRoleModal(false)}
        okText="保存"
        cancelText="取消"
        width={400}
      >
        <div style={{ maxHeight: 300, overflowY: 'auto' }}>
          <Checkbox.Group
            value={selectedRoles}
            onChange={setSelectedRoles}
            style={{ width: '100%' }}
          >
            <Row>
              {roles.map((role) => (
                <Col span={24} key={role.roleId} style={{ marginBottom: 8 }}>
                  <Checkbox value={role.roleId}>
                    {role.roleName}
                  </Checkbox>
                </Col>
              ))}
            </Row>
          </Checkbox.Group>
        </div>
      </Modal>
    </div>
  );
};

export default UserManagement;