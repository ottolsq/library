package com.libraryadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 借阅记录表（读者与图书多对多中间表）
 */
@Data
@TableName("`borrow`")
public class Borrow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 借阅流水主键
     */
    @TableId(value = "borrow_id", type = IdType.AUTO)
    private Long borrowId;

    /**
     * 读者id
     */
    private Long readerId;

    /**
     * 图书id
     */
    private Long bookId;

    /**
     * 借书时间
     */
    private LocalDateTime borrowTime;

    /**
     * 应还到期时间
     */
    private LocalDateTime shouldReturnTime;

    /**
     * 实际归还时间，null代表未归还
     */
    private LocalDateTime actualReturnTime;

    /**
     * 借阅状态：0借出，1已归还，2逾期
     */
    private Integer borrowStatus;
}
