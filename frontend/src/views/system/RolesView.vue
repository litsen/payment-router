<template>
  <section class="page-panel">
    <div class="toolbar">
      <el-button v-if="authStore.canManageRoles" type="primary" :icon="Plus" @click="openCreate">新增角色</el-button>
    </div>

    <el-table :data="roles" border>
      <el-table-column prop="roleCode" label="角色编码" min-width="150" />
      <el-table-column prop="roleName" label="角色名称" min-width="140" />
      <el-table-column prop="description" label="说明" min-width="180" />
      <el-table-column label="权限" min-width="320">
        <template #default="{ row }">
          <el-tag v-for="permission in readablePermissions(row.permissions)" :key="permission" size="small" class="tag-gap">
            {{ permission }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="authStore.canManageRoles" label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="removeRole(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingRole ? '编辑角色' : '新增角色'" width="680px">
      <el-form :model="form" label-position="top">
        <el-form-item label="角色编码">
          <el-input v-model="form.roleCode" :disabled="Boolean(editingRole)" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" />
        </el-form-item>
        <el-form-item label="权限配置">
          <el-tree
            ref="permissionTreeRef"
            class="permission-tree"
            :data="permissionTree"
            show-checkbox
            node-key="value"
            default-expand-all
            v-loading="permissionTreeLoading"
            :props="{ label: 'label', children: 'children' }"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { createRole, deleteRole, listPermissionTree, listRoles, updateRole, type PermissionNode, type RoleItem } from '@/api/system'
import type { ApiResult } from '@/api/auth'

const authStore = useAuthStore()
const roles = ref<RoleItem[]>([])
const permissionTree = ref<PermissionNode[]>([])
const permissionTreeLoading = ref(false)
const permissionLabelMap = computed(() => flattenPermissions(permissionTree.value))
const dialogVisible = ref(false)
const editingRole = ref<RoleItem | null>(null)
const permissionTreeRef = ref()
const form = reactive({ roleCode: '', roleName: '', description: '' })

onMounted(async () => {
  await Promise.all([fetchPermissionTree(), fetchRoles()])
})

async function fetchRoles() {
  const response = await listRoles() as unknown as ApiResult<RoleItem[]>
  roles.value = response.data
}

async function fetchPermissionTree() {
  permissionTreeLoading.value = true
  try {
    const response = await listPermissionTree() as unknown as ApiResult<PermissionNode[]>
    permissionTree.value = response.data
  } finally {
    permissionTreeLoading.value = false
  }
}

async function openCreate() {
  editingRole.value = null
  Object.assign(form, { roleCode: '', roleName: '', description: '' })
  dialogVisible.value = true
  await nextTick()
  permissionTreeRef.value?.setCheckedKeys(['dashboard:view'])
}

async function openEdit(role: RoleItem) {
  editingRole.value = role
  Object.assign(form, {
    roleCode: role.roleCode,
    roleName: role.roleName,
    description: role.description || ''
  })
  dialogVisible.value = true
  await nextTick()
  permissionTreeRef.value?.setCheckedKeys([...role.permissions])
}

async function submitRole() {
  const permissions = checkedPermissions()
  if (editingRole.value) {
    await updateRole(editingRole.value.id, { roleName: form.roleName, description: form.description, permissions })
  } else {
    await createRole({ roleCode: form.roleCode, roleName: form.roleName, description: form.description, permissions })
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await fetchRoles()
}

async function removeRole(role: RoleItem) {
  await ElMessageBox.confirm(`确认删除角色 ${role.roleCode}？`, '删除确认', { type: 'warning' })
  await deleteRole(role.id)
  ElMessage.success('删除成功')
  await fetchRoles()
}

function checkedPermissions() {
  const keys = permissionTreeRef.value?.getCheckedKeys(true) ?? []
  return keys.filter((key: string) => !key.startsWith('group:'))
}

function readablePermissions(permissions: string[]) {
  return permissions.map(permission => permissionLabelMap.value[permission] || permission)
}

function flattenPermissions(nodes: PermissionNode[]) {
  const map: Record<string, string> = {}
  for (const node of nodes) {
    if (node.children?.length) {
      Object.assign(map, flattenPermissions(node.children))
    } else {
      map[node.value] = node.label
    }
  }
  return map
}
</script>
