package com.libraryadmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.libraryadmin.dto.BorrowVO;
import com.libraryadmin.dto.CreateBorrowDTO;
import com.libraryadmin.entity.Book;
import com.libraryadmin.entity.Borrow;
import com.libraryadmin.entity.Reader;
import com.libraryadmin.mapper.BookMapper;
import com.libraryadmin.mapper.BorrowMapper;
import com.libraryadmin.mapper.ReaderMapper;
import com.libraryadmin.service.BorrowService;
import com.librarycommon.exception.BizException;
import com.librarycommon.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 借阅 Service 实现
 *
 * <p>借书 / 还书都伴随图书库存的扣减与回补，必须在同一事务内完成。
 * 权限校验由方法注解 {@code @PreAuthorize} 统一拦截。
 */
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl extends ServiceImpl<BorrowMapper, Borrow> implements BorrowService {

    /**
     * 借阅状态常量（与 borrow 表 borrow_status 对齐）
     */
    private static final int STATUS_BORROWED = 0;
    private static final int STATUS_RETURNED = 1;

    private final BookMapper bookMapper;
    private final ReaderMapper readerMapper;
    private final BorrowMapper borrowMapper;

    @Override
    public IPage<BorrowVO> pageBorrows(long pageNum, long pageSize, String keyword, Integer status) {
        // 先分页查借阅记录
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<Borrow>()
                .eq(status != null, Borrow::getBorrowStatus, status)
                .orderByDesc(Borrow::getBorrowId);
        if (StringUtils.hasText(keyword)) {
            // 关键词匹配读者姓名 / 学号（先查出读者 ID，再按 ID 过滤借阅记录）
            List<Long> readerIds = readerMapper.selectList(
                            new LambdaQueryWrapper<Reader>()
                                    .like(Reader::getReaderName, keyword)
                                    .or().like(Reader::getStudentId, keyword))
                    .stream().map(Reader::getReaderId).toList();
            List<Long> bookIds = bookMapper.selectList(
                            new LambdaQueryWrapper<Book>()
                                    .like(Book::getBookName, keyword)
                                    .or().like(Book::getIsbn, keyword))
                    .stream().map(Book::getBookId).toList();
            if (readerIds.isEmpty() && bookIds.isEmpty()) {
                // 关键词命中不了任何读者或图书，直接返回空页
                Page<BorrowVO> empty = new Page<>(pageNum, pageSize, 0);
                empty.setRecords(List.of());
                return empty;
            }
            wrapper.and(w -> {
                if (!readerIds.isEmpty()) {
                    w.in(Borrow::getReaderId, readerIds);
                }
                if (!bookIds.isEmpty()) {
                    if (!readerIds.isEmpty()) {
                        w.or();
                    }
                    w.in(Borrow::getBookId, bookIds);
                }
            });
        }
        IPage<Borrow> page = borrowMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        // 联表字段（读者姓名、图书信息）按 ID 批量补齐，避免 N+1
        List<Borrow> records = page.getRecords();
        if (records.isEmpty()) {
            Page<BorrowVO> empty = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            empty.setRecords(List.of());
            return empty;
        }
        List<Long> readerIds = records.stream().map(Borrow::getReaderId).distinct().toList();
        List<Long> bookIds = records.stream().map(Borrow::getBookId).distinct().toList();
        Map<Long, Reader> readerMap = readerMapper.selectBatchIds(readerIds).stream()
                .collect(Collectors.toMap(Reader::getReaderId, r -> r));
        Map<Long, Book> bookMap = bookMapper.selectBatchIds(bookIds).stream()
                .collect(Collectors.toMap(Book::getBookId, b -> b));
        List<BorrowVO> vos = records.stream()
                .map(b -> toVO(b, readerMap.get(b.getReaderId()), bookMap.get(b.getBookId())))
                .toList();
        // 组装同结构分页结果
        Page<BorrowVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    @Transactional
    public Long createBorrow(CreateBorrowDTO dto) {
        // 1. 读者存在且状态正常
        Reader reader = readerMapper.selectById(dto.readerId());
        if (reader == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "读者不存在");
        }
        if (reader.getStatus() == null || reader.getStatus() != 1) {
            throw new BizException(ResultCode.PARAM_INVALID, "读者已被禁用");
        }
        // 2. 读者当前在借数量未达上限
        Long borrowing = lambdaQuery()
                .eq(Borrow::getReaderId, dto.readerId())
                .eq(Borrow::getBorrowStatus, STATUS_BORROWED)
                .count();
        if (borrowing != null && reader.getMaxBorrowCnt() != null
                && borrowing >= reader.getMaxBorrowCnt()) {
            throw new BizException(ResultCode.PARAM_INVALID,
                    "已达最大可借数量（" + reader.getMaxBorrowCnt() + " 本）");
        }
        // 3. 图书存在且有库存
        Book book = bookMapper.selectById(dto.bookId());
        if (book == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "图书不存在");
        }
        if (book.getAvailableNum() == null || book.getAvailableNum() <= 0) {
            throw new BizException(ResultCode.PARAM_INVALID, "图书库存不足");
        }
        // 4. 事务内：写借阅记录 + 扣减库存（带条件的 update 保证并发下不超扣）
        Borrow borrow = new Borrow();
        borrow.setReaderId(dto.readerId());
        borrow.setBookId(dto.bookId());
        borrow.setBorrowTime(LocalDateTime.now());
        borrow.setShouldReturnTime(dto.shouldReturnTime());
        borrow.setBorrowStatus(STATUS_BORROWED);
        save(borrow);
        int updated = bookMapper.update(
                new LambdaUpdateWrapper<Book>()
                        .eq(Book::getBookId, dto.bookId())
                        .gt(Book::getAvailableNum, 0)
                        .setSql("available_num = available_num - 1"));
        if (updated == 0) {
            throw new BizException(ResultCode.PARAM_INVALID, "图书库存不足");
        }
        return borrow.getBorrowId();
    }

    @Override
    @Transactional
    public void returnBook(Long borrowId) {
        Borrow borrow = requireBorrow(borrowId);
        if (borrow.getBorrowStatus() != null && borrow.getBorrowStatus() != STATUS_BORROWED) {
            throw new BizException(ResultCode.PARAM_INVALID, "该记录已归还");
        }
        // 事务内：写归还时间改状态 + 回补库存
        borrow.setActualReturnTime(LocalDateTime.now());
        borrow.setBorrowStatus(STATUS_RETURNED);
        updateById(borrow);
        bookMapper.update(
                new LambdaUpdateWrapper<Book>()
                        .eq(Book::getBookId, borrow.getBookId())
                        .setSql("available_num = available_num + 1"));
    }

    @Override
    @Transactional
    public void deleteBorrow(Long borrowId) {
        Borrow borrow = requireBorrow(borrowId);
        if (borrow.getBorrowStatus() == null || borrow.getBorrowStatus() == STATUS_BORROWED) {
            // 未归还的记录不允许删除，防止库存与记录失去对应
            throw new BizException(ResultCode.PARAM_INVALID, "借出中的记录不允许删除");
        }
        removeById(borrowId);
    }

    private Borrow requireBorrow(Long borrowId) {
        Borrow borrow = getById(borrowId);
        if (borrow == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "借阅记录不存在");
        }
        return borrow;
    }

    private BorrowVO toVO(Borrow borrow, Reader reader, Book book) {
        return new BorrowVO(
                borrow.getBorrowId(),
                borrow.getReaderId(),
                reader == null ? null : reader.getReaderName(),
                borrow.getBookId(),
                book == null ? null : book.getBookName(),
                book == null ? null : book.getIsbn(),
                borrow.getBorrowTime(),
                borrow.getShouldReturnTime(),
                borrow.getActualReturnTime(),
                borrow.getBorrowStatus()
        );
    }
}
