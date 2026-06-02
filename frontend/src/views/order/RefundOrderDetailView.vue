<template>
  <section class="page-panel">
    <div class="toolbar">
      <el-button @click="router.back()">返回</el-button>
      <el-button v-if="authStore.canManageRefunds" type="primary" :disabled="!refund || isTerminal(refund.status)" @click="manualQuery">退款查单</el-button>
    </div>
    <el-descriptions v-if="refund" :column="2" border>
      <el-descriptions-item label="退款订单号">{{ refund.merchantRefundNo }}</el-descriptions-item>
      <el-descriptions-item label="商户订单号">{{ refund.merchantOrderNo }}</el-descriptions-item>
      <el-descriptions-item label="平台订单号">{{ refund.platformOrderNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="上游退款号">{{ refund.upstreamRefundNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="商户">{{ refund.poolName }}</el-descriptions-item>
      <el-descriptions-item label="支付参数">{{ refund.accountName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="订单金额">{{ refund.orderAmount }}</el-descriptions-item>
      <el-descriptions-item label="退款金额">{{ refund.refundAmount }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ statusLabel(refund.status) }}</el-descriptions-item>
      <el-descriptions-item label="查单次数">{{ refund.queryCount ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="退款原因" :span="2">{{ refund.reason || '-' }}</el-descriptions-item>
      <el-descriptions-item label="通知地址" :span="2">{{ refund.notifyUrl || '-' }}</el-descriptions-item>
      <el-descriptions-item label="成功时间">{{ formatDateTime(refund.refundSuccessTime) }}</el-descriptions-item>
      <el-descriptions-item label="最后查单时间">{{ formatDateTime(refund.lastQueryTime) }}</el-descriptions-item>
      <el-descriptions-item label="上游响应码">{{ refund.upstreamResponseCode || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatDateTime(refund.createdAt) }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ formatDateTime(refund.updatedAt) }}</el-descriptions-item>
      <el-descriptions-item label="上游响应" :span="2">{{ refund.upstreamResponseMsg || '-' }}</el-descriptions-item>
      <el-descriptions-item label="原始响应" :span="2">{{ refund.upstreamRawResponse || '-' }}</el-descriptions-item>
    </el-descriptions>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getRefundOrder, queryRefundOrder, type RefundOrderItem } from '@/api/order'
import type { ApiResult } from '@/api/auth'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const refund = ref<RefundOrderItem | null>(null)

onMounted(fetchRefund)

async function fetchRefund() {
  const response = await getRefundOrder(Number(route.params.id)) as unknown as ApiResult<RefundOrderItem>
  refund.value = response.data
}

async function manualQuery() {
  await queryRefundOrder(Number(route.params.id))
  ElMessage.success('查单完成')
  await fetchRefund()
}

function isTerminal(status: string) {
  return status === 'SUCCESS' || status === 'FAILED'
}

function statusLabel(status: string) {
  return { SUCCESS: '退款成功', FAILED: '退款失败', PROCESSING: '处理中', UNKNOWN: '待确认', INIT: '初始化' }[status] || status
}
</script>
