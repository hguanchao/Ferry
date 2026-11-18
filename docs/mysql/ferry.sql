-- 表结构以本文件为唯一权威口径：结构变更直接修改这里的全量建表脚本
-- 本文件含 DROP TABLE，切勿在存量库执行，存量库按交付说明执行增量 ALTER
/*
 Source Server Type    : MySQL
 Source Server Version : 8.4.0
 Source Schema         : ferry
*/

-- 本文件只放建表语句，初始与演示数据见 ferry_data.sql，区域数据见 ferry_region_data.sql
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置名称',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键名',
  `value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置键值',
  `is_internal` tinyint NULL DEFAULT 0 COMMENT '是否系统内置 0：否 1：是',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT NULL COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父部门id',
  `ancestors` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '祖级列表',
  `code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门编码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '部门状态 0：禁用 1：正常',
  `sort` int NULL DEFAULT 1 COMMENT '显示顺序',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父字典id',
  `code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典码',
  `label` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典标签',
  `value` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '字典值',
  `sort` int NULL DEFAULT 1 COMMENT '显示顺序',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父菜单id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `type` tinyint(1) NOT NULL COMMENT '菜单类型 0：目录 1：菜单 2：按钮',
  `icon` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路由地址',
  `component` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限标识',
  `is_show` tinyint(1) NULL DEFAULT 1 COMMENT '是否显示 0：隐藏 1：显示',
  `is_frame` tinyint(1) NULL DEFAULT 0 COMMENT '是否为外链 0：否 1：是',
  `is_cache` tinyint(1) NULL DEFAULT 1 COMMENT '是否缓存 0：不缓存 1：缓存',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '菜单状态 0：停用 1：正常',
  `sort` int NULL DEFAULT 1 COMMENT '显示顺序',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单权限表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '岗位状态 0：停用 1：正常',
  `sort` int NULL DEFAULT 1 COMMENT '显示顺序',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_region
-- ----------------------------
DROP TABLE IF EXISTS `sys_region`;
CREATE TABLE `sys_region`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父级区域ID',
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '区域编码',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '区域名称',
  `sort` int NULL DEFAULT 1 COMMENT '排序',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态 0：禁用 1：正常',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  -- 区域列表按父级逐级懒加载（parent_id + 排序）与按编码前缀取子树（code LIKE），两者都需要索引
  INDEX `idx_region_parent`(`parent_id`, `sort`) USING BTREE,
  INDEX `idx_region_code`(`code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '区域表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '角色状态 0：停用 1：正常',
  `data_scope` tinyint NULL DEFAULT 1 COMMENT '数据范围 1：全部 2：自定义 3：本部门 4：本部门及以下 5：仅本人',
  `sort` int NULL DEFAULT 1 COMMENT '显示顺序',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职责描述',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint NOT NULL COMMENT '角色id',
  `menu_id` bigint NOT NULL COMMENT '菜单id'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色和菜单关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
  `role_id` bigint NOT NULL COMMENT '角色id',
  `dept_id` bigint NOT NULL COMMENT '部门id',
  PRIMARY KEY (`role_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色和部门关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户名称',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '租户状态 0：禁用 1：正常',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `account` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号',
  `password` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户密码',
  `nickname` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像',
  `email` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号码',
  `birthday` date NULL DEFAULT NULL COMMENT '生日',
  `gender` tinyint(1) NULL DEFAULT NULL COMMENT '性别 1：男 2：女',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `is_super` tinyint NULL DEFAULT 0 COMMENT '超级管理员 0：否 1：是',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '账号状态 0：禁用 1：正常',
  `dept_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属部门id',
  `post_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '在职岗位id',
  `oauth_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方认证id',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint NOT NULL COMMENT '用户id',
  `role_id` bigint NOT NULL COMMENT '角色id'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户和角色关联表' ROW_FORMAT = DYNAMIC;


-- ==================== 交付域 devops（dev_* 共 63 张表） ====================

-- ----------------------------
-- Table structure for dev_app
-- ----------------------------
DROP TABLE IF EXISTS `dev_app`;
CREATE TABLE `dev_app`  (
  `id` bigint NOT NULL COMMENT '主键',
  `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '应用编码，小写字母/数字/中划线，创建后不可改',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '应用名称',
  `team_id` bigint NOT NULL COMMENT '所属团队，对应 sys_dept.id',
  `is_core` tinyint(1) NOT NULL DEFAULT 0 COMMENT '核心链路 0 否 1 是',
  `platform` tinyint NOT NULL COMMENT '代码平台 1 GitLab 2 GitHub 3 Gitee',
  `default_branch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '默认分支',
  `default_commit_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '默认提交',
  `storage_bucket` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '对象存储 bucket',
  `storage_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '对象存储路径前缀',
  `registry_repo` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'OCI Registry 仓库名',
  `monitor_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '监控系统类型',
  `monitor_dashboard` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '监控大盘地址',
  `alert_group` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '告警规则组',
  `oncall_window` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '值班时段',
  `setup_status` tinyint NOT NULL DEFAULT 1 COMMENT '接入完善度 1 待完善 2 已完善',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '描述',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_app_code` (`code`) USING BTREE,
  KEY `idx_app_team` (`team_id`) USING BTREE,
  KEY `idx_app_name` (`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '应用档案表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_app_repo
-- ----------------------------
DROP TABLE IF EXISTS `dev_app_repo`;
CREATE TABLE `dev_app_repo`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `platform` tinyint NOT NULL COMMENT '平台枚举，与 dev_app.platform 一致',
  `repo_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'group/project 路径',
  `repo_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '完整 Git 地址',
  `credential_id` bigint NULL COMMENT '凭据id',
  `is_main` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否主仓库',
  `default_branch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '该仓库默认分支',
  `is_public` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否公开仓库',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_app_repo_app` (`app_id`) USING BTREE,
  KEY `idx_app_repo_main` (`app_id`,`is_main`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '应用代码源表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_app_owner
-- ----------------------------
DROP TABLE IF EXISTS `dev_app_owner`;
CREATE TABLE `dev_app_owner`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `type` tinyint NOT NULL COMMENT '1 负责人 2 备份负责人',
  `sort` int NOT NULL DEFAULT 0 COMMENT '顺序',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_app_owner_app` (`app_id`,`type`,`sort`) USING BTREE,
  UNIQUE KEY `uk_app_owner` (`app_id`,`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '应用负责人表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_app_dependency
-- ----------------------------
DROP TABLE IF EXISTS `dev_app_dependency`;
CREATE TABLE `dev_app_dependency`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `dep_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '依赖服务名',
  `dep_app_id` bigint NULL COMMENT '关联应用id，可空',
  `version_range` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '版本范围',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_app_dep_app` (`app_id`) USING BTREE,
  KEY `idx_app_dep_dep` (`dep_app_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '依赖服务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_app_core_flag_apply
-- ----------------------------
DROP TABLE IF EXISTS `dev_app_core_flag_apply`;
CREATE TABLE `dev_app_core_flag_apply`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `is_target_core` tinyint(1) NOT NULL COMMENT '申请的目标值，0 关闭 1 开启',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请理由',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 待审批 2 已通过 3 已驳回',
  `applicant` bigint NOT NULL COMMENT '申请人',
  `approver` bigint NULL COMMENT '审批人',
  `approve_time` datetime(3) NULL COMMENT '审批时间',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_core_apply_app` (`app_id`,`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '核心链路变更申请表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_pipeline
-- ----------------------------
DROP TABLE IF EXISTS `dev_pipeline`;
CREATE TABLE `dev_pipeline`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流水线名称',
  `is_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '启用 1 是 0 否',
  `disabled_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '停用原因',
  `current_version_id` bigint NULL COMMENT '当前版本id',
  `owner_id` bigint NULL COMMENT '负责人',
  `last_run_id` bigint NULL COMMENT '最近运行id',
  `last_run_status` tinyint NULL COMMENT '最近运行状态冗余',
  `last_run_time` datetime(3) NULL COMMENT '最近运行时间',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_pipeline_app` (`app_id`) USING BTREE,
  KEY `idx_pipeline_name` (`name`) USING BTREE,
  KEY `idx_pipeline_last` (`last_run_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流水线表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_pipeline_version
-- ----------------------------
DROP TABLE IF EXISTS `dev_pipeline_version`;
CREATE TABLE `dev_pipeline_version`  (
  `id` bigint NOT NULL COMMENT '主键',
  `pipeline_id` bigint NOT NULL COMMENT '流水线id',
  `version_no` int NOT NULL COMMENT '自增版本号',
  `content_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '结构化定义',
  `content_yaml` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'YAML 原文',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内容 sha256',
  `change_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '变更说明',
  `source_version_id` bigint NULL COMMENT '回退来源版本',
  `source_type` tinyint NOT NULL DEFAULT 1 COMMENT '1 表单 2 YAML 3 回退 4 模板合并',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_pipeline_version` (`pipeline_id`,`version_no`) USING BTREE,
  KEY `idx_pipeline_version_hash` (`content_hash`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流水线版本快照表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_pipeline_template
-- ----------------------------
DROP TABLE IF EXISTS `dev_pipeline_template`;
CREATE TABLE `dev_pipeline_template`  (
  `id` bigint NOT NULL COMMENT '主键',
  `scope` tinyint NOT NULL COMMENT '1 组织级 2 团队级',
  `team_id` bigint NULL COMMENT '团队id，团队级必填',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `scene` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '适用场景',
  `is_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '启停',
  `current_version_id` bigint NULL COMMENT '当前版本',
  `derived_count` int NOT NULL DEFAULT 0 COMMENT '派生数',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_template_scope` (`scope`,`team_id`) USING BTREE,
  KEY `idx_template_name` (`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流水线模板表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_pipeline_template_version
-- ----------------------------
DROP TABLE IF EXISTS `dev_pipeline_template_version`;
CREATE TABLE `dev_pipeline_template_version`  (
  `id` bigint NOT NULL COMMENT '主键',
  `template_id` bigint NOT NULL COMMENT '模板id',
  `version_no` int NOT NULL COMMENT '版本号',
  `content_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '定义',
  `content_yaml` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'YAML',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '摘要',
  `change_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '变更说明',
  `source_version_id` bigint NULL COMMENT '回退来源',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_template_version` (`template_id`,`version_no`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模板版本表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_pipeline_template_var
-- ----------------------------
DROP TABLE IF EXISTS `dev_pipeline_template_var`;
CREATE TABLE `dev_pipeline_template_var`  (
  `id` bigint NOT NULL COMMENT '主键',
  `template_version_id` bigint NOT NULL COMMENT '模板版本id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '变量名',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '说明',
  `default_value` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '默认值',
  `is_required` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否必填',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_template_var` (`template_version_id`,`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模板占位变量表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_pipeline_template_derivation
-- ----------------------------
DROP TABLE IF EXISTS `dev_pipeline_template_derivation`;
CREATE TABLE `dev_pipeline_template_derivation`  (
  `id` bigint NOT NULL COMMENT '主键',
  `template_id` bigint NOT NULL COMMENT '模板id',
  `template_version_id` bigint NOT NULL COMMENT '派生时版本',
  `pipeline_id` bigint NOT NULL COMMENT '派生出的流水线',
  `merged_version_id` bigint NULL COMMENT '最近合并后的流水线版本',
  `is_behind` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否落后于模板',
  `last_merge_time` datetime(3) NULL COMMENT '最近合并时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_derivation_pipeline` (`pipeline_id`) USING BTREE,
  KEY `idx_derivation_template` (`template_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '派生关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_run
-- ----------------------------
DROP TABLE IF EXISTS `dev_run`;
CREATE TABLE `dev_run`  (
  `id` bigint NOT NULL COMMENT '主键',
  `run_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '运行编号 RUN-yyyyMMdd-序号',
  `pipeline_id` bigint NOT NULL COMMENT '流水线id',
  `pipeline_version_id` bigint NOT NULL COMMENT '版本id',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `branch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分支',
  `commit_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提交',
  `ref_type` tinyint NOT NULL DEFAULT 1 COMMENT '1 Commit 2 Tag/Ref',
  `ref_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始引用值',
  `trigger_type` tinyint NOT NULL DEFAULT 1 COMMENT '1 手动（历史值保留）',
  `trigger_user` bigint NOT NULL COMMENT '触发人',
  `trigger_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '触发说明',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 排队中 2 运行中 3 阻塞 4 成功 5 失败 6 超时 7 已取消 8 结果待确认',
  `block_reason` tinyint NULL COMMENT '1 门禁不通过 2 门禁数据获取失败 3 人工暂停',
  `is_precheck_pass` tinyint(1) NOT NULL DEFAULT 0 COMMENT '预校验是否通过',
  `idempotent_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '幂等键',
  `rerun_from_run_id` bigint NULL COMMENT '重跑自',
  `reused_run_id` bigint NULL COMMENT '复用自',
  `artifact_id` bigint NULL COMMENT '产出制品',
  `jenkins_build_no` bigint NULL COMMENT 'Jenkins 构建号',
  `result_expire_time` datetime(3) NULL COMMENT '结果待确认截止时间',
  `start_time` datetime(3) NULL COMMENT '开始时间',
  `end_time` datetime(3) NULL COMMENT '结束时间',
  `duration` bigint NULL COMMENT '耗时毫秒',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_run_no` (`run_no`) USING BTREE,
  KEY `idx_run_pipeline` (`pipeline_id`,`start_time`) USING BTREE,
  KEY `idx_run_app_status` (`app_id`,`status`) USING BTREE,
  KEY `idx_run_job` (`jenkins_build_no`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '运行表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_run_stage
-- ----------------------------
DROP TABLE IF EXISTS `dev_run_stage`;
CREATE TABLE `dev_run_stage`  (
  `id` bigint NOT NULL COMMENT '主键',
  `run_id` bigint NOT NULL COMMENT '运行id',
  `stage_type` tinyint NOT NULL COMMENT '1–8',
  `stage_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '阶段名',
  `seq` int NOT NULL COMMENT '顺序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1–9',
  `block_reason` tinyint NULL COMMENT '阻塞原因',
  `reused_from_stage_id` bigint NULL COMMENT '复用来源阶段',
  `jenkins_job_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'Jenkins Job 标识',
  `start_time` datetime(3) NULL COMMENT '开始时间',
  `end_time` datetime(3) NULL COMMENT '结束时间',
  `duration` bigint NULL COMMENT '耗时毫秒',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_stage_run` (`run_id`,`seq`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '运行阶段表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_run_task
-- ----------------------------
DROP TABLE IF EXISTS `dev_run_task`;
CREATE TABLE `dev_run_task`  (
  `id` bigint NOT NULL COMMENT '主键',
  `run_id` bigint NOT NULL COMMENT '运行id',
  `stage_id` bigint NOT NULL COMMENT '阶段id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名',
  `executor` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '执行器',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `exit_code` int NULL COMMENT '退出码',
  `duration` bigint NULL COMMENT '耗时',
  `log_line_count` int NOT NULL DEFAULT 0 COMMENT '日志行数',
  `is_truncated` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否截断',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_task_stage` (`stage_id`) USING BTREE,
  KEY `idx_task_run` (`run_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '运行任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_run_log_segment
-- ----------------------------
DROP TABLE IF EXISTS `dev_run_log_segment`;
CREATE TABLE `dev_run_log_segment`  (
  `id` bigint NOT NULL COMMENT '主键',
  `run_id` bigint NOT NULL COMMENT '运行id',
  `task_id` bigint NOT NULL COMMENT '任务id',
  `segment_no` int NOT NULL COMMENT '段号',
  `start_line` int NOT NULL COMMENT '起始行号',
  `end_line` int NOT NULL COMMENT '结束行号',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '打码后的日志',
  `is_truncated` tinyint(1) NOT NULL DEFAULT 0 COMMENT '段是否被截断',
  `create_time` datetime(3) NOT NULL COMMENT '入库时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_log_task` (`task_id`,`segment_no`) USING BTREE,
  KEY `idx_log_run` (`run_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '日志分段表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_run_precheck
-- ----------------------------
DROP TABLE IF EXISTS `dev_run_precheck`;
CREATE TABLE `dev_run_precheck`  (
  `id` bigint NOT NULL COMMENT '主键',
  `run_id` bigint NOT NULL COMMENT '运行id',
  `item_type` tinyint NOT NULL COMMENT '1 仓库可达 2 分支存在 3 提交可达 4 定义匹配 5 幂等检查',
  `conclusion` tinyint NOT NULL COMMENT '1 通过 2 不通过 3 合并 4 复用',
  `detail` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '明细',
  `checked_at` datetime(3) NOT NULL COMMENT '校验时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_precheck_run` (`run_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '触发预校验表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_run_trigger
-- ----------------------------
DROP TABLE IF EXISTS `dev_run_trigger`;
CREATE TABLE `dev_run_trigger`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `pipeline_id` bigint NOT NULL COMMENT '流水线id',
  `branch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分支',
  `commit_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提交',
  `trigger_user` bigint NOT NULL COMMENT '触发人',
  `result` tinyint NOT NULL COMMENT '1 已创建 2 已合并 3 已复用 4 已拒绝',
  `run_id` bigint NULL COMMENT '运行id',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '拒绝原因',
  `create_time` datetime(3) NOT NULL COMMENT '触发时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_trigger_app` (`app_id`,`create_time`) USING BTREE,
  KEY `idx_trigger_user` (`trigger_user`,`create_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '触发记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_run_operation
-- ----------------------------
DROP TABLE IF EXISTS `dev_run_operation`;
CREATE TABLE `dev_run_operation`  (
  `id` bigint NOT NULL COMMENT '主键',
  `run_id` bigint NOT NULL COMMENT '运行id',
  `type` tinyint NOT NULL COMMENT '1 暂停 2 继续 3 中止',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '原因',
  `operator` bigint NOT NULL COMMENT '操作人',
  `create_time` datetime(3) NOT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_run_op` (`run_id`,`create_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '运行干预记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_run_idempotency
-- ----------------------------
DROP TABLE IF EXISTS `dev_run_idempotency`;
CREATE TABLE `dev_run_idempotency`  (
  `id` bigint NOT NULL COMMENT '主键',
  `idempotent_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'sha256',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `run_id` bigint NOT NULL COMMENT '运行id',
  `state` tinyint NOT NULL DEFAULT 1 COMMENT '1 活动中 2 已成功 3 已失效',
  `expire_time` datetime(3) NULL COMMENT '失效时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_idem_key` (`idempotent_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '触发幂等表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_gate_rule
-- ----------------------------
DROP TABLE IF EXISTS `dev_gate_rule`;
CREATE TABLE `dev_gate_rule`  (
  `id` bigint NOT NULL COMMENT '主键',
  `scope_type` tinyint NOT NULL COMMENT '1 组织 2 应用 3 分支',
  `app_id` bigint NULL COMMENT '应用id，应用/分支级必填',
  `branch_pattern` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '分支通配，仅分支级',
  `rule_type` tinyint NOT NULL COMMENT '1 单测通过率 2 行覆盖率 3 分支覆盖率 4 阻断漏洞 5 高危依赖 6 许可证黑名单',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则名',
  `threshold` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '阈值',
  `compare_direction` tinyint NOT NULL COMMENT '1 不低于 2 不高于',
  `stage_types` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '作用阶段，逗号分隔',
  `is_exempt_allowed` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否允许豁免',
  `is_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '启停',
  `version` int NOT NULL DEFAULT 1 COMMENT '规则版本，每次修改 +1',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_gate_rule_scope` (`scope_type`,`app_id`,`is_enabled`) USING BTREE,
  KEY `idx_gate_rule_type` (`rule_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '门禁规则表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_gate_result
-- ----------------------------
DROP TABLE IF EXISTS `dev_gate_result`;
CREATE TABLE `dev_gate_result`  (
  `id` bigint NOT NULL COMMENT '主键',
  `run_id` bigint NOT NULL COMMENT '运行id',
  `stage_id` bigint NOT NULL COMMENT '阶段id',
  `rule_id` bigint NULL COMMENT '规则id',
  `rule_snapshot` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则快照',
  `current_value` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '当前值',
  `threshold` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '阈值',
  `conclusion` tinyint NOT NULL COMMENT '1 通过 2 不通过 3 不适用 4 已豁免 5 数据获取失败',
  `evidence` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '证据链接',
  `source` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '报告来源',
  `schema_version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'schema 版本',
  `fetch_error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '获取失败原因',
  `create_time` datetime(3) NOT NULL COMMENT '判定时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_gate_result_run` (`run_id`,`conclusion`) USING BTREE,
  KEY `idx_gate_result_rule` (`rule_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '门禁结果表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_gate_exemption
-- ----------------------------
DROP TABLE IF EXISTS `dev_gate_exemption`;
CREATE TABLE `dev_gate_exemption`  (
  `id` bigint NOT NULL COMMENT '主键',
  `gate_result_id` bigint NOT NULL COMMENT '结果id',
  `run_id` bigint NOT NULL COMMENT '运行id',
  `rule_id` bigint NOT NULL COMMENT '规则id',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '理由，不少于 20 字',
  `expire_at` datetime(3) NOT NULL COMMENT '到期时间',
  `issue_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '关联问题单',
  `applicant` bigint NOT NULL COMMENT '申请人',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 生效 2 已到期 3 已撤销',
  `create_time` datetime(3) NOT NULL COMMENT '申请时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_exempt_result` (`gate_result_id`) USING BTREE,
  KEY `idx_exempt_expire` (`status`,`expire_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '门禁豁免表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_gate_baseline
-- ----------------------------
DROP TABLE IF EXISTS `dev_gate_baseline`;
CREATE TABLE `dev_gate_baseline`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `tool` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '扫描工具',
  `rule_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '规则版本',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '问题总数',
  `severity_stats` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '各级别数量 JSON',
  `fingerprint_version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '指纹算法版本',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否生效',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_baseline_app` (`app_id`,`is_active`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '存量基线表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_gate_baseline_issue
-- ----------------------------
DROP TABLE IF EXISTS `dev_gate_baseline_issue`;
CREATE TABLE `dev_gate_baseline_issue`  (
  `id` bigint NOT NULL COMMENT '主键',
  `baseline_id` bigint NOT NULL COMMENT '基线id',
  `fingerprint` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题指纹',
  `rule_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则',
  `severity` tinyint NOT NULL COMMENT '1 阻断 2 高危 3 中危 4 低危',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文件路径',
  `snippet_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '片段哈希',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 遗留 2 已修复 3 误报',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_baseline_issue` (`baseline_id`,`fingerprint`) USING BTREE,
  KEY `idx_baseline_issue_sev` (`severity`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '基线问题表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_gate_baseline_reset
-- ----------------------------
DROP TABLE IF EXISTS `dev_gate_baseline_reset`;
CREATE TABLE `dev_gate_baseline_reset`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `old_baseline_id` bigint NULL COMMENT '原基线',
  `new_baseline_id` bigint NULL COMMENT '新基线',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '理由',
  `approver` bigint NULL COMMENT '审批人',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 待审批 2 已通过 3 已驳回',
  `create_time` datetime(3) NOT NULL COMMENT '申请时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_baseline_reset_app` (`app_id`,`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '基线重设表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_issue
-- ----------------------------
DROP TABLE IF EXISTS `dev_issue`;
CREATE TABLE `dev_issue`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `fingerprint` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '指纹',
  `severity` tinyint NOT NULL COMMENT '级别',
  `rule_type` tinyint NULL COMMENT '规则类型',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文件路径',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 新增 2 遗留 3 已修复 4 误报',
  `assignee` bigint NULL COMMENT '修复责任人',
  `first_seen` datetime(3) NOT NULL COMMENT '首次出现',
  `last_seen` datetime(3) NULL COMMENT '最近出现',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_issue_fingerprint` (`app_id`,`fingerprint`) USING BTREE,
  KEY `idx_issue_status` (`app_id`,`status`,`severity`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '存量问题清单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_artifact
-- ----------------------------
DROP TABLE IF EXISTS `dev_artifact`;
CREATE TABLE `dev_artifact`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `version` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '版本号',
  `form` tinyint NOT NULL COMMENT '1 包制品 2 镜像制品',
  `digest` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'sha256 摘要',
  `size` bigint NOT NULL COMMENT '字节数',
  `run_id` bigint NOT NULL COMMENT '产出运行',
  `build_no` bigint NULL COMMENT 'Jenkins 构建号',
  `build_image` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '构建镜像',
  `branch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '分支',
  `commit_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '提交',
  `builder` bigint NULL COMMENT '构建人',
  `gate_conclusion` tinyint NULL COMMENT '汇总结论',
  `prod_use_status` tinyint NOT NULL DEFAULT 3 COMMENT '1 当前生效 2 历史版本 3 从未使用',
  `confirm_status` tinyint NOT NULL DEFAULT 1 COMMENT '1 正常 2 待确认',
  `delete_seq` bigint NOT NULL DEFAULT 0 COMMENT '删除序号，0 表示未删',
  `deleted_at` datetime(3) NULL COMMENT '软删除时间',
  `delete_source` tinyint NULL COMMENT '1 人工 2 清理任务',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_artifact_version` (`app_id`,`version`,`delete_seq`) USING BTREE,
  UNIQUE KEY `uk_artifact_digest` (`digest`,`delete_seq`) USING BTREE,
  KEY `idx_artifact_app_time` (`app_id`,`create_time`) USING BTREE,
  KEY `idx_artifact_run` (`run_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '制品元数据表（delete_seq 代替逻辑删除）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_artifact_env
-- ----------------------------
DROP TABLE IF EXISTS `dev_artifact_env`;
CREATE TABLE `dev_artifact_env`  (
  `id` bigint NOT NULL COMMENT '主键',
  `artifact_id` bigint NOT NULL COMMENT '制品id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `env_category` tinyint NOT NULL COMMENT '环境类别快照',
  `source_env_id` bigint NULL COMMENT '晋级来源环境',
  `promoter` bigint NULL COMMENT '晋级人',
  `promoted_at` datetime(3) NULL COMMENT '晋级时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_artifact_env` (`artifact_id`,`env_id`) USING BTREE,
  KEY `idx_artifact_env_env` (`env_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '制品环境标记表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_artifact_clean_policy
-- ----------------------------
DROP TABLE IF EXISTS `dev_artifact_clean_policy`;
CREATE TABLE `dev_artifact_clean_policy`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id，全局策略写 0',
  `keep_latest` int NOT NULL DEFAULT 20 COMMENT '保留最近 N 个',
  `is_keep_prod` tinyint(1) NOT NULL DEFAULT 1 COMMENT '永久保留生产用过的',
  `soft_delete_days` int NOT NULL DEFAULT 30 COMMENT '软删除保留天数',
  `cron` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '执行时间',
  `is_auto_exec` tinyint(1) NOT NULL DEFAULT 0 COMMENT '自动执行',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_clean_policy_app` (`app_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '制品清理策略表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_artifact_clean_task
-- ----------------------------
DROP TABLE IF EXISTS `dev_artifact_clean_task`;
CREATE TABLE `dev_artifact_clean_task`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `task_time` datetime(3) NOT NULL COMMENT '任务时间',
  `pending_count` int NOT NULL DEFAULT 0 COMMENT '待清理数',
  `deleted_count` int NOT NULL DEFAULT 0 COMMENT '实际清理数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 待确认 2 已执行 3 失败',
  `manifest_file` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '清单文件地址',
  `operator` bigint NULL COMMENT '执行人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_clean_task_app` (`app_id`,`task_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '清理任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_artifact_clean_item
-- ----------------------------
DROP TABLE IF EXISTS `dev_artifact_clean_item`;
CREATE TABLE `dev_artifact_clean_item`  (
  `id` bigint NOT NULL COMMENT '主键',
  `task_id` bigint NOT NULL COMMENT '任务id',
  `artifact_id` bigint NOT NULL COMMENT '制品id',
  `version` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '版本快照',
  `digest` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '摘要快照',
  `size` bigint NOT NULL COMMENT '大小',
  `keep_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '保留原因',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_clean_item_task` (`task_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '清理明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_artifact_compensation
-- ----------------------------
DROP TABLE IF EXISTS `dev_artifact_compensation`;
CREATE TABLE `dev_artifact_compensation`  (
  `id` bigint NOT NULL COMMENT '主键',
  `artifact_id` bigint NOT NULL COMMENT '制品id',
  `run_id` bigint NOT NULL COMMENT '运行id',
  `scene` tinyint NOT NULL COMMENT '1 push 成功写表失败 2 写表成功 push 失败',
  `attempt` int NOT NULL DEFAULT 0 COMMENT '尝试次数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 处理中 2 成功 3 失败',
  `last_error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '最近错误',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_comp_artifact` (`artifact_id`,`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '制品补偿表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_environment
-- ----------------------------
DROP TABLE IF EXISTS `dev_environment`;
CREATE TABLE `dev_environment`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '环境名',
  `category` tinyint NOT NULL COMMENT '1 测试 2 预发 3 生产',
  `deploy_type` tinyint NOT NULL COMMENT '1 SSH 2 Docker 3 Kubernetes',
  `is_protected` tinyint(1) NOT NULL DEFAULT 0 COMMENT '保护标记，生产自动置 1',
  `target_config` json CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标资源，结构见 5.8',
  `health_config` json CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '健康检查配置',
  `observe_minutes` int NULL COMMENT '观察时长分钟',
  `config_version` int NOT NULL DEFAULT 0 COMMENT '配置版本',
  `current_artifact_id` bigint NULL COMMENT '当前生效制品',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 正常 2 异常',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_env_name` (`app_id`,`name`,`is_deleted`) USING BTREE,
  KEY `idx_env_app` (`app_id`) USING BTREE,
  KEY `idx_env_category` (`category`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '环境表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_env_config
-- ----------------------------
DROP TABLE IF EXISTS `dev_env_config`;
CREATE TABLE `dev_env_config`  (
  `id` bigint NOT NULL COMMENT '主键',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `config_key` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '配置值',
  `value_type` tinyint NOT NULL DEFAULT 1 COMMENT '1 普通 2 凭据',
  `credential_id` bigint NULL COMMENT '凭据id',
  `version` int NOT NULL DEFAULT 1 COMMENT '配置版本',
  `is_encrypted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否加密',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_env_config` (`env_id`,`config_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '环境配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_credential
-- ----------------------------
DROP TABLE IF EXISTS `dev_credential`;
CREATE TABLE `dev_credential`  (
  `id` bigint NOT NULL COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '凭据名称',
  `type` tinyint NOT NULL COMMENT '1 SSH 密钥 2 对象存储 AK/SK 3 Registry 账号 4 K8s 凭据 5 第三方 Token',
  `cipher_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AES 密文',
  `ref_count` int NOT NULL DEFAULT 0 COMMENT '引用数',
  `last_rotate_time` datetime(3) NULL COMMENT '最近轮换',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_cred_type` (`type`) USING BTREE,
  KEY `idx_cred_name` (`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '凭据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_deploy_lock
-- ----------------------------
DROP TABLE IF EXISTS `dev_deploy_lock`;
CREATE TABLE `dev_deploy_lock`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `owner` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '持锁标识',
  `owner_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '持锁人地址',
  `acquired_at` datetime(3) NOT NULL COMMENT '获取时间',
  `heartbeat_at` datetime(3) NULL COMMENT '心跳',
  `expire_at` datetime(3) NOT NULL COMMENT '到期',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否有效',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_deploy_lock_env` (`app_id`,`env_id`,`is_active`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部署锁镜像表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_deploy_record
-- ----------------------------
DROP TABLE IF EXISTS `dev_deploy_record`;
CREATE TABLE `dev_deploy_record`  (
  `id` bigint NOT NULL COMMENT '主键',
  `deploy_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部署编号 DP-yyyyMMdd-序号',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `deploy_type` tinyint NOT NULL COMMENT '执行器类型',
  `artifact_id` bigint NOT NULL COMMENT '制品id',
  `release_id` bigint NULL COMMENT '发布单id',
  `batch_plan` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '批次计划 JSON',
  `precheck_snapshot` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '校验快照',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 未开始 2 执行中 3 观察中 4 成功 5 失败 6 已回滚 7 已取消',
  `operator` bigint NOT NULL COMMENT '操作人',
  `confirmer` bigint NULL COMMENT '确认人，生产必填',
  `start_time` datetime(3) NULL COMMENT '开始时间',
  `end_time` datetime(3) NULL COMMENT '结束时间',
  `duration` bigint NULL COMMENT '耗时毫秒',
  `fail_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '失败原因',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_deploy_app_env` (`app_id`,`env_id`,`start_time`) USING BTREE,
  UNIQUE KEY `uk_deploy_no` (`deploy_no`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部署记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_deploy_batch
-- ----------------------------
DROP TABLE IF EXISTS `dev_deploy_batch`;
CREATE TABLE `dev_deploy_batch`  (
  `id` bigint NOT NULL COMMENT '主键',
  `deploy_id` bigint NOT NULL COMMENT '部署id',
  `batch_no` int NOT NULL COMMENT '批次号',
  `target_count` int NOT NULL DEFAULT 0 COMMENT '影响实例数',
  `target_snapshot` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实例清单快照',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1–7',
  `start_time` datetime(3) NULL COMMENT '开始时间',
  `end_time` datetime(3) NULL COMMENT '结束时间',
  `health_result` tinyint NULL COMMENT '1 通过 2 不通过',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_batch_deploy` (`deploy_id`,`batch_no`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部署批次表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_health_item
-- ----------------------------
DROP TABLE IF EXISTS `dev_health_item`;
CREATE TABLE `dev_health_item`  (
  `id` bigint NOT NULL COMMENT '主键',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `check_type` tinyint NOT NULL COMMENT '1 存活 2 成功率 3 P95 4 错误日志 5 核心指标',
  `threshold` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '阈值',
  `sample_interval` int NOT NULL COMMENT '采样间隔秒',
  `consecutive` int NOT NULL COMMENT '连续命中次数',
  `is_supported` tinyint(1) NOT NULL DEFAULT 1 COMMENT '该类型是否支持',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_health_item` (`env_id`,`check_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '健康检查项配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_health_sample
-- ----------------------------
DROP TABLE IF EXISTS `dev_health_sample`;
CREATE TABLE `dev_health_sample`  (
  `id` bigint NOT NULL COMMENT '主键',
  `deploy_id` bigint NOT NULL COMMENT '部署id',
  `batch_id` bigint NULL COMMENT '批次id',
  `check_type` tinyint NOT NULL COMMENT '检查项',
  `value` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '采样值',
  `threshold` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '阈值快照',
  `conclusion` tinyint NOT NULL COMMENT '1 通过 2 不通过 3 不支持',
  `sampled_at` datetime(3) NOT NULL COMMENT '采样时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sample_deploy` (`deploy_id`,`sampled_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '健康检查采样表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_deploy_precheck
-- ----------------------------
DROP TABLE IF EXISTS `dev_deploy_precheck`;
CREATE TABLE `dev_deploy_precheck`  (
  `id` bigint NOT NULL COMMENT '主键',
  `deploy_id` bigint NOT NULL COMMENT '部署id',
  `check_type` tinyint NOT NULL COMMENT '1 制品存在 2 形态匹配 3 环境空闲 4 依赖兼容 5 变更单',
  `conclusion` tinyint NOT NULL COMMENT '1 通过 2 不通过',
  `detail` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '明细',
  `checked_at` datetime(3) NOT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_precheck_deploy` (`deploy_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部署前校验表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_rollback_record
-- ----------------------------
DROP TABLE IF EXISTS `dev_rollback_record`;
CREATE TABLE `dev_rollback_record`  (
  `id` bigint NOT NULL COMMENT '主键',
  `deploy_id` bigint NULL COMMENT '关联部署',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `artifact_id` bigint NOT NULL COMMENT '回滚目标制品',
  `current_artifact_id` bigint NULL COMMENT '回滚前制品',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原因',
  `operator` bigint NOT NULL COMMENT '操作人',
  `confirmer` bigint NULL COMMENT '确认人',
  `start_time` datetime(3) NULL COMMENT '开始时间',
  `end_time` datetime(3) NULL COMMENT '结束时间',
  `duration` bigint NULL COMMENT '耗时毫秒',
  `result` tinyint NOT NULL COMMENT '1 成功 2 失败',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_rollback_env` (`env_id`,`start_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '回滚记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_change_ticket
-- ----------------------------
DROP TABLE IF EXISTS `dev_change_ticket`;
CREATE TABLE `dev_change_ticket`  (
  `id` bigint NOT NULL COMMENT '主键',
  `ticket_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '变更单号 CH-yyyyMMdd-序号',
  `type` tinyint NOT NULL COMMENT '1 常规 2 紧急 3 标准',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '变更内容',
  `window_start` datetime(3) NOT NULL COMMENT '计划窗口开始',
  `window_end` datetime(3) NOT NULL COMMENT '计划窗口结束',
  `rollback_plan` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '回滚方案',
  `impact_json` json CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '影响评估',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 草稿 2 待审批 3 已批准 4 已实施 5 已关闭 6 已取消',
  `applicant` bigint NOT NULL COMMENT '申请人',
  `approver` bigint NULL COMMENT '审批人',
  `source` tinyint NOT NULL DEFAULT 1 COMMENT '1 本系统 2 外部同步',
  `external_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '外部单号',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_ticket_no` (`ticket_no`) USING BTREE,
  KEY `idx_ticket_status` (`status`,`window_start`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '变更单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_change_ticket_app
-- ----------------------------
DROP TABLE IF EXISTS `dev_change_ticket_app`;
CREATE TABLE `dev_change_ticket_app`  (
  `id` bigint NOT NULL COMMENT '主键',
  `ticket_id` bigint NOT NULL COMMENT '变更单id',
  `app_id` bigint NOT NULL COMMENT '应用id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_ticket_app` (`ticket_id`,`app_id`) USING BTREE,
  KEY `idx_ticket_app_app` (`app_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '变更单应用表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_release_candidate
-- ----------------------------
DROP TABLE IF EXISTS `dev_release_candidate`;
CREATE TABLE `dev_release_candidate`  (
  `id` bigint NOT NULL COMMENT '主键',
  `rc_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发布单号 RC-yyyyMMdd-序号',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 草稿 2 待审批 3 已批准 4 发布中 5 观察中 6 已完成 7 已回滚 8 已驳回 9 已取消',
  `risk_level` tinyint NULL COMMENT '1 低 2 中 3 高',
  `risk_score` decimal(4,2) NULL COMMENT '加权分',
  `risk_detail` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '五因子明细 JSON',
  `impact_json` json CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '三层影响面',
  `window_start` datetime(3) NOT NULL COMMENT '计划窗口开始',
  `window_end` datetime(3) NOT NULL COMMENT '计划窗口结束',
  `change_ticket_id` bigint NULL COMMENT '变更单id',
  `creator` bigint NOT NULL COMMENT '创建人',
  `submitted_at` datetime(3) NULL COMMENT '提交时间',
  `closed_at` datetime(3) NULL COMMENT '关闭时间',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_rc_no` (`rc_no`) USING BTREE,
  KEY `idx_rc_status` (`status`,`window_start`) USING BTREE,
  KEY `idx_rc_creator` (`creator`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '发布单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_release_artifact
-- ----------------------------
DROP TABLE IF EXISTS `dev_release_artifact`;
CREATE TABLE `dev_release_artifact`  (
  `id` bigint NOT NULL COMMENT '主键',
  `rc_id` bigint NOT NULL COMMENT '发布单id',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `artifact_id` bigint NOT NULL COMMENT '制品id',
  `gate_conclusion` tinyint NULL COMMENT '门禁汇总快照',
  `is_locked` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否锁定',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_rc_artifact` (`rc_id`,`app_id`) USING BTREE,
  KEY `idx_rc_artifact_artifact` (`artifact_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '发布单制品表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_release_check_item
-- ----------------------------
DROP TABLE IF EXISTS `dev_release_check_item`;
CREATE TABLE `dev_release_check_item`  (
  `id` bigint NOT NULL COMMENT '主键',
  `rc_id` bigint NOT NULL COMMENT '发布单id',
  `check_type` tinyint NOT NULL COMMENT '1 门禁 2 影响面 3 回滚方案 4 监控覆盖 5 变更单 6 值班确认',
  `system_conclusion` tinyint NOT NULL COMMENT '系统结论',
  `manual_conclusion` tinyint NULL COMMENT '人工结论 1 通过 2 不通过 3 不适用 4 已知悉',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
  `evidence` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '证据',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_rc_check` (`rc_id`,`check_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '准入检查项表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_release_approval
-- ----------------------------
DROP TABLE IF EXISTS `dev_release_approval`;
CREATE TABLE `dev_release_approval`  (
  `id` bigint NOT NULL COMMENT '主键',
  `rc_id` bigint NOT NULL COMMENT '发布单id',
  `node` int NOT NULL COMMENT '审批节点',
  `approver` bigint NOT NULL COMMENT '审批人',
  `opinion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '意见',
  `result` tinyint NOT NULL COMMENT '1 通过 2 驳回',
  `create_time` datetime(3) NOT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_approval_rc` (`rc_id`,`node`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审批记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_release_strategy
-- ----------------------------
DROP TABLE IF EXISTS `dev_release_strategy`;
CREATE TABLE `dev_release_strategy`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `windows_json` json CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '多段发布窗口',
  `is_closure_excluded` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否排除封网',
  `observe_minutes` int NULL COMMENT '默认观察期覆盖值',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_strategy_app` (`app_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '发布策略表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_network_closure
-- ----------------------------
DROP TABLE IF EXISTS `dev_network_closure`;
CREATE TABLE `dev_network_closure`  (
  `id` bigint NOT NULL COMMENT '主键',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `scope` tinyint NOT NULL COMMENT '1 全局 2 应用 3 团队',
  `app_id` bigint NULL COMMENT '应用id',
  `team_id` bigint NULL COMMENT '团队id',
  `reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '原因',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_closure_date` (`start_date`,`end_date`) USING BTREE,
  KEY `idx_closure_app` (`app_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '封网期表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_release_record
-- ----------------------------
DROP TABLE IF EXISTS `dev_release_record`;
CREATE TABLE `dev_release_record`  (
  `id` bigint NOT NULL COMMENT '主键',
  `rc_id` bigint NOT NULL COMMENT '发布单id',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `env_id` bigint NOT NULL COMMENT '环境id',
  `deploy_type` tinyint NOT NULL COMMENT '部署类型',
  `artifact_id` bigint NOT NULL COMMENT '制品id',
  `batch_detail` json CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '批次明细快照',
  `health_snapshot` json CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '健康检查快照',
  `result` tinyint NOT NULL COMMENT '1 成功 2 失败 3 已回滚 4 已取消',
  `operator` bigint NOT NULL COMMENT '操作人',
  `confirmer` bigint NULL COMMENT '确认人',
  `start_time` datetime(3) NULL COMMENT '开始时间',
  `end_time` datetime(3) NULL COMMENT '结束时间',
  `duration` bigint NULL COMMENT '耗时毫秒',
  `is_rolled_back` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否回滚过',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_release_record_rc` (`rc_id`) USING BTREE,
  KEY `idx_release_record_app` (`app_id`,`start_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '发布记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_release_timeline
-- ----------------------------
DROP TABLE IF EXISTS `dev_release_timeline`;
CREATE TABLE `dev_release_timeline`  (
  `id` bigint NOT NULL COMMENT '主键',
  `rc_id` bigint NOT NULL COMMENT '发布单id',
  `node` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点',
  `operator` bigint NULL COMMENT '操作人',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
  `create_time` datetime(3) NOT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_timeline_rc` (`rc_id`,`create_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '发布单时间线表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_ai_output
-- ----------------------------
DROP TABLE IF EXISTS `dev_ai_output`;
CREATE TABLE `dev_ai_output`  (
  `id` bigint NOT NULL COMMENT '主键',
  `type` tinyint NOT NULL COMMENT '1 失败归因 2 准入辅助 3 预案 4 异常归因',
  `biz_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务id',
  `app_id` bigint NULL COMMENT '应用id',
  `run_id` bigint NULL COMMENT '运行id',
  `rc_id` bigint NULL COMMENT '发布单id',
  `alert_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '告警id',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '根因类别',
  `conclusion` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '结论',
  `confidence` tinyint NULL COMMENT '1 高 2 中 3 低',
  `suggestion_json` json CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '建议',
  `model_version` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '模型版本',
  `prompt_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '提示版本',
  `rule_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '规则版本',
  `knowledge_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '知识版本',
  `status` tinyint NOT NULL COMMENT '1 成功 2 丢弃 3 超时 4 无法归因',
  `duration` bigint NULL COMMENT '耗时',
  `discarded_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '丢弃原因',
  `create_time` datetime(3) NOT NULL COMMENT '生成时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_ai_output_biz` (`type`,`biz_id`) USING BTREE,
  KEY `idx_ai_output_app` (`app_id`,`create_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 输出表（含公共字段）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_ai_evidence
-- ----------------------------
DROP TABLE IF EXISTS `dev_ai_evidence`;
CREATE TABLE `dev_ai_evidence`  (
  `id` bigint NOT NULL COMMENT '主键',
  `output_id` bigint NOT NULL COMMENT '输出id',
  `type` tinyint NOT NULL COMMENT '1 日志 2 指标 3 变更 4 历史',
  `locator` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '定位',
  `snippet` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '原文片段',
  `is_valid` tinyint(1) NOT NULL DEFAULT 0 COMMENT '服务端校验是否通过',
  `sort` int NOT NULL DEFAULT 0 COMMENT '顺序',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_evidence_output` (`output_id`,`sort`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '证据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_ai_feedback
-- ----------------------------
DROP TABLE IF EXISTS `dev_ai_feedback`;
CREATE TABLE `dev_ai_feedback`  (
  `id` bigint NOT NULL COMMENT '主键',
  `output_id` bigint NOT NULL COMMENT '输出id',
  `is_helpful` tinyint(1) NOT NULL COMMENT '是否有帮助',
  `correct_answer` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '正确答案',
  `user_id` bigint NOT NULL COMMENT '反馈人',
  `create_time` datetime(3) NOT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_feedback_output` (`output_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '反馈表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_action_catalog
-- ----------------------------
DROP TABLE IF EXISTS `dev_action_catalog`;
CREATE TABLE `dev_action_catalog`  (
  `id` bigint NOT NULL COMMENT '主键',
  `action_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '动作标识',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `level` tinyint NOT NULL COMMENT '0–3',
  `params_schema` json CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '参数约束',
  `precondition` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '前置条件',
  `undo_mode` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '撤销方式',
  `is_undoable` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否可撤销',
  `freq_minutes` int NULL COMMENT '频控窗口',
  `freq_limit` int NULL COMMENT '窗口内上限',
  `is_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '启停',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_action_id` (`action_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '动作目录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_action_record
-- ----------------------------
DROP TABLE IF EXISTS `dev_action_record`;
CREATE TABLE `dev_action_record`  (
  `id` bigint NOT NULL COMMENT '主键',
  `action_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '动作标识',
  `level` tinyint NOT NULL COMMENT '级别',
  `source` tinyint NOT NULL COMMENT '1 归因 2 预案 3 异常',
  `biz_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '业务id',
  `app_id` bigint NULL COMMENT '应用id',
  `env_id` bigint NULL COMMENT '环境id',
  `params_json` json CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '参数',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '理由',
  `evidence_json` json CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '证据',
  `operator` bigint NOT NULL COMMENT '执行人',
  `model_version` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '模型版本',
  `prompt_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '提示版本',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1–8',
  `execute_time` datetime(3) NULL COMMENT '执行时间',
  `undo_deadline` datetime(3) NULL COMMENT '撤销截止',
  `undo_time` datetime(3) NULL COMMENT '撤销时间',
  `undo_user` bigint NULL COMMENT '撤销人',
  `result` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '结果',
  `error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '失败原因',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_action_record_app` (`app_id`,`execute_time`) USING BTREE,
  KEY `idx_action_record_action` (`action_id`,`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '动作记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_app_ai_level
-- ----------------------------
DROP TABLE IF EXISTS `dev_app_ai_level`;
CREATE TABLE `dev_app_ai_level`  (
  `id` bigint NOT NULL COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `level` tinyint NOT NULL COMMENT '已开通最高级别',
  `granted_by` bigint NOT NULL COMMENT '授权人',
  `granted_at` datetime(3) NOT NULL COMMENT '授权时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_app_ai_level` (`app_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '应用 AI 授权表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_ai_eval_sample
-- ----------------------------
DROP TABLE IF EXISTS `dev_ai_eval_sample`;
CREATE TABLE `dev_ai_eval_sample`  (
  `id` bigint NOT NULL COMMENT '主键',
  `output_id` bigint NOT NULL COMMENT '输出id',
  `source` tinyint NOT NULL COMMENT '1 没帮助 2 人工修正',
  `score` tinyint NULL COMMENT '评分',
  `note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_eval_output` (`output_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评测样本表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_ai_prompt_version
-- ----------------------------
DROP TABLE IF EXISTS `dev_ai_prompt_version`;
CREATE TABLE `dev_ai_prompt_version`  (
  `id` bigint NOT NULL COMMENT '主键',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '版本号',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提示内容',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 草稿 2 生效 3 归档',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_prompt_code` (`code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提示版本表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_ai_rule_pattern
-- ----------------------------
DROP TABLE IF EXISTS `dev_ai_rule_pattern`;
CREATE TABLE `dev_ai_rule_pattern`  (
  `id` bigint NOT NULL COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模式名',
  `pattern` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '正则',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '根因类别',
  `is_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '启停',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_rule_pattern_enabled` (`is_enabled`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '规则模式库表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_metric_daily
-- ----------------------------
DROP TABLE IF EXISTS `dev_metric_daily`;
CREATE TABLE `dev_metric_daily`  (
  `id` bigint NOT NULL COMMENT '主键',
  `stat_date` date NOT NULL COMMENT '统计日',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `deploy_freq` int NULL COMMENT '部署频率',
  `lead_time_p50` bigint NULL COMMENT '变更前置时间秒',
  `change_fail_rate` decimal(5,4) NULL COMMENT '变更失败率',
  `mttr_p50` bigint NULL COMMENT '恢复时长秒',
  `gate_exempt_rate` decimal(5,4) NULL COMMENT '豁免率',
  `gate_fetch_fail_rate` decimal(5,4) NULL COMMENT '数据获取失败率',
  `ai_adopt_rate` decimal(5,4) NULL COMMENT '采纳率',
  `ai_low_confidence_rate` decimal(5,4) NULL COMMENT '低置信占比',
  `ai_refusal_rate` decimal(5,4) NULL COMMENT '拒答率',
  `action_success_rate` decimal(5,4) NULL COMMENT '动作成功率',
  `action_undo_rate` decimal(5,4) NULL COMMENT '动作撤销率',
  `data_status` tinyint NOT NULL DEFAULT 1 COMMENT '1 正常 2 数据不足',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_metric_day` (`stat_date`,`app_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '每日指标表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_stage_stat
-- ----------------------------
DROP TABLE IF EXISTS `dev_stage_stat`;
CREATE TABLE `dev_stage_stat`  (
  `id` bigint NOT NULL COMMENT '主键',
  `stat_date` date NOT NULL COMMENT '统计日',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `stage_type` tinyint NOT NULL COMMENT '阶段类型',
  `p50` bigint NULL COMMENT '中位耗时',
  `p90` bigint NULL COMMENT 'P90 耗时',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败数',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_stage_stat` (`stat_date`,`app_id`,`stage_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '阶段耗时统计表' ROW_FORMAT = DYNAMIC;



-- ----------------------------
-- Table structure for sys_storage_policy
-- ----------------------------
DROP TABLE IF EXISTS `sys_storage_policy`;
CREATE TABLE `sys_storage_policy`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `app_id` bigint NOT NULL DEFAULT 0 COMMENT '应用id，0 为全局',
  `log_keep_days` int NOT NULL DEFAULT 365 COMMENT '操作日志留存天数，≥365',
  `archive_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '归档位置',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime(3) NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime(3) NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_storage_policy` (`tenant_id`,`app_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '存储与保留策略表' ROW_FORMAT = DYNAMIC;



-- ==================== 系统扩展表（通知/Token/导出） ====================

-- ----------------------------
-- Table structure for sys_notify_channel
-- ----------------------------
DROP TABLE IF EXISTS `sys_notify_channel`;
CREATE TABLE `sys_notify_channel`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道名',
  `type` tinyint NOT NULL COMMENT '1 IM 2 邮件 3 Webhook',
  `config_json` json CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '连接配置，敏感项加密',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 正常 2 异常',
  `last_send_time` datetime(3) NULL COMMENT '最近发送',
  `last_error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '最近失败',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_channel_type` (`type`,`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知渠道表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_notify_template
-- ----------------------------
DROP TABLE IF EXISTS `sys_notify_template`;
CREATE TABLE `sys_notify_template`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `event` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件',
  `channel_type` tinyint NOT NULL COMMENT '渠道类型',
  `title_template` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题模板',
  `body_template` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '正文模板',
  `variables` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '变量清单',
  `is_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '启停',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_notify_template` (`tenant_id`,`event`,`channel_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知模板表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_notify_subscription
-- ----------------------------
DROP TABLE IF EXISTS `sys_notify_subscription`;
CREATE TABLE `sys_notify_subscription`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `event` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件',
  `role_id` bigint NULL COMMENT '按角色订阅',
  `user_id` bigint NULL COMMENT '按用户订阅',
  `channel_id` bigint NOT NULL COMMENT '渠道id',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_notify_sub_event` (`event`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知订阅表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_notify_record
-- ----------------------------
DROP TABLE IF EXISTS `sys_notify_record`;
CREATE TABLE `sys_notify_record`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `event` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件',
  `receiver` bigint NULL COMMENT '接收人',
  `channel_id` bigint NOT NULL COMMENT '渠道',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内容快照',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 待发 2 成功 3 失败 4 合并',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `fail_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '失败原因',
  `send_time` datetime(3) NULL COMMENT '发送时间',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_notify_record_status` (`status`,`create_time`) USING BTREE,
  KEY `idx_notify_record_event` (`event`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_notify_dnd
-- ----------------------------
DROP TABLE IF EXISTS `sys_notify_dnd`;
CREATE TABLE `sys_notify_dnd`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `scope` tinyint NOT NULL COMMENT '1 全局 2 角色 3 用户',
  `role_id` bigint NULL COMMENT '角色id',
  `user_id` bigint NULL COMMENT '用户id',
  `start_time` time NOT NULL COMMENT '开始',
  `end_time` time NOT NULL COMMENT '结束',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_dnd_scope` (`scope`,`role_id`,`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知免打扰表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_notify_escalation
-- ----------------------------
DROP TABLE IF EXISTS `sys_notify_escalation`;
CREATE TABLE `sys_notify_escalation`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `event` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件',
  `unread_minutes` int NOT NULL COMMENT '未读分钟数',
  `escalate_role_id` bigint NOT NULL COMMENT '升级角色',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_escalation_event` (`event`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知升级规则表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_open_token
-- ----------------------------
DROP TABLE IF EXISTS `sys_open_token`;
CREATE TABLE `sys_open_token`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `app_id` bigint NOT NULL COMMENT '绑定应用',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '名称',
  `token_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'sha256',
  `token_mask` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '掩码',
  `quota_per_minute` int NOT NULL DEFAULT 100 COMMENT '每分钟配额',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 正常 2 禁用',
  `expire_at` datetime(3) NULL COMMENT '过期时间',
  `last_used_time` datetime(3) NULL COMMENT '最近调用',
  `last_rotate_time` datetime(3) NULL COMMENT '最近轮换',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_open_token_hash` (`token_hash`) USING BTREE,
  KEY `idx_open_token_app` (`app_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '开放API Token表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_open_api_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_open_api_log`;
CREATE TABLE `sys_open_api_log`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `token_id` bigint NOT NULL COMMENT 'Token',
  `app_id` bigint NOT NULL COMMENT '应用',
  `path` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接口',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '调用方 IP',
  `response_code` int NOT NULL COMMENT '响应码',
  `duration` bigint NOT NULL COMMENT '耗时',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_open_log_token` (`token_id`,`create_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '开放API调用明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_export_task
-- ----------------------------
DROP TABLE IF EXISTS `sys_export_task`;
CREATE TABLE `sys_export_task`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编码',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模块',
  `filters` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '筛选条件',
  `ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '勾选id',
  `format` tinyint NOT NULL DEFAULT 1 COMMENT '1 CSV 2 Excel',
  `row_count` int NULL COMMENT '行数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 待执行 2 执行中 3 成功 4 失败 5 已过期',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文件地址',
  `fail_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '失败原因',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试',
  `expire_at` datetime(3) NULL COMMENT '过期时间',
  `create_user` bigint NULL COMMENT '创建人',
  `create_time` datetime(3) NULL COMMENT '创建时间',
  `update_user` bigint NULL COMMENT '更新人',
  `update_time` datetime(3) NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_export_status` (`status`,`create_time`) USING BTREE,
  KEY `idx_export_user` (`create_user`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '导出任务表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
SET FOREIGN_KEY_CHECKS = 1;
