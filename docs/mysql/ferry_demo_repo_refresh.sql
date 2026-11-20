-- ----------------------------
-- 演示应用仓库地址刷新
-- 适用：已导入 ferry_data.sql 的存量库；新建库直接使用 ferry_data.sql 即可
-- 说明：三套演示应用改为主流 GitHub 公开仓库，token 留空，匿名克隆即可
--       order-service   RuoYi（master）
--       payment-service mall（master）
--       web-portal      vue-element-admin（master）
-- ----------------------------

SET NAMES utf8mb4;

UPDATE `dev_app`
SET `description`   = '订单核心交易服务（演示仓库：RuoYi）',
    `repo_type`      = 2,
    `repo_branch`    = 'master',
    `repo_commit_id` = '47843e3d99c0f9cfd9311b8cfdf598a69e895306',
    `repo_url`       = 'https://github.com/yangzongzhuan/RuoYi.git',
    `repo_token`     = NULL
WHERE `code` = 'order-service'
  AND `is_deleted` = 0;

UPDATE `dev_app`
SET `description`   = '支付与对账服务（演示仓库：mall）',
    `repo_type`      = 2,
    `repo_branch`    = 'master',
    `repo_commit_id` = 'dcaa93b3150352e5044708d7211b5bed0af4509f',
    `repo_url`       = 'https://github.com/macrozheng/mall.git',
    `repo_token`     = NULL
WHERE `code` = 'payment-service'
  AND `is_deleted` = 0;

UPDATE `dev_app`
SET `description`   = '面向用户的 Web 门户（演示仓库：vue-element-admin）',
    `repo_type`      = 2,
    `repo_branch`    = 'master',
    `repo_commit_id` = '6858a9ad67483025f6a9432a926beb9327037be3',
    `repo_url`       = 'https://github.com/PanJiaChen/vue-element-admin.git',
    `repo_token`     = NULL
WHERE `code` = 'web-portal'
  AND `is_deleted` = 0;

-- ----------------------------
-- 演示流水线构建工具与任务脚本
-- 构建工具仅作阶段属性；实际执行命令写在任务 script_content 中。
-- ----------------------------
UPDATE `dev_pipeline_stage` SET `build_tool` = 'Maven' WHERE `id` IN (2000000000000008001,2000000000000008002,2000000000000008003,2000000000000008004,2000000000000008009,2000000000000008010,2000000000000008011,2000000000000008012);
UPDATE `dev_pipeline_stage` SET `build_tool` = 'Node' WHERE `id` IN (2000000000000008017,2000000000000008018,2000000000000008019,2000000000000008020);
UPDATE `dev_pipeline_stage` SET `outputs` = NULL WHERE `id` IN (2000000000000008001,2000000000000008009,2000000000000008017);
UPDATE `dev_pipeline_stage` SET `outputs` = 'target/*.jar' WHERE `id` IN (2000000000000008004,2000000000000008012);
UPDATE `dev_pipeline_stage` SET `outputs` = 'dist/**' WHERE `id` = 2000000000000008020;
UPDATE `dev_pipeline_stage_task` SET `script_content` = 'mvn -B -DskipTests clean package' WHERE `id` IN (2000000000000008101,2000000000000008109);
UPDATE `dev_pipeline_stage_task` SET `script_content` = 'mvn -B test' WHERE `id` IN (2000000000000008102,2000000000000008110);
UPDATE `dev_pipeline_stage_task` SET `script_content` = 'mvn -B -DskipTests verify' WHERE `id` IN (2000000000000008103,2000000000000008111);
UPDATE `dev_pipeline_stage_task` SET `script_content` = 'echo "制品已生成"' WHERE `id` IN (2000000000000008104,2000000000000008112);
UPDATE `dev_pipeline_stage_task` SET `script_content` = 'npm install && npm run build:prod' WHERE `id` = 2000000000000008117;
UPDATE `dev_pipeline_stage_task` SET `script_content` = 'npm run test:unit' WHERE `id` = 2000000000000008118;
UPDATE `dev_pipeline_stage_task` SET `script_content` = 'npm run lint' WHERE `id` = 2000000000000008119;
UPDATE `dev_pipeline_stage_task` SET `script_content` = 'echo "前端产物已生成"' WHERE `id` = 2000000000000008120;
