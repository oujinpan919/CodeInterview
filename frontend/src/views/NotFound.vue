<template>
  <section class="status-page">
    <p class="status-page__code" aria-hidden="true">404</p>
    <h1 class="status-page__title">页面不存在</h1>
    <p class="status-page__desc">你访问的页面不存在，或者已经被移除。</p>
    <div class="status-page__actions">
      <el-button type="primary" @click="router.push('/')">回首页</el-button>
      <el-button @click="router.push('/question')">去题库</el-button>
    </div>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
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
