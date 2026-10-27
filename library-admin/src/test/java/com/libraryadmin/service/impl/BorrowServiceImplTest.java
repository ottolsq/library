package com.libraryadmin.service.impl;

import com.libraryadmin.dto.CreateBorrowDTO;
import com.libraryadmin.entity.Book;
import com.libraryadmin.entity.Borrow;
import com.libraryadmin.entity.Reader;
import com.libraryadmin.mapper.BookMapper;
import com.libraryadmin.mapper.BorrowMapper;
import com.libraryadmin.mapper.ReaderMapper;
import com.librarycommon.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 借阅业务测试：读者自助借书的归属绑定与默认借期。
 */
@ExtendWith(MockitoExtension.class)
class BorrowServiceImplTest {

    @Mock
    private BookMapper bookMapper;

    @Mock
    private ReaderMapper readerMapper;

    @Mock
    private BorrowMapper borrowMapper;

    private BorrowServiceImpl borrowService;

    @BeforeEach
    void setUp() {
        borrowService = new BorrowServiceImpl(bookMapper, readerMapper, borrowMapper);
        // ServiceImpl 的 baseMapper 由 Spring 注入，单元测试中手动补上
        ReflectionTestUtils.setField(borrowService, "baseMapper", borrowMapper);
    }

    @Test
    @DisplayName("读者为他人借书时被拒绝")
    void createBorrow_readerForOthers_throws() {
        when(readerMapper.selectOne(any())).thenReturn(reader(10L, 5L));

        CreateBorrowDTO dto = new CreateBorrowDTO(99L, 1L, null);

        assertThatThrownBy(() -> borrowService.createBorrow(dto, 5L, false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只能为本人借书");

        verify(bookMapper, never()).update(any());
    }

    @Test
    @DisplayName("读者账号未关联读者档案时拒绝借书")
    void createBorrow_readerWithoutProfile_throws() {
        when(readerMapper.selectOne(any())).thenReturn(null);

        CreateBorrowDTO dto = new CreateBorrowDTO(null, 1L, null);

        assertThatThrownBy(() -> borrowService.createBorrow(dto, 5L, false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未关联读者档案");
    }

    @Test
    @DisplayName("读者借书强制使用本人档案与默认借期")
    void createBorrow_reader_success_usesSelfAndDefaultDueDate() {
        Reader self = reader(10L, 5L);
        self.setStatus(1);
        self.setMaxBorrowCnt(5);
        when(readerMapper.selectOne(any())).thenReturn(self);
        when(borrowMapper.selectCount(any())).thenReturn(0L);

        Book book = new Book();
        book.setBookId(1L);
        book.setAvailableNum(2);
        when(bookMapper.selectById(1L)).thenReturn(book);
        when(bookMapper.update(any())).thenReturn(1);
        doAnswer(invocation -> {
            Borrow borrow = invocation.getArgument(0);
            borrow.setBorrowId(99L);
            return 1;
        }).when(borrowMapper).insert(any(Borrow.class));

        // 即使前端传了 10 天，读者也只能使用默认 30 天借期
        CreateBorrowDTO dto = new CreateBorrowDTO(null, 1L, LocalDateTime.now().plusDays(10));

        Long borrowId = borrowService.createBorrow(dto, 5L, false);

        assertThat(borrowId).isEqualTo(99L);
        ArgumentCaptor<Borrow> captor = ArgumentCaptor.forClass(Borrow.class);
        verify(borrowMapper).insert(captor.capture());
        Borrow saved = captor.getValue();
        assertThat(saved.getReaderId()).isEqualTo(10L);
        assertThat(saved.getShouldReturnTime())
                .isAfter(LocalDateTime.now().plusDays(29));
    }

    private Reader reader(Long readerId, Long userId) {
        Reader reader = new Reader();
        reader.setReaderId(readerId);
        reader.setUserId(userId);
        return reader;
    }
}
