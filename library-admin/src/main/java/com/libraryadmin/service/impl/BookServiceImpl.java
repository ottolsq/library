package com.libraryadmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.libraryadmin.dto.SaveBookDTO;
import com.libraryadmin.entity.Book;
import com.libraryadmin.entity.Borrow;
import com.libraryadmin.mapper.BookMapper;
import com.libraryadmin.mapper.BorrowMapper;
import com.libraryadmin.service.BookService;
import com.librarycommon.exception.BizException;
import com.librarycommon.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 图书 Service 实现
 *
 * <p>仅做参数与业务规则的最小校验，权限校验由方法注解 {@code @PreAuthorize} 统一拦截。
 */
@Service
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    private static final int ISBN_MAX_LENGTH = 30;

    private final BorrowMapper borrowMapper;

    @Override
    public IPage<Book> pageBooks(long pageNum, long pageSize, String keyword, String category) {
        LambdaQueryWrapper<Book> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            // 关键词同时匹配书名 / 作者 / ISBN
            wrapper.and(w -> w
                    .like(Book::getBookName, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getIsbn, keyword));
        }
        // 分类为精确筛选，与关键词之间是 AND 关系，支持组合查询
        wrapper.eq(StringUtils.hasText(category), Book::getCategory, category);
        wrapper.orderByDesc(Book::getBookId);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Book getBook(Long bookId) {
        return requireBook(bookId);
    }

    @Override
    @Transactional
    public Long createBook(SaveBookDTO dto) {
        checkIsbnUnique(dto.isbn(), null);
        checkStock(dto, null);
        Book book = new Book();
        copyProps(dto, book);
        try {
            if (!save(book)) {
                throw new BizException(ResultCode.SYSTEM_ERROR, "新增图书失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS,
                    "ISBN已存在（可能被已删除图书占用）");
        }
        return book.getBookId();
    }

    @Override
    @Transactional
    public void updateBook(Long bookId, SaveBookDTO dto) {
        Book book = requireBook(bookId);
        checkIsbnUnique(dto.isbn(), bookId);
        checkStock(dto, book);
        copyProps(dto, book);
        try {
            if (!updateById(book)) {
                throw new BizException(ResultCode.DATA_NOT_FOUND, "图书不存在或已被删除");
            }
        } catch (DuplicateKeyException e) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS,
                    "ISBN已存在（可能被已删除图书占用）");
        }
    }

    /**
     * 库存校验：可借库存不能超过总册数，且不能把总册数改到当前在借数量以下。
     *
     * <p>当前在借数量 = 修改前的总册数 - 修改前的可借库存。若编辑后
     * 「总册数 - 可借库存」小于该值，归还时就会把可借库存推高到总册数以上。
     * <p>前端已有同名校验，这里兜底防止绕过页面直接调用接口。
     */
    private void checkStock(SaveBookDTO dto, Book current) {
        if (dto.totalNum() == null || dto.availableNum() == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "总册数与可借库存不能为空");
        }
        if (dto.availableNum() > dto.totalNum()) {
            throw new BizException(ResultCode.PARAM_INVALID, "可借库存不能大于总册数");
        }
        if (current == null) {
            return;
        }
        int currentTotal = current.getTotalNum() == null ? 0 : current.getTotalNum();
        int currentAvailable = current.getAvailableNum() == null ? 0 : current.getAvailableNum();
        int outstanding = Math.max(currentTotal - currentAvailable, 0);
        if (dto.totalNum() - dto.availableNum() < outstanding) {
            throw new BizException(ResultCode.PARAM_INVALID,
                    "总册数与可借库存的差额不能小于当前借出数量（" + outstanding + " 本）");
        }
    }

    @Override
    @Transactional
    public void deleteBook(Long bookId) {
        Book book = requireBook(bookId);
        // 有借阅记录时保留图书，避免历史借阅记录失去书名/ISBN
        Long borrowCount = borrowMapper.selectCount(
                new LambdaQueryWrapper<Borrow>().eq(Borrow::getBookId, bookId));
        if (borrowCount != null && borrowCount > 0) {
            throw new BizException(ResultCode.PARAM_INVALID, "该图书存在借阅记录，不能删除");
        }
        // 逻辑删除前改写 ISBN，释放唯一索引，允许重新录入同一 ISBN
        book.setIsbn(deletedIsbn(book));
        if (!updateById(book)) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "删除图书失败：ISBN 改写失败");
        }
        if (!removeById(bookId)) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "图书不存在或已被删除");
        }
    }

    /**
     * ISBN 唯一性校验（修改时排除自身）
     */
    private void checkIsbnUnique(String isbn, Long excludeBookId) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<Book>()
                .eq(Book::getIsbn, isbn)
                .ne(excludeBookId != null, Book::getBookId, excludeBookId));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "ISBN已存在");
        }
    }

    /**
     * 逻辑删除时使用的唯一墓碑 ISBN，长度不超过列宽 30。
     */
    private String deletedIsbn(Book book) {
        String suffix = "del" + book.getBookId() + "_"
                + UUID.randomUUID().toString().substring(0, 8);
        if (suffix.length() > ISBN_MAX_LENGTH) {
            suffix = suffix.substring(0, ISBN_MAX_LENGTH);
        }
        String base = book.getIsbn() == null ? "" : book.getIsbn();
        int maxBaseLength = Math.max(ISBN_MAX_LENGTH - suffix.length(), 0);
        if (base.length() > maxBaseLength) {
            base = base.substring(0, maxBaseLength);
        }
        return base + suffix;
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
