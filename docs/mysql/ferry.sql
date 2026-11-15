-- 表结构以 Flyway 迁移为准：系统管理域在 server/system/system-biz/src/main/resources/db/migration，
-- 交付域在 server/devops/devops-biz/src/main/resources/db/migration
-- 本文件保留用于手工重建与排查，含 DROP TABLE，切勿在存量库执行
/*
 Source Server Type    : MySQL
 Source Server Version : 8.4.0
 Source Schema         : ferry
*/

-- 本文件只放建表语句，初始与演示数据见 ferry_data.sql
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for dev_app
-- ----------------------------
DROP TABLE IF EXISTS `dev_app`;
CREATE TABLE `dev_app`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '应用编码，小写字母/数字/中划线，创建后不可改',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '应用名称',
  `dept_id` bigint NOT NULL COMMENT '所属团队',
  `owner_ids` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '应用负责人id',
  `owner_names` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '应用负责人名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '应用描述',
  `is_core` tinyint(1) NOT NULL DEFAULT 0 COMMENT '核心链路 0：否 1：是',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 0：停用 1：正常',
  `repo_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '代码托管平台 GITLAB/GITHUB/GITEE',
  `repo_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '仓库 Git 地址',
  `repo_branch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '默认分支',
  `repo_commit_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '提交ID',
  `repo_credential_id` bigint NULL DEFAULT NULL COMMENT '平台凭据id',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_dept`(`dept_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '应用档案表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_pipeline
-- ----------------------------
DROP TABLE IF EXISTS `dev_pipeline`;
CREATE TABLE `dev_pipeline`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `app_id` bigint NOT NULL COMMENT '应用id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流水线名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `current_version_id` bigint NULL DEFAULT NULL COMMENT '当前版本id',
  `current_version_no` int NULL DEFAULT NULL COMMENT '当前版本号',
  `stage_count` int NOT NULL DEFAULT 0 COMMENT '阶段数',
  `source_template_id` bigint NULL DEFAULT NULL COMMENT '来源模板id，非模板派生为空',
  `source_template_version_no` int NULL DEFAULT NULL COMMENT '派生或最后合并时的模板版本号',
  `source_template_vars` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '派生时占位变量取值JSON',
  `source_template_merged_time` datetime NULL DEFAULT NULL COMMENT '最后合并模板时间',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_app_name`(`app_id` ASC, `name` ASC) USING BTREE,
  INDEX `idx_source_template`(`source_template_id` ASC) USING BTREE,
  INDEX `idx_update_time`(`update_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流水线定义表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_pipeline_version
-- ----------------------------
DROP TABLE IF EXISTS `dev_pipeline_version`;
CREATE TABLE `dev_pipeline_version`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `pipeline_id` bigint NOT NULL COMMENT '流水线id',
  `version_no` int NOT NULL COMMENT '版本号',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '定义快照',
  `content_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内容hash',
  `change_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '变更说明',
  `create_user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更人',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pipeline_version`(`pipeline_id` ASC, `version_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流水线版本表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_pipeline_template
-- ----------------------------
DROP TABLE IF EXISTS `dev_pipeline_template`;
CREATE TABLE `dev_pipeline_template`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `scope` tinyint(1) NOT NULL COMMENT '模板层级 0：组织级 1：团队级',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '归属部门id，组织级为空',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '适用场景',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 0：停用 1：启用',
  `current_version_id` bigint NULL DEFAULT NULL COMMENT '当前版本id',
  `current_version_no` int NULL DEFAULT NULL COMMENT '当前版本号',
  `stage_count` int NOT NULL DEFAULT 0 COMMENT '阶段数',
  `variable_count` int NOT NULL DEFAULT 0 COMMENT '占位变量数',
  `create_user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人名称快照',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_scope_name`(`scope` ASC, `dept_id` ASC, `name` ASC) USING BTREE,
  INDEX `idx_update_time`(`update_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流水线模板表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for dev_pipeline_template_version
-- ----------------------------
DROP TABLE IF EXISTS `dev_pipeline_template_version`;
CREATE TABLE `dev_pipeline_template_version`  (
  `id` bigint NOT NULL COMMENT '数据id',
  `template_id` bigint NOT NULL COMMENT '模板id',
  `version_no` int NOT NULL COMMENT '版本号',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '定义快照，含阶段与占位变量',
  `content_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内容hash',
  `change_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '变更说明',
  `create_user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更人',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除 0：否 1：是',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_template_version`(`template_id` ASC, `version_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流水线模板版本表' ROW_FORMAT = DYNAMIC;

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

-- ----------------------------
SET FOREIGN_KEY_CHECKS = 1;
