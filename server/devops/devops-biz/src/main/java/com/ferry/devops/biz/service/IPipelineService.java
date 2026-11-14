package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.PipelineDTO;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.vo.PipelineVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface IPipelineService extends IService<Pipeline> {

    /**
     * 流水线管理-分页列表
     *
     * @param body 查询条件
     * @return 流水线分页列表
     */
    IPage<PipelineVO> pageList(PipelineQuery body);

    /**
     * 流水线管理-详情
     *
     * @param id 流水线id
     * @return 流水线详情，含当前定义
     */
    PipelineVO detail(Long id);

    /**
     * 流水线管理-新增或编辑
     *
     * <p>新增时携带初始定义并在同一事务内生成 V1；编辑只允许改名称与描述。
     *
     * @param body 表单数据
     * @return true 成功 || false 失败
     */
    boolean save(PipelineDTO body);

    /**
     * 流水线管理-逻辑删除
     *
     * @param ids 流水线id集合
     * @return true 成功 || false 失败
     */
    boolean deleted(List<Long> ids);

    /**
     * 流水线管理-导出
     *
     * <p>传 ids 时只导出这些流水线（勾选导出），不传则按查询条件导出（筛选结果导出）；
     * 两种情况都受数据权限约束，与列表同一口径。
     *
     * @param body     查询条件
     * @param response HTTP 响应，xlsx 直接写入响应流
     */
    void export(PipelineQuery body, HttpServletResponse response);
}
