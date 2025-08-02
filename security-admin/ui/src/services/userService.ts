import { businessApi } from './api';

/**
 * 用户管理服务API
 */
export const userService = {
  /**
   * 获取用户列表（分页）
   * @param page 页码
   * @param size 每页大小
   * @param keyword 搜索关键词
   */
  getUsers: async (page: number = 1, size: number = 10, keyword?: string) => {
    const params = { page, size };
    if (keyword) {
      (params as any).keyword = keyword;
    }
    const response = await businessApi.get('/api/users', { params });
    return response.data;
  },

  /**
   * 获取用户详情
   * @param id 用户ID
   */
  getUserById: async (id: number) => {
    const response = await businessApi.get(`/api/users/${id}`);
    return response.data;
  },

  /**
   * 创建用户
   * @param user 用户信息
   */
  createUser: async (user: any) => {
    const response = await businessApi.post('/api/users', user);
    return response.data;
  },

  /**
   * 更新用户
   * @param id 用户ID
   * @param user 用户信息
   */
  updateUser: async (id: number, user: any) => {
    const response = await businessApi.put(`/api/users/${id}`, user);
    return response.data;
  },

  /**
   * 删除用户
   * @param id 用户ID
   */
  deleteUser: async (id: number) => {
    const response = await businessApi.delete(`/api/users/${id}`);
    return response.data;
  },

  /**
   * 启用/禁用用户
   * @param id 用户ID
   * @param status 状态（1启用，0禁用）
   */
  toggleUserStatus: async (id: number, status: number) => {
    const response = await businessApi.patch(`/api/users/${id}/status`, { status });
    return response.data;
  },

  /**
   * 获取用户角色
   * @param userId 用户ID
   */
  getUserRoles: async (userId: number) => {
    const response = await businessApi.get(`/api/users/${userId}/roles`);
    return response.data;
  },

  /**
   * 分配用户角色
   * @param userId 用户ID
   * @param roleIds 角色ID列表
   */
  assignUserRoles: async (userId: number, roleIds: number[]) => {
    const response = await businessApi.post(`/api/users/${userId}/roles`, { roleIds });
    return response.data;
  }
};