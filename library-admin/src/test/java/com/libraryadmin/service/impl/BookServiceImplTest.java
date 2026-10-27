package com.libraryadmin.service.impl;

import com.libraryadmin.dto.SaveBookDTO;
import com.libraryadmin.entity.Book;
import com.libraryadmin.mapper.BookMapper;
import com.libraryadmin.mapper.BorrowMapper;
import com.librarycommon.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 图书库存校验测试。
 */
@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookMapper bookMapper;

    @Mock
    private BorrowMapper borrowMapper;

    private BookServiceImpl bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookServiceImpl(borrowMapper);
        ReflectionTestUtils.setField(bookService, "baseMapper", bookMapper);
    }

    @Test
    @DisplayName("可借库存大于总册数时拒绝新增")
    void createBook_availableGreaterThanTotal_throws() {
        when(bookMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> bookService.createBook(dto(2, 3)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("可借库存不能大于总册数");

        verify(bookMapper, never()).insert(any(Book.class));
    }

    @Test
    @DisplayName("总册数与可借库存的差额小于当前借出数量时拒绝修改")
    void updateBook_totalBelowOutstanding_throws() {
        // 当前 5 册、可借 1 册，说明有 4 册在借
        when(bookMapper.selectById(1L)).thenReturn(book(1L, 5, 1));
        when(bookMapper.selectCount(any())).thenReturn(0L);

        // 改成 2 册、可借 1 册，差额 1 < 在借 4，归还后会超过总册数
        assertThatThrownBy(() -> bookService.updateBook(1L, dto(2, 1)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能小于当前借出数量");

        verify(bookMapper, never()).updateById(any(Book.class));
    }

    @Test
    @DisplayName("库存调整不低于当前借出数量时允许修改")
    void updateBook_validStock_updates() {
        when(bookMapper.selectById(1L)).thenReturn(book(1L, 5, 1));
        when(bookMapper.selectCount(any())).thenReturn(0L);
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);

        // 新增 1 册并保持 4 册在借的差额
        bookService.updateBook(1L, dto(6, 2));

        verify(bookMapper).updateById(any(Book.class));
    }

    @Test
    @DisplayName("有借阅记录时拒绝删除图书")
    void deleteBook_withBorrowRecords_throws() {
        when(bookMapper.selectById(1L)).thenReturn(book(1L, 5, 1));
        when(borrowMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> bookService.deleteBook(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("借阅记录");

        verify(bookMapper, never()).updateById(any(Book.class));
        verify(bookMapper, never()).deleteById(1L);
    }

    @Test
    @DisplayName("删除无借阅记录的图书时改写 ISBN 并逻辑删除")
    void deleteBook_noBorrow_renamesIsbnAndDeletes() {
        when(bookMapper.selectById(1L)).thenReturn(book(1L, 5, 1));
        when(borrowMapper.selectCount(any())).thenReturn(0L);
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);
        when(bookMapper.deleteById(1L)).thenReturn(1);

        bookService.deleteBook(1L);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookMapper).updateById(captor.capture());
        assertThat(captor.getValue().getIsbn()).startsWith("9780000000001del1_");
        assertThat(captor.getValue().getIsbn().length()).isLessThanOrEqualTo(30);
        verify(bookMapper).deleteById(1L);
    }

    private SaveBookDTO dto(int totalNum, int availableNum) {
        return new SaveBookDTO(
                "9780000000001", "测试图书", "测试作者",
                null, null, totalNum, availableNum);
    }

    private Book book(Long bookId, int totalNum, int availableNum) {
        Book book = new Book();
        book.setBookId(bookId);
        book.setIsbn("9780000000001");
        book.setTotalNum(totalNum);
        book.setAvailableNum(availableNum);
        return book;
    }
}
