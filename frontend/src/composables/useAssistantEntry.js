import { ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message-box.css'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 助手入口统一逻辑（08 §4.1 / D8）。
 *
 * v3.20 起助手是**独立页面**，因此这里只负责「未登录拦截 + 跳路由」，
 * 不再展开任何浮层。详情页的「问助手」也走这里，避免重复实现（见 08 §11.3）。
 */
export function useAssistantEntry() {
  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()

  /**
   * 打开助手页。
   * @param {object} [options]
   * @param {string|number} [options.questionId] 带入的题目 id（题干由服务端查库拼装）
   */
  async function openAssistant(options = {}) {
    const { questionId } = options
    if (!userStore.isLogin) {
      try {
        await ElMessageBox.confirm('登录后才能使用 AI 助手，去登录？', '需要登录', {
          type: 'info',
          confirmButtonText: '去登录',
          cancelButtonText: '取消'
        })
      } catch {
        return false
      }
      router.push({ path: '/login', query: { redirect: route.fullPath } })
      return false
    }
    const query = questionId ? { questionId: String(questionId) } : {}
    router.push({ path: '/assistant', query })
    return true
  }

  return { openAssistant }
}
