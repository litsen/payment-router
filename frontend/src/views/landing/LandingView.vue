<template>
  <main class="landing-page">
    <section class="landing-hero" :style="heroBackgroundStyle">
      <div class="dynamic-flow-bg" aria-hidden="true">
        <span class="flow-line flow-line-a"></span>
        <span class="flow-line flow-line-b"></span>
        <span class="flow-line flow-line-c"></span>
        <span class="flow-dot flow-dot-a"></span>
        <span class="flow-dot flow-dot-b"></span>
        <span class="flow-dot flow-dot-c"></span>
      </div>
      <nav class="landing-nav" aria-label="Landing navigation">
        <RouterLink class="landing-brand" to="/landing">
          <img :src="displayLogoUrl" alt="Payment Router" />
          <span>Payment Router</span>
        </RouterLink>
        <div class="landing-nav-actions">
          <a href="#routes">路由模拟</a>
          <RouterLink to="/docs">接口文档</RouterLink>
          <a href="https://github.com/litsen/payment-router" target="_blank" rel="noopener noreferrer">GitHub</a>
          <RouterLink class="nav-login" to="/login">演示后台登录</RouterLink>
        </div>
      </nav>

      <div class="hero-content">
        <div class="hero-copy">
          <span class="eyebrow">多商户支付路由与限额分流中台</span>
          <h1>支付路由中台，为多商户交易自动分流</h1>
          <p>
            统一承接支付、查单、退款与通知请求，把上游通道差异收束为稳定、可观测的标准网关。
          </p>
          <div class="hero-actions">
            <a class="primary-action" href="#routes">体验路由模拟器</a>
          </div>
        </div>

        <div class="feature-carousel" aria-label="Payment Router feature carousel">
          <div class="carousel-shell">
            <div class="carousel-kicker">
              <span>{{ activeFeature.kicker }}</span>
              <strong>{{ activeFeature.index }}</strong>
            </div>
            <div class="carousel-stage" :key="activeFeature.key">
              <component :is="activeFeature.icon" />
              <h2>{{ activeFeature.title }}</h2>
              <p>{{ activeFeature.description }}</p>
              <div class="carousel-stats">
                <span v-for="stat in activeFeature.stats" :key="stat.label">
                  <strong>{{ stat.value }}</strong>
                  {{ stat.label }}
                </span>
              </div>
            </div>
            <div class="carousel-progress">
              <button
                v-for="feature in featureSlides"
                :key="feature.key"
                :class="{ active: activeFeatureKey === feature.key }"
                type="button"
                @click="activeFeatureKey = feature.key"
              >
                <span>{{ feature.nav }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="landing-section capability-section" id="capabilities">
      <div class="section-heading">
        <span>Core Capabilities</span>
        <h2>从单接口网关升级成可运营的支付路由层</h2>
      </div>
      <div class="capability-grid">
        <article v-for="item in capabilities" :key="item.title" class="capability-card">
          <component :is="item.icon" />
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
        </article>
      </div>
    </section>

    <section class="landing-section segment-section">
      <div class="section-heading">
        <span>Customer Fit</span>
        <h2>适合需要“统一出口 + 可控分流”的支付团队</h2>
      </div>
      <div class="segment-layout">
        <div class="segment-tabs" role="tablist" aria-label="Customer segments">
          <button
            v-for="segment in customerSegments"
            :key="segment.key"
            :class="{ active: activeSegmentKey === segment.key }"
            type="button"
            @click="activeSegmentKey = segment.key"
          >
            <component :is="segment.icon" />
            <span>{{ segment.name }}</span>
          </button>
        </div>
        <article class="segment-detail">
          <span>{{ activeSegment.badge }}</span>
          <h3>{{ activeSegment.title }}</h3>
          <p>{{ activeSegment.description }}</p>
          <ul>
            <li v-for="point in activeSegment.points" :key="point">{{ point }}</li>
          </ul>
        </article>
      </div>
    </section>

    <section class="landing-section simulator-section" id="routes">
      <div class="section-heading">
        <span>Route Simulator</span>
        <h2>调一笔订单，看看系统如何选择通道</h2>
      </div>
      <div class="simulator-layout">
        <form class="simulator-form" @submit.prevent>
          <label>
            商户应用
            <select v-model="simulation.app">
              <option>平台商户 A / MCH202605300001</option>
              <option>游戏充值业务 / MCH202605300018</option>
              <option>本地生活业务 / MCH202605300027</option>
            </select>
          </label>
          <label>
            支付方式
            <select v-model="simulation.method">
              <option value="SCAN_PAY">聚合扫码</option>
              <option value="QRCODE_PAY">扫码支付</option>
              <option value="H5_PAY">H5 支付</option>
              <option value="WECHAT_JSAPI">微信 JSAPI</option>
              <option value="REFUND">退款</option>
            </select>
          </label>
          <label>
            交易金额
            <input v-model.number="simulation.amount" min="1" max="50000" type="range" />
            <strong>{{ simulation.amount.toLocaleString() }} 元</strong>
          </label>
          <label class="toggle-row">
            <input v-model="simulation.primaryAvailable" type="checkbox" />
            主通道可用
          </label>
          <label class="toggle-row">
            <input v-model="simulation.limitReservation" type="checkbox" />
            开启限额强一致预占
          </label>
        </form>

        <div class="simulator-result">
          <div class="result-topline">
            <span>{{ simulationResult.status }}</span>
            <strong>{{ simulationResult.channel }}</strong>
          </div>
          <div class="result-grid">
            <div>
              <span>支付参数</span>
              <strong>{{ simulationResult.account }}</strong>
            </div>
            <div>
              <span>路由策略</span>
              <strong>{{ simulationResult.strategy }}</strong>
            </div>
            <div>
              <span>限额处理</span>
              <strong>{{ simulationResult.limit }}</strong>
            </div>
            <div>
              <span>记录结果</span>
              <strong>{{ simulationResult.record }}</strong>
            </div>
          </div>
          <pre>{{ simulationResult.trace }}</pre>
        </div>
      </div>
    </section>

    <section class="landing-section api-section">
      <div class="section-heading">
        <span>API Experience</span>
        <h2>商户接入保持简单，上游差异交给路由层</h2>
      </div>
      <div class="api-layout">
        <div class="api-list">
          <button
            v-for="api in apis"
            :key="api.path"
            :class="{ active: activeApiPath === api.path }"
            type="button"
            @click="activeApiPath = api.path"
          >
            <span>{{ api.name }}</span>
            <code>{{ api.path }}</code>
          </button>
        </div>
        <div class="api-preview">
          <div class="api-preview-header">
            <span>{{ activeApi.name }}</span>
            <RouterLink to="/docs">查看接口文档</RouterLink>
          </div>
          <pre>{{ activeApi.payload }}</pre>
          <div class="signature-preview">
            <span>签名串预览</span>
            <code>{{ signaturePreview }}</code>
          </div>
        </div>
      </div>
    </section>

    <section class="landing-section ops-section">
      <div class="section-heading">
        <span>Production Ready</span>
        <h2>交付时不只给接口，也给上线和运维路径</h2>
      </div>
      <div class="ops-rail">
        <article v-for="item in opsItems" :key="item.title">
          <component :is="item.icon" />
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
        </article>
      </div>
    </section>

    <section class="landing-cta" :style="ctaBackgroundStyle">
      <div class="dynamic-flow-bg compact-flow" aria-hidden="true">
        <span class="flow-line flow-line-a"></span>
        <span class="flow-line flow-line-b"></span>
        <span class="flow-line flow-line-c"></span>
        <span class="flow-dot flow-dot-a"></span>
        <span class="flow-dot flow-dot-b"></span>
      </div>
      <div class="landing-cta-inner">
        <div>
          <span>Ready For Deployment</span>
          <h2>从单一支付接口，升级为可路由、可观测、可交付的支付中台</h2>
        </div>
        <div class="cta-actions">
          <a class="secondary-action" href="/pay-api-test.html">打开支付测试工具</a>
        </div>
      </div>
    </section>

    <footer class="landing-footer">
      <div class="footer-brand">
        <img :src="displayLogoUrl" alt="Payment Router" />
        <span>Payment Router</span>
      </div>
      <div class="footer-info">
        <span>Copyright © 2026 Payment Router</span>
        <a href="tel:+8613800000000">+86 138 0000 0000</a>
        <a href="mailto:contact@payment-router.example">contact@payment-router.example</a>
      </div>
    </footer>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  Connection,
  DataAnalysis,
  Document,
  Finished,
  Lock,
  Monitor,
  Operation,
  Refresh,
  ScaleToOriginal,
  Switch,
  TrendCharts,
  Wallet
} from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const displayLogoUrl = computed(() => appStore.logoUrl || '/brand/logo.png')
const heroBackgroundStyle = computed(() => ({
  backgroundImage: `linear-gradient(90deg, rgba(2, 9, 30, 0.92) 0%, rgba(4, 14, 42, 0.78) 45%, rgba(4, 14, 42, 0.36) 100%), url(${appStore.loginBackgroundUrl || '/brand/login-bg.png'})`
}))
const ctaBackgroundStyle = computed(() => ({
  backgroundImage: `linear-gradient(135deg, rgba(3, 10, 30, 0.9), rgba(11, 29, 74, 0.78)), url(${appStore.loginBackgroundUrl || '/brand/login-bg.png'})`
}))

