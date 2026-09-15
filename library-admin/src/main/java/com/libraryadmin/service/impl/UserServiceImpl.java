package com.libraryadmin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.libraryadmin.entity.User;
import com.libraryadmin.mapper.UserMapper;
import com.libraryadmin.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 系统用户 Service 实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
