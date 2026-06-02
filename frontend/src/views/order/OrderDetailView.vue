<template>
  <section class="page-panel">
    <div class="toolbar">
      <el-button @click="router.back()">返回</el-button>
      <el-button v-if="authStore.canManageOrders" type="primary" :disabled="!order || isTerminal(order.status)" :loading="querying" @click="manualQuery">手动查单</el-button>
      <el-button v-if="authStore.canManageOrders && order?.status === 'FAILED'" type="warning" :loading="querying" @click="forceQuery">重新查询状态</el-button>
    </div>
    <el-descriptions v-if="order" :column="2" border>
      <el-descriptions-item label="商户订单号">{{ order.merchantOrderNo }}</el-descriptions-item>
      <el-descriptions-item label="平台订单号">{{ order.platformOrderNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="上游订单号">{{ order.channelOrderNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="支付方式">{{ order.payMethod }}</el-descriptions-item>
      <el-descriptions-item label="金额">{{ order.amount }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ statusLabel(order.status) }}</el-descriptions-item>
      <el-descriptions-item label="AppId">{{ order.appId }}</el-descriptions-item>
      <el-descriptions-item label="商户">{{ order.poolName }}</el-descriptions-item>
      <el-descriptions-item label="支付参数">{{ order.accountName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="支付参数ID">{{ order.accountId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="APIKEY">{{ order.accountApiKeyMasked || '-' }}</el-descriptions-item>
      <el-descriptions-item label="商品标题">{{ order.subject }}</el-descriptions-item>
      <el-descriptions-item label="付款码">{{ order.authCodeMasked || '-' }}</el-descriptions-item>
      <el-descriptions-item label="路由类型">{{ order.routeType || '-' }}</el-descriptions-item>
      <el-descriptions-item label="路由记录ID">{{ order.routeRecordId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="通知地址" :span="2">{{ order.notifyUrl || '-' }}</el-descriptions-item>
      <el-descriptions-item label="上游下单时间">{{ order.upstreamOrderTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="成功时间">{{ formatDateTime(order.paySuccessTime) }}</el-descriptions-item>
      <el-descriptions-item label="过期时间">{{ formatDateTime(order.expiredTime) }}</el-descriptions-item>
      <el-descriptions-item label="最后查单时间">{{ formatDateTime(order.lastQueryTime) }}</el-descriptions-item>
      <el-descriptions-item label="查单次数">{{ order.queryCount ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="上游响应码">{{ order.upstreamResponseCode || '-' }}</el-descriptions-item>
      <el-descriptions-item label="上游响应" :span="2">{{ order.upstreamResponseMsg || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatDateTime(order.createdAt) }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ formatDateTime(order.updatedAt) }}</el-descriptions-item>
    </el-descriptions>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getOrder, queryOrder, type OrderItem } from '@/api/order'
import type { ApiResult } from '@/api/auth'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const order = ref<OrderItem | null>(null)
const querying = ref(false)

onMounted(fetchOrder)

async function fetchOrder() {
  const response = await getOrder(Number(route.params.id)) as unknown as ApiResult<OrderItem>
  order.value = response.data
}

async function manualQuery() {
  await runQuery(false)
}

async function forceQuery() {
  await runQuery(true)
}

async function runQuery(force: boolean) {
  querying.value = true
  try {
    await queryOrder(Number(route.params.id), force)
    ElMessage.success(force ? '状态重新查询完成' : '查单完成')
    await fetchOrder()
  } finally {
    querying.value = false
  }
}

function isTerminal(status: string) {
  return status === 'SUCCESS' || status === 'FAILED'
}

function statusLabel(status: string) {
  return { SUCCESS: '支付成功', FAILED: '支付失败', PAYING: '处理中', UNKNOWN: '待确认' }[status] || status
}
</script>
