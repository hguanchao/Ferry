-- ============================================================
-- 系统管理演示数据
-- 覆盖：部门 / 岗位 / 角色 / 用户 / 字典 / 参数
-- 所有账号初始密码统一为 123456（bcrypt，与 admin 同算法）
-- 执行顺序：本脚本依赖 ferry_data.sql 中的部门与岗位 id
-- ============================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 部门：在研发部下增加测试与架构两组，补齐技术部下的基础架构组
-- ----------------------------
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `ancestors`, `code`, `name`, `status`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000001001, '000000', 1947124125297291265, '1947122620343267329,1947124125297291265', 'QA', '质量保障组', 1, 2, 1604755687527075842, '2026-03-02 09:15:00', 1604755687527075842, '2026-03-02 09:15:00', 0);
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `ancestors`, `code`, `name`, `status`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000001002, '000000', 1947122620343267329, '1947122620343267329', 'ARC', '架构组', 1, 2, 1604755687527075842, '2026-03-02 09:15:00', 1604755687527075842, '2026-03-02 09:15:00', 0);
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `ancestors`, `code`, `name`, `status`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000001003, '000000', 1947122356555100162, '1947122356555100162', 'INFRA', '基础架构组', 1, 1, 1604755687527075842, '2026-03-02 09:15:00', 1604755687527075842, '2026-03-02 09:15:00', 0);

-- ----------------------------
-- 岗位：补齐交付流程上的职责序列
-- ----------------------------
INSERT INTO `sys_post` (`id`, `tenant_id`, `code`, `name`, `status`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000002001, '000000', 'DEVOPS', 'DevOps 工程师', 1, 3, 1604755687527075842, '2026-03-02 10:00:00', 1604755687527075842, '2026-03-02 10:00:00', 0);
INSERT INTO `sys_post` (`id`, `tenant_id`, `code`, `name`, `status`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000002002, '000000', 'QAENG', '测试工程师', 1, 4, 1604755687527075842, '2026-03-02 10:00:00', 1604755687527075842, '2026-03-02 10:00:00', 0);
INSERT INTO `sys_post` (`id`, `tenant_id`, `code`, `name`, `status`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000002003, '000000', 'PM', '产品经理', 1, 5, 1604755687527075842, '2026-03-02 10:00:00', 1604755687527075842, '2026-03-02 10:00:00', 0);
INSERT INTO `sys_post` (`id`, `tenant_id`, `code`, `name`, `status`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000002004, '000000', 'OPSENG', '运维工程师', 1, 6, 1604755687527075842, '2026-03-02 10:00:00', 1604755687527075842, '2026-03-02 10:00:00', 0);

-- ----------------------------
-- 角色：按数据范围区分，覆盖 5 种 data_scope 中的 4 种
-- ----------------------------
INSERT INTO `sys_role` (`id`, `tenant_id`, `code`, `name`, `data_scope`, `status`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000003001, '000000', 'dept_admin', '部门管理员', 4, 1, 3, 1604755687527075842, '2026-03-02 10:30:00', 1604755687527075842, '2026-03-02 10:30:00', 0);
INSERT INTO `sys_role` (`id`, `tenant_id`, `code`, `name`, `data_scope`, `status`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000003002, '000000', 'ops', '运维工程师', 3, 1, 4, 1604755687527075842, '2026-03-02 10:30:00', 1604755687527075842, '2026-03-02 10:30:00', 0);
INSERT INTO `sys_role` (`id`, `tenant_id`, `code`, `name`, `data_scope`, `status`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000003003, '000000', 'readonly', '只读审计员', 1, 1, 5, 1604755687527075842, '2026-03-02 10:30:00', 1604755687527075842, '2026-03-02 10:30:00', 0);
INSERT INTO `sys_role` (`id`, `tenant_id`, `code`, `name`, `data_scope`, `status`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000003004, '000000', 'self_only', '普通研发', 5, 1, 6, 1604755687527075842, '2026-03-02 10:30:00', 1604755687527075842, '2026-03-02 10:30:00', 0);

