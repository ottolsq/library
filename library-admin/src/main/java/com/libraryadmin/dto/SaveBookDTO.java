package com.libraryadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增/修改图书请求参数
 *
 * @param isbn         ISBN编号
 * @param bookName     图书名称
 * @param author       作者
 * @param publisher    出版社
 * @param category     图书分类
 * @param totalNum     图书总册数
 * @param availableNum 当前可借库存
 */
public record SaveBookDTO(
        @NotBlank(message = "ISBN不能为空")
        @Size(max = 30, message = "ISBN长度不能超过30位")
        String isbn,

        @NotBlank(message = "图书名称不能为空")
        @Size(max = 200, message = "图书名称长度不能超过200字")
        String bookName,

        @NotBlank(message = "作者不能为空")
        @Size(max = 100, message = "作者长度不能超过100字")
        String author,

        @Size(max = 100, message = "出版社长度不能超过100字")
        String publisher,

        @Size(max = 50, message = "分类长度不能超过50字")
        String category,

        @NotNull(message = "总册数不能为空")
        @Min(value = 0, message = "总册数不能为负数")
        Integer totalNum,

        @NotNull(message = "可借库存不能为空")
        @Min(value = 0, message = "可借库存不能为负数")
        Integer availableNum
) {
}
