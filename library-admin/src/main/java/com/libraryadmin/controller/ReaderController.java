package com.libraryadmin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.libraryadmin.dto.SaveReaderDTO;
import com.libraryadmin.entity.Reader;
import com.libraryadmin.service.ReaderService;
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

import java.util.Map;

/**
 * 读者接口
 *
 * <p>所有方法均挂 {@code @PreAuthorize("@ss.hasPermission('reader:xxx')")}，
 * 与 {@code SecurityConstants.PERMISSION_RULES} 协同实现路径级 + 方法级双重校验。
 */
@RestController
@RequestMapping("/reader")
@RequiredArgsConstructor
public class ReaderController {

    private final ReaderService readerService;

    /**
     * 分页查询读者列表（关键词匹配姓名/学号/电话）
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('reader:query')")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        IPage<Reader> page = readerService.pageReaders(pageNum, pageSize, keyword);
        return Result.ok(Map.of(
                "total", page.getTotal(),
                "pageNum", page.getCurrent(),
                "pageSize", page.getSize(),
                "items", page.getRecords()
        ));
    }

    /**
     * 查询读者详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('reader:query')")
    public Result<Reader> detail(@PathVariable Long id) {
        return Result.ok(readerService.getById(id));
    }

    /**
     * 新增读者档案
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermission('reader:add')")
    public Result<Long> create(@Valid @RequestBody SaveReaderDTO dto) {
        return Result.ok(readerService.createReader(dto));
    }

    /**
     * 修改读者档案
     */
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('reader:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SaveReaderDTO dto) {
        readerService.updateReader(id, dto);
        return Result.ok();
    }

    /**
     * 删除读者档案
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('reader:remove')")
    public Result<Void> delete(@PathVariable Long id) {
        readerService.deleteReader(id);
        return Result.ok();
    }
}
