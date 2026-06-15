import client from './client'

export const authApi = {
  sendCode: (email: string) => client.post('/auth/send-code', { email }),
  register: (data: { email: string; password: string; nickname: string }, code: string) =>
    client.post(`/auth/register?code=${code}`, data),
  login: (data: { email: string; password: string }) => client.post('/auth/login', data),
  refresh: (refreshToken: string) => client.post('/auth/refresh', { refreshToken }),
}
