<template>
  <div class="ais">
    <!-- 左侧：已配置列表（与对话页的会话栏同一套视觉语言） -->
    <aside class="ais__side">
      <div class="ais__side-head">
        <RouterLink to="/assistant" class="ais__back">
          <svg viewBox="0 0 24 24" width="13" height="13" aria-hidden="true" focusable="false">
            <path fill="currentColor" d="M15.4 7.4 14 6l-6 6 6 6 1.4-1.4L10.8 12z" />
          </svg>
          <span>返回对话</span>
        </RouterLink>
      </div>

      <div class="ais__side-body">
        <div class="ais__list-head">
          <span class="ais__list-title">已配置 {{ models.length }} 个模型</span>
        </div>

        <ul v-if="models.length" class="ais__cards">
          <li
            v-for="model in models"
            :key="model.id"
            class="ais__item"
            :class="{ 'is-active': editingId === model.id, 'is-menu-open': menuId === model.id }"
          >
            <button type="button" class="ais__item-main" @click="startEdit(model)">
              <span class="ais__item-row">
                <span class="ais__item-name">{{ model.displayName }}</span>
                <span
                  class="ais__item-dot"
                  :class="`is-${model.testStatus}`"
                  :title="testStatusText(model.testStatus)"
                  aria-hidden="true"
                ></span>
              </span>
              <span class="ais__item-sub">
                <span class="ais__item-model">{{ model.modelId }}</span>
                <span v-if="model.isDefault" class="ais__tag ais__tag--primary">默认</span>
                <span v-if="!model.isEnabled" class="ais__tag">已停用</span>
              </span>
              <span class="ais__item-meta">
                {{ model.hasCredential ? model.apiKeyHint : '未填写凭证' }}
              </span>
            </button>

            <button
              type="button"
              class="ais__item-more"
              :class="{ 'is-open': menuId === model.id }"
              :aria-label="`更多操作：${model.displayName}`"
              :aria-expanded="menuId === model.id ? 'true' : 'false'"
              @click.stop="toggleItemMenu(model.id)"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
                <circle cx="6" cy="12" r="1.6" fill="currentColor" />
                <circle cx="12" cy="12" r="1.6" fill="currentColor" />
                <circle cx="18" cy="12" r="1.6" fill="currentColor" />
              </svg>
            </button>

            <div v-if="menuId === model.id" class="ais__item-menu" role="menu">
              <button type="button" role="menuitem" @click.stop="startEdit(model)">编辑</button>
              <button
                type="button"
                role="menuitem"
                :disabled="!model.isEnabled"
                @click.stop="handleSetDefault(model)"
              >
                设为默认
              </button>
              <button type="button" role="menuitem" @click.stop="handleToggle(model)">
                {{ model.isEnabled ? '停用' : '启用' }}
              </button>
              <button
                type="button"
                role="menuitem"
                class="is-danger"
                @click.stop="handleDelete(model)"
              >
                删除
              </button>
            </div>
          </li>
        </ul>

        <div v-else class="ais__empty">
          <span class="ais__empty-mark" aria-hidden="true">✦</span>
          <p class="ais__empty-title">还没有配置</p>
          <p class="ais__empty-sub">在右侧填写服务商地址、凭证与模型 ID，即可开始对话</p>
        </div>
      </div>

      <div class="ais__side-foot">
        <span>支持 OpenAI 兼容接口</span>
      </div>
    </aside>

    <!-- 右侧：新增 / 编辑表单（B2 单表单） -->
    <main class="ais__main">
      <div class="ais__scroll">
        <div class="ais__inner">
          <!-- 页面标题（原在左栏，挪到主区，避免一页两个标题把版面切碎） -->
          <header class="ais__page-head">
            <div class="ais__page-head-main">
              <span class="ais__page-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="19" height="19" fill="none" focusable="false">
                  <path
                    d="M4 7h8M16 7h4M4 17h4M12 17h8"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                  />
                  <circle cx="14" cy="7" r="2.2" stroke="currentColor" stroke-width="1.8" />
                  <circle cx="10" cy="17" r="2.2" stroke="currentColor" stroke-width="1.8" />
                </svg>
              </span>
              <div class="ais__page-head-copy">
                <h1 class="ais__page-title">模型配置</h1>
                <p class="ais__page-sub">配置要接入的大模型；凭证加密保存，页面上只显示掩码</p>
              </div>
            </div>

            <div v-if="models.length" class="ais__page-meta">
              <span class="ais__meta-pill">启用 {{ enabledCount }}/{{ MAX_ENABLED }}</span>
              <template v-for="model in models" :key="model.id">
                <span v-if="model.isDefault" class="ais__meta-pill ais__meta-pill--primary">
                  默认 · {{ model.displayName }}
                </span>
              </template>
            </div>
          </header>

          <!-- D3-b：主密钥备份横幅 -->
          <div v-if="masterKeyAutoGenerated || showMasterKeyBanner" class="ais__banner ais__banner--key">
            <span class="ais__banner-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" focusable="false">
                <path
                  d="M12 3 5 6v5c0 4.4 2.9 8.2 7 9.5 4.1-1.3 7-5.1 7-9.5V6l-7-3Z"
                  stroke="currentColor"
                  stroke-width="1.7"
                  stroke-linejoin="round"
                />
                <path
                  d="M9.5 12.2 11.4 14l3.4-3.6"
                  stroke="currentColor"
                  stroke-width="1.7"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </span>
            <p>
              主密钥由本机自动生成。<strong>换机或从备份恢复数据库前请先备份主密钥</strong>，否则已保存的凭证将无法解密。
            </p>
            <div class="ais__banner-actions">
              <el-button v-if="isAdmin" size="small" @click="handleExportKey">显示 / 备份主密钥</el-button>
              <el-button size="small" text @click="showMasterKeyBanner = false">知道了</el-button>
            </div>
          </div>

          <div v-if="!allowUserOwnKey" class="ais__banner ais__banner--warn">
            <span class="ais__banner-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" focusable="false">
                <path
                  d="M12 4.5 21 20H3L12 4.5Z"
                  stroke="currentColor"
                  stroke-width="1.7"
                  stroke-linejoin="round"
                />
                <path d="M12 10v4.2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                <circle cx="12" cy="17" r="1" fill="currentColor" />
              </svg>
            </span>
            <p>管理员已关闭「用户自带凭证」功能，当前无法新增或修改配置（已有配置会在开关重新打开后恢复）。</p>
          </div>

          <form class="ais__form" @submit.prevent="handleSave">
            <header class="ais__form-head">
              <h2 class="ais__form-title">{{ editingId ? '编辑配置' : '新增配置' }}</h2>
              <!-- B2 的补偿：模板快捷填充（只预填，不改变表单结构） -->
              <div class="ais__presets">
                <span class="ais__presets-label">快捷填充</span>
                <button
                  v-for="preset in PRESETS"
                  :key="preset.key"
                  type="button"
                  class="ais__preset"
                  @click="applyPreset(preset)"
                >
                  {{ preset.label }}
                </button>
              </div>
            </header>

            <fieldset class="ais__fieldset" :disabled="!allowUserOwnKey">
              <legend class="ais__sr-only">模型配置表单</legend>

              <!-- ① 连接 -->
              <section class="ais__group">
                <h3 class="ais__group-title">连接</h3>
                <p class="ais__group-desc">服务商地址与访问凭证</p>
                <div class="ais__fields ais__fields--split">
                  <label class="ais__field" :class="{ 'is-err': fieldErrors.displayName }" data-field="displayName">
                    <span class="ais__label">展示名 <em class="ais__req">*</em></span>
                    <input
                      v-model="form.displayName"
                      maxlength="32"
                      placeholder="如 我的 DeepSeek"
                      @input="clearFieldError('displayName')"
                    />
                    <small v-if="fieldErrors.displayName" class="ais__hint ais__hint--err">
                      {{ fieldErrors.displayName }}
                    </small>
                  </label>

                  <label class="ais__field" :class="{ 'is-err': fieldErrors.baseUrl }" data-field="baseUrl">
                    <span class="ais__label">接口地址（Base URL） <em class="ais__req">*</em></span>
                    <input
                      v-model="form.baseUrl"
                      placeholder="https://api.deepseek.com/v1"
                      @input="clearFieldError('baseUrl')"
                    />
                    <small v-if="fieldErrors.baseUrl" class="ais__hint ais__hint--err">
                      {{ fieldErrors.baseUrl }}
                    </small>
                    <small v-else-if="baseUrlNeedsScheme" class="ais__hint ais__hint--warn">
                      缺少协议头
                      <button type="button" class="ais__link-btn" @click="prefixHttps">补全 https://</button>
                    </small>
                    <small v-if="suggestV1" class="ais__hint ais__hint--warn">
                      地址没有路径后缀，多数厂商需要带 <code>/v1</code>
                    </small>
                  </label>

                  <label class="ais__field ais__field--wide" :class="{ 'is-err': fieldErrors.apiKey }" data-field="apiKey">
                    <span class="ais__label">
                      API Key
                      <em v-if="!editingId" class="ais__req">*</em>
                      <template v-else>（留空表示不修改）</template>
                    </span>
                    <input
                      v-model="form.apiKey"
                      type="password"
                      autocomplete="new-password"
                      :placeholder="editingHint || 'ak-xxxx 或 sk-xxxx'"
                      @input="clearFieldError('apiKey')"
                    />
                    <small v-if="fieldErrors.apiKey" class="ais__hint ais__hint--err">
                      {{ fieldErrors.apiKey }}
                    </small>
                    <small v-else class="ais__hint">只做加密存储，页面与日志都不会显示明文</small>
                  </label>
                </div>
              </section>

              <!-- ② 模型 -->
              <section class="ais__group">
                <h3 class="ais__group-title">模型</h3>
                <p class="ais__group-desc">实际调用的模型标识</p>
                <div class="ais__fields ais__fields--row">
                  <label class="ais__field" :class="{ 'is-err': fieldErrors.modelId }" data-field="modelId">
                    <span class="ais__label">模型 ID <em class="ais__req">*</em></span>
                    <input
                      v-model="form.modelId"
                      placeholder="如 deepseek-chat"
                      @input="clearFieldError('modelId')"
                    />
                    <small v-if="fieldErrors.modelId" class="ais__hint ais__hint--err">
                      {{ fieldErrors.modelId }}
                    </small>
                    <small v-else-if="modelIdNotInFetched" class="ais__hint ais__hint--warn">
                      该模型不在「测试连接」返回的列表里（仍可保存，请确认拼写）
                    </small>
                    <small v-if="fetchedModels.length" class="ais__hint">
                      测试返回 {{ fetchedModels.length }} 个模型，点击填入：
                      <button
                        v-for="m in fetchedModels.slice(0, 8)"
                        :key="m"
                        type="button"
                        class="ais__model-chip"
                        @click="form.modelId = m; clearFieldError('modelId')"
                      >
                        {{ m }}
                      </button>
                    </small>
                  </label>

                  <label class="ais__field" :class="{ 'is-err': fieldErrors.modelDisplay }" data-field="modelDisplay">
                    <span class="ais__label">模型展示名</span>
                    <input
                      v-model="form.modelDisplay"
                      placeholder="留空则与模型 ID 相同"
                      @input="clearFieldError('modelDisplay')"
                    />
                    <small v-if="fieldErrors.modelDisplay" class="ais__hint ais__hint--err">
                      {{ fieldErrors.modelDisplay }}
                    </small>
                  </label>
                </div>
              </section>

            </fieldset>

            <!-- ④ 测试与保存 -->
            <div class="ais__actions">
              <div class="ais__actionbar">
                <div class="ais__actionbar-left">
                  <el-button
                    :loading="testing"
                    :disabled="!allowUserOwnKey || !form.baseUrl || (!form.apiKey && !editingId)"
                    @click="handleTest"
                  >
                    测试连接
                  </el-button>
                  <span
                    v-if="testResult"
                    class="ais__test-result"
                    :class="testResult.ok ? 'is-ok' : 'is-fail'"
                  >
                    {{ testResultText }}
                  </span>
                </div>

                <div class="ais__actionbar-right">
                  <el-button v-if="editingId" @click="resetForm">取消</el-button>
                  <el-button type="primary" :loading="saving" :disabled="!allowUserOwnKey" @click="handleSave">
                    {{ editingId ? '保存修改' : '保存配置' }}
                  </el-button>
                </div>
              </div>

              <p v-if="formError" class="ais__error">{{ formError }}</p>
              <p v-if="!allowUserOwnKey" class="ais__hint">管理员已关闭自带凭证，表单当前只读。</p>
            </div>
          </form>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createMyModel,
  deleteMyModel,
  exportMasterKey,
  getChatMeta,
  listMyModels,
  setDefaultModel,
  testConnection,
  toggleModel,
  updateMyModel
} from '@/api/ai'
import { FALLBACK_MESSAGES } from '@/utils/request'
import { useUserStore } from '@/stores/user'

