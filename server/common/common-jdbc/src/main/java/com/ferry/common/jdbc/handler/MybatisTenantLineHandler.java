package com.ferry.common.jdbc.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.ferry.common.core.constant.StrConst;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.config.props.MybatisProperties;
import com.ferry.common.security.utils.AuthUtil;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MyBatis租户处理器
 */
public class MybatisTenantLineHandler implements TenantLineHandler {
    private final MybatisProperties properties;
    private final Set<String> concernTables;

    /**
     * 默认要添加租户的表：系统表 + 交付域表
     */
    private static final List<String> DEFAULT_CONCERN_TABLES = CollUtil.asList(
            "sys_config", "sys_dept", "sys_dict", "sys_menu", "sys_post", "sys_role", "sys_user", "sys_storage_policy",
            "dev_app", "dev_app_repo", "dev_app_owner", "dev_app_dependency", "dev_app_core_flag_apply", "dev_app_ai_level",
            "dev_pipeline", "dev_pipeline_version", "dev_pipeline_template", "dev_pipeline_template_version",
            "dev_pipeline_template_var", "dev_pipeline_template_derivation",
            "dev_run", "dev_run_stage", "dev_run_task", "dev_run_log_segment", "dev_run_precheck",
            "dev_run_trigger", "dev_run_operation", "dev_run_idempotency",
            "dev_gate_rule", "dev_gate_result", "dev_gate_exemption", "dev_gate_baseline",
            "dev_gate_baseline_issue", "dev_gate_baseline_reset", "dev_issue",
            "dev_artifact", "dev_artifact_env", "dev_artifact_clean_policy", "dev_artifact_clean_task",
            "dev_artifact_clean_item", "dev_artifact_compensation",
            "dev_environment", "dev_env_config", "dev_credential", "dev_deploy_lock", "dev_deploy_record",
            "dev_deploy_batch", "dev_health_item", "dev_health_sample", "dev_deploy_precheck", "dev_rollback_record",
            "dev_change_ticket", "dev_change_ticket_app", "dev_release_candidate", "dev_release_artifact",
            "dev_release_check_item", "dev_release_approval", "dev_release_strategy", "dev_network_closure",
            "dev_release_record", "dev_release_timeline",
            "dev_ai_output", "dev_ai_evidence", "dev_ai_feedback", "dev_action_catalog", "dev_action_record",
            "dev_ai_eval_sample", "dev_ai_prompt_version", "dev_ai_rule_pattern",
            "dev_metric_daily", "dev_stage_stat"
    );

    public MybatisTenantLineHandler(MybatisProperties properties) {
        this.properties = properties;
        this.concernTables = new HashSet<>(DEFAULT_CONCERN_TABLES);
        this.concernTables.addAll(properties.getTenant().getTables());
    }

    @Override
    public Expression getTenantId() {
        String tenantId = AuthUtil.getTenantId();
        if (StrUtil.isBlank(tenantId)) {
            tenantId = StrConst.EMPTY;
        }
        return new StringValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return properties.getTenant().getColumn();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return !concernTables.contains(tableName);
    }
}
