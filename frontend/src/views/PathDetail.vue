<template>
  <section class="path-page">
    <div v-if="loading" class="path-page__skeleton" aria-busy="true">
      <el-skeleton :rows="8" animated />
    </div>

    <ErrorState v-else-if="loadError" :message="loadError" @retry="loadPath" />

    <template v-else-if="path">
      <!-- 卡片 A：集合身份卡（对应 mianshiya 题库详情页顶部 banner） -->
      <section class="path-hero">
        <div class="path-hero__cover" aria-hidden="true">
          <CategoryIcon :name="path.name" />
        </div>

        <div class="path-hero__main">
          <h1 class="path-hero__title">{{ path.name }}</h1>
          <p class="path-hero__desc">{{ path.description }}</p>

          <div class="path-hero__actions">
            <el-button type="primary" class="path-hero__action" @click="continuePath">
              继续刷题
            </el-button>
            <el-button class="path-hero__action" @click="restartPath">从头刷</el-button>
            <el-button class="path-hero__action" @click="randomPractice">随机练一题</el-button>
            <el-button class="path-hero__action" @click="copyLink">复制链接</el-button>
          </div>

          <div class="path-hero__progress">
            <el-progress
              class="path-hero__bar"
              :percentage="percent"
              :stroke-width="6"
              :show-text="false"
              :color="percent >= 100 ? 'var(--color-success)' : 'var(--color-primary)'"
            />
            <span class="path-hero__progress-text">
              <template v-if="isLogin">{{ doneCount }} / {{ items.length }} 已刷</template>
              <template v-else>共 {{ items.length }} 题 · 登录后记录进度</template>
            </span>
          </div>
        </div>
      </section>

      <!-- 卡片 B：题目列表面板 -->
      <section class="path-list">
        <header class="path-list__head">
          <h2 class="path-list__title">题目列表</h2>

          <div class="path-list__filters">
            <el-input
              v-model="keyword"
              class="path-list__search"
              placeholder="搜索题目"
              maxlength="30"
              clearable
            />
            <el-select v-model="difficulty" class="path-list__select" placeholder="难度" clearable>
              <el-option label="简单" value="1" />
              <el-option label="中等" value="2" />
              <el-option label="困难" value="3" />
            </el-select>
            <el-select
              v-model="selectedTagIds"
              class="path-list__select path-list__select--tags"
              placeholder="标签"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
            >
              <el-option v-for="tag in tagOptions" :key="tag.id" :label="tag.name" :value="tag.id" />
            </el-select>
            <el-select v-model="sortMode" class="path-list__select" placeholder="排序">
              <el-option label="默认顺序" value="default" />
              <el-option label="难度（易→难）" value="difficulty" />
              <el-option label="未刷优先" value="undone" />
            </el-select>
          </div>
        </header>

        <p class="path-list__meta">
          共 {{ filteredItems.length }} 题<template v-if="hasFilter">（已筛选 / 全部 {{ items.length }}）</template>
        </p>

        <EmptyState
          v-if="filteredItems.length === 0"
          title="没有匹配的题目"
          description="换个关键词、难度或标签，或者清空筛选"
          action-text="清空筛选"
          @action="clearFilters"
        />

        <!-- ≥1024：表格式列表（标记 | 题目 | 难度 | 标签） -->
        <div v-else-if="isWide" class="path-table" :class="{ 'path-table--guest': !isLogin }" role="table">
          <div class="path-table__head" role="row">
            <span v-if="isLogin" class="path-table__cell path-table__cell--mark" role="columnheader">
              标记
            </span>
            <span class="path-table__cell" role="columnheader">题目</span>
            <span class="path-table__cell" role="columnheader">难度</span>
            <span class="path-table__cell" role="columnheader">标签</span>
          </div>

          <div
            v-for="item in filteredItems"
            :key="item.questionId"
            class="path-table__row"
            role="row"
            tabindex="0"
            @click="openQuestion(item)"
            @keydown.enter.prevent="openQuestion(item)"
            @keydown.space.prevent="openQuestion(item)"
          >
            <span v-if="isLogin" class="path-table__cell path-table__cell--mark" role="cell">
              <button
                class="path-table__mark"
                :class="{ 'path-table__mark--on': item.favorite }"
                type="button"
                :aria-label="item.favorite ? '取消收藏' : '收藏这道题'"
                @click.stop="toggleFavorite(item)"
              >
                {{ item.favorite ? '★' : '☆' }}
              </button>
            </span>

            <span class="path-table__title" role="cell">
              <span v-if="item.done" class="path-table__done" aria-label="已刷">✓</span>
              <span class="path-table__title-text" :class="{ 'path-table__title-text--done': item.done }">
                <HighlightText :text="item.title" :keyword="keyword" />
              </span>
            </span>

            <span
              class="path-table__difficulty"
              :class="`path-table__difficulty--${item.difficulty}`"
              role="cell"
            >
              {{ difficultyLabel(item.difficulty) }}
            </span>

            <span class="path-table__tags" role="cell">
              <span
                v-for="tag in (item.tags || []).slice(0, 3)"
                :key="tag.id"
                class="path-table__tag"
              >
                {{ tag.name }}
              </span>
              <span v-if="(item.tags || []).length > 3" class="path-table__tag path-table__tag--more">
                +{{ item.tags.length - 3 }}
              </span>
            </span>
          </div>
        </div>

        <!-- <1024：紧凑行（序号/✓ + 标题 + 难度 + 标签） -->
        <ul v-else class="path-rows">
          <li
            v-for="item in filteredItems"
            :key="item.questionId"
            class="path-row"
            tabindex="0"
            @click="openQuestion(item)"
            @keydown.enter.prevent="openQuestion(item)"
          >
            <span class="path-row__index" :class="{ 'path-row__index--done': item.done }">
              <template v-if="item.done">✓</template>
              <template v-else>{{ item.sort }}</template>
            </span>
            <span class="path-row__body">
              <span class="path-row__title">
                <HighlightText :text="item.title" :keyword="keyword" />
              </span>
              <span class="path-row__meta">
                <span class="path-row__difficulty" :class="`path-row__difficulty--${item.difficulty}`">
                  {{ difficultyLabel(item.difficulty) }}
                </span>
                <span
                  v-for="tag in (item.tags || []).slice(0, 2)"
                  :key="tag.id"
                  class="path-row__tag"
                >
                  {{ tag.name }}
                </span>
              </span>
            </span>
            <button
              v-if="isLogin"
              class="path-row__mark"
              :class="{ 'path-row__mark--on': item.favorite }"
              type="button"
              :aria-label="item.favorite ? '取消收藏' : '收藏这道题'"
              @click.stop="toggleFavorite(item)"
            >
              {{ item.favorite ? '★' : '☆' }}
            </button>
          </li>
        </ul>
      </section>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import { getLearningPath } from '@/api/path'
