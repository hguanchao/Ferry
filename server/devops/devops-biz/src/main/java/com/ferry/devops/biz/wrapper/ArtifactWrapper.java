package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.ArtifactQuery;
import com.ferry.devops.api.entity.Artifact;
import com.ferry.devops.api.vo.ArtifactVO;

/**
 * 制品列表查询与 VO 转换。制品用 delete_seq 而非 is_deleted，
 * 因此这里显式追加 delete_seq = 0（除非查询要求包含已删除）。
 */
public class ArtifactWrapper extends BaseEntityWrapper<Artifact, ArtifactVO> {

    public static ArtifactWrapper build() {
        return new ArtifactWrapper();
    }

    @Override
    public ArtifactVO vo(Artifact entity) {
        return BeanUtil.copy(entity, ArtifactVO.class);
    }

    /**
     * 构造查询条件：默认只查 delete_seq = 0 的有效制品。
     */
    public LambdaQueryWrapper<Artifact> lambda(ArtifactQuery query) {
        LambdaQueryWrapper<Artifact> wrapper = Wrappers.<Artifact>lambdaQuery()
                .eq(!Boolean.TRUE.equals(query.getIncludeDeleted()), Artifact::getDeleteSeq, 0L)
                .eq(ObjUtil.isNotEmpty(query.getAppId()), Artifact::getAppId, query.getAppId())
                .in(CollUtil.isNotEmpty(query.getForms()), Artifact::getForm, query.getForms())
                .like(StrUtil.isNotBlank(query.getVersion()), Artifact::getVersion, query.getVersion())
                .like(StrUtil.isNotBlank(query.getBranch()), Artifact::getBranch, query.getBranch())
                .eq(ObjUtil.isNotEmpty(query.getProdUseStatus()), Artifact::getProdUseStatus, query.getProdUseStatus())
                .eq(ObjUtil.isNotEmpty(query.getBuilder()), Artifact::getBuilder, query.getBuilder())
                .ge(ObjUtil.isNotEmpty(query.getBuildStart()), Artifact::getCreateTime, query.getBuildStart())
                .le(ObjUtil.isNotEmpty(query.getBuildEnd()), Artifact::getCreateTime, query.getBuildEnd())
                .orderByDesc(Artifact::getCreateTime);
        if (ObjUtil.isNotEmpty(query.getEnvId())) {
            wrapper.inSql(Artifact::getId,
                    "SELECT artifact_id FROM dev_artifact_env WHERE env_id = " + query.getEnvId());
        }
        return wrapper;
    }
}
