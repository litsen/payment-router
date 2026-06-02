import { http } from './http'
import type { ApiResult } from './auth'

export interface ApiDocErrorCode {
  code: string
  message: string
}

export interface ApiDocParameter {
  name: string
  type: string
  required: string
  description: string
  enums?: string
  example?: string
}

export interface ApiDoc {
  slug?: string
  title: string
  description: string
  requestMethod?: string
  requestPath?: string
  requestParams?: ApiDocParameter[]
  responseParams?: ApiDocParameter[]
  request?: string
  response?: string
  rules?: string
  errors?: ApiDocErrorCode[]
}

export interface ApiDocSummary {
  slug: string
  title: string
  path: string
}

export function listApiDocs(publicAccess = false) {
  return http.get<ApiResult<ApiDocSummary[]>>(publicAccess ? '/api/docs' : '/admin/api-docs')
}

export function getApiDoc(slug: string, publicAccess = false) {
  return http.get<ApiResult<ApiDoc>>(publicAccess ? `/api/docs/${slug}` : `/admin/api-docs/${slug}`)
}
