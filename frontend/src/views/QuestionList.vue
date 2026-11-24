<template>
  <section class="question-list">
    <!-- 卡片 A：题库身份卡（对齐 mianshiya 题库详情页顶部 banner）
         身份随筛选范围变化：分类 → 该分类；标签 → 该标签；关键词 → 搜索；否则 → 全部题库 -->
    <section class="list-hero">
      <div class="list-hero__cover" aria-hidden="true">
        <CategoryIcon :name="heroIconName" />
      </div>

      <div class="list-hero__main">
        <h1 class="list-hero__title">{{ heroTitle }}</h1>
        <p class="list-hero__desc">{{ heroDesc }}</p>

        <div class="list-hero__actions">
          <el-button
            v-if="heroScope"
            type="primary"
            class="list-hero__action"
            :loading="randomLoading"
            @click="goRandomPractice"
          >
            开始刷题
          </el-button>
          <el-button
            v-else
            type="primary"
            class="list-hero__action"
            :loading="todayLoading"
            @click="startTodayPlan"
          >
            开始今日计划
          </el-button>

          <el-button v-if="heroScope" class="list-hero__action" @click="clearFilters">
            查看全部题库
          </el-button>
          <el-button v-else class="list-hero__action" :loading="randomLoading" @click="goRandomPractice">
            随机练习
          </el-button>

          <el-button class="list-hero__action" @click="copyLink">复制链接</el-button>
        </div>
      </div>
    </section>

    <!-- 卡片 B：题目列表面板 -->
    <section class="list-panel">
      <header class="list-panel__head">
        <h2 class="list-panel__title">题目列表</h2>

        <div class="list-panel__filters">
          <!-- v3.15（A1）：本页不再放第二个搜索框，关键词统一走顶部全局搜索写入 URL；
               这里只加「深度搜索」开关（默认只搜标题/标签，勾选后连题干/答案一起搜） -->
          <el-checkbox
            v-if="filters.keyword"
            :model-value="filters.searchBody"
            class="list-panel__deep"
            @change="handleDeepSearchChange"
          >
            含答案正文
          </el-checkbox>
          <el-select
            :model-value="filters.categoryId"
            class="list-panel__select"
            placeholder="分类"
            clearable
            @change="handleCategoryChange"
          >
            <el-option label="全部分类" value="" />
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="`${category.name}（${category.questionCount}）`"
              :value="category.id"
            />
          </el-select>
          <el-select
            :model-value="filters.difficulty"
            class="list-panel__select list-panel__select--sm"
            placeholder="难度"
            clearable
            @change="handleDifficultyChange"
          >
            <el-option label="全部难度" value="" />
            <el-option label="简单" value="1" />
            <el-option label="中等" value="2" />
            <el-option label="困难" value="3" />
          </el-select>
          <el-select
            :model-value="filters.tagId"
            class="list-panel__select list-panel__select--tags"
            placeholder="标签"
            clearable
            filterable
            @change="handleTagChange"
          >
            <el-option label="全部标签" value="" />
            <el-option
              v-for="tag in tags"
              :key="tag.id"
              :label="`${tag.name}（${tag.questionCount}）`"
              :value="tag.id"
            />
          </el-select>
          <el-select
            :model-value="filters.sort"
            class="list-panel__select list-panel__select--sm"
            placeholder="排序"
            @change="handleSortChange"
          >
            <el-option label="最新" value="latest" />
            <el-option label="热门" value="hot" />
            <el-option label="难度" value="difficulty" />
          </el-select>
        </div>
      </header>

      <!-- 最近搜索（仅关键词为空时出现，命中关键词时给清空入口） -->
      <div v-if="searchHistory.length > 0 && !filters.keyword" class="list-panel__history">
        <span class="list-panel__history-label">最近搜索</span>
        <button
          v-for="word in searchHistory"
          :key="word"
          type="button"
          class="list-panel__history-item"
          @click="useHistory(word)"
        >
          {{ word }}
        </button>
        <button type="button" class="list-panel__history-clear" @click="clearHistory">清空</button>
      </div>

      <!-- 计数行只在「已筛选」或分类/标签加载失败时出现；未筛选时总数由分页器承担，避免重复 -->
      <p v-if="hasActiveFilter || taxonomyError" class="list-panel__meta">
        <template v-if="hasActiveFilter">共 {{ total }} 题（已筛选）</template>
        <el-button
          v-if="taxonomyError"
          class="list-panel__retry"
          link
          type="primary"
          @click="loadTaxonomy"
        >
          分类/标签加载失败，重试
        </el-button>
      </p>

      <div v-if="loading && questions.length === 0" class="list-panel__skeleton" aria-busy="true">
        <el-skeleton :rows="6" animated />
      </div>

      <ErrorState
        v-else-if="loadError && questions.length === 0"
        :message="loadError"
        @retry="retryLoad"
      />

      <EmptyState
        v-else-if="questions.length === 0"
        title="没有匹配的题目"
        description="换个关键词，或者把筛选条件放宽一点"
        action-text="清空筛选"
        @action="clearFilters"
      />

      <template v-else>
        <div v-if="staleError" class="list-panel__stale" role="alert">
          <span>{{ staleError }}</span>
          <el-button link type="primary" @click="retryLoad">重试</el-button>
        </div>

        <div
          v-if="loading"
          class="list-panel__loading"
          aria-hidden="true"
        />

        <!-- ≥1024：表格式列表（标记 | 题目 | 难度 | 标签） -->
        <div
          v-if="isWide"
          class="question-table"
          :class="{ 'question-table--guest': !isLogin }"
          role="table"
        >
          <div class="question-table__head" role="row">
            <span v-if="isLogin" class="question-table__col question-table__col--mark" role="columnheader">
              标记
            </span>
            <span class="question-table__col" role="columnheader">题目</span>
            <span class="question-table__col question-table__col--difficulty" role="columnheader">
              难度
            </span>
            <span class="question-table__col question-table__col--tags" role="columnheader">标签</span>
          </div>

          <div
            v-for="question in questions"
            :key="question.id"
            class="question-table__row"
            role="row"
            tabindex="0"
            @click="goDetail(question)"
            @keydown.enter.prevent="goDetail(question)"
            @keydown.space.prevent="goDetail(question)"
          >
            <span v-if="isLogin" class="question-table__col question-table__col--mark" role="cell">
              <button
                class="question-table__mark"
                :class="{ 'question-table__mark--on': question.favorite }"
                type="button"
                :aria-label="question.favorite ? '取消收藏' : '收藏这道题'"
                @click.stop="toggleFavorite(question)"
              >
                {{ question.favorite ? '★' : '☆' }}
              </button>
            </span>
            <!-- v4.1：未访问→主色蓝，已访问→绿色；行 hover 时不再抢成蓝色（见样式表） -->
            <span
              class="question-table__title"
              :class="{
                'question-table__title--unvisited': question.visited === false,
                'question-table__title--visited': question.visited === true
              }"
              role="cell"
            >
              <HighlightText :text="question.title" :keyword="filters.keyword" />
              <span v-if="matchLabel(question)" class="question-table__match">
                {{ matchLabel(question) }}
              </span>
            </span>
            <span
              class="question-table__difficulty"
              :class="`question-table__difficulty--${question.difficulty}`"
              role="cell"
            >
              {{ difficultyLabel(question.difficulty) }}
            </span>
            <span class="question-table__tags" role="cell">
              <span
                v-for="tag in (question.tags || []).slice(0, 3)"
                :key="tag.id"
                class="question-table__tag"
              >
                {{ tag.name }}
              </span>
              <span
                v-if="(question.tags || []).length > 3"
                class="question-table__tag question-table__tag--more"
              >
                +{{ question.tags.length - 3 }}
              </span>
            </span>
          </div>
        </div>

        <!-- <1024：卡片列表（触控友好） -->
        <div v-else class="question-list__cards">
          <QuestionCard
            v-for="question in questions"
            :key="question.id"
            :question="question"
            :keyword="filters.keyword"
            @click="goDetail"
          />
        </div>

        <PageBar
          :page="filters.page"
          :size="PAGE_SIZE"
          :total="total"
          :show-total="false"
          @change="handlePageChange"
        />
      </template>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { ElMessage } from 'element-plus'