const featureSlides = [
  {
    key: 'access',
    nav: '统一接入',
    index: '01',
    kicker: 'Merchant Access',
    title: '一套标准接口承接多商户支付请求',
    description: '下游使用 appId/appSecret 和 SHA256 签名接入，支付、查单、退款、通知统一归口。',
    stats: [
      { value: '10+', label: '网关接口' },
      { value: 'SHA256', label: '签名校验' },
      { value: '白名单', label: '通知地址' }
    ],
    icon: Connection
  },
  {
    key: 'route',
    nav: '路由分流',
    index: '02',
    kicker: 'Smart Routing',
    title: '按限额、时段、熔断状态自动选择通道',
    description: '路由规则结合支付参数优先级、可用时间、限额和通道适配结果，减少人工切换成本。',
    stats: [
      { value: '灰度', label: '限额预占' },
      { value: '熔断', label: '通道保护' },
      { value: '记录', label: '路由留痕' }
    ],
    icon: Switch
  },
  {
    key: 'ops',
    nav: '生产交付',
    index: '03',
    kicker: 'Production Ops',
    title: '从部署、备份到健康检查形成交付闭环',
    description: 'Docker Compose、Nginx、MySQL、Redis 和发布脚本配套，适合项目交付和生产运维。',
    stats: [
      { value: 'Docker', label: '容器部署' },
      { value: '备份', label: '发布前置' },
      { value: 'UP', label: '健康检查' }
    ],
    icon: Finished
  }
]

