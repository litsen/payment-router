<template>
  <el-container class="app-shell">
    <el-aside class="app-sidebar" width="220px">
      <div class="brand">
        <img :src="logoUrl" alt="Payment Router" class="brand-logo" />
        <span>支付路由后台</span>
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
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="page-title">{{ currentTitle }}</div>
        <div class="header-actions">
          <span class="current-user">{{ authStore.user?.realName || authStore.user?.username }}</span>
          <el-button size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="app-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Aim,
  CreditCard,
  DataBoard,
  Document,
  Files,
  Key,
  List,
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
import logoUrl from '@/assets/brand/logo.png'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const currentTitle = computed(() => route.meta.title ?? '首页')
const showApiDocsMenu = computed(() => authStore.canViewDashboard)
const showSystemMenu = computed(() => authStore.canViewUsers || authStore.canViewRoles)
const showMerchantMenu = computed(
  () => authStore.canViewMerchantPools || authStore.canViewMerchantAccounts || authStore.canViewPayMethods
)
const showRouteMenu = computed(() => authStore.canViewRouteRules || authStore.canViewRouteRecords || authStore.canTestRoute)
const showOrderMenu = computed(() => authStore.canViewOrders || authStore.canViewRefunds || authStore.canViewOrderLogs)

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>
