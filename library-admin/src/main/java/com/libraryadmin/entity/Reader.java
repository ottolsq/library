package com.libraryadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 读者表
 */
@Data
@TableName("reader")
public class Reader implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 读者主键ID
     */
    @TableId(value = "reader_id", type = IdType.AUTO)
    private Long readerId;

    /**
     * 关联系统用户id
     */
    private Long userId;

    /**
     * 读者姓名
     */
    private String readerName;

    /**
     * 学号，业务唯一
     */
    private String studentId;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 最大可借图书数量
     */
    private Integer maxBorrowCnt;

    /**
     * 状态：0禁用，1正常
     */
    private Integer status;
}
