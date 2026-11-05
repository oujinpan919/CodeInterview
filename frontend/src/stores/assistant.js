import { computed, reactive, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { getQuota } from '@/api/ai'
import { streamChat } from '@/utils/sse'
import { FALLBACK_MESSAGES } from '@/utils/request'
import { useUserStore } from '@/stores/user'

/**
 * 助手会话状态（02 §4.7 store 契约 / 05 §6.3 状态机）：
 * idle → connecting → streaming → done
 *                ↘（流前错误）idle + 提示    ↘ error → failed（保留文本）
 * streaming → stop() → aborted（保留文本）
 *
 * 契约字段：visible / messages / streaming / questionId / quota / openWithQuestion(id) / send / stop / reset。
 * 会话只存在内存中（刷新即清空）；服务端不持久化消息正文。
 * 额度来自 GET /ai/quota，done.quota 回来后局部更新。
 */

const MAX_MESSAGES = 10
const MAX_CHARS = 6000
const MAX_SINGLE_CHARS = 2000
const MIN_INTERVAL_MS = 3000

let seq = 0

function nextId(prefix) {
  seq += 1
  return `${prefix}-${Date.now()}-${seq}`
}

function newSessionId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
    const random = (Math.random() * 16) | 0
    const value = char === 'x' ? random : (random & 0x3) | 0x8
    return value.toString(16)
  })
}

