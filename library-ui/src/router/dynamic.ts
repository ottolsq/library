import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import router from './index'
import type { MenuNode } from '@/api/types'
import { joinMenuPath } from '@/utils/menu'

/**
 * 构建期扫描 views 目录，菜单里的 component 路径直接映射到对应 SFC。
 * 例如 menu.component = "book/index" → ../views/book/index.vue
 */
const viewModules = import.meta.glob<Component>('../views/**/*.vue')

const dynamicRouteNames: string[] = []
let registered = false

function resolveComponent(component: string | null): (() => Promise<Component>) | null {
  if (!component) {
    return null
  }
  return viewModules[`../views/${component}.vue`] ?? null
}

function buildRoutes(nodes: MenuNode[], basePath = ''): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []

  for (const node of nodes) {
    if (node.menuType === 'F') {
      continue
    }

    const fullPath = joinMenuPath(basePath, node.path)

    if (node.menuType === 'C' && node.component) {
      const component = resolveComponent(node.component)
      if (component) {
        routes.push({
          // Main 的子路由使用相对路径，最终 URL 仍为 /xxx
          path: fullPath.replace(/^\//, ''),
          name: `menu-${node.menuId}`,
          component,
          meta: {
            title: node.menuName,
            permission: node.perms ?? '',
            icon: node.icon ?? '',
          },
        })
      }
    }

    if (node.children?.length) {
      routes.push(...buildRoutes(node.children, fullPath))
    }
  }

  return routes
}

/** 按当前用户菜单注册动态路由（先回收旧路由，避免切换账号后残留） */
export function registerDynamicRoutes(menus: MenuNode[]): void {
  resetDynamicRoutes()
  for (const route of buildRoutes(menus)) {
    router.addRoute('Main', route)
    dynamicRouteNames.push(String(route.name))
  }
  registered = true
}

/** 退出登录时回收动态路由 */
export function resetDynamicRoutes(): void {
  for (const name of dynamicRouteNames.splice(0)) {
    if (router.hasRoute(name)) {
      router.removeRoute(name)
    }
  }
  registered = false
}

/** 是否已经按菜单完成过注册（菜单为空也算已注册，避免守卫死循环） */
export function areRoutesRegistered(): boolean {
  return registered
}
