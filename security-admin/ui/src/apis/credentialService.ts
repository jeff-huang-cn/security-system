import apiClient from './api';
import { PagedDTO, PagedResult, CredentialVO, CredentialCreateResultDTO } from './types';

/**
 * 客户端凭证管理API服务
 */
export const credentialService = {
  /**
   * 分页查询客户端凭证
   */
  paged: async (pagedDTO: PagedDTO): Promise<PagedResult<CredentialVO>> => {
    return apiClient.post('/api/sys-client-credentials/paged', pagedDTO);
  },

  /**
   * 获取所有客户端凭证
   */
  all: async (): Promise<CredentialVO[]> => {
    return apiClient.get('/api/sys-client-credentials/all');
  },

  /**
   * 创建客户端凭证
   */
  create: async (remark: string): Promise<CredentialCreateResultDTO> => {
    return apiClient.post('/api/sys-client-credentials', {
      remark
    });
  },

  /**
   * 下载凭证文件
   * 注意：此方法返回的是文件流，需要在前端处理下载
   */
  download: async (appId: string): Promise<Blob> => {
    return apiClient.get(`/api/sys-client-credentials/download/${appId}`, {
      responseType: 'blob'
    });
  },

  /**
   * 更新客户端凭证状态
   */
  updateStatus: async (appId: string, status: number): Promise<void> => {
    await apiClient.patch(`/api/sys-client-credentials/${appId}/status?status=${status}`);
  },
  
  /**
   * 获取客户端凭证已授权的资源ID列表
   */
  getResourceIds: async (credentialId: number): Promise<number[]> => {
    return apiClient.get(`/api/sys-credential-resource-rel/${credentialId}/resource-ids`);
  },

  /**
   * 为客户端凭证分配API资源权限
   */
  assignResources: async (credentialId: number, resourceIds: number[]): Promise<void> => {
    await apiClient.post(`/api/sys-credential-resource-rel/${credentialId}/assign`, {
      resourceIds
    });
  }
};

export default credentialService; 