import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
} from '@/api/auth'
import { getMenuTree } from '@/api/menu'
import type { CurrentUser, LoginPayload, LoginResult, MenuNode } from '@/api/types'
import { clearToken, getToken, setToken } from '@/utils/token'

/**
 * 前端会话用户模型。
 * 登录接口不返回 userId，登录后通过 /user/current 补全。
 */
export interface SessionUser {
  userId: number | null
  username: string
  nickname: string
  enabled: boolean
  roles: string[]
  permissions: string[]
  superAdmin: boolean
}

function fromLoginResult(result: LoginResult): SessionUser {
  const permissions = result.permissions ?? []
  return {
    userId: null,
    username: result.username,
    nickname: result.nickname || result.username,
    enabled: true,
    roles: result.roles ?? [],
    permissions,
    superAdmin: permissions.includes('*:*:*'),
  }
}

function fromProfile(profile: CurrentUser): SessionUser {
  return {
    userId: profile.userId,
    username: profile.username,
    nickname: profile.username,
    enabled: profile.enabled,
    roles: profile.roles ?? [],
    permissions: profile.permissions ?? [],
    superAdmin: profile.superAdmin,
  }
}

/**
 * 全局认证状态：令牌、用户信息、权限集合、菜单树集中在这里。
 * 页面标题、导航裁剪、路由守卫、退出清理都读取同一份数据。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken() ?? '')
  const user = ref<SessionUser | null>(null)
  const menus = ref<MenuNode[]>([])
  const menusLoaded = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value) && Boolean(user.value))
  const username = computed(() => user.value?.username ?? '')
  const nickname = computed(() => user.value?.nickname ?? '')
  const roles = computed(() => user.value?.roles ?? [])
  const permissions = computed(() => user.value?.permissions ?? [])
  const isSuperAdmin = computed(() => user.value?.superAdmin ?? false)

  /** 按钮/路由级权限判断：超管通配符直接放行 */
  function hasPermission(permission: string): boolean {
    return isSuperAdmin.value || permissions.value.includes(permission)
  }

  function applyLogin(result: LoginResult): void {
    token.value = result.token
    setToken(result.token)
    user.value = fromLoginResult(result)
  }

  /** 刷新页面后补加载用户档案 */
  async function fetchProfile(): Promise<void> {
    user.value = fromProfile(await getCurrentUser())
  }

  /** 登录成功后单独拉取菜单树（登录接口不含菜单） */
  async function fetchMenus(): Promise<void> {
    menus.value = await getMenuTree()
    menusLoaded.value = true
  }

  function clearSession(): void {
    token.value = ''
    user.value = null
    menus.value = []
    menusLoaded.value = false
    clearToken()
  }

  /**
   * 登录：登录接口 → 补档案 → 拉菜单。
   * 登录接口只返回令牌、用户信息与权限，不包含 userId 和菜单。
   */
  async function login(payload: LoginPayload): Promise<void> {
    const result = await loginRequest(payload)
    applyLogin(result)
    try {
      await fetchProfile()
      await fetchMenus()
    } catch (error) {
      // 登录后的档案/菜单拉取失败时不保留半成品会话
      clearSession()
      throw error
    }
  }

  /**
   * 刷新后恢复会话：token 在 localStorage，用户档案与菜单在内存，
   * 因此需要重新拉取。失败时清理本地状态。
   */
  async function ensureSession(): Promise<boolean> {
    if (!token.value) {
      return false
    }
    try {
      if (!user.value) {
        await fetchProfile()
      }
      if (!menusLoaded.value) {
        await fetchMenus()
      }
      return true
    } catch {
      clearSession()
      return false
    }
  }

  /** 退出：通知后端注销 Redis 登录态，再清理本地状态 */
  async function logout(): Promise<void> {
    try {
      await logoutRequest()
    } catch {
      // 令牌可能已过期，退出以本地清理为准
    } finally {
      clearSession()
    }
  }

  return {
    token,
    user,
    menus,
    menusLoaded,
    isLoggedIn,
    username,
    nickname,
    roles,
    permissions,
    isSuperAdmin,
    hasPermission,
    login,
    fetchProfile,
    fetchMenus,
    ensureSession,
    clearSession,
    logout,
  }
})
