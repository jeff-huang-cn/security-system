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
  Tree,
  Divider
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  TeamOutlined,
  SearchOutlined,
  ReloadOutlined,
  SettingOutlined
} from '@ant-design/icons';
import { roleService, permissionService } from '../services';

const { Option } = Select;
const { Search } = Input;
const { TextArea } = Input;

interface Role {
  roleId: number;
  roleName: string;
  roleCode: string;
  description: string;
  status: number;
  createTime: string;
  permissions?: Permission[];
}

interface Permission {
  permissionId: number;
  permissionName: string;
  permissionCode: string;
  permissionType: string;
  parentId: number;
  path: string;
  children?: Permission[];
}

const RoleManagement: React.FC = () => {
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [permissionTree, setPermissionTree] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [showPermissionModal, setShowPermissionModal] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [currentRoleId, setCurrentRoleId] = useState<number | null>(null);
  const [selectedPermissions, setSelectedPermissions] = useState<number[]>([]);
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

  useEffect(() => {
    loadRoles();
    loadPermissions();
  }, [pagination.current, pagination.pageSize, searchKeyword]);

  const loadRoles = async () => {
    setLoading(true);
    try {
      const result = await roleService.getRoles(pagination.current, pagination.pageSize, searchKeyword);
      // 处理返回结果，适应新的响应格式
      if (result && typeof result === 'object') {
        // 如果返回的是分页对象
        if ('list' in result && 'total' in result) {
          setRoles(Array.isArray(result.list) ? result.list : []);
          setPagination(prev => ({
            ...prev,
            total: typeof result.total === 'number' ? result.total : 0
          }));
        } 
        // 如果返回的是数组
        else if (Array.isArray(result)) {
          setRoles(result);
          setPagination(prev => ({
            ...prev,
            total: result.length
          }));
        } else {
          setRoles([]);
          setPagination(prev => ({
            ...prev,
            total: 0
          }));
        }
      } else {
        setRoles([]);
        setPagination(prev => ({
          ...prev,
          total: 0
        }));
      }
    } catch (error) {
      message.error('加载角色列表失败');
      console.error('加载角色列表失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadPermissions = async () => {
    try {
      // 使用新的 getAllPermissions 方法获取所有权限
      const permissionList = await permissionService.getAllPermissions();
      // 确保结果是数组
      const permissions: Permission[] = Array.isArray(permissionList) ? permissionList : [];
      setPermissions(permissions);
      
      // 构建权限树
      const tree = buildPermissionTree(permissions);
      setPermissionTree(tree);
    } catch (error) {
      message.error('加载权限列表失败');
      console.error('加载权限列表失败:', error);
    }
  };

  const buildPermissionTree = (permissions: Permission[]) => {
    const tree: any[] = [];
    const map: { [key: number]: any } = {};

    // 创建节点映射
    permissions.forEach(permission => {
      map[permission.permissionId] = {
        key: permission.permissionId,
        title: permission.permissionName,
        value: permission.permissionId,
        children: []
      };
    });

    // 构建树结构
    permissions.forEach(permission => {
      if (permission.parentId === 0) {
        tree.push(map[permission.permissionId]);
      } else if (map[permission.parentId]) {
        map[permission.parentId].children.push(map[permission.permissionId]);
      }
    });

    return tree;
  };

  const handleCreate = () => {
    setEditingRole(null);
    form.resetFields();
    setShowModal(true);
  };

  const handleEdit = (role: Role) => {
    setEditingRole(role);
    form.setFieldsValue({
      roleName: role.roleName,
      roleCode: role.roleCode,
      description: role.description,
      status: role.status
    });
    setShowModal(true);
  };

  const handleSubmit = async (values: any) => {
    setLoading(true);
    try {
      if (editingRole) {
        await roleService.updateRole(editingRole.roleId, values);
        message.success('角色更新成功');
      } else {
        await roleService.createRole(values);
        message.success('角色创建成功');
      }
      setShowModal(false);
      loadRoles();
    } catch (error) {
      message.error(editingRole ? '角色更新失败' : '角色创建失败');
      console.error('保存角色失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (roleId: number) => {
    try {
      // 检查角色是否被使用
      const isUsed = await roleService.checkRoleUsage(roleId);
      if (isUsed) {
        message.warning('该角色正在被使用，无法删除');
        return;
      }
      
      await roleService.deleteRole(roleId);
      message.success('角色删除成功');
      loadRoles();
    } catch (error) {
      message.error('角色删除失败');
      console.error('删除角色失败:', error);
    }
  };

  const handleToggleStatus = async (roleId: number, status: number) => {
    try {
      await roleService.toggleRoleStatus(roleId, status === 1 ? 0 : 1);
      message.success('角色状态更新成功');
      loadRoles();
    } catch (error) {
      message.error('角色状态更新失败');
      console.error('更新角色状态失败:', error);
    }
  };

  const handleAssignPermissions = async (roleId: number) => {
    setCurrentRoleId(roleId);
    try {
      const rolePermissions = await roleService.getRolePermissions(roleId);
      // 确保返回的是数组
      const permissionsList: Permission[] = Array.isArray(rolePermissions) ? rolePermissions : [];
      setSelectedPermissions(permissionsList.map(permission => permission.permissionId));
      setShowPermissionModal(true);
    } catch (error) {
      message.error('获取角色权限失败');
      console.error('获取角色权限失败:', error);
    }
  };

  const handleSavePermissions = async () => {
    if (currentRoleId) {
      try {
        await roleService.assignRolePermissions(currentRoleId, selectedPermissions);
        message.success('权限分配成功');
        setShowPermissionModal(false);
        loadRoles();
      } catch (error) {
        message.error('权限分配失败');
        console.error('分配权限失败:', error);
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
      title: '角色名称',
      dataIndex: 'roleName',
      key: 'roleName',
      width: 150,
      render: (text: string) => (
        <Space>
          <TeamOutlined />
          {text}
        </Space>
      ),
    },
    {
      title: '角色编码',
      dataIndex: 'roleCode',
      key: 'roleCode',
      width: 120,
      render: (text: string) => <Tag color="blue">{text}</Tag>,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 200,
      ellipsis: true,
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
      width: 220,
      render: (_: any, record: Role) => (
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
            onClick={() => handleToggleStatus(record.roleId, record.status)}
          >
            {record.status === 1 ? '禁用' : '启用'}
          </Button>
          <Button
            type="link"
            size="small"
            icon={<SettingOutlined />}
            onClick={() => handleAssignPermissions(record.roleId)}
          >
            分配权限
          </Button>
          <Popconfirm
            title="确定要删除这个角色吗？"
            onConfirm={() => handleDelete(record.roleId)}
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
            <h2 style={{ margin: 0 }}>角色管理</h2>
          </Col>
          <Col>
            <Space>
              <Search
                placeholder="搜索角色名称或编码"
                allowClear
                enterButton={<SearchOutlined />}
                size="middle"
                onSearch={handleSearch}
                style={{ width: 250 }}
              />
              <Button
                type="default"
                icon={<ReloadOutlined />}
                onClick={loadRoles}
              >
                刷新
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleCreate}
              >
                新增角色
              </Button>
            </Space>
          </Col>
        </Row>

        <Table
          columns={columns}
          dataSource={roles}
          rowKey="roleId"
          loading={loading}
          pagination={pagination}
          onChange={handleTableChange}
          scroll={{ x: 1000 }}
        />
      </Card>

      {/* 角色编辑模态框 */}
      <Modal
        title={editingRole ? '编辑角色' : '新增角色'}
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
            label="角色名称"
            name="roleName"
            rules={[{ required: true, message: '请输入角色名称' }]}
          >
            <Input placeholder="请输入角色名称" />
          </Form.Item>

          <Form.Item
            label="角色编码"
            name="roleCode"
            rules={[{ required: true, message: '请输入角色编码' }]}
          >
            <Input placeholder="请输入角色编码" />
          </Form.Item>

          <Form.Item
            label="描述"
            name="description"
          >
            <TextArea rows={3} placeholder="请输入角色描述" />
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

      {/* 权限分配模态框 */}
      <Modal
        title="分配权限"
        open={showPermissionModal}
        onOk={handleSavePermissions}
        onCancel={() => setShowPermissionModal(false)}
        okText="保存"
        cancelText="取消"
        width={500}
      >
        <div style={{ maxHeight: 400, overflowY: 'auto' }}>
          <Tree
            checkable
            checkedKeys={selectedPermissions}
            onCheck={(checkedKeys: any) => setSelectedPermissions(checkedKeys)}
            treeData={permissionTree}
            defaultExpandAll
          />
        </div>
      </Modal>
    </div>
  );
};

export default RoleManagement;