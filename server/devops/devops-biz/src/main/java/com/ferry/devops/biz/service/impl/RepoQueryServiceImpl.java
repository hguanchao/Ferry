package com.ferry.devops.biz.service.impl;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.vo.RepoCommitVO;
import com.ferry.devops.biz.integration.repo.RepoClient;
import com.ferry.devops.biz.integration.repo.RepoCommit;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.service.IRepoQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 触发弹窗仓库候选查询实现。
 */
@Service
@RequiredArgsConstructor
public class RepoQueryServiceImpl implements IRepoQueryService {

    private static final int DEFAULT_COMMIT_LIMIT = 10;
    private static final int MAX_COMMIT_LIMIT = 50;

    private final AppMapper appMapper;
    private final RepoClient repoClient;

    @Override
    public List<String> branches(Long appId) {
        App app = requireApp(appId);
        List<String> branches = new ArrayList<>(repoClient.listBranches(app));
        String defaultBranch = app.getRepoBranch();
        // 默认分支排首位，其余按字母序，方便弹窗下拉直接选中
        branches.sort(Comparator.comparing((String branch) -> !branch.equals(defaultBranch))
                .thenComparing(Comparator.naturalOrder()));
        return branches;
    }

    @Override
    public List<RepoCommitVO> commits(Long appId, String branch, Integer limit) {
        if (StrUtil.isBlank(branch)) {
            throw new ServiceException("请先选择分支");
        }
        int size = limit == null ? DEFAULT_COMMIT_LIMIT : Math.min(Math.max(limit, 1), MAX_COMMIT_LIMIT);
        App app = requireApp(appId);
        return repoClient.listCommits(app, branch.trim(), size).stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    private App requireApp(Long appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
        if (StrUtil.isBlank(app.getRepoUrl())) {
            throw new ServiceException("应用未登记仓库地址");
        }
        return app;
    }

    private RepoCommitVO toVo(RepoCommit commit) {
        RepoCommitVO vo = new RepoCommitVO();
        vo.setSha(commit.sha());
        vo.setShortSha(commit.shortSha());
        vo.setMessage(commit.message());
        vo.setAuthorName(commit.authorName());
        vo.setCommittedAt(commit.committedAt());
        return vo;
    }
}