/** 单个用户最多同时启用的模型数（D13） */
const MAX_ENABLED = 5

/**
 * B2 单表单（08 §9.2）：预设退化为「快捷填充」——只填 base_url 与模型候选，
 * 不改变表单结构，避免回到 B1 的分支复杂度。
 */
const PRESETS = [
  { key: 'openai', label: 'OpenAI', baseUrl: 'https://api.openai.com/v1', modelId: 'gpt-4o-mini' },
  {
    key: 'spark',
    label: '讯飞星火',
    baseUrl: 'https://maas-api.cn-huabei-1.xf-yun.com/v2',
    modelId: 'spark-x2.6',
    thinking: true
  },
  { key: 'deepseek', label: 'DeepSeek', baseUrl: 'https://api.deepseek.com/v1', modelId: 'deepseek-chat' },
  { key: 'custom', label: '自定义兼容', baseUrl: '', modelId: '' }
]

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)

const models = ref([])
const editingId = ref(null)
const saving = ref(false)
const testing = ref(false)
const formError = ref('')
const testResult = ref(null)
const fetchedModels = ref([])
const suggestV1 = ref(false)
const editingHint = ref('')
const allowUserOwnKey = ref(true)
const masterKeyAutoGenerated = ref(false)
const showMasterKeyBanner = ref(false)

/**
 * 表单默认值。
 *
 * thinkingDisabled 的语义是「禁用上游思维链」，提交时取反成 thinkingEnabled：
 * 后端只有 thinkingEnabled=false 才会在请求体写入 thinking:{type:"disabled"}，
 * 而 05 §2.1 要求必须关闭（Spark 不关会卡首 token 且按输出 token 多计费）。
 * 所以这里的默认是 true（= 禁用），与全局默认一致。
 *
 * temperature / maxTokens / isEnabled / makeDefault 仍在表单状态里保留：
 * 界面上不再暴露，但编辑时要把服务端已有的值原样带回去，避免「改个名字把参数重置了」。
 */
const emptyForm = () => ({
  providerType: 'custom',
  displayName: '',
  baseUrl: '',
  accessMode: 'direct',
  modelId: '',
  modelDisplay: '',
  apiKey: '',
  temperature: 0.3,
  maxTokens: 1200,
  thinkingDisabled: true,
  isEnabled: true,
  makeDefault: false
})

const form = reactive(emptyForm())

const enabledCount = computed(() => models.value.filter((m) => m.isEnabled).length)

const testResultText = computed(() => {
  const result = testResult.value
  if (!result) return ''
  if (result.ok) {
    const degraded = result.status === 'passed_degraded' ? '（该网关不支持 /models，已用最小请求验证）' : ''
    return `连接正常 ${result.latencyMs}ms${degraded}`
  }
  return result.error || '连接失败'
})

function testStatusText(status) {
  return (
    {
      untested: '未测试',
      passed: '已连通',
      passed_degraded: '已连通（降级）',
      failed: '测试失败'
    }[status] || '未测试'
  )
}

function applyPreset(preset) {
  form.providerType = preset.key
  if (preset.baseUrl) {
    form.baseUrl = preset.baseUrl
    clearFieldError('baseUrl')
  }
  if (preset.modelId) {
    form.modelId = preset.modelId
    clearFieldError('modelId')
  }
  if (preset.thinking) form.thinkingDisabled = true
  suggestV1.value = false
}

