import { http } from './request'
import type {
  ChangePasswordPayload,
  PageResult,
  UserCreatePayload,
  UserDetail,
  UserListItem,
  UserQuery,
  UserUpdatePayload,
} from './types'

/** 分页查询系统用户：GET /user/list */
export function listUsers(params: UserQuery): Promise<PageResult<UserListItem>> {
  return http.get<PageResult<UserListItem>>('/user/list', { params })
}

/** 用户详情：GET /user/{id} */
export function getUser(id: number): Promise<UserDetail> {
  return http.get<UserDetail>(`/user/${id}`)
}

/** 新增用户：POST /user */
export function createUser(data: UserCreatePayload): Promise<number> {
  return http.post<number>('/user', data)
}

/** 修改用户资料：PUT /user/{id} */
export function updateUser(id: number, data: UserUpdatePayload): Promise<void> {
  return http.put<void>(`/user/${id}`, data)
}

/** 启用/禁用账号：PUT /user/{id}/status?status=0|1 */
export function updateUserStatus(id: number, status: 0 | 1): Promise<void> {
  return http.put<void>(`/user/${id}/status`, undefined, { params: { status } })
}

/** 重置密码：PUT /user/{id}/password */
export function resetPassword(id: number, newPassword: string): Promise<void> {
  return http.put<void>(`/user/${id}/password`, { newPassword })
}

/** 本人修改密码：PUT /user/password（需校验原密码） */
export function changePassword(data: ChangePasswordPayload): Promise<void> {
  return http.put<void>('/user/password', data)
}

/** 删除用户：DELETE /user/{id} */
export function deleteUser(id: number): Promise<void> {
  return http.delete<void>(`/user/${id}`)
}
