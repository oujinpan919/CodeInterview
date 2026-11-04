<template>
  <div class="placeholder">
    <div class="placeholder__card">
      <p class="placeholder__eyebrow">CodeInterview</p>
      <h1 class="placeholder__title">码上记</h1>
      <p class="placeholder__subtitle">面试题库 · 三档自评 · 错题复习</p>
      <p class="placeholder__tip">前端脚手架已就绪，页面开发中。</p>
    </div>
  </div>
</template>

<style scoped>
/* T01 占位页：T04 会替换为 DefaultLayout + RouterView */
.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  min-height: 100dvh;
  padding: var(--space-6);
  background-color: var(--color-bg-page);
}

.placeholder__card {
  width: 100%;
  max-width: 480px;
  padding: var(--space-6);
  text-align: center;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.placeholder__eyebrow {
  margin-bottom: var(--space-2);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  color: var(--color-text-secondary);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.placeholder__title {
  font-size: var(--font-size-h1);
  line-height: var(--line-height-h1);
  color: var(--color-primary);
}

.placeholder__subtitle {
  margin-top: var(--space-3);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  color: var(--color-text);
}

.placeholder__tip {
  margin-top: var(--space-5);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  color: var(--color-text-secondary);
}

@media (max-width: 767px) {
  .placeholder {
    padding: var(--layout-gutter);
  }

  .placeholder__card {
    padding: var(--space-5) var(--space-4);
    border-radius: var(--radius-card);
  }
}
</style>