export const useAssistantStore = defineStore('assistant', () => {
  const userStore = useUserStore()

  const visible = ref(false)
  const state = ref('idle')
  const messages = ref([])
  const questionId = ref(null)
  const questionTitle = ref('')
  const quota = ref(null)
  const notice = ref('')
  const sessionId = ref(newSessionId())

  let controller = null
  let lastSentAt = 0

  const question = computed(() =>
    questionId.value ? { id: questionId.value, title: questionTitle.value } : null
  )
  const streaming = computed(() => state.value === 'connecting' || state.value === 'streaming')
  const quotaExhausted = computed(
    () => Boolean(quota.value) && (quota.value.remaining ?? 1) <= 0
  )

  function reset() {
    visible.value = false
    messages.value = []
    state.value = 'idle'
    questionId.value = null
    questionTitle.value = ''
    quota.value = null
    notice.value = ''
    sessionId.value = newSessionId()
    controller = null
    lastSentAt = 0
  }

  function openPanel() {
    visible.value = true
    if (userStore.isLogin) loadQuota()
  }

  function closePanel() {
    if (streaming.value) stop()
    visible.value = false
  }

  function togglePanel() {
    if (visible.value) closePanel()
    else openPanel()
  }

  /** 详情页「问助手」：带入题目 id + 标题（题干由服务端按 questionId 查库拼进 prompt）。 */
  function openWithQuestion(id, title) {
    if (id === undefined || id === null) return
    questionId.value = String(id)
    questionTitle.value = title || '当前题目'
    openPanel()
  }

  function clearQuestion() {
    questionId.value = null
    questionTitle.value = ''
  }

  async function loadQuota() {
    try {
      quota.value = await getQuota()
    } catch {
      // 额度条拿不到不阻塞对话；发送失败时后端仍会给 42901
    }
  }

  function applyQuota(payload) {
    if (!payload) return
    const current = quota.value || {}
    const limit = payload.limit ?? current.dailyLimit ?? 30
    const used = payload.used ?? 0
    quota.value = {
      ...current,
      dailyLimit: limit,
      used,
      remaining: Math.max(0, limit - used)
    }
  }

  /** 05 §6.3：每次请求把 messages 裁到最近 10 条 / 6000 字符，当前用户消息必须保留。 */
  function buildContext() {
    const kept = messages.value
      .filter(
        (msg) =>
          msg.role === 'user' || (msg.role === 'assistant' && msg.status === 'done' && msg.content)
      )
      .map((msg) => ({ role: msg.role, content: msg.content }))

    let trimmed = kept.slice(Math.max(0, kept.length - MAX_MESSAGES))
    const totalChars = () => trimmed.reduce((sum, msg) => sum + msg.content.length, 0)
    while (trimmed.length > 1 && totalChars() > MAX_CHARS) {
      trimmed = trimmed.slice(1)
    }
    return trimmed
  }

  /** 错误码 → 文案：与 04 B5.3 共用同一张表（request.js 导出），不再维护第二份。 */
  function messageForError(error) {
    if (!error) return '请求失败，请稍后重试'
    return FALLBACK_MESSAGES[error.code] || error.message || '请求失败，请稍后重试'
  }

  async function send(rawText, { bypassInterval = false } = {}) {
    notice.value = ''
    const text = String(rawText || '').trim()
    if (!text) return false
    if (text.length > MAX_SINGLE_CHARS) {
      notice.value = `单次提问不超过 ${MAX_SINGLE_CHARS} 字`
      return false
    }
    if (streaming.value) return false
    if (!bypassInterval && Date.now() - lastSentAt < MIN_INTERVAL_MS) {
      notice.value = '请稍后再试（两次提问间隔 3 秒）'
      return false
    }
    if (quotaExhausted.value) {
      notice.value = '今日额度已用完，明天 00:00 重置'
      return false
    }

    const userMessage = { id: nextId('u'), role: 'user', content: text, status: 'done' }
    const assistantMessage = reactive({
      id: nextId('a'),
      role: 'assistant',
      content: '',
      status: 'connecting'
    })
    messages.value.push(userMessage, assistantMessage)
    state.value = 'connecting'
    lastSentAt = Date.now()
    controller = new AbortController()

    let receivedMeta = false
    let receivedDone = false
    const payload = { messages: buildContext(), questionId: questionId.value }

    try {
      await streamChat({
        payload,
        token: userStore.token,
        sessionId: sessionId.value,
        signal: controller.signal,
        onEvent(event) {
          if (!event) return
          const { event: name, data } = event
          if (name === 'meta') {
            receivedMeta = true
            state.value = 'streaming'
            assistantMessage.status = 'streaming'
          } else if (name === 'delta') {
            if (data && typeof data.content === 'string' && data.content) {
              assistantMessage.content += data.content
              assistantMessage.status = 'streaming'
              state.value = 'streaming'
            }
          } else if (name === 'done') {
            receivedDone = true
            assistantMessage.status = 'done'
            state.value = 'done'
            if (data && data.quota) applyQuota(data.quota)
          } else if (name === 'error') {
            receivedMeta = true
            assistantMessage.status = 'failed'
            state.value = 'failed'
            notice.value = messageForError(data)
          }
        }
      })
      if (!receivedDone && state.value === 'streaming') {
        // 连接被上游结束但没有 done/error：保留已收文本，标「未完成」
        assistantMessage.status = 'failed'
        state.value = 'failed'
        notice.value = '回答中断，请重试'
      }
    } catch (error) {
      if (error && error.name === 'AbortError') {
        assistantMessage.status = 'aborted'
        state.value = 'aborted'
        loadQuota()
        return true
      }
      if (!receivedMeta) {
        // 流前错误：不产生新气泡，只给提示（05 §4.3 / §6.3）
        messages.value = messages.value.filter((msg) => msg.id !== assistantMessage.id)
        state.value = 'idle'
        notice.value = messageForError(error)
        if (error && error.code === 42901) {
          quota.value = { ...(quota.value || {}), remaining: 0 }
        }
        return false
      }
      assistantMessage.status = 'failed'
      state.value = 'failed'
      notice.value = messageForError(error)
    } finally {
      controller = null
    }
    return true
  }

  function stop() {
    if (controller) controller.abort()
  }

  /** 重新生成：取最后一条用户消息，移除其后的助手气泡后重发。 */
  function regenerate() {
    if (streaming.value) return
    const lastUser = [...messages.value].reverse().find((msg) => msg.role === 'user')
    if (!lastUser) return
    const lastUserIndex = messages.value.lastIndexOf(lastUser)
    messages.value = messages.value.slice(0, lastUserIndex + 1)
    lastSentAt = 0
    send(lastUser.content, { bypassInterval: true })
  }

  watch(
    () => userStore.isLogin,
    (loggedIn) => {
      if (!loggedIn) reset()
    }
  )

  return {
    visible,
    state,
    messages,
    questionId,
    question,
    streaming,
    quota,
    notice,
    quotaExhausted,
    openPanel,
    closePanel,
    togglePanel,
    openWithQuestion,
    clearQuestion,
    loadQuota,
    send,
    stop,
    regenerate,
    reset
  }
})
