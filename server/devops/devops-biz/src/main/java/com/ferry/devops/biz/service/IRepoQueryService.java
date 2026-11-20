package com.ferry.devops.biz.service;

import com.ferry.devops.api.vo.RepoCommitVO;

import java.util.List;

/**
 * 触发弹窗仓库候选查询：分支列表与最近提交。
 */
public interface IRepoQueryService {

    /**
     * 查询应用仓库的分支候选。
     *
     * @param appId 应用id
     * @return 分支名列表，应用默认分支排首位
     */
    List<String> branches(Long appId);

    /**
     * 查询指定分支的最近提交候选。
     *
     * @param appId  应用id
     * @param branch 分支名
     * @param limit  返回条数，空值默认 10，最大 50
     * @return 提交候选列表
     */
    List<RepoCommitVO> commits(Long appId, String branch, Integer limit);
}
