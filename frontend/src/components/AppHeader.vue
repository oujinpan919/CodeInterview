<template>
  <header class="app-header">
    <div class="app-header__inner">
      <button
        v-if="isMobile"
        class="app-header__menu"
        type="button"
        aria-label="打开导航菜单"
        @click="drawerVisible = true"
      >
        <span class="app-header__menu-icon" aria-hidden="true">☰</span>
      </button>

      <RouterLink to="/home" class="app-header__brand">码上记</RouterLink>

      <nav v-if="!isMobile" class="app-header__nav" aria-label="主导航">
        <RouterLink
          v-for="item in desktopNavItems"
          :key="item.path"
          :to="item.path"
          class="app-header__nav-link"
        >
          {{ item.label }}
        </RouterLink>
        <!-- v3.20 硬约束-2：AI 助手是独立页面入口（RouterLink），不再是浮层开关 -->
        <RouterLink to="/assistant" class="app-header__nav-link app-header__ai" aria-label="AI 助手">
          <span class="app-header__ai-mark" aria-hidden="true">✦</span>
          <span class="app-header__ai-label">AI 助手</span>
          <!-- D9：后台继续生成时给出可见状态，点击即回到对话页 -->
          <span
            v-if="assistant.streaming"
            class="app-header__ai-dot"
            title="回答生成中"
            aria-label="回答生成中"
          ></span>
        </RouterLink>
      </nav>

      <!-- v3.7：全局搜索入口（mianshiya 常驻搜索框的启发；≥1280 显示输入框，更窄只留图标）
           v3.14：所有页面（含题库页）都显示，且与登录/头像一起包进右侧组，保证右对齐不随搜索显隐漂移 -->
      <div class="app-header__right">
        <form
          class="app-header__search"
          :class="{ 'app-header__search--compact': !isWide }"
          role="search"
          @submit.prevent="submitSearch"
        >
          <el-input
            v-if="isWide"
            v-model="searchKeyword"
            class="app-header__search-input"
            placeholder="搜索题目"
            maxlength="30"
            clearable
            size="default"
          >
            <template #prefix>
              <!-- v4.0：原来用 emoji 🔍，彩色字形和全站线性图标语言不一致，改内联 SVG -->
              <span class="app-header__search-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" focusable="false">
                  <circle cx="10.8" cy="10.8" r="6.4" stroke="currentColor" stroke-width="1.7" />
                  <path
                    d="M15.6 15.6 20 20"
                    stroke="currentColor"
                    stroke-width="1.7"
                    stroke-linecap="round"
                  />
                </svg>
              </span>
            </template>
          </el-input>
          <button
            v-else
            class="app-header__search-button"
            type="button"
            aria-label="搜索题目"
            @click="submitSearch"
          >
            <svg
              viewBox="0 0 24 24"
              width="18"
              height="18"
              fill="none"
              aria-hidden="true"
              focusable="false"
            >
              <circle cx="10.8" cy="10.8" r="6.4" stroke="currentColor" stroke-width="1.7" />
              <path d="M15.6 15.6 20 20" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
            </svg>
          </button>
        </form>

        <div class="app-header__actions">
          <RouterLink v-if="!isLogin" to="/login" class="app-header__login">登录</RouterLink>
          <el-dropdown v-else trigger="click" @command="handleCommand">
            <button class="app-header__avatar" type="button" :aria-label="avatarLabel">
              <!-- v4.12：有头像显示图片，没有则回退首字母 -->
              <img v-if="avatarUrl" class="app-header__avatar-img" :src="avatarUrl" alt="" />
              <template v-else>{{ avatarText }}</template>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item v-if="showRecordsInDropdown" command="records">
                  刷题记录
                </el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin">管理端</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      append-to-body
      direction="ltr"
      size="78%"
      title="导航"
      class="app-header__drawer"
    >
      <nav class="app-header__drawer-nav" aria-label="移动端导航">
        <RouterLink
          v-for="item in drawerNavItems"
          :key="item.path"
          :to="item.path"
          class="app-header__drawer-link"
          @click="drawerVisible = false"
        >
          {{ item.label }}
        </RouterLink>
        <!-- v3.20 硬约束-1：取消抽屉式助手入口 -->
      </nav>
      <div class="app-header__drawer-actions">
        <RouterLink
          v-if="!isLogin"
          to="/login"
          class="app-header__drawer-login"
          @click="drawerVisible = false"
        >
          登录
        </RouterLink>
        <button v-else class="app-header__drawer-logout" type="button" @click="handleLogout">
          退出登录
        </button>
      </div>
    </el-drawer>
  </header>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message-box.css'
import { useMediaQuery } from '@vueuse/core'
import { useUserStore } from '@/stores/user'
import { useAssistantStore } from '@/stores/assistant'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const assistant = useAssistantStore()

