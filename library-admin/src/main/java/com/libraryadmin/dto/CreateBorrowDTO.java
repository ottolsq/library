package com.libraryadmin.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 新增借阅请求参数
 *
 * @param readerId         读者ID
 * @param bookId           图书ID
 * @param shouldReturnTime 应还到期时间；为空时由服务端按默认借期生成
 */
public record CreateBorrowDTO(
        @NotNull(message = "读者不能为空")
        Long readerId,

        @NotNull(message = "图书不能为空")
        Long bookId,

        @Future(message = "应还时间必须晚于当前时间")
        LocalDateTime shouldReturnTime
) {
}
