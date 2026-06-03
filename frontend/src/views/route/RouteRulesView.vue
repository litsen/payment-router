<template>
  <section class="page-panel">
    <div class="toolbar">
      <el-input v-model="keyword" clearable placeholder="搜索规则名称/编码" class="search-input" @keyup.enter="fetchRules" />
      <el-select v-model="poolId" clearable placeholder="商户" class="status-select">
        <el-option v-for="pool in pools" :key="pool.id" :label="pool.poolName" :value="pool.id" />
      </el-select>
      <el-button :icon="Search" @click="fetchRules">查询</el-button>
      <el-button v-if="authStore.canManageRouteRules" type="primary" :icon="Plus" @click="openCreate">新增规则</el-button>
    </div>

    <el-table :data="rules" border>
      <el-table-column prop="ruleName" label="规则名称" min-width="160" />
      <el-table-column prop="poolName" label="商户" min-width="150" />
      <el-table-column label="规则类型" min-width="130">
        <template #default="{ row }">{{ ruleTypeLabel(row.ruleType) }}</template>
      </el-table-column>
      <el-table-column label="支付方式" min-width="150">
        <template #default="{ row }">{{ payMethodLabel(row.payMethod) }}</template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="配置" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">{{ configSummary(row) }}</template>
      </el-table-column>
      <el-table-column v-if="authStore.canManageRouteRules" label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="toggleRule(row)">{{ row.enabled ? '停用' : '启用' }}</el-button>
          <el-button link type="danger" @click="removeRule(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.current"
      v-model:page-size="pagination.size"
      class="pagination"
      layout="total, sizes, prev, pager, next"
      :total="pagination.total"
      @current-change="fetchRules"
      @size-change="fetchRules"
    />

    <el-dialog v-model="dialogVisible" :title="editingRule ? '编辑路由规则' : '新增路由规则'" width="680px">
      <el-form :model="form" label-position="top">
        <el-form-item label="规则名称">
          <el-input v-model="form.ruleName" />
        </el-form-item>
        <el-form-item label="商户">
          <el-select v-model="form.poolId" class="full-width">
            <el-option v-for="pool in pools" :key="pool.id" :label="pool.poolName" :value="pool.id" />
          </el-select>
        </el-form-item>
        <el-alert
          v-if="selectedPoolConflictRule"
          :title="`该商户已配置路由规则「${selectedPoolConflictRule.ruleName}」，同一个商户只能配置一条路由规则。`"
          type="warning"
          :closable="false"
          class="form-alert"
        />
        <el-form-item label="支付方式">
          <el-select v-model="form.payMethods" multiple class="full-width" @change="handlePayMethodsChange">
            <el-option label="全部" value="ALL" />
            <el-option v-for="item in payMethodOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则类型">
          <el-select v-model="form.ruleType" class="full-width" @change="resetRuleConfig">
            <el-option v-for="item in ruleTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <div class="route-rule-config">
          <template v-if="form.ruleType === 'ROUND_ROBIN'">
            <el-alert title="按同优先级支付参数逐笔轮询，轮询游标由 Redis 保存。" type="info" :closable="false" />
          </template>
          <template v-else-if="form.ruleType === 'WEIGHT_RANDOM'">
            <el-alert title="按支付参数配置中的权重字段执行加权随机。" type="info" :closable="false" />
          </template>
          <template v-else-if="form.ruleType === 'TIME_RANGE'">
            <el-form-item label="生效开始时间">
              <el-time-picker v-model="configForm.startTime" value-format="HH:mm" format="HH:mm" class="full-width" />
            </el-form-item>
            <el-form-item label="生效结束时间">
              <el-time-picker v-model="configForm.endTime" value-format="HH:mm" format="HH:mm" class="full-width" />
            </el-form-item>
          </template>
          <template v-else-if="form.ruleType === 'LIMIT_AMOUNT'">
            <el-checkbox v-model="configForm.checkSingleAmount" disabled>单笔金额范围</el-checkbox>
            <el-checkbox v-model="configForm.checkDailyLimit" disabled>日限额</el-checkbox>
            <el-checkbox v-model="configForm.checkMonthlyLimit" disabled>月限额</el-checkbox>
            <el-alert title="限额值来自支付参数配置，路由时固定校验单笔金额、日限额和月限额。" type="info" :closable="false" />
          </template>
          <template v-else-if="form.ruleType === 'FAILOVER'">
            <el-form-item label="连续失败次数">
              <el-input-number v-model="configForm.failThreshold" :min="1" :max="99" class="full-width" />
            </el-form-item>
            <el-form-item label="熔断分钟数">
              <el-input-number v-model="configForm.breakMinutes" :min="1" :max="1440" class="full-width" />
            </el-form-item>
          </template>
        </div>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="1" :max="9999" class="full-width" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="Boolean(selectedPoolConflictRule)" @click="submitRule">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { listMerchantPools, type MerchantPoolItem } from '@/api/merchant'
