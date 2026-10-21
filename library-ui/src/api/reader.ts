import { http } from './request'
import type { PageResult, Reader, ReaderPayload, ReaderQuery } from './types'

/** 分页查询读者：GET /reader/list */
export function listReaders(params: ReaderQuery): Promise<PageResult<Reader>> {
  return http.get<PageResult<Reader>>('/reader/list', { params })
}

/** 读者详情：GET /reader/{id} */
export function getReader(id: number): Promise<Reader> {
  return http.get<Reader>(`/reader/${id}`)
}

/** 新增读者档案：POST /reader */
export function createReader(data: ReaderPayload): Promise<number> {
  return http.post<number>('/reader', data)
}

/** 修改读者档案：PUT /reader/{id} */
export function updateReader(id: number, data: ReaderPayload): Promise<void> {
  return http.put<void>(`/reader/${id}`, data)
}

/** 删除读者档案：DELETE /reader/{id} */
export function deleteReader(id: number): Promise<void> {
  return http.delete<void>(`/reader/${id}`)
}