import { favoriteQuestion, unfavoriteQuestion } from '@/api/question'
import { useUserStore } from '@/stores/user'
import CategoryIcon from '@/components/CategoryIcon.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import HighlightText from '@/components/HighlightText.vue'

const DIFFICULTY_LABELS = { 1: '简单', 2: '中等', 3: '困难' }

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isWide = useMediaQuery('(min-width: 1024px)')

const loading = ref(true)
const loadError = ref('')
const path = ref(null)
const items = ref([])

const isLogin = computed(() => userStore.isLogin)
const keyword = ref('')
const difficulty = ref('')
const selectedTagIds = ref([])
const sortMode = ref('default')

const doneCount = computed(() => items.value.filter((item) => item.done).length)
const percent = computed(() => {
  const total = items.value.length
  if (total <= 0) return 0
  return Math.min(100, Math.round((doneCount.value / total) * 100))
})

/** 标签选项从本路线题目里聚合（≤30 题），不额外打接口 */
const tagOptions = computed(() => {
  const map = new Map()
  items.value.forEach((item) => {
    ;(item.tags || []).forEach((tag) => {
      if (!map.has(tag.id)) map.set(tag.id, tag)
    })
  })
  return [...map.values()].sort((a, b) => a.name.localeCompare(b.name))
})

const hasFilter = computed(
  () =>
    Boolean(keyword.value.trim()) ||
    Boolean(difficulty.value) ||
    selectedTagIds.value.length > 0 ||
    sortMode.value !== 'default'
)

/** 客户端过滤 + 排序：数据已整条拉回，输入即时响应，零接口压力 */
const filteredItems = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const diff = Number(difficulty.value) || null
  const tagIds = selectedTagIds.value
  const list = items.value.filter((item) => {
    if (diff && item.difficulty !== diff) return false
    if (tagIds.length > 0 && !(item.tags || []).some((tag) => tagIds.includes(tag.id))) {
      return false
    }
    if (!kw) return true
    const inTitle = (item.title || '').toLowerCase().includes(kw)
    const inTags = (item.tags || []).some((tag) => (tag.name || '').toLowerCase().includes(kw))
    const inCategory = (item.categoryName || '').toLowerCase().includes(kw)
    return inTitle || inTags || inCategory
  })
  const sorted = [...list]
  if (sortMode.value === 'difficulty') {
    sorted.sort((a, b) => a.difficulty - b.difficulty || a.sort - b.sort)
  } else if (sortMode.value === 'undone') {
    sorted.sort((a, b) => Number(a.done) - Number(b.done) || a.sort - b.sort)
  } else {
    sorted.sort((a, b) => a.sort - b.sort)
  }
  return sorted
})

