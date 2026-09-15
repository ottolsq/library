package com.libraryadmin.controller;

import com.libraryadmin.service.ReaderService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 读者接口
 */
@RestController
@RequestMapping("/reader")
public class ReaderController {

    private final ReaderService readerService;

    public ReaderController(ReaderService readerService) {
        this.readerService = readerService;
    }
}
