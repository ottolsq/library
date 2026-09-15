package com.libraryadmin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.libraryadmin.entity.Reader;
import com.libraryadmin.mapper.ReaderMapper;
import com.libraryadmin.service.ReaderService;
import org.springframework.stereotype.Service;

/**
 * 读者 Service 实现
 */
@Service
public class ReaderServiceImpl extends ServiceImpl<ReaderMapper, Reader> implements ReaderService {
}
