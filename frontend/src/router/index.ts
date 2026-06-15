import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  // 公开路由
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
  { path: '/verify-email', name: 'VerifyEmail', component: () => import('@/views/VerifyEmail.vue') },
  // 需认证路由（包裹在 AppLayout 中）
  {
    path: '/',
    component: () => import('@/components/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') },
      { path: 'files', name: 'MyFiles', component: () => import('@/views/MyFiles.vue') },
      { path: 'files/upload', name: 'Upload', component: () => import('@/views/Upload.vue') },
      { path: 'inbox', name: 'Inbox', component: () => import('@/views/Inbox.vue') },
      { path: 'pool', name: 'Pool', component: () => import('@/views/Pool.vue') },
      { path: 'friends', name: 'Friends', component: () => import('@/views/Friends.vue') },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue') },
      { path: 'admin/review', name: 'AdminReview', component: () => import('@/views/admin/Review.vue') },
      { path: 'admin/users', name: 'AdminUsers', component: () => import('@/views/admin/Users.vue') },
      { path: 'super-admin/system', name: 'SuperSystem', component: () => import('@/views/super-admin/System.vue') },
    ]
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
