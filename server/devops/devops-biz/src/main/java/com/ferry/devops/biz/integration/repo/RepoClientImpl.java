package com.ferry.devops.biz.integration.repo;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.devops.api.entity.App;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 仓库读取门面：解析应用仓库地址后，按代码平台分发到对应 Provider。
 */
@Slf4j
@Component
@RequiredArgsConstructor
class RepoClientImpl implements RepoClient {

    private final List<RepoProvider> providers;

    @Override
    public List<String> listBranches(App app) {
        RepoTarget target = RepoUrlParser.parse(app);
        return providerOf(target).listBranches(target);
    }

    @Override
    public List<RepoCommit> listCommits(App app, String branch, int limit) {
        RepoTarget target = RepoUrlParser.parse(app);
        return providerOf(target).listCommits(target, branch, limit);
    }

    private RepoProvider providerOf(RepoTarget target) {
        log.debug("repo query: repoType={}, host={}, project={}",
                target.repoType(), target.host(), target.projectPath());
        return providers.stream()
                .filter(provider -> provider.repoType() == target.repoType())
                .findFirst()
                .orElseThrow(() -> new ServiceException("暂不支持该代码平台"));
    }
}
