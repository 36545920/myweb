import client from './client'

export const filesApi = {
  uploadInit: (data: { originalName: string; fileSize: number }) =>
    client.post('/upload/init', data),
  uploadChunk: (uploadId: string, index: number, chunk: Blob) =>
    client.post(`/upload/${uploadId}/chunk/${index}`, chunk, {
      headers: { 'Content-Type': 'application/octet-stream' }
    }),
  getUploadStatus: (uploadId: string) => client.get(`/upload/${uploadId}/status`),
  completeUpload: (uploadId: string) => client.post(`/upload/${uploadId}/complete`),
  createFile: (data: any) => client.post('/files', data),
  listFiles: (page = 1, size = 20, type?: string, sort = 'date', order = 'desc', keyword?: string) =>
    client.get('/files', { params: { page, size, type, sort, order, keyword } }),
  getFile: (id: number) => client.get(`/files/${id}`),
  deleteFile: (id: number) => client.delete(`/files/${id}`),
  deleteFiles: (ids: number[]) => client.post('/files/batch-delete', { ids }),
  rename: (id: number, title: string) => client.put(`/files/${id}/rename`, { title }),
  listRecent: (size = 10) => client.get('/files/recent', { params: { size } }),
  downloadUrl: (id: number) => `/api/v1/files/${id}/download`,
}