const isMobile = useMediaQuery('(max-width: 767px)')
/**
 * 768–1279：导航项较多，把「刷题记录」收进头像下拉，给「AI 助手」留位置。
 * （Header 有 backdrop-filter，抽屉必须 append-to-body，否则会被约束成 56px 高）
 */
const isSmallLaptop = useMediaQuery('(min-width: 768px) and (max-width: 1279px)')
/** v3.7：≥1280 才展开搜索输入框，更窄的窗口只留搜索图标（避免导航被挤换行） */
const isWide = useMediaQuery('(min-width: 1280px)')

const drawerVisible = ref(false)
const searchKeyword = ref('')

const isLogin = computed(() => userStore.isLogin)
const isAdmin = computed(() => userStore.isAdmin)
const user = computed(() => userStore.user)
const avatarText = computed(() => {
  const nickname = user.value && user.value.nickname ? user.value.nickname : ''
  return nickname ? nickname.slice(0, 1).toUpperCase() : '我'
})
/** v4.12 顶栏头像：有 OSS 图就显示图，否则回退首字母色块 */
const avatarUrl = computed(() => (user.value && user.value.avatar) || '')
const avatarLabel = computed(() => `${user.value ? user.value.nickname : '用户'} 的账号菜单`)

const baseNavItems = [
  { label: '首页', path: '/home' },
  { label: '题库', path: '/question' },
  { label: '路线', path: '/paths' },
  { label: '我的收藏', path: '/favorites' },
  { label: '错题本', path: '/wrong-book' },
  { label: '刷题记录', path: '/records' }
]

// 1024–1279 时「刷题记录」收进头像下拉（04 B2）
const desktopNavItems = computed(() =>
  isSmallLaptop.value ? baseNavItems.filter((item) => item.path !== '/records') : baseNavItems
)
const showRecordsInDropdown = computed(() => isSmallLaptop.value && isLogin.value)

const drawerNavItems = computed(() => {
  const items = [...baseNavItems]
  if (isLogin.value) {
    items.push({ label: '个人中心', path: '/profile' })
  }
  if (isAdmin.value) {
    items.push({ label: '管理端', path: '/admin/questions' })
  }
  // v3.20 硬约束-1：移动端导航项直接跳独立页（不是抽屉式展示助手本体）
  items.push({ label: 'AI 助手', path: '/assistant' })
  return items
})

watch(
  () => route.fullPath,
  () => {
    drawerVisible.value = false
  }
)

function handleCommand(command) {
  if (command === 'profile') router.push('/profile')
  else if (command === 'records') router.push('/records')
  else if (command === 'admin') router.push('/admin/questions')
  else if (command === 'logout') handleLogout()
}

/** v3.20 硬约束-1：抽屉式助手入口已移除，移动端通过导航项跳转独立页。 */

/**
 * v3.7：全局搜索。与题库页一致支持 `#123` / 纯数字直达题目；
 * 空关键词直接进题库页，不写无意义的 ?keyword=。
 */
