package com.libraryadmin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.libraryadmin.entity.Book;
import com.libraryadmin.mapper.BookMapper;
import com.libraryadmin.service.BookService;
import org.springframework.stereotype.Service;

/**
 * 图书 Service 实现
 */
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {
}
