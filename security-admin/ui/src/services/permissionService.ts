import { businessApi } from './api';

/**
 * 权限管理服务API
 */
export const permissionService = {
  /**
   * 获取权限列表（分页）
   * @param page 页码
   * @param size 每页大小
   * @param keyword 搜索关键词
   */
  getPermissions: async (page: number = 1, size: number = 10, keyword?: string) => {
    const params = { page, size };
    if (keyword) {
      (params as any).keyword = keyword;
    }
    const response = await businessApi.get('/api/permissions', { params });
    return response.data;
  },

  /**
   * 获取权限详情
   * @param id 权限ID
   */
  getPermissionById: async (id: number) => {
    const response = await businessApi.get(`/api/permissions/${id}`);
    return response.data;
  },

  /**
   * 创建权限
   * @param permission 权限信息
   */
  createPermission: async (permission: any) => {
    const response = await businessApi.post('/api/permissions', permission);
    return response.data;
  },

  /**
   * 更新权限
   * @param id 权限ID
   * @param permission 权限信息
   */
  updatePermission: async (id: number, permission: any) => {
    const response = await businessApi.put(`/api/permissions/${id}`, permission);
    return response.data;
  },

  /**
   * 删除权限
   * @param id 权限ID
   */
  deletePermission: async (id: number) => {
    const response = await businessApi.delete(`/api/permissions/${id}`);
    return response.data;
  },

  /**
   * 启用/禁用权限
   * @param id 权限ID
   * @param status 状态（1启用，0禁用）
   */
  togglePermissionStatus: async (id: number, status: number) => {
    const response = await businessApi.patch(`/api/permissions/${id}/status`, { status });
    return response.data;
  },

  /**
   * 获取权限树
   */
  getPermissionTree: async () => {
    const response = await businessApi.get('/api/permissions/tree');
    return response.data;
  }
};