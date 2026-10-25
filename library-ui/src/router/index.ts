import { createRouter, createWebHistory } from 'vue-router'

/**
 * 静态路由：登录页、403、主布局壳、404。
 * 业务页面在登录后根据后端菜单树动态注册（见 router/dynamic.ts）。
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录', public: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/register/index.vue'),
      meta: { title: '注册', public: true },
    },
    {
      path: '/403',
      name: 'Forbidden',
      component: () => import('@/views/error/403.vue'),
      meta: { title: '无访问权限', public: true },
    },
    {
      path: '/',
      name: 'Main',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/error/404.vue'),
      meta: { title: '页面不存在' },
    },
  ],
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : ''
  document.title = title ? `${title} · 藏書閣` : '藏書閣 · 图书管理系统'
})

export default router
