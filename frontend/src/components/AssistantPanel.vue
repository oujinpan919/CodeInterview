<template>
  <div v-if="!isAuthPage" class="assistant">
    <button
      v-if="!store.open"
      type="button"
      class="assistant__launcher"
      aria-label="打开 AI 助手"
      @click="handleLauncher"
    >
      AI 助手
    </button>

    <section v-else class="assistant__panel" role="dialog" aria-label="AI 助手">
      <header class="assistant__head">
        <div class="assistant__title">
          <strong>AI 助手</strong>
          <span v-if="userStore.isLogin" class="assistant__quota">
            今日剩余 {{ quotaRemaining }}/{{ quotaLimit }}
          </span>
        </div>
        <button type="button" class="assistant__icon-btn" aria-label="关闭助手" @click="store.closePanel()">
          ✕
        </button>
      </header>

      <div v-if="store.question" class="assistant__context">
        <span class="assistant__context-text">已带入：{{ store.question.title }}</span>
        <button
          type="button"
          class="assistant__context-clear"
          aria-label="取消带入题目"
          @click="store.clearQuestion()"
        >
          ×
        </button>
      </div>

      <div ref="listRef" class="assistant__messages" @scroll="onScroll">
        <div v-if="store.messages.length === 0" class="assistant__empty">
          <p class="assistant__empty-title">试试这样问：</p>
          <button
            v-for="example in EXAMPLES"
            :key="example"
            type="button"
            class="assistant__example"
            @click="fillExample(example)"
          >
            {{ example }}
          </button>
        </div>

        <div
          v-for="message in store.messages"
          :key="message.id"
          class="assistant__msg"
          :class="`assistant__msg--${message.role}`"
        >
          <template v-if="message.role === 'user'">
            <div class="assistant__bubble assistant__bubble--user">{{ message.content }}</div>
          </template>
          <template v-else>
            <div class="assistant__bubble assistant__bubble--assistant">
              <MarkdownView
                v-if="message.status === 'done'"
                :content="message.content"
                empty-text="（空回答）"
              />
              <template v-else>
                <span class="assistant__stream">{{ message.content }}</span>
                <span
                  v-if="message.status === 'connecting' || message.status === 'streaming'"
                  class="assistant__cursor"
                  aria-hidden="true"
                  >▍</span
                >
              </template>
              <div v-if="message.status === 'aborted'" class="assistant__state">已停止</div>
              <div v-else-if="message.status === 'failed'" class="assistant__state assistant__state--failed">
                未完成
              </div>
            </div>
            <button
              v-if="message.status === 'failed' || message.status === 'aborted'"
              type="button"
              class="assistant__retry"
              @click="store.regenerate()"
            >
              重新生成
            </button>
          </template>
        </div>
      </div>

      <p v-if="store.notice" class="assistant__notice" role="status">{{ store.notice }}</p>

      <div class="assistant__input">
        <textarea
          ref="inputRef"
          v-model="draft"
          class="assistant__textarea"
          rows="1"
          maxlength="2000"
          :disabled="store.quotaExhausted"
          :placeholder="inputPlaceholder"
          aria-label="输入问题"
          @input="resizeInput"
          @keydown.enter.exact.prevent="handleSend"
        ></textarea>
        <div class="assistant__input-foot">
          <span class="assistant__count">{{ draft.length }}/2000</span>
          <button
            v-if="store.isStreaming"
            type="button"
            class="assistant__send assistant__send--stop"
            @click="store.stop()"
          >
            停止
          </button>
          <button
            v-else
            type="button"
            class="assistant__send"
            :disabled="sendDisabled"
            @click="handleSend"
          >
            发送
          </button>
        </div>
        <p v-if="store.quotaExhausted" class="assistant__exhausted">
          今日额度已用完，明天 00:00 重置
        </p>
      </div>

      <p class="assistant__footnote">AI 生成，仅供参考</p>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message-box.css'
import { useAssistantStore } from '@/stores/assistant'
import { useUserStore } from '@/stores/user'
import MarkdownView from '@/components/MarkdownView.vue'

const EXAMPLES = [
  'HashMap 的扩容机制是什么？',
  'MySQL 为什么用 B+ 树做索引？',
  'TCP 三次握手为什么不能是两次？'
]

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const store = useAssistantStore()

const draft = ref('')
const listRef = ref(null)
const inputRef = ref(null)
const stickToBottom = ref(true)

const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')
const quotaRemaining = computed(() =>
  store.quota && Number.isFinite(Number(store.quota.remaining)) ? store.quota.remaining : '—'
)
const quotaLimit = computed(() =>
  store.quota && Number.isFinite(Number(store.quota.dailyLimit)) ? store.quota.dailyLimit : '—'
)
const inputPlaceholder = computed(() =>
  store.quotaExhausted ? '今日额度已用完' : '问问 AI 助手…（Enter 发送，Shift+Enter 换行）'
)
const sendDisabled = computed(
  () => draft.value.trim().length === 0 || store.isStreaming || store.quotaExhausted
)

