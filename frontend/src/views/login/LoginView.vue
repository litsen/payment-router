<template>
  <main class="login-page" :style="loginBackgroundStyle">
    <section class="login-card">
      <div class="login-brand">
        <img :src="displayLogoUrl" alt="Payment Router" class="login-logo" />
        <h1>{{ appStore.name }}</h1>
      </div>
      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" @blur="refreshLoginStatus" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item v-if="captchaRequired" label="图片验证码">
          <div class="login-captcha-row">
            <el-input v-model="form.captchaCode" placeholder="请输入验证码结果" @keyup.enter="handleLogin" />
            <button class="login-captcha-image" type="button" @click="refreshCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="captcha" />
            </button>
          </div>
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" class="login-button">登录</el-button>
      </el-form>
    </section>
    <footer class="login-footer">{{ appStore.copyrightText }}</footer>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { captchaApi, loginStatusApi } from '@/api/auth'
import type { ApiResult, CaptchaResponse, LoginSecurityStatusResponse } from '@/api/auth'
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
  username: '',
  password: '',
  captchaId: '',
  captchaCode: ''
})
const captchaRequired = ref(false)
const captchaImage = ref('')

onMounted(refreshLoginStatus)

async function refreshCaptcha() {
  if (!form.username) {
    return
  }
  captchaRequired.value = true
  const response = await captchaApi(form.username) as unknown as ApiResult<CaptchaResponse>
  captchaRequired.value = response.data.required
  form.captchaId = response.data.captchaId || ''
  form.captchaCode = ''
  captchaImage.value = response.data.imageBase64 || ''
}

async function refreshLoginStatus() {
  if (!form.username) {
    captchaRequired.value = false
    captchaImage.value = ''
    return
  }
  const response = await loginStatusApi(form.username) as unknown as ApiResult<LoginSecurityStatusResponse>
  captchaRequired.value = response.data.captchaRequired
  if (captchaRequired.value) {
    await refreshCaptcha()
  } else {
    form.captchaId = ''
    form.captchaCode = ''
    captchaImage.value = ''
  }
}

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
    const message = error?.response?.data?.message || '登录失败，请检查用户名和密码'
    ElMessage.error(message)
    if (message.includes('验证码')) {
      await refreshCaptcha()
    } else {
      await refreshLoginStatus()
    }
  } finally {
    loading.value = false
  }
}
</script>
