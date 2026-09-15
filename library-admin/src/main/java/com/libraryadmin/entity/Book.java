package com.libraryadmin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图书表
 */
@Data
@TableName("book")
public class Book implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图书主键ID
     */
    @TableId(value = "book_id", type = IdType.AUTO)
    private Long bookId;

    /**
     * ISBN编号，图书唯一标识
     */
    private String isbn;

    /**
     * 图书名称
     */
    private String bookName;

    /**
     * 作者
     */
    private String author;

    /**
     * 出版社
     */
    private String publisher;

    /**
     * 图书分类
     */
    private String category;

    /**
     * 图书总册数
     */
    private Integer totalNum;

    /**
     * 当前可借库存
     */
    private Integer availableNum;

    /**
     * 逻辑删除 0未删，1已删除
     */
    @TableLogic
    private Integer isDeleted;
}
