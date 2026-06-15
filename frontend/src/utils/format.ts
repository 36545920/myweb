export function formatFileSize(bytes: number): string {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0) + ' ' + units[i]
}

export function formatDate(date: string | null): string {
  if (!date) return '永久'
  return new Date(date).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

export function maskEmail(email: string, isFriend: boolean): string {
  if (isFriend) return email
  const [name, domain] = email.split('@')
  if (!domain) return email
  return name.substring(0, Math.min(3, name.length)) + '***@' + domain
}
