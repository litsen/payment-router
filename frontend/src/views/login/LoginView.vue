<template>
  <main class="login-page" :style="loginBackgroundStyle">
    <section class="login-card">
      <div class="login-brand">
        <img :src="displayLogoUrl" alt="Payment Router" class="login-logo" />
        <h1>{{ appStore.name }}</h1>
      </div>
      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" class="login-button">登录</el-button>
      </el-form>
    </section>
    <footer class="login-footer">{{ appStore.copyrightText }}</footer>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import logoUrl from '@/assets/brand/logo.png'
import loginBgUrl from '@/assets/brand/login-bg.png'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const displayLogoUrl = computed(() => appStore.logoUrl || logoUrl)
const loginBackgroundStyle = computed(() => ({
  backgroundImage: `url(${appStore.loginBackgroundUrl || loginBgUrl})`
}))
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: ''
})

async function handleLogin() {
  if (loading.value) {
    return
  }
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
