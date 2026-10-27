-- 03_permission_alignment.sql 权限对齐脚本
-- 说明：在 02_menu.sql 之后执行。读者可自助借书（保留 4101 borrow:add），
--       还书仍仅管理员可办理（收回 4102 borrow:edit），删除记录也不向读者开放；
--       读者仅保留借阅记录查看权限，后端会按本人 reader_id 做水平越权过滤（F15）。
--
-- 注意：登录用户的权限集合缓存在 Redis（login:user:{userId}，TTL 24 小时），
--       执行本脚本后已登录用户需重新登录；如需立即生效，可清理登录缓存：
--       redis-cli --scan --pattern "login:user:*" | xargs -r redis-cli del
USE book_library;

DELETE FROM `sys_role_menu`
WHERE `role_id` = 1
  AND `menu_id` IN (4102, 4103);

-- 兼容已初始化过的库：确保读者拥有自助借书按钮权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 4101);

-- 校验：读者角色应只剩 300/310/400/410/4101
SELECT rm.role_id, m.menu_id, m.menu_name, m.menu_type, m.perms
FROM `sys_role_menu` rm
JOIN `sys_menu` m ON m.menu_id = rm.menu_id
WHERE rm.role_id = 1
ORDER BY m.menu_id;
