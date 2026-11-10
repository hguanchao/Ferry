package com.ferry.system.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.TreeUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.system.api.dto.MenuDTO;
import com.ferry.system.api.dto.query.MenuQuery;
import com.ferry.system.api.entity.Menu;
import com.ferry.system.api.enums.MenuTypeEnum;
import com.ferry.system.api.vo.tree.MenuTree;
import com.ferry.system.biz.mapper.MenuMapper;
import com.ferry.system.biz.service.IMenuService;
import com.ferry.system.biz.service.IRoleMenuService;
import com.ferry.system.biz.wrapper.MenuWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ferry.common.core.constant.SysConst.TOP_PARENT_ID;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements IMenuService {

    @Override
    public List<MenuTree> treeList(MenuQuery body) {
        List<Menu> list = list(MenuWrapper.build().lambda(body));
        List<MenuTree> menuList = BeanUtil.copy(list, MenuTree.class);
        List<MenuTree> treeList = TreeUtil.build(menuList);
        return CollUtil.isNotEmpty(treeList) ? treeList : menuList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(MenuDTO body) {
        // 类型必须命中枚举，否则脏值会进菜单树
        if (!MenuTypeEnum.contains(body.getType())) {
            throw new ServiceException("菜单类型仅支持目录、菜单、按钮");
        }
        Long parentId = body.getParentId();
        // 前端「根菜单」固定传 parentId=0，0 不是真实菜单id，不能拿去查库
        if (TreeUtil.isRoot(parentId)) {
            body.setParentId(TOP_PARENT_ID);
        } else {
            if (ObjUtil.equals(parentId, body.getId())) {
                throw new ServiceException("上级菜单不能是自身");
            }
            if (ObjUtil.isEmpty(getById(parentId))) {
                throw new ServiceException("上级菜单不存在");
            }
        }
        return saveOrUpdate(BeanUtil.copy(body, Menu.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的菜单");
        }
        // 父菜单被删除后子菜单会从树上脱节，先拦住
        long childCount = count(Wrappers.<Menu>lambdaQuery().in(Menu::getParentId, ids));
        if (childCount > IntConst.ZERO) {
            throw new ServiceException("存在子菜单，请先删除子菜单");
        }
        return removeByIds(ids);
    }
}