const activeFeatureKey = ref(featureSlides[0].key)
const activeFeature = computed(() => featureSlides.find(item => item.key === activeFeatureKey.value) || featureSlides[0])

let featureTimer: number | undefined

onMounted(() => {
  featureTimer = window.setInterval(() => {
    const currentIndex = featureSlides.findIndex(item => item.key === activeFeatureKey.value)
    activeFeatureKey.value = featureSlides[(currentIndex + 1) % featureSlides.length].key
  }, 4200)
})

onBeforeUnmount(() => {
  if (featureTimer) {
    window.clearInterval(featureTimer)
  }
})

const capabilities = [
  { title: '多商户统一接入', description: '商户应用、密钥、通知白名单和频控策略统一管理，降低下游接入成本。', icon: Connection },
  { title: '多支付方式覆盖', description: '条码、扫码、聚合扫码、H5、微信 JSAPI、支付宝 JSAPI 和退款链路统一出口。', icon: Wallet },
  { title: '智能路由分流', description: '按支付参数优先级、限额、可用时间、熔断状态选择最合适的通道。', icon: Switch },
  { title: '限额强一致预占', description: '灰度开启数据库预占，成功确认、失败释放，减少并发额度风险。', icon: ScaleToOriginal },
  { title: '可观测接口链路', description: '路由记录、请求日志、通知日志、错误码体系和结构化兼容路径日志。', icon: DataAnalysis },
  { title: '生产发布闭环', description: 'Docker Compose、Nginx、数据库备份、健康检查和发布脚本形成交付路径。', icon: Finished }
]

const customerSegments = [
  {
    key: 'aggregator',
    name: '聚合支付服务商',
    badge: 'Payment Service Provider',
    title: '用一个标准网关管理多个商户和上游通道',
    description: '适合需要把不同支付通道包装成统一能力，再向下游商户开放的服务商。',
    points: ['统一商户接入与签名规则', '按额度、时间、熔断状态分流', '保留完整交易和通知日志'],
    icon: Operation
  },
  {
    key: 'platform',
    name: '平台型商户',
    badge: 'Platform Business',
    title: '给不同业务线分配可控的支付出口',
    description: '适合电商、SaaS、本地生活、游戏充值等拥有多业务线和多结算主体的平台。',
    points: ['商户池与应用隔离', '支付方式和参数可独立配置', '订单、退款、请求链路集中查看'],
    icon: Monitor
  },
  {
    key: 'channel',
    name: '通道代理商',
    badge: 'Channel Operator',
    title: '把复杂上游接口包装成更容易卖的标准产品',
    description: '适合拥有上游通道资源、但需要更强下游联调与运维能力的团队。',
    points: ['真实通道适配已落地', 'Mock 通道便于演示和测试', '接口文档与测试工具降低联调成本'],
    icon: TrendCharts
  },
  {
    key: 'migration',
    name: '旧系统迁移客户',
    badge: 'Migration',
    title: '兼容旧签名路径，逐步迁移到新接入模式',
    description: '适合已有历史商户接入，又希望把新商户切换到 appId/appSecret 模式的客户。',
    points: ['旧 poolCode + signKey 兼容', 'warn-only 和 allow/deny 灰度', '通过指标观察迁移进度'],
    icon: Refresh
  }
]

