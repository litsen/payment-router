<template>
  <section class="page-panel">
    <div class="toolbar">
      <el-input v-model="merchantOrderNo" clearable placeholder="搜索商户订单号" class="search-input" @keyup.enter="fetchRecords" />
      <el-select v-model="poolId" clearable placeholder="商户" class="status-select">
        <el-option v-for="pool in pools" :key="pool.id" :label="pool.poolName" :value="pool.id" />
      </el-select>
      <el-button :icon="Search" @click="fetchRecords">查询</el-button>
    </div>

    <el-table :data="records" border>
      <el-table-column prop="merchantOrderNo" label="商户订单号" min-width="170" />
      <el-table-column prop="poolName" label="商户" min-width="140" />
      <el-table-column prop="accountName" label="命中支付参数" min-width="160" />
      <el-table-column prop="routeRuleName" label="命中规则" min-width="150" />
      <el-table-column prop="routeType" label="路由类型" min-width="120" />
      <el-table-column prop="amount" label="金额" width="110" />
      <el-table-column prop="createdAt" label="路由时间" min-width="170" />
      <el-table-column label="快照" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openSnapshot(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.current"
      v-model:page-size="pagination.size"
      class="pagination"
      layout="total, sizes, prev, pager, next"
      :total="pagination.total"
      @current-change="fetchRecords"
      @size-change="fetchRecords"
    />

    <el-dialog v-model="snapshotVisible" title="路由快照" width="720px">
      <pre class="json-preview">{{ snapshotText }}</pre>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { listMerchantPools, type MerchantPoolItem } from '@/api/merchant'
import { listRouteRecords, type RouteRecordItem } from '@/api/route'
import type { ApiResult } from '@/api/auth'
import type { PageResult } from '@/api/system'

const merchantOrderNo = ref('')
const poolId = ref<number>()
const pools = ref<MerchantPoolItem[]>([])
const records = ref<RouteRecordItem[]>([])
const snapshotVisible = ref(false)
const snapshotText = ref('')
const pagination = reactive({ current: 1, size: 10, total: 0 })

onMounted(async () => {
  const poolsResponse = await listMerchantPools({ current: 1, size: 100 }) as unknown as ApiResult<PageResult<MerchantPoolItem>>
  pools.value = poolsResponse.data.records
  await fetchRecords()
})

async function fetchRecords() {
  const response = await listRouteRecords({ current: pagination.current, size: pagination.size, merchantOrderNo: merchantOrderNo.value, poolId: poolId.value }) as unknown as ApiResult<PageResult<RouteRecordItem>>
  records.value = response.data.records
  pagination.total = response.data.total
}

function openSnapshot(row: RouteRecordItem) {
  try {
    snapshotText.value = JSON.stringify(JSON.parse(row.routeSnapshotJson || '{}'), null, 2)
  } catch {
    snapshotText.value = row.routeSnapshotJson || '{}'
  }
  snapshotVisible.value = true
}
</script>