import {
  createRouteRule,
  deleteRouteRule,
  disableRouteRule,
  enableRouteRule,
  listRouteRules,
  updateRouteRule,
  type RouteRuleItem
} from '@/api/route'
import type { ApiResult } from '@/api/auth'
import type { PageResult } from '@/api/system'

const ruleTypes = [
  { label: '每笔轮询', value: 'ROUND_ROBIN' },
  { label: '加权随机', value: 'WEIGHT_RANDOM' },
  { label: '按时间段', value: 'TIME_RANGE' },
  { label: '按限额', value: 'LIMIT_AMOUNT' },
  { label: '失败熔断', value: 'FAILOVER' }
]

const authStore = useAuthStore()

const payMethodOptions = [
  { label: '统一收银台', value: 'PRE_ORDER' },
  { label: '条码支付：商户扫顾客付款码', value: 'BARCODE_PAY' },
  { label: '条码支付前解码', value: 'DECODE_BAR' },
  { label: '聚合扫码支付接口', value: 'SCAN_PAY' },
  { label: '扫码支付接口', value: 'QRCODE_PAY' },
  { label: 'H5/链接跳转支付', value: 'H5_PAY' },
  { label: '微信公众号和小程序支付', value: 'WECHAT_JSAPI_PAY' },
  { label: '支付宝生活号和小程序支付', value: 'ALIPAY_JSAPI_PAY' }
]

const keyword = ref('')
const poolId = ref<number>()
const pools = ref<MerchantPoolItem[]>([])
const rules = ref<RouteRuleItem[]>([])
const allRules = ref<RouteRuleItem[]>([])
const dialogVisible = ref(false)
const editingRule = ref<RouteRuleItem | null>(null)
const pagination = reactive({ current: 1, size: 10, total: 0 })
const form = reactive({ ruleName: '', poolId: undefined as number | undefined, payMethods: ['ALL'], ruleType: 'ROUND_ROBIN', priority: 100, enabled: true })
const configForm = reactive({
  startTime: '09:00',
  endTime: '18:00',
  checkSingleAmount: true,
  checkDailyLimit: true,
  checkMonthlyLimit: true,
  failThreshold: 3,
  breakMinutes: 30
})
const selectedPoolConflictRule = computed(() => {
  if (!form.poolId) {
    return null
  }
  return allRules.value.find(rule => rule.poolId === form.poolId && rule.id !== editingRule.value?.id) || null
})

onMounted(async () => {
  await fetchPools()
  await fetchRules()
  await fetchAllRules()
})

async function fetchPools() {
  const response = await listMerchantPools({ current: 1, size: 100 }) as unknown as ApiResult<PageResult<MerchantPoolItem>>
  pools.value = response.data.records
}

async function fetchRules() {
  const response = await listRouteRules({ current: pagination.current, size: pagination.size, keyword: keyword.value, poolId: poolId.value }) as unknown as ApiResult<PageResult<RouteRuleItem>>
  rules.value = response.data.records
  pagination.total = response.data.total
}

async function fetchAllRules() {
  const response = await listRouteRules({ current: 1, size: 1000 }) as unknown as ApiResult<PageResult<RouteRuleItem>>
  allRules.value = response.data.records
}

function openCreate() {
  editingRule.value = null
  const availablePool = pools.value.find(pool => !allRules.value.some(rule => rule.poolId === pool.id))
  Object.assign(form, { ruleName: '', poolId: availablePool?.id || pools.value[0]?.id, payMethods: ['ALL'], ruleType: 'ROUND_ROBIN', priority: 100, enabled: true })
  resetRuleConfig()
  dialogVisible.value = true
}

