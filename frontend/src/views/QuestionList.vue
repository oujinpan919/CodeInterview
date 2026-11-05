<template>
  <section class="question-list">
    <div class="question-list__layout">
      <aside v-if="isWide" class="question-list__sidebar" aria-label="筛选条件">
        <div class="filter-block">
          <h3 class="filter-block__title">分类</h3>
          <el-radio-group
            :model-value="filters.categoryId"
            class="filter-block__group filter-block__group--column"
            @change="handleCategoryChange"
          >
            <el-radio value="">全部</el-radio>
            <el-radio v-for="category in categories" :key="category.id" :value="category.id">
              {{ category.name }}（{{ category.questionCount }}）
            </el-radio>
          </el-radio-group>
        </div>

        <div class="filter-block">
          <h3 class="filter-block__title">难度</h3>
          <el-radio-group
            :model-value="filters.difficulty"
            class="filter-block__group filter-block__group--column"
            @change="handleDifficultyChange"
          >
            <el-radio value="">全部</el-radio>
            <el-radio value="1">简单</el-radio>
            <el-radio value="2">中等</el-radio>
            <el-radio value="3">困难</el-radio>
          </el-radio-group>
        </div>

        <div class="filter-block">
          <h3 class="filter-block__title">标签</h3>
          <el-select
            :model-value="filters.tagId"
            class="filter-block__select"
            placeholder="全部标签"
            clearable
            filterable
            @change="handleTagChange"
          >
            <el-option
              v-for="tag in tags"
              :key="tag.id"
              :label="`${tag.name}（${tag.questionCount}）`"
              :value="tag.id"
            />
          </el-select>
        </div>

        <p v-if="taxonomyLoading" class="filter-tip">分类与标签加载中…</p>
        <p v-else-if="taxonomyError" class="filter-tip filter-tip--error">
          分类与标签加载失败
          <el-button link type="primary" @click="loadTaxonomy">重试</el-button>
        </p>

        <el-button class="filter-block__clear" @click="clearFilters">清空筛选</el-button>
      </aside>

      <div ref="contentRef" class="question-list__content">
        <div class="question-list__toolbar">
          <el-input
            v-model="searchInput"
            class="question-list__search"
            placeholder="搜索题目或标签"
            maxlength="30"
            clearable
            @keyup.enter="handleSearchEnter"
          />

          <div class="question-list__toolbar-row">
            <el-radio-group :model-value="filters.sort" @change="handleSortChange">
              <el-radio-button value="latest">最新</el-radio-button>
              <el-radio-button value="hot">热门</el-radio-button>
            </el-radio-group>
            <el-button
              v-if="!isWide"
              class="question-list__filter-button"
              @click="filterDrawer = true"
            >
              筛选
            </el-button>
          </div>

          <p class="question-list__count">共 {{ total }} 题</p>
        </div>

        <div v-if="!isWide && activeChips.length > 0" class="question-list__chips">
          <button
            v-for="chip in activeChips"
            :key="chip.key"
            type="button"
            class="question-list__chip"
            @click="removeChip(chip.key)"
          >
            {{ chip.label }}
            <span class="question-list__chip-close" aria-hidden="true">×</span>
          </button>
          <button type="button" class="question-list__chip-clear" @click="clearFilters">
            清空
          </button>
        </div>

        <div v-if="loading && questions.length > 0" class="question-list__loading" aria-hidden="true" />

        <div v-if="staleError" class="question-list__stale" role="alert">
          <span>{{ staleError }}</span>
          <el-button link type="primary" @click="retryLoad">重试</el-button>
        </div>

        <div v-if="showSkeleton" class="question-list__cards">
          <div v-for="item in 6" :key="`skeleton-${item}`" class="question-skeleton">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="h3" class="question-skeleton__title" />
                <el-skeleton-item variant="text" class="question-skeleton__meta" />
                <el-skeleton-item variant="text" class="question-skeleton__line" />
              </template>
            </el-skeleton>
          </div>
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

        <div v-else class="question-list__cards">
          <QuestionCard
            v-for="question in questions"
            :key="question.id"
            :question="question"
            @click="goDetail"
          />
        </div>

        <PageBar
          v-if="questions.length > 0"
          :page="filters.page"
          :size="pageSize"
          :total="total"
          @change="handlePageChange"
        />
      </div>
    </div>

    <el-drawer v-model="filterDrawer" direction="btt" size="80%" title="筛选">
      <div class="filter-drawer">
        <div class="filter-block">
          <h3 class="filter-block__title">分类</h3>
          <el-radio-group
            :model-value="filters.categoryId"
            class="filter-block__group filter-block__group--column"
            @change="handleCategoryChange"
          >
            <el-radio value="">全部</el-radio>
            <el-radio v-for="category in categories" :key="`drawer-${category.id}`" :value="category.id">
              {{ category.name }}（{{ category.questionCount }}）
            </el-radio>
          </el-radio-group>
        </div>

        <div class="filter-block">
          <h3 class="filter-block__title">难度</h3>
          <el-radio-group
            :model-value="filters.difficulty"
            class="filter-block__group filter-block__group--column"
            @change="handleDifficultyChange"
          >
            <el-radio value="">全部</el-radio>
            <el-radio value="1">简单</el-radio>
            <el-radio value="2">中等</el-radio>
            <el-radio value="3">困难</el-radio>
          </el-radio-group>
        </div>

        <div class="filter-block">
          <h3 class="filter-block__title">标签</h3>
          <el-select
            :model-value="filters.tagId"
            class="filter-block__select"
            placeholder="全部标签"
            clearable
            filterable
            @change="handleTagChange"
          >
            <el-option
              v-for="tag in tags"
              :key="`drawer-${tag.id}`"
              :label="`${tag.name}（${tag.questionCount}）`"
              :value="tag.id"
            />
          </el-select>
        </div>

        <p v-if="taxonomyError" class="filter-tip filter-tip--error">
          分类与标签加载失败
          <el-button link type="primary" @click="loadTaxonomy">重试</el-button>
        </p>
      </div>

      <template #footer>
        <div class="filter-drawer__footer">
          <el-button @click="clearFilters">清空筛选</el-button>
          <el-button type="primary" @click="filterDrawer = false">完成</el-button>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDebounceFn, useMediaQuery } from '@vueuse/core'
