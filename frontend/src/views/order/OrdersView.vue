<template>
  <section class="page-panel">
    <div class="toolbar">
      <el-input v-model="filters.merchantOrderNo" clearable placeholder="商户订单号" class="search-input" @keyup.enter="fetchOrders" />
      <el-input v-model="filters.platformOrderNo" clearable placeholder="平台订单号" class="search-input" @keyup.enter="fetchOrders" />
      <el-select v-model="filters.status" clearable placeholder="状态" class="status-select">
        <el-option label="支付成功" value="SUCCESS" />
        <el-option label="支付失败" value="FAILED" />
        <el-option label="处理中" value="PAYING" />
        <el-option label="待确认" value="UNKNOWN" />
      </el-select>
      <el-select v-model="filters.poolId" clearable placeholder="商户" class="status-select">
        <el-option v-for="pool in pools" :key="pool.id" :label="pool.poolName" :value="pool.id" />
      </el-select>
      <el-button :icon="Search" @click="fetchOrders">查询</el-button>
    </div>

    <el-table :data="orders" border>
      <el-table-column prop="merchantOrderNo" label="商户订单号" min-width="170" />
      <el-table-column prop="platformOrderNo" label="平台订单号" min-width="150" />
      <el-table-column prop="channelOrderNo" label="上游订单号" min-width="150" />
      <el-table-column prop="poolName" label="商户" min-width="130" />
      <el-table-column prop="accountName" label="支付参数" min-width="150" />
      <el-table-column prop="accountApiKey" label="APIKEY" min-width="170" />
      <el-table-column prop="amount" label="金额" width="100" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="queryCount" label="查单次数" width="100" />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/orders/${row.id}`)">详情</el-button>
          <el-button v-if="authStore.canManageOrders" link type="primary" :disabled="isTerminal(row.status)" @click="manualQuery(row)">查单</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.current"
      v-model:page-size="pagination.size"
      class="pagination"
      layout="total, sizes, prev, pager, next"
      :total="pagination.total"
      @current-change="fetchOrders"
      @size-change="fetchOrders"
    />
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { listMerchantPools, type MerchantPoolItem } from '@/api/merchant'
import { listOrders, queryOrder, type OrderItem } from '@/api/order'
import type { ApiResult } from '@/api/auth'
import type { PageResult } from '@/api/system'

const router = useRouter()
const authStore = useAuthStore()
const orders = ref<OrderItem[]>([])
const pools = ref<MerchantPoolItem[]>([])
const filters = reactive({ merchantOrderNo: '', platformOrderNo: '', status: '', poolId: undefined as number | undefined })
const pagination = reactive({ current: 1, size: 10, total: 0 })

onMounted(async () => {
  const poolResponse = await listMerchantPools({ current: 1, size: 100 }) as unknown as ApiResult<PageResult<MerchantPoolItem>>
  pools.value = poolResponse.data.records
  await fetchOrders()
})

async function fetchOrders() {
  const response = await listOrders({ current: pagination.current, size: pagination.size, ...filters }) as unknown as ApiResult<PageResult<OrderItem>>
  orders.value = response.data.records
  pagination.total = response.data.total
}

async function manualQuery(row: OrderItem) {
  await queryOrder(row.id)
  ElMessage.success('查单完成')
  await fetchOrders()
}

function isTerminal(status: string) {
  return status === 'SUCCESS' || status === 'FAILED'
}

function statusLabel(status: string) {
  return { SUCCESS: '支付成功', FAILED: '支付失败', PAYING: '处理中', UNKNOWN: '待确认' }[status] || status
}

function statusType(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'UNKNOWN') return 'warning'
  return 'info'
}
</script>
