import { http } from './http'
import type { ApiResult } from './auth'
import type { PageResult } from './system'

export interface RouteRuleItem {
  id: number
  tenantId: string
  ruleName: string
  ruleCode: string
  poolId: number
  poolName: string
  payMethod: string
  ruleType: string
  ruleConfigJson?: string
  priority: number
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export interface RouteRulePayload {
  tenantId?: string
  ruleName: string
  ruleCode?: string
  poolId?: number
  payMethod: string
  ruleType: string
  ruleConfigJson?: string
  priority?: number
  enabled?: boolean
}

export interface RouteRecordItem {
  id: number
  tenantId: string
  orderId?: number
  merchantOrderNo?: string
  poolId: number
  poolName: string
  accountId: number
  accountName: string
  routeRuleId?: number
  routeRuleName?: string
  routeType: string
  routeSnapshotJson?: string
  amount: number
  createdAt?: string
}

export interface RouteTestResult {
  recordId: number
  accountId: number
  accountName: string
  poolId: number
  routeRuleId?: number
  routeType: string
  merchantOrderNo: string
  amount: number
  simulatedFailure: boolean
  message: string
}

export interface BarcodePayPayload {
  poolId: number
  payMethod: string
  merchantOrderNo: string
  amount: number
  authCode?: string
  subject: string
  notifyUrl?: string
  channel?: string
  returnUrl?: string
  subAppId?: string
  payerId?: string
}

export interface QueryPayPayload {
  poolId: number
  payMethod: string
  merchantOrderNo: string
}

export interface PayGatewayResult {
  appId: string
  merchantOrderNo: string
  amount: number
  payMethod: string
  status: string
  platformOrderNo?: string
  channelOrderNo?: string
  payData?: unknown
  message?: string
}

export function listRouteRules(params: { current: number; size: number; keyword?: string; poolId?: number; payMethod?: string; enabled?: boolean }) {
  return http.get<ApiResult<PageResult<RouteRuleItem>>>('/admin/route-rules', { params })
}

export function createRouteRule(payload: RouteRulePayload) {
  return http.post<ApiResult<RouteRuleItem>>('/admin/route-rules', payload)
}

export function updateRouteRule(id: number, payload: RouteRulePayload) {
  return http.put<ApiResult<RouteRuleItem>>(`/admin/route-rules/${id}`, payload)
}

export function deleteRouteRule(id: number) {
  return http.delete<ApiResult<null>>(`/admin/route-rules/${id}`)
}

export function enableRouteRule(id: number) {
  return http.post<ApiResult<RouteRuleItem>>(`/admin/route-rules/${id}/enable`)
}

export function disableRouteRule(id: number) {
  return http.post<ApiResult<RouteRuleItem>>(`/admin/route-rules/${id}/disable`)
}

export function testRoute(payload: { poolId?: number; payMethod: string; amount?: number; merchantOrderNo?: string; simulateFailure?: boolean }) {
  return http.post<ApiResult<RouteTestResult>>('/admin/route/test', payload)
}

export function barcodePay(payload: BarcodePayPayload) {
  return http.post<ApiResult<PayGatewayResult>>('/admin/route/pay-test', payload)
}

export function queryPay(payload: QueryPayPayload) {
  return http.post<ApiResult<PayGatewayResult>>('/admin/route/query-test', payload)
}

export function listRouteRecords(params: { current: number; size: number; merchantOrderNo?: string; poolId?: number; accountId?: number }) {
  return http.get<ApiResult<PageResult<RouteRecordItem>>>('/admin/route-records', { params })
}
