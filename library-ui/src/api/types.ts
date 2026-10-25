/**
 * 与后端 DTO 一一对应的类型定义。
 * 字段名必须与后端 JSON 保持一致（后端使用 Jackson 默认序列化）。
 */

/** 统一响应体 {code, message, data} */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页响应体 */
export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  items: T[]
}

/** 分页查询参数 */
export interface PageQuery {
  pageNum?: number
  pageSize?: number
}

/* ========== 认证 ========== */

export interface LoginPayload {
  username: string
  password: string
}

/** 登录响应：登录接口不返回菜单树 */
export interface LoginResult {
  token: string
  expiresIn: number
  username: string
  nickname: string
  roles: string[]
  permissions: string[]
}

export interface CurrentUser {
  userId: number
  username: string
  enabled: boolean
  roles: string[]
  permissions: string[]
  superAdmin: boolean
}

/* ========== 菜单 ========== */

export type MenuType = 'M' | 'C' | 'F'

export interface MenuNode {
  menuId: number
  menuName: string
  menuType: MenuType
  path: string | null
  component: string | null
  icon: string | null
  perms: string | null
  children: MenuNode[]
}

/* ========== 图书 ========== */

export interface Book {
  bookId: number
  isbn: string
  bookName: string
  author: string
  publisher: string | null
  category: string | null
  totalNum: number
  availableNum: number
  isDeleted?: number
}

export interface BookQuery extends PageQuery {
  keyword?: string
  category?: string
}

export interface BookPayload {
  isbn: string
  bookName: string
  author: string
  publisher?: string
  category?: string
  totalNum: number
  availableNum: number
}

/* ========== 读者 ========== */

export interface Reader {
  readerId: number
  userId: number
  readerName: string
  studentId: string
  phone: string | null
  maxBorrowCnt: number
  /** 0 禁用 / 1 正常 */
  status: number
}

export interface ReaderQuery extends PageQuery {
  keyword?: string
}

export interface ReaderPayload {
  userId: number
  readerName: string
  studentId: string
  phone?: string
  maxBorrowCnt: number
}

/* ========== 借阅 ========== */

export interface BorrowRecord {
  borrowId: number
  readerId: number
  readerName: string
  bookId: number
  bookName: string
  isbn: string
  borrowTime: string
  shouldReturnTime: string
  actualReturnTime: string | null
  /** 0 借出 / 1 已归还 / 2 逾期 */
  borrowStatus: number
  /** 逾期天数，未逾期为 0 */
  overdueDays: number
}

export interface BorrowQuery extends PageQuery {
  keyword?: string
  status?: number
}

export interface BorrowPayload {
  readerId: number
  bookId: number
  /** LocalDateTime，格式 YYYY-MM-DDTHH:mm:ss；留空时由服务端生成默认借期 */
  shouldReturnTime?: string
}

/* ========== 系统用户 ========== */

/** 后端 UserStatus 枚举按名称序列化 */
export type UserStatus = 'ENABLED' | 'DISABLED'

export interface UserListItem {
  userId: number
  username: string
  /** 0 管理员 / 1 读者 */
  role: number
  status: UserStatus
  createTime: string
}

export interface UserDetail {
  userId: number
  username: string
  role: number
  status: UserStatus
  createTime: string
}

export interface UserCreatePayload {
  username: string
  password: string
  role: number
  status: UserStatus
}

export interface UserUpdatePayload {
  username?: string
  role?: number
  status?: UserStatus
}

/** 本人修改密码：PUT /user/password */
export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
}
