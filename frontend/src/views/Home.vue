<template>
  <section class="home">
    <!-- 首次加载：Hero + 分类 + 两栏各 3 行的骨架 -->
    <div v-if="loading" class="home-loading" aria-busy="true" aria-label="首页加载中">
      <div class="home-loading__hero">
        <div class="home-loading__hero-copy">
          <span class="home-loading__bar home-loading__bar--xs" />
          <span class="home-loading__bar home-loading__bar--lg" />
          <span class="home-loading__bar home-loading__bar--sm" />
          <span class="home-loading__bar home-loading__bar--full" />
          <span class="home-loading__bar home-loading__bar--md" />
          <div class="home-loading__actions">
            <span class="home-loading__button" />
            <span class="home-loading__button" />
          </div>
        </div>
        <div class="home-loading__hero-visual" />
      </div>

      <div class="home-loading__categories">
        <div v-for="item in 5" :key="`skeleton-category-${item}`" class="home-loading__cat" />
      </div>

      <div class="home-loading__panels">
        <div v-for="panel in 2" :key="`skeleton-panel-${panel}`" class="home-loading__panel">
          <span class="home-loading__bar home-loading__bar--md" />
          <div
            v-for="row in 3"
            :key="`skeleton-row-${panel}-${row}`"
            class="home-loading__row"
          >
            <span class="home-loading__bar home-loading__bar--xs" />
            <span class="home-loading__bar home-loading__bar--full" />
          </div>
        </div>
      </div>
    </div>

    <template v-else>
      <!-- Hero -->
      <section class="home-hero">
        <div class="home-hero__copy">
          <p class="home-hero__eyebrow">CODEINTERVIEW · 面试题库</p>
          <h1 class="home-hero__title">码上记</h1>
          <p class="home-hero__subtitle">面试题库 · 三档自评 · 错题复习</p>
          <p class="home-hero__slogan">刷题不求多，先把不会的记住。</p>
          <div class="home-hero__actions">
            <el-button
              class="home-hero__action"
              type="primary"
              @click="goQuestions"
            >
              开始刷题
            </el-button>
            <el-button class="home-hero__action" @click="goWrongBook">看看我的错题</el-button>
          </div>
        </div>

        <div class="home-hero__visual" aria-hidden="true">
          <div class="home-visual">
            <span class="home-visual__ghost home-visual__ghost--back" />
            <span class="home-visual__ghost home-visual__ghost--mid" />
            <div class="home-visual__card">
              <div class="home-visual__top">
                <span class="home-visual__mono">JAVA · 集合</span>
                <span class="home-visual__level">中等</span>
              </div>
              <p class="home-visual__title">HashMap 的扩容机制是怎样的？</p>
              <div class="home-visual__tags">
                <span class="home-visual__tag">JVM</span>
                <span class="home-visual__tag">集合</span>
              </div>
              <div class="home-visual__assess">
                <span class="home-visual__assess-label">三档自评</span>
                <span class="home-visual__pill">掌握</span>
                <span class="home-visual__pill home-visual__pill--active">模糊</span>
                <span class="home-visual__pill">不会</span>
              </div>
              <div class="home-visual__footer">
                <span>已练 9 次</span>
                <span class="home-visual__more">继续刷 →</span>
              </div>
            </div>
            <span class="home-visual__badge">错题自动收口</span>
          </div>
        </div>
      </section>

      <ErrorState v-if="loadError" class="home-error" :message="loadError" @retry="loadHome" />

      <template v-else>
        <!-- 分类入口 -->
        <section class="home-section">
          <header class="home-section__head">
            <h2 class="home-section__title">按分类刷题</h2>
            <RouterLink class="home-section__link" to="/question">全部题目 →</RouterLink>
          </header>

          <ErrorState
            v-if="categoriesError"
            message="分类加载失败，请稍后重试"
            @retry="loadHome"
          />
          <div v-else-if="categories.length > 0" class="home-categories">
            <RouterLink
              v-for="category in categories"
              :key="category.id"
              class="home-category"
              :to="{ path: '/question', query: { categoryId: category.id } }"
            >
              <span class="home-category__initial" aria-hidden="true">
                {{ categoryInitial(category.name) }}
              </span>
              <h3 class="home-category__name">{{ category.name }}</h3>
              <p class="home-category__desc">{{ category.description }}</p>
              <span class="home-category__count">{{ category.questionCount }} 题</span>
            </RouterLink>
          </div>
          <p v-else class="home-empty-line">暂无分类</p>
        </section>

        <!-- 空库引导 -->
        <EmptyState
          v-if="isEmptyLibrary"
          class="home-empty"
          title="题库建设中"
          description="管理员录入并发布题目后，最新与热门会出现在这里"
          :action-text="isAdmin ? '去录题' : ''"
          @action="goAdminCreate"
        />

        <template v-else>
          <!-- 最新 / 热门 -->
          <section class="home-panels">
            <div class="home-panel">
              <header class="home-section__head">
                <h2 class="home-section__title">最新上架</h2>
                <RouterLink class="home-section__link" to="/question">查看全部 →</RouterLink>
              </header>

              <ErrorState
                v-if="latestError"
                message="最新题目加载失败，请稍后重试"
                @retry="loadHome"
              />
              <ul v-else-if="latest.length > 0" class="home-questions">
                <li v-for="question in latest" :key="question.id">
                  <RouterLink
                    class="home-question"
                    :to="{ name: 'question-detail', params: { id: question.id } }"
                  >
                    <DifficultyTag :value="question.difficulty" />
                    <span class="home-question__title">{{ question.title }}</span>
                    <span class="home-question__category">{{ question.categoryName }}</span>
                  </RouterLink>
                </li>
              </ul>
              <p v-else class="home-empty-line">暂无最新题目</p>
            </div>

            <div class="home-panel">
              <header class="home-section__head">
                <h2 class="home-section__title">热门</h2>
                <RouterLink class="home-section__link" :to="{ path: '/question', query: { sort: 'hot' } }">
                  查看全部 →
                </RouterLink>
              </header>

              <ErrorState
                v-if="hotError"
                message="热门题目加载失败，请稍后重试"
                @retry="loadHome"
              />
              <ul v-else-if="hot.length > 0" class="home-questions">
                <li v-for="question in hot" :key="question.id">
                  <RouterLink
                    class="home-question"
                    :to="{ name: 'question-detail', params: { id: question.id } }"
                  >
                    <DifficultyTag :value="question.difficulty" />
                    <span class="home-question__title">{{ question.title }}</span>
                    <span class="home-question__category">{{ question.categoryName }}</span>
                  </RouterLink>
                </li>
              </ul>
              <p v-else class="home-empty-line">暂无热门题目</p>
            </div>
          </section>

          <!-- 继续刷：仅登录且有记录时显示 -->
          <section v-if="showContinue" class="home-section">
            <header class="home-section__head">
              <h2 class="home-section__title">继续刷</h2>
              <span class="home-section__hint">上次自评的题，接着过一遍</span>
            </header>

            <ErrorState
              v-if="continueError"
              message="继续刷加载失败，请稍后重试"
              @retry="loadHome"
            />
            <div v-else class="home-continue">
              <button
                v-for="item in continueLearning"
                :key="item.id"
                type="button"
                class="home-continue__card"
                @click="goQuestion(item.id)"
              >
                <span class="home-continue__title">{{ item.title }}</span>
                <span class="home-continue__meta">
                  <span class="home-continue__assess">
                    上次自评：{{ assessLabel(item.lastAssess) }}
                  </span>
                  <span class="home-continue__time">{{ formatTime(item.lastAssessedAt) }}</span>
                </span>
                <span class="home-continue__cta">继续 →</span>
              </button>
            </div>
          </section>
        </template>
      </template>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { getHome } from '@/api/home'
