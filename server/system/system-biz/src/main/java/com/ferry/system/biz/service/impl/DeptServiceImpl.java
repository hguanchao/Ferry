package com.ferry.system.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.constant.StrConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.core.utils.TreeUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.system.api.dto.DeptDTO;
import com.ferry.system.api.dto.query.DeptQuery;
import com.ferry.system.api.entity.Dept;
import com.ferry.system.api.vo.tree.DeptTree;
import com.ferry.system.biz.mapper.DeptMapper;
import com.ferry.system.biz.service.IDeptService;
import com.ferry.system.biz.wrapper.DeptWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static com.ferry.common.core.constant.SysConst.TOP_PARENT_ID;

@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements IDeptService {

    @Override
    public IPage<DeptTree> pageList(DeptQuery body) {
        DeptWrapper wrapper = DeptWrapper.build();
        IPage<Dept> pages = page(Page.create(body), wrapper.lambda(body));
        return wrapper.pageVo(pages);
    }

    @Override
    public List<DeptTree> treeList(DeptQuery body) {
        List<Dept> list = list(DeptWrapper.build().lambda(body));
        List<DeptTree> deptList = BeanUtil.copy(list, DeptTree.class);
        List<DeptTree> treeList = TreeUtil.build(deptList);
        return CollUtil.isNotEmpty(treeList) ? treeList : deptList;
    }

    @Override
    public DeptTree detail(Long id) {
        Dept dept = getById(id);
        if (ObjUtil.isEmpty(dept)) {
            throw new ServiceException("部门不存在");
        }
        return BeanUtil.copy(dept, DeptTree.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(DeptDTO body) {
        Dept dept = BeanUtil.copy(body, Dept.class);
        Long parentId = dept.getParentId();
        // 前端「根部门」固定传 parentId=0，0 不是真实部门id，不能拿去查库
        if (TreeUtil.isRoot(parentId)) {
            parentId = TOP_PARENT_ID;
            // 空串能写进库，null 会被更新策略跳过，导致改成根部门后旧路径还在
            dept.setAncestors(StrConst.EMPTY);
        } else {
            Dept parentDept = getById(parentId);
            if (ObjUtil.isEmpty(parentDept)) {
                throw new ServiceException("上级部门不存在");
            }
            dept.setAncestors(StrUtil.isNotBlank(parentDept.getAncestors())
                    ? parentDept.getAncestors() + StrConst.COMMA + parentId
                    : parentId.toString());
        }
        dept.setParentId(parentId);
        return saveOrUpdate(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的部门");
        }
        // 上级部门被删除后下级会从树上脱节，先拦住
        long childCount = count(Wrappers.<Dept>lambdaQuery().in(Dept::getParentId, ids));
        if (childCount > IntConst.ZERO) {
            throw new ServiceException("存在下级部门，请先删除下级部门");
        }
        // 用户表按部门id关联，删部门后这些 id 会悬空，先拦住
        for (Long id : ids) {
            long userCount = baseMapper.selectUserCountById(id);
            if (userCount > IntConst.ZERO) {
                throw new ServiceException("部门下存在用户，请先调整用户所属部门");
            }
        }
        return removeByIds(ids);
    }

    @Override
    public List<Long> selfAndChildIds(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        return baseMapper.selectSelfAndChildIds(deptId);
    }
}
