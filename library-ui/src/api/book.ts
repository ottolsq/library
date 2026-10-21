import { http } from './request'
import type { Book, BookPayload, BookQuery, PageResult } from './types'

/** 分页查询图书：GET /book/list */
export function listBooks(params: BookQuery): Promise<PageResult<Book>> {
  return http.get<PageResult<Book>>('/book/list', { params })
}

/** 图书详情：GET /book/{id} */
export function getBook(id: number): Promise<Book> {
  return http.get<Book>(`/book/${id}`)
}

/** 新增图书：POST /book */
export function createBook(data: BookPayload): Promise<number> {
  return http.post<number>('/book', data)
}

/** 修改图书：PUT /book/{id} */
export function updateBook(id: number, data: BookPayload): Promise<void> {
  return http.put<void>(`/book/${id}`, data)
}

/** 删除图书（逻辑删除）：DELETE /book/{id} */
export function deleteBook(id: number): Promise<void> {
  return http.delete<void>(`/book/${id}`)
}
