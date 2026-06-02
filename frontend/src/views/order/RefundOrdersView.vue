<template>
  <section class="page-panel">
    <div class="toolbar">
      <el-input v-model="filters.merchantOrderNo" clearable placeholder="商户订单号" class="search-input" @keyup.enter="fetchRefunds" />
      <el-input v-model="filters.merchantRefundNo" clearable placeholder="退款订单号" class="search-input" @keyup.enter="fetchRefunds" />
      <el-select v-model="filters.status" clearable placeholder="状态" class="status-select">
        <el-option label="退款成功" value="SUCCESS" />
        <el-option label="退款失败" value="FAILED" />
        <el-option label="处理中" value="PROCESSING" />
        <el-option label="待确认" value="UNKNOWN" />
        <el-option label="初始化" value="INIT" />
      </el-select>
      <el-select v-model="filters.poolId" clearable placeholder="商户" class="status-select">
        <el-option v-for="pool in pools" :key="pool.id" :label="pool.poolName" :value="pool.id" />
      </el-select>
      <el-button :icon="Search" @click="fetchRefunds">查询</el-button>
    </div>

    <el-table :data="refunds" border>
      <el-table-column prop="merchantRefundNo" label="退款订单号" min-width="170" />
      <el-table-column prop="merchantOrderNo" label="商户订单号" min-width="170" />
      <el-table-column prop="platformOrderNo" label="平台订单号" min-width="150" />
      <el-table-column prop="poolName" label="商户" min-width="130" />
      <el-table-column prop="accountName" label="支付参数" min-width="150" />
      <el-table-column prop="orderAmount" label="订单金额" width="120" />
      <el-table-column prop="refundAmount" label="退款金额" width="120" />
      <el-table-column label="状态" width="115">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="queryCount" label="查单次数" width="100" />
      <el-table-column label="创建时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/refunds/${row.id}`)">详情</el-button>
          <el-button v-if="authStore.canManageRefunds" link type="primary" :disabled="isTerminal(row.status)" @click="manualQuery(row)">查单</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.current"
      v-model:page-size="pagination.size"
      class="pagination"
      layout="total, sizes, prev, pager, next"
      :total="pagination.total"
      @current-change="fetchRefunds"
      @size-change="fetchRefunds"
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
import { listRefundOrders, queryRefundOrder, type RefundOrderItem } from '@/api/order'
import type { ApiResult } from '@/api/auth'
import type { PageResult } from '@/api/system'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const authStore = useAuthStore()
const refunds = ref<RefundOrderItem[]>([])
const pools = ref<MerchantPoolItem[]>([])
const filters = reactive({ merchantOrderNo: '', merchantRefundNo: '', status: '', poolId: undefined as number | undefined })
const pagination = reactive({ current: 1, size: 10, total: 0 })

onMounted(async () => {
  const poolResponse = await listMerchantPools({ current: 1, size: 100 }) as unknown as ApiResult<PageResult<MerchantPoolItem>>
  pools.value = poolResponse.data.records
  await fetchRefunds()
})

async function fetchRefunds() {
  const response = await listRefundOrders({ current: pagination.current, size: pagination.size, ...filters }) as unknown as ApiResult<PageResult<RefundOrderItem>>
  refunds.value = response.data.records
  pagination.total = response.data.total
}

async function manualQuery(row: RefundOrderItem) {
  await queryRefundOrder(row.id)
  ElMessage.success('查单完成')
  await fetchRefunds()
}

function isTerminal(status: string) {
  return status === 'SUCCESS' || status === 'FAILED'
}

function statusLabel(status: string) {
  return { SUCCESS: '退款成功', FAILED: '退款失败', PROCESSING: '处理中', UNKNOWN: '待确认', INIT: '初始化' }[status] || status
}

function statusType(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'UNKNOWN') return 'warning'
  return 'info'
}
</script>
