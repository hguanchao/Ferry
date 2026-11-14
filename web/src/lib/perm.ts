import type { AuthInfo } from '@/api/types'

/** 权限标识匹配：与后端 PreAuthorizeAspect 同口径，按 : 分段且段数一致，任一侧该段为 * 即通过 */
export function matchPerm(held?: string | null, required?: string | null): boolean {
  if (!held || !required) return false
  const left = held.split(':')
  const right = required.split(':')
  if (left.length !== right.length) return false
  return left.every((segment, index) => segment === '*' || right[index] === '*' || segment === right[index])
}

export function can(user: AuthInfo | null, required?: string | null): boolean {
  if (!user) return false
  if (user.isSuper === 1) return true
  if (!required) return true
  return user.permissions.some((perm) => matchPerm(perm.perms, required))
}

export function canAny(user: AuthInfo | null, required: Array<string | undefined>): boolean {
  if (!user) return false
  if (user.isSuper === 1) return true
  return required.some((item) => can(user, item))
}
