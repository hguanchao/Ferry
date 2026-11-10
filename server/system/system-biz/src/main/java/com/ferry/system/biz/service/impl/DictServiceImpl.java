package com.ferry.system.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.TreeUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.system.api.dto.DictDTO;
import com.ferry.system.api.dto.query.DictQuery;
import com.ferry.system.api.entity.Dict;
import com.ferry.system.api.vo.DictVO;
import com.ferry.system.api.vo.tree.DictTree;
import com.ferry.system.biz.mapper.DictMapper;
import com.ferry.system.biz.service.IDictService;
import com.ferry.system.biz.wrapper.DictWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ferry.common.core.constant.SysConst.TOP_PARENT_ID;

@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements IDictService {
    @Override
    public List<DictTree> treeList(DictQuery body) {
        List<Dict> list = list(DictWrapper.build().lambda(body));
        List<DictTree> dictList = BeanUtil.copy(list, DictTree.class);
        List<DictTree> treeList = TreeUtil.build(dictList);
        return CollUtil.isNotEmpty(treeList) ? treeList : dictList;
    }

    @Override
    public IPage<DictVO> pageList(DictQuery body) {
        DictWrapper wrapper = DictWrapper.build();
        IPage<Dict> pages = page(Page.create(body), wrapper.lambda(body));
        return wrapper.pageVo(pages);
    }

    @Override
    public DictTree detail(Long id) {
        Dict dict = getById(id);
        if (ObjUtil.isEmpty(dict)) {
            throw new ServiceException("字典不存在");
        }
        return BeanUtil.copy(dict, DictTree.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(DictDTO body) {
        Dict dict = BeanUtil.copy(body, Dict.class);
        // 前端「根分组」固定传 parentId=0，0 不是真实字典id，不能拿去查库
        if (TreeUtil.isRoot(dict.getParentId())) {
            dict.setParentId(TOP_PARENT_ID);
            // 字典码属于分组：分组之间必须唯一，否则按编码取字典会命中多个分组
            long count = count(Wrappers.<Dict>lambdaQuery().eq(Dict::getParentId, TOP_PARENT_ID)
                    .eq(Dict::getCode, dict.getCode()).ne(ObjUtil.isNotEmpty(dict.getId()), Dict::getId, dict.getId()));
            if (count > IntConst.ZERO) {
                throw new ServiceException("字典编码[" + dict.getCode() + "]已存在");
            }
        } else {
            // 字典项没有独立字典码，统一继承所属分组；前端不再提供该字段
            Dict parent = getById(dict.getParentId());
            // 字典只有「分组 → 字典项」两层：父级本身也得挂在根上，否则第三层躲过删除
            if (ObjUtil.isEmpty(parent) || !TreeUtil.isRoot(parent.getParentId())) {
                throw new ServiceException("字典项只能挂在字典分组下");
            }
            dict.setCode(parent.getCode());
        }
        boolean isSuccess = saveOrUpdate(dict);
        boolean isGroupEdit = TreeUtil.isRoot(dict.getParentId()) && ObjUtil.isNotEmpty(dict.getId());
        if (isSuccess && isGroupEdit) {
            // 分组改码时字典项的 code 要跟着换，否则按编码查字典会漏掉仍挂旧码的子项
            update(Wrappers.<Dict>lambdaUpdate().eq(Dict::getParentId, dict.getId())
                    .ne(Dict::getCode, dict.getCode()).set(Dict::getCode, dict.getCode()));
        }
        return isSuccess;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的字典");
        }
        boolean isSuccess = removeByIds(ids);
        if (isSuccess) {
            remove(Wrappers.<Dict>lambdaQuery().in(Dict::getParentId, ids));
        }
        return isSuccess;
    }
}