async function loadModels() {
  try {
    models.value = await listMyModels()
  } catch {
    models.value = []
  }
}

async function loadMeta() {
  try {
    const meta = await getChatMeta()
    allowUserOwnKey.value = Boolean(meta.allowUserOwnKey)
    masterKeyAutoGenerated.value = Boolean(meta.masterKeyAutoGenerated)
  } catch {
    // 元数据失败不阻塞页面；保存时后端仍会给 AI_CONFIG_DISABLED
  }
}

/* ---------------- 侧栏卡片「⋯」菜单（与对话页会话项一致） ---------------- */

const menuId = ref(null)

function toggleItemMenu(id) {
  menuId.value = menuId.value === id ? null : id
}

function closeItemMenu() {
  menuId.value = null
}

/** 点击菜单外部关闭；键盘用户可用 Esc */
function onDocumentClick(event) {
  if (!menuId.value) return
  const target = event.target
  if (target && target.closest && target.closest('.ais__item')) return
  closeItemMenu()
}

function onDocumentKeydown(event) {
  if (event.key === 'Escape') closeItemMenu()
}

function startEdit(model) {
  closeItemMenu()
  editingId.value = model.id
  form.providerType = model.providerType
  form.displayName = model.displayName
  form.baseUrl = model.baseUrl
  form.accessMode = model.accessMode
  form.modelId = model.modelId
  form.modelDisplay = model.modelDisplay
  form.apiKey = ''
  form.temperature = model.temperature ?? 0.3
  form.maxTokens = model.maxTokens ?? 1200
  form.thinkingDisabled = model.thinkingEnabled === false
  form.isEnabled = Boolean(model.isEnabled)
  form.makeDefault = false
  editingHint.value = model.hasCredential ? `已保存：${model.apiKeyHint}` : ''
  formError.value = ''
  fieldErrors.value = {}
  testResult.value = null
  fetchedModels.value = []
  suggestV1.value = false
}

function resetForm() {
  closeItemMenu()
  editingId.value = null
  Object.assign(form, emptyForm())
  formError.value = ''
  fieldErrors.value = {}
  testResult.value = null
  fetchedModels.value = []
  suggestV1.value = false
  editingHint.value = ''
}

/* ---------------- v3.24：字段级校验 ---------------- */

const fieldErrors = ref({})

/** 接口地址是否缺少协议头（用于行内提示 + 一键补全） */
const baseUrlNeedsScheme = computed(() => {
  const value = form.baseUrl.trim()
  return Boolean(value) && !/^https?:\/\//i.test(value)
})

/** 填写的模型 ID 是否不在「测试连接」返回的列表里（软提示，不阻断保存） */
const modelIdNotInFetched = computed(() => {
  const id = form.modelId.trim()
  return Boolean(id) && fetchedModels.value.length > 0 && !fetchedModels.value.includes(id)
})

function clearFieldError(field) {
  if (fieldErrors.value[field]) delete fieldErrors.value[field]
}

function prefixHttps() {
  const value = form.baseUrl.trim()
  if (value && !/^https?:\/\//i.test(value)) {
    form.baseUrl = `https://${value}`
    clearFieldError('baseUrl')
  }
}

/**
 * 提交前校验（与后端 ModelConfigSaveReq 的约束保持一致）。
 * 后端仍是权威，这里只是把错误提前到字段上，避免「点保存→底部冒出一条错误→自己找是哪个字段」。
 */
