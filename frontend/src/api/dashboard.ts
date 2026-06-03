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

export interface DashboardDateRangeParams {
  startDate?: string
  endDate?: string
}

export function getDashboardSummary(params?: DashboardDateRangeParams) {
  return http.get<ApiResult<DashboardSummary>>('/admin/dashboard/summary', { params })
}

export function getHourlyTrend(params?: DashboardDateRangeParams) {
  return http.get<ApiResult<HourlyTrendItem[]>>('/admin/dashboard/hourly-trend', { params })
}

export function getAccountStats(params?: DashboardDateRangeParams) {
  return http.get<ApiResult<AccountStatsItem[]>>('/admin/dashboard/account-stats', { params })
}

export function getStatusDistribution(params?: DashboardDateRangeParams) {
  return http.get<ApiResult<StatusDistributionItem[]>>('/admin/dashboard/status-distribution', { params })
}
