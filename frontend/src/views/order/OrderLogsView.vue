<template>
  <section class="page-panel">
    <div class="toolbar">
      <el-input v-model="merchantOrderNo" clearable placeholder="商户订单号" class="search-input" @keyup.enter="fetchLogs" />
      <el-select v-model="direction" clearable placeholder="方向" class="status-select">
        <el-option label="下游请求" value="INBOUND" />
        <el-option label="请求上游" value="OUTBOUND" />
      </el-select>
      <el-select v-model="apiType" clearable placeholder="类型" class="status-select">
        <el-option label="条码支付" value="BARCODE_PAY" />
        <el-option label="查单" value="QUERY" />
        <el-option label="通知" value="NOTIFY" />
      </el-select>
      <el-button :icon="Search" @click="fetchLogs">查询</el-button>
    </div>
    <el-table :data="logs" border>
      <el-table-column prop="merchantOrderNo" label="商户订单号" min-width="170" />
      <el-table-column prop="direction" label="方向" width="100" />
      <el-table-column prop="apiType" label="类型" width="120" />
      <el-table-column prop="resultStatus" label="结果" width="120" />
      <el-table-column prop="httpStatus" label="HTTP" width="90" />
      <el-table-column label="时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="报文" width="100" fixed="right">
        <template #default="{ row }"><el-button link type="primary" @click="openLog(row)">查看</el-button></template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="pagination.current" v-model:page-size="pagination.size" class="pagination" layout="total, sizes, prev, pager, next" :total="pagination.total" @current-change="fetchLogs" @size-change="fetchLogs" />
    <el-dialog v-model="dialogVisible" title="请求响应报文" width="820px">
      <pre class="json-preview">{{ logText }}</pre>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { listOrderLogs, type OrderLogItem } from '@/api/order'
import type { ApiResult } from '@/api/auth'
import type { PageResult } from '@/api/system'
import { formatDateTime } from '@/utils/format'

const logs = ref<OrderLogItem[]>([])
const merchantOrderNo = ref('')
const direction = ref('')
const apiType = ref('')
const dialogVisible = ref(false)
const logText = ref('')
const pagination = reactive({ current: 1, size: 10, total: 0 })

onMounted(fetchLogs)

async function fetchLogs() {
  const response = await listOrderLogs({ current: pagination.current, size: pagination.size, merchantOrderNo: merchantOrderNo.value, direction: direction.value, apiType: apiType.value }) as unknown as ApiResult<PageResult<OrderLogItem>>
  logs.value = response.data.records
  pagination.total = response.data.total
}

function openLog(row: OrderLogItem) {
  logText.value = JSON.stringify({ request: tryJson(row.requestBody), response: tryJson(row.responseBody), error: row.errorMessage }, null, 2)
  dialogVisible.value = true
}

function tryJson(text?: string) {
  if (!text) return null
  try { return JSON.parse(text) } catch { return text }
}
</script>