function openEdit(rule: RouteRuleItem) {
  editingRule.value = rule
  Object.assign(form, {
    ruleName: rule.ruleName,
    poolId: rule.poolId,
    payMethods: parsePayMethods(rule.payMethod),
    ruleType: rule.ruleType,
    priority: rule.priority,
    enabled: rule.enabled
  })
  applyRuleConfig(rule.ruleConfigJson)
  dialogVisible.value = true
}

function resetRuleConfig() {
  Object.assign(configForm, {
    startTime: '09:00',
    endTime: '18:00',
    checkSingleAmount: true,
    checkDailyLimit: true,
    checkMonthlyLimit: true,
    failThreshold: 3,
    breakMinutes: 30
  })
}

function applyRuleConfig(configJson?: string) {
  resetRuleConfig()
  if (!configJson) {
    return
  }
  try {
    const parsed = JSON.parse(configJson)
    Object.assign(configForm, parsed)
  } catch {
    resetRuleConfig()
  }
}

async function submitRule() {
  const payload = {
    ruleName: form.ruleName,
    poolId: form.poolId,
    payMethod: serializePayMethods(),
    ruleType: form.ruleType,
    ruleConfigJson: serializeRuleConfig(),
    priority: form.priority,
    enabled: form.enabled
  }
  if (editingRule.value) {
    await updateRouteRule(editingRule.value.id, payload)
  } else {
    await createRouteRule(payload)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await fetchRules()
  await fetchAllRules()
}

async function toggleRule(rule: RouteRuleItem) {
  if (rule.enabled) {
    await disableRouteRule(rule.id)
    ElMessage.success('已停用')
  } else {
    await enableRouteRule(rule.id)
    ElMessage.success('已启用')
  }
  await fetchRules()
}

async function removeRule(rule: RouteRuleItem) {
  await ElMessageBox.confirm(`确认删除路由规则 ${rule.ruleName}？`, '删除确认', { type: 'warning' })
  await deleteRouteRule(rule.id)
  ElMessage.success('删除成功')
  await fetchRules()
  await fetchAllRules()
}

function ruleTypeLabel(value: string) {
  return ruleTypes.find(item => item.value === value)?.label || value
}

function configSummary(rule: RouteRuleItem) {
  const config = safeParseConfig(rule.ruleConfigJson)
  if (rule.ruleType === 'TIME_RANGE') {
    return `${config.startTime || '未设置'} - ${config.endTime || '未设置'}`
  }
  if (rule.ruleType === 'FAILOVER') {
    return `连续失败 ${config.failThreshold || 3} 次，熔断 ${config.breakMinutes || 30} 分钟`
  }
  if (rule.ruleType === 'LIMIT_AMOUNT') {
    return '校验单笔金额、日限额、月限额'
  }
  if (rule.ruleType === 'WEIGHT_RANDOM') {
    return '使用支付参数权重'
  }
  return '使用 Redis 轮询游标'
}

function safeParseConfig(configJson?: string) {
  try {
    return JSON.parse(configJson || '{}')
  } catch {
    return {}
  }
}

function payMethodLabel(value: string) {
  if (value === 'ALL') {
    return '全部'
  }
  return value.split(',').map(item => payMethodOptions.find(option => option.value === item)?.label || item).join('、')
}

function parsePayMethods(value: string) {
  if (!value || value === 'ALL') {
    return ['ALL']
  }
  return value.split(',').map(item => item.trim()).filter(Boolean)
}

function handlePayMethodsChange(values: string[]) {
  if (values.includes('ALL') && values.length > 1) {
    form.payMethods = ['ALL']
  }
  if (!form.payMethods.length) {
    form.payMethods = ['ALL']
  }
}

function serializePayMethods() {
  return form.payMethods.includes('ALL') ? 'ALL' : form.payMethods.join(',')
}

function serializeRuleConfig() {
  if (form.ruleType === 'TIME_RANGE') {
    return JSON.stringify({ startTime: configForm.startTime, endTime: configForm.endTime })
  }
  if (form.ruleType === 'LIMIT_AMOUNT') {
    return JSON.stringify({
      checkSingleAmount: configForm.checkSingleAmount,
      checkDailyLimit: configForm.checkDailyLimit,
      checkMonthlyLimit: configForm.checkMonthlyLimit
    })
  }
  if (form.ruleType === 'FAILOVER') {
    return JSON.stringify({ failThreshold: configForm.failThreshold, breakMinutes: configForm.breakMinutes })
  }
  return '{}'
}
</script>
