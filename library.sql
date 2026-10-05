-- 先删除库（可选，全新环境注释掉）
DROP DATABASE IF EXISTS book_library;
CREATE DATABASE book_library DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE book_library;

-- 1. 系统用户表 user
CREATE TABLE `user` (
                        `user_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
                        `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
                        `password` VARCHAR(100) NOT NULL COMMENT '登录密码（BCrypt 哈希）',
                        `role` TINYINT NOT NULL COMMENT '角色 0管理员,1读者',
                        `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态 0禁用,1正常',
                        `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
                        `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删，1已删',
                        PRIMARY KEY (`user_id`),
                        UNIQUE KEY `uniq_username` (`username`) COMMENT '账号唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';

-- 2. 读者表 reader
CREATE TABLE `reader` (
                          `reader_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '读者主键ID',
                          `user_id` INT UNSIGNED NOT NULL COMMENT '关联系统用户id',
                          `reader_name` VARCHAR(50) NOT NULL COMMENT '读者姓名',
                          `student_id` VARCHAR(30) NOT NULL COMMENT '学号，业务唯一',
                          `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
                          `max_borrow_cnt` TINYINT UNSIGNED NOT NULL DEFAULT 5 COMMENT '最大可借图书数量',
                          `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1正常',
                          PRIMARY KEY (`reader_id`),
                          UNIQUE KEY `uniq_student_id` (`student_id`) COMMENT '学号唯一索引',
                          UNIQUE KEY `uniq_reader_user` (`user_id`) COMMENT '一对一关联用户，唯一',
                          CONSTRAINT `fk_reader_user` FOREIGN KEY (`user_id`)
                              REFERENCES `user`(`user_id`)
                              ON UPDATE CASCADE
                              ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='读者表';

-- 3. 图书表 book
CREATE TABLE `book` (
                        `book_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '图书主键ID',
                        `isbn` VARCHAR(30) NOT NULL COMMENT 'ISBN编号，图书唯一标识',
                        `book_name` VARCHAR(200) NOT NULL COMMENT '图书名称',
                        `author` VARCHAR(100) NOT NULL COMMENT '作者',
                        `publisher` VARCHAR(100) DEFAULT NULL COMMENT '出版社',
                        `category` VARCHAR(50) DEFAULT NULL COMMENT '图书分类',
                        `total_num` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '图书总册数',
                        `available_num` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前可借库存',
                        `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删，1已删除',
                        PRIMARY KEY (`book_id`),
                        UNIQUE KEY `uniq_isbn` (`isbn`) COMMENT 'ISBN唯一索引',
                        KEY `idx_category` (`category`) COMMENT '图书分类查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图书表';

-- 4. 借阅记录表 borrow（M:N中间表：读者<->图书）
CREATE TABLE `borrow` (
                          `borrow_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '借阅流水主键',
                          `reader_id` INT UNSIGNED NOT NULL COMMENT '读者id',
                          `book_id` INT UNSIGNED NOT NULL COMMENT '图书id',
                          `borrow_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '借书时间',
                          `should_return_time` DATETIME NOT NULL COMMENT '应还到期时间',
                          `actual_return_time` DATETIME DEFAULT NULL COMMENT '实际归还时间，null代表未归还',
                          `borrow_status` TINYINT NOT NULL DEFAULT 0 COMMENT '借阅状态：0借出，1已归还，2逾期',
                          PRIMARY KEY (`borrow_id`),
                          KEY `idx_reader_id` (`reader_id`) COMMENT '读者查询借阅索引',
                          KEY `idx_book_id` (`book_id`) COMMENT '图书借阅记录索引',
                          KEY `idx_borrow_status` (`borrow_status`) COMMENT '借阅状态索引',
                          CONSTRAINT `fk_borrow_reader` FOREIGN KEY (`reader_id`)
                              REFERENCES `reader`(`reader_id`)
                              ON UPDATE CASCADE
                              ON DELETE RESTRICT,
                          CONSTRAINT `fk_borrow_book` FOREIGN KEY (`book_id`)
                              REFERENCES `book`(`book_id`)
                              ON UPDATE CASCADE
                              ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='借阅记录表（读者与图书多对多中间表）';
