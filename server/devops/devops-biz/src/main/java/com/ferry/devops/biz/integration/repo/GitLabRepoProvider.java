package com.ferry.devops.biz.integration.repo;

import com.ferry.common.core.utils.StrUtil;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GitLab 仓库查询：/api/v4/projects/{project}/repository/{branches|commits}。
 */
@Component
public class GitLabRepoProvider extends AbstractRepoProvider {

    private static final int REPO_TYPE = 1;

    @Override
    public int repoType() {
        return REPO_TYPE;
    }

    @Override
    public List<String> listBranches(RepoTarget target) {
        String url = apiBase(target) + "/projects/" + encode(target.projectPath())
                + "/repository/branches?per_page=" + MAX_BRANCHES;
        return parseBranches(getJson(url, headers(target), "分支列表"));
    }

    @Override
    public List<RepoCommit> listCommits(RepoTarget target, String branch, int limit) {
        String url = apiBase(target) + "/projects/" + encode(target.projectPath())
                + "/repository/commits?ref_name=" + encode(branch) + "&per_page=" + limit;
        JsonNode root = getJson(url, headers(target), "提交列表");
        List<RepoCommit> commits = new ArrayList<>();
        if (root == null || !root.isArray()) {
            return commits;
        }
        for (JsonNode item : root) {
            String sha = text(item, "id");
            String shortSha = text(item, "short_id");
            commits.add(new RepoCommit(sha, shortSha.isBlank() ? shortSha(sha) : shortSha,
                    firstLine(text(item, "title")), text(item, "author_name"), text(item, "authored_date")));
        }
        return commits;
    }

    private List<String> parseBranches(JsonNode root) {
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

    private String apiBase(RepoTarget target) {
        return target.scheme() + "://" + target.authority() + "/api/v4";
    }

    private Map<String, String> headers(RepoTarget target) {
        Map<String, String> headers = new HashMap<>();
        if (StrUtil.isNotBlank(target.token())) {
            headers.put("PRIVATE-TOKEN", target.token());
        }
        return headers;
    }
}
