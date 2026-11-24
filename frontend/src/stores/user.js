import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getMe, login as loginApi, logout as logoutApi } from '@/api/auth'

/**
 * 登录态（02 §4.7）：
 * - token 放内存并镜像到 sessionStorage（key ci_token）：刷新保持、关闭标签页失效；
 * - user 不持久化，刷新后由路由守卫调 fetchMe() 恢复；
 * - 不使用 localStorage / Cookie。
 */

const TOKEN_KEY = 'ci_token'

export const useUserStore = defineStore('user', () => {
  const token = ref(sessionStorage.getItem(TOKEN_KEY) || '')
  const user = ref(null)

  const isLogin = computed(() => Boolean(token.value))
  const isAdmin = computed(() => user.value !== null && user.value.role === 1)

  function setToken(value) {
    token.value = value || ''
    if (token.value) {
      sessionStorage.setItem(TOKEN_KEY, token.value)
    } else {
      sessionStorage.removeItem(TOKEN_KEY)
    }
  }

  /** 只清本地登录态，不发请求（401 拦截器、退出登录、改密/改名后都用它） */
  function reset() {
    setToken('')
    user.value = null
    // assistant store 持有上一账号的消息/会话/模型选择，必须一起清；
    // 用动态 import 避免 user ↔ assistant 的静态循环依赖。
    import('@/stores/assistant')
      .then(({ useAssistantStore }) => {
        try {
          useAssistantStore().reset()
        } catch {
          // Pinia 尚未初始化时忽略
        }
      })
      .catch(() => {})
  }

  async function login(nickname, password) {
    const data = await loginApi({ nickname, password })
    setToken(data.token)
    user.value = data.user
    return data
  }

  async function logout() {
    try {
      if (token.value) {
        await logoutApi()
      }
    } catch {
      // 本地登出优先：接口失败（如 token 已过期）也必须清态
    } finally {
      reset()
    }
  }

  async function fetchMe() {
    const data = await getMe()
    user.value = data
    return data
  }

  /**
   * 合并式更新用户信息。
   * v4.12 修正：原来是直接替换，只传 { avatar } 会把 nickname/role 一起冲掉，
   * 导致头像更新后昵称变空、管理菜单消失。改为合并语义。
   */
  function setProfile(profile) {
    if (!profile) {
      user.value = null
      return
    }
    user.value = user.value ? { ...user.value, ...profile } : { ...profile }
  }

  return {
    token,
    user,
    isLogin,
    isAdmin,
    login,
    logout,
    fetchMe,
    setProfile,
    reset,
    setToken
  }
})
