import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '登录', public: true }
    },
    {
      path: '/docs',
      redirect: '/docs/barcode-pay'
    },
    {
      path: '/docs/:slug',
      name: 'PublicApiDoc',
      component: () => import('@/views/api-docs/ApiDocView.vue'),
      meta: { title: '接口文档', public: true }
    },
    {
      path: '/',
      component: BasicLayout,
      children: [
        {
          path: '',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '首页', permission: 'dashboard:view' }
        },
        {
          path: 'api-docs/barcode-pay',
          name: 'BarcodePayDoc',
          component: () => import('@/views/api-docs/ApiDocView.vue'),
          meta: { title: '条码支付接口', permission: 'dashboard:view' }
        },
        {
          path: 'api-docs/scan-pay',
          name: 'ScanPayDoc',
          component: () => import('@/views/api-docs/ApiDocView.vue'),
          meta: { title: '扫码支付接口', permission: 'dashboard:view' }
        },
        {
          path: 'api-docs/h5-pay',
          name: 'H5PayDoc',
          component: () => import('@/views/api-docs/ApiDocView.vue'),
          meta: { title: 'H5/链接跳转支付接口', permission: 'dashboard:view' }
        },
        {
          path: 'api-docs/wechat-jsapi-pay',
          name: 'WechatJsapiPayDoc',
          component: () => import('@/views/api-docs/ApiDocView.vue'),
          meta: { title: '微信公众号和小程序支付接口', permission: 'dashboard:view' }
        },
        {
          path: 'api-docs/alipay-jsapi-pay',
          name: 'AlipayJsapiPayDoc',
          component: () => import('@/views/api-docs/ApiDocView.vue'),
          meta: { title: '支付宝生活号和小程序支付接口', permission: 'dashboard:view' }
        },
        {
          path: 'api-docs/query-pay',
          name: 'QueryPayDoc',
          component: () => import('@/views/api-docs/ApiDocView.vue'),
          meta: { title: '查单接口', permission: 'dashboard:view' }
        },
        {
          path: 'api-docs/refund',
          name: 'RefundDoc',
          component: () => import('@/views/api-docs/ApiDocView.vue'),
          meta: { title: '退款接口', permission: 'dashboard:view' }
        },
        {
          path: 'api-docs/refund-query',
          name: 'RefundQueryDoc',
          component: () => import('@/views/api-docs/ApiDocView.vue'),
          meta: { title: '退款查询接口', permission: 'dashboard:view' }
        },
        {
          path: 'api-docs/sign',
          name: 'SignDoc',
          component: () => import('@/views/api-docs/ApiDocView.vue'),
          meta: { title: '签名规则', permission: 'dashboard:view' }
        },
        {
          path: 'api-docs/error-codes',
          name: 'ErrorCodesDoc',
          component: () => import('@/views/api-docs/ApiDocView.vue'),
          meta: { title: '错误码说明', permission: 'dashboard:view' }
        },
        {
          path: 'merchant/pools',
          name: 'MerchantPools',
          component: () => import('@/views/merchant/MerchantPoolsView.vue'),
          meta: { title: '商户管理', permission: 'merchant:pool:view' }
        },
        {
          path: 'merchant/accounts',
          name: 'MerchantAccounts',
          component: () => import('@/views/merchant/MerchantAccountsView.vue'),
          meta: { title: '支付参数配置', permission: 'merchant:account:view' }
        },
        {
          path: 'merchant/accounts/create',
          name: 'MerchantAccountCreate',
          component: () => import('@/views/merchant/MerchantAccountFormView.vue'),
          meta: { title: '新增支付参数', permission: 'merchant:account:manage' }
        },
        {
          path: 'merchant/accounts/:id/edit',
          name: 'MerchantAccountEdit',
          component: () => import('@/views/merchant/MerchantAccountFormView.vue'),
          meta: { title: '编辑支付参数', permission: 'merchant:account:manage' }
        },
        {
          path: 'pay-methods',
          name: 'PayMethods',
          component: () => import('@/views/paymethod/PayMethodsView.vue'),
          meta: { title: '支付方式配置', permission: 'paymethod:view' }
        },
        {
          path: 'route/rules',
          name: 'RouteRules',
          component: () => import('@/views/route/RouteRulesView.vue'),
          meta: { title: '路由规则', permission: 'route:rule:view' }
        },
        {
          path: 'route/records',
          name: 'RouteRecords',
          component: () => import('@/views/route/RouteRecordsView.vue'),
          meta: { title: '路由记录', permission: 'route:record:view' }
        },
        {
          path: 'route/test',
          name: 'RouteTest',
          component: () => import('@/views/route/RouteTestView.vue'),
          meta: { title: '路由测试', permission: 'route:test' }
        },
        {
          path: 'orders',
          name: 'Orders',
          component: () => import('@/views/order/OrdersView.vue'),
          meta: { title: '订单流水', permission: 'order:view' }
        },
        {
          path: 'orders/:id',
          name: 'OrderDetail',
          component: () => import('@/views/order/OrderDetailView.vue'),
          meta: { title: '订单详情', permission: 'order:view' }
        },
        {
          path: 'refunds',
          name: 'RefundOrders',
          component: () => import('@/views/order/RefundOrdersView.vue'),
          meta: { title: '退款流水', permission: 'refund:view' }
        },
        {
          path: 'refunds/:id',
          name: 'RefundOrderDetail',
          component: () => import('@/views/order/RefundOrderDetailView.vue'),
          meta: { title: '退款详情', permission: 'refund:view' }
        },
        {
          path: 'order-logs',
          name: 'OrderLogs',
          component: () => import('@/views/order/OrderLogsView.vue'),
          meta: { title: '请求日志', permission: 'order:log:view' }
        },
        {
          path: 'notify-logs',
          name: 'NotifyLogs',
          component: () => import('@/views/order/NotifyLogsView.vue'),
          meta: { title: '通知日志', permission: 'order:log:view' }
        },
        {
          path: 'system/users',
          name: 'SystemUsers',
          component: () => import('@/views/system/UsersView.vue'),
          meta: { title: '用户管理', permission: 'system:user:view' }
        },
        {
          path: 'system/roles',
          name: 'SystemRoles',
          component: () => import('@/views/system/RolesView.vue'),
          meta: { title: '角色管理', permission: 'system:role:view' }
        }
      ]
    }
  ]
})

router.beforeEach(async to => {
  const authStore = useAuthStore()
  if (to.meta.public) {
    if (to.path === '/login' && authStore.token) {
      return '/'
    }
    return true
  }
  if (!authStore.token) {
    return '/login'
  }
  if (!authStore.user) {
    try {
      await authStore.loadCurrentUser()
    } catch {
      authStore.clearSession()
      return '/login'
    }
  }
  const permission = to.meta.permission as string | undefined
  if (permission && !authStore.permissions.includes(permission)) {
    return '/'
  }
  return true
})

export default router