import { useUserStore } from '@/stores/user'
import DifficultyTag from '@/components/DifficultyTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const loadError = ref('')
const data = ref(null)

const isLogin = computed(() => userStore.isLogin)
const isAdmin = computed(() => userStore.isAdmin)

const categories = computed(() => readList('categories'))
const latest = computed(() => readList('latest'))
const hot = computed(() => readList('hot'))
const continueLearning = computed(() => readList('continueLearning'))

// 后端分块失败时字段为 null（成功但无数据为 []），据此做分块 ErrorState
const categoriesError = computed(
  () => !loading.value && data.value !== null && data.value.categories === null
)
const latestError = computed(
  () => !loading.value && data.value !== null && data.value.latest === null
)
const hotError = computed(() => !loading.value && data.value !== null && data.value.hot === null)
const continueError = computed(
  () =>
    !loading.value &&
    data.value !== null &&
    isLogin.value &&
    data.value.continueLearning === null
)

const isEmptyLibrary = computed(
  () =>
    !loading.value &&
    data.value !== null &&
    !latestError.value &&
    !hotError.value &&
    latest.value.length === 0 &&
    hot.value.length === 0
)

const showContinue = computed(
  () =>
    !loading.value &&
    data.value !== null &&
    isLogin.value &&
    (continueError.value || continueLearning.value.length > 0)
)

