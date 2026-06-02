import { http } from './http'
import type { ApiResult } from './auth'
import type { PageResult } from './system'

export interface MerchantPoolItem {
  id: number
  tenantId: string
  poolName: string
  poolCode: string
  appId?: string
  appSecretMasked?: string
  plainAppSecret?: string
  status: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface MerchantAccountItem {
  id: number
  tenantId: string
  poolId: number
  poolName: string
  accountName: string
  channelCode?: string
  apiKeyMasked?: string
  signKeyMasked?: string
  privateKeyMasked?: string
  publicKeyMasked?: string
  certPath?: string
  certPasswordMasked?: string
  extraConfigJson?: string
  supportPayMethods: string
  priority: number
  weight: number
  dailyAmountLimit?: number
  monthlyAmountLimit?: number
  singleMinAmount?: number
  singleMaxAmount?: number
  availableStartDate?: string
  availableEndDate?: string
  availableStartTime?: string
  availableEndTime?: string
  status: string
  failCount: number
  lastFailTime?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface MerchantAppItem {
  id: number
  tenantId: string
  poolId: number
  poolName: string
  appId: string
  appName: string
  secretMasked?: string
  plainSecret?: string
  notifyUrlWhitelist?: string
  rateLimitPerMinute: number
  status: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface MerchantAppPayload {
  tenantId?: string
  poolId: number
  appId?: string
  appName: string
  notifyUrlWhitelist?: string
  rateLimitPerMinute?: number
  status?: string
  remark?: string
}

export interface MerchantAccountPayload {
  tenantId?: string
  poolId?: number
  accountName: string
  channelCode?: string
  apiKey?: string
  signKey?: string
  privateKey?: string
  publicKey?: string
  certPath?: string
  certPassword?: string
  extraConfigJson?: string
  supportPayMethods?: string
  priority?: number
  weight?: number
  dailyAmountLimit?: number
  monthlyAmountLimit?: number
  singleMinAmount?: number
  singleMaxAmount?: number
  availableStartDate?: string
  availableEndDate?: string
  availableStartTime?: string
  availableEndTime?: string
  status?: string
  remark?: string
}

export function listMerchantPools(params: { current: number; size: number; keyword?: string; status?: string }) {
  return http.get<ApiResult<PageResult<MerchantPoolItem>>>('/admin/merchant-pools', { params })
}

export function createMerchantPool(payload: { tenantId?: string; poolName: string; poolCode?: string; status: string; remark?: string }) {
  return http.post<ApiResult<MerchantPoolItem>>('/admin/merchant-pools', payload)
}

export function updateMerchantPool(id: number, payload: { poolName: string; status: string; remark?: string }) {
  return http.put<ApiResult<MerchantPoolItem>>(`/admin/merchant-pools/${id}`, payload)
}

export function deleteMerchantPool(id: number) {
  return http.delete<ApiResult<null>>(`/admin/merchant-pools/${id}`)
}

export function resetMerchantPoolAppSecret(id: number) {
  return http.post<ApiResult<MerchantPoolItem>>(`/admin/merchant-pools/${id}/reset-app-secret`)
}

export function listMerchantApps(params: { current: number; size: number; keyword?: string; poolId?: number; status?: string }) {
  return http.get<ApiResult<PageResult<MerchantAppItem>>>('/admin/merchant-apps', { params })
}

export function createMerchantApp(payload: MerchantAppPayload) {
  return http.post<ApiResult<MerchantAppItem>>('/admin/merchant-apps', payload)
}

export function updateMerchantApp(id: number, payload: MerchantAppPayload) {
  return http.put<ApiResult<MerchantAppItem>>(`/admin/merchant-apps/${id}`, payload)
}

export function deleteMerchantApp(id: number) {
  return http.delete<ApiResult<null>>(`/admin/merchant-apps/${id}`)
}

export function resetMerchantAppSecret(id: number) {
  return http.post<ApiResult<MerchantAppItem>>(`/admin/merchant-apps/${id}/reset-secret`)
}

export function listMerchantAccounts(params: { current: number; size: number; keyword?: string; poolId?: number; status?: string }) {
  return http.get<ApiResult<PageResult<MerchantAccountItem>>>('/admin/merchant-accounts', { params })
}

export function getMerchantAccount(id: number) {
  return http.get<ApiResult<MerchantAccountItem>>(`/admin/merchant-accounts/${id}`)
}

export function createMerchantAccount(payload: MerchantAccountPayload) {
  return http.post<ApiResult<MerchantAccountItem>>('/admin/merchant-accounts', payload)
}

export function updateMerchantAccount(id: number, payload: MerchantAccountPayload) {
  return http.put<ApiResult<MerchantAccountItem>>(`/admin/merchant-accounts/${id}`, payload)
}

export function deleteMerchantAccount(id: number) {
  return http.delete<ApiResult<null>>(`/admin/merchant-accounts/${id}`)
}

export function enableMerchantAccount(id: number) {
  return http.post<ApiResult<MerchantAccountItem>>(`/admin/merchant-accounts/${id}/enable`)
}

export function disableMerchantAccount(id: number) {
  return http.post<ApiResult<MerchantAccountItem>>(`/admin/merchant-accounts/${id}/disable`)
}
