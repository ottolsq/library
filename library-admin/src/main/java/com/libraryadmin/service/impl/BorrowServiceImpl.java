package com.libraryadmin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.libraryadmin.entity.Borrow;
import com.libraryadmin.mapper.BorrowMapper;
import com.libraryadmin.service.BorrowService;
import org.springframework.stereotype.Service;

/**
 * 借阅记录 Service 实现
 */
@Service
public class BorrowServiceImpl extends ServiceImpl<BorrowMapper, Borrow> implements BorrowService {
}