const ASSESS_LABELS = { 1: '掌握', 2: '模糊', 3: '不会' }

function readList(key) {
  const value = data.value ? data.value[key] : null
  return Array.isArray(value) ? value : []
}

function assessLabel(value) {
  return ASSESS_LABELS[value] || '未自评'
}

function formatTime(value) {
  if (!value) return ''
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm') : String(value)
}

function categoryInitial(name) {
  return name ? String(name).slice(0, 1).toUpperCase() : '·'
}

async function loadHome() {
  loading.value = true
  loadError.value = ''
  try {
    const result = await getHome()
    data.value = result && typeof result === 'object' ? result : {}
  } catch (error) {
    data.value = null
    loadError.value = (error && error.message) || '首页加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function goQuestions() {
  router.push('/question')
}

function goWrongBook() {
  router.push('/wrong-book')
}

function goAdminCreate() {
  router.push('/admin/questions/edit')
}

function goQuestion(id) {
  if (!id) return
  router.push({ name: 'question-detail', params: { id } })
}

onMounted(loadHome)

// 登录态变化（如退出登录）时重新拉取，控制「继续刷」显隐与数据
watch(isLogin, () => {
  loadHome()
})
</script>

<style scoped>
.home {
  min-width: 0;
}

/* ---------------------------------------------------------------------------
   Hero
   --------------------------------------------------------------------------- */
.home-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: var(--space-8);
  align-items: center;
  padding: var(--space-10) var(--space-8);
  overflow: hidden;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.home-hero::before {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    circle at 88% 18%,
    color-mix(in srgb, var(--color-primary) 10%, transparent),
    transparent 55%
  );
  content: '';
  pointer-events: none;
}

