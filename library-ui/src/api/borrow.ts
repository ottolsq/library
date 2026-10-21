import { http } from './request'
import type { BorrowPayload, BorrowQuery, BorrowRecord, PageResult } from './types'

/** 分页查询借阅记录：GET /borrow/list */
export function listBorrows(params: BorrowQuery): Promise<PageResult<BorrowRecord>> {
  return http.get<PageResult<BorrowRecord>>('/borrow/list', { params })
}

/** 新增借阅（借书）：POST /borrow */
export function createBorrow(data: BorrowPayload): Promise<number> {
  return http.post<number>('/borrow', data)
}

/** 归还图书：PUT /borrow/{id}/return */
export function returnBook(id: number): Promise<void> {
  return http.put<void>(`/borrow/${id}/return`)
}

/** 删除借阅记录（仅已归还）：DELETE /borrow/{id} */
export function deleteBorrow(id: number): Promise<void> {
  return http.delete<void>(`/borrow/${id}`)
}