function difficultyLabel(value) {
  return DIFFICULTY_LABELS[value] || '—'
}

function clearFilters() {
  keyword.value = ''
  difficulty.value = ''
  selectedTagIds.value = []
  sortMode.value = 'default'
}

async function loadPath() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getLearningPath(route.params.id)
    path.value = data
    items.value = Array.isArray(data && data.items) ? data.items : []
  } catch (error) {
    path.value = null
    items.value = []
    loadError.value = (error && error.message) || '路线加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

/** 行点击：默认进入题目详情（上下题按同分类，不打断既有上下文逻辑） */
function openQuestion(item) {
  router.push({ name: 'question-detail', params: { id: item.questionId } })
}

function continuePath() {
  if (items.value.length === 0) {
    ElMessage.info('这条路线暂时没有可刷的题目')
    return
  }
  const next = items.value.find((item) => !item.done) || items.value[0]
  openQuestion(next)
}

function restartPath() {
  if (items.value.length === 0) {
    ElMessage.info('这条路线暂时没有可刷的题目')
    return
  }
  openQuestion(items.value[0])
}

/** 「随机练一题」限定在本路线内随机（比全库随机更贴合页面语义，且零接口） */
async function randomPractice() {
  if (items.value.length === 0) {
    ElMessage.info('这条路线暂时没有可刷的题目')
    return
  }
  const pool = keywordsOrAll()
  const picked = pool[Math.floor(Math.random() * pool.length)]
  openQuestion(picked)
}

function keywordsOrAll() {
  return filteredItems.value.length > 0 ? filteredItems.value : items.value
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制地址栏')
  }
}

