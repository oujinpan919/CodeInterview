<template>
  <section class="records">
    <header class="records__head">
      <h1 class="records__title">刷题记录</h1>
    </header>

    <section class="records__list-card">
      <div class="records__filter">
        <el-radio-group v-model="selfAssessFilter" @change="handleFilterChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="1">掌握</el-radio-button>
          <el-radio-button value="2">模糊</el-radio-button>
          <el-radio-button value="3">不会</el-radio-button>
        </el-radio-group>
      </div>

      <div v-if="loading && records.length === 0" class="records__skeleton" aria-busy="true">
        <el-skeleton :rows="4" animated />
      </div>

      <ErrorState
        v-else-if="loadError && records.length === 0"
        :message="loadError"
        @retry="loadRecords"
      />

      <EmptyState
        v-else-if="records.length === 0"
        title="还没有刷题记录"
        description="去题库做一道题，自评后会出现在这里"
        action-text="去刷题"
        @action="goQuestionList"
      />

      <template v-else>
        <el-table
          v-if="!isMobile"
          :data="records"
          class="records__table"
          row-key="id"
          :show-overflow-tooltip="true"
        >
          <el-table-column prop="title" label="题目" min-width="280">
            <template #default="{ row }">
              <RouterLink
                class="records__title-link"
                :to="{ name: 'question-detail', params: { id: row.questionId } }"
              >
                {{ row.title }}
              </RouterLink>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" width="120" />
          <el-table-column label="自评" width="100">
            <template #default="{ row }">
              <el-tag :type="assessType(row.selfAssess)" size="small" effect="plain">
                {{ assessLabel(row.selfAssess) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="120">
            <template #default="{ row }">{{ sourceLabel(row.source) }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="时间" width="180" />
        </el-table>

        <ul v-else class="records__cards">
          <li v-for="row in records" :key="row.id" class="records__card">
            <RouterLink
              class="records__card-title"
              :to="{ name: 'question-detail', params: { id: row.questionId } }"
            >
              {{ row.title }}
            </RouterLink>
            <div class="records__card-meta">
              <el-tag :type="assessType(row.selfAssess)" size="small" effect="plain">
                {{ assessLabel(row.selfAssess) }}
              </el-tag>
              <span class="records__card-item">{{ row.categoryName }}</span>
              <span class="records__card-item">{{ sourceLabel(row.source) }}</span>
            </div>
            <p class="records__card-time">{{ row.createdAt }}</p>
          </li>
        </ul>

        <PageBar :page="page" :size="PAGE_SIZE" :total="total" @change="handlePageChange" />
      </template>
    </section>

    <p class="records__note">统计口径：自评记录，非考试判分</p>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { getPracticeRecords } from '@/api/record'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import PageBar from '@/components/PageBar.vue'

const PAGE_SIZE = 10

const ASSESS_LABELS = { 1: '掌握', 2: '模糊', 3: '不会' }
const ASSESS_TYPES = { 1: 'success', 2: 'warning', 3: 'danger' }
const SOURCE_LABELS = { 1: '详情页', 2: '随机刷题', 3: '错题本' }

const router = useRouter()
const isMobile = useMediaQuery('(max-width: 767px)')

const records = ref([])
const total = ref(0)
const page = ref(1)
const selfAssessFilter = ref('')
const loading = ref(false)
const loadError = ref('')

let loadSeq = 0

function assessLabel(value) {
  return ASSESS_LABELS[value] || '—'
}

function assessType(value) {
  return ASSESS_TYPES[value] || 'info'
}

function sourceLabel(value) {
  return SOURCE_LABELS[value] || '—'
}

async function loadRecords() {
  const seq = ++loadSeq
  loading.value = true
  const params = { page: page.value, pageSize: PAGE_SIZE }
  if (selfAssessFilter.value) params.selfAssess = selfAssessFilter.value
  try {
    const data = await getPracticeRecords(params)
    if (seq !== loadSeq) return
    records.value = Array.isArray(data.list) ? data.list : []
    total.value = typeof data.total === 'number' ? data.total : 0
    loadError.value = ''
  } catch (error) {
    if (seq !== loadSeq) return
    if (records.value.length === 0) {
      loadError.value = (error && error.message) || '加载失败，请稍后重试'
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

function handleFilterChange() {
  page.value = 1
  loadRecords()
}

function handlePageChange(next) {
  page.value = next
  loadRecords()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function goQuestionList() {
  router.push('/question')
}

// T11 会在本页加统计卡与图表；记录列表保持独立加载
watch(selfAssessFilter, () => {
  page.value = 1
})

onMounted(loadRecords)
</script>

<style scoped>
.records__head {
  margin-bottom: var(--space-6);
}

.records__title {
  color: var(--color-text);
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
  font-weight: 600;
}

.records__list-card {
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.records__filter {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.records__skeleton {
  padding: var(--space-4) 0;
}

.records__title-link {
  color: var(--color-primary);
  overflow-wrap: anywhere;
}

.records__cards {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.records__card {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-button);
}

.records__card-title {
  display: block;
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
  overflow-wrap: anywhere;
}

.records__card-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-3);
  margin-top: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.records__card-time {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.records__note {
  margin-top: var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  text-align: center;
}

@media (max-width: 767px) {
  .records__head {
    margin-bottom: var(--space-4);
  }

  .records__list-card {
    padding: var(--space-4);
  }

  .records__filter :deep(.el-radio-button__inner) {
    display: inline-flex;
    align-items: center;
    min-height: 44px;
    padding: 0 var(--space-3);
  }
}
</style>
