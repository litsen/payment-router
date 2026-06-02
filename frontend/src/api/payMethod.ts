import { http } from './http'
import type { ApiResult } from './auth'

export interface PayMethodItem {
  id: number
  tenantId: string
  methodCode: string
  methodName: string
  enabled: boolean
  reserved: boolean
  sortOrder: number
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export function listPayMethods() {
  return http.get<ApiResult<PayMethodItem[]>>('/admin/pay-methods')
}

export function updatePayMethod(id: number, payload: { methodName: string; enabled: boolean; sortOrder?: number; remark?: string }) {
  return http.put<ApiResult<PayMethodItem>>(`/admin/pay-methods/${id}`, payload)
}

export function enablePayMethod(id: number) {
  return http.post<ApiResult<PayMethodItem>>(`/admin/pay-methods/${id}/enable`)
}

export function disablePayMethod(id: number) {
  return http.post<ApiResult<PayMethodItem>>(`/admin/pay-methods/${id}/disable`)
}
