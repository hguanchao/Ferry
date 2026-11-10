package com.ferry.system.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.system.api.dto.query.DictQuery;
import com.ferry.system.api.entity.Dict;
import com.ferry.system.api.vo.DictVO;

public class DictWrapper extends BaseEntityWrapper<Dict, DictVO> {
    public static DictWrapper build() {
        return new DictWrapper();
    }

    @Override
    public DictVO vo(Dict entity) {
        return BeanUtil.copy(entity, DictVO.class);
    }

    public LambdaQueryWrapper<Dict> lambda(DictQuery query) {
        Long parentId = query.getParentId();
        String code = query.getCode();
        String label = query.getLabel();
        return Wrappers.<Dict>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(parentId), Dict::getParentId, parentId)
                .like(StrUtil.isNotBlank(code), Dict::getCode, code)
                .like(StrUtil.isNotBlank(label), Dict::getLabel, label)
                .orderByAsc(Dict::getSort).orderByDesc(Dict::getCreateTime);
    }
}
