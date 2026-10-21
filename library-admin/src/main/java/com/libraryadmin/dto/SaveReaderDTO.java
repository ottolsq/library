package com.libraryadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增/修改读者请求参数
 *
 * @param userId      关联系统用户ID
 * @param readerName  读者姓名
 * @param studentId   学号，业务唯一
 * @param phone       联系电话
 * @param maxBorrowCnt 最大可借图书数量
 */
public record SaveReaderDTO(
        @NotNull(message = "关联用户不能为空")
        Long userId,

        @NotBlank(message = "读者姓名不能为空")
        @Size(max = 50, message = "姓名长度不能超过50字")
        String readerName,

        @NotBlank(message = "学号不能为空")
        @Size(max = 30, message = "学号长度不能超过30位")
        String studentId,

        @Pattern(regexp = "^$|\\d{5,20}", message = "联系电话应为5-20位数字")
        String phone,

        @NotNull(message = "最大可借数量不能为空")
        @Min(1)
        @Max(99)
        Integer maxBorrowCnt
) {
}
