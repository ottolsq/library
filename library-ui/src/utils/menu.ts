import type { MenuNode } from '@/api/types'

function normalize(segment: string | null | undefined): string {
  return (segment ?? '').replace(/^\/+|\/+$/g, '')
}

/** 拼接菜单路径：父级为空时直接返回 /子路径 */
export function joinMenuPath(base: string, path: string | null | undefined): string {
  const baseSegment = normalize(base)
  const pathSegment = normalize(path)
  if (!pathSegment) {
    return baseSegment ? `/${baseSegment}` : ''
  }
  const joined = baseSegment ? `${baseSegment}/${pathSegment}` : pathSegment
  return `/${joined}`
}

/**
 * 生成导航用的菜单树：过滤 F 按钮节点，并去掉没有可见子菜单的空目录。
 */
export function filterMenuTree(nodes: MenuNode[]): MenuNode[] {
  const result: MenuNode[] = []
  for (const node of nodes) {
    if (node.menuType === 'F') {
      continue
    }
    const children = filterMenuTree(node.children ?? [])
    if (node.menuType === 'M' && children.length === 0) {
      continue
    }
    result.push({ ...node, children })
  }
  return result
}

/** 找到第一个可访问的 C 菜单路径，用于根路径跳转 */
export function findFirstMenuPath(nodes: MenuNode[], base = ''): string {
  for (const node of nodes) {
    if (node.menuType === 'F') {
      continue
    }
    const fullPath = joinMenuPath(base, node.path)
    if (node.menuType === 'C' && fullPath) {
      return fullPath
    }
    const childPath = findFirstMenuPath(node.children ?? [], fullPath)
    if (childPath) {
      return childPath
    }
  }
  return ''
}

/** 找到当前路由对应的菜单链（目录 → 菜单），用于面包屑 */
export function findMenuTrail(
  nodes: MenuNode[],
  routePath: string,
  base = '',
): MenuNode[] {
  for (const node of nodes) {
    if (node.menuType === 'F') {
      continue
    }
    const fullPath = joinMenuPath(base, node.path)
    if (node.menuType === 'C' && fullPath === routePath) {
      return [node]
    }
    const childTrail = findMenuTrail(node.children ?? [], routePath, fullPath)
    if (childTrail.length > 0) {
      return [node, ...childTrail]
    }
  }
  return []
}
