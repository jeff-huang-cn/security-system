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
  TreeSelect
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SafetyOutlined,
  SearchOutlined,
  ReloadOutlined,
  FolderOutlined,
  FileOutlined
} from '@ant-design/icons';
import { permissionService } from '../services';

const { Option } = Select;
const { Search } = Input;
const { TextArea } = Input;

interface Permission {
  permissionId: number;
  permissionName: string;
  permissionCode: string;
  permissionType: string;
  parentId: number;
  path: string;
  icon: string;
  sortOrder: number;
  status: number;
  createTime: string;
  children?: Permission[];
}

const PermissionManagement: React.FC = () => {
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [permissionTree, setPermissionTree] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingPermission, setEditingPermission] = useState<Permission | null>(null);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);

  const [form] = Form.useForm();

  useEffect(() => {
    loadPermissions();
  }, [searchKeyword]);

  const loadPermissions = async () => {
    setLoading(true);
    try {
      const response = await permissionService.getPermissions(1, 1000, searchKeyword);
      const permissionList = response.data || [];
      
      // 构建树形结构
      const tree = buildPermissionTree(permissionList);
      setPermissions(tree);
      
      // 构建TreeSelect数据
      const treeSelectData = buildTreeSelectData(permissionList);
      setPermissionTree(treeSelectData);
      
      // 默认展开所有节点
      const allKeys = permissionList.map((p: Permission) => p.permissionId);
      setExpandedRowKeys(allKeys);
    } catch (error) {
      message.error('加载权限列表失败');
      console.error('加载权限列表失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const buildPermissionTree = (permissions: Permission[]): Permission[] => {
    const map: { [key: number]: Permission } = {};
    const tree: Permission[] = [];

    // 创建映射
    permissions.forEach(permission => {
      map[permission.permissionId] = { ...permission, children: [] };
    });

    // 构建树结构
    permissions.forEach(permission => {
      if (permission.parentId === 0) {
        tree.push(map[permission.permissionId]);
      } else if (map[permission.parentId]) {
        map[permission.parentId].children!.push(map[permission.permissionId]);
      }
    });

    return tree;
  };

  const buildTreeSelectData = (permissions: Permission[]) => {
    const tree: any[] = [];
    const map: { [key: number]: any } = {};

    // 添加根节点
    tree.push({
      title: '根目录',
      value: 0,
      key: 0,
      children: []
    });

    // 创建节点映射
    permissions.forEach(permission => {
      map[permission.permissionId] = {
        title: permission.permissionName,
        value: permission.permissionId,
        key: permission.permissionId,
        children: []
      };
    });

    // 构建树结构
    permissions.forEach(permission => {
      if (permission.parentId === 0) {
        tree[0].children.push(map[permission.permissionId]);
      } else if (map[permission.parentId]) {
        map[permission.parentId].children.push(map[permission.permissionId]);
      }
    });

    return tree;
  };

  const handleCreate = () => {
    setEditingPermission(null);
    form.resetFields();
    setShowModal(true);
  };

  const handleEdit = (permission: Permission) => {
    setEditingPermission(permission);
    form.setFieldsValue({
      permissionName: permission.permissionName,
      permissionCode: permission.permissionCode,
      permissionType: permission.permissionType,
      parentId: permission.parentId,
      path: permission.path,
      icon: permission.icon,
      sortOrder: permission.sortOrder,
      status: permission.status
    });
    setShowModal(true);
  };

  const handleSubmit = async (values: any) => {
    setLoading(true);
    try {
      if (editingPermission) {
        await permissionService.updatePermission(editingPermission.permissionId, values);
        message.success('权限更新成功');
      } else {
        await permissionService.createPermission(values);
        message.success('权限创建成功');
      }
      setShowModal(false);
      loadPermissions();
    } catch (error) {
      message.error(editingPermission ? '权限更新失败' : '权限创建失败');
      console.error('保存权限失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (permissionId: number) => {
    try {
      await permissionService.deletePermission(permissionId);
      message.success('权限删除成功');
      loadPermissions();
    } catch (error) {
      message.error('权限删除失败');
      console.error('删除权限失败:', error);
    }
  };

  const handleToggleStatus = async (permissionId: number, status: number) => {
    try {
      await permissionService.togglePermissionStatus(permissionId, status === 1 ? 0 : 1);
      message.success('权限状态更新成功');
      loadPermissions();
    } catch (error) {
      message.error('权限状态更新失败');
      console.error('更新权限状态失败:', error);
    }
  };

  const handleSearch = (value: string) => {
    setSearchKeyword(value);
  };

  const getPermissionTypeTag = (type: string) => {
    const typeMap: { [key: string]: { color: string; text: string } } = {
      'menu': { color: 'blue', text: '菜单' },
      'button': { color: 'green', text: '按钮' },
      'api': { color: 'orange', text: 'API' }
    };
    const config = typeMap[type] || { color: 'default', text: type };
    return <Tag color={config.color}>{config.text}</Tag>;
  };

  const getPermissionIcon = (type: string) => {
    switch (type) {
      case 'menu':
        return <FolderOutlined style={{ color: '#1890ff' }} />;
      case 'button':
        return <SafetyOutlined style={{ color: '#52c41a' }} />;
      case 'api':
        return <FileOutlined style={{ color: '#fa8c16' }} />;
      default:
        return <SafetyOutlined />;
    }
  };

  const columns = [
    {
      title: '权限名称',
      dataIndex: 'permissionName',
      key: 'permissionName',
      width: 200,
      render: (text: string, record: Permission) => (
        <Space>
          {getPermissionIcon(record.permissionType)}
          {text}
        </Space>
      ),
    },
    {
      title: '权限编码',
      dataIndex: 'permissionCode',
      key: 'permissionCode',
      width: 150,
      render: (text: string) => <Tag color="purple">{text}</Tag>,
    },
    {
      title: '权限类型',
      dataIndex: 'permissionType',
      key: 'permissionType',
      width: 100,
      render: (type: string) => getPermissionTypeTag(type),
    },
    {
      title: '路径',
      dataIndex: 'path',
      key: 'path',
      width: 150,
      ellipsis: true,
    },
    {
      title: '图标',
      dataIndex: 'icon',
      key: 'icon',
      width: 80,
      render: (icon: string) => icon ? <span>{icon}</span> : '-',
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 80,
      sorter: (a: Permission, b: Permission) => a.sortOrder - b.sortOrder,
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
      width: 180,
      render: (_: any, record: Permission) => (
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
            onClick={() => handleToggleStatus(record.permissionId, record.status)}
          >
            {record.status === 1 ? '禁用' : '启用'}
          </Button>
          <Popconfirm
            title="确定要删除这个权限吗？"
            onConfirm={() => handleDelete(record.permissionId)}
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
            <h2 style={{ margin: 0 }}>权限管理</h2>
          </Col>
          <Col>
            <Space>
              <Search
                placeholder="搜索权限名称或编码"
                allowClear
                enterButton={<SearchOutlined />}
                size="middle"
                onSearch={handleSearch}
                style={{ width: 250 }}
              />
              <Button
                type="default"
                icon={<ReloadOutlined />}
                onClick={loadPermissions}
              >
                刷新
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleCreate}
              >
                新增权限
              </Button>
            </Space>
          </Col>
        </Row>

        <Table
          columns={columns}
          dataSource={permissions}
          rowKey="permissionId"
          loading={loading}
          pagination={false}
          expandable={{
            expandedRowKeys,
            onExpandedRowsChange: (keys) => setExpandedRowKeys([...keys]),
            childrenColumnName: 'children',
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* 权限编辑模态框 */}
      <Modal
        title={editingPermission ? '编辑权限' : '新增权限'}
        open={showModal}
        onCancel={() => setShowModal(false)}
        footer={null}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{ status: 1, sortOrder: 0, parentId: 0 }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="权限名称"
                name="permissionName"
                rules={[{ required: true, message: '请输入权限名称' }]}
              >
                <Input placeholder="请输入权限名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="权限编码"
                name="permissionCode"
                rules={[{ required: true, message: '请输入权限编码' }]}
              >
                <Input placeholder="请输入权限编码" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="权限类型"
                name="permissionType"
                rules={[{ required: true, message: '请选择权限类型' }]}
              >
                <Select placeholder="请选择权限类型">
                  <Option value="menu">菜单</Option>
                  <Option value="button">按钮</Option>
                  <Option value="api">API</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="上级权限"
                name="parentId"
              >
                <TreeSelect
                  placeholder="请选择上级权限"
                  treeData={permissionTree}
                  treeDefaultExpandAll
                  allowClear
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            label="路径"
            name="path"
          >
            <Input placeholder="请输入路径" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="图标"
                name="icon"
              >
                <Input placeholder="请输入图标" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="排序"
                name="sortOrder"
              >
                <Input type="number" placeholder="请输入排序号" />
              </Form.Item>
            </Col>
          </Row>

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
    </div>
  );
};

export default PermissionManagement;