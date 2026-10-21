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
     * 分页查询图书（按书名/作者/ISBN 关键词模糊搜索）
     */
    IPage<Book> pageBooks(long pageNum, long pageSize, String keyword);

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
