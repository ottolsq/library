package com.libraryadmin.dto;

import java.time.LocalDateTime;

/**
 * 借阅记录列表/详情 VO
 *
 * @param borrowId         借阅流水ID
 * @param readerId         读者ID
 * @param readerName       读者姓名（联表冗余，前端直接展示）
 * @param bookId           图书ID
 * @param bookName         图书名称（联表冗余）
 * @param isbn             ISBN（联表冗余）
 * @param borrowTime       借书时间
 * @param shouldReturnTime 应还到期时间
 * @param actualReturnTime 实际归还时间，null代表未归还
 * @param borrowStatus     借阅状态：0借出，1已归还，2逾期
 * @param overdueDays      逾期天数，未逾期为 0
 */
public record BorrowVO(
        Long borrowId,
        Long readerId,
        String readerName,
        Long bookId,
        String bookName,
        String isbn,
        LocalDateTime borrowTime,
        LocalDateTime shouldReturnTime,
        LocalDateTime actualReturnTime,
        Integer borrowStatus,
        long overdueDays
) {
}
