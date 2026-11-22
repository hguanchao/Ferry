package com.ferry.devops.biz.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.GateRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GateRuleMapper extends BaseMapper<GateRule> {

    /**
     * 按作用域/应用/类型加载启用规则；stageType 非空时只取作用阶段包含该类型的规则。
     */
    @Select("""
            <script>
            SELECT * FROM dev_gate_rule WHERE is_deleted = 0 AND is_enabled = 1
            AND scope_type = #{scopeType} AND app_id = #{appId} AND rule_type = #{ruleType}
            <if test="stageType != null">
                AND (stage_types IS NULL OR stage_types = '' OR FIND_IN_SET(#{stageType}, REPLACE(stage_types, ' ', '')))
            </if>
            </script>
            """)
    List<GateRule> byScope(@Param("scopeType") Integer scopeType, @Param("appId") Long appId,
                           @Param("ruleType") Integer ruleType, @Param("stageType") Integer stageType);

    /**
     * 分支级规则按通配符加载（分支级规则 app_id 亦需匹配）。
     */
    @Select("SELECT * FROM dev_gate_rule WHERE is_deleted = 0 AND is_enabled = 1 "
            + "AND scope_type = 3 AND app_id = #{appId}")
    List<GateRule> branchRules(@Param("appId") Long appId);
}