import 'element-plus/theme-chalk/el-message.css'
import { getQuestions, getRandomQuestion, favoriteQuestion, unfavoriteQuestion } from '@/api/question'
import { createSession, getCurrentSession } from '@/api/session'
import { getCategories, getTags } from '@/api/taxonomy'
import { useUserStore } from '@/stores/user'
import CategoryIcon from '@/components/CategoryIcon.vue'
import QuestionCard from '@/components/QuestionCard.vue'
import HighlightText from '@/components/HighlightText.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import PageBar from '@/components/PageBar.vue'

const PAGE_SIZE = 10
const DIFFICULTY_LABELS = { 1: '简单', 2: '中等', 3: '困难' }
const SEARCH_HISTORY_KEY = 'ci_search_history'
const SEARCH_HISTORY_MAX = 8

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isWide = useMediaQuery('(min-width: 1024px)')

const isLogin = computed(() => userStore.isLogin)

const loading = ref(false)
const loadError = ref('')
const staleError = ref('')
const questions = ref([])
const total = ref(0)
/** 全库发布态题数（不随筛选变化，身份卡用） */
const baseTotal = ref(0)

const categories = ref([])
const tags = ref([])
const taxonomyError = ref(false)

const filters = ref({
  categoryId: '',
  tagId: '',
  difficulty: '',
  keyword: '',
  /** v3.15：是否连题干/答案正文一起搜（默认只搜标题与标签） */
  searchBody: false,
  sort: 'latest',
  page: 1
})

