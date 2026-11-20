package com.ferry.devops.biz.integration.repo;

import java.util.List;

/**
 * 代码平台仓库查询协议：分支列表与最近提交。
 *
 * <p>实现类只做平台协议转换，不承载业务判断；令牌缺失时按公开仓库访问。
 */
public interface RepoProvider {

    /**
     * 支持的代码平台类型。
     *
     * @return 1 GitLab 2 GitHub 3 Gitee
     */
    int repoType();

    /**
     * 查询分支列表。
     *
     * @param target 仓库访问参数
     * @return 分支名列表，最多 100 个
     */
    List<String> listBranches(RepoTarget target);

    /**
     * 查询指定分支的最近提交。
     *
     * @param target 仓库访问参数
     * @param branch 分支名
     * @param limit  返回条数
     * @return 提交摘要列表
     */
    List<RepoCommit> listCommits(RepoTarget target, String branch, int limit);
}