import { getQuestions } from '@/api/question'
import { getCategories, getTags } from '@/api/taxonomy'
import { usePagination } from '@/composables/usePagination'
import QuestionCard from '@/components/QuestionCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import PageBar from '@/components/PageBar.vue'

const PAGE_SIZE = 10
const DIFFICULTY_LABELS = { 1: '简单', 2: '中等', 3: '困难' }

const route = useRoute()
const router = useRouter()

const isWide = useMediaQuery('(min-width: 1024px)')

const contentRef = ref(null)
const filterDrawer = ref(false)
const searchInput = ref('')

const questions = ref([])
const total = ref(0)
const loading = ref(false)
const loadError = ref('')
const staleError = ref('')

const categories = ref([])
const tags = ref([])
const taxonomyLoading = ref(true)
const taxonomyError = ref(false)

const filters = reactive({
  categoryId: '',
  tagId: '',
  difficulty: '',
  keyword: '',
  sort: 'latest',
  page: 1
})

const pageSize = PAGE_SIZE
const pageNumber = computed(() => filters.page)
const { clampPage } = usePagination({
  page: pageNumber,
  pageSize: computed(() => PAGE_SIZE),
  total
})

const showSkeleton = computed(() => loading.value && questions.value.length === 0)

const categoryName = computed(() => {
  const found = categories.value.find((item) => item.id === filters.categoryId)
  return found ? found.name : ''
})

const tagName = computed(() => {
  const found = tags.value.find((item) => item.id === filters.tagId)
  return found ? found.name : ''
})

const activeChips = computed(() => {
  const chips = []
  if (filters.categoryId) {
    chips.push({ key: 'categoryId', label: `分类：${categoryName.value || filters.categoryId}` })
  }
  if (filters.tagId) {
    chips.push({ key: 'tagId', label: `标签：${tagName.value || filters.tagId}` })
  }
  if (filters.difficulty) {
    chips.push({ key: 'difficulty', label: `难度：${DIFFICULTY_LABELS[filters.difficulty] || ''}` })
  }
  if (filters.keyword) {
    chips.push({ key: 'keyword', label: `关键词：${filters.keyword}` })
  }
  return chips
})

