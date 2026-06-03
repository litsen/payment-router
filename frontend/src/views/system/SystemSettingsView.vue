<template>
  <section class="page-panel system-settings-page">
    <el-form :model="form" label-position="top" class="system-settings-form">
      <el-form-item label="网站名称">
        <el-input v-model="form.siteName" :disabled="!authStore.canManageSystemSettings" maxlength="64" />
      </el-form-item>
      <el-form-item label="版权文案">
        <el-input v-model="form.copyrightText" :disabled="!authStore.canManageSystemSettings" maxlength="128" />
      </el-form-item>

      <div class="asset-grid">
        <div class="asset-field">
          <div class="asset-title">LOGO</div>
          <img :src="form.logoUrl || logoUrl" alt="logo" class="asset-preview logo-preview" />
          <el-input v-model="form.logoUrl" :disabled="!authStore.canManageSystemSettings" placeholder="/uploads/system/logo/xxx.png" />
          <el-upload
            v-if="authStore.canManageSystemSettings"
            :show-file-list="false"
            accept="image/png,image/jpeg,image/gif,image/webp,image/x-icon,image/vnd.microsoft.icon"
            :http-request="uploadLogo"
          >
            <el-button type="primary">上传 LOGO</el-button>
          </el-upload>
        </div>

        <div class="asset-field">
          <div class="asset-title">登录页背景图</div>
          <img :src="form.loginBackgroundUrl || loginBgUrl" alt="login background" class="asset-preview background-preview" />
          <el-input v-model="form.loginBackgroundUrl" :disabled="!authStore.canManageSystemSettings" placeholder="/uploads/system/loginBackground/xxx.png" />
          <el-upload
            v-if="authStore.canManageSystemSettings"
            :show-file-list="false"
            accept="image/png,image/jpeg,image/gif,image/webp"
            :http-request="uploadLoginBackground"
          >
            <el-button type="primary">上传背景图</el-button>
          </el-upload>
        </div>

        <div class="asset-field">
          <div class="asset-title">favicon 图片</div>
          <img :src="form.faviconUrl || form.logoUrl || logoUrl" alt="favicon" class="asset-preview favicon-preview" />
          <el-input v-model="form.faviconUrl" :disabled="!authStore.canManageSystemSettings" placeholder="/uploads/system/favicon/xxx.ico" />
          <el-upload
            v-if="authStore.canManageSystemSettings"
            :show-file-list="false"
            accept="image/png,image/jpeg,image/gif,image/webp,image/x-icon,image/vnd.microsoft.icon"
            :http-request="uploadFavicon"
          >
            <el-button type="primary">上传 favicon</el-button>
          </el-upload>
        </div>
      </div>

      <div class="toolbar">
        <el-button type="primary" :loading="saving" :disabled="!authStore.canManageSystemSettings" @click="saveSettings">保存设置</el-button>
      </div>
    </el-form>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemSettings, updateSystemSettings, uploadSystemAsset, type SystemSettings } from '@/api/system'
import type { ApiResult } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import logoUrl from '@/assets/brand/logo.png'
import loginBgUrl from '@/assets/brand/login-bg.png'

const authStore = useAuthStore()
const appStore = useAppStore()
const saving = ref(false)
const form = reactive<SystemSettings>({
  siteName: '支付路由后台',
  copyrightText: 'Copyright © xxx公司',
  logoUrl: '/brand/logo.png',
  loginBackgroundUrl: '/brand/login-bg.png',
  faviconUrl: '/brand/logo.png'
})

onMounted(fetchSettings)

async function fetchSettings() {
  const response = await getSystemSettings() as unknown as ApiResult<SystemSettings>
  Object.assign(form, response.data)
}

async function uploadAsset(type: 'logo' | 'loginBackground' | 'favicon', options: any) {
  const response = await uploadSystemAsset(type, options.file) as unknown as ApiResult<{ url: string }>
  if (type === 'logo') {
    form.logoUrl = response.data.url
  } else if (type === 'loginBackground') {
    form.loginBackgroundUrl = response.data.url
  } else {
    form.faviconUrl = response.data.url
  }
  ElMessage.success('上传成功')
}

function uploadLogo(options: any) {
  return uploadAsset('logo', options)
}

function uploadLoginBackground(options: any) {
  return uploadAsset('loginBackground', options)
}

function uploadFavicon(options: any) {
  return uploadAsset('favicon', options)
}

async function saveSettings() {
  saving.value = true
  try {
    const response = await updateSystemSettings(form) as unknown as ApiResult<SystemSettings>
    Object.assign(form, response.data)
    appStore.applySettings(response.data)
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}
</script>