.home-hero__copy {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.home-hero__eyebrow {
  margin-bottom: var(--space-3);
  color: var(--color-primary);
  font-family: var(--font-family-mono);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  letter-spacing: 0.12em;
}

.home-hero__title {
  font-size: var(--font-size-h1);
  line-height: var(--line-height-h1);
  letter-spacing: 0.04em;
}

.home-hero__subtitle {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.home-hero__slogan {
  margin-top: var(--space-4);
  padding-left: var(--space-3);
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
  border-left: 3px solid var(--color-primary);
}

.home-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.home-hero__action {
  min-height: 44px;
  padding: 0 var(--space-6);
  border-radius: var(--radius-button);
}

/* Hero 右侧：题卡叠层（产品核心：题卡 + 三档自评 + 错题） */
.home-hero__visual {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
}

.home-hero__visual::before {
  position: absolute;
  inset: 4% 2%;
  background-image: repeating-linear-gradient(
      0deg,
      color-mix(in srgb, var(--color-primary) 9%, transparent) 0 1px,
      transparent 1px 24px
    ),
    repeating-linear-gradient(
      90deg,
      color-mix(in srgb, var(--color-primary) 9%, transparent) 0 1px,
      transparent 1px 24px
    );
  border-radius: var(--radius-card);
  content: '';
}

.home-visual {
  position: relative;
  width: 100%;
  max-width: 400px;
}

.home-visual__ghost {
  position: absolute;
  inset: 0;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.home-visual__ghost--back {
  opacity: 0.55;
  transform: rotate(-7deg) translate(-14px, 12px);
}

.home-visual__ghost--mid {
  opacity: 0.8;
  transform: rotate(4deg) translate(12px, -6px);
}

.home-visual__card {
  position: relative;
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: 0 18px 44px color-mix(in srgb, var(--color-text) 12%, transparent);
}

.home-visual__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.home-visual__mono {
  color: var(--color-text-secondary);
  font-family: var(--font-family-mono);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  letter-spacing: 0.08em;
}

.home-visual__level {
  padding: 2px var(--space-2);
  color: var(--color-warning);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  background-color: color-mix(in srgb, var(--color-warning) 12%, transparent);
  border-radius: var(--radius-tag);
}

.home-visual__title {
  margin-top: var(--space-3);
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
}

.home-visual__tags {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.home-visual__tag {
  padding: 2px var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  background-color: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-tag);
}

.home-visual__assess {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-4);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.home-visual__assess-label {
  margin-right: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.home-visual__pill {
  padding: 2px var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-tag);
}

.home-visual__pill--active {
  color: var(--color-bg-card);
  background-color: var(--color-primary);
  border-color: var(--color-primary);
}

.home-visual__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.home-visual__more {
  color: var(--color-primary);
  font-weight: 600;
}

.home-visual__badge {
  position: absolute;
  right: var(--space-4);
  bottom: calc(-1 * var(--space-3));
  padding: var(--space-1) var(--space-3);
  color: var(--color-bg-card);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  background-color: var(--color-primary);
  border-radius: var(--radius-tag);
}

/* ---------------------------------------------------------------------------
   区块与分类
   --------------------------------------------------------------------------- */
.home-section {
  margin-top: var(--space-6);
}

.home-section__head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-2) var(--space-3);
  margin-bottom: var(--space-3);
}

.home-section__title {
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
}

.home-section__link {
  color: var(--color-primary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.home-section__link:hover {
  color: var(--color-primary-hover);
  text-decoration: underline;
}

.home-section__hint {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.home-categories {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-3);
}

.home-category {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-height: 44px;
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  transition: border-color 0.15s ease, transform 0.15s ease;
}

.home-category:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
}

.home-category:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.home-category__initial {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: var(--color-primary);
  font-family: var(--font-family-mono);
  font-size: var(--font-size-body);
  font-weight: 600;
  background-color: color-mix(in srgb, var(--color-primary) 10%, transparent);
  border-radius: var(--radius-button);
}

.home-category__name {
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
}

.home-category__desc {
  display: -webkit-box;
  min-height: calc(var(--line-height-aux) * 2);
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.home-category__count {
  margin-top: auto;
  color: var(--color-primary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  font-weight: 600;
}

/* ---------------------------------------------------------------------------
   最新 / 热门
   --------------------------------------------------------------------------- */
.home-panels {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  margin-top: var(--space-6);
}

.home-panel {
  min-width: 0;
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

/* 分块错误嵌在卡片内时去掉 ErrorState 自带边框，避免双层描边 */
.home-panel :deep(.error-state) {
  padding: var(--space-6) var(--space-2);
  background-color: transparent;
  border: 0;
}

.home-questions {
  display: flex;
  flex-direction: column;
}

.home-questions li + li {
  border-top: 1px solid var(--color-border);
}

.home-question {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-height: 44px;
  padding: var(--space-2) 0;
}

.home-question__title {
  display: -webkit-box;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  transition: color 0.15s ease;
}

.home-question:hover .home-question__title {
  color: var(--color-primary);
}

.home-question__category {
  flex-shrink: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

/* ---------------------------------------------------------------------------
   继续刷
   --------------------------------------------------------------------------- */
.home-continue {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
}

.home-continue__card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-height: 44px;
  padding: var(--space-5);
  text-align: left;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  transition: border-color 0.15s ease, transform 0.15s ease;
}

.home-continue__card:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
}

.home-continue__card:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.home-continue__title {
  display: -webkit-box;
  overflow: hidden;
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.home-continue__meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.home-continue__assess {
  color: var(--color-text);
}

.home-continue__cta {
  margin-top: auto;
  color: var(--color-primary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  font-weight: 600;
}

/* ---------------------------------------------------------------------------
   空态 / 错误 / 空行
   --------------------------------------------------------------------------- */
.home-empty {
  margin-top: var(--space-6);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.home-error {
  margin-top: var(--space-6);
}

.home-empty-line {
  padding: var(--space-4) 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  text-align: center;
}

/* ---------------------------------------------------------------------------
   骨架
   --------------------------------------------------------------------------- */
.home-loading {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.home-loading__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: var(--space-8);
  align-items: center;
  padding: var(--space-10) var(--space-8);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.home-loading__hero-copy {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.home-loading__bar,
.home-loading__button,
.home-loading__cat,
.home-loading__hero-visual {
  display: block;
  background-color: var(--color-border);
  border-radius: var(--radius-tag);
  animation: home-loading-pulse 1.2s ease-in-out infinite;
}

.home-loading__bar {
  height: 12px;
}

.home-loading__bar--xs {
  width: 96px;
  height: 10px;
}

.home-loading__bar--sm {
  width: 34%;
}

.home-loading__bar--md {
  width: 62%;
}

.home-loading__bar--lg {
  width: 46%;
  height: 28px;
}

.home-loading__bar--full {
  width: 100%;
}

.home-loading__actions {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.home-loading__button {
  width: 132px;
  height: 44px;
  border-radius: var(--radius-button);
}

.home-loading__hero-visual {
  min-height: 230px;
  border-radius: var(--radius-card);
}

.home-loading__categories {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-3);
}

.home-loading__cat {
  height: 152px;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.home-loading__panels {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.home-loading__panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.home-loading__row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

@keyframes home-loading-pulse {
  0%,
  100% {
    opacity: 0.55;
  }

  50% {
    opacity: 1;
  }
}

/* ---------------------------------------------------------------------------
   响应式（04 B6 / B7.2：768 / 1024 / 1280）
   --------------------------------------------------------------------------- */
@media (max-width: 1279px) {
  .home-hero {
    padding: var(--space-8) var(--space-6);
  }

  .home-loading__hero {
    padding: var(--space-8) var(--space-6);
  }
}

@media (max-width: 1023px) {
  .home-hero {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-6);
  }

  .home-hero__visual {
    display: none;
  }

  .home-loading__hero {
    grid-template-columns: minmax(0, 1fr);
  }

  .home-loading__hero-visual {
    display: none;
  }

  .home-categories,
  .home-loading__categories {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .home-hero {
    padding: var(--space-6) var(--space-5);
  }

  .home-hero__actions {
    flex-direction: column;
    gap: var(--space-2);
  }

  .home-hero__action {
    width: 100%;
    margin-left: 0;
  }

  .home-loading__hero {
    padding: var(--space-6) var(--space-5);
  }

  .home-loading__button {
    width: 100%;
  }

  .home-categories,
  .home-loading__categories {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .home-category {
    padding: var(--space-4);
  }

  .home-panels,
  .home-loading__panels {
    grid-template-columns: minmax(0, 1fr);
  }

  .home-panel {
    padding: var(--space-4);
  }

  .home-continue {
    display: flex;
    gap: var(--space-3);
    padding-bottom: var(--space-1);
    overflow-x: auto;
    scroll-snap-type: x proximity;
    -webkit-overflow-scrolling: touch;
  }

  .home-continue__card {
    flex: 0 0 78%;
    padding: var(--space-4);
    scroll-snap-align: start;
  }
}
</style>
