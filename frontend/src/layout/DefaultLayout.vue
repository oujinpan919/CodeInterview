<template>
  <div class="default-layout">
    <AppHeader v-if="!isAuthPage && !isMarketingPage" />
    <!-- v3.20 硬约束-2/D8：助手改为独立页面（/assistant），不再有全局浮层与快捷入口 -->

    <main
      class="default-layout__main"
      :class="{
        'default-layout__main--auth': isAuthPage,
        'default-layout__main--full': isFullBleed,
        'default-layout__main--marketing': isMarketingPage
      }"
    >
      <slot />
    </main>

    <footer v-if="!isAuthPage && !isFullBleed && !isMarketingPage" class="app-footer">
      <div class="app-footer__inner">
        <div class="app-footer__top">
          <div class="app-footer__brand">
            <RouterLink to="/home" class="app-footer__logo">码上记</RouterLink>
            <p class="app-footer__slogan">面试题库 · 三档自评 · 错题复习</p>
          </div>

          <nav class="app-footer__nav" aria-label="页脚导航">
            <div v-for="group in footerGroups" :key="group.title" class="app-footer__group">
              <h2 class="app-footer__group-title">{{ group.title }}</h2>
              <ul class="app-footer__list">
                <li v-for="link in group.links" :key="link.to">
                  <RouterLink :to="link.to" class="app-footer__link">{{ link.label }}</RouterLink>
                </li>
              </ul>
            </div>
          </nav>
        </div>

        <div class="app-footer__bottom">
          <p class="app-footer__copy">© {{ currentYear }} 码上记</p>
          <p class="app-footer__meta">自建题库 · 持续更新</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()

// 登录 / 注册按 04 B4.4「整屏」要求隐藏 Header / Footer
const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')

/**
 * 营销首页使用独立头尾，不套应用导航与页脚。
 * 通过路由 meta.marketing 控制，登录态分流由全局守卫负责。
 */
const isMarketingPage = computed(() => Boolean(route.meta && route.meta.marketing))

/**
 * v3.22：应用态页面（目前仅 /assistant）取消 1200px 限宽与内边距、隐藏页脚，
 * 由页面自己接管整块视口；全局顶栏保留，站点导航不丢。
 * 通过路由 meta.fullBleed 控制，可随时逐路由回滚。
 */
const isFullBleed = computed(() => Boolean(route.meta && route.meta.fullBleed))

/**
 * v4.11：页脚从单行版权改为「品牌 + 分组导航 + 底栏」。
 * 顶栏只放 6 个高频入口，刷题记录 / 个人中心 / 管理端没有位置，
 * 正好由页脚承接；不重复顶栏已有的「首页 / 题库 / 路线」等主入口。
 */
const footerGroups = [
  {
    title: '练习',
    links: [
      { label: '题库', to: '/question' },
      { label: '刷题路线', to: '/paths' },
      { label: '错题本', to: '/wrong-book' }
    ]
  },
  {
    title: '我的',
    links: [
      { label: '我的收藏', to: '/favorites' },
      { label: '刷题记录', to: '/records' },
      { label: '个人中心', to: '/profile' }
    ]
  },
  {
    title: '工具',
    links: [{ label: 'AI 助手', to: '/assistant' }]
  }
]

const currentYear = new Date().getFullYear()
</script>

<style scoped>
.default-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
}

/*
 * v4.11：去掉 min-height: var(--content-min-height)。
 * 那个变量按旧的单行页脚写死 --footer-height: 80px，而新页脚约 210px，
 * 少算的 130px 会让总高超出视口，把页脚挤到首屏之外。
 * 撑满视口交给外层 flex + 这里的 flex: 1 0 auto 就够了，不需要预设高度。
 */
.default-layout__main {
  flex: 1 0 auto;
  width: 100%;
  max-width: var(--layout-max-width);
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

/* v3.22 应用态：取消限宽与内边距，页面自行接管视口（/assistant） */
.default-layout__main--full {
  max-width: none;
  padding: 0;
  background-color: var(--color-bg-card);
}

/* 营销首页：完全交给 Landing 控制宽度、背景与头尾 */
.default-layout__main--marketing {
  max-width: none;
  padding: 0;
  background-color: #fff;
}

.app-footer {
  flex-shrink: 0;
  background-color: var(--color-bg-card);
  border-top: 1px solid var(--color-border);
}

.app-footer__inner {
  max-width: var(--layout-max-width);
  margin: 0 auto;
  padding: var(--space-8) var(--space-6) var(--space-5);
}

/*
 * 品牌占 2/5，3 组导航平分剩下的 3/5——四块内容均匀铺满整行。
 * 不用 space-between + 固定宽品牌区，那样中间会空出几百像素。
 */
.app-footer__top {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 3fr);
  gap: var(--space-12);
  padding-bottom: var(--space-6);
}

/* ---------- 品牌区 ---------- */
.app-footer__brand {
  min-width: 0;
}

/* 与顶栏 .app-header__brand 同一视觉语言（主色 + 加粗），字号降一档，
   页脚是次要区域不应压过顶栏；顶栏用纯文字，这里也不加图标块。 */
.app-footer__logo {
  display: inline-block;
  color: var(--color-primary);
  font-size: var(--font-size-body);
  font-weight: 700;
  line-height: var(--line-height-body);
  letter-spacing: 0.02em;
  text-decoration: none;
}

.app-footer__slogan {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

/* ---------- 分组导航 ---------- */
.app-footer__nav {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-6);
}

.app-footer__group {
  min-width: 0;
}

.app-footer__group-title {
  margin-bottom: var(--space-3);
  color: var(--color-text);
  font-size: var(--font-size-aux);
  font-weight: 600;
  line-height: var(--line-height-aux);
  letter-spacing: 0.02em;
}

.app-footer__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin: 0;
  padding: 0;
  list-style: none;
}

.app-footer__link {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  text-decoration: none;
  transition: color var(--motion-fast) var(--motion-ease);
}

.app-footer__link:hover {
  color: var(--color-primary);
}

/* ---------- 底栏 ---------- */
.app-footer__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

.app-footer__copy,
.app-footer__meta {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

@media (max-width: 767px) {
  /* 同 v4.11：不预设 min-height，靠外层 flex 撑满，避免按页脚固定高度算错 */
  .default-layout__main {
    padding: var(--space-4);
  }

  .default-layout__main--auth {
    min-height: 100dvh;
    padding: var(--space-4);
  }

  /* v3.22 应用态在移动端同样取消内边距，页面自己撑满视口 */
  .default-layout__main--full {
    padding: 0;
  }

  .app-footer__inner {
    padding: var(--space-6) var(--space-4) var(--space-4);
  }

  .app-footer__top {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-6);
    padding-bottom: var(--space-5);
  }

  /* 移动端 3 组并排会挤到 100px 以内，缩小列间距 */
  .app-footer__nav {
    gap: var(--space-4);
  }

  .app-footer__bottom {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-1);
  }
}

/* ---------- md：768–1023px ---------- */
@media (min-width: 768px) and (max-width: 1023px) {
  .app-footer__top {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-6);
  }
}
</style>
