<template>
  <footer class="mk-footer">
    <div class="mk-container">
      <div class="mk-footer__top">
        <div class="mk-footer__brand">
          <RouterLink to="/" class="mk-footer__logo">
            <img
              src="/favicon.svg"
              alt=""
              class="mk-footer__icon"
              width="28"
              height="28"
              aria-hidden="true"
            />
            <span>码上记</span>
          </RouterLink>
          <p class="mk-footer__slogan">把面试题刷成体系。</p>
        </div>

        <nav class="mk-footer__nav" aria-label="营销页页脚导航">
          <div v-for="group in FOOTER_GROUPS" :key="group.title" class="mk-footer__group">
            <h2>{{ group.title }}</h2>
            <RouterLink v-for="link in group.links" :key="link.to" :to="link.to">
              {{ link.label }}
            </RouterLink>
          </div>
        </nav>
      </div>

      <div class="mk-footer__bottom">
        <p>© {{ currentYear }} 码上记 · 自建题库，持续更新</p>
        <p>本站为面试练习项目，题目与解析仅供学习参考。</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
const currentYear = new Date().getFullYear()

const FOOTER_GROUPS = [
  {
    title: '练习',
    links: [
      { label: '题库', to: '/question' },
      { label: '刷题路线', to: '/paths' },
      { label: '错题本', to: '/wrong-book' },
      { label: 'AI 助手', to: '/assistant' }
    ]
  },
  {
    title: '账号',
    links: [
      { label: '登录', to: '/login' },
      { label: '注册', to: '/register' },
      { label: '个人中心', to: '/profile' }
    ]
  }
]
</script>

<style scoped>
.mk-footer {
  padding: 64px 0 28px;
  background-color: #f7f9fc;
  border-top: 1px solid var(--mk-line);
}

.mk-footer__top {
  display: flex;
  gap: 48px;
  align-items: flex-start;
  justify-content: space-between;
}

.mk-footer__brand {
  max-width: 320px;
}

.mk-footer__logo {
  display: inline-flex;
  gap: 9px;
  align-items: center;
  color: var(--mk-blue);
  font-size: 17px;
  font-weight: 700;
  text-decoration: none;
}

.mk-footer__icon {
  display: block;
  flex-shrink: 0;
  width: 28px;
  height: 28px;
}

.mk-footer__slogan {
  margin: 12px 0 0;
  color: var(--mk-muted);
  font-size: 13px;
  line-height: 1.7;
}

.mk-footer__nav {
  display: flex;
  gap: 72px;
}

.mk-footer__group h2 {
  margin: 0 0 12px;
  color: var(--mk-faint);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.5;
  letter-spacing: 0.1em;
}

.mk-footer__group a {
  display: block;
  margin-bottom: 9px;
  color: var(--mk-muted);
  font-size: 13px;
  line-height: 1.5;
  text-decoration: none;
  transition: color 0.18s ease;
}

.mk-footer__group a:hover {
  color: var(--mk-blue);
}

.mk-footer__bottom {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  align-items: center;
  justify-content: space-between;
  padding-top: 28px;
  margin-top: 48px;
  color: var(--mk-faint);
  font-size: 11.5px;
  line-height: 1.6;
  border-top: 1px solid var(--mk-line);
}

.mk-footer__bottom p {
  margin: 0;
}

@media (max-width: 767px) {
  .mk-footer {
    padding: 48px 0 24px;
  }

  .mk-footer__top {
    flex-direction: column;
    gap: 32px;
  }

  .mk-footer__nav {
    gap: 48px;
  }

  .mk-footer__bottom {
    flex-direction: column;
    align-items: flex-start;
    padding-top: 20px;
    margin-top: 32px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mk-footer__group a {
    transition: none;
  }
}
</style>
