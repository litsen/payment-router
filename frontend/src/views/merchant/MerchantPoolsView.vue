<template>
  <section class="page-panel">
    <el-alert
      class="form-alert"
      type="info"
      show-icon
      :closable="false"
      title="接口接入凭证随商户自动生成：appId 使用商户编码，appSecret 由系统随机生成并与 appId 唯一匹配。"
    />

    <div class="toolbar">
      <el-input v-model="keyword" clearable placeholder="搜索商户名称或 appId" class="search-input" @keyup.enter="fetchPools" />
      <el-select v-model="status" clearable placeholder="状态" class="status-select">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <el-button :icon="Search" @click="fetchPools">查询</el-button>
      <el-button v-if="authStore.canManageMerchantPools" type="primary" :icon="Plus" @click="openCreate">新增商户</el-button>
    </div>

    <el-table :data="pools" border>
      <el-table-column prop="poolName" label="商户名称" min-width="160" />
      <el-table-column label="appId" min-width="190">
        <template #default="{ row }">
          <el-text tag="code">{{ row.appId || row.poolCode }}</el-text>
        </template>
      </el-table-column>
      <el-table-column label="appSecret" min-width="180">
        <template #default="{ row }">
          <el-text tag="code">{{ row.appSecretMasked || '-' }}</el-text>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column v-if="authStore.canManageMerchantPools" label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="resetSecret(row)">重置密钥</el-button>
          <el-button link type="danger" @click="removePool(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.current"
      v-model:page-size="pagination.size"
      class="pagination"
      layout="total, sizes, prev, pager, next"
      :total="pagination.total"
      @current-change="fetchPools"
      @size-change="fetchPools"
    />

    <el-dialog v-model="dialogVisible" :title="editingPool ? '编辑商户' : '新增商户'" width="520px">
      <el-form :model="form" label-position="top">
        <el-form-item label="商户名称">
          <el-input v-model="form.poolName" />
        </el-form-item>
        <el-form-item v-if="editingPool" label="appId">
          <el-input :model-value="editingPool.poolCode" disabled />
        </el-form-item>
        <el-form-item v-if="editingPool" label="appSecret">
          <el-input :model-value="editingPool.appSecretMasked || '-'" disabled />
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
        <el-button type="primary" @click="submitPool">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="secretDialogVisible" title="请保存新的 appSecret" width="560px">
      <el-alert
        type="warning"
        show-icon
        :closable="false"
        title="appSecret 只在本次生成后明文显示，关闭后只能重置。"
      />
      <div class="secret-box">
        <div class="secret-row">
          <span>appId</span>
          <el-text tag="code">{{ generatedCredential.appId }}</el-text>
        </div>
        <div class="secret-row">
          <span>appSecret</span>
          <el-text tag="code">{{ generatedCredential.appSecret }}</el-text>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="secretDialogVisible = false">我已保存</el-button>
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
  createMerchantPool,
  deleteMerchantPool,
  listMerchantPools,
  resetMerchantPoolAppSecret,
  updateMerchantPool,
  type MerchantPoolItem
} from '@/api/merchant'
import type { ApiResult } from '@/api/auth'
import type { PageResult } from '@/api/system'

const keyword = ref('')
const authStore = useAuthStore()
const status = ref('')
const pools = ref<MerchantPoolItem[]>([])
const dialogVisible = ref(false)
const secretDialogVisible = ref(false)
const editingPool = ref<MerchantPoolItem | null>(null)
const pagination = reactive({ current: 1, size: 10, total: 0 })
const form = reactive({ poolName: '', status: 'ENABLED', remark: '' })
const generatedCredential = reactive({ appId: '', appSecret: '' })

onMounted(fetchPools)

async function fetchPools() {
  const response = await listMerchantPools({
    current: pagination.current,
    size: pagination.size,
    keyword: keyword.value,
    status: status.value
  }) as unknown as ApiResult<PageResult<MerchantPoolItem>>
  pools.value = response.data.records
  pagination.total = response.data.total
}

function openCreate() {
  editingPool.value = null
  Object.assign(form, { poolName: '', status: 'ENABLED', remark: '' })
  dialogVisible.value = true
}

function openEdit(pool: MerchantPoolItem) {
  editingPool.value = pool
  Object.assign(form, {
    poolName: pool.poolName,
    status: pool.status,
    remark: pool.remark || ''
  })
  dialogVisible.value = true
}

async function submitPool() {
  if (editingPool.value) {
    await updateMerchantPool(editingPool.value.id, { poolName: form.poolName, status: form.status, remark: form.remark })
  } else {
    const response = await createMerchantPool({ poolName: form.poolName, status: form.status, remark: form.remark }) as unknown as ApiResult<MerchantPoolItem>
    showGeneratedSecret(response.data)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await fetchPools()
}

async function resetSecret(pool: MerchantPoolItem) {
  await ElMessageBox.confirm(`确认重置商户 ${pool.poolName} 的 appSecret？旧密钥会立即失效。`, '重置密钥确认', { type: 'warning' })
  const response = await resetMerchantPoolAppSecret(pool.id) as unknown as ApiResult<MerchantPoolItem>
  showGeneratedSecret(response.data)
  ElMessage.success('密钥已重置')
  await fetchPools()
}

function showGeneratedSecret(pool: MerchantPoolItem) {
  if (!pool.plainAppSecret) {
    return
  }
  generatedCredential.appId = pool.appId || pool.poolCode
  generatedCredential.appSecret = pool.plainAppSecret
  secretDialogVisible.value = true
}

async function removePool(pool: MerchantPoolItem) {
  await ElMessageBox.confirm(`确认删除商户 ${pool.poolName}？`, '删除确认', { type: 'warning' })
  await deleteMerchantPool(pool.id)
  ElMessage.success('删除成功')
  await fetchPools()
}
</script>

<style scoped>
.secret-box {
  margin-top: 16px;
  display: grid;
  gap: 12px;
}

.secret-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}
</style>