function validateForm() {
  const errors = {}

  const displayName = form.displayName.trim()
  if (!displayName) errors.displayName = '请填写展示名'
  else if (displayName.length > 32) errors.displayName = '展示名不能超过 32 字'

  const baseUrl = form.baseUrl.trim()
  if (!baseUrl) {
    errors.baseUrl = '请填写接口地址（Base URL）'
  } else if (!/^https?:\/\/\S+$/i.test(baseUrl)) {
    errors.baseUrl = '需以 http:// 或 https:// 开头，且不能包含空格'
  } else if (baseUrl.length > 512) {
    errors.baseUrl = '接口地址过长（最多 512 字符）'
  }

  const modelId = form.modelId.trim()
  if (!modelId) errors.modelId = '请填写模型 ID'
  else if (modelId.length > 128) errors.modelId = '模型 ID 不能超过 128 字符'

  if (!editingId.value && !form.apiKey.trim()) errors.apiKey = '请填写 API Key'
  else if (form.apiKey.length > 512) errors.apiKey = 'API Key 过长（最多 512 字符）'

  if (form.modelDisplay.trim().length > 64) errors.modelDisplay = '模型展示名不能超过 64 字'

  const temperature = Number(form.temperature)
  if (!Number.isFinite(temperature) || temperature < 0 || temperature > 2) {
    errors.temperature = '需在 0–2 之间'
  }
  const maxTokens = Number(form.maxTokens)
  if (!Number.isInteger(maxTokens) || maxTokens < 1 || maxTokens > 8000) {
    errors.maxTokens = '需在 1–8000 之间'
  }

  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

/** 定位到第一个出错的字段 */
function focusFirstError() {
  const first = Object.keys(fieldErrors.value)[0]
  if (!first) return
  const box = document.querySelector(`[data-field="${first}"]`)
  if (!box) return
  box.scrollIntoView({ behavior: 'smooth', block: 'center' })
  const input = box.querySelector('input')
  if (input) input.focus({ preventScroll: true })
}

async function handleTest() {
  // 先做格式校验：地址没协议头时直接测只会拿到难懂的网络错误
  if (!validateForm()) {
    focusFirstError()
    return
  }
  testing.value = true
  formError.value = ''
  testResult.value = null
  try {
    const result = await testConnection({
      configId: editingId.value,
      providerType: form.providerType,
      baseUrl: form.baseUrl.trim(),
      apiKey: form.apiKey || undefined,
      modelId: form.modelId.trim()
    })
    testResult.value = result
    fetchedModels.value = result.models || []
    suggestV1.value = Boolean(result.suggestV1) && !result.ok
    if (result.ok) {
      await loadModels()
    }
  } catch (error) {
    testResult.value = { ok: false, error: error.message || '测试失败' }
  } finally {
    testing.value = false
  }
}

/** 去掉尾部斜杠再比较，避免 /v1 与 /v1/ 被当成不同端点 */
function normalizeForCompare(url) {
  return (url || '').trim().replace(/\/+$/, '')
}

/** 已存在「同接口地址 + 同模型」的配置（新增前软确认，不阻断） */
function findSameEndpoint() {
  const base = normalizeForCompare(form.baseUrl)
  const modelId = form.modelId.trim()
  return models.value.find(
    (item) => normalizeForCompare(item.baseUrl) === base && item.modelId === modelId
  )
}

/** 去掉后端校验消息里的技术字段前缀（"baseUrl: xxx" → "xxx"），给用户看的文案不留 camelCase */
function humanizeServerMessage(message) {
  return String(message || '').replace(/(^|; )([a-zA-Z][a-zA-Z0-9_]*): /g, '$1')
}

async function handleSave() {
  if (!validateForm()) {
    focusFirstError()
    return
  }

  // 同一模型 + 同一地址可能是「严谨版/创意版」这类不同参数配置，属合法用法，
  // 因此只做确认提示，不禁止（参考 Open WebUI 对重复 Model ID 的拒绝仅限同一连接内）
  if (!editingId.value) {
    const same = findSameEndpoint()
    if (same) {
      try {
        await ElMessageBox.confirm(
          `已存在同接口地址与模型的配置「${same.displayName}」。若你想用不同参数（如 temperature），继续新增即可；否则建议直接编辑它。`,
          '确认新增相似配置',
          { confirmButtonText: '仍然新增', cancelButtonText: '取消', type: 'info' }
        )
      } catch {
        return
      }
    }
  }

  saving.value = true
  formError.value = ''
  const payload = {
    providerType: form.providerType,
    displayName: form.displayName.trim(),
    baseUrl: form.baseUrl.trim(),
    accessMode: form.accessMode,
    modelId: form.modelId.trim(),
    modelDisplay: form.modelDisplay.trim() || undefined,
    apiKey: form.apiKey.trim() || undefined,
    temperature: Number(form.temperature),
    maxTokens: Number(form.maxTokens),
    thinkingEnabled: !form.thinkingDisabled,
    isEnabled: form.isEnabled,
    makeDefault: form.makeDefault
  }
  try {
    if (editingId.value) {
      await updateMyModel(editingId.value, payload)
    } else {
      await createMyModel(payload)
    }
    ElMessage.success(editingId.value ? '已保存修改' : '配置已保存')
    resetForm()
    await loadModels()
  } catch (error) {
    formError.value = humanizeServerMessage(
      FALLBACK_MESSAGES[error.code] || error.message || '保存失败'
    )
  } finally {
    saving.value = false
  }
}

async function handleSetDefault(model) {
  closeItemMenu()
  try {
    await setDefaultModel(model.id)
    ElMessage.success(`已将「${model.displayName}」设为默认`)
    await loadModels()
  } catch (error) {
    ElMessage.error(FALLBACK_MESSAGES[error.code] || error.message || '操作失败')
  }
}

async function handleToggle(model) {
  closeItemMenu()
  try {
    await toggleModel(model.id, !model.isEnabled)
    await loadModels()
  } catch (error) {
    ElMessage.error(FALLBACK_MESSAGES[error.code] || error.message || '操作失败')
  }
}

async function handleDelete(model) {
  closeItemMenu()
  try {
    await ElMessageBox.confirm(
      `确定删除「${model.displayName}」？被历史会话引用时无法删除，可改为停用。`,
      '删除配置',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteMyModel(model.id)
    ElMessage.success('已删除')
    if (editingId.value === model.id) resetForm()
    await loadModels()
  } catch (error) {
    ElMessage.error(FALLBACK_MESSAGES[error.code] || error.message || '删除失败')
  }
}

async function handleExportKey() {
  try {
    await ElMessageBox.confirm(
      '主密钥等同于所有已保存凭证的钥匙，请勿截图外传。确认显示？',
      '备份主密钥',
      { type: 'warning', confirmButtonText: '显示', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    const result = await exportMasterKey()
    await ElMessageBox.alert(
      `主密钥（Base64）：\n\n${result.base64}\n\n${result.notice}`,
      '请立即备份',
      { confirmButtonText: '我已备份' }
    )
  } catch (error) {
    ElMessage.error(FALLBACK_MESSAGES[error.code] || error.message || '导出失败')
  }
}

onMounted(async () => {
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('keydown', onDocumentKeydown)
  await Promise.all([loadModels(), loadMeta()])
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('keydown', onDocumentKeydown)
})
</script>

<style scoped>
.ais {
  height: calc(100dvh - var(--header-height));
  background-color: var(--color-bg-card);
  display: flex;
  overflow: hidden;
}
.ais__side {
  background-color: var(--ai-sidebar-bg);
  flex-direction: column;
  flex-shrink: 0;
  width: 300px;
  display: flex;
}
.ais__side-head {
  padding: var(--space-4) var(--space-3) var(--space-1);
  flex-shrink: 0;
}
.ais__back {
  margin-bottom: 0;
  color: var(--ai-text-2);
  font-size: var(--font-size-aux);
  transition: color var(--motion-fast) var(--motion-ease);
  align-items: center;
  gap: 2px;
  text-decoration: none;
  display: inline-flex;
}
.ais__back:hover {
  color: var(--ai-text);
}
.ais__side-body {
  min-height: 0;
  padding: 0 var(--space-2) var(--space-2);
  flex: 1;
  overflow-y: auto;
}
.ais__list-head {
  gap: var(--space-2);
  padding: var(--space-2) var(--space-2) var(--space-1);
  justify-content: space-between;
  align-items: center;
  display: flex;
}
.ais__list-title {
  color: var(--ai-text-2);
  font-size: var(--font-size-aux);
  font-weight: 500;
}
.ais__count {
  color: var(--ai-text-3);
  font-size: var(--font-size-aux);
}
.ais__cards {
  flex-direction: column;
  gap: 2px;
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
}
.ais__item {
  position: relative;
  padding: var(--space-2);
  border-radius: var(--ai-radius-sm);
  transition: background-color var(--motion-fast) var(--motion-ease);
}
.ais__item::before {
  position: absolute;
  top: 50%;
  left: 0;
  width: 3px;
  height: 0;
  content: '';
  background-color: var(--color-primary);
  border-radius: 0 3px 3px 0;
  transform: translateY(-50%);
  transition: height var(--motion-fast) var(--motion-ease);
}
.ais__item:hover {
  background-color: var(--ai-hover);
}
.ais__item.is-active {
  background-color: var(--ai-active);
}
.ais__item.is-active::before {
  height: 60%;
}
.ais__item-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  padding: 0 26px 0 0;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  background: 0 0;
  border: none;
}
.ais__item-row {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: flex-start;
}
.ais__item-name {
  overflow: hidden;
  color: var(--ai-text);
  font-size: var(--font-size-body);
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.ais__item-sub {
  display: flex;
  gap: 6px;
  align-items: center;
  min-width: 0;
}
.ais__item-model {
  overflow: hidden;
  color: var(--ai-text-3);
  font-size: var(--font-size-aux);
  white-space: nowrap;
  text-overflow: ellipsis;
}
.ais__tag {
  padding: 0 6px;
  color: var(--ai-text-2);
  font-size: var(--font-size-aux);
  line-height: 18px;
  white-space: nowrap;
  background-color: #0f172a0f;
  border-radius: 6px;
}
.ais__tag--primary {
  color: var(--color-primary);
  background-color: #2563eb1a;
}
/* 连接状态点：颜色替代文字，标题右侧一个 8px 圆点 */
.ais__item-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  background-color: #c4c8cf;
  border-radius: 50%;
}
.ais__item-dot.is-passed {
  background-color: var(--color-success, #12a150);
}
.ais__item-dot.is-passed_degraded {
  background-color: #d97706;
}
.ais__item-dot.is-failed {
  background-color: var(--color-danger);
}
.ais__item-meta {
  overflow: hidden;
  margin: 2px 0 0;
  color: var(--ai-text-2);
  font-family: var(--font-family-mono);
  font-size: var(--font-size-aux);
  white-space: nowrap;
  text-overflow: ellipsis;
}
/* 「⋯」：hover / 键盘聚焦 / 菜单展开时出现（不依赖 hover 才能点） */
.ais__item-more {
  position: absolute;
  top: 8px;
  right: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  color: var(--ai-text-2);
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: 8px;
  opacity: 0;
  transition:
    opacity var(--motion-fast) var(--motion-ease),
    background-color var(--motion-fast) var(--motion-ease);
}
.ais__item:hover .ais__item-more,
.ais__item:focus-within .ais__item-more,
.ais__item.is-menu-open .ais__item-more {
  opacity: 1;
}
.ais__item-more:hover,
.ais__item-more.is-open {
  background-color: rgb(15 23 42 / 6%);
}
.ais__item-menu {
  position: absolute;
  top: 34px;
  right: 6px;
  z-index: 20;
  min-width: 116px;
  padding: 4px;
  background-color: var(--color-bg-card);
  border: 1px solid var(--ai-border);
  border-radius: 10px;
  box-shadow: 0 12px 32px -12px rgb(15 23 42 / 22%);
}
.ais__item-menu button {
  display: flex;
  align-items: center;
  width: 100%;
  height: 32px;
  padding: 0 10px;
  color: var(--ai-text);
  font-family: inherit;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
  background: 0 0;
  border: none;
  border-radius: 8px;
}
.ais__item-menu button:hover {
  background-color: var(--ai-hover);
}
.ais__item-menu button:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}
.ais__item-menu button.is-danger {
  color: var(--color-danger);
}
.ais__item-menu button.is-danger:hover {
  background-color: rgb(var(--color-danger-rgb) / 8%);
}

.ais__empty {
  padding: var(--space-4) var(--space-2);
  text-align: center;
}
.ais__empty-mark {
  margin-bottom: var(--space-2);
  color: var(--ai-text-3);
  font-size: 16px;
  display: block;
}
.ais__empty-title {
  color: var(--ai-text);
  font-size: var(--font-size-body);
  margin: 0;
  font-weight: 500;
}
.ais__empty-sub {
  color: var(--ai-text-2);
  font-size: var(--font-size-aux);
  margin: 2px 0 0;
  line-height: 1.5;
}
.ais__side-foot {
  padding: var(--space-2) var(--space-4) var(--space-3);
  color: var(--ai-text-3);
  font-size: var(--font-size-aux);
  flex-shrink: 0;
}
.ais__main {
  flex-direction: column;
  flex: 1;
  min-width: 0;
  display: flex;
}
.ais__scroll {
  flex: 1;
  overflow-y: auto;
}
.ais__inner {
  max-width: 760px;
  /* 方案 A：不再居中，从侧栏右侧留 48px 开始，消除中间大段空白 */
  padding: var(--space-8) var(--space-6) var(--space-10) var(--space-12);
  margin: 0;
}
.ais__page-head {
  margin-bottom: var(--space-4);
}
.ais__page-title {
  color: var(--ai-text);
  font-size: var(--font-size-xl);
  margin: 0;
  font-weight: 600;
  line-height: 1.25;
}
.ais__page-sub {
  color: var(--ai-text-2);
  font-size: var(--font-size-aux);
  margin: 4px 0 0;
  line-height: 1.5;
}
.ais__banner {
  gap: var(--space-2);
  padding: var(--space-3);
  margin-bottom: var(--space-4);
  color: var(--ai-text);
  font-size: var(--font-size-aux);
  border-radius: var(--ai-radius-sm);
  background-color: #2563eb0f;
  border: 1px solid #2563eb24;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  line-height: 1.6;
  display: flex;
}
.ais__banner p {
  margin: 0;
}
.ais__banner-actions {
  gap: var(--space-1);
  display: flex;
}
.ais__banner--warn {
  background-color: #ea580c0f;
  border-color: #ea580c2e;
}
.ais__form-head {
  margin-bottom: var(--space-4);
}
.ais__form-title {
  margin: 0 0 var(--space-2);
  color: var(--ai-text);
  font-size: var(--font-size-lg);
  font-weight: 600;
  line-height: 1.3;
}
.ais__presets {
  gap: var(--space-1);
  flex-wrap: wrap;
  align-items: center;
  display: flex;
}
.ais__presets-label {
  color: var(--ai-text-3);
  font-size: var(--font-size-aux);
  margin-right: 2px;
}
.ais__preset {
  height: 26px;
  color: var(--ai-text-2);
  font-family: inherit;
  font-size: var(--font-size-aux);
  border: 1px solid var(--ai-border);
  cursor: pointer;
  transition:
    color var(--motion-fast) var(--motion-ease),
    border-color var(--motion-fast) var(--motion-ease),
    background-color var(--motion-fast) var(--motion-ease);
  background-color: #0000;
  border-radius: 999px;
  padding: 0 10px;
}
.ais__preset:hover {
  color: var(--color-primary);
  background-color: #2563eb0d;
  border-color: #2563eb66;
}
.ais__form {
  gap: var(--space-3);
  flex-direction: column;
  display: flex;
}
.ais__group {
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--ai-border);
  border-radius: var(--ai-radius-md);
}
.ais__group-title {
  color: var(--ai-text);
  font-size: var(--font-size-body);
  margin: 0;
  font-weight: 600;
}
.ais__group-desc {
  margin: 2px 0 var(--space-3);
  color: var(--ai-text-3);
  font-size: var(--font-size-aux);
}
.ais__fields {
  gap: var(--space-4);
  flex-direction: column;
  display: flex;
}
.ais__fields--row {
  gap: var(--space-3);
  grid-template-columns: 1fr 1fr;
  display: grid;
}
.ais__field {
  flex-direction: column;
  gap: 8px;
  display: flex;
}
.ais__label {
  color: var(--ai-text-2);
  font-size: var(--font-size-aux);
  font-weight: 500;
}
.ais__req {
  color: var(--color-danger);
  font-style: normal;
}
.ais__field input:not([type="checkbox"]) {
  height: 44px;
  color: var(--ai-text);
  font-family: inherit;
  font-size: var(--font-size-body);
  background-color: var(--color-bg-card);
  border: 1px solid var(--ai-border);
  border-radius: var(--ai-radius-sm);
  transition:
    border-color var(--motion-fast) var(--motion-ease),
    box-shadow var(--motion-fast) var(--motion-ease);
  outline: none;
  padding: 0 12px;
}
.ais__field input:not([type="checkbox"])::placeholder {
  color: var(--ai-text-3);
}
.ais__field input:not([type="checkbox"]):hover {
  border-color: #d6d9df;
}
.ais__field input:not([type="checkbox"]):focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px #2563eb1f;
}
.ais__hint {
  color: var(--ai-text-3);
  font-size: var(--font-size-aux);
  line-height: 1.5;
  display: block;
}
.ais__hint--warn {
  color: #b45309;
}
.ais__hint code {
  font-family: var(--font-family-mono);
  background-color: #0f172a0f;
  border-radius: 4px;
  padding: 0 4px;
}
.ais__model-chip {
  height: 22px;
  color: var(--color-primary);
  font-family: inherit;
  font-size: var(--font-size-aux);
  cursor: pointer;
  background-color: #2563eb14;
  border: none;
  border-radius: 999px;
  margin-left: 4px;
  padding: 0 8px;
}
.ais__model-chip:hover {
  background-color: #2563eb24;
}
.ais__checks {
  gap: var(--space-2);
  padding-top: var(--space-3);
  margin-top: var(--space-3);
  border-top: 1px solid var(--ai-border);
  flex-direction: column;
  display: flex;
}
.ais__checks--plain {
  border-top: none;
  margin-top: 0;
  padding-top: 0;
}
.ais__check {
  color: var(--ai-text);
  font-size: var(--font-size-aux);
  cursor: pointer;
  align-items: flex-start;
  gap: 8px;
  line-height: 1.5;
  display: flex;
}
.ais__check input {
  cursor: pointer;
  width: 15px;
  height: 15px;
  accent-color: var(--color-primary);
  margin: 1px 0 0;
}
.ais__actions {
  gap: var(--space-3);
  flex-direction: column;
  display: flex;
}
/* 操作条：左「测试连接」右「取消 / 保存」，用上边框与表单分组区分 */
.ais__actionbar {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  align-items: center;
  justify-content: space-between;
  padding-top: var(--space-4);
  border-top: 1px solid var(--ai-border);
}
.ais__actionbar-left,
.ais__actionbar-right {
  display: flex;
  gap: var(--space-2);
  align-items: center;
}
.ais__test-result {
  font-size: var(--font-size-aux);
}
.ais__test-result.is-ok {
  color: var(--color-success, #12a150);
}
.ais__test-result.is-fail {
  color: var(--color-danger);
}
.ais__error {
  padding: var(--space-2) var(--space-3);
  color: var(--color-danger);
  font-size: var(--font-size-aux);
  background-color: rgb(var(--color-danger-rgb) / 6%);
  border-radius: var(--ai-radius-sm);
  margin: 0;
}
.ais__fieldset {
  gap: var(--space-3);
  border: none;
  flex-direction: column;
  min-width: 0;
  margin: 0;
  padding: 0;
  display: flex;
}
.ais__fieldset:disabled {
  opacity: 0.6;
}
.ais__fieldset:disabled input {
  cursor: not-allowed;
}
.ais__sr-only {
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  position: absolute;
  overflow: hidden;
}
.ais__field.is-err input:not([type="checkbox"]) {
  border-color: var(--color-danger);
}
.ais__field.is-err input:not([type="checkbox"]):focus {
  box-shadow: 0 0 0 3px rgb(var(--color-danger-rgb) / 12%);
}
.ais__hint--err {
  color: var(--color-danger);
}
.ais__hint--block {
  margin: var(--space-2) 0 0;
}
.ais__link-btn {
  color: var(--color-primary);
  font-family: inherit;
  font-size: inherit;
  cursor: pointer;
  text-underline-offset: 2px;
  background: 0 0;
  border: none;
  margin-left: 4px;
  padding: 0;
  text-decoration: underline;
}
.ais__link-btn:hover {
  opacity: 0.8;
}
@media (width<=1023px) {
  .ais {
    height: auto;
    min-height: calc(100dvh - var(--header-height));
    flex-direction: column;
    overflow: visible;
  }
  .ais__side {
    width: 100%;
  }
  .ais__side-body,
  .ais__scroll {
    overflow: visible;
  }
  .ais__inner {
    padding: var(--space-4);
  }
}
@media (width<=767px) {
  .ais {
    min-height: calc(100dvh - 56px);
  }
  .ais__side-head {
    padding: var(--space-3);
  }
  .ais__fields--row {
    grid-template-columns: 1fr;
  }
}

/* ---------------- v3.25 字体对齐豆包（实测对比后调整） ----------------
 * 1) 12px 辅助文字此前没有显式行高，继承 body 的 22px（1.83）；豆包同类用 1.5。
 * 2) 字重克制：600 只留给页面级标题（模型配置），区块/分组标题降到 500。
 *    （豆包全站 400/500/600 三档，600 仅出现在其大标题上）
 */
.ais__list-title,
.ais__count,
.ais__back,
.ais__item-model,
.ais__item-meta,
.ais__label,
.ais__status {
  line-height: 1.5;
}

/* ---------------- v3.28 全站基准提到 16px 后的字号补齐 ----------------
 * body 由 14 → 16 后，原先 12px 的标签/说明相对变小。
 * 这里按「标签 13 / 说明性文字 14 / 正文 16」重排配置页，与对话页侧栏保持一致。
 */
.ais__label,
.ais__hint,
.ais__presets-label,
.ais__preset,
.ais__list-title,
.ais__count,
.ais__item-model,
.ais__item-meta,
.ais__status,
.ais__side-foot {
  font-size: 13px;
}

.ais__back,
.ais__empty-sub,
.ais__group-desc,
.ais__page-sub {
  font-size: 14px;
}

.ais__empty-title,
.ais__item-name {
  font-size: 16px;
}

/* 12px 文本块：行高收到 1.5 */
.ais__presets-label,
.ais__group-desc,
.ais__side-foot,
.ais__test-result,
.ais__error {
  line-height: 1.5;
}

/* 固定高度的胶囊按钮：行高 = 高度，才能让 12px 文字垂直居中（不能直接 1.5） */
.ais__preset {
  line-height: 26px;
}

.ais__item-actions button {
  line-height: 24px;
}

.ais__model-chip {
  line-height: 22px;
}

/* 字重克制：600 只留给页面级标题（模型配置），区块/分组标题用 500 */
.ais__form-title,
.ais__group-title {
  font-weight: 500;
}
/* ============================================================================
   v6.0 布局重构 · 设置工作台
   ----------------------------------------------------------------------------
   仅调整布局与排版：
   - 左栏 = 模型清单（卡片化、状态点、当前项高亮），与助手页会话栏同一语言；
   - 右栏 = 居中 960px 工作区：页头 → 提示条 → 单张表单卡（双列字段）；
   - 表单卡内部：卡片头（标题 + 快捷填充）→ 连接 / 模型两个分区 → 底部操作条；
   - 所有字段、校验、接口与状态逻辑保持原样。
   ============================================================================ */

.ais {
  --ais-bg: #fbfcff;
  --ais-surface: rgb(255 255 255 / 82%);
  --ais-text: #101828;
  --ais-text-2: #5b6474;
  --ais-text-3: #687284;
  --ais-border: rgb(15 23 42 / 7%);
  --ais-border-strong: rgb(15 23 42 / 12%);
  --ais-primary-weak: rgb(37 99 235 / 8%);
  --ais-gradient: linear-gradient(135deg, #4b8bff 0%, #2563eb 52%, #4f46e5 100%);
  --ais-radius-card: 20px;
  --ais-radius-md: 14px;
  --ais-radius-sm: 10px;
  --ais-shadow-1: 0 1px 2px rgb(16 24 40 / 4%);
  --ais-shadow-2: 0 26px 52px -44px rgb(16 24 40 / 44%);

  color: var(--ais-text);
  background-color: var(--ais-bg);
}

/* ---------- 左栏：模型清单 ---------- */
.ais__side {
  flex: 0 0 300px;
  width: 300px;
  min-height: 0;
  background: linear-gradient(180deg, #f8faff 0%, #f6f8fd 100%);
  border-right: 1px solid var(--ais-border);
}

.ais__side-head {
  flex-shrink: 0;
  padding: 14px 12px 6px;
}

.ais__back {
  display: inline-flex;
  gap: 3px;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  color: var(--ais-text-2);
  font-size: 12.5px;
  line-height: 1.5;
  background-color: rgb(255 255 255 / 70%);
  border: 1px solid var(--ais-border);
  border-radius: 999px;
  box-shadow: 0 1px 2px rgb(16 24 40 / 3%);
  transition:
    color var(--motion-fast) var(--motion-ease),
    background-color var(--motion-fast) var(--motion-ease),
    border-color var(--motion-fast) var(--motion-ease);
}

.ais__back:hover {
  color: var(--color-primary);
  background-color: #fff;
  border-color: rgb(37 99 235 / 22%);
}

.ais__side-body {
  flex: 1;
  min-height: 0;
  padding: 2px 10px 10px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: #dde3ee transparent;
}

.ais__side-body::-webkit-scrollbar {
  width: 8px;
}

.ais__side-body::-webkit-scrollbar-thumb {
  background-color: #dde3ee;
  background-clip: content-box;
  border: 2px solid transparent;
  border-radius: 999px;
}

.ais__list-head {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  padding: 12px 8px 8px;
}

.ais__list-title {
  color: var(--ais-text-3);
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1.5;
  letter-spacing: 0.06em;
}

.ais__cards {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.ais__item {
  position: relative;
  padding: 11px 12px;
  background-color: rgb(255 255 255 / 58%);
  border: 1px solid var(--ais-border);
  border-radius: 12px;
  box-shadow: 0 1px 2px rgb(16 24 40 / 2.5%);
  transition:
    background-color var(--motion-fast) var(--motion-ease),
    border-color var(--motion-fast) var(--motion-ease),
    box-shadow var(--motion-fast) var(--motion-ease);
}

.ais__item::before {
  position: absolute;
  top: 50%;
  left: 0;
  width: 3px;
  height: 0;
  content: '';
  background: var(--ais-gradient);
  border-radius: 0 3px 3px 0;
  transform: translateY(-50%);
  transition: height var(--motion-fast) var(--motion-ease);
}

.ais__item:hover {
  background-color: #fff;
  border-color: rgb(37 99 235 / 14%);
}

.ais__item.is-active {
  background-color: #fff;
  border-color: rgb(37 99 235 / 16%);
  box-shadow: 0 10px 22px -20px rgb(37 99 235 / 65%);
}

.ais__item.is-active::before {
  height: 58%;
}

.ais__item.is-active .ais__item-name {
  color: var(--color-primary-hover);
}

.ais__item-main {
  gap: 3px;
  padding: 0 28px 0 0;
}

.ais__item-row {
  gap: 7px;
}

.ais__item-name {
  color: var(--ais-text);
  font-size: 14.5px;
  font-weight: 500;
  line-height: 1.45;
}

.ais__item-sub {
  gap: 6px;
}

.ais__item-model {
  color: var(--ais-text-3);
  font-size: 11.5px;
  line-height: 1.5;
}

.ais__item-meta {
  margin-top: 4px;
  color: var(--ais-text-2);
  font-size: 11px;
  line-height: 1.5;
}

.ais__tag {
  padding: 0 6px;
  color: var(--ais-text-2);
  font-size: 10.5px;
  line-height: 17px;
  background-color: rgb(15 23 42 / 6%);
  border-radius: 5px;
}

.ais__tag--primary {
  color: var(--color-primary);
  background-color: rgb(37 99 235 / 10%);
}

.ais__item-dot {
  width: 7px;
  height: 7px;
  background-color: #c4c8cf;
  box-shadow: 0 0 0 3px rgb(16 24 40 / 4%);
}

.ais__item-more {
  top: 9px;
  right: 7px;
  width: 26px;
  height: 26px;
  border-radius: 8px;
}

.ais__item-menu {
  top: 36px;
  right: 7px;
  min-width: 120px;
  padding: 5px;
  background-color: #fff;
  border-color: var(--ais-border-strong);
  border-radius: 14px;
  box-shadow: 0 18px 36px -16px rgb(16 24 40 / 30%);
}

.ais__item-menu button {
  height: 32px;
  padding: 0 10px;
  color: var(--ais-text);
  font-size: 12.5px;
  border-radius: 8px;
}

.ais__empty {
  padding: 28px 12px;
  text-align: center;
}

.ais__empty-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  margin: 0 auto 12px;
  color: #fff;
  font-size: 17px;
  line-height: 1;
  background-image: var(--ais-gradient);
  border-radius: 13px;
  box-shadow:
    0 8px 18px -12px rgb(37 99 235 / 70%),
    inset 0 1px 0 rgb(255 255 255 / 26%);
}

.ais__empty-title {
  margin: 0;
  color: var(--ais-text);
  font-size: 14.5px;
  font-weight: 600;
  line-height: 1.5;
}

.ais__empty-sub {
  margin: 4px 0 0;
  color: var(--ais-text-3);
  font-size: 12.5px;
  line-height: 1.6;
}

.ais__side-foot {
  flex-shrink: 0;
  padding: 10px 18px 14px;
  color: var(--ais-text-3);
  font-size: 11.5px;
  line-height: 1.6;
  border-top: 1px solid var(--ais-border);
}

.ais__side-foot::before {
  display: inline-block;
  width: 5px;
  height: 5px;
  margin-right: 6px;
  vertical-align: middle;
  content: '';
  background-color: var(--color-primary);
  border-radius: 50%;
}

/* ---------- 右栏：工作区 ---------- */
.ais__main {
  position: relative;
  z-index: 1;
  background:
    radial-gradient(820px 460px at 86% -10%, rgb(37 99 235 / 8%), transparent 62%),
    radial-gradient(680px 420px at 2% -8%, rgb(99 102 241 / 6%), transparent 64%),
    linear-gradient(180deg, #fdfeff 0%, #fbfcff 46%, #fff 100%);
}

.ais__scroll {
  scrollbar-width: thin;
  scrollbar-color: #cdd5e3 transparent;
}

.ais__scroll::-webkit-scrollbar {
  width: 10px;
}

.ais__scroll::-webkit-scrollbar-track {
  background: transparent;
}

.ais__scroll::-webkit-scrollbar-thumb {
  background-color: #d3dae7;
  background-clip: content-box;
  border: 3px solid transparent;
  border-radius: 999px;
}

.ais__scroll::-webkit-scrollbar-thumb:hover {
  background-color: #b9c4d8;
}

.ais__inner {
  width: 100%;
  max-width: 960px;
  padding: 36px 32px 56px;
  margin: 0 auto;
}

/* ---------- 页头 ---------- */
.ais__page-head {
  display: flex;
  flex-wrap: wrap;
  gap: 18px 24px;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 22px;
}

.ais__page-head-main {
  display: flex;
  gap: 14px;
  align-items: center;
  min-width: 0;
}

.ais__page-icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  color: #fff;
  background-image: var(--ais-gradient);
  border-radius: 14px;
  box-shadow:
    0 12px 24px -18px rgb(37 99 235 / 80%),
    inset 0 1px 0 rgb(255 255 255 / 26%);
}

.ais__page-head-copy {
  min-width: 0;
}

.ais__page-title {
  margin: 0;
  color: var(--ais-text);
  font-size: clamp(24px, 2.2vw, 30px);
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: -0.025em;
}

.ais__page-sub {
  margin: 6px 0 0;
  color: var(--ais-text-2);
  font-size: 13.5px;
  line-height: 1.6;
}

.ais__page-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  margin-left: auto;
}

.ais__meta-pill {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  max-width: 240px;
  padding: 5px 11px;
  overflow: hidden;
  color: var(--ais-text-2);
  font-size: 12px;
  line-height: 1.5;
  white-space: nowrap;
  text-overflow: ellipsis;
  background-color: rgb(255 255 255 / 74%);
  border: 1px solid var(--ais-border);
  border-radius: 999px;
  box-shadow: var(--ais-shadow-1);
}

.ais__meta-pill::before {
  flex-shrink: 0;
  width: 5px;
  height: 5px;
  content: '';
  background-color: #22c55e;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgb(34 197 94 / 10%);
}

.ais__meta-pill--primary {
  color: #1d4ed8;
  background-color: rgb(37 99 235 / 7%);
  border-color: rgb(37 99 235 / 12%);
}

.ais__meta-pill--primary::before {
  background-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgb(37 99 235 / 10%);
}

/* ---------- 提示条 ---------- */
.ais__banner {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
  padding: 14px 16px;
  margin-bottom: 16px;
  color: var(--ais-text);
  font-size: 13px;
  line-height: 1.7;
  background-color: rgb(37 99 235 / 5%);
  border: 1px solid rgb(37 99 235 / 12%);
  border-radius: 14px;
}

.ais__banner p {
  min-width: 0;
  margin: 0;
}

.ais__banner strong {
  color: #1d4ed8;
  font-weight: 600;
}

.ais__banner-icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: var(--color-primary);
  background-color: #fff;
  border: 1px solid rgb(37 99 235 / 12%);
  border-radius: 9px;
  box-shadow: var(--ais-shadow-1);
}

.ais__banner-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  justify-content: flex-end;
}

.ais__banner--warn {
  grid-template-columns: auto minmax(0, 1fr);
  background-color: rgb(217 119 6 / 5%);
  border-color: rgb(217 119 6 / 18%);
}

.ais__banner--warn .ais__banner-icon {
  color: #b45309;
  border-color: rgb(217 119 6 / 18%);
}

.ais__banner--warn p {
  color: #92400e;
}

/* ---------- 表单卡 ---------- */
.ais__form {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 24px 28px 0;
  overflow: hidden;
  background-color: var(--ais-surface);
  background-image: radial-gradient(520px 280px at 100% 0%, rgb(37 99 235 / 4.5%), transparent 72%);
  border: 1px solid var(--ais-border);
  border-radius: var(--ais-radius-card);
  box-shadow:
    var(--ais-shadow-1),
    var(--ais-shadow-2);
  backdrop-filter: blur(8px);
}

.ais__form-head {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 18px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--ais-border);
}

