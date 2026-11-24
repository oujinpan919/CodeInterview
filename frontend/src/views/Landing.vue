<template>
  <div ref="root" class="mk-root mk-page">
    <MarketingHeader />

    <main>
      <!-- Hero -->
      <section id="top" class="mk-hero">
        <div class="mk-container mk-hero__grid">
          <div class="mk-hero__copy">
            <p class="mk-eyebrow">
              <span class="mk-eyebrow__dot" aria-hidden="true"></span>
              面试题库 · 刷题路线 · AI 助手
            </p>
            <h1 class="mk-hero__title">
              <span class="mk-hero__line">把面试题，</span>
              <span class="mk-hl">刷成自己的体系</span>
            </h1>
            <p class="mk-hero__sub">
              从分类题库到刷题路线，从三档自评到错题复习，码上记帮你把零散的知识点，连成面试时能说清楚的答案。
            </p>
            <div class="mk-hero__actions">
              <RouterLink to="/register" class="mk-btn mk-btn--primary mk-btn--lg">
                开始刷题
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" aria-hidden="true">
                  <path d="M5 12h13M13 6l6 6-6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </RouterLink>
              <RouterLink to="/question" class="mk-btn mk-btn--ghost mk-btn--lg">
                先逛逛题库
              </RouterLink>
            </div>
            <ul class="mk-hero__points">
              <li>
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" aria-hidden="true">
                  <path d="m5 12.5 4.2 4.2L19 6.8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                注册后保存练习进度
              </li>
              <li>
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" aria-hidden="true">
                  <path d="m5 12.5 4.2 4.2L19 6.8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                三档自评与错题复习
              </li>
              <li>
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" aria-hidden="true">
                  <path d="m5 12.5 4.2 4.2L19 6.8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                AI 助手围绕当前题目回答
              </li>
            </ul>
          </div>

          <div class="mk-hero__visual">
            <div class="mk-hero__glow" aria-hidden="true"></div>

            <div class="mk-float mk-float--assess" aria-hidden="true">
              <span class="mk-float__label">三档自评</span>
              <span class="mk-chip mk-chip--on">掌握</span>
              <span class="mk-chip mk-chip--mid">模糊</span>
              <span class="mk-chip">不会</span>
            </div>

            <div class="mk-browser mk-browser--main">
              <div class="mk-browser__bar" aria-hidden="true">
                <span></span><span></span><span></span>
                <em>码上记 / 题目练习</em>
              </div>
              <img
                src="/marketing/practice.jpg"
                alt="码上记题目练习页面截图"
                loading="eager"
                decoding="async"
              />
            </div>

            <div class="mk-float mk-float--assist">
              <div class="mk-float__head">
                <span class="mk-float__mark" aria-hidden="true">✦</span>
                <span>AI 助手</span>
                <span class="mk-float__live">在线</span>
              </div>
              <img
                src="/marketing/assistant.jpg"
                alt="码上记 AI 面试助手页面截图"
                loading="lazy"
                decoding="async"
              />
            </div>
          </div>
        </div>
      </section>

      <!-- 产品能力 -->
      <section id="capabilities" class="mk-section">
        <div class="mk-container">
          <div class="mk-section-head mk-reveal">
            <p class="mk-eyebrow">
              <span class="mk-eyebrow__line" aria-hidden="true"></span>
              PRODUCT / 产品能力
            </p>
            <h2 class="mk-h2">从刷题到复盘，一条链就够了</h2>
            <p class="mk-section-sub">不是把题堆在一起，而是让每一道题都有下一步。</p>
          </div>

          <div class="mk-cap-grid">
            <article v-for="cap in CAPABILITIES" :key="cap.key" class="mk-cap mk-reveal">
              <div class="mk-cap__head">
                <span class="mk-icon" aria-hidden="true">
                  <svg v-if="cap.icon === 'bank'" viewBox="0 0 24 24" width="22" height="22" fill="none">
                    <path d="m12 3 9 4.5-9 4.5-9-4.5L12 3Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round" />
                    <path d="m4.5 12 7.5 3.75L19.5 12M4.5 16.5 12 20.25l7.5-3.75" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                  <svg v-else-if="cap.icon === 'path'" viewBox="0 0 24 24" width="22" height="22" fill="none">
                    <circle cx="6" cy="18" r="2.2" stroke="currentColor" stroke-width="1.6" />
                    <circle cx="18" cy="6" r="2.2" stroke="currentColor" stroke-width="1.6" />
                    <path d="M8.2 18H11a4 4 0 0 0 4-4V8.2" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
                  </svg>
                  <svg v-else-if="cap.icon === 'assess'" viewBox="0 0 24 24" width="22" height="22" fill="none">
                    <path d="M5 6.5h8M5 12h8M5 17.5h5.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                    <path d="m16 16.5 2 2 3.2-4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                  <svg v-else viewBox="0 0 24 24" width="22" height="22" fill="none">
                    <path d="M12 3.5 14 10l6.5 2-6.5 2-2 6.5-2-6.5L3.5 12 10 10l2-6.5Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round" />
                  </svg>
                </span>
                <span v-if="cap.private" class="mk-badge">登录后可用</span>
              </div>
              <h3>{{ cap.title }}</h3>
              <p>{{ cap.desc }}</p>
              <RouterLink :to="cap.to" class="mk-textlink">
                {{ cap.link }}
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" aria-hidden="true">
                  <path d="M5 12h13M13 6l6 6-6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </RouterLink>
            </article>
          </div>
        </div>
      </section>

      <!-- 学习闭环 -->
      <section id="loop" class="mk-section mk-section--soft">
        <div class="mk-container">
          <div class="mk-section-head mk-section-head--center mk-reveal">
            <p class="mk-eyebrow mk-eyebrow--center">
              <span class="mk-eyebrow__line" aria-hidden="true"></span>
              HOW IT WORKS / 学习闭环
              <span class="mk-eyebrow__line mk-eyebrow__line--reverse" aria-hidden="true"></span>
            </p>
            <h2 class="mk-h2">学、练、评、复，四步把题变成自己的</h2>
            <p class="mk-section-sub">刷题不是一次性动作，码上记把每一步都留成下一次的起点。</p>
          </div>

          <ol class="mk-loop">
            <li v-for="(step, index) in LOOP_STEPS" :key="step.title" class="mk-loop__step mk-reveal">
              <span class="mk-loop__no">0{{ index + 1 }}</span>
              <h3>{{ step.title }}</h3>
              <p>{{ step.desc }}</p>
            </li>
          </ol>
        </div>
      </section>

      <!-- 题库覆盖 -->
      <section id="coverage" class="mk-section">
        <div class="mk-container mk-coverage">
          <div class="mk-coverage__copy mk-reveal">
            <p class="mk-eyebrow">
              <span class="mk-eyebrow__line" aria-hidden="true"></span>
              QUESTION BANK / 题库覆盖
            </p>
            <h2 class="mk-h2">从一个方向，<br />刷到一整张知识网</h2>
            <p class="mk-section-sub">题库按面试方向分类，题量与掌握度持续更新。</p>

            <div v-if="hasRealBank" class="mk-stats">
              <div class="mk-stat">
                <strong>{{ categories.length }}</strong>
                <span>个方向</span>
              </div>
              <div class="mk-stat">
                <strong>{{ totalQuestions }}</strong>
                <span>道题 · 当前分类合计</span>
              </div>
              <div class="mk-stat">
                <strong>3</strong>
                <span>档自评</span>
              </div>
            </div>
            <p v-else class="mk-note">{{ bankNote }}</p>

            <div v-if="displayHot.length" class="mk-hot">
              <p class="mk-hot__label">最近在刷</p>
              <div class="mk-hot__list">
                <RouterLink
                  v-for="question in displayHot"
                  :key="question.id"
                  :to="{ name: 'question-detail', params: { id: question.id } }"
                  class="mk-hot__item"
                >
                  <span class="mk-hot__dot" aria-hidden="true"></span>
                  <span>{{ question.title }}</span>
                </RouterLink>
              </div>
            </div>

            <RouterLink to="/question" class="mk-textlink mk-coverage__link">
              查看全部题库
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" aria-hidden="true">
                <path d="M5 12h13M13 6l6 6-6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </RouterLink>
          </div>

          <div class="mk-cat-grid">
            <RouterLink
              v-for="cat in displayCategories"
              :key="cat.id || cat.name"
              :to="cat.id ? { path: '/question', query: { categoryId: cat.id } } : '/question'"
              class="mk-cat mk-reveal"
            >
              <span class="mk-cat__icon" aria-hidden="true">{{ categoryMark(cat.name) }}</span>
              <span class="mk-cat__body">
                <strong>{{ cat.name }}</strong>
                <span class="mk-cat__desc">{{ cat.description }}</span>
              </span>
              <span class="mk-cat__meta">
                {{ cat.questionCount ? `${cat.questionCount} 题` : '持续更新' }}
              </span>
            </RouterLink>
          </div>
        </div>
      </section>

      <!-- 适用场景 -->
      <section id="scenarios" class="mk-section mk-section--soft">
        <div class="mk-container">
          <div class="mk-section-head mk-reveal">
            <p class="mk-eyebrow">
              <span class="mk-eyebrow__line" aria-hidden="true"></span>
              FOR WHOM / 适用场景
            </p>
            <h2 class="mk-h2">不管你现在在哪一步，都能接得上</h2>
            <p class="mk-section-sub">从第一次准备面试，到面试前的最后一轮查漏，按你的阶段选入口。</p>
          </div>

          <div class="mk-scene-grid">
            <article v-for="(scene, index) in SCENES" :key="scene.title" class="mk-scene mk-reveal">
              <span class="mk-scene__index">0{{ index + 1 }}</span>
              <h3>{{ scene.title }}</h3>
              <p>{{ scene.desc }}</p>
            </article>
          </div>
        </div>
      </section>

      <!-- 底部 CTA -->
      <section class="mk-cta-section">
        <div class="mk-container">
          <div class="mk-cta mk-reveal">
            <div class="mk-cta__glow" aria-hidden="true"></div>
            <p class="mk-eyebrow mk-eyebrow--center">START NOW / 开始刷题</p>
            <h2>今天先把一个薄弱点，变成能讲清楚的答案</h2>
            <p>注册后保存刷题进度、三档自评与错题记录，下次回来接着刷。</p>
            <div class="mk-cta__actions">
              <RouterLink to="/register" class="mk-btn mk-btn--primary mk-btn--lg">开始刷题</RouterLink>
              <RouterLink to="/question" class="mk-btn mk-btn--ghost mk-btn--lg">先逛逛题库</RouterLink>
            </div>
            <p class="mk-cta__login">已有账号？<RouterLink to="/login">登录</RouterLink></p>
          </div>
        </div>
      </section>
    </main>

    <MarketingFooter />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import MarketingHeader from '@/components/marketing/MarketingHeader.vue'
