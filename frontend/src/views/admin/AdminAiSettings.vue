<template>
  <div class="admin-ai">
    <header class="admin-ai__head">
      <h1 class="admin-ai__title">AI 站点配置</h1>
      <p class="admin-ai__sub">站点级模型、用户自带凭证开关、Prompt 模板。</p>
    </header>

    <!-- 站点开关（D2） -->
    <section class="admin-ai__section">
      <h2 class="admin-ai__section-title">站点开关</h2>
      <div class="admin-ai__setting">
        <el-switch v-model="allowUserOwnKey" @change="handleToggleOwnKey" />
        <div>
          <strong>允许用户自带凭证</strong>
          <p class="admin-ai__hint">
            关闭后用户配置不再参与解析链，但数据保留；重新打开即恢复（A7）。
          </p>
        </div>
      </div>
    </section>

    <!-- 站点级模型 -->
    <section class="admin-ai__section">
      <h2 class="admin-ai__section-title">站点级模型（{{ siteModels.length }}）</h2>
      <p v-if="siteModels.length === 0" class="admin-ai__hint">还没有站点级模型。</p>
      <ul class="admin-ai__cards">
        <li v-for="model in siteModels" :key="model.id" class="admin-ai__card">
          <div class="admin-ai__card-head">
            <strong>{{ model.displayName }}</strong>
            <span v-if="model.isDefault" class="admin-ai__tag">站点默认</span>
            <span v-if="!model.isEnabled" class="admin-ai__tag admin-ai__tag--muted">已停用</span>
          </div>
          <p class="admin-ai__card-meta">{{ model.modelId }} · {{ model.baseUrl }}</p>
          <p class="admin-ai__card-meta">凭证：{{ model.hasCredential ? model.apiKeyHint : '未填写' }}</p>
          <div class="admin-ai__card-actions">
            <el-button size="small" text :disabled="!model.isEnabled" @click="handleSetDefault(model)">
              设为站点默认
            </el-button>
            <el-button size="small" text type="danger" @click="handleDeleteSite(model)">删除</el-button>
          </div>
        </li>
      </ul>

      <div class="admin-ai__form">
        <h3 class="admin-ai__form-title">新增站点级模型</h3>
        <label class="admin-ai__field">
          <span>展示名 *</span>
          <input v-model="siteForm.displayName" maxlength="32" placeholder="如 站点默认 DeepSeek" />
        </label>
        <label class="admin-ai__field">
          <span>Base URL *</span>
          <input v-model="siteForm.baseUrl" placeholder="https://api.deepseek.com/v1" />
        </label>
        <label class="admin-ai__field">
          <span>API Key *</span>
          <input v-model="siteForm.apiKey" type="password" autocomplete="new-password" />
        </label>
        <label class="admin-ai__field">
          <span>模型 ID *</span>
          <input v-model="siteForm.modelId" placeholder="如 deepseek-chat" />
        </label>
        <label class="admin-ai__field admin-ai__field--inline">
          <input v-model="siteForm.makeDefault" type="checkbox" />
          <span>设为站点默认</span>
        </label>
        <p v-if="siteError" class="admin-ai__error">{{ siteError }}</p>
        <div class="admin-ai__form-actions">
          <el-button type="primary" :loading="saving" @click="handleCreateSite">保存</el-button>
          <el-button :loading="testing" @click="handleTestSite">测试连接</el-button>
          <span v-if="testText" class="admin-ai__hint">{{ testText }}</span>
        </div>
      </div>
    </section>

    <!-- Prompt 模板（D11） -->
    <section class="admin-ai__section">
      <h2 class="admin-ai__section-title">Prompt 模板（{{ templates.length }}）</h2>
      <p class="admin-ai__hint">
        用户在对话页只能选择模板，不能改正文；客户端传的 system 消息仍会被丢弃。
      </p>
      <ul class="admin-ai__cards">
        <li v-for="tpl in templates" :key="tpl.id" class="admin-ai__card">
          <div class="admin-ai__card-head">
            <strong>{{ tpl.name }}</strong>
            <span v-if="tpl.isBuiltin" class="admin-ai__tag">内置</span>
            <span v-if="!tpl.enabled" class="admin-ai__tag admin-ai__tag--muted">已停用</span>
          </div>
          <p class="admin-ai__card-meta">{{ tpl.description }}</p>
          <div class="admin-ai__card-actions">
            <el-button size="small" text @click="startEditTemplate(tpl)">编辑</el-button>
            <el-button size="small" text type="danger" :disabled="tpl.isBuiltin" @click="handleDeleteTemplate(tpl)">
              删除
            </el-button>
          </div>
        </li>
      </ul>

      <div class="admin-ai__form">
        <h3 class="admin-ai__form-title">{{ editingTemplateId ? '编辑模板' : '新增模板' }}</h3>
        <label class="admin-ai__field">
          <span>模板名 *</span>
          <input v-model="templateForm.name" maxlength="32" />
        </label>
        <label class="admin-ai__field">
          <span>说明</span>
          <input v-model="templateForm.description" maxlength="255" />
        </label>
        <label class="admin-ai__field">
          <span>正文 *</span>
          <textarea v-model="templateForm.content" rows="8"></textarea>
        </label>
        <label class="admin-ai__field admin-ai__field--inline">
          <input v-model="templateForm.enabled" type="checkbox" />
          <span>启用</span>
        </label>
        <p v-if="templateError" class="admin-ai__error">{{ templateError }}</p>
        <div class="admin-ai__form-actions">
          <el-button type="primary" :loading="savingTemplate" @click="handleSaveTemplate">保存</el-button>
          <el-button v-if="editingTemplateId" @click="resetTemplateForm">取消编辑</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message-box.css'
