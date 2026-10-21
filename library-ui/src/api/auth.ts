import { http } from './request'
import type { CurrentUser, LoginPayload, LoginResult } from './types'

/** 登录：POST /user/login */
export function login(data: LoginPayload): Promise<LoginResult> {
  return http.post<LoginResult>('/user/login', data)
}

/** 当前登录用户：GET /user/current */
export function getCurrentUser(): Promise<CurrentUser> {
  return http.get<CurrentUser>('/user/current')
}

/** 退出登录：POST /user/logout */
export function logout(): Promise<void> {
  return http.post<void>('/user/logout')
}