.ais__form-title {
  margin: 0;
  color: var(--ais-text);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: -0.015em;
}

.ais__presets {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.ais__presets-label {
  margin-right: 2px;
  color: var(--ais-text-3);
  font-size: 12px;
  line-height: 1.5;
}

.ais__preset {
  height: 30px;
  padding: 0 11px;
  color: var(--ais-text-2);
  font-family: inherit;
  font-size: 12px;
  line-height: 30px;
  background-color: #f7f9fd;
  border: 1px solid var(--ais-border);
  border-radius: 999px;
  cursor: pointer;
  transition:
    color var(--motion-fast) var(--motion-ease),
    background-color var(--motion-fast) var(--motion-ease),
    border-color var(--motion-fast) var(--motion-ease);
}

.ais__preset:hover {
  color: var(--color-primary);
  background-color: #fff;
  border-color: rgb(37 99 235 / 26%);
}

/* ---------- 表单分区 ---------- */
.ais__fieldset {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-width: 0;
  padding: 0;
  margin: 0;
  border: none;
}

.ais__group {
  padding: 0;
  background: none;
  border: none;
  border-radius: 0;
}

.ais__group + .ais__group {
  padding-top: 24px;
  margin-top: 24px;
  border-top: 1px solid var(--ais-border);
}

.ais__group-title {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 0;
  color: var(--ais-text);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
  letter-spacing: -0.01em;
}

.ais__group-title::before {
  flex-shrink: 0;
  width: 4px;
  height: 15px;
  content: '';
  background-image: var(--ais-gradient);
  border-radius: 999px;
}

.ais__group-desc {
  margin: 5px 0 18px;
  color: var(--ais-text-3);
  font-size: 12.5px;
  line-height: 1.6;
}

.ais__fields {
  display: grid;
  gap: 18px 20px;
}

.ais__fields--split {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 20px;
}

.ais__fields--row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 20px;
}

