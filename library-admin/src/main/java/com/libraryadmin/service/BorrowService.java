package com.libraryadmin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.libraryadmin.dto.BorrowVO;
import com.libraryadmin.dto.CreateBorrowDTO;
import com.libraryadmin.entity.Borrow;

/**
 * 借阅 Service
 */
public interface BorrowService extends IService<Borrow> {

    /**
     * 分页查询借阅记录（按读者姓名/图书名称关键词模糊搜索，联表冗余展示字段）
     *
     * @param currentUserId 当前登录用户ID
     * @param superAdmin    是否超级管理员；非管理员只能看到本人读者档案下的记录
     */
    IPage<BorrowVO> pageBorrows(long pageNum, long pageSize, String keyword, Integer status,
                                Long currentUserId, boolean superAdmin);

    /**
     * 将到期未归还的记录标记为逾期
     *
     * @return 本次更新的记录数
     */
    int markOverdue();

    /**
     * 新增借阅（借书）：校验读者状态与可借余量、图书库存，事务内扣减库存
     */
    Long createBorrow(CreateBorrowDTO dto);

    /**
     * 归还图书：写入实际归还时间、状态改为已归还，事务内回补库存
     */
    void returnBook(Long borrowId, Long currentUserId, boolean superAdmin);

    /**
     * 删除借阅记录（仅允许删除已归还的记录）
     */
    void deleteBorrow(Long borrowId, Long currentUserId, boolean superAdmin);
}