import MarketingFooter from '@/components/marketing/MarketingFooter.vue'
import { getHome } from '@/api/home'
import '@/assets/styles/marketing.css'

const FALLBACK_CATEGORIES = [
  { id: null, name: 'Java 集合', description: 'List / Map / Set 实现原理、扩容与线程安全', questionCount: null },
  { id: null, name: 'Java 并发', description: '线程池、锁、AQS、JMM 与并发工具', questionCount: null },
  { id: null, name: 'MySQL', description: '索引、事务与锁、MVCC 与优化', questionCount: null },
  { id: null, name: 'Redis', description: '数据结构、缓存与高可用', questionCount: null }
]

const CAPABILITIES = [
  {
    key: 'bank',
    icon: 'bank',
    title: '分类题库',
    desc: '按 Java、MySQL、Redis 等面试方向整理高频题，支持难度、标签与关键词检索。',
    to: '/question',
    link: '浏览题库'
  },
  {
    key: 'paths',
    icon: 'path',
    title: '刷题路线',
    desc: '把题目按学习顺序串成路线，不用每天重新决定先刷什么。',
    to: '/paths',
    link: '查看路线'
  },
  {
    key: 'assess',
    icon: 'assess',
    title: '三档自评 + 错题本',
    desc: '掌握、模糊、不会，一键记录真实程度；不会的题自动回到复习队列。',
    to: { path: '/login', query: { redirect: '/wrong-book' } },
    link: '登录后使用',
    private: true
  },
  {
    key: 'ai',
    icon: 'ai',
    title: 'AI 面试助手',
    desc: '带入当前题目追问考点、边界与相似题区别，回答围绕这道题展开。',
    to: { path: '/login', query: { redirect: '/assistant' } },
    link: '登录后使用',
    private: true
  }
]

