/**
 * 树结构通用操作
 *
 * 部门、菜单、字典、区域都是同构的父子结构，页面里各写一遍递归既容易发散，
 * 也容易在「找节点」「取名称」这类小事上出现不一致，这里统一收口。
 */
export interface TreeLike<T> {
  children?: T[]
}

export function flattenTree<T extends TreeLike<T>>(nodes: T[], acc: T[] = []): T[] {
  nodes.forEach((node) => {
    acc.push(node)
    if (node.children?.length) flattenTree(node.children, acc)
  })
  return acc
}

export function countTree<T extends TreeLike<T>>(nodes: T[]): number {
  return nodes.reduce((total, node) => total + 1 + (node.children?.length ? countTree(node.children) : 0), 0)
}

export function findInTree<T extends TreeLike<T>>(nodes: T[], predicate: (node: T) => boolean): T | null {
  for (const node of nodes) {
    if (predicate(node)) return node
    if (node.children?.length) {
      const found = findInTree(node.children, predicate)
      if (found) return found
    }
  }
  return null
}

/** 按 id 找节点，名字回显用 */
export function findInTreeById<T extends TreeLike<T> & { id: string }>(nodes: T[], id?: string | null): T | null {
  if (!id) return null
  return findInTree(nodes, (node) => node.id === id)
}
