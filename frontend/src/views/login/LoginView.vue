<template>
  <main class="login-page" :style="loginBackgroundStyle">
    <section class="login-card">
      <div class="login-brand">
        <img :src="logoUrl" alt="Payment Router" class="login-logo" />
        <h1>支付路由后台</h1>
      </div>
      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="login-button" @click="handleLogin">登录</el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import logoUrl from '@/assets/brand/logo.png'
import loginBgUrl from '@/assets/brand/login-bg.png'

const router = useRouter()
const authStore = useAuthStore()
const loginBackgroundStyle = {
  backgroundImage: `url(${loginBgUrl})`
}
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: ''
})

async function handleLogin() {
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>
