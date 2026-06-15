import client from './client'

export const adminApi = {
  listUsers: (page = 1, size = 20) => client.get('/admin/users', { params: { page, size } }),
  updateQuota: (email: string, quota: number) => client.put(`/admin/users/${email}/quota`, { quota }),
  updateStatus: (email: string, status: string) => client.put(`/admin/users/${email}/status`, { status }),
  updateRole: (email: string, role: string) => client.put(`/super-admin/users/${email}/role`, { role }),
  getConfig: () => client.get('/admin/config'),
  updateConfig: (configs: Record<string, string>) => client.put('/admin/config', configs),
  poolList: (page = 1, size = 20) => client.get('/pool', { params: { page, size } }),
  reviewList: (page = 1, size = 20) => client.get('/admin/review', { params: { page, size } }),
  reviewFile: (id: number, approved: boolean, comment: string) =>
    client.put(`/admin/review/${id}`, { approved, comment }),
}
