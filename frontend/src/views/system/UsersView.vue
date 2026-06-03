<template>
  <section class="page-panel">
    <div class="toolbar">
      <el-input v-model="keyword" clearable placeholder="搜索用户名或姓名" class="search-input" @keyup.enter="fetchUsers" />
      <el-button :icon="Search" @click="fetchUsers">查询</el-button>
      <el-button v-if="authStore.canManageUsers" type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
    </div>

    <el-table :data="users" border>
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="120" />
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="role in row.roles" :key="role" size="small" class="tag-gap">{{ role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column label="登录安全" min-width="220">
        <template #default="{ row }">
          <div class="login-security-cell">
            <el-tag v-if="row.loginLocked" type="danger" size="small">已锁定</el-tag>
            <el-tag v-else-if="(row.loginFailCount || 0) >= 3" type="warning" size="small">需验证码</el-tag>
            <el-tag v-else type="success" size="small">正常</el-tag>
            <span>失败 {{ row.loginFailCount || 0 }} 次</span>
            <span v-if="row.lockedIp">IP：{{ row.lockedIp }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="authStore.canManageUsers" label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.loginLocked || row.loginFailCount" link type="warning" @click="unlockLogin(row)">解除限制</el-button>
          <el-button link type="danger" @click="removeUser(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.current"
      v-model:page-size="pagination.size"
      class="pagination"
      layout="total, sizes, prev, pager, next"
      :total="pagination.total"
      @current-change="fetchUsers"
      @size-change="fetchUsers"
    />

    <el-dialog v-model="dialogVisible" :title="editingUser ? '编辑用户' : '新增用户'" width="520px">
      <el-form :model="form" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="Boolean(editingUser)" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password :placeholder="editingUser ? '留空则不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roles" multiple class="full-width">
            <el-option v-for="role in roles" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="full-width">
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { createUser, deleteUser, listRoles, listUsers, unlockUserLoginLimit, updateUser, type RoleItem, type UserItem } from '@/api/system'
import type { ApiResult } from '@/api/auth'
import type { PageResult } from '@/api/system'

const keyword = ref('')
const authStore = useAuthStore()
const users = ref<UserItem[]>([])
const roles = ref<RoleItem[]>([])
const dialogVisible = ref(false)
const editingUser = ref<UserItem | null>(null)
const pagination = reactive({ current: 1, size: 10, total: 0 })
const form = reactive({ username: '', password: '', realName: '', roles: [] as string[], status: 'ENABLED' })

onMounted(async () => {
  await Promise.all([fetchRoles(), fetchUsers()])
})

async function fetchUsers() {
  const response = await listUsers({ current: pagination.current, size: pagination.size, keyword: keyword.value }) as unknown as ApiResult<PageResult<UserItem>>
  users.value = response.data.records
  pagination.total = response.data.total
}

async function fetchRoles() {
  const response = await listRoles() as unknown as ApiResult<RoleItem[]>
  roles.value = response.data
}

function openCreate() {
  editingUser.value = null
  Object.assign(form, { username: '', password: '', realName: '', roles: ['OPERATOR'], status: 'ENABLED' })
  dialogVisible.value = true
}

function openEdit(user: UserItem) {
  editingUser.value = user
  Object.assign(form, { username: user.username, password: '', realName: user.realName, roles: [...user.roles], status: user.status })
  dialogVisible.value = true
}

async function submitUser() {
  if (editingUser.value) {
    await updateUser(editingUser.value.id, { password: form.password || undefined, realName: form.realName, roles: form.roles, status: form.status })
  } else {
    await createUser({ username: form.username, password: form.password, realName: form.realName, roles: form.roles, status: form.status })
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await fetchUsers()
}

async function removeUser(user: UserItem) {
  await ElMessageBox.confirm(`确认删除用户 ${user.username}？`, '删除确认', { type: 'warning' })
  await deleteUser(user.id)
  ElMessage.success('删除成功')
  await fetchUsers()
}

async function unlockLogin(user: UserItem) {
  await ElMessageBox.confirm(`确认解除 ${user.username} 的账号和 IP 登录限制？`, '解除登录限制', { type: 'warning' })
  await unlockUserLoginLimit(user.id)
  ElMessage.success('已解除登录限制')
  await fetchUsers()
}
</script>
