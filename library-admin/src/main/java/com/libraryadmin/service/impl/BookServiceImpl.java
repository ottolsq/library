package com.libraryadmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.libraryadmin.dto.SaveBookDTO;
import com.libraryadmin.entity.Book;
import com.libraryadmin.mapper.BookMapper;
import com.libraryadmin.service.BookService;
import com.librarycommon.exception.BizException;
import com.librarycommon.result.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 图书 Service 实现
 *
 * <p>仅做参数与业务规则的最小校验，权限校验由方法注解 {@code @PreAuthorize} 统一拦截。
 */
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Override
    public IPage<Book> pageBooks(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<Book> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            // 关键词同时匹配书名 / 作者 / ISBN
            wrapper.and(w -> w
                    .like(Book::getBookName, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getIsbn, keyword));
        }
        wrapper.orderByDesc(Book::getBookId);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Long createBook(SaveBookDTO dto) {
        checkIsbnUnique(dto.isbn(), null);
        Book book = new Book();
        copyProps(dto, book);
        save(book);
        return book.getBookId();
    }

    @Override
    public void updateBook(Long bookId, SaveBookDTO dto) {
        Book book = requireBook(bookId);
        checkIsbnUnique(dto.isbn(), bookId);
        copyProps(dto, book);
        updateById(book);
    }

    @Override
    public void deleteBook(Long bookId) {
        requireBook(bookId);
        // MyBatis-Plus 已配置逻辑删除字段，removeById 会转为 update is_deleted=1
        removeById(bookId);
    }

    /**
     * ISBN 唯一性校验（修改时排除自身）
     */
    private void checkIsbnUnique(String isbn, Long excludeBookId) {
        Long count = lambdaQuery()
                .eq(Book::getIsbn, isbn)
                .ne(excludeBookId != null, Book::getBookId, excludeBookId)
                .count();
        if (count != null && count > 0) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "ISBN已存在");
        }
    }

    private Book requireBook(Long bookId) {
        Book book = getById(bookId);
        if (book == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "图书不存在");
        }
        return book;
    }

    private void copyProps(SaveBookDTO dto, Book book) {
        book.setIsbn(dto.isbn());
        book.setBookName(dto.bookName());
        book.setAuthor(dto.author());
        book.setPublisher(dto.publisher());
        book.setCategory(dto.category());
        book.setTotalNum(dto.totalNum());
        book.setAvailableNum(dto.availableNum());
    }
}
