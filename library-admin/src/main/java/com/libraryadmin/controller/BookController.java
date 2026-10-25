package com.libraryadmin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.libraryadmin.dto.SaveBookDTO;
import com.libraryadmin.entity.Book;
import com.libraryadmin.service.BookService;
import com.librarycommon.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 图书接口
 *
 * <p>所有方法均挂 {@code @PreAuthorize("@ss.hasPermission('book:xxx')")}，
 * 与 {@code SecurityConstants.PERMISSION_RULES} 协同实现路径级 + 方法级双重校验。
 */
@RestController
@RequestMapping("/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * 分页查询图书列表（关键词匹配书名/作者/ISBN）
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('book:query')")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        IPage<Book> page = bookService.pageBooks(pageNum, pageSize, keyword, category);
        return Result.ok(Map.of(
                "total", page.getTotal(),
                "pageNum", page.getCurrent(),
                "pageSize", page.getSize(),
                "items", page.getRecords()
        ));
    }

    /**
     * 查询图书详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('book:query')")
    public Result<Book> detail(@PathVariable Long id) {
        return Result.ok(bookService.getBook(id));
    }

    /**
     * 新增图书
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermission('book:add')")
    public Result<Long> create(@Valid @RequestBody SaveBookDTO dto) {
        return Result.ok(bookService.createBook(dto));
    }

    /**
     * 修改图书
     */
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('book:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SaveBookDTO dto) {
        bookService.updateBook(id, dto);
        return Result.ok();
    }

    /**
     * 删除图书（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('book:remove')")
    public Result<Void> delete(@PathVariable Long id) {
        bookService.deleteBook(id);
        return Result.ok();
    }
}
