package com.ferry.devops.biz.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.ArtifactCleanPolicyDTO;
import com.ferry.devops.api.entity.Artifact;
import com.ferry.devops.api.entity.ArtifactCleanItem;
import com.ferry.devops.api.entity.ArtifactCleanPolicy;
import com.ferry.devops.api.entity.ArtifactCleanTask;
import com.ferry.devops.api.vo.CleanPolicyVO;
import com.ferry.devops.api.vo.CleanTaskVO;
import com.ferry.devops.biz.mapper.ArtifactCleanItemMapper;
import com.ferry.devops.biz.mapper.ArtifactCleanPolicyMapper;
import com.ferry.devops.biz.mapper.ArtifactCleanTaskMapper;
import com.ferry.devops.biz.mapper.ArtifactMapper;
import com.ferry.devops.biz.service.IArtifactCleanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 制品清理服务实现：策略配置 + 生成任务清单 + 软删除标记。
 * 最小实现：只生成清单与标记，不做真实物理删除定时任务；物理删除由独立任务到期后执行。
 */
@Service
@RequiredArgsConstructor
public class ArtifactCleanServiceImpl extends ServiceImpl<ArtifactCleanPolicyMapper, ArtifactCleanPolicy>
        implements IArtifactCleanService {

    private final ArtifactMapper artifactMapper;
    private final ArtifactCleanTaskMapper taskMapper;
    private final ArtifactCleanItemMapper itemMapper;

    @Override
    public CleanPolicyVO policy(Long appId) {
        ArtifactCleanPolicy policy = baseMapper.findByApp(appId);
        if (policy == null) {
            // 未配置时返回默认值，便于页面展示默认表单
            CleanPolicyVO vo = new CleanPolicyVO();
            vo.setAppId(appId);
            vo.setKeepLatest(20);
            vo.setIsKeepProd(IntConst.ONE);
            vo.setSoftDeleteDays(30);
            vo.setIsAutoExec(IntConst.ZERO);
            return vo;
        }
        return BeanUtil.copy(policy, CleanPolicyVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean savePolicy(ArtifactCleanPolicyDTO body) {
        if (body.getKeepLatest() <= IntConst.ZERO) {
            throw new ServiceException("保留数必须大于 0");
        }
        if (body.getSoftDeleteDays() <= IntConst.ZERO) {
            throw new ServiceException("软删除天数必须大于 0");
        }
        ArtifactCleanPolicy policy = BeanUtil.copy(body, ArtifactCleanPolicy.class);
        if (ObjUtil.isEmpty(body.getId())) {
            ArtifactCleanPolicy exists = baseMapper.findByApp(body.getAppId());
            if (exists != null) {
                throw new ServiceException("该应用已存在清理策略");
            }
            return save(policy);
        }
        return updateById(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean run(Long appId, Boolean confirm) {
        ArtifactCleanPolicy policy = baseMapper.findByApp(appId);
        if (policy == null) {
            throw new ServiceException("请先配置清理策略");
        }
        int keepLatest = policy.getKeepLatest() == null ? 20 : policy.getKeepLatest();
        boolean keepProd = ObjUtil.equals(policy.getIsKeepProd(), IntConst.ONE);

        // 保留集合 = 最近 N 个 ∪ 生产用过的
        List<Artifact> candidates = artifactMapper.cleanCandidates(appId);
        Set<Long> keep = candidates.stream().limit(keepLatest).map(Artifact::getId).collect(Collectors.toSet());
        if (keepProd) {
            keep.addAll(artifactMapper.prodUsedArtifactIds(appId));
        }

        List<Artifact> toClean = candidates.stream().filter(a -> !keep.contains(a.getId())).collect(Collectors.toList());

        ArtifactCleanTask task = new ArtifactCleanTask();
        task.setAppId(appId);
        task.setTaskTime(new Date());
        task.setPendingCount(toClean.size());
        task.setDeletedCount(IntConst.ZERO);
        task.setOperator(AuthUtil.getUserId());
        List<ArtifactCleanItem> items = new ArrayList<>();
        for (Artifact a : toClean) {
            ArtifactCleanItem item = new ArtifactCleanItem();
            item.setArtifactId(a.getId());
            item.setVersion(a.getVersion());
            item.setDigest(a.getDigest());
            item.setSize(a.getSize());
            items.add(item);
        }
        int deleted = IntConst.ZERO;
        if (Boolean.TRUE.equals(confirm)) {
            // 软删除：delete_seq=id、deleted_at=now、来源=清理任务
            for (Artifact a : toClean) {
                Artifact update = new Artifact();
                update.setId(a.getId());
                update.setDeleteSeq(a.getId());
                update.setDeletedAt(new Date());
                update.setDeleteSource(IntConst.TWO);
                artifactMapper.updateById(update);
                deleted++;
            }
        }
        task.setDeletedCount(deleted);
        task.setStatus(Boolean.TRUE.equals(confirm) ? IntConst.TWO : IntConst.ONE);
        task.setManifestFile("clean-task-" + appId + "-" + System.currentTimeMillis() + ".json");
        taskMapper.insert(task);
        for (ArtifactCleanItem item : items) {
            item.setTaskId(task.getId());
            itemMapper.insert(item);
        }
        return true;
    }

    @Override
    public IPage<CleanTaskVO> taskPage(Long appId, Integer current, Integer size) {
        IPage<ArtifactCleanTask> pages = taskMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        current == null ? 1 : current, size == null ? 30 : size),
                Wrappers.<ArtifactCleanTask>lambdaQuery()
                        .eq(ObjUtil.isNotEmpty(appId), ArtifactCleanTask::getAppId, appId)
                        .orderByDesc(ArtifactCleanTask::getTaskTime));
        IPage<CleanTaskVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pages.getCurrent(), pages.getSize(), pages.getTotal());
        result.setRecords(BeanUtil.copy(pages.getRecords(), CleanTaskVO.class));
        return result;
    }

    @Override
    public String manifest(Long taskId) {
        ArtifactCleanTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("清理任务不存在");
        }
        List<ArtifactCleanItem> items = itemMapper.selectList(Wrappers.<ArtifactCleanItem>lambdaQuery()
                .eq(ArtifactCleanItem::getTaskId, taskId));
        return JsonUtil.toJson(items);
    }
}
