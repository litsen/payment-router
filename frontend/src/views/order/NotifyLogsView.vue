<template>
  <section class="page-panel">
    <div class="toolbar">
      <el-input v-model="merchantOrderNo" clearable placeholder="商户订单号" class="search-input" @keyup.enter="fetchLogs" />
      <el-button :icon="Search" @click="fetchLogs">查询</el-button>
    </div>
    <el-table :data="logs" border>
      <el-table-column prop="merchantOrderNo" label="商户订单号" min-width="170" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }"><el-tag>{{ directionLabel(row) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="验签" width="90">
        <template #default="{ row }"><el-tag :type="row.verified ? 'success' : 'danger'">{{ isOutbound(row) ? '已签名' : row.verified ? '通过' : '失败' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="处理" width="90">
        <template #default="{ row }"><el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="errorMessage" label="错误" min-width="180" />
      <el-table-column prop="createdAt" label="时间" min-width="170" />
      <el-table-column label="报文" width="100" fixed="right">
        <template #default="{ row }"><el-button link type="primary" @click="openLog(row)">查看</el-button></template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="pagination.current" v-model:page-size="pagination.size" class="pagination" layout="total, sizes, prev, pager, next" :total="pagination.total" @current-change="fetchLogs" @size-change="fetchLogs" />
    <el-dialog v-model="dialogVisible" title="通知报文" width="820px">
      <pre class="json-preview">{{ logText }}</pre>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { listNotifyLogs, type NotifyLogItem } from '@/api/order'
import type { ApiResult } from '@/api/auth'
import type { PageResult } from '@/api/system'

const logs = ref<NotifyLogItem[]>([])
const merchantOrderNo = ref('')
const dialogVisible = ref(false)
const logText = ref('')
const pagination = reactive({ current: 1, size: 10, total: 0 })

onMounted(fetchLogs)

async function fetchLogs() {
  const response = await listNotifyLogs({ current: pagination.current, size: pagination.size, merchantOrderNo: merchantOrderNo.value }) as unknown as ApiResult<PageResult<NotifyLogItem>>
  logs.value = response.data.records
  pagination.total = response.data.total
}

function openLog(row: NotifyLogItem) {
  try {
    logText.value = JSON.stringify(JSON.parse(row.notifyBody || '{}'), null, 2)
  } catch {
    logText.value = row.notifyBody || ''
  }
  dialogVisible.value = true
}

function directionLabel(row: NotifyLogItem) {
  return isOutbound(row) ? '下游通知' : '上游回调'
}

function isOutbound(row: NotifyLogItem) {
  return parseNotifyBody(row).direction === 'OUTBOUND'
}

function parseNotifyBody(row: NotifyLogItem) {
  try {
    return JSON.parse(row.notifyBody || '{}')
  } catch {
    return {}
  }
}
</script>