const activeSegmentKey = ref('aggregator')
const activeSegment = computed(() => customerSegments.find(item => item.key === activeSegmentKey.value) || customerSegments[0])

const simulation = reactive({
  app: '平台商户 A / MCH202605300001',
  method: 'SCAN_PAY',
  amount: 1880,
  primaryAvailable: true,
  limitReservation: true
})

const simulationResult = computed(() => {
  const fallback = !simulation.primaryAvailable || simulation.amount > 20000
  const channel = fallback ? '备用支付参数 / Mock-B' : '主支付参数 / Channel-A'
  const status = fallback ? '已切换备用路径' : '主路径命中'
  const limit = simulation.limitReservation ? '预占成功，等待支付结果确认' : '仅记录限额流水'
  return {
    status,
    channel,
    account: fallback ? 'account_api_key_bak_02' : 'account_api_key_main_01',
    strategy: fallback ? '主通道不可用或金额触发限额，降级备用参数' : '优先级最高且处于可用窗口',
    limit,
    record: `RR-${simulation.method}-${simulation.amount}`,
    trace: [
      `merchant=${simulation.app}`,
      `method=${simulation.method}`,
      `amount=${simulation.amount}`,
      `route=${channel}`,
      `limit=${limit}`
    ].join('\n')
  }
})

const apis = [
  {
    name: '扫码支付',
    path: '/api/pay/qrcode',
    payload: '{\n  "appId": "MCH202605300001",\n  "merchantOrderNo": "Q202606050001",\n  "amount": "1880.00",\n  "service": "pay.wxpay.qrcode",\n  "notifyUrl": "https://merchant.example.com/pay/notify"\n}'
  },
  {
    name: 'H5 支付',
    path: '/api/pay/h5',
    payload: '{\n  "appId": "MCH202605300001",\n  "merchantOrderNo": "H202606050001",\n  "amount": "299.00",\n  "returnUrl": "https://merchant.example.com/return"\n}'
  },
  {
    name: '订单查询',
    path: '/api/pay/query',
    payload: '{\n  "appId": "MCH202605300001",\n  "merchantOrderNo": "Q202606050001",\n  "timestamp": 1790000000000,\n  "nonce": "query_nonce_001"\n}'
  },
  {
    name: '退款',
    path: '/api/pay/refund',
    payload: '{\n  "appId": "MCH202605300001",\n  "merchantOrderNo": "Q202606050001",\n  "merchantRefundNo": "R202606050001",\n  "refundAmount": "1880.00"\n}'
  }
]

const activeApiPath = ref('/api/pay/qrcode')
const activeApi = computed(() => apis.find(api => api.path === activeApiPath.value) || apis[0])
const signaturePreview = computed(() => `amount=${simulation.amount}.00&appId=MCH202605300001&nonce=route_demo_nonce&path=${activeApi.value.path}{appSecret}`)

const opsItems = [
  { title: '发布脚本', description: '拉取代码、切换 tag、重建容器、健康检查和失败回滚提示一条链路完成。', icon: Operation },
  { title: '数据库备份', description: '发布前自动备份到 deploy/backups，避免生产变更缺少恢复点。', icon: Document },
  { title: '健康检查', description: '后端、MySQL、Redis 状态统一暴露，方便发布和监控系统确认可用性。', icon: Monitor },
  { title: '安全基线', description: 'JWT、验证码、账号/IP 锁定、日志脱敏和生产敏感配置检查。', icon: Lock }
]
</script>

<style scoped>
.landing-page {
  min-width: 320px;
  min-height: 100vh;
  overflow-x: hidden;
  color: #172033;
  background: #f6f8fc;
}

.landing-hero {
  position: relative;
  overflow: hidden;
  min-height: 84vh;
  padding: 24px clamp(20px, 4vw, 64px) 36px;
  color: #ffffff;
  background-color: #06163b;
  background-position: center;
  background-size: cover;
}

