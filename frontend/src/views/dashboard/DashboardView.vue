<template>
  <div v-loading="loading" class="dashboard-page">
    <section class="dashboard-toolbar">
      <div>
        <h2>交易数据看板</h2>
        <span>{{ selectedRangeLabel }}</span>
      </div>
      <div class="dashboard-filters">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :clearable="false"
          :disabled-date="disabledDate"
        />
        <el-button class="dashboard-query-button" type="primary" :icon="DataAnalysis" @click="loadDashboard">查询</el-button>
        <el-button :icon="RefreshRight" @click="resetToday">今日</el-button>
      </div>
    </section>

    <div class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-card" :class="item.tone">
        <div class="metric-content">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
        <div class="metric-icon">
          <component :is="item.icon" />
        </div>
      </div>
    </div>

    <div class="chart-grid">
      <section class="chart-panel">
        <header>{{ trendTitle }}</header>
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
import {
  CircleCheck,
  CircleClose,
  CreditCard,
  DataAnalysis,
  RefreshRight,
  SwitchButton,
  TrendCharts,
  UserFilled,
  Wallet
} from '@element-plus/icons-vue'
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
import { ElMessage } from 'element-plus'

use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const loading = ref(false)
const dateRange = ref<[string, string]>([formatDate(new Date()), formatDate(new Date())])
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
  { label: '区间交易金额', value: formatMoney(summary.value.todayAmount), icon: Wallet, tone: 'metric-purple' },
  { label: '区间交易笔数', value: summary.value.todayOrderCount, icon: CreditCard, tone: 'metric-blue' },
  { label: '区间成功笔数', value: summary.value.todaySuccessCount, icon: CircleCheck, tone: 'metric-blue' },
  { label: '区间失败笔数', value: summary.value.todayFailedCount, icon: CircleClose, tone: 'metric-pink' },
  { label: '区间成功率', value: `${summary.value.todaySuccessRate}%`, icon: TrendCharts, tone: 'metric-cyan' },
  { label: '区间 UNKNOWN 订单数', value: summary.value.todayUnknownCount, icon: RefreshRight, tone: 'metric-blue' },
  { label: '当前可用商户号数量', value: summary.value.availableAccountCount, icon: UserFilled, tone: 'metric-blue' },
  { label: '当前熔断商户号数量', value: summary.value.circuitBrokenAccountCount, icon: SwitchButton, tone: 'metric-muted' }
])

const selectedRangeLabel = computed(() => {
  const [startDate, endDate] = dateRange.value
  return startDate === endDate ? `${startDate} 数据` : `${startDate} 至 ${endDate} 数据`
})

