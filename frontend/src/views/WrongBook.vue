<template>
  <section ref="topRef" class="wrong-book">
    <header class="wrong-book__head">
      <h1 class="wrong-book__title">错题本 · 复习队列</h1>
      <p class="wrong-book__hint">
        所有自评过的题都会进入复习队列：模糊/不会回到「在学」，掌握后按 1/3/7 天回顾，连续 4 次掌握退出队列
      </p>
    </header>

    <section class="wrong-book__card">
      <el-tabs v-model="tab" class="wrong-book__tabs" @tab-change="handleTabChange">
        <el-tab-pane name="learning">
          <template #label>
            在学<span v-if="summary.learning" class="wrong-book__badge">{{ summary.learning }}</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="reviewing">
          <template #label>
            待回顾<span v-if="summary.reviewing" class="wrong-book__badge">{{ summary.reviewing }}</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="mastered">
          <template #label>
            真掌握<span v-if="summary.mastered" class="wrong-book__badge">{{ summary.mastered }}</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="all">
          <template #label>
            全部<span v-if="summary.total" class="wrong-book__badge">{{ summary.total }}</span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <div class="wrong-book__filters">
        <span class="wrong-book__filter-label">分类</span>
        <el-select
          v-model="filterCategory"
          class="wrong-book__filter-select"
          placeholder="全部分类"
          clearable
          :disabled="loading"
          @change="handleFilterChange"
        >
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
        <span class="wrong-book__filter-label">难度</span>
        <el-radio-group
          v-model="filterDifficulty"
          :disabled="loading"
          @change="handleFilterChange"
        >
          <el-radio value="">全部</el-radio>
          <el-radio value="1">简单</el-radio>
          <el-radio value="2">中等</el-radio>
          <el-radio value="3">困难</el-radio>
        </el-radio-group>
        <el-button link type="primary" class="wrong-book__filter-clear" @click="clearFilters">
          清空筛选
        </el-button>
      </div>

      <div v-if="loading && items.length === 0" class="wrong-book__skeleton" aria-busy="true">
        <el-skeleton :rows="4" animated />
      </div>

      <ErrorState
        v-else-if="loadError && items.length === 0"
        :message="loadError"
        @retry="loadList"
      />

      <EmptyState
        v-else-if="items.length === 0"
        :title="emptyTitle"
        :description="emptyDescription"
        :action-text="hasActiveFilters ? '清空筛选' : '去刷题'"
        @action="hasActiveFilters ? clearFilters : goQuestionList"
      />

      <template v-else>
        <el-table
          v-if="!isMobile"
          :data="items"
          class="wrong-book__table"
          row-key="questionId"
          :show-overflow-tooltip="true"
        >
          <el-table-column label="题目" min-width="260">
            <template #default="{ row }">
              <RouterLink
                class="wrong-book__link"
                :to="{ name: 'question-detail', params: { id: row.questionId } }"
              >
                {{ row.title }}
              </RouterLink>
              <el-tag
                v-if="row.hasNote"
                class="wrong-book__note-tag"
                size="small"
                type="info"
                effect="plain"
              >
                笔记
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" width="110" />
          <el-table-column label="难度" width="90">
            <template #default="{ row }">
              <DifficultyTag :value="row.difficulty" />
            </template>
          </el-table-column>
          <el-table-column label="自评" width="90">
            <template #default="{ row }">
              <el-tag :type="assessType(row.lastAssess)" size="small" effect="plain">
                {{ assessLabel(row.lastAssess) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="wrongCount" label="错次" width="80" />
          <el-table-column label="下次复习" width="130">
            <template #default="{ row }">
              <span :class="dueClass(row)">{{ dueText(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <div class="wrong-book__actions">
                <el-tooltip
                  :disabled="row.status !== 2"
                  content="题目已下架"
                  placement="top"
                >
                  <span>
                    <el-button
                      class="wrong-book__action"
                      type="primary"
                      link
                      :disabled="row.status === 2"
                      @click="goReview(row)"
                    >
                      去复习
                    </el-button>
                  </span>
                </el-tooltip>
                <el-button
                  v-if="row.stage !== 'mastered'"
                  class="wrong-book__action"
                  type="success"
                  link
                  :loading="actingId === row.questionId"
                  @click="markMastered(row)"
                >
                  标记掌握
                </el-button>
                <el-button
                  class="wrong-book__action wrong-book__action--danger"
                  type="danger"
                  link
                  :loading="actingId === row.questionId"
                  @click="removeItem(row)"
                >
                  彻底移除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <ul v-else class="wrong-book__cards">
          <li v-for="row in items" :key="row.questionId" class="wrong-book__item">
            <RouterLink
              class="wrong-book__item-title"
              :to="{ name: 'question-detail', params: { id: row.questionId } }"
            >
              {{ row.title }}
            </RouterLink>
            <div class="wrong-book__item-meta">
              <DifficultyTag :value="row.difficulty" />
              <el-tag :type="assessType(row.lastAssess)" size="small" effect="plain">
                {{ assessLabel(row.lastAssess) }}
              </el-tag>
              <span class="wrong-book__item-text">{{ row.categoryName }}</span>
              <span class="wrong-book__item-text">错 {{ row.wrongCount }} 次</span>
              <span v-if="row.hasNote" class="wrong-book__item-text">有笔记</span>
            </div>
            <p class="wrong-book__item-time">
              下次复习：<span :class="dueClass(row)">{{ dueText(row) }}</span>
            </p>
            <div class="wrong-book__item-actions">
              <el-button
                class="wrong-book__action"
                type="primary"
                :disabled="row.status === 2"
                @click="goReview(row)"
              >
                去复习
              </el-button>
              <el-button
                v-if="row.stage !== 'mastered'"
                class="wrong-book__action"
                type="success"
                plain
                :loading="actingId === row.questionId"
                @click="markMastered(row)"
              >
                标记掌握
              </el-button>
              <el-button
                class="wrong-book__action"
                type="danger"
                plain
                :loading="actingId === row.questionId"
                @click="removeItem(row)"
              >
                彻底移除
              </el-button>
            </div>
            <p v-if="row.status === 2" class="wrong-book__offline">题目已下架，仅供查看</p>
          </li>
        </ul>

        <PageBar :page="page" :size="PAGE_SIZE" :total="total" @change="handlePageChange" />
      </template>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import { getWrongBook, getWrongBookSummary, markWrongResolved, removeWrong } from '@/api/record'
import { getCategories } from '@/api/taxonomy'
import DifficultyTag from '@/components/DifficultyTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import PageBar from '@/components/PageBar.vue'

const PAGE_SIZE = 10
const ASSESS_LABELS = { 1: '掌握', 2: '模糊', 3: '不会' }
const ASSESS_TYPES = { 1: 'success', 2: 'warning', 3: 'danger' }
/** v3.6 三态视图（01 F-008）：在学 / 已掌握待回顾 / 真掌握 / 全部 */
const TAB_EMPTY = {
  learning: ['错题本是空的，去刷几道题吧', '自评「模糊」或「不会」后会出现在这里'],
  reviewing: ['没有待回顾的题', '连续掌握后会按 1/3/7 天回来回顾一次'],
  mastered: ['还没有真掌握的题', '连续 4 次自评「掌握」即判为真掌握'],
  all: ['还没有任何复习记录', '刷过的题都会进入复习队列']
}

const router = useRouter()
const isMobile = useMediaQuery('(max-width: 767px)')
const topRef = ref(null)

const tab = ref('learning')
const items = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const loadError = ref('')
const actingId = ref('')
const filterCategory = ref('')
const filterDifficulty = ref('')
const categories = ref([])
const summary = ref({ learning: 0, reviewing: 0, mastered: 0, total: 0 })

const hasActiveFilters = computed(() =>
  Boolean(filterCategory.value || filterDifficulty.value)
)
const emptyTitle = computed(() =>
  hasActiveFilters.value ? '没有匹配的题目' : TAB_EMPTY[tab.value][0]
)
const emptyDescription = computed(() =>
  hasActiveFilters.value ? '换个分类或难度，或者清空筛选' : TAB_EMPTY[tab.value][1]
)

let loadSeq = 0

function assessLabel(value) {
  return ASSESS_LABELS[value] || '—'
}

function assessType(value) {
  return ASSESS_TYPES[value] || 'info'
}

/** 到期文案（01 F-008）：逾期最久优先，其次是今天 / N 天后 */
function dueText(row) {
  if (row.stage === 'mastered') return '已掌握'
  if (!row.dueAt) return '未排程'
  const due = dayjs(row.dueAt)
  if (!due.isValid()) return '—'
  const days = due.startOf('day').diff(dayjs().startOf('day'), 'day')
  if (days < 0) return `逾期 ${-days} 天`
  if (days === 0) return '今天'
  return `${days} 天后`
}

function dueClass(row) {
  if (row.stage === 'mastered') return 'wrong-book__due--done'
  if (!row.dueAt) return 'wrong-book__due'
  const days = dayjs(row.dueAt).startOf('day').diff(dayjs().startOf('day'), 'day')
  return days < 0 ? 'wrong-book__due--overdue' : 'wrong-book__due'
}

async function loadSummary() {
  try {
    const data = await getWrongBookSummary()
    summary.value = {
      learning: Number(data && data.learning) || 0,
      reviewing: Number(data && data.reviewing) || 0,
      mastered: Number(data && data.mastered) || 0,
      total: Number(data && data.total) || 0
    }
  } catch {
    summary.value = { learning: 0, reviewing: 0, mastered: 0, total: 0 }
  }
}

async function loadList() {
  const seq = ++loadSeq
  loading.value = true
  try {
    const params = {
      scope: tab.value,
      page: page.value,
      pageSize: PAGE_SIZE
    }
    if (filterCategory.value) params.categoryId = filterCategory.value
    if (filterDifficulty.value) params.difficulty = filterDifficulty.value
    const data = await getWrongBook(params)
    if (seq !== loadSeq) return
    items.value = Array.isArray(data.list) ? data.list : []
    total.value = typeof data.total === 'number' ? data.total : 0
    loadError.value = ''
  } catch (error) {
    if (seq !== loadSeq) return
    if (items.value.length === 0) {
      loadError.value = (error && error.message) || '加载失败，请稍后重试'
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

function handleTabChange() {
  page.value = 1
  loadList()
}

function handleFilterChange() {
  page.value = 1
  loadList()
}

function clearFilters() {
  filterCategory.value = ''
  filterDifficulty.value = ''
  page.value = 1
  loadList()
}

async function loadCategories() {
  try {
    const data = await getCategories()
    categories.value = Array.isArray(data) ? data : []
  } catch {
    categories.value = []
  }
}

function handlePageChange(next) {
  page.value = next
  loadList()
  // 04 B5.2：切页后滚动到内容区顶部（scrollIntoView）
  if (topRef.value) topRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/** 去复习带 ?from=wrong，详情页据此传 source=3（04 B4.7 / 01 F04）；
 *  v3.6 修1：再带 ctx=wrong + 当前 tab scope，详情页上下题按本视图顺序翻 */
function goReview(row) {
  if (row.status === 2) return
  router.push({
    name: 'question-detail',
    params: { id: row.questionId },
    query: { from: 'wrong', ctx: 'wrong', scope: tab.value }
  })
}

async function markMastered(row) {
  if (actingId.value) return
  actingId.value = row.questionId
  try {
    await markWrongResolved(row.questionId)
    // 成功后从当前 tab 移除（04 B4.7）
    items.value = items.value.filter((item) => item.questionId !== row.questionId)
    total.value = Math.max(0, total.value - 1)
    ElMessage.success('已标记为掌握')
    loadSummary()
    if (items.value.length === 0 && page.value > 1) {
      page.value -= 1
      await loadList()
    }
  } catch (error) {
    // 04 A3.4 #17：无记录返回 40400，前端刷新列表
    if (error && error.code === 40400) {
      await loadList()
    }
    ElMessage.error((error && error.message) || '操作失败，请重试')
  } finally {
    actingId.value = ''
  }
}

async function removeItem(row) {
  if (actingId.value) return
  try {
    await ElMessageBox.confirm('移除后该题的历史错题记录将不可恢复，确定彻底移除吗？', '彻底移除', {
      type: 'warning',
      confirmButtonText: '彻底移除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  actingId.value = row.questionId
  try {
    await removeWrong(row.questionId)
    items.value = items.value.filter((item) => item.questionId !== row.questionId)
    total.value = Math.max(0, total.value - 1)
    ElMessage.success('已彻底移除')
    loadSummary()
    if (items.value.length === 0 && page.value > 1) {
      page.value -= 1
      await loadList()
    }
  } catch (error) {
    ElMessage.error((error && error.message) || '操作失败，请重试')
  } finally {
    actingId.value = ''
  }
}

function goQuestionList() {
  router.push('/question')
}

onMounted(() => {
  loadList()
  loadCategories()
  loadSummary()
})
</script>

<style scoped>
.wrong-book__head {
  margin-bottom: var(--space-6);
}

.wrong-book__title {
  color: var(--color-text);
  font-size: var(--font-size-h1);
  line-height: var(--line-height-h1);
  font-weight: 600;
}

.wrong-book__hint {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.wrong-book__card {
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.wrong-book__skeleton {
  padding: var(--space-4) 0;
}

.wrong-book__link {
  color: var(--color-primary);
  overflow-wrap: anywhere;
}

.wrong-book__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.wrong-book__action {
  min-height: 44px;
  margin-left: 0;
}

.wrong-book__action--danger {
  color: var(--color-danger);
}

.wrong-book__cards {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.wrong-book__item {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-button);
}

.wrong-book__item-title {
  display: block;
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
  overflow-wrap: anywhere;
}

.wrong-book__item-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-3);
  margin-top: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.wrong-book__item-time {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.wrong-book__item-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.wrong-book__item-actions .wrong-book__action {
  flex: 1 1 0;
  min-width: 0;
  margin-left: 0;
}

.wrong-book__offline {
  margin-top: var(--space-2);
  color: var(--color-warning);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

@media (max-width: 767px) {
  .wrong-book__head {
    margin-bottom: var(--space-4);
  }

  .wrong-book__card {
    padding: var(--space-4);
  }
}

.wrong-book__filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-3);
  margin-bottom: var(--space-4);
}

.wrong-book__filter-label {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.wrong-book__filter-select {
  width: 200px;
}

.wrong-book__filter-select :deep(.el-select__wrapper) {
  min-height: 44px;
}

.wrong-book__filters :deep(.el-radio) {
  min-height: 44px;
  margin-right: 0;
}

.wrong-book__filter-clear {
  min-height: 44px;
  margin-left: auto;
}

@media (max-width: 767px) {
  .wrong-book__filter-select {
    width: 100%;
  }

  .wrong-book__filters {
    align-items: flex-start;
  }

  .wrong-book__filter-clear {
    margin-left: 0;
  }
}
/* v3.6：三态视图角标与到期文案 */
.wrong-book__badge {
  display: inline-block;
  min-width: 18px;
  margin-left: var(--space-1);
  padding: 0 5px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 18px;
  text-align: center;
  background-color: var(--color-bg-page);
  border-radius: 9px;
}

.wrong-book__due {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

.wrong-book__due--overdue {
  color: var(--color-danger);
  font-size: var(--font-size-aux);
  font-weight: 600;
}

.wrong-book__due--done {
  color: var(--color-success);
  font-size: var(--font-size-aux);
}
/* v3.6 C6：错题本行显示「笔记」标记（04 C3） */
.wrong-book__note-tag {
  margin-left: var(--space-2);
  border-radius: var(--radius-tag);
}
</style>