async function handleLauncher() {
  if (!userStore.isLogin) {
    try {
      await ElMessageBox.confirm('登录后才能使用 AI 助手，去登录？', '需要登录', {
        type: 'info',
        confirmButtonText: '去登录',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  store.openPanel()
}

function fillExample(text) {
  draft.value = text
  nextTick(() => {
    inputRef.value && inputRef.value.focus()
    resizeInput()
  })
}

function resizeInput() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 96)}px`
}

async function handleSend() {
  if (sendDisabled.value) return
  const text = draft.value
  draft.value = ''
  const sent = await store.send(text)
  if (!sent) draft.value = text
  nextTick(() => {
    resizeInput()
    if (inputRef.value) inputRef.value.focus()
  })
}

function onScroll() {
  const el = listRef.value
  if (!el) return
  stickToBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < 40
}

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

watch(
  () => store.messages.map((message) => `${message.id}:${message.content.length}`).join('|'),
  () => {
    if (stickToBottom.value) scrollToBottom()
  }
)

watch(
  () => store.open,
  async (open) => {
    if (!open) return
    stickToBottom.value = true
    await scrollToBottom()
    await nextTick()
    if (inputRef.value) inputRef.value.focus()
    resizeInput()
  }
)
</script>

<style scoped>
.assistant {
  position: fixed;
  right: var(--space-6);
  bottom: var(--space-6);
  z-index: var(--z-assistant);
}

.assistant__launcher {
  min-height: 48px;
  padding: 0 var(--space-5);
  color: var(--color-bg-card);
  background-color: var(--color-primary);
  border: none;
  border-radius: 999px;
  font-size: var(--font-size-body);
  font-weight: 600;
  box-shadow: 0 8px 24px rgba(var(--color-primary-rgb), 0.28);
}

.assistant__launcher:hover {
  background-color: var(--color-primary-hover);
}

.assistant__panel {
  display: flex;
  flex-direction: column;
  width: 380px;
  height: 520px;
  overflow: hidden;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: 0 16px 48px rgba(var(--color-text-rgb), 0.18);
}

.assistant__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  flex-shrink: 0;
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.assistant__title {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-2);
  font-size: var(--font-size-body);
}

.assistant__quota {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  font-weight: 400;
}

.assistant__icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  margin: -10px -10px -10px 0;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  border-radius: var(--radius-button);
  font-size: 16px;
}

.assistant__icon-btn:hover {
  color: var(--color-text);
  background-color: var(--color-bg-page);
}

.assistant__context {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
  padding: var(--space-2) var(--space-4);
  background-color: rgba(var(--color-primary-rgb), 0.08);
  border-bottom: 1px solid var(--color-border);
}

.assistant__context-text {
  flex: 1;
  overflow: hidden;
  color: var(--color-primary-hover);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.assistant__context-clear {
  width: 44px;
  height: 44px;
  margin: -12px -12px -12px 0;
  color: var(--color-primary-hover);
  background: transparent;
  border: none;
  font-size: 18px;
}

.assistant__messages {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: var(--space-3);
  min-height: 0;
  padding: var(--space-4);
  overflow-y: auto;
  background-color: var(--color-bg-page);
}

.assistant__empty {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.assistant__empty-title {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

.assistant__example {
  padding: var(--space-3);
  color: var(--color-text);
  text-align: left;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-button);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.assistant__example:hover {
  border-color: var(--color-primary);
  color: var(--color-primary-hover);
}

.assistant__msg {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  max-width: 100%;
}

.assistant__msg--user {
  align-items: flex-end;
}

.assistant__msg--assistant {
  align-items: flex-start;
}

.assistant__bubble {
  max-width: 88%;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-card);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  overflow-wrap: anywhere;
}

.assistant__bubble--user {
  color: var(--color-bg-card);
  background-color: var(--color-primary);
  border-bottom-right-radius: var(--radius-tag);
  white-space: pre-wrap;
}

.assistant__bubble--assistant {
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-bottom-left-radius: var(--radius-tag);
}

.assistant__stream {
  white-space: pre-wrap;
  word-break: break-word;
}

.assistant__cursor {
  color: var(--color-primary);
  animation: assistant-blink 1s steps(2, start) infinite;
}

@keyframes assistant-blink {
  to {
    opacity: 0;
  }
}

.assistant__state {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

.assistant__state--failed {
  color: var(--color-danger);
}

.assistant__retry {
  padding: var(--space-1) var(--space-2);
  color: var(--color-primary);
  background: transparent;
  border: none;
  font-size: var(--font-size-aux);
}

.assistant__notice {
  flex-shrink: 0;
  margin: 0 var(--space-3) var(--space-2);
  padding: var(--space-2) var(--space-3);
  color: var(--color-danger);
  background-color: rgba(var(--color-danger-rgb), 0.08);
  border-radius: var(--radius-button);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.assistant__input {
  flex-shrink: 0;
  padding: var(--space-3) var(--space-3) 0;
  border-top: 1px solid var(--color-border);
}

.assistant__textarea {
  width: 100%;
  min-height: 44px;
  max-height: 96px;
  padding: var(--space-2) var(--space-3);
  color: var(--color-text);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-button);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  resize: none;
}

.assistant__textarea:focus {
  border-color: var(--color-primary);
  outline: none;
}

.assistant__input-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-2) 0;
}

.assistant__count {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

.assistant__send {
  min-width: 72px;
  min-height: 44px;
  padding: 0 var(--space-4);
  color: var(--color-bg-card);
  background-color: var(--color-primary);
  border: none;
  border-radius: var(--radius-button);
  font-size: var(--font-size-body);
}

.assistant__send:hover:not(:disabled) {
  background-color: var(--color-primary-hover);
}

.assistant__send:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.assistant__send--stop {
  background-color: var(--color-danger);
}

.assistant__exhausted {
  padding-bottom: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.assistant__footnote {
  flex-shrink: 0;
  padding: var(--space-1) 0 var(--space-2);
  color: var(--color-text-disabled);
  font-size: var(--font-size-aux);
  text-align: center;
}

@media (max-width: 767px) {
  .assistant {
    right: var(--space-4);
    bottom: calc(var(--space-4) + env(safe-area-inset-bottom));
  }

  .assistant__panel {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 100dvh;
    border: none;
    border-radius: 0;
    padding-bottom: env(safe-area-inset-bottom);
  }

  .assistant__messages {
    padding: var(--space-3);
  }

  .assistant__bubble {
    max-width: 92%;
  }
}
</style>
