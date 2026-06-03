import { defineStore } from 'pinia'
import { loginApi, logoutApi, meApi, type CurrentUser, type ApiResult, type LoginResponse } from '@/api/auth'

const TOKEN_KEY = 'payment-router-token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null as CurrentUser | null
  }),
  getters: {
    isLoggedIn: state => Boolean(state.token),
    roles: state => state.user?.roles ?? [],
    permissions: state => state.user?.permissions ?? [],
    canViewDashboard: state => Boolean(state.user?.permissions.includes('dashboard:view')),
    canViewUsers: state => Boolean(state.user?.permissions.includes('system:user:view')),
    canManageUsers: state => Boolean(state.user?.permissions.includes('system:user:manage')),
    canViewRoles: state => Boolean(state.user?.permissions.includes('system:role:view')),
    canManageRoles: state => Boolean(state.user?.permissions.includes('system:role:manage')),
    canViewSystemSettings: state => Boolean(state.user?.permissions.includes('system:settings:view')),
    canManageSystemSettings: state => Boolean(state.user?.permissions.includes('system:settings:manage')),
    canViewMerchantPools: state => Boolean(state.user?.permissions.includes('merchant:pool:view')),
    canManageMerchantPools: state => Boolean(state.user?.permissions.includes('merchant:pool:manage')),
    canViewMerchantAccounts: state => Boolean(state.user?.permissions.includes('merchant:account:view')),
    canManageMerchantAccounts: state => Boolean(state.user?.permissions.includes('merchant:account:manage')),
    canViewMerchantApps: state => Boolean(state.user?.permissions.includes('merchant:app:view')),
    canManageMerchantApps: state => Boolean(state.user?.permissions.includes('merchant:app:manage')),
    canViewPayMethods: state => Boolean(state.user?.permissions.includes('paymethod:view')),
    canManagePayMethods: state => Boolean(state.user?.permissions.includes('paymethod:manage')),
    canViewRouteRules: state => Boolean(state.user?.permissions.includes('route:rule:view')),
    canManageRouteRules: state => Boolean(state.user?.permissions.includes('route:rule:manage')),
    canViewRouteRecords: state => Boolean(state.user?.permissions.includes('route:record:view')),
    canTestRoute: state => Boolean(state.user?.permissions.includes('route:test')),
    canViewOrders: state => Boolean(state.user?.permissions.includes('order:view')),
    canManageOrders: state => Boolean(state.user?.permissions.includes('order:manage')),
    canViewOrderLogs: state => Boolean(state.user?.permissions.includes('order:log:view')),
    canViewRefunds: state => Boolean(state.user?.permissions.includes('refund:view')),
    canManageRefunds: state => Boolean(state.user?.permissions.includes('refund:manage'))
  },
  actions: {
    async login(payload: { username: string; password: string; captchaId?: string; captchaCode?: string }) {
      const response = await loginApi(payload) as unknown as ApiResult<LoginResponse>
      this.token = response.data.token
      this.user = response.data.user
      localStorage.setItem(TOKEN_KEY, this.token)
    },
    async loadCurrentUser() {
      if (!this.token) {
        return
      }
      const response = await meApi() as unknown as ApiResult<CurrentUser>
      this.user = response.data
    },
    clearSession() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
    },
    async logout() {
      if (this.token) {
        await logoutApi().catch(() => undefined)
      }
      this.clearSession()
    }
  }
})
