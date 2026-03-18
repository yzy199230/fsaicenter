import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/index.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '仪表盘' }
        },
        {
          path: 'system/users',
          name: 'system-users',
          component: () => import('@/views/system/users/index.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'system/roles',
          name: 'system-roles',
          component: () => import('@/views/system/roles/index.vue'),
          meta: { title: '角色管理' }
        },
        {
          path: 'apikey/list',
          name: 'apikey-list',
          component: () => import('@/views/apikey/list/index.vue'),
          meta: { title: 'API Key 管理' }
        },
        {
          path: 'model/list',
          name: 'model-list',
          component: () => import('@/views/model/list/index.vue'),
          meta: { title: '模型管理' }
        },
        {
          path: 'model/providers',
          name: 'model-providers',
          component: () => import('@/views/model/providers/index.vue'),
          meta: { title: '提供商管理' }
        },
        {
          path: 'billing/records',
          name: 'billing-records',
          component: () => import('@/views/billing/records/index.vue'),
          meta: { title: '计费记录' }
        },
        {
          path: 'logs/requests',
          name: 'logs-requests',
          component: () => import('@/views/logs/requests/index.vue'),
          meta: { title: '请求日志' }
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth !== false && !userStore.token) {
    next('/login')
  } else if (to.path === '/login' && userStore.token) {
    next('/')
  } else {
    next()
  }
})

export default router
