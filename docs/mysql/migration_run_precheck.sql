-- ----------------------------
-- 运行预检字段调整
--
-- 背景：预检状态与运行状态语义重复，统一由 dev_run.status 表达：
--       0=待确认 1=排队中 2=运行中 3=阻塞 4=成功 5=失败 6=超时 7=已取消 8=预检未通过
--       pre_check_status 恒为 1（从未写过异常值），删除；pre_check_content 改名并承载 AI 研判理由。
-- 适用：已按旧结构建库的环境；新建库直接使用 ferry.sql。
-- ----------------------------

ALTER TABLE `dev_run`
    DROP COLUMN `pre_check_status`,
    CHANGE COLUMN `pre_check_content` `pre_check_result` varchar(500)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL
        COMMENT '预检结果：五项校验摘要与 AI 研判理由';

ALTER TABLE `dev_run`
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 1
        COMMENT '状态 0=待确认 1=排队中 2=运行中 3=阻塞 4=成功 5=失败 6=超时 7=已取消 8=预检未通过';
