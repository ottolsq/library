import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { clearToken, getToken } from '@/utils/token'
import type { ApiResult } from './types'

/** 后端约定：200 表示业务成功 */
const SUCCESS_CODE = 200
const UNAUTHORIZED_CODE = 401
const FORBIDDEN_CODE = 403

/** 业务错误：保留后端 code，便于调用方按需处理 */
export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const service = axios.create({
  // 开发环境由 Vite 代理把 /api 转发到 8080；生产环境由反向代理或后端 CORS 接管。
  baseURL: '/api',
  timeout: 15_000,
})

/**
 * 请求拦截器：自动携带 Bearer Token。
 * 注意 `Bearer` 与令牌之间必须有且只有一个空格。
 */
service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 401：清除本地令牌并回到登录页，同时记住当前地址供登录后回跳。
 * 已经位于登录页时只提示，不再跳转（避免登录失败时刷新页面）。
 */
function handleUnauthorized(message?: string): void {
  clearToken()
  if (window.location.pathname !== '/login') {
    ElMessage.error(message || '登录已过期，请重新登录')
    const redirect = encodeURIComponent(
      `${window.location.pathname}${window.location.search}`,
    )
    window.location.replace(`/login?redirect=${redirect}`)
    return
  }
  ElMessage.error(message || '账号或密码错误')
}

/**
 * 响应拦截器：
 * - 业务成功：原样返回，由 request() 统一解包 data
 * - 业务失败：按 401 / 403 / 其他错误分流提示
 * - HTTP 失败：401 清登录态，403 提示无权限，其余给出可读文案
 */
service.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown> | null

    // 非统一响应结构（如 204 空响应）直接放行
    if (!result || typeof result.code !== 'number') {
      return response
    }

    if (result.code === SUCCESS_CODE) {
      return response
    }

    const message = result.message || '请求失败'

    if (result.code === UNAUTHORIZED_CODE) {
      handleUnauthorized(message)
    } else if (result.code === FORBIDDEN_CODE) {
      ElMessage.error(message || '没有访问权限')
    } else {
      ElMessage.error(message)
    }

    return Promise.reject(new ApiError(result.code, message))
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    const message = error.response?.data?.message

    if (status === UNAUTHORIZED_CODE) {
      handleUnauthorized(message)
    } else if (status === FORBIDDEN_CODE) {
      ElMessage.error(message || '没有访问权限')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (!error.response) {
      ElMessage.error('无法连接服务器，请确认后端已启动')
    } else {
      ElMessage.error(message || '请求失败，请稍后重试')
    }

    return Promise.reject(error)
  },
)

/** 统一解包 {code, message, data}，返回业务数据 */
export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return service.request<ApiResult<T>>(config).then((response) => response.data.data)
}

/** 语义化请求方法，页面只调用接口函数，不直接使用 axios */
export const http = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'GET', url })
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'POST', url, data })
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'PUT', url, data })
  },
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'DELETE', url })
  },
}
