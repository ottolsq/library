import router from './index'
import { areRoutesRegistered, registerDynamicRoutes, resetDynamicRoutes } from './dynamic'
import { useAuthStore } from '@/stores/auth'
import { findFirstMenuPath } from '@/utils/menu'

/**
 * 全局路由守卫：
 * 1. 未登录拦截 → /login?redirect=...
 * 2. 刷新后补加载用户档案与菜单
 * 3. 登录后按菜单注册动态路由
 * 4. 根路径跳第一个可访问菜单
 * 5. 页面级权限兜底 → /403
 */
export function setupRouterGuard(): void {
  router.beforeEach(async (to) => {
    const auth = useAuthStore()
    const isPublic = to.meta.public === true

    if (isPublic) {
      // 已登录用户访问登录页，直接尝试恢复会话并回首页
      if (to.path === '/login' && auth.token) {
        const restored = await auth.ensureSession()
        if (restored) {
          return { path: '/', replace: true }
        }
      }
      return true
    }

    if (!auth.token) {
      if (areRoutesRegistered()) {
        resetDynamicRoutes()
      }
      return {
        path: '/login',
        query: { redirect: to.fullPath },
        replace: true,
      }
    }

    const sessionReady = await auth.ensureSession()
    if (!sessionReady) {
      resetDynamicRoutes()
      return {
        path: '/login',
        query: { redirect: to.fullPath },
        replace: true,
      }
    }

    // 首次进入或账号切换后，先注册动态路由再重新解析目标地址
    if (!areRoutesRegistered()) {
      registerDynamicRoutes(auth.menus)
      return {
        path: to.path,
        query: to.query,
        hash: to.hash,
        replace: true,
      }
    }

    // 根路径跳到第一个可访问菜单
    if (to.path === '/') {
      const firstPath = findFirstMenuPath(auth.menus)
      return firstPath
        ? { path: firstPath, replace: true }
        : { path: '/403', replace: true }
    }

    // 页面级权限兜底
    const permission = typeof to.meta.permission === 'string' ? to.meta.permission : ''
    if (permission && !auth.hasPermission(permission)) {
      return { path: '/403', replace: true }
    }

    return true
  })
}
