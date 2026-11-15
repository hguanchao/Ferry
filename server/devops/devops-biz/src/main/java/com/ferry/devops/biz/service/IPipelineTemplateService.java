package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.PipelineTemplateDTO;
import com.ferry.devops.api.dto.query.PipelineTemplateQuery;
import com.ferry.devops.api.entity.PipelineTemplate;
import com.ferry.devops.api.vo.PipelineTemplateVO;
import com.ferry.devops.api.vo.PipelineVO;

import java.util.List;

public interface IPipelineTemplateService extends IService<PipelineTemplate> {

    /**
     * 流水线模板-分页列表
     *
     * @param body 查询条件，层级必传，组织级与团队级分两条查询
     * @return 模板分页列表，含被派生流水线数
     */
    IPage<PipelineTemplateVO> pageList(PipelineTemplateQuery body);

    /**
     * 流水线模板-详情
     *
     * @param id 模板id
     * @return 模板详情，含当前定义与占位变量
     */
    PipelineTemplateVO detail(Long id);

    /**
     * 流水线模板-新增或编辑
     *
     * <p>新增时组织级仅平台管理员可建，团队级归属部门必须在数据权限范围内，并在同一事务内生成 V1；
     * 编辑只允许改名称与适用场景。
     *
     * @param body 表单数据
     * @return true 成功 || false 失败
     */
    boolean save(PipelineTemplateDTO body);

    /**
     * 流水线模板-逻辑删除
     *
     * <p>删除前置：无派生流水线，有派生时只能停用。
     *
     * @param ids 模板id集合
     * @return true 成功 || false 失败
     */
    boolean deleted(List<Long> ids);

    /**
     * 流水线模板-停用或启用
     *
     * <p>停用后不可被新流水线选用，已派生流水线不受影响。
     *
     * @param id     模板id
     * @param status 目标状态 0：停用 1：启用
     * @return true 成功 || false 失败
     */
    boolean changeStatus(Long id, Integer status);

    /**
     * 流水线模板-派生流水线清单
     *
     * @param body 查询条件，id 为模板id
     * @return 由该模板派生的流水线分页列表，含是否落后于模板与最后合并时间
     */
    IPage<PipelineVO> derivedList(PipelineTemplateQuery body);

    /**
     * 流水线模板-选用候选
     *
     * <p>只返回启用中的模板，组织级与团队级合并返回；团队级按数据权限过滤。
     *
     * @param keyword 模板名称模糊搜索，可为空
     * @return 模板候选列表
     */
    List<PipelineTemplateVO> options(String keyword);
}
