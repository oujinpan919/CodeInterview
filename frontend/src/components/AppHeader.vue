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

      <RouterLink to="/" class="app-header__brand">码上记</RouterLink>

      <nav v-if="!isMobile" class="app-header__nav" aria-label="主导航">
        <RouterLink
          v-for="item in desktopNavItems"
          :key="item.path"
          :to="item.path"
          class="app-header__nav-link"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="app-header__actions">
        <RouterLink v-if="!isLogin" to="/login" class="app-header__login">登录</RouterLink>
        <el-dropdown v-else trigger="click" @command="handleCommand">
          <button class="app-header__avatar" type="button" :aria-label="avatarLabel">
            {{ avatarText }}
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

    <el-drawer
      v-model="drawerVisible"
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

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isMobile = useMediaQuery('(max-width: 767px)')
const isSmallLaptop = useMediaQuery('(min-width: 1024px) and (max-width: 1279px)')

const drawerVisible = ref(false)

const isLogin = computed(() => userStore.isLogin)
const isAdmin = computed(() => userStore.isAdmin)
const user = computed(() => userStore.user)
const avatarText = computed(() => {
  const nickname = user.value && user.value.nickname ? user.value.nickname : ''
  return nickname ? nickname.slice(0, 1).toUpperCase() : '我'
})
const avatarLabel = computed(() => `${user.value ? user.value.nickname : '用户'} 的账号菜单`)

const baseNavItems = [
  { label: '首页', path: '/' },
  { label: '题库', path: '/question' },
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
  background-color: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
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
  margin-left: auto;
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
  color: var(--color-bg-card);
  font-size: var(--font-size-body);
  font-weight: 600;
  background-color: var(--color-primary);
  border: 0;
  border-radius: 50%;
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
    gap: var(--space-3);
    height: 56px;
    padding: 0 var(--space-4);
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
