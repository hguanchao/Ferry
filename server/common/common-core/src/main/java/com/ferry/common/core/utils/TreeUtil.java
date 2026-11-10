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
     * 转换为树形列表
     *
     * @param nodes 子节点列表
     * @return TreeNode
     */
    public static <T extends TreeNode> List<T> build(List<T> nodes) {
        return build(nodes, SysConst.TOP_PARENT_ID);
    }

    /**
     * 转换为树形列表
     *
     * @param nodes    子节点列表
     * @param parentId 父节点id
     * @return TreeNode
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
