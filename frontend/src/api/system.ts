import { http } from './http'
import type { ApiResult } from './auth'

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface UserItem {
  id: number
  username: string
  realName: string
  status: string
  roles: string[]
  lastLoginTime?: string
  createdAt?: string
}

export interface RoleItem {
  id: number
  roleCode: string
  roleName: string
  description?: string
  permissions: string[]
  createdAt?: string
}

export interface PermissionNode {
  label: string
  value: string
  children?: PermissionNode[]
}

export function listUsers(params: { current: number; size: number; keyword?: string }) {
  return http.get<ApiResult<PageResult<UserItem>>>('/admin/users', { params })
}

export function createUser(payload: { username: string; password: string; realName: string; roles: string[]; status: string }) {
  return http.post<ApiResult<UserItem>>('/admin/users', payload)
}

export function updateUser(id: number, payload: { password?: string; realName: string; roles: string[]; status: string }) {
  return http.put<ApiResult<UserItem>>(`/admin/users/${id}`, payload)
}

export function deleteUser(id: number) {
  return http.delete<ApiResult<null>>(`/admin/users/${id}`)
}

export function listRoles() {
  return http.get<ApiResult<RoleItem[]>>('/admin/roles')
}

export function listPermissionTree() {
  return http.get<ApiResult<PermissionNode[]>>('/admin/permissions/tree')
}

export function createRole(payload: { roleCode: string; roleName: string; description?: string; permissions: string[] }) {
  return http.post<ApiResult<RoleItem>>('/admin/roles', payload)
}

export function updateRole(id: number, payload: { roleName: string; description?: string; permissions: string[] }) {
  return http.put<ApiResult<RoleItem>>(`/admin/roles/${id}`, payload)
}

export function deleteRole(id: number) {
  return http.delete<ApiResult<null>>(`/admin/roles/${id}`)
}
