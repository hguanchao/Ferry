package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.security.annotation.DataScope;
import com.ferry.devops.api.dto.query.PipelineTemplateQuery;
import com.ferry.devops.api.entity.PipelineTemplate;
import com.ferry.devops.api.vo.PipelineTemplateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流水线模板数据访问
 */
@Mapper
@DataScope(value = "dev_pipeline_template", deptColumn = "dept_id")
public interface PipelineTemplateMapper extends BaseMapper<PipelineTemplate> {

    /**
     * 分页查询组织级模板列表
     *
     * <p>组织级模板全员可见是业务约定，跳过数据权限过滤。
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 模板列表
     */
    @InterceptorIgnore(dataPermission = "1")
    IPage<PipelineTemplateVO> selectOrgPageList(IPage<PipelineTemplateVO> page, @Param("query") PipelineTemplateQuery query);

    /**
     * 分页查询团队级模板列表
     *
     * <p>数据权限按归属部门过滤，只看本团队范围内的模板。
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 模板列表
     */
    IPage<PipelineTemplateVO> selectTeamPageList(IPage<PipelineTemplateVO> page, @Param("query") PipelineTemplateQuery query);

    /**
     * 查询启用的组织级模板候选
     *
     * @param keyword 模板名称模糊搜索，可为空
     * @return 模板候选列表
     */
    @InterceptorIgnore(dataPermission = "1")
    List<PipelineTemplateVO> selectOrgOptions(@Param("keyword") String keyword);

    /**
     * 查询启用的团队级模板候选
     *
     * <p>数据权限按归属部门过滤，跨团队模板只有超管或全数据范围可选用。
     *
     * @param keyword 模板名称模糊搜索，可为空
     * @return 模板候选列表
     */
    List<PipelineTemplateVO> selectTeamOptions(@Param("keyword") String keyword);

    /**
     * 查询已有派生流水线的模板id
     *
     * <p>查询的是流水线表本身，不在本接口的数据权限声明范围内，删除前置校验不看查看者的数据范围。
     *
     * @param templateIds 模板id集合
     * @return 存在派生流水线的模板id列表
     */
    List<Long> selectDerivedTemplateIds(@Param("templateIds") List<Long> templateIds);
}
