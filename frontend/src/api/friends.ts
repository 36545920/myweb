import client from './client'

export const friendsApi = {
  add: (friendEmail: string) => client.post('/friends', { friendEmail }),
  accept: (id: number, accept: boolean) => client.put(`/friends/${id}`, { accept }),
  list: () => client.get('/friends'),
  remove: (email: string) => client.delete(`/friends/${email}`),
}
