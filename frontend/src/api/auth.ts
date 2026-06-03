import { http } from './http'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface CurrentUser {
  id: number
  username: string
  realName: string
  roles: string[]
  permissions: string[]
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: CurrentUser
}

export function loginApi(payload: { username: string; password: string }) {
  return http.post<ApiResult<LoginResponse>>('/admin/auth/login', payload)
}

export function logoutApi() {
  return http.post<ApiResult<null>>('/admin/auth/logout')
}

export function changePasswordApi(payload: { oldPassword: string; newPassword: string }) {
  return http.post<ApiResult<null>>('/admin/auth/change-password', payload)
}

export function meApi() {
  return http.get<ApiResult<CurrentUser>>('/admin/auth/me')
}
