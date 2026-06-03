<template>
  <section class="page-panel route-test-layout">
    <el-form :model="form" label-position="top" class="route-test-form">
      <el-form-item label="商户">
        <el-select v-model="form.poolId" class="full-width" @change="handlePoolChange">
          <el-option v-for="pool in pools" :key="pool.id" :label="`${pool.poolName}（${pool.poolCode}）`" :value="pool.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="支付方式">
        <el-select v-model="form.payMethod" class="full-width">
          <el-option
            v-for="method in availablePayMethods"
            :key="method.methodCode"
            :label="method.methodName"
            :value="method.methodCode"
            :disabled="!method.enabled"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="金额">
        <el-input-number v-model="form.amount" :min="0.01" :precision="2" class="full-width" />
      </el-form-item>
      <el-form-item v-if="form.payMethod === 'BARCODE_PAY'" label="付款码">
        <el-input v-model="form.authCode" placeholder="Mock：以 1 结尾成功，以 2 结尾失败，其他处理中" />
      </el-form-item>
      <el-form-item v-if="form.payMethod === 'H5_PAY'" label="返回地址">
        <el-input v-model="form.returnUrl" clearable placeholder="可选，支付完成后的跳转地址" />
      </el-form-item>
      <el-form-item v-if="form.payMethod === 'QRCODE_PAY'" label="service">
        <el-select v-model="form.service" class="full-width">
          <el-option label="微信扫码 pay.wxpay.qrcode" value="pay.wxpay.qrcode" />
          <el-option label="支付宝扫码 pay.alipay.qrcode" value="pay.alipay.qrcode" />
          <el-option label="云闪付扫码 pay.unpay.qrcode" value="pay.unpay.qrcode" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.payMethod === 'WECHAT_JSAPI_PAY' || form.payMethod === 'ALIPAY_JSAPI_PAY'" label="平台 AppId">
        <el-input v-model="form.subAppId" clearable :placeholder="form.payMethod === 'WECHAT_JSAPI_PAY' ? '微信 sub_appid，必填' : '支付宝 sub_appid，可选'" />
      </el-form-item>
      <el-form-item v-if="form.payMethod === 'WECHAT_JSAPI_PAY' || form.payMethod === 'ALIPAY_JSAPI_PAY'" label="付款用户标识">
        <el-input v-model="form.payerId" :placeholder="form.payMethod === 'WECHAT_JSAPI_PAY' ? '微信 sub_openid' : '支付宝 buyer_id 或 buyer_open_id'" />
      </el-form-item>
      <el-form-item label="商品标题">
        <el-input v-model="form.subject" />
      </el-form-item>
      <el-form-item label="商户订单号">
        <el-input v-model="form.merchantOrderNo" />
      </el-form-item>
      <el-form-item label="通知地址">
        <el-input v-model="form.notifyUrl" clearable placeholder="可选" />
      </el-form-item>
      <div class="toolbar compact-toolbar">
        <el-button type="primary" :icon="Promotion" :loading="submitting" :disabled="!canSubmit" @click="submitPay">调用支付</el-button>
        <el-button :icon="Search" :loading="querying" :disabled="!canQuery" @click="submitQuery">查单</el-button>
        <el-button :icon="Refresh" @click="resetOrderNo">换订单号</el-button>
      </div>
    </el-form>

    <div class="route-test-result">
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        class="form-alert"
      />
      <el-descriptions v-if="result" :column="1" border>
        <el-descriptions-item label="AppId">{{ result.appId }}</el-descriptions-item>
        <el-descriptions-item label="商户订单号">{{ result.merchantOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ result.amount }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ result.payMethod }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="statusTagType(result.status)">{{ statusLabel(result.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="平台订单号">{{ result.platformOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上游订单号">{{ result.channelOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="paymentContent" label="支付链接/二维码内容">
          <a v-if="isPaymentLink" :href="paymentContent" target="_blank" rel="noopener noreferrer">{{ paymentContent }}</a>
          <span v-else>{{ paymentContent }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="result.payData" label="支付参数">
          <pre class="json-preview inline-json-preview">{{ JSON.stringify(result.payData, null, 2) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="rawResponse" label="上游原始响应">
          <pre class="json-preview inline-json-preview">{{ rawResponse }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="结果">{{ result.message || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="requestPreview" class="request-preview">
        <div class="preview-title">测试参数</div>
        <pre class="json-preview">{{ requestPreview }}</pre>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, Refresh, Search } from '@element-plus/icons-vue'
import { listMerchantAccounts, listMerchantPools, type MerchantAccountItem, type MerchantPoolItem } from '@/api/merchant'
import { listPayMethods, type PayMethodItem } from '@/api/payMethod'
import { barcodePay, queryPay, type PayGatewayResult } from '@/api/route'
import type { ApiResult } from '@/api/auth'
import type { PageResult } from '@/api/system'

const pools = ref<MerchantPoolItem[]>([])
const accounts = ref<MerchantAccountItem[]>([])
const payMethods = ref<PayMethodItem[]>([])
const result = ref<PayGatewayResult | null>(null)
const errorMessage = ref('')
const requestPreview = ref('')
const submitting = ref(false)
const querying = ref(false)
const form = reactive({
  poolId: undefined as number | undefined,
  payMethod: 'BARCODE_PAY',
  amount: 1.00,
  authCode: '280000000000000001',
  subject: '测试商品',
  merchantOrderNo: nextOrderNo(),
  notifyUrl: '',
  service: 'pay.wxpay.qrcode',
  returnUrl: '',
  subAppId: '',
  payerId: ''
})

const canSubmit = computed(() => {
  const baseReady = Boolean(form.poolId && form.payMethod && form.merchantOrderNo && form.amount && form.subject)
  if (form.payMethod === 'BARCODE_PAY') return baseReady && Boolean(form.authCode)
  if (form.payMethod === 'QRCODE_PAY') return baseReady && Boolean(form.service)
  if (form.payMethod === 'WECHAT_JSAPI_PAY') return baseReady && Boolean(form.subAppId && form.payerId)
  if (form.payMethod === 'ALIPAY_JSAPI_PAY') return baseReady && Boolean(form.payerId)
  return baseReady
})
const canQuery = computed(() => Boolean(form.poolId && form.payMethod && form.merchantOrderNo))
const rawResponse = computed(() => {
  const data = result.value?.payData
  return data && typeof data === 'object' && 'rawResponse' in data ? String((data as Record<string, unknown>).rawResponse || '') : ''
})
const paymentContent = computed(() => {
  const data = result.value?.payData
  if (!data || typeof data !== 'object') return ''
  const values = data as Record<string, unknown>
  return String(values.payUrl || values.pay_url || values.url || values.qr_code || values.code_url || '')
})
const isPaymentLink = computed(() => paymentContent.value.startsWith('http://') || paymentContent.value.startsWith('https://'))
const availablePayMethods = computed(() => {
  const supported = supportedMethodsForPool(form.poolId)
  return payMethods.value.filter(method => method.enabled && supported.has(method.methodCode))
})

onMounted(async () => {
  const [poolResponse, accountResponse, methodResponse] = await Promise.all([
    listMerchantPools({ current: 1, size: 100 }) as unknown as Promise<ApiResult<PageResult<MerchantPoolItem>>>,
    listMerchantAccounts({ current: 1, size: 1000, status: 'ENABLED' }) as unknown as Promise<ApiResult<PageResult<MerchantAccountItem>>>,
    listPayMethods() as unknown as Promise<ApiResult<PayMethodItem[]>>
  ])
  pools.value = poolResponse.data.records
  accounts.value = accountResponse.data.records
  payMethods.value = methodResponse.data.filter(method => method.methodCode !== 'DECODE_BAR')
  form.poolId = pools.value[0]?.id
  syncPayMethodForPool()
})

function handlePoolChange() {
  result.value = null
  requestPreview.value = ''
  syncPayMethodForPool()
}

function supportedMethodsForPool(poolId?: number) {
  const values = new Set<string>()
  const poolAccounts = accounts.value.filter(account => account.poolId === poolId && account.status === 'ENABLED')
  for (const account of poolAccounts) {
    for (const item of account.supportPayMethods.split(',').map(value => value.trim()).filter(Boolean)) {
      if (item === 'ALL') {
        payMethods.value.forEach(method => values.add(method.methodCode))
      } else {
        values.add(item)
      }
    }
  }
  return values
}

function syncPayMethodForPool() {
  const currentSupported = availablePayMethods.value.some(method => method.methodCode === form.payMethod)
  if (!currentSupported) {
    form.payMethod = availablePayMethods.value[0]?.methodCode || ''
  }
}

async function submitPay() {
  submitting.value = true
  errorMessage.value = ''
  result.value = null
  try {
    const payload = buildBarcodePayload()
    requestPreview.value = JSON.stringify(payload, null, 2)
    const response = await barcodePay(payload) as unknown as ApiResult<PayGatewayResult>
    result.value = response.data
    ElMessage.success('支付调试完成')
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || '支付调试失败，请检查商户、支付方式、支付参数、金额和路由配置。'
  } finally {
    submitting.value = false
  }
}

async function submitQuery() {
  querying.value = true
  errorMessage.value = ''
  try {
    const payload = buildQueryPayload()
    requestPreview.value = JSON.stringify(payload, null, 2)
    const response = await queryPay(payload) as unknown as ApiResult<PayGatewayResult>
    result.value = response.data
    ElMessage.success('查单接口调用完成')
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || '查单接口调用失败，请检查订单号和支付参数。'
  } finally {
    querying.value = false
  }
}

function buildBarcodePayload() {
  return {
    poolId: form.poolId!,
    payMethod: form.payMethod,
    merchantOrderNo: form.merchantOrderNo,
    amount: Number(form.amount.toFixed(2)),
    subject: form.subject,
    ...(form.payMethod === 'BARCODE_PAY' ? { authCode: form.authCode } : {}),
    ...(form.notifyUrl ? { notifyUrl: form.notifyUrl } : {}),
    ...(form.payMethod === 'QRCODE_PAY' ? { service: form.service } : {}),
    ...(form.returnUrl && form.payMethod === 'H5_PAY' ? { returnUrl: form.returnUrl } : {}),
    ...(form.subAppId && (form.payMethod === 'WECHAT_JSAPI_PAY' || form.payMethod === 'ALIPAY_JSAPI_PAY') ? { subAppId: form.subAppId } : {}),
    ...(form.payerId && (form.payMethod === 'WECHAT_JSAPI_PAY' || form.payMethod === 'ALIPAY_JSAPI_PAY') ? { payerId: form.payerId } : {})
  }
}

function buildQueryPayload() {
  return {
    poolId: form.poolId!,
    payMethod: form.payMethod,
    merchantOrderNo: form.merchantOrderNo
  }
}

function resetOrderNo() {
  form.merchantOrderNo = nextOrderNo()
  result.value = null
  requestPreview.value = ''
}

function nextOrderNo() {
  return `TEST${Date.now()}`
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    SUCCESS: '支付成功',
    FAILED: '支付失败',
    PAYING: '处理中',
    UNKNOWN: '待确认'
  }
  return labels[status] || status
}

function statusTagType(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'UNKNOWN') return 'warning'
  return 'info'
}
</script>
