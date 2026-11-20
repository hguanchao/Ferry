package com.ferry.devops.biz.integration.repo;

import com.ferry.devops.api.entity.App;

import java.util.List;

/**
 * 仓库读取客户端：按应用登记的代码平台查询分支与最近提交。
 */
public interface RepoClient {

    /**
     * 查询应用仓库的分支列表。
     *
     * @param app 应用档案
     * @return 分支名列表
     */
    List<String> listBranches(App app);

    /**
     * 查询应用仓库指定分支的最近提交。
     *
     * @param app    应用档案
     * @param branch 分支名
     * @param limit  返回条数
     * @return 提交摘要列表
     */
    List<RepoCommit> listCommits(App app, String branch, int limit);
}
