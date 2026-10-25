-- 作业2 实现对齐脚本（在已有 book_library 库上执行）
-- 目的：把读者角色的借阅按钮权限收回，使借书/还书仅管理员可办理（作业1 F13/F14）；
--       读者仅保留借阅记录查看权限，后端会按本人 reader_id 做水平越权过滤（F15）。
--
-- 注意：登录用户的权限集合缓存在 Redis（login:user:{userId}，TTL 24 小时），
--       执行本脚本后已登录用户需重新登录；如需立即生效，可清理登录缓存：
--       redis-cli --scan --pattern "login:user:*" | xargs -r redis-cli del
USE book_library;

DELETE FROM `sys_role_menu`
WHERE `role_id` = 1
  AND `menu_id` IN (4101, 4102);

-- 校验：读者角色应只剩 300/310/400/410 四个菜单/目录节点
SELECT rm.role_id, m.menu_id, m.menu_name, m.menu_type, m.perms
FROM `sys_role_menu` rm
JOIN `sys_menu` m ON m.menu_id = rm.menu_id
WHERE rm.role_id = 1
ORDER BY m.menu_id;
