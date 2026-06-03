<template>
  <el-container class="app-shell">
    <el-aside class="app-sidebar" width="220px">
      <div class="brand">
        <img :src="displayLogoUrl" alt="Payment Router" class="brand-logo" />
        <span>{{ appStore.name }}</span>
      </div>
      <el-menu router :default-active="route.path" class="sidebar-menu">
        <el-menu-item index="/">
          <el-icon><DataBoard /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-sub-menu v-if="showApiDocsMenu" index="/api-docs">
          <template #title>
            <el-icon><Reading /></el-icon>
            <span>接口文档</span>
          </template>
          <el-menu-item index="/api-docs/barcode-pay">
            <el-icon><Document /></el-icon>
            <span>条码支付接口</span>
          </el-menu-item>
          <el-menu-item index="/api-docs/scan-pay">
            <el-icon><Document /></el-icon>
            <span>聚合扫码支付接口</span>
          </el-menu-item>
          <el-menu-item index="/api-docs/qrcode-pay">
            <el-icon><Document /></el-icon>
            <span>扫码支付接口</span>
          </el-menu-item>
          <el-menu-item index="/api-docs/h5-pay">
            <el-icon><Document /></el-icon>
            <span>H5/链接跳转支付接口</span>
          </el-menu-item>
          <el-menu-item index="/api-docs/wechat-jsapi-pay">
            <el-icon><Document /></el-icon>
            <span>微信公众号和小程序支付接口</span>
          </el-menu-item>
          <el-menu-item index="/api-docs/alipay-jsapi-pay">
            <el-icon><Document /></el-icon>
            <span>支付宝生活号和小程序支付接口</span>
          </el-menu-item>
          <el-menu-item index="/api-docs/query-pay">
            <el-icon><Search /></el-icon>
            <span>查单接口</span>
          </el-menu-item>
          <el-menu-item index="/api-docs/refund">
            <el-icon><Document /></el-icon>
            <span>退款接口</span>
          </el-menu-item>
          <el-menu-item index="/api-docs/refund-query">
            <el-icon><Search /></el-icon>
            <span>退款查询接口</span>
          </el-menu-item>
          <el-menu-item index="/api-docs/sign">
            <el-icon><Key /></el-icon>
            <span>签名规则</span>
          </el-menu-item>
          <el-menu-item index="/api-docs/error-codes">
            <el-icon><Warning /></el-icon>
            <span>错误码说明</span>
          </el-menu-item>
        </el-sub-menu>
        <el-sub-menu v-if="showMerchantMenu" index="/merchant">
          <template #title>
            <el-icon><CreditCard /></el-icon>
            <span>商户配置</span>
          </template>
          <el-menu-item v-if="authStore.canViewMerchantPools" index="/merchant/pools">
            <el-icon><Files /></el-icon>
            <span>商户管理</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.canViewMerchantAccounts" index="/merchant/accounts">
            <el-icon><Tickets /></el-icon>
            <span>支付参数配置</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.canViewPayMethods" index="/pay-methods">
            <el-icon><SwitchButton /></el-icon>
            <span>支付方式配置</span>
          </el-menu-item>
        </el-sub-menu>
        <el-sub-menu v-if="showOrderMenu" index="/orders">
          <template #title>
            <el-icon><List /></el-icon>
            <span>订单管理</span>
          </template>
          <el-menu-item v-if="authStore.canViewOrders" index="/orders">
            <el-icon><Notebook /></el-icon>
            <span>订单流水</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.canViewRefunds" index="/refunds">
            <el-icon><Refresh /></el-icon>
            <span>退款流水</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.canViewOrderLogs" index="/order-logs">
            <el-icon><Memo /></el-icon>
            <span>请求日志</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.canViewOrderLogs" index="/notify-logs">
            <el-icon><MessageBox /></el-icon>
            <span>通知日志</span>
          </el-menu-item>
        </el-sub-menu>
        <el-sub-menu v-if="showRouteMenu" index="/route">
          <template #title>
            <el-icon><Share /></el-icon>
            <span>路由管理</span>
          </template>
          <el-menu-item v-if="authStore.canViewRouteRules" index="/route/rules">
            <el-icon><Operation /></el-icon>
            <span>路由规则</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.canViewRouteRecords" index="/route/records">
            <el-icon><Document /></el-icon>
            <span>路由记录</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.canTestRoute" index="/route/test">
            <el-icon><Aim /></el-icon>
            <span>路由测试</span>
          </el-menu-item>
        </el-sub-menu>
        <el-sub-menu v-if="showSystemMenu" index="/system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item v-if="authStore.canViewUsers" index="/system/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.canViewRoles" index="/system/roles">
            <el-icon><Key /></el-icon>
            <span>角色管理</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.canViewSystemSettings" index="/system/settings">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="page-title">{{ currentTitle }}</div>
        <div class="header-actions">
          <el-dropdown trigger="hover" @command="handleUserMenu">
            <button class="user-menu-trigger" type="button">
              <el-icon><User /></el-icon>
              <span>{{ authStore.user?.realName || authStore.user?.username }}</span>
              <el-icon class="user-menu-arrow"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon>
                  修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="app-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="passwordDialogVisible" title="修改密码" width="420px" destroy-on-close>
    <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="passwordSubmitting" @click="submitChangePassword">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Aim,
  ArrowDown,
  CreditCard,
  DataBoard,
  Document,
  Files,
  Key,
  List,
  Lock,
  Memo,
  MessageBox,
  Notebook,
  Operation,
  Reading,
  Refresh,
  Search,
  Setting,
  Share,
  SwitchButton,
  Tickets,
  User,
  Warning
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import logoUrl from '@/assets/brand/logo.png'
import { changePasswordApi } from '@/api/auth'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const currentTitle = computed(() => route.meta.title ?? '首页')
const showApiDocsMenu = computed(() => authStore.canViewDashboard)
const showSystemMenu = computed(() => authStore.canViewUsers || authStore.canViewRoles || authStore.canViewSystemSettings)
const displayLogoUrl = computed(() => appStore.logoUrl || logoUrl)
const showMerchantMenu = computed(
  () => authStore.canViewMerchantPools || authStore.canViewMerchantAccounts || authStore.canViewPayMethods
)
const showRouteMenu = computed(() => authStore.canViewRouteRules || authStore.canViewRouteRecords || authStore.canTestRoute)
const showOrderMenu = computed(() => authStore.canViewOrders || authStore.canViewRefunds || authStore.canViewOrderLogs)
const passwordDialogVisible = ref(false)
const passwordSubmitting = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度为 6-64 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function handleUserMenu(command: string) {
  if (command === 'password') {
    openPasswordDialog()
  } else if (command === 'logout') {
    handleLogout()
  }
}

function openPasswordDialog() {
  Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  passwordDialogVisible.value = true
}

async function submitChangePassword() {
  await passwordFormRef.value?.validate()
  passwordSubmitting.value = true
  try {
    await changePasswordApi({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码已修改，请重新登录')
    passwordDialogVisible.value = false
    authStore.clearSession()
    router.push('/login')
  } finally {
    passwordSubmitting.value = false
  }
}

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>
