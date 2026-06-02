<template>
  <div v-loading="loading" class="dashboard-page">
    <div class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>

    <div class="chart-grid">
      <section class="chart-panel">
        <header>最近 24 小时交易趋势</header>
        <div ref="trendChartRef" class="chart-box"></div>
      </section>
      <section class="chart-panel">
        <header>各商户号交易金额占比</header>
        <div ref="amountChartRef" class="chart-box"></div>
      </section>
      <section class="chart-panel">
        <header>各商户号成功率</header>
        <div ref="rateChartRef" class="chart-box"></div>
      </section>
      <section class="chart-panel">
        <header>支付状态分布</header>
        <div ref="statusChartRef" class="chart-box"></div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use, type ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import {
  getAccountStats,
  getDashboardSummary,
  getHourlyTrend,
  getStatusDistribution,
  type AccountStatsItem,
  type DashboardSummary,
  type HourlyTrendItem,
  type StatusDistributionItem
} from '@/api/dashboard'
import type { ApiResult } from '@/api/auth'

use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const loading = ref(false)
const summary = ref<DashboardSummary>({
  todayAmount: 0,
  todayOrderCount: 0,
  todaySuccessCount: 0,
  todayFailedCount: 0,
  todaySuccessRate: 0,
  todayUnknownCount: 0,
  availableAccountCount: 0,
  circuitBrokenAccountCount: 0
})
const hourlyTrend = ref<HourlyTrendItem[]>([])
const accountStats = ref<AccountStatsItem[]>([])
const statusDistribution = ref<StatusDistributionItem[]>([])

const trendChartRef = ref<HTMLDivElement>()
const amountChartRef = ref<HTMLDivElement>()
const rateChartRef = ref<HTMLDivElement>()
const statusChartRef = ref<HTMLDivElement>()
let trendChart: ECharts | undefined
let amountChart: ECharts | undefined
let rateChart: ECharts | undefined
let statusChart: ECharts | undefined

const metrics = computed(() => [
  { label: '今日交易金额', value: formatMoney(summary.value.todayAmount) },
  { label: '今日交易笔数', value: summary.value.todayOrderCount },
  { label: '今日成功笔数', value: summary.value.todaySuccessCount },
  { label: '今日失败笔数', value: summary.value.todayFailedCount },
  { label: '今日成功率', value: `${summary.value.todaySuccessRate}%` },
  { label: '今日 UNKNOWN 订单数', value: summary.value.todayUnknownCount },
  { label: '当前可用商户号数量', value: summary.value.availableAccountCount },
  { label: '当前熔断商户号数量', value: summary.value.circuitBrokenAccountCount }
])

onMounted(async () => {
  await loadDashboard()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChart?.dispose()
  amountChart?.dispose()
  rateChart?.dispose()
  statusChart?.dispose()
})

async function loadDashboard() {
  loading.value = true
  try {
    const [summaryRes, trendRes, accountRes, statusRes] = await Promise.all([
      getDashboardSummary(),
      getHourlyTrend(),
      getAccountStats(),
      getStatusDistribution()
    ])
    summary.value = (summaryRes as unknown as ApiResult<DashboardSummary>).data
    hourlyTrend.value = (trendRes as unknown as ApiResult<HourlyTrendItem[]>).data
    accountStats.value = (accountRes as unknown as ApiResult<AccountStatsItem[]>).data
    statusDistribution.value = (statusRes as unknown as ApiResult<StatusDistributionItem[]>).data
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  trendChart = initChart(trendChartRef.value, trendChart)
  amountChart = initChart(amountChartRef.value, amountChart)
  rateChart = initChart(rateChartRef.value, rateChart)
  statusChart = initChart(statusChartRef.value, statusChart)

  trendChart?.setOption({
    color: ['#2563eb', '#16a34a'],
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 18, top: 28, bottom: 42 },
    xAxis: { type: 'category', data: hourlyTrend.value.map(item => item.hour), axisLabel: { rotate: 45 } },
    yAxis: [
      { type: 'value', name: '金额' },
      { type: 'value', name: '笔数' }
    ],
    series: [
      { name: '交易金额', type: 'line', smooth: true, data: hourlyTrend.value.map(item => item.amount) },
      { name: '交易笔数', type: 'bar', yAxisIndex: 1, data: hourlyTrend.value.map(item => item.orderCount) }
    ]
  })

  amountChart?.setOption({
    color: ['#2563eb', '#16a34a', '#f59e0b', '#dc2626', '#7c3aed', '#0891b2'],
    tooltip: { trigger: 'item' },
    series: [
      {
        name: '交易金额',
        type: 'pie',
        radius: ['42%', '70%'],
        data: accountStats.value.map(item => ({ name: item.accountName, value: item.amount }))
      }
    ]
  })

  rateChart?.setOption({
    color: ['#16a34a'],
    tooltip: { trigger: 'axis', formatter: '{b}<br />成功率: {c}%' },
    grid: { left: 48, right: 20, top: 20, bottom: 56 },
    xAxis: { type: 'category', data: accountStats.value.map(item => item.accountName), axisLabel: { rotate: 35 } },
    yAxis: { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
    series: [{ name: '成功率', type: 'bar', data: accountStats.value.map(item => item.successRate) }]
  })

  statusChart?.setOption({
    color: ['#16a34a', '#dc2626', '#f59e0b', '#2563eb', '#64748b'],
    tooltip: { trigger: 'item' },
    series: [
      {
        name: '订单数',
        type: 'pie',
        radius: '68%',
        data: statusDistribution.value.map(item => ({ name: item.status, value: item.count }))
      }
    ]
  })
}

function initChart(el: HTMLDivElement | undefined, chart: ECharts | undefined) {
  if (!el) {
    return undefined
  }
  return chart ?? init(el)
}

function resizeCharts() {
  trendChart?.resize()
  amountChart?.resize()
  rateChart?.resize()
  statusChart?.resize()
}

function formatMoney(value: number) {
  return `￥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}
</script>
