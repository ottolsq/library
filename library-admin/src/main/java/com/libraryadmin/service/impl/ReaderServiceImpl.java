package com.libraryadmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.libraryadmin.dto.SaveReaderDTO;
import com.libraryadmin.entity.Borrow;
import com.libraryadmin.entity.Reader;
import com.libraryadmin.mapper.BorrowMapper;
import com.libraryadmin.mapper.ReaderMapper;
import com.libraryadmin.mapper.UserMapper;
import com.libraryadmin.service.ReaderService;
import com.librarycommon.exception.BizException;
import com.librarycommon.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 读者 Service 实现
 *
 * <p>仅做参数与业务规则的最小校验，权限校验由方法注解 {@code @PreAuthorize} 统一拦截。
 */
@Service
@RequiredArgsConstructor
public class ReaderServiceImpl extends ServiceImpl<ReaderMapper, Reader> implements ReaderService {

    private final UserMapper userMapper;
    private final BorrowMapper borrowMapper;

    @Override
    public IPage<Reader> pageReaders(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<Reader> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            // 关键词同时匹配姓名 / 学号 / 电话
            wrapper.and(w -> w
                    .like(Reader::getReaderName, keyword)
                    .or().like(Reader::getStudentId, keyword)
                    .or().like(Reader::getPhone, keyword));
        }
        wrapper.orderByDesc(Reader::getReaderId);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Reader getReader(Long readerId) {
        return requireReader(readerId);
    }

    @Override
    public Reader getByStudentId(String studentId) {
        return lambdaQuery().eq(Reader::getStudentId, studentId).one();
    }

    @Override
    public Long createReader(SaveReaderDTO dto) {
        checkStudentIdUnique(dto.studentId(), null);
        checkUserAvailable(dto.userId(), null);
        Reader reader = new Reader();
        copyProps(dto, reader);
        save(reader);
        return reader.getReaderId();
    }

    @Override
    public void updateReader(Long readerId, SaveReaderDTO dto) {
        Reader reader = requireReader(readerId);
        checkStudentIdUnique(dto.studentId(), readerId);
        checkUserAvailable(dto.userId(), readerId);
        copyProps(dto, reader);
        updateById(reader);
    }

    @Override
    public void deleteReader(Long readerId) {
        requireReader(readerId);
        Long borrowCount = borrowMapper.selectCount(
                new LambdaQueryWrapper<Borrow>().eq(Borrow::getReaderId, readerId));
        if (borrowCount != null && borrowCount > 0) {
            throw new BizException(ResultCode.PARAM_INVALID, "该读者存在借阅记录，不能删除");
        }
        removeById(readerId);
    }

    /**
     * 学号唯一性校验（修改时排除自身）
     */
    private void checkStudentIdUnique(String studentId, Long excludeReaderId) {
        Long count = lambdaQuery()
                .eq(Reader::getStudentId, studentId)
                .ne(excludeReaderId != null, Reader::getReaderId, excludeReaderId)
                .count();
        if (count != null && count > 0) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "学号已存在");
        }
    }

    /**
     * 关联用户必须存在，且不能被其他读者档案占用（user 与 reader 一对一）
     */
    private void checkUserAvailable(Long userId, Long excludeReaderId) {
        if (userId == null || userMapper.selectById(userId) == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "关联用户不存在");
        }
        Long count = lambdaQuery()
                .eq(Reader::getUserId, userId)
                .ne(excludeReaderId != null, Reader::getReaderId, excludeReaderId)
                .count();
        if (count != null && count > 0) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "该用户已关联读者档案");
        }
    }

    private Reader requireReader(Long readerId) {
        Reader reader = getById(readerId);
        if (reader == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "读者不存在");
        }
        return reader;
    }

    private void copyProps(SaveReaderDTO dto, Reader reader) {
        reader.setUserId(dto.userId());
        reader.setReaderName(dto.readerName());
        reader.setStudentId(dto.studentId());
        reader.setPhone(dto.phone());
        reader.setMaxBorrowCnt(dto.maxBorrowCnt());
    }
}
