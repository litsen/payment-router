<template>
  <section class="page-panel">
    <div class="toolbar">
      <el-input v-model="keyword" clearable placeholder="搜索商户号名称" class="search-input" @keyup.enter="fetchAccounts" />
      <el-select v-model="poolId" clearable placeholder="所属商户" class="filter-select">
        <el-option v-for="pool in pools" :key="pool.id" :label="pool.poolName" :value="pool.id" />
      </el-select>
      <el-select v-model="status" clearable placeholder="状态" class="status-select">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <el-button :icon="Search" @click="fetchAccounts">查询</el-button>
      <el-button v-if="authStore.canManageMerchantAccounts" type="primary" :icon="Plus" @click="router.push('/merchant/accounts/create')">新增支付参数</el-button>
    </div>

    <el-table :data="accounts" border>
      <el-table-column prop="poolName" label="所属商户" min-width="140" />
      <el-table-column prop="accountName" label="商户号名称" min-width="160" />
      <el-table-column prop="apiKeyMasked" label="APIKEY" min-width="150" />
      <el-table-column prop="signKeyMasked" label="APISecret" min-width="150" />
      <el-table-column prop="supportPayMethods" label="支付方式" min-width="150" />
      <el-table-column prop="priority" label="优先级" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="authStore.canManageMerchantAccounts" label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/merchant/accounts/${row.id}/edit`)">编辑</el-button>
          <el-button v-if="row.status !== 'ENABLED'" link type="success" @click="enableAccount(row)">启用</el-button>
          <el-button v-else link type="warning" @click="disableAccount(row)">停用</el-button>
          <el-button link type="danger" @click="removeAccount(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.current"
      v-model:page-size="pagination.size"
      class="pagination"
      layout="total, sizes, prev, pager, next"
      :total="pagination.total"
      @current-change="fetchAccounts"
      @size-change="fetchAccounts"
    />
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import {
  deleteMerchantAccount,
  disableMerchantAccount,
  enableMerchantAccount,
  listMerchantAccounts,
  listMerchantPools,
  type MerchantAccountItem,
  type MerchantPoolItem
} from '@/api/merchant'
import type { ApiResult } from '@/api/auth'
import type { PageResult } from '@/api/system'

const router = useRouter()
const authStore = useAuthStore()
const keyword = ref('')
const status = ref('')
const poolId = ref<number>()
const accounts = ref<MerchantAccountItem[]>([])
const pools = ref<MerchantPoolItem[]>([])
const pagination = reactive({ current: 1, size: 10, total: 0 })

onMounted(async () => {
  await Promise.all([fetchPools(), fetchAccounts()])
})

async function fetchAccounts() {
  const response = await listMerchantAccounts({
    current: pagination.current,
    size: pagination.size,
    keyword: keyword.value,
    poolId: poolId.value,
    status: status.value
  }) as unknown as ApiResult<PageResult<MerchantAccountItem>>
  accounts.value = response.data.records
  pagination.total = response.data.total
}

async function fetchPools() {
  const response = await listMerchantPools({ current: 1, size: 1000 }) as unknown as ApiResult<PageResult<MerchantPoolItem>>
  pools.value = response.data.records
}

async function enableAccount(account: MerchantAccountItem) {
  await enableMerchantAccount(account.id)
  ElMessage.success('已启用')
  await fetchAccounts()
}

async function disableAccount(account: MerchantAccountItem) {
  await disableMerchantAccount(account.id)
  ElMessage.success('已停用')
  await fetchAccounts()
}

async function removeAccount(account: MerchantAccountItem) {
  await ElMessageBox.confirm(`确认删除支付参数 ${account.accountName}？`, '删除确认', { type: 'warning' })
  await deleteMerchantAccount(account.id)
  ElMessage.success('删除成功')
  await fetchAccounts()
}
</script>