const LOOP_STEPS = [
  { title: '学', desc: '先建立框架。从题库分类或路线进入一个方向，知道要掌握什么。' },
  { title: '练', desc: '再动手做题。按难度和分类推进，遇到边界情况随时看解析。' },
  { title: '评', desc: '用三档自评记录真实掌握度：掌握、模糊、不会，不制造虚假进度。' },
  { title: '复', desc: '最后回到薄弱点。错题和薄弱分类自动汇总，下一轮优先复习。' }
]

const SCENES = [
  { title: '校招 / 实习', desc: '基础还没串起来？先从路线建立顺序，再用错题本查漏。' },
  { title: '社招 / 跳槽', desc: '时间有限？优先刷高频与薄弱分类，让每次练习都留下记录。' },
  { title: '转行 / 补基础', desc: '面对陌生方向？按分类补齐知识框架，遇到概念随时问 AI 助手。' },
  { title: '面试冲刺', desc: '只剩几天？用错题本和薄弱分类做最后一轮边界与区别题排查。' }
]

const root = ref(null)
const categories = ref([])
const hot = ref([])
const loading = ref(true)
const bankError = ref(false)

const hasRealBank = computed(() => categories.value.length > 0)
const displayCategories = computed(() =>
  hasRealBank.value ? categories.value.slice(0, 6) : FALLBACK_CATEGORIES
)
const totalQuestions = computed(() =>
  categories.value.reduce((sum, category) => sum + (Number(category.questionCount) || 0), 0)
)
const displayHot = computed(() => hot.value.slice(0, 3))
const bankNote = computed(() => {
  if (loading.value) return '正在读取题库…'
  if (bankError.value) return '题库数据暂时不可用，先去看看已发布的题目。'
  return '题库正在整理中，先去看看已发布的题目。'
})

