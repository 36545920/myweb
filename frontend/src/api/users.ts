import client from './client'

export const usersApi = {
  getProfile: () => client.get('/users/me'),
  updateProfile: (data: { nickname?: string; avatar?: string; oldPassword?: string; newPassword?: string }) =>
    client.put('/users/me', data),
  search: (email: string) => client.get('/users/search', { params: { email } }),
}
