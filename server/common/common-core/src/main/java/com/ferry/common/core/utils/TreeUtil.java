package com.ferry.common.core.utils;

import com.ferry.common.core.constant.SysConst;
import com.ferry.common.core.model.node.TreeNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 树形数据工具类
 */
public final class TreeUtil {

    private TreeUtil() {
    }

    /**
     * 是否为根节点：上级传 null 或 0 都表示没有父节点
     *
     * @param parentId 父节点id
     * @return 是否根节点
     */
    public static boolean isRoot(Long parentId) {
        return ObjUtil.isEmpty(parentId) || SysConst.TOP_PARENT_ID.equals(parentId);
    }

    /**
     * 转换为树形列表，根节点从 0 开始判定
     *
     * @param nodes 节点平铺列表
     * @return 树形列表
     */
    public static <T extends TreeNode> List<T> build(List<T> nodes) {
        return build(nodes, SysConst.TOP_PARENT_ID);
    }

    /**
     * 转换为树形列表，从指定父节点开始挂载
     *
     * @param nodes    节点平铺列表
     * @param parentId 根判定用的父节点id
     * @return 树形列表
     */
    public static <T extends TreeNode> List<T> build(List<T> nodes, Object parentId) {
        if (CollUtil.isEmpty(nodes)) {
            return new ArrayList<>();
        }

        Map<Object, List<T>> childrenMap = new HashMap<>(nodes.size());
        for (T node : nodes) {
            node.setChildren(new ArrayList<>());
            childrenMap.computeIfAbsent(node.getParentId(), key -> new ArrayList<>()).add(node);
        }

        return buildChildren(childrenMap, parentId);
    }

    /**
     * 从子节点映射递归组装指定父节点的子级列表
     *
     * @param childrenMap 父节点id到子节点列表的映射
     * @param parentId    当前父节点id
     * @return 子级列表，没有子级时为空列表
     */
    private static <T extends TreeNode> List<T> buildChildren(Map<Object, List<T>> childrenMap, Object parentId) {
        List<T> children = childrenMap.get(parentId);
        if (CollUtil.isEmpty(children)) {
            return new ArrayList<>();
        }

        for (T child : children) {
            child.setChildren(buildChildren(childrenMap, child.getId()));
        }
        return children;
    }
}
