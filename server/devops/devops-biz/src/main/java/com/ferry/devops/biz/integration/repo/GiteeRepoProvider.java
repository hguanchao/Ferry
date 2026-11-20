package com.ferry.devops.biz.integration.repo;

import com.ferry.common.core.utils.StrUtil;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Gitee 仓库查询：/api/v5/repos/{owner}/{repo}/{branches|commits}。
 *
 * <p>Gitee OpenAPI 只支持 access_token 查询参数；{@link com.ferry.common.core.utils.HttpUtil}
 * 落日志前会打码该参数，避免令牌泄露。
 */
@Component
public class GiteeRepoProvider extends AbstractRepoProvider {

    private static final int REPO_TYPE = 3;

    @Override
    public int repoType() {
        return REPO_TYPE;
    }

    @Override
    public List<String> listBranches(RepoTarget target) {
        String url = apiBase(target) + "/repos/" + target.projectPath()
                + "/branches?per_page=" + MAX_BRANCHES + tokenParam(target);
        JsonNode root = getJson(url, null, "分支列表");
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
                + "/commits?sha=" + encode(branch) + "&per_page=" + limit + tokenParam(target);
        JsonNode root = getJson(url, null, "提交列表");
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
        return "https://" + target.authority() + "/api/v5";
    }

    private String tokenParam(RepoTarget target) {
        return StrUtil.isBlank(target.token()) ? "" : "&access_token=" + encode(target.token());
    }
}
