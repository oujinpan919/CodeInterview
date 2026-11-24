<template>
  <header class="mk-header" :class="{ 'is-scrolled': scrolled }">
    <div class="mk-header__inner">
      <a href="#top" class="mk-brand" @click="closeMenu">
        <img
          src="/favicon.svg"
          alt=""
          class="mk-brand__icon"
          width="30"
          height="30"
          aria-hidden="true"
        />
        <span class="mk-brand__name">码上记</span>
      </a>

      <nav class="mk-nav" aria-label="页面导航">
        <a v-for="item in NAV_ITEMS" :key="item.href" :href="item.href" @click="closeMenu">
          {{ item.label }}
        </a>
      </nav>

      <div class="mk-header__actions">
        <RouterLink to="/login" class="mk-login">登录</RouterLink>
        <RouterLink to="/register" class="mk-btn mk-btn--primary mk-btn--sm">开始刷题</RouterLink>
        <button
          class="mk-menu-btn"
          type="button"
          aria-label="打开导航菜单"
          aria-controls="mk-mobile-nav"
          :aria-expanded="menuOpen"
          @click="menuOpen = !menuOpen"
        >
          <span :class="{ 'is-open': menuOpen }"></span>
          <span :class="{ 'is-open': menuOpen }"></span>
          <span :class="{ 'is-open': menuOpen }"></span>
        </button>
      </div>
    </div>

    <Transition name="mk-drawer">
      <div v-if="menuOpen" id="mk-mobile-nav" class="mk-mobile">
        <nav class="mk-mobile__nav" aria-label="移动端页面导航">
          <a v-for="item in NAV_ITEMS" :key="item.href" :href="item.href" @click="closeMenu">
            {{ item.label }}
          </a>
        </nav>
        <div class="mk-mobile__actions">
          <RouterLink to="/login" class="mk-btn mk-btn--ghost mk-btn--sm" @click="closeMenu">
            登录
          </RouterLink>
          <RouterLink to="/register" class="mk-btn mk-btn--primary mk-btn--sm" @click="closeMenu">
            开始刷题
          </RouterLink>
        </div>
      </div>
    </Transition>
  </header>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const NAV_ITEMS = [
  { label: '产品能力', href: '#capabilities' },
  { label: '学习闭环', href: '#loop' },
  { label: '题库覆盖', href: '#coverage' },
  { label: '适用场景', href: '#scenarios' }
]

const scrolled = ref(false)
const menuOpen = ref(false)

function onScroll() {
  scrolled.value = window.scrollY > 6
}

function closeMenu() {
  menuOpen.value = false
}

onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.mk-header {
  position: sticky;
  top: 0;
  z-index: 50;
  background-color: rgb(255 255 255 / 76%);
  border-bottom: 1px solid transparent;
  backdrop-filter: blur(18px) saturate(1.3);
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.mk-header.is-scrolled {
  background-color: rgb(255 255 255 / 92%);
  border-bottom-color: var(--mk-line);
  box-shadow: 0 10px 30px -28px rgb(16 24 40 / 24%);
}

.mk-header__inner {
  display: flex;
  gap: 32px;
  align-items: center;
  width: 100%;
  max-width: var(--mk-container);
  height: 68px;
  padding: 0 32px;
  margin: 0 auto;
}

.mk-brand {
  display: flex;
  flex-shrink: 0;
  gap: 9px;
  align-items: center;
  text-decoration: none;
}

.mk-brand__icon {
  display: block;
  flex-shrink: 0;
  width: 30px;
  height: 30px;
}

.mk-brand__name {
  color: var(--mk-blue);
  font-size: 17px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.01em;
}

.mk-nav {
  display: flex;
  gap: 2px;
  align-items: center;
}

.mk-nav a {
  padding: 8px 12px;
  color: var(--mk-muted);
  font-size: 13.5px;
  line-height: 1.5;
  text-decoration: none;
  border-radius: 8px;
  transition:
    color 0.18s ease,
    background-color 0.18s ease;
}

.mk-nav a:hover {
  color: var(--mk-blue);
  background-color: #f3f7ff;
}

.mk-header__actions {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-left: auto;
}

.mk-login {
  padding: 8px 6px;
  color: var(--mk-muted);
  font-size: 13.5px;
  line-height: 1.5;
  text-decoration: none;
  transition: color 0.18s ease;
}

.mk-login:hover {
  color: var(--mk-blue);
}

.mk-menu-btn {
  display: none;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  background-color: #fff;
  border: 1px solid var(--mk-line);
  border-radius: 9px;
  cursor: pointer;
}

.mk-menu-btn span {
  display: block;
  width: 15px;
  height: 1.5px;
  background-color: var(--mk-ink);
  border-radius: 2px;
  transition:
    transform 0.18s ease,
    opacity 0.18s ease;
}

.mk-menu-btn span:first-child.is-open {
  transform: translateY(5.5px) rotate(45deg);
}

.mk-menu-btn span:nth-child(2).is-open {
  opacity: 0;
}

.mk-menu-btn span:last-child.is-open {
  transform: translateY(-5.5px) rotate(-45deg);
}

.mk-mobile {
  position: absolute;
  top: 100%;
  right: 0;
  left: 0;
  padding: 10px 20px 18px;
  background-color: #fff;
  border-bottom: 1px solid var(--mk-line);
  box-shadow: 0 20px 36px -28px rgb(16 24 40 / 24%);
}

.mk-mobile__nav {
  display: flex;
  flex-direction: column;
}

.mk-mobile__nav a {
  padding: 12px 4px;
  color: var(--mk-text);
  font-size: 14px;
  text-decoration: none;
  border-bottom: 1px solid #f3f4f7;
}

.mk-mobile__nav a:hover {
  color: var(--mk-blue);
}

.mk-mobile__actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.mk-mobile__actions .mk-btn {
  flex: 1;
}

.mk-drawer-enter-active,
.mk-drawer-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.mk-drawer-enter-from,
.mk-drawer-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 900px) {
  .mk-header__inner {
    gap: 12px;
    padding: 0 20px;
  }

  .mk-nav {
    display: none;
  }

  .mk-menu-btn {
    display: inline-flex;
  }
}

@media (max-width: 560px) {
  .mk-login {
    display: none;
  }

  .mk-header__actions .mk-btn {
    height: 36px;
    padding: 0 14px;
    font-size: 13px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mk-header,
  .mk-nav a,
  .mk-login,
  .mk-menu-btn span {
    transition: none;
  }
}
</style>