async function toggleFavorite(item) {
  if (!isLogin.value) {
    try {
      await ElMessageBox.confirm('登录后才能收藏题目，去登录？', '需要登录', {
        type: 'info',
        confirmButtonText: '去登录',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  const next = !item.favorite
  item.favorite = next
  try {
    if (next) {
      await favoriteQuestion(item.questionId)
    } else {
      await unfavoriteQuestion(item.questionId)
    }
  } catch (error) {
    item.favorite = !next
    ElMessage.error((error && error.message) || '操作失败，请重试')
  }
}

onMounted(loadPath)
</script>

<style scoped>
.path-page {
  min-width: 0;
}

.path-page__skeleton {
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

/* ---------------------------------------------------------------------------
   卡片 A：集合身份卡
   --------------------------------------------------------------------------- */
.path-hero {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  gap: var(--space-6);
  align-items: start;
  padding: var(--space-6);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}

.path-hero__cover {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 128px;
  height: 128px;
}

/* 复用分类图标并放大到 96px（内部图案同步放大） */
.path-hero__cover :deep(.category-icon) {
  width: 96px;
  height: 96px;
  border-radius: 22px;
}

.path-hero__cover :deep(.category-icon__svg) {
  width: 52px;
  height: 52px;
}

.path-hero__main {
  min-width: 0;
}

.path-hero__title {
  margin: 0;
  color: var(--color-text);
  font-size: var(--font-size-h1);
  font-weight: 600;
  line-height: var(--line-height-h1);
  overflow-wrap: anywhere;
}

.path-hero__desc {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.path-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.path-hero__action {
  min-height: 36px;
  margin-left: 0;
  padding: 0 var(--space-4);
  border-radius: var(--radius-button);
}

.path-hero__progress {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.path-hero__bar {
  flex: 0 1 280px;
}

.path-hero__progress-text {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  white-space: nowrap;
}

/* ---------------------------------------------------------------------------
   卡片 B：题目列表面板
   --------------------------------------------------------------------------- */
.path-list {
  margin-top: var(--space-5);
  padding: var(--space-6);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}

.path-list__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.path-list__title {
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 600;
  line-height: var(--line-height-body);
}

.path-list__filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin-left: auto;
}

.path-list__search {
  width: 180px;
}

.path-list__search :deep(.el-input__wrapper),
.path-list__select :deep(.el-select__wrapper) {
  min-height: 36px;
  border-radius: var(--radius-button);
}

.path-list__select {
  width: 112px;
}

.path-list__select--tags {
  width: 160px;
}

.path-list__meta {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

/* ---------------------------------------------------------------------------
   表格式列表（≥1024）：标记 | 题目 | 难度 | 标签
   --------------------------------------------------------------------------- */
.path-table {
  margin-top: var(--space-3);
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-button);
}

.path-table__head,
.path-table__row {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) 64px minmax(0, 260px);
  gap: var(--space-4);
  align-items: center;
  padding: var(--space-3) var(--space-4);
}

.path-table--guest .path-table__head,
.path-table--guest .path-table__row {
  grid-template-columns: minmax(0, 1fr) 64px minmax(0, 260px);
}

.path-table__head {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  background-color: var(--color-bg-page);
  border-bottom: 1px solid var(--color-border);
}

.path-table__row {
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  transition: background-color var(--motion-fast) var(--motion-ease);
}

.path-table__row:last-child {
  border-bottom: 0;
}

.path-table__row:hover,
.path-table__row:focus-visible {
  background-color: var(--color-bg-page);
  outline: none;
}

.path-table__row:focus-visible {
  box-shadow: inset 0 0 0 2px var(--color-primary);
}

.path-table__cell--mark {
  text-align: center;
}

.path-table__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--color-text-disabled);
  font-size: 16px;
  background: transparent;
  border: 0;
  border-radius: var(--radius-button);
  cursor: pointer;
  transition: color var(--motion-fast) var(--motion-ease);
}

.path-table__mark:hover {
  color: var(--color-warning);
}

.path-table__mark--on {
  color: var(--color-warning);
}

.path-table__title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.path-table__done {
  flex-shrink: 0;
  color: var(--color-success);
  font-size: var(--font-size-aux);
  font-weight: 700;
}

.path-table__title-text {
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 500;
  line-height: var(--line-height-body);
  overflow-wrap: anywhere;
  transition: color var(--motion-fast) var(--motion-ease);
}

.path-table__row:hover .path-table__title-text {
  color: var(--color-primary);
}

.path-table__title-text--done {
  color: var(--color-text-secondary);
}

.path-table__difficulty {
  font-size: var(--font-size-aux);
  font-weight: 600;
  line-height: var(--line-height-aux);
}

.path-table__difficulty--1 {
  color: var(--color-success);
}

.path-table__difficulty--2 {
  color: var(--color-warning);
}

.path-table__difficulty--3 {
  color: var(--color-danger);
}

.path-table__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  min-width: 0;
}

.path-table__tag {
  padding: 1px var(--space-2);
  color: var(--color-text-secondary);
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
  background-color: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-tag);
}

.path-table__tag--more {
  border-style: dashed;
}

/* ---------------------------------------------------------------------------
   紧凑行（<1024）
   --------------------------------------------------------------------------- */
.path-rows {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.path-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  min-height: 44px;
  padding: var(--space-3);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-button);
  cursor: pointer;
  transition: border-color var(--motion-fast) var(--motion-ease);
}

.path-row:hover {
  border-color: var(--color-primary);
}

.path-row__index {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  background-color: var(--color-bg-page);
  border-radius: 50%;
}

.path-row__index--done {
  color: var(--color-bg-card);
  background-color: var(--color-success);
}

.path-row__body {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: var(--space-1);
  min-width: 0;
}

.path-row__title {
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 500;
  overflow-wrap: anywhere;
}

.path-row__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}

.path-row__difficulty {
  font-size: var(--font-size-aux);
  font-weight: 600;
}

.path-row__difficulty--1 {
  color: var(--color-success);
}

.path-row__difficulty--2 {
  color: var(--color-warning);
}

.path-row__difficulty--3 {
  color: var(--color-danger);
}

.path-row__tag {
  padding: 0 var(--space-2);
  color: var(--color-text-secondary);
  font-size: 12px;
  line-height: 18px;
  background-color: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-tag);
}

.path-row__mark {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: var(--color-text-disabled);
  font-size: 18px;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.path-row__mark--on {
  color: var(--color-warning);
}

@media (max-width: 1023px) {
  .path-hero {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-4);
    padding: var(--space-4);
  }

  .path-hero__cover {
    width: auto;
    height: auto;
    justify-content: flex-start;
  }

  .path-hero__cover :deep(.category-icon) {
    width: 64px;
    height: 64px;
    border-radius: 16px;
  }

  .path-hero__cover :deep(.category-icon__svg) {
    width: 36px;
    height: 36px;
  }

  .path-list {
    padding: var(--space-4);
  }

  .path-list__filters {
    width: 100%;
    margin-left: 0;
  }

  .path-list__search {
    flex: 1 1 140px;
    width: auto;
  }

  .path-list__select {
    flex: 1 1 96px;
    width: auto;
  }
}
</style>
