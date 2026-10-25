package com.libraryadmin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.libraryadmin.dto.BorrowVO;
import com.libraryadmin.dto.CreateBorrowDTO;
import com.libraryadmin.service.BorrowService;
import com.librarycommon.result.Result;
import com.librarycommon.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * 借阅接口
 *
 * <p>所有方法均挂 {@code @PreAuthorize("@ss.hasPermission('borrow:xxx')")}，
 * 与 {@code SecurityConstants.PERMISSION_RULES} 协同实现路径级 + 方法级双重校验。
 */
@RestController
@RequestMapping("/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    /**
     * 分页查询借阅记录（关键词匹配读者姓名/图书名称；status 筛选借阅状态）
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('borrow:query')")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @AuthenticationPrincipal LoginUser loginUser) {
        IPage<BorrowVO> page = borrowService.pageBorrows(
                pageNum, pageSize, keyword, status,
                loginUser.getUserId(), loginUser.isSuperAdmin());
        return Result.ok(Map.of(
                "total", page.getTotal(),
                "pageNum", page.getCurrent(),
                "pageSize", page.getSize(),
                "items", page.getRecords()
        ));
    }

    /**
     * 新增借阅（借书）：校验读者可借余量与图书库存，事务内扣减库存
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermission('borrow:add')")
    public Result<Long> create(@Valid @RequestBody CreateBorrowDTO dto) {
        return Result.ok(borrowService.createBorrow(dto));
    }

    /**
     * 归还图书：写入归还时间并回补库存
     */
    @PutMapping("/{id}/return")
    @PreAuthorize("@ss.hasPermission('borrow:edit')")
    public Result<Void> returnBook(@PathVariable Long id,
                                   @AuthenticationPrincipal LoginUser loginUser) {
        borrowService.returnBook(id, loginUser.getUserId(), loginUser.isSuperAdmin());
        return Result.ok();
    }

    /**
     * 删除借阅记录（仅已归还的记录）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('borrow:remove')")
    public Result<Void> delete(@PathVariable Long id,
                               @AuthenticationPrincipal LoginUser loginUser) {
        borrowService.deleteBorrow(id, loginUser.getUserId(), loginUser.isSuperAdmin());
        return Result.ok();
    }
}