const trendTitle = computed(() => {
  const [startDate, endDate] = dateRange.value
  return daysBetween(startDate, endDate) > 2 ? '交易趋势（按日）' : '交易趋势（按小时）'
})

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
  if (daysBetween(dateRange.value[0], dateRange.value[1]) > 31) {
    ElMessage.warning('首页看板最多支持查询 31 天数据')
    return
  }
  loading.value = true
  try {
    const params = dashboardParams()
    const [summaryRes, trendRes, accountRes, statusRes] = await Promise.all([
      getDashboardSummary(params),
      getHourlyTrend(params),
      getAccountStats(params),
      getStatusDistribution(params)
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

function dashboardParams() {
  const [startDate, endDate] = dateRange.value
  return { startDate, endDate }
}

function resetToday() {
  const today = formatDate(new Date())
  dateRange.value = [today, today]
  loadDashboard()
}

function disabledDate(date: Date) {
  return date.getTime() > Date.now()
}

function renderCharts() {
  trendChart = initChart(trendChartRef.value, trendChart)
  amountChart = initChart(amountChartRef.value, amountChart)
  rateChart = initChart(rateChartRef.value, rateChart)
  statusChart = initChart(statusChartRef.value, statusChart)

  trendChart?.setOption({
    color: ['#7c3cff', '#2f80ed'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#e8edff',
      borderWidth: 1,
      textStyle: { color: '#44516c' }
    },
    legend: {
      top: 4,
      left: 0,
      itemWidth: 12,
      itemHeight: 8,
      textStyle: { color: '#71809f', fontSize: 12 }
    },
    grid: { left: 46, right: 28, top: 48, bottom: 34 },
    xAxis: {
      type: 'category',
      data: hourlyTrend.value.map(item => item.hour),
      boundaryGap: false,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e8edff' } },
      axisLabel: { color: '#75829e' }
    },
    yAxis: [
      { type: 'value', axisLabel: { color: '#75829e' }, splitLine: { lineStyle: { color: '#edf1fb' } } },
      { type: 'value', name: '笔数', axisLabel: { color: '#75829e' }, splitLine: { show: false } }
    ],
    series: [
      {
        name: '金额（￥）',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        lineStyle: { width: 4 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(124, 60, 255, 0.28)' },
              { offset: 1, color: 'rgba(124, 60, 255, 0)' }
            ]
          }
        },
        data: hourlyTrend.value.map(item => item.amount)
      },
      {
        name: '笔数',
        type: 'bar',
        yAxisIndex: 1,
        barWidth: 14,
        itemStyle: {
          borderRadius: [7, 7, 0, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#2f80ed' },
              { offset: 1, color: 'rgba(47, 128, 237, 0.12)' }
            ]
          }
        },
        data: hourlyTrend.value.map(item => item.orderCount)
      }
    ]
  })

  amountChart?.setOption({
    color: ['#653df5', '#2f80ed', '#38bdf8', '#8b7cf6', '#4f46e5', '#22d3ee'],
    tooltip: { trigger: 'item', formatter: '{b}<br />金额: ￥{c}<br />占比: {d}%' },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'middle',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#65728f', fontSize: 12 }
    },
    series: [
      {
        name: '交易金额',
        type: 'pie',
        radius: ['46%', '74%'],
        center: ['38%', '52%'],
        label: { show: false },
        labelLine: { show: false },
        data: accountStats.value.map(item => ({ name: item.accountName, value: item.amount }))
      }
    ]
  })

  rateChart?.setOption({
    color: ['#6f42f5'],
    tooltip: { trigger: 'axis', formatter: '{b}<br />成功率: {c}%' },
    grid: { left: 46, right: 20, top: 34, bottom: 44 },
    xAxis: {
      type: 'category',
      data: accountStats.value.map(item => item.accountName),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e8edff' } },
      axisLabel: { color: '#75829e', interval: 0 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: { formatter: '{value}%', color: '#75829e' },
      splitLine: { lineStyle: { color: '#edf1fb' } }
    },
    series: [
      {
        name: '成功率',
        type: 'bar',
        barWidth: 34,
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#6f42f5' },
              { offset: 1, color: 'rgba(111, 66, 245, 0.2)' }
            ]
          }
        },
        label: { show: true, position: 'top', formatter: '{c}%', color: '#44516c', fontWeight: 700 },
        data: accountStats.value.map(item => item.successRate)
      }
    ]
  })

  statusChart?.setOption({
    color: ['#4f6df5', '#7c3cff', '#38bdf8', '#f472b6', '#94a3b8'],
    tooltip: { trigger: 'item', formatter: '{b}<br />笔数: {c}<br />占比: {d}%' },
    legend: {
      orient: 'vertical',
      right: 36,
      top: 'middle',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#65728f', fontSize: 12 }
    },
    series: [
      {
        name: '订单数',
        type: 'pie',
        radius: ['48%', '76%'],
        center: ['38%', '52%'],
        label: { show: false },
        labelLine: { show: false },
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

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function daysBetween(startDate: string, endDate: string) {
  const start = new Date(`${startDate}T00:00:00`).getTime()
  const end = new Date(`${endDate}T00:00:00`).getTime()
  return Math.floor((end - start) / 86_400_000) + 1
}
</script>
