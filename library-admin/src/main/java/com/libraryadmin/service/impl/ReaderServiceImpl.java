package com.libraryadmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.libraryadmin.dto.SaveReaderDTO;
import com.libraryadmin.entity.Reader;
import com.libraryadmin.mapper.ReaderMapper;
import com.libraryadmin.service.ReaderService;
import com.librarycommon.exception.BizException;
import com.librarycommon.result.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 读者 Service 实现
 *
 * <p>仅做参数与业务规则的最小校验，权限校验由方法注解 {@code @PreAuthorize} 统一拦截。
 */
@Service
public class ReaderServiceImpl extends ServiceImpl<ReaderMapper, Reader> implements ReaderService {

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
    public Long createReader(SaveReaderDTO dto) {
        checkStudentIdUnique(dto.studentId(), null);
        Reader reader = new Reader();
        copyProps(dto, reader);
        save(reader);
        return reader.getReaderId();
    }

    @Override
    public void updateReader(Long readerId, SaveReaderDTO dto) {
        Reader reader = requireReader(readerId);
        checkStudentIdUnique(dto.studentId(), readerId);
        copyProps(dto, reader);
        updateById(reader);
    }

    @Override
    public void deleteReader(Long readerId) {
        requireReader(readerId);
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