-- 角色菜单关联：部门管理员拿系统管理全量，运维拿只读页，只读审计员仅审计与参数，普通研发无菜单权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2037000000000003001, 1796436077724094466),
(2037000000000003001, 1796440097456967682),
(2037000000000003001, 1796447133955530753),
(2037000000000003001, 1796465429169872898),
(2037000000000003001, 1796465759832023042),
(2037000000000003001, 1955192321430597634),
(2037000000000003001, 1955194301599264769),
(2037000000000003001, 1960000000000000103),
(2037000000000003001, 1960000000000000104),
(2037000000000003001, 1960000000000000105),
(2037000000000003002, 1796436077724094466),
(2037000000000003002, 1960000000000000105),
(2037000000000003002, 1960000000000000104),
(2037000000000003003, 1796436077724094466),
(2037000000000003003, 1960000000000000104);

-- ----------------------------
-- 用户：9 个，覆盖正常 / 停用、有部门 / 无部门、有岗位 / 无岗位
-- ----------------------------
INSERT INTO `sys_user` (`id`, `tenant_id`, `account`, `password`, `nickname`, `avatar`, `email`, `phone`, `birthday`, `gender`, `address`, `is_super`, `status`, `dept_id`, `post_id`, `oauth_id`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000004001, '000000', 'zhangwei', '$2a$10$begcGjIdaTYV.Vie7DSO6epKXO9Ec.r1YpcKQAcSDwE/x0zODHNzG', '张伟', NULL, 'zhangwei@ferry.dev', '13801234567', '1992-04-18', 1, '杭州市西湖区文三路 100 号', 0, 1, '1947125325874868226', '1659421348215566337', NULL, 1604755687527075842, '2026-03-05 14:20:00', 1604755687527075842, '2026-03-05 14:20:00', 0);
INSERT INTO `sys_user` (`id`, `tenant_id`, `account`, `password`, `nickname`, `avatar`, `email`, `phone`, `birthday`, `gender`, `address`, `is_super`, `status`, `dept_id`, `post_id`, `oauth_id`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000004002, '000000', 'liyan', '$2a$10$begcGjIdaTYV.Vie7DSO6epKXO9Ec.r1YpcKQAcSDwE/x0zODHNzG', '李艳', NULL, 'liyan@ferry.dev', '13802345678', '1995-09-02', 2, '杭州市滨江区江南大道 288 号', 0, 1, '1947125427007926273', '1659421438896418817', NULL, 1604755687527075842, '2026-03-05 14:20:00', 1604755687527075842, '2026-03-05 14:20:00', 0);
INSERT INTO `sys_user` (`id`, `tenant_id`, `account`, `password`, `nickname`, `avatar`, `email`, `phone`, `birthday`, `gender`, `address`, `is_super`, `status`, `dept_id`, `post_id`, `oauth_id`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000004003, '000000', 'wangqiang', '$2a$10$begcGjIdaTYV.Vie7DSO6epKXO9Ec.r1YpcKQAcSDwE/x0zODHNzG', '王强', NULL, 'wangqiang@ferry.dev', '13803456789', '1993-11-25', 1, '杭州市余杭区文一西路 969 号', 0, 1, '1947125536223408129', '1659421438896418817', NULL, 1604755687527075842, '2026-03-05 14:20:00', 1604755687527075842, '2026-03-05 14:20:00', 0);
INSERT INTO `sys_user` (`id`, `tenant_id`, `account`, `password`, `nickname`, `avatar`, `email`, `phone`, `birthday`, `gender`, `address`, `is_super`, `status`, `dept_id`, `post_id`, `oauth_id`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000004004, '000000', 'chenjing', '$2a$10$begcGjIdaTYV.Vie7DSO6epKXO9Ec.r1YpcKQAcSDwE/x0zODHNzG', '陈静', NULL, 'chenjing@ferry.dev', '13804567890', '1990-06-30', 2, '杭州市拱墅区莫干山路 18 号', 0, 1, '1947124125297291265', '2037000000000002003', NULL, 1604755687527075842, '2026-03-05 14:20:00', 1604755687527075842, '2026-03-05 14:20:00', 0);
INSERT INTO `sys_user` (`id`, `tenant_id`, `account`, `password`, `nickname`, `avatar`, `email`, `phone`, `birthday`, `gender`, `address`, `is_super`, `status`, `dept_id`, `post_id`, `oauth_id`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000004005, '000000', 'zhaolei', '$2a$10$begcGjIdaTYV.Vie7DSO6epKXO9Ec.r1YpcKQAcSDwE/x0zODHNzG', '赵磊', NULL, 'zhaolei@ferry.dev', '13805678901', '1991-02-14', 1, '杭州市上城区解放路 66 号', 0, 1, '1947125741438119938', '2037000000000002004', NULL, 1604755687527075842, '2026-03-05 14:20:00', 1604755687527075842, '2026-03-05 14:20:00', 0);
INSERT INTO `sys_user` (`id`, `tenant_id`, `account`, `password`, `nickname`, `avatar`, `email`, `phone`, `birthday`, `gender`, `address`, `is_super`, `status`, `dept_id`, `post_id`, `oauth_id`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000004006, '000000', 'sunli', '$2a$10$begcGjIdaTYV.Vie7DSO6epKXO9Ec.r1YpcKQAcSDwE/x0zODHNzG', '孙丽', NULL, 'sunli@ferry.dev', '13806789012', '1994-07-08', 2, '杭州市西湖区教工路 21 号', 0, 1, '1947125868479393793', '2037000000000002001', NULL, 1604755687527075842, '2026-03-05 14:20:00', 1604755687527075842, '2026-03-05 14:20:00', 0);
INSERT INTO `sys_user` (`id`, `tenant_id`, `account`, `password`, `nickname`, `avatar`, `email`, `phone`, `birthday`, `gender`, `address`, `is_super`, `status`, `dept_id`, `post_id`, `oauth_id`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000004007, '000000', 'zhoumin', '$2a$10$begcGjIdaTYV.Vie7DSO6epKXO9Ec.r1YpcKQAcSDwE/x0zODHNzG', '周敏', NULL, 'zhoumin@ferry.dev', '13807890123', '1989-12-03', 2, '杭州市钱塘区下沙路 88 号', 0, 1, '1651886721766735873', '2037000000000002003', NULL, 1604755687527075842, '2026-03-05 14:20:00', 1604755687527075842, '2026-03-05 14:20:00', 0);
INSERT INTO `sys_user` (`id`, `tenant_id`, `account`, `password`, `nickname`, `avatar`, `email`, `phone`, `birthday`, `gender`, `address`, `is_super`, `status`, `dept_id`, `post_id`, `oauth_id`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000004008, '000000', 'wujun', '$2a$10$begcGjIdaTYV.Vie7DSO6epKXO9Ec.r1YpcKQAcSDwE/x0zODHNzG', '吴俊', NULL, 'wujun@ferry.dev', '13808901234', '1996-05-21', 1, '杭州市萧山区市心路 5 号', 0, 0, '1947122268785094658', NULL, NULL, 1604755687527075842, '2026-03-05 14:20:00', 1604755687527075842, '2026-03-05 14:20:00', 0);
INSERT INTO `sys_user` (`id`, `tenant_id`, `account`, `password`, `nickname`, `avatar`, `email`, `phone`, `birthday`, `gender`, `address`, `is_super`, `status`, `dept_id`, `post_id`, `oauth_id`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000004009, '000000', 'xuting', '$2a$10$begcGjIdaTYV.Vie7DSO6epKXO9Ec.r1YpcKQAcSDwE/x0zODHNzG', '徐婷', NULL, 'xuting@ferry.dev', '13809012345', '1997-08-16', 2, '杭州市临平区南苑街道 12 号', 0, 1, NULL, NULL, NULL, 1604755687527075842, '2026-03-05 14:20:00', 1604755687527075842, '2026-03-05 14:20:00', 0);

