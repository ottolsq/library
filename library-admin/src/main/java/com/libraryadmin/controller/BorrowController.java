package com.libraryadmin.controller;

import com.libraryadmin.service.BorrowService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 借阅记录接口
 */
@RestController
@RequestMapping("/borrow")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }
}