function pickQueryString(value) {
  if (Array.isArray(value)) return typeof value[0] === 'string' ? value[0] : ''
  return typeof value === 'string' ? value : ''
}

// 不用 Number()/parseInt()（02 §8 id 红线检查的同款约束），page 是纯数字串
function toPositivePage(value) {
  const raw = pickQueryString(value)
  if (!/^\d+$/.test(raw)) return 1
  const parsed = raw * 1
  return parsed > 0 ? parsed : 1
}

function readQuery() {
  const query = route.query
  filters.categoryId = pickQueryString(query.categoryId)
  filters.tagId = pickQueryString(query.tagId)
  const difficulty = pickQueryString(query.difficulty)
  filters.difficulty = ['1', '2', '3'].includes(difficulty) ? difficulty : ''
  filters.keyword = pickQueryString(query.keyword).slice(0, 30)
  filters.sort = query.sort === 'hot' ? 'hot' : 'latest'
  filters.page = toPositivePage(query.page)
  // 先写 filters 再回填输入框，搜索 watcher 依靠等值判断不会反向提交
  searchInput.value = filters.keyword
}

function buildQuery(next) {
  const query = {}
  if (next.categoryId) query.categoryId = next.categoryId
  if (next.tagId) query.tagId = next.tagId
  if (next.difficulty) query.difficulty = next.difficulty
  if (next.keyword) query.keyword = next.keyword
  if (next.sort === 'hot') query.sort = 'hot'
  if (next.page > 1) query.page = String(next.page)
  return query
}

/** 所有筛选/排序/分页都写进 URL query（router.replace，不产生历史栈） */
function applyFilters(patch, resetPage = true) {
  const next = { ...filters, ...patch }
  if (resetPage) next.page = 1
  router.replace({ path: '/question', query: buildQuery(next) })
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
  applyFilters({ sort: value === 'hot' ? 'hot' : 'latest' })
}

function handleSearchEnter() {
  const keyword = searchInput.value.trim()
  if (keyword !== filters.keyword) applyFilters({ keyword })
}

function clearFilters() {
  searchInput.value = ''
  applyFilters({ categoryId: '', tagId: '', difficulty: '', keyword: '' })
}

function removeChip(key) {
  const patch = {}
  patch[key] = ''
  applyFilters(patch)
}

