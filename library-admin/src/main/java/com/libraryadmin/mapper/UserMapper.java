package com.libraryadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.libraryadmin.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
