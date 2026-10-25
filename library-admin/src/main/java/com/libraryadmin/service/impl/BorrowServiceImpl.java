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
import com.libraryadmin.util.BorrowRules;
import com.librarycommon.exception.BizException;
import com.librarycommon.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private final BookMapper bookMapper;
    private final ReaderMapper readerMapper;
    private final BorrowMapper borrowMapper;

    @Override
    public IPage<BorrowVO> pageBorrows(long pageNum, long pageSize, String keyword, Integer status,
                                       Long currentUserId, boolean superAdmin) {
        // 逾期状态由定时任务回写；查询时按“有效状态”兜底过滤与展示，避免读请求产生写操作
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();
        appendStatusFilter(wrapper, status, now);
        wrapper.orderByDesc(Borrow::getBorrowId);

        // 非管理员只能查看本人读者档案下的记录（水平越权防护）
        if (!superAdmin) {
            Reader self = readerMapper.selectOne(new LambdaQueryWrapper<Reader>()
                    .eq(Reader::getUserId, currentUserId));
            if (self == null) {
                return emptyPage(pageNum, pageSize, 0);
            }
            wrapper.eq(Borrow::getReaderId, self.getReaderId());
        }

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
                return emptyPage(pageNum, pageSize, 0);
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
            return emptyPage(page.getCurrent(), page.getSize(), page.getTotal());
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
        LocalDateTime now = LocalDateTime.now();
        // 未指定应还时间时，服务端按默认借期生成，避免依赖前端传值
        LocalDateTime shouldReturnTime = dto.shouldReturnTime() == null
                ? BorrowRules.defaultShouldReturnTime(now)
                : dto.shouldReturnTime();
        if (!shouldReturnTime.isAfter(now)) {
            throw new BizException(ResultCode.PARAM_INVALID, "应还时间必须晚于当前时间");
        }
        // 1. 读者存在且状态正常（FOR UPDATE 锁定读者行，避免并发借书绕过可借上限）
        Reader reader = readerMapper.selectOne(new LambdaQueryWrapper<Reader>()
                .eq(Reader::getReaderId, dto.readerId())
                .last("FOR UPDATE"));
        if (reader == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "读者不存在");
        }
        if (reader.getStatus() == null || reader.getStatus() != 1) {
            throw new BizException(ResultCode.PARAM_INVALID, "读者已被禁用");
        }
        // 2. 读者当前在借数量未达上限（借出与逾期都占用额度）
        Long borrowing = lambdaQuery()
                .eq(Borrow::getReaderId, dto.readerId())
                .in(Borrow::getBorrowStatus,
                        BorrowRules.STATUS_BORROWED, BorrowRules.STATUS_OVERDUE)
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
        borrow.setBorrowTime(now);
        borrow.setShouldReturnTime(shouldReturnTime);
        borrow.setBorrowStatus(BorrowRules.STATUS_BORROWED);
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
    public void returnBook(Long borrowId, Long currentUserId, boolean superAdmin) {
        Borrow borrow = requireBorrow(borrowId);
        assertOwnership(borrow, currentUserId, superAdmin);
        // 借出与逾期都允许归还；只有已归还的记录才拒绝重复归还
        if (borrow.getBorrowStatus() != null
                && borrow.getBorrowStatus() != BorrowRules.STATUS_BORROWED
                && borrow.getBorrowStatus() != BorrowRules.STATUS_OVERDUE) {
            throw new BizException(ResultCode.PARAM_INVALID, "该记录已归还");
        }
        // 事务内：写归还时间改状态 + 回补库存
        borrow.setActualReturnTime(LocalDateTime.now());
        borrow.setBorrowStatus(BorrowRules.STATUS_RETURNED);
        updateById(borrow);
        bookMapper.update(
                new LambdaUpdateWrapper<Book>()
                        .eq(Book::getBookId, borrow.getBookId())
                        .setSql("available_num = available_num + 1"));
    }

    @Override
    @Transactional
    public void deleteBorrow(Long borrowId, Long currentUserId, boolean superAdmin) {
        Borrow borrow = requireBorrow(borrowId);
        assertOwnership(borrow, currentUserId, superAdmin);
        if (borrow.getBorrowStatus() == null
                || borrow.getBorrowStatus() != BorrowRules.STATUS_RETURNED) {
            // 仅已归还的记录允许删除；借出/逾期记录删除会导致库存与记录失去对应
            throw new BizException(ResultCode.PARAM_INVALID, "仅已归还的记录允许删除");
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

    @Override
    public int markOverdue() {
        return borrowMapper.update(null, new LambdaUpdateWrapper<Borrow>()
                .eq(Borrow::getBorrowStatus, BorrowRules.STATUS_BORROWED)
                .lt(Borrow::getShouldReturnTime, LocalDateTime.now())
                .set(Borrow::getBorrowStatus, BorrowRules.STATUS_OVERDUE));
    }

    /**
     * 非管理员只能操作本人读者档案下的记录（水平越权防护）。
     */
    private void assertOwnership(Borrow borrow, Long currentUserId, boolean superAdmin) {
        if (superAdmin) {
            return;
        }
        Reader self = readerMapper.selectOne(new LambdaQueryWrapper<Reader>()
                .eq(Reader::getUserId, currentUserId));
        if (self == null || !Objects.equals(self.getReaderId(), borrow.getReaderId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权操作他人的借阅记录");
        }
    }

    /**
     * 按“有效状态”拼接筛选条件：数据库中的借出记录若已过期，也应按逾期命中。
     */
    private void appendStatusFilter(LambdaQueryWrapper<Borrow> wrapper,
                                    Integer status,
                                    LocalDateTime now) {
        if (status == null) {
            return;
        }
        if (status == BorrowRules.STATUS_BORROWED) {
            wrapper.eq(Borrow::getBorrowStatus, BorrowRules.STATUS_BORROWED)
                    .ge(Borrow::getShouldReturnTime, now);
            return;
        }
        if (status == BorrowRules.STATUS_OVERDUE) {
            wrapper.and(w -> w.eq(Borrow::getBorrowStatus, BorrowRules.STATUS_OVERDUE)
                    .or(o -> o.eq(Borrow::getBorrowStatus, BorrowRules.STATUS_BORROWED)
                            .lt(Borrow::getShouldReturnTime, now)));
            return;
        }
        wrapper.eq(Borrow::getBorrowStatus, status);
    }

    private Page<BorrowVO> emptyPage(long pageNum, long pageSize, long total) {
        Page<BorrowVO> empty = new Page<>(pageNum, pageSize, total);
        empty.setRecords(List.of());
        return empty;
    }

    private BorrowVO toVO(Borrow borrow, Reader reader, Book book) {
        LocalDateTime now = LocalDateTime.now();
        int effectiveStatus = BorrowRules.effectiveStatus(
                borrow.getBorrowStatus(), borrow.getShouldReturnTime(), now);
        LocalDateTime end = borrow.getActualReturnTime() != null
                ? borrow.getActualReturnTime()
                : now;
        long overdueDays = BorrowRules.overdueDays(borrow.getShouldReturnTime(), end);
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
                effectiveStatus,
                overdueDays
        );
    }
}