.ais__field--wide {
  grid-column: 1 / -1;
}

.ais__field {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.ais__label {
  color: var(--ais-text-2);
  font-size: 12.5px;
  font-weight: 500;
  line-height: 1.5;
}

.ais__label .ais__req {
  margin-left: 2px;
}

.ais__field input:not([type="checkbox"]) {
  width: 100%;
  height: 46px;
  padding: 0 13px;
  color: var(--ais-text);
  font-family: inherit;
  font-size: 14.5px;
  background-color: #fbfcfe;
  border: 1px solid var(--ais-border-strong);
  border-radius: 10px;
  outline: none;
  transition:
    background-color var(--motion-fast) var(--motion-ease),
    border-color var(--motion-fast) var(--motion-ease),
    box-shadow var(--motion-fast) var(--motion-ease);
}

.ais__field input:not([type="checkbox"]):hover {
  background-color: #fff;
  border-color: #c9d1e0;
}

.ais__field input:not([type="checkbox"]):focus {
  background-color: #fff;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgb(37 99 235 / 12%);
}

.ais__field input:not([type="checkbox"])::placeholder {
  color: #8a93a3;
}

.ais__field.is-err input:not([type="checkbox"]) {
  border-color: var(--color-danger);
}

.ais__field.is-err input:not([type="checkbox"]):focus {
  box-shadow: 0 0 0 3px rgb(220 38 38 / 12%);
}

.ais__hint {
  display: block;
  color: var(--ais-text-3);
  font-size: 12px;
  line-height: 1.6;
}

.ais__hint--warn {
  color: #b45309;
}

.ais__hint--err {
  color: var(--color-danger);
}

.ais__hint code {
  padding: 0 4px;
  font-family: var(--font-family-mono);
  background-color: rgb(15 23 42 / 6%);
  border-radius: 4px;
}

.ais__model-chip {
  height: 24px;
  padding: 0 9px;
  margin-left: 4px;
  color: var(--color-primary);
  font-family: inherit;
  font-size: 11.5px;
  line-height: 24px;
  background-color: rgb(37 99 235 / 8%);
  border: 1px solid rgb(37 99 235 / 10%);
  border-radius: 999px;
  cursor: pointer;
  transition: background-color var(--motion-fast) var(--motion-ease);
}

.ais__model-chip:hover {
  background-color: rgb(37 99 235 / 14%);
}

/* ---------- 操作条 ---------- */
.ais__actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px 28px 20px;
  margin: 28px -28px 0;
  background-color: #f8fafe;
  border-top: 1px solid var(--ais-border);
}