function categoryMark(name) {
  const text = String(name || '').replace(/^Java\s*/i, '').trim()
  return text.slice(0, 1) || '题'
}

let observer = null

function setupReveal() {
  if (!root.value) return

  const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const elements = Array.from(root.value.querySelectorAll('.mk-reveal:not(.is-visible)'))
  if (prefersReduced || !('IntersectionObserver' in window)) {
    elements.forEach((element) => element.classList.add('is-visible'))
    return
  }

  if (!observer) {
    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible')
            observer.unobserve(entry.target)
          }
        })
      },
      { threshold: 0.12, rootMargin: '0px 0px -32px 0px' }
    )
  }

  elements.forEach((element) => observer.observe(element))
}

async function loadBank() {
  try {
    const data = await getHome()
    categories.value = Array.isArray(data && data.categories) ? data.categories : []
    const hotSource = Array.isArray(data && data.hot)
      ? data.hot
      : Array.isArray(data && data.latest)
        ? data.latest
        : []
    hot.value = hotSource
  } catch {
    bankError.value = true
    categories.value = []
    hot.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  document.documentElement.classList.add('mk-smooth-scroll')
  setupReveal()
  await loadBank()
  await nextTick()
  setupReveal()
})

onBeforeUnmount(() => {
  document.documentElement.classList.remove('mk-smooth-scroll')
  if (observer) observer.disconnect()
})
</script>

<style scoped>
.mk-page {
  width: 100%;
  background-color: #fff;
}

.mk-hero {
  position: relative;
  padding: 76px 0 104px;
  overflow: hidden;
  background:
    radial-gradient(900px 480px at 78% -12%, rgb(37 99 235 / 10%), transparent 62%),
    radial-gradient(700px 420px at 4% -4%, rgb(99 102 241 / 7%), transparent 64%),
    #fff;
}

.mk-hero::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: '';
  background-image:
    linear-gradient(rgb(37 99 235 / 4%) 1px, transparent 1px),
    linear-gradient(90deg, rgb(37 99 235 / 4%) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at 62% 18%, #000 0%, transparent 72%);
}

.mk-hero__grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 56px;
  align-items: center;
}

.mk-hero__copy {
  max-width: 560px;
  animation: mk-rise 0.8s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.mk-hero__title {
  margin: 20px 0 0;
  color: var(--mk-ink);
  font-size: clamp(40px, 4.6vw, 64px);
  font-weight: 700;
  line-height: 1.08;
  letter-spacing: -0.045em;
}

.mk-hero__line,
.mk-hl {
  display: block;
  width: max-content;
  max-width: 100%;
}

.mk-hl {
  position: relative;
  z-index: 0;
  color: var(--mk-blue);
  isolation: isolate;
}

.mk-hl::after {
  position: absolute;
  right: -5px;
  bottom: 5px;
  left: -5px;
  z-index: -1;
  height: 13px;
  content: '';
  background-color: #dbeafe;
  border-radius: 5px;
}

.mk-hero__sub {
  max-width: 520px;
  margin: 22px 0 0;
  color: var(--mk-text);
  font-size: 16px;
  line-height: 1.85;
}

.mk-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 30px;
}