.landing-hero > *:not(.dynamic-flow-bg),
.landing-cta > *:not(.dynamic-flow-bg) {
  position: relative;
  z-index: 1;
}

.dynamic-flow-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

.flow-line {
  position: absolute;
  left: 48%;
  width: 58vw;
  height: 88px;
  border-radius: 999px;
  opacity: 0.74;
  filter: blur(0.2px) drop-shadow(0 0 20px rgba(34, 211, 238, 0.28));
  transform-origin: left center;
}

.flow-line::before,
.flow-line::after {
  position: absolute;
  inset: 0;
  content: "";
  border-radius: inherit;
}

.flow-line::before {
  background: linear-gradient(90deg, transparent 0%, rgba(30, 144, 255, 0.08) 28%, rgba(33, 222, 255, 0.62) 68%, rgba(157, 80, 255, 0.86) 100%);
  clip-path: polygon(0 42%, 82% 42%, 82% 18%, 100% 50%, 82% 82%, 82% 58%, 0 58%);
}

.flow-line::after {
  inset: 17px 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.22), transparent);
  clip-path: polygon(6% 47%, 84% 47%, 84% 40%, 94% 50%, 84% 60%, 84% 53%, 6% 53%);
}

.flow-line-a {
  top: 30%;
  animation: flow-drift-a 13s ease-in-out infinite;
  transform: rotate(-8deg);
}

.flow-line-b {
  top: 45%;
  opacity: 0.64;
  animation: flow-drift-b 15s ease-in-out infinite;
  transform: rotate(2deg);
}

.flow-line-b::before {
  background: linear-gradient(90deg, transparent 0%, rgba(33, 222, 255, 0.08) 24%, rgba(19, 222, 255, 0.76) 78%, rgba(25, 112, 255, 0.92) 100%);
}

.flow-line-c {
  top: 60%;
  opacity: 0.58;
  animation: flow-drift-c 16s ease-in-out infinite;
  transform: rotate(10deg);
}

.flow-line-c::before {
  background: linear-gradient(90deg, transparent 0%, rgba(21, 122, 255, 0.08) 28%, rgba(29, 219, 255, 0.72) 72%, rgba(16, 230, 238, 0.95) 100%);
}

.flow-dot {
  position: absolute;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #24e7ff;
  box-shadow: 0 0 20px rgba(36, 231, 255, 0.82);
}

.flow-dot-a {
  top: 38%;
  left: 67%;
  animation: pulse-dot 2.6s ease-in-out infinite;
}

.flow-dot-b {
  top: 54%;
  left: 78%;
  animation: pulse-dot 3.2s ease-in-out infinite 0.4s;
}

.flow-dot-c {
  top: 67%;
  left: 70%;
  animation: pulse-dot 3s ease-in-out infinite 0.9s;
}

.compact-flow {
  opacity: 0.82;
}

.compact-flow .flow-line {
  left: 36%;
  width: 70vw;
}

.landing-nav,
.hero-content,
.landing-section,
.landing-cta-inner,
.landing-footer {
  width: min(1180px, 100%);
  margin: 0 auto;
}

.landing-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  height: 54px;
}

.landing-brand,
.landing-nav-actions,
.hero-actions,
.cta-actions {
  display: flex;
  align-items: center;
}

.landing-brand {
  gap: 10px;
  color: #ffffff;
  font-weight: 800;
  text-decoration: none;
}

.landing-brand img {
  width: 36px;
  height: 36px;
  object-fit: contain;
}

.landing-nav-actions {
  gap: 10px;
}

.landing-nav-actions a,
.nav-login,
.secondary-action,
.primary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 6px;
  color: inherit;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}

.landing-nav-actions a {
  color: rgba(255, 255, 255, 0.78);
}

.landing-nav-actions a:hover {
  color: #ffffff;
}

.landing-nav-actions .nav-login {
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.36);
  background: rgba(255, 255, 255, 0.08);
}

.hero-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(380px, 500px);
  gap: clamp(32px, 6vw, 80px);
  align-items: center;
  min-height: calc(84vh - 80px);
  padding-top: 30px;
}

.hero-copy {
  max-width: 660px;
}

.eyebrow,
.section-heading span,
.landing-cta span {
  display: inline-flex;
  margin-bottom: 14px;
  color: #19d3ff;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: 0;
}

.hero-copy p {
  max-width: 560px;
  margin: 18px 0 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 15px;
  line-height: 1.75;
}

