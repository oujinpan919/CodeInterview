<template>
  <div class="default-layout">
    <AppHeader v-if="!isAuthPage" />

    <main
      class="default-layout__main"
      :class="{ 'default-layout__main--auth': isAuthPage }"
    >
      <slot />
    </main>

    <footer v-if="!isAuthPage" class="app-footer">
      <div class="app-footer__inner">
        <p>© 码上记 · AI 生成内容仅供参考 · 题目仅学习用途</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()

// 登录 / 注册按 04 B4.4「整屏」要求隐藏 Header / Footer
const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')
</script>

<style scoped>
.default-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
}

.default-layout__main {
  flex: 1 0 auto;
  width: 100%;
  max-width: var(--layout-max-width);
  min-height: var(--content-min-height);
  margin: 0 auto;
  padding: var(--space-6);
}

.default-layout__main--auth {
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: none;
  min-height: 100vh;
  min-height: 100dvh;
  padding: var(--space-6) var(--space-4);
  background-color: var(--color-bg-page);
}

.app-footer {
  flex-shrink: 0;
  background-color: var(--color-bg-card);
  border-top: 1px solid var(--color-border);
}

.app-footer__inner {
  max-width: var(--layout-max-width);
  margin: 0 auto;
  padding: var(--space-4) var(--space-6);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  text-align: center;
}

@media (max-width: 767px) {
  .default-layout__main {
    min-height: calc(100dvh - 56px - 56px);
    padding: var(--space-4);
  }

  .default-layout__main--auth {
    min-height: 100dvh;
    padding: var(--space-4);
  }

  .app-footer__inner {
    padding: var(--space-3) var(--space-4);
  }
}
</style>