const randomLoading = ref(false)
const todayLoading = ref(false)

const categoryCount = computed(() => categories.value.length)

/* ---------------------------------------------------------------------------
 * 身份卡随筛选范围变化（修复：从首页点 Java 进来仍显示「全部题库」）
 * ------------------------------------------------------------------------- */
const activeCategory = computed(
  () => categories.value.find((item) => String(item.id) === String(filters.value.categoryId)) || null
)
const activeTag = computed(
  () => tags.value.find((item) => String(item.id) === String(filters.value.tagId)) || null
)
/** 是否存在"集合级"筛选：分类 / 标签 / 关键词。仅难度/排序不算身份变化 */
const heroScope = computed(
  () => Boolean(activeCategory.value) || Boolean(activeTag.value) || Boolean(filters.value.keyword)
)

const heroTitle = computed(() => {
  if (activeCategory.value) return activeCategory.value.name
  if (activeTag.value) return activeTag.value.name
  if (filters.value.keyword) return `搜索：${filters.value.keyword}`
  return '全部题库'
})

const heroIconName = computed(() => {
  if (activeCategory.value) return activeCategory.value.name
  if (activeTag.value) return activeTag.value.name
  return '全部题库'
})

const heroDesc = computed(() => {
  if (activeCategory.value) {
    const base = activeCategory.value.description || ''
    return `${base ? `${base} · ` : ''}共 ${activeCategory.value.questionCount} 题`
  }
  if (activeTag.value) {
    return `标签「${activeTag.value.name}」下共 ${activeTag.value.questionCount} 题`
  }
  if (filters.value.keyword) {
    return `关键词「${filters.value.keyword}」命中 ${total.value} 题`
  }
  const totalText = baseTotal.value || total.value
  const categoryText = categoryCount.value > 0 ? ` · ${categoryCount.value} 个分类` : ''
  const tagText = tags.value.length > 0 ? ` · ${tags.value.length} 个标签` : ''
  return `共 ${totalText} 道发布态题目${categoryText}${tagText}，按分类 / 标签 / 难度筛选后开刷。`
})
const isFiltered = computed(
  () =>
    Boolean(filters.value.categoryId) ||
    Boolean(filters.value.tagId) ||
    Boolean(filters.value.difficulty) ||
    Boolean(filters.value.keyword)
)

