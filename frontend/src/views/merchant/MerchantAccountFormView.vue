<template>
  <section class="page-panel">
    <el-form :model="form" label-position="top" class="account-form">
      <div class="form-grid">
        <el-form-item label="所属商户">
          <el-select v-model="form.poolId" class="full-width" placeholder="请选择所属商户">
            <el-option v-for="pool in pools" :key="pool.id" :label="pool.poolName" :value="pool.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商户号名称">
          <el-input v-model="form.accountName" />
        </el-form-item>
        <el-form-item label="APIKEY">
          <el-input v-model="form.apiKey" show-password :placeholder="isEdit ? maskedHint(account?.apiKeyMasked) : ''" />
        </el-form-item>
        <el-form-item label="APISecret">
          <el-input v-model="form.signKey" show-password :placeholder="isEdit ? maskedHint(account?.signKeyMasked) : ''" />
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="payMethods" multiple class="full-width" @change="handlePayMethodsChange">
            <el-option label="全部" value="ALL" />
            <el-option label="统一收银台" value="PRE_ORDER" />
            <el-option label="条码支付：商户扫顾客付款码" value="BARCODE_PAY" />
            <el-option label="条码支付前解码" value="DECODE_BAR" />
            <el-option label="聚合扫码支付接口" value="SCAN_PAY" />
            <el-option label="扫码支付接口" value="QRCODE_PAY" />
            <el-option label="H5/链接跳转支付" value="H5_PAY" />
            <el-option label="微信公众号和小程序支付" value="WECHAT_JSAPI_PAY" />
            <el-option label="支付宝生活号和小程序支付" value="ALIPAY_JSAPI_PAY" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <template #label>
            <span class="label-with-help">
              优先级
              <el-tooltip content="路由选择时优先使用数值更小的支付参数；同优先级内再结合权重、限额和可用时间等规则。">
                <el-icon><InfoFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-input-number v-model="form.priority" :min="1" :max="9999" class="full-width" />
        </el-form-item>
        <el-form-item label="日限额">
          <el-input-number v-model="form.dailyAmountLimit" :min="0" :precision="2" class="full-width" />
          <div class="amount-shortcuts">
            <el-button v-for="item in amountShortcuts" :key="`daily-${item.value}`" size="small" @click="form.dailyAmountLimit = item.value">
              {{ item.label }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="月限额">
          <el-input-number v-model="form.monthlyAmountLimit" :min="0" :precision="2" class="full-width" />
          <div class="amount-shortcuts">
            <el-button v-for="item in amountShortcuts" :key="`monthly-${item.value}`" size="small" @click="form.monthlyAmountLimit = item.value">
              {{ item.label }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="单笔最小">
          <el-input-number v-model="form.singleMinAmount" :min="0" :precision="2" class="full-width" />
        </el-form-item>
        <el-form-item label="单笔最大">
          <el-input-number v-model="form.singleMaxAmount" :min="0" :precision="2" class="full-width" />
        </el-form-item>
        <el-form-item label="可用日期范围">
          <el-date-picker
            v-model="availableDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="full-width"
          />
        </el-form-item>
        <el-form-item label="当日可用开始时间">
          <el-time-picker v-model="form.availableStartTime" value-format="HH:mm:ss" format="HH:mm:ss" class="full-width" />
        </el-form-item>
        <el-form-item label="当日可用结束时间">
          <el-time-picker v-model="form.availableEndTime" value-format="HH:mm:ss" format="HH:mm:ss" class="full-width" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="full-width">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" />
      </el-form-item>

      <div class="form-actions">
        <el-button @click="router.push('/merchant/accounts')">取消</el-button>
        <el-button type="primary" @click="submitAccount">保存</el-button>
      </div>
    </el-form>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import {
  createMerchantAccount,
  getMerchantAccount,
  listMerchantPools,
  updateMerchantAccount,
  type MerchantAccountItem,
  type MerchantAccountPayload,
  type MerchantPoolItem
} from '@/api/merchant'
import type { ApiResult } from '@/api/auth'
import type { PageResult } from '@/api/system'

const route = useRoute()
const router = useRouter()
const account = ref<MerchantAccountItem | null>(null)
const pools = ref<MerchantPoolItem[]>([])
const payMethods = ref<string[]>(['ALL'])
const availableDateRange = ref<[string, string] | undefined>()
const amountShortcuts = [
  { label: '5万', value: 50000 },
  { label: '10万', value: 100000 },
  { label: '50万', value: 500000 },
  { label: '100万', value: 1000000 }
]
const isEdit = computed(() => Boolean(route.params.id))
const form = reactive<MerchantAccountPayload>({
  poolId: undefined,
  accountName: '',
  apiKey: '',
  signKey: '',
  supportPayMethods: 'ALL',
  priority: 100,
  dailyAmountLimit: undefined,
  monthlyAmountLimit: undefined,
  singleMinAmount: undefined,
  singleMaxAmount: undefined,
  availableStartDate: undefined,
  availableEndDate: undefined,
  availableStartTime: undefined,
  availableEndTime: undefined,
  status: 'ENABLED',
  remark: ''
})

onMounted(async () => {
  await fetchPools()
  if (isEdit.value) {
    await fetchAccount()
  }
})

async function fetchPools() {
  const response = await listMerchantPools({ current: 1, size: 1000, status: 'ENABLED' }) as unknown as ApiResult<PageResult<MerchantPoolItem>>
  pools.value = response.data.records
}

async function fetchAccount() {
  const response = await getMerchantAccount(Number(route.params.id)) as unknown as ApiResult<MerchantAccountItem>
  account.value = response.data
  payMethods.value = parsePayMethods(response.data.supportPayMethods)
  availableDateRange.value = response.data.availableStartDate && response.data.availableEndDate
    ? [response.data.availableStartDate, response.data.availableEndDate]
    : undefined
  Object.assign(form, {
    poolId: response.data.poolId,
    accountName: response.data.accountName,
    apiKey: '',
    signKey: '',
    supportPayMethods: response.data.supportPayMethods,
    priority: response.data.priority,
    dailyAmountLimit: response.data.dailyAmountLimit,
    monthlyAmountLimit: response.data.monthlyAmountLimit,
    singleMinAmount: response.data.singleMinAmount,
    singleMaxAmount: response.data.singleMaxAmount,
    availableStartDate: response.data.availableStartDate,
    availableEndDate: response.data.availableEndDate,
    availableStartTime: response.data.availableStartTime,
    availableEndTime: response.data.availableEndTime,
    status: response.data.status,
    remark: response.data.remark || ''
  })
}

function maskedHint(value?: string) {
  return value ? `当前：${value}，留空不修改` : '留空不修改'
}

async function submitAccount() {
  const payload = cleanPayload()
  if (isEdit.value) {
    await updateMerchantAccount(Number(route.params.id), payload)
  } else {
    await createMerchantAccount(payload)
  }
  ElMessage.success('保存成功')
  router.push('/merchant/accounts')
}

function cleanPayload() {
  const payload: MerchantAccountPayload = {
    ...form,
    channelCode: 'DEFAULT',
    supportPayMethods: serializePayMethods(),
    availableStartDate: availableDateRange.value?.[0],
    availableEndDate: availableDateRange.value?.[1]
  }
  if (isEdit.value) {
    for (const key of ['apiKey', 'signKey'] as const) {
      if (!payload[key]) {
        delete payload[key]
      }
    }
  }
  for (const key of Object.keys(payload) as Array<keyof MerchantAccountPayload>) {
    if (payload[key] === '') {
      delete payload[key]
    }
  }
  return payload
}

function parsePayMethods(value?: string) {
  if (!value || value === 'ALL') {
    return ['ALL']
  }
  return value.split(',').map(item => item.trim()).filter(Boolean)
}

function handlePayMethodsChange(values: string[]) {
  if (values.includes('ALL') && values.length > 1) {
    payMethods.value = ['ALL']
  }
  if (!payMethods.value.length) {
    payMethods.value = ['ALL']
  }
}

function serializePayMethods() {
  return payMethods.value.includes('ALL') ? 'ALL' : payMethods.value.join(',')
}
</script>