function handlePageChange(next) {
  const page = clampPage(next)
  if (page === filters.page) return
  applyFilters({ page }, false)
  if (contentRef.value) contentRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function goDetail(question) {
  router.push({ name: 'question-detail', params: { id: question.id } })
}

function retryLoad() {
  loadQuestions()
}

// 搜索 300ms 防抖（04 B4.2）
const commitKeyword = useDebounceFn(() => {
  const keyword = searchInput.value.trim()
  if (keyword === filters.keyword) return
  applyFilters({ keyword })
}, 300)

watch(searchInput, () => {
  commitKeyword()
})

let loadSeq = 0

async function loadQuestions() {
  const seq = ++loadSeq
  loading.value = true
  staleError.value = ''
  const params = { page: filters.page, pageSize: PAGE_SIZE, sort: filters.sort }
  if (filters.categoryId) params.categoryId = filters.categoryId
  if (filters.tagId) params.tagId = filters.tagId
  if (filters.difficulty) params.difficulty = filters.difficulty
  if (filters.keyword) params.keyword = filters.keyword
  try {
    const data = await getQuestions(params)
    if (seq !== loadSeq) return
    questions.value = Array.isArray(data.list) ? data.list : []
    total.value = typeof data.total === 'number' ? data.total : 0
    loadError.value = ''
  } catch (error) {
    if (seq !== loadSeq) return
    const message = (error && error.message) || '加载失败，请稍后重试'
    if (questions.value.length > 0) {
      staleError.value = message
    } else {
      loadError.value = message
      questions.value = []
      total.value = 0
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

async function loadTaxonomy() {
  taxonomyLoading.value = true
  taxonomyError.value = false
  try {
    const [categoryList, tagList] = await Promise.all([getCategories(), getTags()])
    categories.value = Array.isArray(categoryList) ? categoryList : []
    tags.value = Array.isArray(tagList) ? tagList : []
  } catch {
    taxonomyError.value = true
  } finally {
    taxonomyLoading.value = false
  }
}

// 从 URL 还原筛选条件并拉取列表（含首次进入 / 浏览器前进后退）
watch(
  () => route.query,
  () => {
    readQuery()
    loadQuestions()
  },
  { immediate: true }
)

onMounted(() => {
  loadTaxonomy()
})
</script>

<style scoped>
.question-list__layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-6);
  align-items: start;
}

.question-list__sidebar {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  max-height: calc(100vh - var(--header-height) - var(--space-6) * 2);
  padding: var(--space-5);
  overflow-y: auto;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

@media (min-width: 1024px) {
  .question-list__layout {
    grid-template-columns: 240px minmax(0, 1fr);
  }
}

.filter-block + .filter-block {
  margin-top: var(--space-5);
}

.filter-block__title {
  margin-bottom: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  font-weight: 600;
}

.filter-block__group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.filter-block__group--column {
  flex-direction: column;
  align-items: flex-start;
}

.filter-block__group :deep(.el-radio) {
  min-height: 44px;
  margin-right: 0;
  color: var(--color-text);
}

.filter-block__select {
  width: 100%;
}

.filter-block__clear {
  width: 100%;
  min-height: 44px;
  margin-top: var(--space-5);
  border-radius: var(--radius-button);
}

.filter-tip {
  margin-top: var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.filter-tip--error {
  color: var(--color-danger);
}

.question-list__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  padding: var(--space-4);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.question-list__search {
  width: 320px;
  max-width: 100%;
}

.question-list__search :deep(.el-input__wrapper) {
  min-height: 40px;
  border-radius: var(--radius-button);
}

.question-list__toolbar-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.question-list__filter-button {
  min-height: 40px;
  border-radius: var(--radius-button);
}

.question-list__count {
  margin-left: auto;
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.question-list__chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}

.question-list__chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  min-height: 36px;
  padding: 0 var(--space-3);
  color: var(--color-primary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-tag);
}

.question-list__chip-close {
  font-size: 14px;
  line-height: 1;
}

.question-list__chip-clear {
  min-height: 36px;
  padding: 0 var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  background: transparent;
  border: 0;
}

.question-list__loading {
  height: 3px;
  margin-bottom: var(--space-3);
  overflow: hidden;
  background-color: var(--color-border);
  border-radius: var(--radius-tag);
}

.question-list__loading::after {
  display: block;
  width: 40%;
  height: 100%;
  background-color: var(--color-primary);
  border-radius: var(--radius-tag);
  content: '';
  animation: question-list-loading 1s ease-in-out infinite;
}

@keyframes question-list-loading {
  0% {
    transform: translateX(-100%);
  }

  100% {
    transform: translateX(260%);
  }
}

.question-list__stale {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
  padding: var(--space-2) var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-tag);
}

.question-list__cards {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.question-skeleton {
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.question-skeleton__title {
  width: 60%;
}

.question-skeleton__meta {
  width: 35%;
  margin-top: var(--space-3);
}

.question-skeleton__line {
  width: 100%;
  margin-top: var(--space-2);
}

.filter-drawer :deep(.el-radio) {
  min-height: 44px;
  margin-right: 0;
}

.filter-drawer__footer {
  display: flex;
  gap: var(--space-3);
}

.filter-drawer__footer :deep(.el-button) {
  flex: 1;
  min-height: 44px;
  margin-left: 0;
  border-radius: var(--radius-button);
}

@media (max-width: 1023px) {
  .question-list__search {
    flex: 1 1 240px;
  }
}

@media (max-width: 767px) {
  .question-list__toolbar {
    flex-direction: column;
    align-items: stretch;
    padding: var(--space-3);
  }

  .question-list__search {
    width: 100%;
    flex: 0 0 auto;
  }

  .question-list__search :deep(.el-input__wrapper) {
    min-height: 44px;
  }

  .question-list__toolbar-row {
    justify-content: space-between;
  }

  .question-list__filter-button {
    min-height: 44px;
    padding: 0 var(--space-4);
  }

  .question-list__toolbar :deep(.el-radio-button__inner) {
    display: inline-flex;
    align-items: center;
    min-height: 44px;
  }

  .question-list__count {
    margin-left: 0;
  }

  .question-list__chip,
  .question-list__chip-clear {
    min-height: 44px;
  }

  .question-skeleton {
    padding: var(--space-4);
  }
}
</style>