function submitSearch() {
  const raw = searchKeyword.value.trim()
  const idMatch = raw.match(/^#?(\d{1,7})$/)
  if (idMatch) {
    router.push({ name: 'question-detail', params: { id: idMatch[1] } })
  } else {
    router.push({ path: '/question', query: raw ? { keyword: raw } : {} })
  }
  searchKeyword.value = ''
  drawerVisible.value = false
}

async function handleLogout() {
  drawerVisible.value = false
  try {
    await ElMessageBox.confirm('退出后需要重新登录，确定退出吗？', '退出登录', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/')
}
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: var(--z-header);
  background-color: var(--header-bg, rgba(255, 255, 255, 0.78));
  border-bottom: 1px solid var(--color-border);
  backdrop-filter: blur(var(--header-blur, 10px));
  -webkit-backdrop-filter: blur(var(--header-blur, 10px));
}

.app-header__inner {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  width: 100%;
  max-width: var(--layout-max-width);
  height: var(--header-height);
  margin: 0 auto;
  padding: 0 var(--space-6);
}

.app-header__menu {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  margin-left: calc(-1 * var(--space-2));
  color: var(--color-text);
  background: transparent;
  border: 0;
  border-radius: var(--radius-button);
}

.app-header__menu-icon {
  font-size: 20px;
  line-height: 1;
}

.app-header__brand {
  color: var(--color-primary);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.app-header__nav {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  height: 100%;
}

.app-header__nav-link {
  display: inline-flex;
  align-items: center;
  height: 40px;
  padding: 0 var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  border-radius: var(--radius-button);
  transition: color 0.15s ease, background-color 0.15s ease;
}

/* 导航里的 AI 助手：和普通导航项同尺寸，但用主色图标点出「助手」身份 */
.app-header__ai {
  gap: var(--space-1);
  background: transparent;
  border: 0;
  cursor: pointer;
  font-family: inherit;
}

.app-header__ai-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  color: var(--color-on-primary);
  /* 与 /assistant 页内的 .ap-mark 同一枚品牌标，跨页保持同一个「身份」 */
  background-image: linear-gradient(140deg, #5b8bff 0%, #2563eb 55%, #1d4ed8 100%);
  border-radius: 8px;
  box-shadow:
    0 2px 6px -2px rgb(37 99 235 / 45%),
    inset 0 1px 0 rgb(255 255 255 / 26%);
  font-size: 11px;
  line-height: 1;
}

.app-header__ai.is-active {
  color: var(--color-primary);
  font-weight: 600;
  background-color: var(--color-bg-page);
}

/* D9：后台继续生成时的状态点，点击导航项即回到 /assistant */
.app-header__ai-dot {
  width: 7px;
  height: 7px;
  margin-left: 2px;
  background-color: var(--color-primary);
  border-radius: 50%;
  animation: app-header-ai-pulse 1.2s ease-in-out infinite;
}

@keyframes app-header-ai-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(0.8);
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-header__ai-dot {
    animation: none;
  }
}

.app-header__nav-link:hover {
  color: var(--color-primary);
  background-color: var(--color-bg-page);
}

.app-header__nav-link.router-link-active {
  color: var(--color-primary);
  font-weight: 600;
}

.app-header__actions {
  display: flex;
  align-items: center;
}

/* v3.7：全局搜索（桌面输入框 / 窄屏图标按钮） */
.app-header__right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-left: auto;
}

.app-header__search {
  display: flex;
  align-items: center;
}

.app-header__search-input {
  width: 264px;
}

.app-header__search-input :deep(.el-input__wrapper) {
  height: 36px;
  padding: 0 12px;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  box-shadow: none;
  transition:
    border-color var(--motion-fast) var(--motion-ease),
    box-shadow var(--motion-fast) var(--motion-ease);
}

.app-header__search-input :deep(.el-input__wrapper:hover) {
  border-color: #d3d7de;
}

.app-header__search-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgb(37 99 235 / 10%);
}

.app-header__search-input :deep(.el-input__inner) {
  font-size: 13.5px;
}

.app-header__search-icon {
  display: inline-flex;
  align-items: center;
  color: var(--color-text-secondary);
}

.app-header__search-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: var(--color-text-secondary);
  background: transparent;
  border: 0;
  border-radius: 10px;
  cursor: pointer;
  transition:
    color var(--motion-fast) var(--motion-ease),
    background-color var(--motion-fast) var(--motion-ease);
}

.app-header__search-button:hover {
  color: var(--color-text);
  background-color: var(--color-bg-page);
}

.app-header__login {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 var(--space-4);
  color: var(--color-primary);
  font-size: var(--font-size-body);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-button);
  transition: background-color 0.15s ease, color 0.15s ease;
}

.app-header__login:hover {
  color: var(--color-bg-card);
  background-color: var(--color-primary);
}

.app-header__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  overflow: hidden;
  color: var(--color-bg-card);
  font-size: var(--font-size-body);
  font-weight: 600;
  background-color: var(--color-primary);
  border: 0;
  border-radius: 50%;
  cursor: pointer;
}

.app-header__avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.app-header__drawer-nav {
  display: flex;
  flex-direction: column;
}

.app-header__drawer-link {
  display: flex;
  align-items: center;
  min-height: 44px;
  color: var(--color-text);
  font-size: var(--font-size-body);
  border-bottom: 1px solid var(--color-border);
}

.app-header__drawer-link.router-link-active {
  color: var(--color-primary);
  font-weight: 600;
}

/* v3.20 硬约束-1：「AI 助手」在抽屉里是普通 RouterLink，不再有专门的按钮样式 */

.app-header__drawer-actions {
  margin-top: var(--space-6);
}

.app-header__drawer-login,
.app-header__drawer-logout {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 44px;
  color: var(--color-primary);
  font-size: var(--font-size-body);
  background-color: transparent;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-button);
}

.app-header__drawer-logout {
  color: var(--color-danger);
  border-color: var(--color-danger);
}

@media (max-width: 767px) {
  .app-header__inner {
    gap: var(--space-2);
    height: 56px;
    padding: 0 var(--space-4);
  }

  /* v3.14：窄屏右侧组只留搜索图标 + 登录/头像，间距收紧避免挤压品牌名 */
  .app-header__right {
    gap: var(--space-1);
  }

  .app-header__brand {
    font-size: 18px;
  }

  .app-header__login {
    min-height: 44px;
  }

  .app-header__avatar {
    width: 44px;
    height: 44px;
  }

  .app-header__brand {
    display: inline-flex;
    align-items: center;
    min-height: 44px;
  }
}
</style>