-- 用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(2037000000000004001, 2037000000000003004),
(2037000000000004002, 2037000000000003004),
(2037000000000004003, 2037000000000003004),
(2037000000000004004, 2037000000000003001),
(2037000000000004005, 2037000000000003002),
(2037000000000004006, 2037000000000003002),
(2037000000000004007, 2037000000000003003),
(2037000000000004008, 2037000000000003004);

-- ----------------------------
-- 字典：补齐部署类型、发布状态、门禁结论、环境等级四组
-- ----------------------------
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005000, '000000', 0, 'DEPLOY_TYPE', '部署类型', NULL, 1, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005100, '000000', 2037000000000005000, 'DEPLOY_TYPE', 'SSH 部署', '1', 1, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005101, '000000', 2037000000000005000, 'DEPLOY_TYPE', 'Docker 部署', '2', 2, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005102, '000000', 2037000000000005000, 'DEPLOY_TYPE', 'Kubernetes 部署', '3', 3, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005001, '000000', 0, 'RELEASE_STATE', '发布状态', NULL, 2, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005103, '000000', 2037000000000005001, 'RELEASE_STATE', '待发布', '0', 1, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005104, '000000', 2037000000000005001, 'RELEASE_STATE', '发布中', '1', 2, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005105, '000000', 2037000000000005001, 'RELEASE_STATE', '发布成功', '2', 3, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005106, '000000', 2037000000000005001, 'RELEASE_STATE', '发布失败', '3', 4, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005107, '000000', 2037000000000005001, 'RELEASE_STATE', '已回滚', '4', 5, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005002, '000000', 0, 'GATE_RESULT', '门禁结论', NULL, 3, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005108, '000000', 2037000000000005002, 'GATE_RESULT', '通过', '1', 1, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005109, '000000', 2037000000000005002, 'GATE_RESULT', '警告', '2', 2, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005110, '000000', 2037000000000005002, 'GATE_RESULT', '拦截', '3', 3, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005003, '000000', 0, 'ENV_LEVEL', '环境等级', NULL, 4, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005111, '000000', 2037000000000005003, 'ENV_LEVEL', '开发', '1', 1, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005112, '000000', 2037000000000005003, 'ENV_LEVEL', '测试', '2', 2, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005113, '000000', 2037000000000005003, 'ENV_LEVEL', '预发', '3', 3, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);
INSERT INTO `sys_dict` (`id`, `tenant_id`, `parent_id`, `code`, `label`, `value`, `sort`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000005114, '000000', 2037000000000005003, 'ENV_LEVEL', '生产', '4', 4, 1604755687527075842, '2026-03-10 11:00:00', 1604755687527075842, '2026-03-10 11:00:00', 0);

