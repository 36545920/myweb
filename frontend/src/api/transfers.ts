import client from './client'

export const transfersApi = {
  send: (fileId: number, toEmail: string, message?: string) =>
    client.post('/transfers', { fileId, toEmail, message }),
  inbox: (page = 1, size = 20) => client.get('/transfers/inbox', { params: { page, size } }),
  sent: (page = 1, size = 20) => client.get('/transfers/sent', { params: { page, size } }),
  remove: (id: number) => client.delete(`/transfers/${id}`),
}