const hasActiveFilter = computed(
  () =>
    Boolean(filters.value.categoryId) ||
    Boolean(filters.value.tagId) ||
    Boolean(filters.value.difficulty) ||
    Boolean(filters.value.keyword) ||
    filters.value.sort !== 'latest'
)

function difficultyLabel(value) {
  return DIFFICULTY_LABELS[value] || '—'
}

/* ---------------------------------------------------------------------------
 * URL ↔ 筛选状态（可分享、刷新保持）
 * ------------------------------------------------------------------------- */
function pickQueryString(value) {
  if (Array.isArray(value)) return pickQueryString(value[0])
  return value == null ? '' : String(value)
}

function readQuery() {
  const query = route.query
  filters.value = {
    categoryId: pickQueryString(query.categoryId),
    tagId: pickQueryString(query.tagId),
    difficulty: pickQueryString(query.difficulty),
    keyword: pickQueryString(query.keyword).slice(0, 30),
    sort: ['hot', 'difficulty'].includes(query.sort) ? query.sort : 'latest',
    searchBody: query.deep === '1',
    page: Math.max(1, Number(query.page) || 1)
  }
}

function buildQuery(next) {
  const query = {}
  if (next.categoryId) query.categoryId = next.categoryId
  if (next.tagId) query.tagId = next.tagId
  if (next.difficulty) query.difficulty = next.difficulty
  if (next.keyword) query.keyword = next.keyword
  if (next.searchBody && next.keyword) query.deep = '1'
  if (next.sort === 'hot' || next.sort === 'difficulty') query.sort = next.sort
  if (next.page > 1) query.page = String(next.page)
  return query
}

function applyFilters(patch, resetPage = true) {
  const next = { ...filters.value, ...patch }
  if (resetPage) next.page = 1
  filters.value = next
  return router.replace({ path: '/question', query: buildQuery(next) })
}

function handleCategoryChange(value) {
  applyFilters({ categoryId: value || '' })
}

function handleDifficultyChange(value) {
  applyFilters({ difficulty: value || '' })
}

function handleTagChange(value) {
  applyFilters({ tagId: value || '' })
}

function handleSortChange(value) {
  applyFilters({ sort: ['hot', 'difficulty'].includes(value) ? value : 'latest' })
}

function clearFilters() {
  return applyFilters({
    categoryId: '',
    tagId: '',
    difficulty: '',
    keyword: '',
    searchBody: false
  })
}

function retryLoad() {
  loadQuestions()
}

function handlePageChange(next) {
  filters.value = { ...filters.value, page: next }
  router.replace({ path: '/question', query: buildQuery(filters.value) })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/* ---------------------------------------------------------------------------
 * 搜索：历史（localStorage）+ 题号直达 + 300ms 防抖
 * ------------------------------------------------------------------------- */
function readSearchHistory() {
  try {
    const raw = localStorage.getItem(SEARCH_HISTORY_KEY)
    const list = raw ? JSON.parse(raw) : []
    if (!Array.isArray(list)) return []
    return list.filter((item) => typeof item === 'string' && item.trim()).slice(0, SEARCH_HISTORY_MAX)
  } catch {
    return []
  }
}

const searchHistory = ref(readSearchHistory())

function rememberKeyword(rawKeyword) {
  const keyword = (rawKeyword || '').trim()
  if (!keyword) return
  const next = [keyword, ...searchHistory.value.filter((item) => item !== keyword)].slice(
    0,
    SEARCH_HISTORY_MAX
  )
  searchHistory.value = next
  try {
    localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(next))
  } catch {
    // 隐私模式 / 存储被禁用：不记录历史，不影响搜索
  }
}

function clearHistory() {
  searchHistory.value = []
  try {
    localStorage.removeItem(SEARCH_HISTORY_KEY)
  } catch {
    // 同上
  }
}

function useHistory(word) {
  rememberKeyword(word)
  applyFilters({ keyword: word })
}

/** v3.15：深度搜索开关（A1 后本页无搜索框，关键词来自顶部全局搜索写入的 URL） */
function handleDeepSearchChange(value) {
  applyFilters({ searchBody: Boolean(value) })
}

