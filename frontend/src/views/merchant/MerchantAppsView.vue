<template>
  <section class="page-panel">
    <el-alert
      class="form-alert"
      type="info"
      show-icon
      :closable="false"
      title="外部 appId 已独立于商户编码；新接入请使用本页应用密钥签名，旧 poolCode 调用仅作兼容。"
    />

    <div class="toolbar">
      <el-input v-model="keyword" clearable placeholder="搜索应用名称或 appId" class="search-input" @keyup.enter="fetchApps" />
      <el-select v-model="poolId" clearable placeholder="所属商户" class="filter-select">
        <el-option v-for="pool in pools" :key="pool.id" :label="pool.poolName" :value="pool.id" />
      </el-select>
      <el-select v-model="status" clearable placeholder="状态" class="status-select">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <el-button :icon="Search" @click="fetchApps">查询</el-button>
      <el-button v-if="authStore.canManageMerchantApps" type="primary" :icon="Plus" @click="openCreate">新增应用</el-button>
    </div>

    <el-table :data="apps" border>
      <el-table-column prop="poolName" label="所属商户" min-width="140" />
      <el-table-column prop="appName" label="应用名称" min-width="150" />
      <el-table-column label="appId" min-width="180">
        <template #default="{ row }">
          <el-text tag="code">{{ row.appId }}</el-text>
        </template>
      </el-table-column>
      <el-table-column prop="secretMasked" label="appSecret" min-width="150" />
      <el-table-column prop="rateLimitPerMinute" label="分钟限流" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column v-if="authStore.canManageMerchantApps" label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="removeApp(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.current"
      v-model:page-size="pagination.size"
      class="pagination"
      layout="total, sizes, prev, pager, next"
      :total="pagination.total"
      @current-change="fetchApps"
      @size-change="fetchApps"
    />

    <el-dialog v-model="dialogVisible" :title="editingApp ? '编辑应用' : '新增应用'" width="620px">
      <el-form :model="form" label-position="top">
        <el-form-item label="所属商户">
          <el-select v-model="form.poolId" class="full-width">
            <el-option v-for="pool in pools" :key="pool.id" :label="pool.poolName" :value="pool.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="应用名称">
          <el-input v-model="form.appName" />
        </el-form-item>
        <el-form-item label="appId">
          <el-input v-model="form.appId" :disabled="Boolean(editingApp)" />
        </el-form-item>
        <el-form-item label="appSecret">
          <el-input :model-value="editingApp ? editingApp.secretMasked : '保存后由系统生成'" disabled />
        </el-form-item>
        <el-form-item label="回调地址白名单">
          <el-input v-model="form.notifyUrlWhitelist" type="textarea" :rows="3" placeholder="多个地址用逗号或换行分隔，按前缀匹配" />
        </el-form-item>
        <el-form-item label="分钟限流">
          <el-input-number v-model="form.rateLimitPerMinute" :min="1" :max="10000" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="full-width">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApp">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import {
  createMerchantApp,
  deleteMerchantApp,
  listMerchantApps,
  listMerchantPools,
  updateMerchantApp,
  type MerchantAppItem,
  type MerchantPoolItem
} from '@/api/merchant'
import type { ApiResult } from '@/api/auth'
import type { PageResult } from '@/api/system'

const authStore = useAuthStore()
const keyword = ref('')
const status = ref('')
const poolId = ref<number>()
const apps = ref<MerchantAppItem[]>([])
const pools = ref<MerchantPoolItem[]>([])
const dialogVisible = ref(false)
const editingApp = ref<MerchantAppItem | null>(null)
const pagination = reactive({ current: 1, size: 10, total: 0 })
const form = reactive({
  poolId: undefined as number | undefined,
  appId: '',
  appName: '',
  notifyUrlWhitelist: '',
  rateLimitPerMinute: 60,
  status: 'ENABLED',
  remark: ''
})

onMounted(async () => {
  await Promise.all([fetchPools(), fetchApps()])
})

async function fetchApps() {
  const response = await listMerchantApps({
    current: pagination.current,
    size: pagination.size,
    keyword: keyword.value,
    poolId: poolId.value,
    status: status.value
  }) as unknown as ApiResult<PageResult<MerchantAppItem>>
  apps.value = response.data.records
  pagination.total = response.data.total
}

async function fetchPools() {
  const response = await listMerchantPools({ current: 1, size: 1000 }) as unknown as ApiResult<PageResult<MerchantPoolItem>>
  pools.value = response.data.records
}

function openCreate() {
  editingApp.value = null
  Object.assign(form, {
    poolId: pools.value[0]?.id,
    appId: '',
    appName: '',
    notifyUrlWhitelist: '',
    rateLimitPerMinute: 60,
    status: 'ENABLED',
    remark: ''
  })
  dialogVisible.value = true
}

function openEdit(app: MerchantAppItem) {
  editingApp.value = app
  Object.assign(form, {
    poolId: app.poolId,
    appId: app.appId,
    appName: app.appName,
    notifyUrlWhitelist: app.notifyUrlWhitelist || '',
    rateLimitPerMinute: app.rateLimitPerMinute || 60,
    status: app.status,
    remark: app.remark || ''
  })
  dialogVisible.value = true
}

async function submitApp() {
  if (!form.poolId) {
    ElMessage.warning('请选择所属商户')
    return
  }
  if (editingApp.value) {
    await updateMerchantApp(editingApp.value.id, {
      poolId: form.poolId,
      appName: form.appName,
      notifyUrlWhitelist: form.notifyUrlWhitelist,
      rateLimitPerMinute: form.rateLimitPerMinute,
      status: form.status,
      remark: form.remark
    })
  } else {
    await createMerchantApp({
      poolId: form.poolId,
      appId: form.appId,
      appName: form.appName,
      notifyUrlWhitelist: form.notifyUrlWhitelist,
      rateLimitPerMinute: form.rateLimitPerMinute,
      status: form.status,
      remark: form.remark
    })
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await fetchApps()
}

async function removeApp(app: MerchantAppItem) {
  await ElMessageBox.confirm(`确认删除应用 ${app.appName}？`, '删除确认', { type: 'warning' })
  await deleteMerchantApp(app.id)
  ElMessage.success('删除成功')
  await fetchApps()
}
</script>