.mk-hero__points {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  padding: 0;
  margin: 26px 0 0;
  color: var(--mk-muted);
  font-size: 12.5px;
  line-height: 1.6;
  list-style: none;
}

.mk-hero__points li {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.mk-hero__points svg {
  flex-shrink: 0;
  color: #16a34a;
}

.mk-hero__visual {
  position: relative;
  min-width: 0;
  padding: 18px 0 46px 30px;
  animation: mk-rise 0.9s 0.08s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.mk-hero__glow {
  position: absolute;
  inset: 6% -10% 2% 12%;
  pointer-events: none;
  background: radial-gradient(circle, rgb(37 99 235 / 16%), transparent 66%);
  filter: blur(26px);
}

.mk-browser {
  position: relative;
  z-index: 1;
  overflow: hidden;
  background-color: #fff;
  border: 1px solid #e5e9f2;
  border-radius: 18px;
  box-shadow: 0 34px 76px -46px rgb(15 23 42 / 38%);
}

.mk-browser--main {
  transform: rotate(-1.1deg);
}

.mk-browser__bar {
  display: flex;
  gap: 6px;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  background-color: #f8f9fc;
  border-bottom: 1px solid #eef0f5;
}

.mk-browser__bar span {
  width: 7px;
  height: 7px;
  background-color: #dfe3eb;
  border-radius: 50%;
}

.mk-browser__bar em {
  padding: 2px 10px;
  margin-left: 8px;
  color: #9aa1ae;
  font-size: 9px;
  font-style: normal;
  line-height: 1.5;
  background-color: #fff;
  border: 1px solid #eef0f5;
  border-radius: 999px;
}

.mk-browser img {
  display: block;
  width: 100%;
  height: auto;
}

.mk-float {
  position: absolute;
  z-index: 2;
  background-color: #fff;
  border: 1px solid #e5e9f2;
  box-shadow: 0 24px 54px -34px rgb(15 23 42 / 40%);
}

.mk-float--assist {
  bottom: 0;
  left: 0;
  width: 46%;
  padding: 8px;
  border-radius: 14px;
}

.mk-float__head {
  display: flex;
  gap: 6px;
  align-items: center;
  padding: 0 4px 7px;
  color: var(--mk-muted);
  font-size: 10px;
}

.mk-float__mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  color: #fff;
  font-size: 9px;
  background-image: var(--mk-gradient);
  border-radius: 5px;
}

.mk-float__live {
  margin-left: auto;
  color: #16a34a;
}

.mk-float--assist img {
  display: block;
  width: 100%;
  border: 1px solid #eef0f5;
  border-radius: 8px;
}

.mk-float--assess {
  top: -10px;
  right: 12px;
  display: flex;
  gap: 5px;
  align-items: center;
  padding: 8px 10px;
  font-size: 10px;
  border-radius: 999px;
}

.mk-float__label {
  margin-right: 2px;
  color: var(--mk-muted);
}

.mk-chip {
  padding: 2px 7px;
  color: #6b7280;
  font-size: 9.5px;
  line-height: 1.5;
  background-color: #f3f4f6;
  border-radius: 999px;
}

.mk-chip--on {
  color: #15803d;
  background-color: #e8f7ee;
}

.mk-chip--mid {
  color: #b45309;
  background-color: #fff7e6;
}

.mk-section {
  padding: 96px 0;
}

#capabilities,
#loop,
#coverage,
#scenarios {
  scroll-margin-top: 84px;
}