import {
  createPromptTemplate,
  createSiteModel,
  deletePromptTemplate,
  deleteSiteModel,
  getSiteSettings,
  listPromptTemplates,
  listSiteModels,
  setDefaultSiteModel,
  testSiteConnection,
  updatePromptTemplate,
  updateSiteSettings
} from '@/api/ai'
import { FALLBACK_MESSAGES } from '@/utils/request'

const allowUserOwnKey = ref(true)
const siteModels = ref([])
const templates = ref([])
const saving = ref(false)
const testing = ref(false)
const testText = ref('')
const siteError = ref('')

const savingTemplate = ref(false)
const templateError = ref('')
const editingTemplateId = ref(null)

const siteForm = reactive({
  displayName: '',
  baseUrl: '',
  apiKey: '',
  modelId: '',
  makeDefault: false
})

const templateForm = reactive({
  name: '',
  description: '',
  content: '',
  enabled: true
})

async function loadAll() {
  try {
    const settings = await getSiteSettings()
    allowUserOwnKey.value = Boolean(settings.allowUserOwnKey)
  } catch {
    // 忽略
  }
  try {
    siteModels.value = await listSiteModels()
  } catch {
    siteModels.value = []
  }
  try {
    templates.value = await listPromptTemplates()
  } catch {
    templates.value = []
  }
}

async function handleToggleOwnKey(value) {
  try {
    const settings = await updateSiteSettings(value)
    allowUserOwnKey.value = Boolean(settings.allowUserOwnKey)
    ElMessage.success(value ? '已允许用户自带凭证' : '已关闭用户自带凭证（数据保留）')
  } catch (error) {
    allowUserOwnKey.value = !value
    ElMessage.error(FALLBACK_MESSAGES[error.code] || error.message || '操作失败')
  }
}

