/** 角色：0 管理员 / 1 读者 */
export const ROLE_OPTIONS = [
  { value: 0, label: '管理员' },
  { value: 1, label: '读者' },
] as const

export function roleLabel(role: number): string {
  return role === 0 ? '管理员' : '读者'
}

/** 用户状态：后端枚举按名称序列化 */
export const USER_STATUS_OPTIONS = [
  { value: 'ENABLED', label: '正常' },
  { value: 'DISABLED', label: '禁用' },
] as const

export function userStatusLabel(status: string): string {
  return status === 'ENABLED' ? '正常' : '禁用'
}

/** 读者状态：0 禁用 / 1 正常 */
export function readerStatusLabel(status: number): string {
  return status === 1 ? '正常' : '禁用'
}

/** 借阅状态：0 借出 / 1 已归还 / 2 逾期 */
export const BORROW_STATUS_OPTIONS = [
  { value: 0, label: '借出' },
  { value: 1, label: '已归还' },
  { value: 2, label: '逾期' },
] as const

export function borrowStatusLabel(status: number): string {
  if (status === 0) {
    return '借出'
  }
  if (status === 1) {
    return '已归还'
  }
  return '逾期'
}

export function borrowStatusTagType(status: number): 'primary' | 'success' | 'danger' {
  if (status === 1) {
    return 'success'
  }
  return status === 2 ? 'danger' : 'primary'
}
