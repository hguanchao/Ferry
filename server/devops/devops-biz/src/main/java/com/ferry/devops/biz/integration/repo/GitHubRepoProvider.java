package com.ferry.devops.biz.integration.repo;

import com.ferry.common.core.utils.StrUtil;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GitHub 仓库查询：github.com 走 api.github.com，企业版走 /api/v3。
 */
@Component
public class GitHubRepoProvider extends AbstractRepoProvider {

    private static final int REPO_TYPE = 2;
    private static final Set<String> PUBLIC_HOSTS = Set.of("github.com", "www.github.com");

    @Override
    public int repoType() {
        return REPO_TYPE;
    }

    @Override
    public List<String> listBranches(RepoTarget target) {
        String url = apiBase(target) + "/repos/" + target.projectPath()
                + "/branches?per_page=" + MAX_BRANCHES;
        JsonNode root = getJson(url, headers(target), "分支列表");
        List<String> branches = new ArrayList<>();
        if (root == null || !root.isArray()) {
            return branches;
        }
        for (JsonNode item : root) {
            String name = text(item, "name");
            if (!name.isBlank()) {
                branches.add(name);
            }
        }
        return branches;
    }

    @Override
    public List<RepoCommit> listCommits(RepoTarget target, String branch, int limit) {
        String url = apiBase(target) + "/repos/" + target.projectPath()
                + "/commits?sha=" + encode(branch) + "&per_page=" + limit;
        JsonNode root = getJson(url, headers(target), "提交列表");
        List<RepoCommit> commits = new ArrayList<>();
        if (root == null || !root.isArray()) {
            return commits;
        }
        for (JsonNode item : root) {
            String sha = text(item, "sha");
            commits.add(new RepoCommit(sha, shortSha(sha),
                    firstLine(nestedText(item, "commit", "message")),
                    nestedText(item, "commit", "author", "name"),
                    nestedText(item, "commit", "author", "date")));
        }
        return commits;
    }

    private String apiBase(RepoTarget target) {
        if (PUBLIC_HOSTS.contains(target.host().toLowerCase())) {
            return "https://api.github.com";
        }
        // 企业版固定 HTTPS，避免令牌经明文链路传输
        return "https://" + target.authority() + "/api/v3";
    }

    private Map<String, String> headers(RepoTarget target) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/vnd.github+json");
        headers.put("X-GitHub-Api-Version", "2022-11-28");
        if (StrUtil.isNotBlank(target.token())) {
            headers.put("Authorization", "Bearer " + target.token());
        }
        return headers;
    }
}
