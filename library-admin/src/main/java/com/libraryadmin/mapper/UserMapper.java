package com.libraryadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.libraryadmin.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据登录账号查询用户
     *
     * <p>逻辑删除字段由 MyBatis-Plus 全局配置自动过滤（deleted=1 的不返回）。
     */
    default User selectByUsername(String username) {
        return selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username));
    }
}