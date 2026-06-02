import { http } from './http'
import type { ApiResult } from './auth'
import type { PageResult } from './system'

export interface OrderItem {
  id: number
  tenantId: string
  appId: string
  merchantOrderNo: string
  platformOrderNo?: string
  channelOrderNo?: string
  payMethod: string
  amount: number
  subject: string
  authCodeMasked?: string
  upstreamOrderTime?: string
  routeType?: string
  routeRecordId?: number
  poolId: number
  poolName: string
  accountId?: number
  accountName?: string
  accountApiKeyMasked?: string
  status: string
  notifyUrl?: string
  paySuccessTime?: string
  expiredTime?: string
  lastQueryTime?: string
  queryCount?: number
  upstreamResponseCode?: string
  upstreamResponseMsg?: string
  createdAt?: string
  updatedAt?: string
}

export interface OrderLogItem {
  id: number
  tenantId?: string
  orderId?: number
  merchantOrderNo?: string
  direction: string
  apiType: string
  requestUrl?: string
  requestHeadersJson?: string
  requestBody?: string
  responseBody?: string
  httpStatus?: number
  costMs?: number
  success?: boolean
  errorCode?: string
  resultStatus?: string
  errorMessage?: string
  createdAt?: string
}

export interface NotifyLogItem {
  id: number
  tenantId?: string
  orderId?: number
  merchantOrderNo?: string
  notifyBody?: string
  verified?: boolean
  success?: boolean
  errorMessage?: string
  createdAt?: string
}

export interface RefundOrderItem {
  id: number
  tenantId: string
  orderId: number
  poolId: number
  poolName: string
  accountId?: number
  accountName?: string
  appId: string
  merchantOrderNo: string
  merchantRefundNo: string
  platformOrderNo?: string
  channelOrderNo?: string
  upstreamRefundNo?: string
  orderAmount: number
  refundAmount: number
  reason?: string
  notifyUrl?: string
  status: string
  upstreamResponseCode?: string
  upstreamResponseMsg?: string
  upstreamRawResponse?: string
  refundSuccessTime?: string
  lastQueryTime?: string
  queryCount?: number
  createdAt?: string
  updatedAt?: string
}

export function listOrders(params: Record<string, unknown>) {
  return http.get<ApiResult<PageResult<OrderItem>>>('/admin/orders', { params })
}

export function getOrder(id: number) {
  return http.get<ApiResult<OrderItem>>(`/admin/orders/${id}`)
}

export function queryOrder(id: number) {
  return http.post<ApiResult<unknown>>(`/admin/orders/${id}/query`)
}

export function listOrderLogs(params: Record<string, unknown>) {
  return http.get<ApiResult<PageResult<OrderLogItem>>>('/admin/order-logs', { params })
}

export function getOrderLog(id: number) {
  return http.get<ApiResult<OrderLogItem>>(`/admin/order-logs/${id}`)
}

export function listNotifyLogs(params: Record<string, unknown>) {
  return http.get<ApiResult<PageResult<NotifyLogItem>>>('/admin/notify-logs', { params })
}

export function listRefundOrders(params: Record<string, unknown>) {
  return http.get<ApiResult<PageResult<RefundOrderItem>>>('/admin/refunds', { params })
}

export function getRefundOrder(id: number) {
  return http.get<ApiResult<RefundOrderItem>>(`/admin/refunds/${id}`)
}

export function queryRefundOrder(id: number) {
  return http.post<ApiResult<unknown>>(`/admin/refunds/${id}/query`)
}
