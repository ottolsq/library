-- 实验5 增量脚本：菜单表 + 角色菜单绑定 + 种子数据
-- 在已有 book_library 库上执行（幂等：先删后建）
USE book_library;

-- 5. 菜单表 sys_menu（M目录/C菜单/F按钮 三层）
DROP TABLE IF EXISTS `sys_role_menu`;
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
                             `menu_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
                             `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
                             `parent_id` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父菜单ID，0为根节点',
                             `menu_type` CHAR(1) NOT NULL COMMENT '类型：M目录 C菜单 F按钮',
                             `path` VARCHAR(200) DEFAULT NULL COMMENT '路由地址（C菜单用）',
                             `component` VARCHAR(255) DEFAULT NULL COMMENT '组件路径（C菜单用）',
                             `icon` VARCHAR(50) DEFAULT NULL COMMENT '图标短名（前端映射表翻译）',
                             `perms` VARCHAR(100) DEFAULT NULL COMMENT '权限标识（F按钮用，格式 模块:功能:操作）',
                             `sort_order` INT NOT NULL DEFAULT 0 COMMENT '同级排序，越小越靠前',
                             PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限表';

-- 6. 角色-菜单绑定表（role 0管理员 1读者）
CREATE TABLE `sys_role_menu` (
                                 `role_id` TINYINT UNSIGNED NOT NULL COMMENT '角色ID：0管理员，1读者',
                                 `menu_id` INT UNSIGNED NOT NULL COMMENT '菜单ID',
                                 PRIMARY KEY (`role_id`, `menu_id`),
                                 KEY `idx_menu` (`menu_id`),
                                 CONSTRAINT `fk_role_menu_menu` FOREIGN KEY (`menu_id`)
                                     REFERENCES `sys_menu`(`menu_id`)
                                     ON UPDATE CASCADE
                                     ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单绑定表';

-- ========== 菜单种子数据 ==========

-- 一级目录
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `icon`, `perms`, `sort_order`) VALUES
(100, '系统管理', 0, 'M', NULL, NULL, 'setting', NULL, 1),
(200, '读者管理', 0, 'M', NULL, NULL, 'user', NULL, 2),
(300, '图书管理', 0, 'M', NULL, NULL, 'book', NULL, 3),
(400, '借阅管理', 0, 'M', NULL, NULL, 'borrow', NULL, 4);

-- 二级菜单
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `icon`, `perms`, `sort_order`) VALUES
(110, '用户管理', 100, 'C', 'user', 'system/user/index', 'user', 'user:list', 1),
(210, '读者档案', 200, 'C', 'reader', 'reader/index', 'peoples', 'reader:query', 1),
(310, '图书列表', 300, 'C', 'book', 'book/index', 'documentation', 'book:query', 1),
(410, '借阅记录', 400, 'C', 'borrow', 'borrow/index', 'list', 'borrow:query', 1);

-- 用户管理按钮（F）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `icon`, `perms`, `sort_order`) VALUES
(1101, '新增用户', 110, 'F', NULL, NULL, NULL, 'user:add', 1),
(1102, '修改用户', 110, 'F', NULL, NULL, NULL, 'user:edit', 2),
(1103, '删除用户', 110, 'F', NULL, NULL, NULL, 'user:delete', 3),
(1104, '启用禁用', 110, 'F', NULL, NULL, NULL, 'user:status', 4),
(1105, '重置密码', 110, 'F', NULL, NULL, NULL, 'user:resetPwd', 5);

-- 读者管理按钮（F）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `icon`, `perms`, `sort_order`) VALUES
(2101, '新增读者', 210, 'F', NULL, NULL, NULL, 'reader:add', 1),
(2102, '修改读者', 210, 'F', NULL, NULL, NULL, 'reader:edit', 2),
(2103, '删除读者', 210, 'F', NULL, NULL, NULL, 'reader:remove', 3);

-- 图书管理按钮（F）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `icon`, `perms`, `sort_order`) VALUES
(3101, '新增图书', 310, 'F', NULL, NULL, NULL, 'book:add', 1),
(3102, '修改图书', 310, 'F', NULL, NULL, NULL, 'book:edit', 2),
(3103, '删除图书', 310, 'F', NULL, NULL, NULL, 'book:remove', 3);

-- 借阅管理按钮（F）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `icon`, `perms`, `sort_order`) VALUES
(4101, '新增借阅', 410, 'F', NULL, NULL, NULL, 'borrow:add', 1),
(4102, '归还图书', 410, 'F', NULL, NULL, NULL, 'borrow:edit', 2),
(4103, '删除记录', 410, 'F', NULL, NULL, NULL, 'borrow:remove', 3);

-- ========== 角色绑定 ==========
-- 管理员(role_id=0)：全部目录 + 全部菜单 + 全部按钮
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 0, `menu_id` FROM `sys_menu`;

-- 读者(role_id=1)：图书管理(只看) + 借阅管理(只看)
-- 借书/还书属于管理员业务操作（作业1 F13/F14），不向读者角色绑定按钮权限；
-- 读者查询借阅记录时由后端按本人 reader_id 过滤（F15）。
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 300),
(1, 310),
(1, 400),
(1, 410);

-- ========== 种子数据 ==========
-- 读者账号 reader01/123456（BCrypt 与 admin 同源，对应 user 表 role=1）
INSERT INTO `user` (`username`, `password`, `role`, `status`)
SELECT 'reader01', (SELECT `password` FROM `user` WHERE `username` = 'admin'), 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'reader01');

-- 读者档案：与 reader01、reader 账号一一关联
INSERT INTO `reader` (`user_id`, `reader_name`, `student_id`, `phone`, `max_borrow_cnt`, `status`)
SELECT u.`user_id`, '读者一号', '2024010101', '13800000001', 5, 1
FROM `user` u
WHERE u.`username` = 'reader01'
  AND NOT EXISTS (SELECT 1 FROM `reader` r WHERE r.`user_id` = u.`user_id`);

INSERT INTO `reader` (`user_id`, `reader_name`, `student_id`, `phone`, `max_borrow_cnt`, `status`)
SELECT u.`user_id`, '测试读者', '2024010102', '13800000002', 3, 1
FROM `user` u
WHERE u.`username` = 'reader'
  AND NOT EXISTS (SELECT 1 FROM `reader` r WHERE r.`user_id` = u.`user_id`);

-- 示例图书
INSERT INTO `book` (`isbn`, `book_name`, `author`, `publisher`, `category`, `total_num`, `available_num`) VALUES
('9787111213826', '深入理解计算机系统', 'Randal E. Bryant', '机械工业出版社', '计算机', 5, 5),
('9787115546081', 'Vue.js 设计与实现', '霍春阳', '人民邮电出版社', '前端', 3, 3),
('9787121362228', 'MySQL 必知必会', 'Ben Forta', '电子工业出版社', '数据库', 4, 4),
('9787115479013', '图解 HTTP', '上野宣', '人民邮电出版社', '网络', 3, 3),
('9787020002207', '红楼梦', '曹雪芹', '人民文学出版社', '文学', 6, 6)
ON DUPLICATE KEY UPDATE `book_name` = VALUES(`book_name`);
