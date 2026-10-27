package com.libraryadmin.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 新增借阅请求参数
 *
 * @param readerId         读者ID；管理员必填，读者由服务端强制使用本人档案，可不传
 * @param bookId           图书ID
 * @param shouldReturnTime 应还到期时间；仅管理员生效，为空或读者操作时按默认借期生成
 */
public record CreateBorrowDTO(
        Long readerId,

        @NotNull(message = "图书不能为空")
        Long bookId,

        LocalDateTime shouldReturnTime
) {
}
