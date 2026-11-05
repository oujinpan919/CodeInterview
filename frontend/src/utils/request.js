import axios from 'axios'
import { ElMessage } from 'element-plus'
import 'element-plus/theme-chalk/el-message.css'
import { useUserStore } from '@/stores/user'

/**
 * 统一 axios 封装（02 §4.5 + 04 B5.3）。
 * - baseURL 取 VITE_API_BASE，默认 /api/v1；默认 timeout 15s，单请求可传 { timeout } 覆盖
 *   （批量导入等长耗时接口由调用方传 60000，实例默认值不放大）。
 * - 非 0 code 抛 { code, message }，调用方自行提示；未被捕获时由 unhandledrejection 兜底提示一次。
 * - 401/40100/40102 清登录态并跳 /login?redirect=当前路由；
 *   40300 跳 /403；管理端 40301 只提示不跳。
 */

const BASE_URL = import.meta.env.VITE_API_BASE || '/api/v1'
const DEFAULT_TIMEOUT = 15000
const NETWORK_ERROR_MESSAGE = '网络异常，请稍后重试'

// 04 B5.3 前端兜底文案（后端 message 优先）
const FALLBACK_MESSAGES = {
  40100: '登录已过期，请重新登录',
  40300: '没有权限执行该操作',
  40301: '需要管理员权限',
  40400: '内容不存在或已下架',
  40900: '数据已存在，请检查后重试',
  40901: '该数据被引用，无法删除',
  42900: '操作太频繁，请稍后再试',
  42901: '今日额度已用完，请明天再试',
  42902: '使用人数较多，请稍后再试',
  42903: '尝试次数过多，请 10 分钟后再试',
  50000: '服务异常，请稍后重试',
  50201: 'AI 服务暂不可用',
  50202: 'AI 响应超时，请重试'
}

// 路由侧注册跳转回调，避免 request ↔ router ↔ stores 的模块循环依赖
let navHandlers = {}

export function registerNavHandlers(handlers) {
  navHandlers = handlers || {}
}

const request = axios.create({
  baseURL: BASE_URL,
  timeout: DEFAULT_TIMEOUT,
  headers: { 'Content-Type': 'application/json;charset=utf-8' }
})

function createRequestError(code, message, config) {
  const error = new Error(message)
  error.name = 'RequestError'
  error.code = code
  error.config = config
  error.__isRequestError = true
  return error
}

function fallbackMessage(code) {
  return FALLBACK_MESSAGES[code] || '请求失败，请稍后重试'
}

function notifyError(message) {
  ElMessage.error(message)
}

function resetLoginState() {
  useUserStore().reset()
}

function isAdminRequest(config) {
  const url = config && config.url ? config.url : ''
  return url.includes('/admin/')
}

function isRegisterRequest(config) {
  const url = config && config.url ? config.url : ''
  return url.includes('/auth/register')
}

function handleBusinessError(payload, config) {
  const code = payload && payload.code
  const message = (payload && payload.message) || fallbackMessage(code)
  const error = createRequestError(code, message, config)

  if (code === 40100 || code === 40102) {
    // 登录页上的 40102（账号被禁用）由页面内联提示，不做二次跳转
    resetLoginState()
    if (navHandlers.onUnauthorized) navHandlers.onUnauthorized()
  } else if (code === 40300) {
    // 注册关闭（04 B4.5）：注册页卡片内原地提示并隐藏表单，不弹 toast、不跳 /403
    if (isRegisterRequest(config)) {
      return error
    }
    error.__notified = true
    notifyError(message)
    if (navHandlers.onForbidden) navHandlers.onForbidden()
  } else if (code === 40301) {
    if (isAdminRequest(config)) {
      // 管理端接口的 40301 只提示不跳转（04 B5.3 / 02 §4.5）
      error.__notified = true
      notifyError(message)
    } else if (navHandlers.onForbidden) {
      navHandlers.onForbidden()
    }
  }
  return error
}

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.satoken = userStore.token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload === 'object' && payload.code === 0) {
      return payload.data
    }
    if (payload && typeof payload === 'object' && typeof payload.code === 'number') {
      return Promise.reject(handleBusinessError(payload, response.config))
    }
    return payload
  },
  (error) => {
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }
    const config = error.config
    const response = error.response
    if (!response) {
      // 断网 / 超时 / CORS 等：统一文案，交给调用方或全局兜底提示
      return Promise.reject(createRequestError(0, NETWORK_ERROR_MESSAGE, config))
    }
    const payload = response.data
    if (payload && typeof payload === 'object' && typeof payload.code === 'number') {
      return Promise.reject(handleBusinessError(payload, config))
    }
    if (response.status === 401) {
      resetLoginState()
      if (navHandlers.onUnauthorized) navHandlers.onUnauthorized()
    } else if (response.status === 403) {
      if (navHandlers.onForbidden) navHandlers.onForbidden()
    }
    return Promise.reject(createRequestError(response.status, fallbackMessage(response.status), config))
  }
)

// 未被调用方 catch 的请求错误，统一提示一次（避免各页面重复弹窗）
if (typeof window !== 'undefined') {
  window.addEventListener('unhandledrejection', (event) => {
    const reason = event.reason
    if (!reason || !reason.__isRequestError || reason.__notified) return
    reason.__notified = true
    notifyError(reason.message)
    event.preventDefault()
  })
}

export default request