-- ----------------------------
-- 参数：登录策略、留存周期、门禁阈值
-- ----------------------------
INSERT INTO `sys_config` (`id`, `tenant_id`, `name`, `code`, `value`, `is_internal`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000006001, '000000', '登录失败锁定次数', 'SYS_LOGIN_FAIL_LIMIT', '5', 1, 1604755687527075842, '2026-03-12 09:40:00', 1604755687527075842, '2026-03-12 09:40:00', 0);
INSERT INTO `sys_config` (`id`, `tenant_id`, `name`, `code`, `value`, `is_internal`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000006002, '000000', '登录失败锁定时长（分钟）', 'SYS_LOGIN_LOCK_MINUTES', '30', 1, 1604755687527075842, '2026-03-12 09:40:00', 1604755687527075842, '2026-03-12 09:40:00', 0);
INSERT INTO `sys_config` (`id`, `tenant_id`, `name`, `code`, `value`, `is_internal`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000006003, '000000', '令牌有效期（分钟）', 'SYS_TOKEN_EXPIRE_MINUTES', '120', 1, 1604755687527075842, '2026-03-12 09:40:00', 1604755687527075842, '2026-03-12 09:40:00', 0);
INSERT INTO `sys_config` (`id`, `tenant_id`, `name`, `code`, `value`, `is_internal`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000006004, '000000', '审计日志留存天数', 'SYS_AUDIT_RETAIN_DAYS', '180', 1, 1604755687527075842, '2026-03-12 09:40:00', 1604755687527075842, '2026-03-12 09:40:00', 0);
INSERT INTO `sys_config` (`id`, `tenant_id`, `name`, `code`, `value`, `is_internal`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000006005, '000000', '发布门禁覆盖率阈值', 'GATE_COVERAGE_THRESHOLD', '80', 0, 1604755687527075842, '2026-03-12 09:40:00', 1604755687527075842, '2026-03-12 09:40:00', 0);
INSERT INTO `sys_config` (`id`, `tenant_id`, `name`, `code`, `value`, `is_internal`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000006006, '000000', '单次发布最长时长（分钟）', 'RELEASE_TIMEOUT_MINUTES', '45', 0, 1604755687527075842, '2026-03-12 09:40:00', 1604755687527075842, '2026-03-12 09:40:00', 0);
INSERT INTO `sys_config` (`id`, `tenant_id`, `name`, `code`, `value`, `is_internal`, `create_user`, `create_time`, `update_user`, `update_time`, `is_deleted`) VALUES (2037000000000006007, '000000', '生产操作二次确认人角色', 'PROD_CONFIRM_ROLE', 'dept_admin', 0, 1604755687527075842, '2026-03-12 09:40:00', 1604755687527075842, '2026-03-12 09:40:00', 0);