.hero-actions {
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 34px;
}

.primary-action {
  color: #06163b;
  background: linear-gradient(135deg, #22e3ff, #8e5dff);
  box-shadow: 0 16px 34px rgba(0, 178, 255, 0.24);
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.secondary-action {
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.08);
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}

.primary-action:hover,
.secondary-action:hover {
  transform: translateY(-2px);
}

.primary-action:hover {
  box-shadow: 0 20px 42px rgba(0, 178, 255, 0.34);
}

.secondary-action:hover {
  border-color: rgba(255, 255, 255, 0.48);
  background: rgba(255, 255, 255, 0.14);
}

.hero-note {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  margin-top: 22px;
  padding: 0 12px;
  color: rgba(255, 255, 255, 0.74);
  font-size: 13px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.06);
}

.feature-carousel,
.segment-detail,
.simulator-result,
.api-preview {
  border: 1px solid rgba(205, 221, 255, 0.18);
  border-radius: 8px;
  background: rgba(7, 18, 49, 0.76);
  box-shadow: 0 26px 80px rgba(0, 9, 34, 0.36);
  backdrop-filter: blur(14px);
}

.feature-carousel {
  position: relative;
  overflow: hidden;
  padding: 18px;
  animation: float-card 7s ease-in-out infinite;
}

.feature-carousel::before {
  position: absolute;
  inset: -80px auto auto -80px;
  width: 220px;
  height: 220px;
  content: "";
  background: radial-gradient(circle, rgba(34, 227, 255, 0.28), transparent 68%);
  pointer-events: none;
}

.carousel-shell {
  position: relative;
  display: grid;
  gap: 18px;
}

.carousel-kicker,
.result-topline,
.api-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.carousel-kicker span,
.result-grid span,
.signature-preview span {
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
  font-weight: 700;
}

.carousel-kicker strong {
  color: #ffffff;
  font-size: 28px;
}

.carousel-stage {
  min-height: 330px;
  padding: 22px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.04)),
    rgba(0, 0, 0, 0.14);
  animation: slide-in 420ms ease both;
}

.carousel-stage svg {
  width: 48px;
  height: 48px;
  color: #22e3ff;
  filter: drop-shadow(0 0 18px rgba(34, 227, 255, 0.34));
}

.carousel-stage h2 {
  margin: 28px 0 12px;
  color: #ffffff;
  font-size: 28px;
  line-height: 1.2;
}

.carousel-stage p {
  margin: 0;
  color: rgba(255, 255, 255, 0.74);
  line-height: 1.75;
}

.carousel-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 28px;
}

.carousel-stats span {
  display: grid;
  gap: 5px;
  min-height: 78px;
  padding: 12px;
  color: rgba(255, 255, 255, 0.62);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
}

.carousel-stats strong {
  color: #ffffff;
  font-size: 18px;
}

.carousel-progress {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.carousel-progress button,
.segment-tabs button,
.api-list button {
  min-width: 0;
  font: inherit;
  border: 0;
  border-radius: 6px;
  cursor: pointer;
}

.carousel-progress button {
  position: relative;
  min-height: 42px;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.08);
}

.carousel-progress button::before {
  position: absolute;
  inset: auto 0 0;
  height: 2px;
  content: "";
  background: #22e3ff;
  transform: scaleX(0);
  transform-origin: left;
}

.carousel-progress button.active {
  color: #04132f;
  background: #22e3ff;
}

.carousel-progress button.active::before {
  animation: progress-line 4200ms linear;
}

.flow-grid {
  display: grid;
  gap: 12px;
}

.flow-node {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 2px 12px;
  align-items: center;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
}

.flow-node svg {
  grid-row: span 2;
  width: 26px;
  height: 26px;
}

.flow-node span {
  color: #ffffff;
  font-weight: 800;
}

.flow-node small {
  color: rgba(255, 255, 255, 0.62);
}

.flow-node.cool svg {
  color: #20d7ff;
}

.flow-node.secure svg {
  color: #8e5dff;
}

.flow-node.warm svg {
  color: #ffd166;
}

.flow-node.active svg {
  color: #37f7a6;
}

.channel-result {
  margin-top: 16px;
  padding: 14px;
  border-radius: 8px;
  background: rgba(28, 227, 255, 0.1);
}

.landing-section {
  padding: 82px 24px 0;
}

.section-heading {
  max-width: 980px;
  margin-bottom: 28px;
}

