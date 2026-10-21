import { http } from './request'
import type { MenuNode } from './types'

/** 当前用户可见菜单树：GET /menu/tree */
export function getMenuTree(): Promise<MenuNode[]> {
  return http.get<MenuNode[]>('/menu/tree')
}