async function handleCreateSite() {
  if (!siteForm.displayName.trim() || !siteForm.baseUrl.trim() || !siteForm.modelId.trim()) {
    siteError.value = '请填写展示名、Base URL 与模型 ID'
    return
  }
  if (!siteForm.apiKey.trim()) {
    siteError.value = '请填写 API Key'
    return
  }
  saving.value = true
  siteError.value = ''
  try {
    await createSiteModel({
      displayName: siteForm.displayName.trim(),
      baseUrl: siteForm.baseUrl.trim(),
      apiKey: siteForm.apiKey.trim(),
      modelId: siteForm.modelId.trim(),
      makeDefault: siteForm.makeDefault,
      isEnabled: true,
      // v3.24 修正：该字段语义是「是否允许上游 thinking」，false 才会在请求体写入
      // thinking:{type:disabled}（LlmStreamClient）。05 §2.1 要求**必须关闭**，
      // 此前硬编码 true 等于站点配置永远不关 thinking，Spark 会卡首 token 且按输出 token 多计费。
      thinkingEnabled: false
    })
    ElMessage.success('已保存站点级模型')
    siteForm.displayName = ''
    siteForm.baseUrl = ''
    siteForm.apiKey = ''
    siteForm.modelId = ''
    siteForm.makeDefault = false
    siteModels.value = await listSiteModels()
  } catch (error) {
    siteError.value = FALLBACK_MESSAGES[error.code] || error.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function handleTestSite() {
  testing.value = true
  testText.value = ''
  try {
    const result = await testSiteConnection({
      baseUrl: siteForm.baseUrl,
      apiKey: siteForm.apiKey || undefined,
      modelId: siteForm.modelId
    })
    testText.value = result.ok
      ? `连接正常 ${result.latencyMs}ms`
      : result.error || '连接失败'
    if (result.suggestV1 && !result.ok) {
      testText.value += '（试试在地址后补 /v1）'
    }
  } catch (error) {
    testText.value = FALLBACK_MESSAGES[error.code] || error.message || '测试失败'
  } finally {
    testing.value = false
  }
}

async function handleSetDefault(model) {
  try {
    await setDefaultSiteModel(model.id)
    ElMessage.success('已设为站点默认')
    siteModels.value = await listSiteModels()
  } catch (error) {
    ElMessage.error(FALLBACK_MESSAGES[error.code] || error.message || '操作失败')
  }
}

async function handleDeleteSite(model) {
  try {
    await ElMessageBox.confirm(`确定删除站点级模型「${model.displayName}」？`, '删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteSiteModel(model.id)
    ElMessage.success('已删除')
    siteModels.value = await listSiteModels()
  } catch (error) {
    ElMessage.error(FALLBACK_MESSAGES[error.code] || error.message || '删除失败')
  }
}

function startEditTemplate(tpl) {
  editingTemplateId.value = tpl.id
  templateForm.name = tpl.name
  templateForm.description = tpl.description || ''
  templateForm.content = tpl.content || ''
  templateForm.enabled = Boolean(tpl.enabled)
  templateError.value = ''
}

function resetTemplateForm() {
  editingTemplateId.value = null
  templateForm.name = ''
  templateForm.description = ''
  templateForm.content = ''
  templateForm.enabled = true
  templateError.value = ''
}

async function handleSaveTemplate() {
  if (!templateForm.name.trim() || !templateForm.content.trim()) {
    templateError.value = '请填写模板名与正文'
    return
  }
  savingTemplate.value = true
  templateError.value = ''
  const payload = {
    name: templateForm.name.trim(),
    description: templateForm.description.trim(),
    content: templateForm.content,
    enabled: templateForm.enabled
  }
  try {
    if (editingTemplateId.value) {
      await updatePromptTemplate(editingTemplateId.value, payload)
    } else {
      await createPromptTemplate(payload)
    }
    ElMessage.success('已保存模板')
    resetTemplateForm()
    templates.value = await listPromptTemplates()
  } catch (error) {
    templateError.value = FALLBACK_MESSAGES[error.code] || error.message || '保存失败'
  } finally {
    savingTemplate.value = false
  }
}

async function handleDeleteTemplate(tpl) {
  try {
    await ElMessageBox.confirm(`确定删除模板「${tpl.name}」？`, '删除模板', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deletePromptTemplate(tpl.id)
    ElMessage.success('已删除')
    templates.value = await listPromptTemplates()
  } catch (error) {
    ElMessage.error(FALLBACK_MESSAGES[error.code] || error.message || '删除失败')
  }
}

onMounted(loadAll)
</script>

<style scoped>
.admin-ai {
  max-width: 1080px;
  margin: 0 auto;
}

.admin-ai__title {
  margin: 0 0 var(--space-1);
  color: var(--color-text-primary);
  font-size: var(--font-size-xl);
  font-weight: 600;
}

.admin-ai__sub {
  margin: 0 0 var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.admin-ai__section {
  margin-bottom: var(--space-6);
}

.admin-ai__section-title {
  margin: 0 0 var(--space-3);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
  font-weight: 600;
}

.admin-ai__setting {
  display: flex;
  gap: var(--space-3);
  align-items: flex-start;
  padding: var(--space-3);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.admin-ai__hint {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

.admin-ai__cards {
  padding: 0;
  margin: 0 0 var(--space-3);
  list-style: none;
}

.admin-ai__card {
  padding: var(--space-3);
  margin-bottom: var(--space-2);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.admin-ai__card-head {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  align-items: center;
  color: var(--color-text-primary);
}

.admin-ai__tag {
  padding: 1px var(--space-2);
  color: var(--color-bg-card);
  font-size: var(--font-size-aux);
  background-color: var(--color-primary);
  border-radius: 999px;
}

.admin-ai__tag--muted {
  background-color: var(--color-text-secondary);
}

.admin-ai__card-meta {
  margin: var(--space-1) 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  overflow-wrap: anywhere;
}

.admin-ai__card-actions {
  display: flex;
  gap: var(--space-1);
  margin-top: var(--space-2);
}

.admin-ai__form {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.admin-ai__form-title {
  margin: 0;
  color: var(--color-text-primary);
  font-size: var(--font-size-base);
  font-weight: 600;
}

.admin-ai__field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.admin-ai__field > span {
  color: var(--color-text-primary);
  font-size: var(--font-size-aux);
}

.admin-ai__field input:not([type='checkbox']),
.admin-ai__field textarea {
  padding: var(--space-2) var(--space-3);
  color: var(--color-text-primary);
  font-family: inherit;
  font-size: var(--font-size-base);
  background-color: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.admin-ai__field--inline {
  flex-direction: row;
  gap: var(--space-2);
  align-items: center;
}

.admin-ai__field--inline > span {
  font-size: var(--font-size-base);
}

.admin-ai__error {
  margin: 0;
  color: var(--color-danger);
  font-size: var(--font-size-aux);
}

.admin-ai__form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  align-items: center;
}
</style>
