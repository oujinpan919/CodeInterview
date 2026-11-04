<template>
  <section class="status-page">
    <p class="status-page__code" aria-hidden="true">403</p>
    <h1 class="status-page__title">无权限访问</h1>
    <p class="status-page__desc">抱歉，你没有权限访问该页面。</p>
    <div class="status-page__actions">
      <el-button type="primary" @click="router.push('/')">回首页</el-button>
      <el-button @click="handleSwitchAccount">切换账号</el-button>
    </div>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

async function handleSwitchAccount() {
  if (userStore.isLogin) {
    await userStore.logout()
  }
  router.replace('/login')
}
</script>

<style scoped>
.status-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - var(--header-height) - 160px);
  padding: var(--space-10) var(--space-4);
  text-align: center;
}

.status-page__code {
  color: var(--color-primary);
  font-family: var(--font-family-mono);
  font-size: 88px;
  font-weight: 700;
  line-height: 1;
}

.status-page__title {
  margin-top: var(--space-4);
}

.status-page__desc {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.status-page__actions {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.status-page__actions :deep(.el-button) {
  min-height: 44px;
  padding: 0 var(--space-5);
  border-radius: var(--radius-button);
}

@media (max-width: 767px) {
  .status-page__code {
    font-size: 64px;
  }

  .status-page__actions {
    width: 100%;
    flex-direction: column;
  }

  .status-page__actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
