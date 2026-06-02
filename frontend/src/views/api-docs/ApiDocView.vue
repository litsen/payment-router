<template>
  <section :class="isPublicDoc ? 'public-doc-shell' : 'api-doc-page'">
    <aside v-if="isPublicDoc" class="public-doc-sidebar">
      <div class="brand">支付路由接口文档</div>
      <el-menu router :default-active="route.path" class="doc-menu">
        <el-menu-item v-for="item in backendDocSummaries || []" :key="item.slug" :index="`/docs/${item.slug}`">
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <main class="doc-content">
      <h1 v-loading="loading">{{ doc.title }}</h1>
      <p>{{ doc.description }}</p>
      <div v-if="doc.requestPath" class="request-address">
        <el-tag size="large" type="success">{{ doc.requestMethod || 'POST' }}</el-tag>
        <code>{{ doc.requestPath }}</code>
      </div>

      <el-tabs v-model="activeTab">
      <el-tab-pane v-if="doc.requestParams?.length" label="请求参数" name="requestParams">
        <el-table :data="doc.requestParams" border>
          <el-table-column prop="name" label="参数名" min-width="170" />
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column prop="required" label="是否必填" width="100" />
          <el-table-column prop="description" label="描述" min-width="260" />
          <el-table-column prop="enums" label="枚举值" min-width="180" />
          <el-table-column prop="example" label="示例" min-width="180" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane v-if="doc.responseParams?.length" label="响应参数" name="responseParams">
        <el-table :data="doc.responseParams" border>
          <el-table-column prop="name" label="参数名" min-width="170" />
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column prop="required" label="是否必填" width="100" />
          <el-table-column prop="description" label="描述" min-width="260" />
          <el-table-column prop="enums" label="枚举值" min-width="180" />
          <el-table-column prop="example" label="示例" min-width="180" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane v-if="doc.request" label="请求示例" name="request">
        <pre class="json-preview">{{ doc.request }}</pre>
      </el-tab-pane>
      <el-tab-pane v-if="doc.response" label="响应示例" name="response">
        <pre class="json-preview">{{ doc.response }}</pre>
      </el-tab-pane>
      <el-tab-pane v-if="doc.rules" label="规则说明" name="rules">
        <div class="doc-section" v-html="doc.rules"></div>
      </el-tab-pane>
      <el-tab-pane v-if="doc.errors" label="错误码说明" name="errors">
        <el-table :data="doc.errors" border>
          <el-table-column prop="code" label="错误码" min-width="220" />
          <el-table-column prop="message" label="说明" min-width="220" />
        </el-table>
      </el-tab-pane>
      </el-tabs>
    </main>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getApiDoc, listApiDocs, type ApiDoc, type ApiDocSummary } from '@/api/apiDocs'
import type { ApiResult } from '@/api/auth'

const route = useRoute()
const activeTab = ref('request')
const loading = ref(false)
const backendDoc = ref<ApiDoc | null>(null)
const backendDocSummaries = ref<ApiDocSummary[] | null>(null)
const isPublicDoc = computed(() => route.path.startsWith('/docs'))

const fallbackDoc: ApiDoc = {
  title: '接口文档暂不可用',
  description: '未能从后端加载当前接口文档，请稍后重试或联系管理员检查 ApiDocService 配置。',
  rules: '<p>接口文档页面现在优先使用后端 /admin/api-docs 数据，仅保留该兜底提示。</p>'
}

const doc = computed(() => backendDoc.value ?? fallbackDoc)

watch(
  () => route.path,
  async () => {
    await loadBackendDoc()
    activeTab.value = firstAvailableTab(doc.value)
  },
  { immediate: true }
)

async function loadBackendDoc() {
  backendDoc.value = null
  const slug = route.path.split('/').pop()
  if (!slug) {
    return
  }
  loading.value = true
  try {
    if (!backendDocSummaries.value) {
      const listResponse = await listApiDocs(isPublicDoc.value) as unknown as ApiResult<ApiDocSummary[]>
      backendDocSummaries.value = listResponse.data
    }
    if (!backendDocSummaries.value.some(item => item.slug === slug)) {
      return
    }
    const response = await getApiDoc(slug, isPublicDoc.value) as unknown as ApiResult<ApiDoc>
    backendDoc.value = response.data
  } catch {
    backendDocSummaries.value = []
  } finally {
    loading.value = false
  }
}

function firstAvailableTab(currentDoc: ApiDoc) {
  if (currentDoc.request) {
    if (currentDoc.requestParams?.length) {
      return 'requestParams'
    }
    return 'request'
  }
  if (currentDoc.responseParams?.length) {
    return 'responseParams'
  }
  if (currentDoc.response) {
    return 'response'
  }
  if (currentDoc.rules) {
    return 'rules'
  }
  return 'errors'
}

</script>

<style scoped>
.api-doc-page {
  padding: 24px;
}

.doc-content {
  min-width: 0;
}

.api-doc-page h1,
.doc-content h1 {
  margin: 0 0 8px;
}

.api-doc-page p,
.doc-content p {
  color: #666;
}

.json-preview {
  background: #111827;
  color: #d1d5db;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
}

.doc-section {
  line-height: 1.8;
}

.request-address {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 14px 0 18px;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.request-address code {
  color: #111827;
  font-size: 14px;
  word-break: break-all;
}

.public-doc-shell {
  display: flex;
  min-height: 100vh;
  background: #f8fafc;
}

.public-doc-sidebar {
  width: 260px;
  flex: 0 0 260px;
  min-height: 100vh;
  background: #fff;
  border-right: 1px solid #e5e7eb;
}

.public-doc-shell .doc-content {
  flex: 1;
  padding: 28px 32px;
}

.brand {
  padding: 20px 18px 14px;
  font-size: 17px;
  font-weight: 600;
  color: #111827;
}

.doc-menu {
  border-right: 0;
}

@media (max-width: 768px) {
  .public-doc-shell {
    flex-direction: column;
  }

  .public-doc-sidebar {
    width: 100%;
    flex-basis: auto;
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid #e5e7eb;
  }

  .public-doc-shell .doc-content {
    padding: 20px 16px;
  }
}
</style>
