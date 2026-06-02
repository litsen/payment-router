import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    name: '支付路由后台'
  })
})
