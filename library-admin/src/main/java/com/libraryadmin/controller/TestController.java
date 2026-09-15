package com.libraryadmin.controller;

import com.libraryadmin.service.BookService;
import com.libraryadmin.service.BorrowService;
import com.libraryadmin.service.ReaderService;
import com.libraryadmin.service.UserService;
import com.librarycommon.exception.BizException;
import com.librarycommon.result.Result;
import com.librarycommon.result.ResultCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 联调验证接口（仅用于本地开发环境）
 */
@RestController
@RequestMapping("/test")
public class TestController {

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final ReaderService readerService;
    private final BookService bookService;
    private final BorrowService borrowService;

    public TestController(JdbcTemplate jdbcTemplate,
                          UserService userService,
                          ReaderService readerService,
                          BookService bookService,
                          BorrowService borrowService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.readerService = readerService;
        this.bookService = bookService;
        this.borrowService = borrowService;
    }

    /**
     * 验证数据源连通
     */
    @GetMapping("/db")
    public String db() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return "datasource ok, SELECT 1 = " + result;
    }

    /**
     * 验证 MyBatis-Plus + 4 张表结构映射
     */
    @GetMapping("/tables")
    public String tables() {
        return "user=" + userService.count()
                + ", reader=" + readerService.count()
                + ", book=" + bookService.count()
                + ", borrow=" + borrowService.count();
    }

    /**
     * 验证统一响应体成功格式
     */
    @GetMapping("/result")
    public Result<Map<String, Object>> result() {
        return Result.ok(Map.of(
                "userCount", userService.count(),
                "bookCount", bookService.count()
        ));
    }

    /**
     * 验证业务异常 → 全局异常处理器 → 统一响应
     */
    @GetMapping("/error/biz")
    public Result<Void> errorBiz() {
        throw new BizException(ResultCode.DATA_NOT_FOUND, "测试：业务异常 - 数据不存在");
    }

    /**
     * 验证未知异常 → 兜底 500
     */
    @GetMapping("/error/sys")
    public Result<Void> errorSys() {
        int i = 1 / 0;
        return Result.ok();
    }
}

