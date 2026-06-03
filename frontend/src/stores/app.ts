import { defineStore } from 'pinia'
import { publicSystemSettings, type SystemSettings } from '@/api/system'
import type { ApiResult } from '@/api/auth'

const DEFAULT_SETTINGS: SystemSettings = {
  siteName: '支付路由后台',
  copyrightText: 'Copyright © xxx公司',
  logoUrl: '',
  loginBackgroundUrl: '',
  faviconUrl: ''
}

export const useAppStore = defineStore('app', {
  state: () => ({
    settings: { ...DEFAULT_SETTINGS },
    loaded: false
  }),
  getters: {
    name: state => state.settings.siteName || DEFAULT_SETTINGS.siteName,
    copyrightText: state => state.settings.copyrightText || DEFAULT_SETTINGS.copyrightText,
    logoUrl: state => state.settings.logoUrl,
    loginBackgroundUrl: state => state.settings.loginBackgroundUrl,
    faviconUrl: state => state.settings.faviconUrl
  },
  actions: {
    async loadSettings() {
      try {
        const response = await publicSystemSettings() as unknown as ApiResult<SystemSettings>
        this.applySettings(response.data)
      } catch {
        this.applySettings(DEFAULT_SETTINGS)
      } finally {
        this.loaded = true
      }
    },
    applySettings(settings: Partial<SystemSettings>) {
      this.settings = { ...DEFAULT_SETTINGS, ...settings }
      document.title = this.name
      updateFavicon(this.settings.faviconUrl)
    }
  }
})

function updateFavicon(url?: string) {
  if (!url) {
    return
  }
  let link = document.querySelector<HTMLLinkElement>('link[rel="icon"]')
  if (!link) {
    link = document.createElement('link')
    link.rel = 'icon'
    document.head.appendChild(link)
  }
  link.href = url
}