/** 命中位置标注：标题命中 > 标签命中 > 答案命中，只显示最主要的一处 */
const MATCH_LABELS = { title: '标题命中', tag: '标签命中', body: '答案命中' }

function matchLabel(question) {
  if (!filters.value.keyword || !question.matchFields) return ''
  const fields = String(question.matchFields).split(',')
  for (const key of ['title', 'tag', 'body']) {
    if (fields.includes(key)) return MATCH_LABELS[key]
  }
  return ''
}

/* ---------------------------------------------------------------------------
 * 数据加载
 * ------------------------------------------------------------------------- */
let loadSeq = 0

async function loadQuestions() {
  const seq = ++loadSeq
  loading.value = true
  staleError.value = ''
  try {
    const params = {
      page: filters.value.page,
      pageSize: PAGE_SIZE,
      sort: filters.value.sort
    }
    if (filters.value.categoryId) params.categoryId = filters.value.categoryId
    if (filters.value.tagId) params.tagId = filters.value.tagId
    if (filters.value.difficulty) params.difficulty = filters.value.difficulty
    if (filters.value.keyword) params.keyword = filters.value.keyword
    if (filters.value.keyword && filters.value.searchBody) params.searchBody = true
    const data = await getQuestions(params)
    if (seq !== loadSeq) return
    questions.value = Array.isArray(data.list) ? data.list : []
    total.value = typeof data.total === 'number' ? data.total : 0
    if (!isFiltered.value) baseTotal.value = total.value
    loadError.value = ''
  } catch (error) {
    if (seq !== loadSeq) return
    const message = (error && error.message) || '加载失败，请稍后重试'
    if (questions.value.length === 0) {
      loadError.value = message
    } else {
      staleError.value = message
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

async function loadTaxonomy() {
  taxonomyError.value = false
  try {
    const [categoryData, tagData] = await Promise.all([getCategories(), getTags()])
    categories.value = Array.isArray(categoryData) ? categoryData : []
    tags.value = Array.isArray(tagData) ? tagData : []
  } catch {
    taxonomyError.value = true
  }
}

/* ---------------------------------------------------------------------------
 * 行为
 * ------------------------------------------------------------------------- */
function goDetail(question) {
  router.push({
    name: 'question-detail',
    params: { id: question.id },
    query: { ...buildQuery(filters.value), ctx: 'list' }
  })
}

function randomParams() {
  const params = {}
  if (filters.value.categoryId) params.categoryId = filters.value.categoryId
  if (filters.value.tagId) params.tagId = filters.value.tagId
  if (filters.value.difficulty) params.difficulty = filters.value.difficulty
  return params
}

async function goRandomPractice() {
  if (randomLoading.value) return
  randomLoading.value = true
  try {
    const data = await getRandomQuestion(randomParams())
    if (!data || !data.id) {
      ElMessage.info('当前筛选下没有可练习的题目')
      return
    }
    router.push({
      name: 'question-detail',
      params: { id: data.id },
      query: { ...randomParams(), from: 'random' }
    })
  } catch (error) {
    ElMessage.error((error && error.message) || '加载失败，请稍后重试')
  } finally {
    randomLoading.value = false
  }
}

/** 与首页「今日计划」同源：先续做进行中的会话，否则按 mode=today 新建 */
async function startTodayPlan() {
  if (todayLoading.value) return
  if (!isLogin.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  todayLoading.value = true
  try {
    const current = await getCurrentSession()
    if (current && current.status === 1 && current.currentQuestionId) {
      router.push({
        name: 'question-detail',
        params: { id: current.currentQuestionId },
        query: { from: 'session' }
      })
      return
    }
    const created = await createSession({ mode: 'today', size: 20 })
    if (!created || !created.currentQuestionId) {
      ElMessage.info('今日暂无待练习题目，去题库挑一道吧')
      return
    }
    router.push({
      name: 'question-detail',
      params: { id: created.currentQuestionId },
      query: { from: 'session' }
    })
  } catch (error) {
    ElMessage.error((error && error.message) || '创建今日计划失败，请稍后重试')
  } finally {
    todayLoading.value = false
  }
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制地址栏')
  }
}

async function toggleFavorite(question) {
  if (!isLogin.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  const next = !question.favorite
  question.favorite = next
  try {
    if (next) {
      await favoriteQuestion(question.id)
    } else {
      await unfavoriteQuestion(question.id)
    }
  } catch (error) {
    question.favorite = !next
    ElMessage.error((error && error.message) || '操作失败，请重试')
  }
}

/* ---------------------------------------------------------------------------
 * 生命周期：URL 变化 → 重读筛选 → 重新加载
 * ------------------------------------------------------------------------- */
watch(
  () => route.query,
  () => {
    if (route.path !== '/question') return
    // v3.16：不再比较 filters 是否变化 —— applyFilters 会先改 filters 再改 URL，
    // 比较会恒等，导致页内改筛选（下拉/深度开关）不重新加载。
    readQuery()
    loadQuestions()
  }
)

onMounted(() => {
  readQuery()
  loadTaxonomy()
  loadQuestions()
})
</script>

<style scoped>
.question-list {
  min-width: 0;
}

/* ---------------------------------------------------------------------------
   卡片 A：题库身份卡
   --------------------------------------------------------------------------- */
.list-hero {
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

.list-hero__cover {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 128px;
  height: 128px;
}

.list-hero__cover :deep(.category-icon) {
  width: 96px;
  height: 96px;
  border-radius: 22px;
}

.list-hero__cover :deep(.category-icon__svg) {
  /* 与 40px 网格图标保持同一占比（36/40 = 90% 的 svg，可见约 81%），
     96 × 0.81 ≈ 78px；hero 圆角 22px 比网格的 12px 相对更小，留白更充裕 */
  width: 78px;
  height: 78px;
}

.list-hero__main {
  min-width: 0;
}

.list-hero__title {
  margin: 0;
  color: var(--color-text);
  font-size: var(--font-size-h1);
  font-weight: 600;
  line-height: var(--line-height-h1);
}

.list-hero__desc {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.list-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.list-hero__action {
  min-height: 36px;
  margin-left: 0;
  padding: 0 var(--space-4);
  border-radius: var(--radius-button);
}

/* ---------------------------------------------------------------------------
   卡片 B：题目列表面板
   --------------------------------------------------------------------------- */
.list-panel {
  margin-top: var(--space-5);
  padding: var(--space-6);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}

.list-panel__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.list-panel__title {
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 600;
  line-height: var(--line-height-body);
}

.list-panel__filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin-left: auto;
}

.list-panel__search {
  width: 240px;
}

.list-panel__search :deep(.el-input__wrapper),
.list-panel__select :deep(.el-select__wrapper) {
  min-height: 36px;
  border-radius: var(--radius-button);
}

.list-panel__select {
  width: 140px;
}

.list-panel__select--sm {
  width: 110px;
}

.list-panel__select--tags {
  width: 160px;
}

.list-panel__history {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.list-panel__history-label {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

.list-panel__history-item,
.list-panel__history-clear {
  min-height: 28px;
  padding: 0 var(--space-2);
  color: var(--color-text);
  font-size: var(--font-size-aux);
  background-color: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-tag);
  cursor: pointer;
}

.list-panel__history-item:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.list-panel__history-clear {
  background: transparent;
  border: 0;
  color: var(--color-text-secondary);
}

.list-panel__history-clear:hover {
  color: var(--color-danger);
}

/* v3.15：深度搜索开关（仅关键词存在时出现） */
.list-panel__deep {
  height: 36px;
  margin-right: var(--space-1);
}

.list-panel__deep :deep(.el-checkbox__label) {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

.list-panel__meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.list-panel__retry {
  min-height: 28px;
}

.list-panel__skeleton {
  margin-top: var(--space-3);
  padding: var(--space-2) 0;
}

.list-panel__stale {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-2);
  color: var(--color-warning);
  font-size: var(--font-size-aux);
}

.list-panel__loading {
  height: 2px;
  margin-top: var(--space-2);
  background: linear-gradient(90deg, transparent, var(--color-primary), transparent);
  background-size: 40% 100%;
  background-repeat: no-repeat;
  animation: list-loading 1.1s linear infinite;
}

@keyframes list-loading {
  from {
    background-position: -40% 0;
  }
  to {
    background-position: 140% 0;
  }
}

/* ---------------------------------------------------------------------------
   表格式列表（≥1024）：标记 | 题目 | 难度 | 标签
   --------------------------------------------------------------------------- */
.question-table {
  margin-top: var(--space-3);
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-button);
}

.question-table__head,
.question-table__row {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) 64px minmax(0, 260px);
  gap: var(--space-4);
  align-items: center;
  padding: var(--space-3) var(--space-4);
}

.question-table--guest .question-table__head,
.question-table--guest .question-table__row {
  grid-template-columns: minmax(0, 1fr) 64px minmax(0, 260px);
}

.question-table__head {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  background-color: var(--color-bg-page);
  border-bottom: 1px solid var(--color-border);
}

.question-table__row {
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  transition: background-color var(--motion-fast) var(--motion-ease);
}

.question-table__row:last-child {
  border-bottom: 0;
}

.question-table__row:hover,
.question-table__row:focus-visible {
  background-color: var(--color-bg-page);
  outline: none;
}

.question-table__row:focus-visible {
  box-shadow: inset 0 0 0 2px var(--color-primary);
}

.question-table__col--mark {
  text-align: center;
}

.question-table__mark {
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

.question-table__mark:hover,
.question-table__mark--on {
  color: var(--color-warning);
}

.question-table__title {
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 500;
  line-height: var(--line-height-body);
  overflow-wrap: anywhere;
  transition: color var(--motion-fast) var(--motion-ease);
}

/* v4.1 已读标记（纯颜色语义，不加图标）：
   未访问 = 主色蓝，提示「这题你还没打开过」；已访问 = 绿色，表示「看过了」。
   两者都必须压过行 hover 的蓝色，否则「已读」一悬停就又变回蓝的，语义就丢了。 */
.question-table__title--unvisited,
.question-table__row:hover .question-table__title--unvisited {
  color: var(--color-primary);
}

.question-table__title--visited,
.question-table__row:hover .question-table__title--visited {
  /* 正文级绿色：--color-success 对白底仅 3.09:1，16px 标题不够，用深一档的 --color-success-text */
  color: var(--color-success-text);
}

/* v3.15：命中位置标注（标题/标签/答案） */
.question-table__match {
  flex-shrink: 0;
  padding: 0 var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 18px;
  white-space: nowrap;
  background-color: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-tag);
}

.question-table__difficulty {
  font-size: var(--font-size-aux);
  font-weight: 600;
  line-height: var(--line-height-aux);
}

.question-table__difficulty--1 {
  color: var(--color-success);
}

.question-table__difficulty--2 {
  color: var(--color-warning);
}

.question-table__difficulty--3 {
  color: var(--color-danger);
}

.question-table__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  min-width: 0;
}

.question-table__tag {
  padding: 1px var(--space-2);
  color: var(--color-text-secondary);
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
  background-color: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-tag);
}

.question-table__tag--more {
  border-style: dashed;
}

.question-list__cards {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-3);
}

@media (max-width: 1023px) {
  .list-hero {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-4);
    padding: var(--space-4);
  }

  .list-hero__cover {
    width: auto;
    height: auto;
    justify-content: flex-start;
  }

  .list-hero__cover :deep(.category-icon) {
    width: 64px;
    height: 64px;
    border-radius: 16px;
  }

  .list-hero__cover :deep(.category-icon__svg) {
    /* 移动端 64px 外框，同比例 81% ≈ 52px */
    width: 52px;
    height: 52px;
  }

  .list-panel {
    padding: var(--space-4);
  }

  .list-panel__filters {
    width: 100%;
    margin-left: 0;
  }

  .list-panel__search {
    flex: 1 1 160px;
    width: auto;
  }

  .list-panel__select {
    flex: 1 1 110px;
    width: auto;
  }

  .list-panel__history-item,
  .list-panel__history-clear {
    min-height: 44px;
    padding: 0 var(--space-3);
  }
}
</style>
