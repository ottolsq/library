package com.libraryadmin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.libraryadmin.dto.SaveReaderDTO;
import com.libraryadmin.entity.Reader;

/**
 * 读者 Service
 */
public interface ReaderService extends IService<Reader> {

    /**
     * 分页查询读者（按姓名/学号关键词模糊搜索）
     */
    IPage<Reader> pageReaders(long pageNum, long pageSize, String keyword);

    /**
     * 查询读者档案，不存在时抛出业务异常
     */
    Reader getReader(Long readerId);

    /**
     * 按学号查询读者档案，用于注册时校验学号唯一
     */
    Reader getByStudentId(String studentId);

    /**
     * 新增读者档案（学号唯一性校验）
     */
    Long createReader(SaveReaderDTO dto);

    /**
     * 修改读者档案（学号唯一性校验，排除自身）
     */
    void updateReader(Long readerId, SaveReaderDTO dto);

    /**
     * 删除读者档案
     */
    void deleteReader(Long readerId);
}
