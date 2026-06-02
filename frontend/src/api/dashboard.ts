import { http } from './http'
import type { ApiResult } from './auth'

export interface DashboardSummary {
  todayAmount: number
  todayOrderCount: number
  todaySuccessCount: number
  todayFailedCount: number
  todaySuccessRate: number
  todayUnknownCount: number
  availableAccountCount: number
  circuitBrokenAccountCount: number
}

export interface HourlyTrendItem {
  hour: string
  amount: number
  orderCount: number
  successCount: number
  failedCount: number
  unknownCount: number
}

export interface AccountStatsItem {
  accountId?: number
  accountName: string
  poolId?: number
  poolName: string
  amount: number
  orderCount: number
  successCount: number
  failedCount: number
  successRate: number
}

export interface StatusDistributionItem {
  status: string
  count: number
  amount: number
}

export function getDashboardSummary() {
  return http.get<ApiResult<DashboardSummary>>('/admin/dashboard/summary')
}

export function getHourlyTrend() {
  return http.get<ApiResult<HourlyTrendItem[]>>('/admin/dashboard/hourly-trend')
}

export function getAccountStats() {
  return http.get<ApiResult<AccountStatsItem[]>>('/admin/dashboard/account-stats')
}

export function getStatusDistribution() {
  return http.get<ApiResult<StatusDistributionItem[]>>('/admin/dashboard/status-distribution')
}
