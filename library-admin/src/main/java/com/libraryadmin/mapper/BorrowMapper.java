package com.libraryadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.libraryadmin.entity.Borrow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 借阅记录 Mapper
 */
@Mapper
public interface BorrowMapper extends BaseMapper<Borrow> {
}
