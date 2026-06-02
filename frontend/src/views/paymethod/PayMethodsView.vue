<template>
  <section class="page-panel">
    <el-table :data="methods" border>
      <el-table-column prop="methodName" label="支付方式" min-width="150" />
      <el-table-column prop="methodCode" label="编码" min-width="140" />
      <el-table-column label="开关" width="140">
        <template #default="{ row }">
          <el-switch
            :model-value="row.enabled"
            :disabled="row.reserved || !authStore.canManagePayMethods"
            active-text="启用"
            inactive-text="停用"
            inline-prompt
            @change="toggleMethod(row, Boolean($event))"
          />
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
      <el-table-column v-if="authStore.canManagePayMethods" label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="编辑支付方式" width="520px">
      <el-form :model="form" label-position="top">
        <el-form-item label="支付方式名称">
          <el-input v-model="form.methodName" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" :disabled="editingMethod?.reserved" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="1" :max="9999" class="full-width" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMethod">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  disablePayMethod,
  enablePayMethod,
  listPayMethods,
  updatePayMethod,
  type PayMethodItem
} from '@/api/payMethod'
import type { ApiResult } from '@/api/auth'

const methods = ref<PayMethodItem[]>([])
const authStore = useAuthStore()
const dialogVisible = ref(false)
const editingMethod = ref<PayMethodItem | null>(null)
const form = reactive({ methodName: '', enabled: false, sortOrder: 100, remark: '' })

onMounted(fetchMethods)

async function fetchMethods() {
  const response = await listPayMethods() as unknown as ApiResult<PayMethodItem[]>
  methods.value = response.data
}

async function toggleMethod(method: PayMethodItem, enabled: boolean) {
  if (enabled) {
    await enablePayMethod(method.id)
    ElMessage.success('已启用')
  } else {
    await disablePayMethod(method.id)
    ElMessage.success('已停用')
  }
  await fetchMethods()
}

function openEdit(method: PayMethodItem) {
  editingMethod.value = method
  Object.assign(form, {
    methodName: method.methodName,
    enabled: method.enabled,
    sortOrder: method.sortOrder,
    remark: method.remark || ''
  })
  dialogVisible.value = true
}

async function submitMethod() {
  if (!editingMethod.value) {
    return
  }
  await updatePayMethod(editingMethod.value.id, {
    methodName: form.methodName,
    enabled: form.enabled,
    sortOrder: form.sortOrder,
    remark: form.remark
  })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await fetchMethods()
}
</script>
