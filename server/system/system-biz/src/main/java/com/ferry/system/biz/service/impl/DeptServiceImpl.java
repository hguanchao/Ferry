package com.ferry.system.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
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
import com.ferry.system.api.entity.Menu;
import com.ferry.system.api.vo.tree.DeptTree;
import com.ferry.system.biz.mapper.DeptMapper;
import com.ferry.system.biz.service.IDeptService;
import com.ferry.system.biz.wrapper.DeptWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        // 把父级不在结果里的可见节点提成根，否则被数据权限裁剪过的节点建树时会塌空
        Set<Long> ids = deptList.stream().map(DeptTree::getId).collect(Collectors.toSet());
        deptList.stream()
                .filter(item -> !TreeUtil.isRoot(item.getParentId()) && !ids.contains(item.getParentId()))
                .forEach(item -> item.setParentId(TOP_PARENT_ID));
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
        Long id = body.getId();
        Long parentId = body.getParentId();
        long count = count(Wrappers.<Dept>lambdaQuery().eq(Dept::getParentId, parentId)
                .eq(Dept::getName, body.getName()).ne(ObjUtil.isNotEmpty(id), Dept::getId, id));
        if (count > 0) {
            throw new ServiceException("部门已存在");
        }
        Dept dept = BeanUtil.copy(body, Dept.class);
        if (TreeUtil.isRoot(parentId)) {
            dept.setAncestors(null);
        } else {
            Dept parent = getById(parentId);
            if (ObjUtil.isEmpty(parent)) {
                throw new ServiceException("上级部门不存在");
            }
            if (ObjUtil.isNotEmpty(id)
                    && baseMapper.selectSelfAndChildIdsByDeptIds(List.of(id)).contains(parentId)) {
                throw new ServiceException("上级部门不能是自身或自身的下级");
            }
            dept.setAncestors(StrUtil.isNotBlank(parent.getAncestors())
                    ? parent.getAncestors() + StrConst.COMMA + parent.getId() : StrUtil.toStr(parent.getId()));
        }
        boolean isSuccess = saveOrUpdate(dept);
        if (isSuccess) {
            refreshChildrenAncestors(dept);
        }
        return isSuccess;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的部门");
        }
        // 拦住已挂用户的部门，删除后用户归属会悬空
        long userCount = baseMapper.selectUserCountByIds(ids);
        if (userCount > IntConst.ZERO) {
            throw new ServiceException("部门下存在用户，请先调整用户所属部门");
        }
        boolean isSuccess = removeByIds(ids);
        if (isSuccess) {
            remove(Wrappers.<Dept>lambdaQuery().in(Dept::getParentId, ids));
        }
        return isSuccess;
    }

    @Override
    public List<Long> selfAndChildIds(List<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyList();
        }
        return baseMapper.selectSelfAndChildIdsByDeptIds(deptIds);
    }

    /**
     * 重算子孙部门的祖级列表，只绕过数据权限，租户条件仍生效
     *
     * @param dept 已保存的部门
     */
    private void refreshChildrenAncestors(Dept dept) {
        InterceptorIgnoreHelper.execute(IgnoreStrategy.builder().dataPermission(true).build(), () -> {
            doRefreshChildrenAncestors(dept);
            return null;
        });
    }

    /**
     * 重算子孙部门的祖级列表，每个子孙截掉旧前缀再接上新前缀，更深的子孙无需单独处理
     *
     * @param dept 已保存的部门，新路径为空表示已提到根部门
     */
    private void doRefreshChildrenAncestors(Dept dept) {
        String id = StrUtil.toStr(dept.getId());
        List<Dept> children = list(Wrappers.<Dept>lambdaQuery().like(Dept::getAncestors, id));
        String prefix = StrUtil.isNotBlank(dept.getAncestors()) ? dept.getAncestors() + StrConst.COMMA + id : id;
        boolean changed = false;
        for (Dept child : children) {
            String ancestors = child.getAncestors();
            // 前后补逗号再定位旧前缀，避免 12 命中 123；不含该段时跳过，以免改坏他人路径
            int start = (StrConst.COMMA + ancestors + StrConst.COMMA).indexOf(StrConst.COMMA + id + StrConst.COMMA);
            if (start < 0) {
                continue;
            }
            String newAncestors = prefix + ancestors.substring(start + id.length());
            if (!newAncestors.equals(ancestors)) {
                child.setAncestors(newAncestors);
                changed = true;
            }
        }
        if (changed) {
            updateBatchById(children);
        }

    }
}
