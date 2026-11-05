import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { registerNavHandlers } from '@/utils/request'

// 04 B1 路由表（14 个页面 + 管理端父级重定向 + 404 catch-all）
const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页', requiresAuth: false, requiresAdmin: false }
  },
  {
    path: '/question',
    name: 'question-list',
    component: () => import('@/views/QuestionList.vue'),
    meta: { title: '题库', requiresAuth: false, requiresAdmin: false }
  },
  {
    path: '/question/:id',
    name: 'question-detail',
    component: () => import('@/views/QuestionDetail.vue'),
    meta: { title: '题目详情', requiresAuth: false, requiresAdmin: false }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false, requiresAdmin: false }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', requiresAuth: false, requiresAdmin: false }
  },
  {
    path: '/favorites',
    name: 'favorites',
    component: () => import('@/views/Favorites.vue'),
    meta: { title: '我的收藏', requiresAuth: true, requiresAdmin: false }
  },
  {
    path: '/wrong-book',
    name: 'wrong-book',
    component: () => import('@/views/WrongBook.vue'),
    meta: { title: '错题本', requiresAuth: true, requiresAdmin: false }
  },
  {
    path: '/records',
    name: 'records',
    component: () => import('@/views/Records.vue'),
    meta: { title: '刷题记录', requiresAuth: true, requiresAdmin: false }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人中心', requiresAuth: true, requiresAdmin: false }
  },
  {
    path: '/admin',
    redirect: '/admin/questions',
    meta: { title: '管理端', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/questions',
    name: 'admin-questions',
    component: () => import('@/views/admin/AdminQuestions.vue'),
    meta: { title: '题目管理', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/questions/edit',
    name: 'admin-question-edit',
    component: () => import('@/views/admin/AdminQuestionEdit.vue'),
    meta: { title: '题目编辑', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/taxonomy',
    name: 'admin-taxonomy',
    component: () => import('@/views/admin/AdminTaxonomy.vue'),
    meta: { title: '分类与标签', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/Forbidden.vue'),
    meta: { title: '无权限', requiresAuth: false, requiresAdmin: false }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在', requiresAuth: false, requiresAdmin: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

let redirectingToLogin = false

registerNavHandlers({
  onUnauthorized() {
    const current = router.currentRoute.value
    if (redirectingToLogin || current.path === '/login' || current.path === '/register') return
    redirectingToLogin = true
    router
      .replace({ path: '/login', query: { redirect: current.fullPath } })
      .finally(() => {
        redirectingToLogin = false
      })
  },
  onForbidden() {
    if (router.currentRoute.value.path !== '/403') {
      router.push('/403')
    }
  }
})

// 全局守卫（02 §4.6 五步，顺序固定）
router.beforeEach(async (to) => {
  // 1) 标题
  document.title = to.meta.title ? `${to.meta.title} · 码上记` : '码上记'

  const userStore = useUserStore()

  // 2) 需要登录但没有 token
  if (to.meta.requiresAuth && !userStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 3) 有 token 但内存里没有 user：用 fetchMe 恢复登录态
  //    只有 40100/40102（token 失效/账号禁用）才清态跳登录；网络错误、429/500 等
  //    不得强制登出（否则读接口一时限流就会把用户踢下线），保留 token 由页面展示错误态。
  if (userStore.token && !userStore.user) {
    try {
      await userStore.fetchMe()
    } catch (error) {
      const authFailed = error && (error.code === 40100 || error.code === 40102)
      if (authFailed) {
        userStore.reset()
        if (to.path === '/login' || to.path === '/register') return true
        return { path: '/login', query: { redirect: to.fullPath } }
      }
    }
  }

  // 4) 管理员路由
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return { path: '/403' }
  }

  // 5) 已登录访问登录 / 注册页
  if (userStore.isLogin && (to.path === '/login' || to.path === '/register')) {
    return { path: '/' }
  }

  return true
})

export default router
