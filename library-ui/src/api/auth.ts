import { http } from './request'
import type { CurrentUser, LoginPayload, LoginResult, RegisterPayload } from './types'

/** 登录：POST /user/login */
export function login(data: LoginPayload): Promise<LoginResult> {
  return http.post<LoginResult>('/user/login', data)
}

/** 读者自助注册：POST /user/register */
export function register(data: RegisterPayload): Promise<number> {
  return http.post<number>('/user/register', data)
}

/** 当前登录用户：GET /user/current */
export function getCurrentUser(): Promise<CurrentUser> {
  return http.get<CurrentUser>('/user/current')
}

/** 退出登录：POST /user/logout */
export function logout(): Promise<void> {
  return http.post<void>('/user/logout')
}