.ais__actionbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding-top: 0;
  border-top: none;
}

.ais__actionbar-left,
.ais__actionbar-right {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.ais__actionbar-right {
  margin-left: auto;
}

.ais__actionbar :deep(.el-button) {
  min-height: 38px;
  padding: 0 18px;
  font-size: 13.5px;
  border-radius: 10px;
}

.ais__actionbar :deep(.el-button + .el-button) {
  margin-left: 0;
}

.ais__actionbar-right :deep(.el-button--primary) {
  color: #fff;
  background-color: var(--color-primary);
  background-image: var(--ais-gradient);
  border-color: transparent;
  box-shadow: 0 10px 20px -12px rgb(37 99 235 / 75%);
}

.ais__actionbar-right :deep(.el-button--primary:hover) {
  background-image: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 55%, #4338ca 100%);
}

.ais__actionbar-right :deep(.el-button--primary.is-disabled) {
  color: #a4acba;
  background-color: #eef1f7;
  background-image: none;
  border-color: transparent;
  box-shadow: none;
}

.ais__test-result {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 4px 9px;
  font-size: 11.5px;
  line-height: 1.5;
  border-radius: 999px;
}

.ais__test-result::before {
  flex-shrink: 0;
  width: 6px;
  height: 6px;
  content: '';
  background-color: currentcolor;
  border-radius: 50%;
}

.ais__test-result.is-ok {
  color: #15803d;
  background-color: rgb(22 163 74 / 8%);
}

.ais__test-result.is-fail {
  color: var(--color-danger);
  background-color: rgb(220 38 38 / 7%);
}

.ais__error {
  padding: 10px 12px;
  margin: 0;
  color: #b42318;
  font-size: 12.5px;
  line-height: 1.6;
  background-color: rgb(220 38 38 / 6%);
  border: 1px solid rgb(220 38 38 / 10%);
  border-radius: 10px;
}

/* ---------- 响应式 ---------- */
@media (max-width: 1023px) {
  .ais {
    height: auto;
    min-height: calc(100dvh - var(--header-height));
    flex-direction: column;
    overflow: visible;
  }

  .ais__side {
    position: relative;
    z-index: 2;
    flex: 0 0 auto;
    width: 100%;
    background: linear-gradient(180deg, #f8faff 0%, #fbfcff 100%);
    border-right: none;
    border-bottom: 1px solid var(--ais-border);
  }

  .ais__side-head {
    padding: 16px 20px 4px;
  }

  .ais__side-body {
    flex: 0 0 auto;
    padding: 0 20px 16px;
    overflow: visible;
  }

  .ais__list-head {
    padding: 8px 0;
  }

  .ais__cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
    gap: 8px;
  }

  /* 平板 / 手机没有稳定的 hover，把「更多」常显，避免菜单不可发现 */
  .ais__item-more {
    opacity: 1;
  }

  .ais__side-foot {
    padding: 0 20px 14px;
    border-top: none;
  }

  .ais__main {
    z-index: 1;
  }

  .ais__inner {
    max-width: 960px;
    padding: 24px 20px 44px;
  }

  .ais__page-head {
    align-items: flex-start;
  }

  .ais__page-meta {
    margin-left: 0;
  }
}

@media (max-width: 767px) {
  .ais {
    min-height: calc(100dvh - 56px);
  }

  .ais__side-head {
    padding: 12px 16px 2px;
  }

  .ais__side-body {
    padding: 0 16px 12px;
  }

  .ais__cards {
    grid-template-columns: 1fr;
  }

  .ais__side-foot {
    padding: 0 16px 12px;
  }

  .ais__empty {
    padding: 18px 12px;
  }

  .ais__empty-mark {
    width: 36px;
    height: 36px;
    margin-bottom: 8px;
    font-size: 15px;
    border-radius: 11px;
  }

  .ais__inner {
    padding: 18px 16px 36px;
  }

  .ais__page-head {
    flex-direction: column;
    gap: 14px;
    align-items: flex-start;
    margin-bottom: 18px;
  }

  .ais__page-icon {
    width: 42px;
    height: 42px;
    border-radius: 12px;
  }

  .ais__page-title {
    font-size: 24px;
  }

  .ais__page-sub {
    font-size: 13px;
  }

  .ais__page-meta {
    justify-content: flex-start;
  }

  .ais__banner {
    grid-template-columns: auto minmax(0, 1fr);
    gap: 10px;
    padding: 12px;
  }

  .ais__banner-actions {
    grid-column: 1 / -1;
    justify-content: flex-start;
    margin-top: 2px;
  }

  .ais__form {
    padding: 18px 16px 0;
    border-radius: 16px;
  }

  .ais__form-head {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
    padding-bottom: 14px;
    margin-bottom: 18px;
  }

  .ais__form-title {
    font-size: 16.5px;
  }

  .ais__presets {
    width: 100%;
  }

  .ais__fields,
  .ais__fields--split,
  .ais__fields--row {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .ais__field--wide {
    grid-column: auto;
  }

  .ais__group + .ais__group {
    padding-top: 20px;
    margin-top: 20px;
  }

  .ais__actions {
    padding: 16px;
    margin: 22px -16px 0;
  }

  .ais__actionbar {
    flex-direction: column;
    align-items: stretch;
  }

  .ais__actionbar-left,
  .ais__actionbar-right {
    width: 100%;
    margin-left: 0;
  }

  .ais__actionbar-left :deep(.el-button),
  .ais__actionbar-right :deep(.el-button) {
    flex: 1;
  }

  .ais__test-result {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ais__item,
  .ais__preset,
  .ais__item-more {
    transition: none;
  }
}

</style>
