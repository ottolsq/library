package com.libraryadmin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.libraryadmin.dto.SaveBookDTO;
import com.libraryadmin.entity.Book;

/**
 * 图书 Service
 */
public interface BookService extends IService<Book> {

    /**
     * 分页查询图书（关键词匹配书名/作者/ISBN，可选按分类精确筛选）
     */
    IPage<Book> pageBooks(long pageNum, long pageSize, String keyword, String category);

    /**
     * 查询图书详情，不存在时抛出业务异常
     */
    Book getBook(Long bookId);

    /**
     * 新增图书（ISBN 唯一性校验）
     */
    Long createBook(SaveBookDTO dto);

    /**
     * 修改图书（ISBN 唯一性校验，排除自身）
     */
    void updateBook(Long bookId, SaveBookDTO dto);

    /**
     * 删除图书（逻辑删除）
     */
    void deleteBook(Long bookId);
}