.mk-section--soft {
  background: linear-gradient(180deg, #f8faff 0%, #fbfcff 100%);
}

.mk-eyebrow__line--reverse {
  background: linear-gradient(90deg, transparent, var(--mk-blue));
}

.mk-cap-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.mk-cap {
  display: flex;
  flex-direction: column;
  min-height: 252px;
  padding: 24px;
  background-color: #fff;
  border: 1px solid var(--mk-line);
  border-radius: 20px;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.mk-cap:hover {
  border-color: rgb(37 99 235 / 22%);
  box-shadow: var(--mk-shadow-soft);
  transform: translateY(-3px);
}

.mk-cap__head {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  justify-content: space-between;
}

.mk-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: var(--mk-blue);
  background-color: var(--mk-blue-soft);
  border-radius: 13px;
}

.mk-badge {
  padding: 3px 8px;
  color: #1d4ed8;
  font-size: 10.5px;
  line-height: 1.5;
  white-space: nowrap;
  background-color: #f2f6ff;
  border: 1px solid #dbe7ff;
  border-radius: 999px;
}

.mk-cap h3 {
  margin: 18px 0 8px;
  color: var(--mk-ink);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: -0.02em;
}

.mk-cap p {
  margin: 0 0 18px;
  color: var(--mk-muted);
  font-size: 13.5px;
  line-height: 1.8;
}

.mk-cap .mk-textlink {
  margin-top: auto;
}

.mk-loop {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 28px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.mk-loop::before {
  position: absolute;
  top: 24px;
  right: 8%;
  left: 8%;
  height: 1px;
  content: '';
  background: linear-gradient(90deg, rgb(37 99 235 / 20%), rgb(37 99 235 / 6%));
}

.mk-loop__step {
  position: relative;
}

.mk-loop__no {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  color: var(--mk-blue);
  font-size: 12px;
  font-weight: 700;
  background-color: #fff;
  border: 1px solid #dbe7ff;
  border-radius: 50%;
  box-shadow: 0 0 0 7px #f9fbff;
}

.mk-loop__step h3 {
  margin: 22px 0 8px;
  color: var(--mk-ink);
  font-size: 17px;
  font-weight: 600;
}

.mk-loop__step p {
  margin: 0;
  color: var(--mk-muted);
  font-size: 13.5px;
  line-height: 1.8;
}

.mk-coverage {
  display: grid;
  grid-template-columns: minmax(0, 0.82fr) minmax(0, 1.18fr);
  gap: 64px;
  align-items: start;
}

.mk-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 26px;
}

.mk-stat {
  min-width: 118px;
  padding: 14px 16px;
  background-color: var(--mk-soft);
  border: 1px solid #e8efff;
  border-radius: 14px;
}

.mk-stat strong {
  display: block;
  color: var(--mk-blue);
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.mk-stat span {
  display: block;
  margin-top: 4px;
  color: var(--mk-muted);
  font-size: 11.5px;
  line-height: 1.5;
}

.mk-note {
  margin: 22px 0 0;
  color: var(--mk-muted);
  font-size: 13px;
  line-height: 1.7;
}

.mk-hot {
  margin-top: 26px;
}

.mk-hot__label {
  margin: 0 0 9px;
  color: var(--mk-faint);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.mk-hot__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mk-hot__item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  color: var(--mk-text);
  font-size: 13px;
  line-height: 1.6;
  text-decoration: none;
  transition: color 0.18s ease;
}

.mk-hot__item:hover {
  color: var(--mk-blue);
}

.mk-hot__dot {
  flex-shrink: 0;
  width: 5px;
  height: 5px;
  margin-top: 8px;
  background-color: var(--mk-blue);
  border-radius: 50%;
}

.mk-coverage__link {
  margin-top: 24px;
}

.mk-cat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mk-cat {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-start;
  padding: 18px;
  text-decoration: none;
  background-color: #fff;
  border: 1px solid var(--mk-line);
  border-radius: 16px;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.mk-cat:hover {
  border-color: rgb(37 99 235 / 22%);
  box-shadow: var(--mk-shadow-soft);
  transform: translateY(-2px);
}

.mk-cat__icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  background-image: linear-gradient(135deg, #6d9bff, #2563eb);
  border-radius: 10px;
}

.mk-cat__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.mk-cat__body strong {
  color: var(--mk-ink);
  font-size: 14.5px;
  font-weight: 600;
  line-height: 1.4;
}

.mk-cat__desc {
  display: -webkit-box;
  overflow: hidden;
  color: var(--mk-muted);
  font-size: 12.5px;
  line-height: 1.65;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.mk-cat__meta {
  width: 100%;
  padding-top: 8px;
  margin-top: 2px;
  color: var(--mk-faint);
  font-size: 11px;
  line-height: 1.5;
  border-top: 1px solid #f1f3f7;
}

.mk-scene-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.mk-scene {
  padding: 24px;
  background-color: #fff;
  border: 1px solid var(--mk-line);
  border-radius: 18px;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.mk-scene:hover {
  border-color: rgb(37 99 235 / 22%);
  box-shadow: var(--mk-shadow-soft);
  transform: translateY(-3px);
}

.mk-scene__index {
  display: block;
  color: var(--mk-blue);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.mk-scene h3 {
  margin: 18px 0 8px;
  color: var(--mk-ink);
  font-size: 17px;
  font-weight: 600;
}

.mk-scene p {
  margin: 0;
  color: var(--mk-muted);
  font-size: 13.5px;
  line-height: 1.8;
}

.mk-cta-section {
  padding: 0 0 96px;
  background-color: #fff;
}

.mk-cta {
  position: relative;
  padding: 64px 48px;
  overflow: hidden;
  text-align: center;
  background: linear-gradient(135deg, #eef4ff 0%, #f8fbff 58%, #eef2ff 100%);
  border: 1px solid #dce8ff;
  border-radius: 28px;
}

.mk-cta__glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 20%, rgb(37 99 235 / 8%), transparent 42%),
    radial-gradient(circle at 82% 82%, rgb(99 102 241 / 8%), transparent 42%);
}

.mk-cta > * {
  position: relative;
  z-index: 1;
}

.mk-cta h2 {
  max-width: 760px;
  margin: 14px auto 0;
  color: var(--mk-ink);
  font-size: clamp(28px, 3vw, 38px);
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.03em;
}

.mk-cta > p {
  max-width: 580px;
  margin: 14px auto 0;
  color: var(--mk-muted);
  font-size: 14.5px;
  line-height: 1.8;
}

.mk-cta__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  margin-top: 28px;
}

.mk-cta__login {
  margin-top: 18px !important;
  font-size: 12.5px !important;
  color: var(--mk-muted) !important;
}

.mk-cta__login a {
  color: var(--mk-blue);
  font-weight: 600;
  text-decoration: none;
}

@keyframes mk-rise {
  from {
    opacity: 0;
    transform: translateY(20px);
  }

  to {
    opacity: 1;
    transform: none;
  }
}

@media (max-width: 1100px) {
  .mk-cap-grid,
  .mk-scene-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .mk-coverage {
    grid-template-columns: 1fr;
    gap: 40px;
  }
}

@media (max-width: 900px) {
  .mk-hero__grid {
    grid-template-columns: 1fr;
    gap: 42px;
  }

  .mk-hero__copy {
    max-width: 680px;
  }

  .mk-hero__visual {
    max-width: 700px;
    padding: 8px 0 42px 20px;
  }

  .mk-loop {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .mk-loop::before {
    top: 24px;
    right: auto;
    bottom: 24px;
    left: 24px;
    width: 1px;
    height: auto;
    background: linear-gradient(180deg, rgb(37 99 235 / 20%), rgb(37 99 235 / 6%));
  }

  .mk-loop__step {
    display: grid;
    grid-template-columns: 48px minmax(0, 1fr);
    column-gap: 18px;
  }

  .mk-loop__no {
    grid-row: 1 / span 2;
  }

  .mk-loop__step h3 {
    margin: 2px 0 4px;
  }

  .mk-loop__step p {
    grid-column: 2;
  }
}

@media (max-width: 767px) {
  .mk-hero {
    padding: 48px 0 64px;
  }

  .mk-hero__title {
    font-size: 38px;
  }

  .mk-hero__sub {
    font-size: 14.5px;
  }

  .mk-hero__actions .mk-btn {
    flex: 1;
    min-width: 140px;
  }

  .mk-hero__visual {
    padding: 4px 0 0;
  }

  .mk-float--assist {
    display: none;
  }

  .mk-float--assess {
    top: -8px;
    right: 8px;
  }

  .mk-browser--main {
    transform: none;
  }

  .mk-section {
    padding: 64px 0;
  }

  .mk-cap-grid,
  .mk-scene-grid,
  .mk-cat-grid {
    grid-template-columns: 1fr;
  }

  .mk-cap {
    min-height: 0;
  }

  .mk-cta-section {
    padding-bottom: 64px;
  }

  .mk-cta {
    padding: 40px 20px;
    border-radius: 22px;
  }

  .mk-cta__actions .mk-btn {
    flex: 1;
    min-width: 140px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mk-hero__copy,
  .mk-hero__visual {
    animation: none;
  }

  .mk-cap,
  .mk-scene,
  .mk-cat,
  .mk-hot__item {
    transition: none;
  }
}
</style>