.section-heading h2,
.landing-cta h2 {
  margin: 0;
  color: #172033;
  font-size: clamp(28px, 3.2vw, 38px);
  line-height: 1.22;
  letter-spacing: 0;
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.capability-card,
.ops-rail article {
  min-height: 210px;
  padding: 22px;
  border: 1px solid #dfe6f3;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 16px 34px rgba(37, 48, 77, 0.07);
  transition: transform 220ms ease, box-shadow 220ms ease, border-color 220ms ease;
}

.capability-card:hover,
.ops-rail article:hover {
  border-color: #bfd8ff;
  box-shadow: 0 22px 46px rgba(37, 48, 77, 0.12);
  transform: translateY(-4px);
}

.capability-card svg,
.ops-rail svg {
  width: 34px;
  height: 34px;
  color: #246bfe;
}

.capability-card h3,
.segment-detail h3,
.ops-rail h3 {
  margin: 18px 0 10px;
  color: #172033;
  font-size: 20px;
  line-height: 1.25;
}

.capability-card p,
.segment-detail p,
.ops-rail p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.segment-layout,
.api-layout {
  display: grid;
  grid-template-columns: 310px minmax(0, 1fr);
  gap: 18px;
}

.segment-tabs,
.api-list {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.segment-tabs button,
.api-list button {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 62px;
  padding: 0 16px;
  color: #475569;
  text-align: left;
  background: #ffffff;
  border: 1px solid #dfe6f3;
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}

.segment-tabs button:hover,
.api-list button:hover {
  transform: translateX(4px);
  border-color: #b7d7ff;
}

.segment-tabs button.active,
.api-list button.active {
  color: #0f2b58;
  border-color: #94c9ff;
  background: #eef7ff;
}

.segment-tabs svg {
  width: 22px;
  height: 22px;
  color: #246bfe;
}

.segment-detail {
  min-height: 278px;
  padding: 30px;
  color: #ffffff;
  background:
    linear-gradient(135deg, rgba(10, 31, 80, 0.95), rgba(20, 34, 74, 0.92)),
    #10234c;
}

.segment-detail span {
  color: #22e3ff;
  font-size: 13px;
  font-weight: 800;
}

.segment-detail h3 {
  color: #ffffff;
}

.segment-detail p,
.segment-detail li {
  color: rgba(255, 255, 255, 0.76);
}

.segment-detail ul {
  display: grid;
  gap: 10px;
  margin: 22px 0 0;
  padding: 0;
  list-style: none;
}

.segment-detail li {
  padding-left: 16px;
  border-left: 3px solid #22e3ff;
}

.simulator-layout {
  display: grid;
  grid-template-columns: minmax(280px, 390px) minmax(0, 1fr);
  gap: 18px;
}

.simulator-form {
  display: grid;
  gap: 16px;
  padding: 22px;
  border: 1px solid #dfe6f3;
  border-radius: 8px;
  background: #ffffff;
}

.simulator-form label {
  display: grid;
  gap: 8px;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.simulator-form select,
.simulator-form input[type="range"] {
  width: 100%;
}

.simulator-form select {
  height: 42px;
  padding: 0 10px;
  color: #172033;
  border: 1px solid #ccd6e5;
  border-radius: 6px;
  background: #ffffff;
}

.simulator-form input[type="range"] {
  accent-color: #246bfe;
}

.simulator-form strong {
  color: #246bfe;
}

.toggle-row {
  display: flex !important;
  grid-template-columns: none !important;
  align-items: center;
  gap: 10px !important;
}

.toggle-row input {
  width: 18px;
  height: 18px;
  accent-color: #246bfe;
}

.simulator-result {
  padding: 24px;
  color: #ffffff;
  background:
    linear-gradient(135deg, rgba(8, 24, 64, 0.96), rgba(22, 37, 78, 0.96)),
    #10234c;
}

.result-topline {
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.result-topline span {
  color: #37f7a6;
  font-weight: 800;
}

.result-topline strong {
  font-size: 24px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.result-grid div {
  min-height: 88px;
  padding: 14px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.07);
}

.result-grid strong {
  display: block;
  margin-top: 8px;
  color: #ffffff;
  line-height: 1.5;
}

.simulator-result pre,
.api-preview pre {
  margin: 18px 0 0;
  padding: 16px;
  overflow: auto;
  color: #d7e5ff;
  line-height: 1.65;
  background: rgba(0, 0, 0, 0.26);
  border-radius: 8px;
}

.api-list button {
  align-items: flex-start;
  flex-direction: column;
  justify-content: center;
}

.api-list code {
  color: #64748b;
  font-size: 12px;
}

.api-preview {
  min-width: 0;
  padding: 24px;
  color: #ffffff;
}

.api-preview-header span {
  color: #ffffff;
  font-size: 20px;
  font-weight: 800;
}

.api-preview-header a {
  color: #22e3ff;
  font-weight: 800;
  text-decoration: none;
}

.signature-preview {
  display: grid;
  gap: 8px;
  margin-top: 18px;
}

.signature-preview code {
  display: block;
  min-width: 0;
  padding: 12px;
  overflow-wrap: anywhere;
  color: #37f7a6;
  background: rgba(55, 247, 166, 0.08);
  border-radius: 8px;
}

.ops-rail {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.landing-cta {
  position: relative;
  display: grid;
  min-height: 430px;
  margin-top: 96px;
  overflow: hidden;
  padding: 64px 24px;
  place-items: center;
  color: #ffffff;
  background-color: #06163b;
  background-position: center;
  background-size: cover;
}

.landing-cta-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  padding: 40px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(7, 18, 49, 0.84), rgba(21, 48, 105, 0.68)),
    rgba(5, 14, 38, 0.58);
  box-shadow: 0 26px 80px rgba(0, 9, 34, 0.36);
  backdrop-filter: blur(12px);
}

.landing-cta h2 {
  max-width: 760px;
  color: #ffffff;
}

.cta-actions {
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

.landing-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 26px 24px 34px;
  color: #526071;
}

.footer-brand,
.footer-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.footer-brand img {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.footer-brand span {
  color: #172033;
  font-weight: 800;
}

.footer-info {
  justify-content: flex-end;
  font-size: 13px;
}

.footer-info a {
  color: #526071;
  text-decoration: none;
}

.footer-info a:hover {
  color: #246bfe;
}

@keyframes slide-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float-card {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-8px);
  }
}

@keyframes progress-line {
  from {
    transform: scaleX(0);
  }

  to {
    transform: scaleX(1);
  }
}

@keyframes flow-drift-a {
  0%,
  100% {
    transform: translate3d(-10px, 0, 0) rotate(-8deg);
  }

  50% {
    transform: translate3d(26px, -14px, 0) rotate(-5deg);
  }
}

@keyframes flow-drift-b {
  0%,
  100% {
    transform: translate3d(8px, 0, 0) rotate(2deg);
  }

  50% {
    transform: translate3d(-24px, 16px, 0) rotate(4deg);
  }
}

@keyframes flow-drift-c {
  0%,
  100% {
    transform: translate3d(-4px, 0, 0) rotate(10deg);
  }

  50% {
    transform: translate3d(22px, 12px, 0) rotate(7deg);
  }
}

@keyframes pulse-dot {
  0%,
  100% {
    opacity: 0.42;
    transform: scale(0.8);
  }

  50% {
    opacity: 1;
    transform: scale(1.35);
  }
}

@media (max-width: 980px) {
  .hero-content,
  .segment-layout,
  .simulator-layout,
  .api-layout {
    grid-template-columns: 1fr;
  }

  .hero-content {
    min-height: auto;
  }

  .capability-grid,
  .ops-rail {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .landing-cta-inner {
    align-items: flex-start;
    flex-direction: column;
  }

  .cta-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 680px) {
  .landing-hero {
    min-height: auto;
    padding: 18px 16px 44px;
  }

  .landing-nav {
    align-items: flex-start;
    height: auto;
    flex-direction: column;
  }

  .landing-nav-actions {
    width: 100%;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .hero-content {
    padding-top: 46px;
  }

  .feature-carousel {
    display: none;
  }

  .hero-copy h1 {
    font-size: 34px;
  }

  .carousel-stats,
  .result-grid,
  .capability-grid,
  .ops-rail {
    grid-template-columns: 1fr;
  }

  .api-list button,
  .api-preview pre {
    max-width: 100%;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }

  .landing-section {
    padding: 58px 16px 0;
  }

  .landing-cta {
    min-height: 360px;
    margin-top: 64px;
    padding: 44px 16px;
  }

  .landing-cta-inner {
    padding: 26px;
  }

  .flow-line {
    left: 24%;
    width: 96vw;
    height: 62px;
    opacity: 0.5;
  }

  .flow-dot {
    opacity: 0.6;
  }

  .landing-footer {
    align-items: flex-start;
    flex-direction: column;
    padding: 24px 16px 32px;
  }

  .footer-info {
    justify-content: flex-start;
  }
}
</style>
