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

export interface CaptchaResponse {
  required: boolean
  captchaId?: string
  imageBase64?: string
}

export interface LoginSecurityStatusResponse {
  captchaRequired: boolean
  locked: boolean
}

export function loginApi(payload: { username: string; password: string; captchaId?: string; captchaCode?: string }) {
  return http.post<ApiResult<LoginResponse>>('/admin/auth/login', payload)
}

export function loginStatusApi(username: string) {
  return http.get<ApiResult<LoginSecurityStatusResponse>>('/admin/auth/login-status', { params: { username } })
}

export function captchaApi(username: string) {
  return http.get<ApiResult<CaptchaResponse>>('/admin/auth/captcha', { params: { username } })
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
