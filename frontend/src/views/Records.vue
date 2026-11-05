<template>
  <section ref="topRef" class="records">
    <header class="records__head">
      <h1 class="records__title">刷题记录</h1>
    </header>

    <section class="records__stats">
      <div class="records__metrics">
        <article v-for="card in metricCards" :key="card.key" class="records__metric">
          <p class="records__metric-label">{{ card.label }}</p>
          <p
            class="records__metric-value"
            :class="{ 'records__metric-value--muted': card.muted }"
            :title="overviewError ? '加载失败' : ''"
          >
            {{ card.value }}
          </p>
          <p v-if="card.hint" class="records__metric-hint">{{ card.hint }}</p>
        </article>
      </div>
      <p v-if="overviewError" class="records__stats-error" role="alert">
        指标加载失败
        <el-button link type="primary" @click="loadOverview">重试</el-button>
      </p>

      <div class="records__charts">
        <section class="records__chart-card records__chart-card--wide">
          <header class="records__chart-head">
            <h2 class="records__chart-title">每日自评趋势</h2>
            <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
              <el-radio-button :value="7">7 天</el-radio-button>
              <el-radio-button :value="30">30 天</el-radio-button>
            </el-radio-group>
          </header>
          <div v-if="trendLoading" class="records__chart-skeleton" aria-busy="true">
            <el-skeleton :rows="4" animated />
          </div>
          <ErrorState
            v-else-if="trendError"
            message="趋势加载失败"
            @retry="loadTrend"
          />
          <p v-else-if="trendEmpty" class="records__chart-empty">暂无数据</p>
          <BaseChart
            v-else
            :option="trendOption"
            :height="isMobile ? '240px' : '300px'"
            aria-label="每日自评趋势折线图"
          />
        </section>

        <section class="records__chart-card">
          <header class="records__chart-head">
            <h2 class="records__chart-title">分类掌握分布</h2>
          </header>
          <div v-if="categoryLoading" class="records__chart-skeleton" aria-busy="true">
            <el-skeleton :rows="4" animated />
          </div>
          <ErrorState
            v-else-if="categoryError"
            message="分类分布加载失败"
            @retry="loadCategory"
          />
          <p v-else-if="categoryEmpty" class="records__chart-empty">暂无数据</p>
          <BaseChart
            v-else
            :option="categoryOption"
            :height="isMobile ? '240px' : '300px'"
            aria-label="分类掌握分布饼图"
          />
        </section>
      </div>
    </section>

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
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { getPracticeRecords } from '@/api/record'
import { getStatsCategory, getStatsOverview, getStatsTrend } from '@/api/stats'
import BaseChart from '@/components/BaseChart.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import PageBar from '@/components/PageBar.vue'

const PAGE_SIZE = 10

const ASSESS_LABELS = { 1: '掌握', 2: '模糊', 3: '不会' }
const ASSESS_TYPES = { 1: 'success', 2: 'warning', 3: 'danger' }
const SOURCE_LABELS = { 1: '详情页', 2: '随机刷题', 3: '错题本' }
const CHART_TOKEN_KEYS = [
  '--color-chart-1',
  '--color-chart-2',
  '--color-chart-3',
  '--color-chart-4',
  '--color-chart-5'
]

const router = useRouter()
const isMobile = useMediaQuery('(max-width: 767px)')
const topRef = ref(null)

const overview = ref(null)
const overviewLoading = ref(true)
const overviewError = ref(false)
const trend = ref([])
const trendDays = ref(7)
const trendLoading = ref(false)
const trendError = ref(false)
const category = ref([])
const categoryLoading = ref(false)
const categoryError = ref(false)

const records = ref([])
const total = ref(0)
const page = ref(1)
const selfAssessFilter = ref('')
const loading = ref(false)
const loadError = ref('')

let loadSeq = 0
let trendSeq = 0
let categorySeq = 0

function readChartPalette() {
  if (typeof window === 'undefined') {
    return { colors: [], border: 'transparent', textSecondary: 'inherit' }
  }
  const styles = window.getComputedStyle(document.documentElement)
  const read = (key) => styles.getPropertyValue(key).trim()
  return {
    colors: CHART_TOKEN_KEYS.map((key) => read(key)).filter(Boolean),
    // 轴色也走 base.css token，组件内不写死十六进制（04 B6）
    border: read('--color-border') || 'transparent',
    textSecondary: read('--color-text-secondary') || 'inherit'
  }
}

const metricCards = computed(() => {
  const data = overview.value
  const loading = overviewLoading.value
  const failed = !loading && (overviewError.value || !data)
  const totalRecordCount = data ? data.totalRecordCount : 0
  return [
    {
      key: 'today',
      label: '今日自评',
      value: loading ? '…' : failed ? '—' : String(data.todayCount),
      muted: loading || failed
    },
    {
      key: 'week',
      label: '近 7 天',
      value: loading ? '…' : failed ? '—' : String(data.weekCount),
      muted: loading || failed
    },
    {
      key: 'rate',
      label: '掌握率',
      value: loading ? '…' : failed ? '—' : formatRate(data.masteredRate),
      muted: loading || failed
    },
    {
      key: 'streak',
      label: '连续天数',
      value: loading ? '…' : failed ? '—' : `${data.streak} 天`,
      muted: loading || failed
    },
    {
      key: 'questions',
      label: '累计题数',
      value: loading ? '…' : failed ? '—' : String(data.totalQuestionCount),
      hint: loading || failed ? '' : `共 ${totalRecordCount} 次自评`,
      muted: loading || failed
    }
  ]
})

function formatRate(rate) {
  if (rate === null || rate === undefined) return '—'
  return `${Math.round(rate * 100)}%`
}

const trendEmpty = computed(() => {
  if (trend.value.length === 0) return true
  return trend.value.every((point) => !point.count)
})

const categoryEmpty = computed(() => {
  if (category.value.length === 0) return true
  return category.value.every((item) => !item.count)
})

const trendOption = computed(() => {
  const palette = readChartPalette()
  return {
    color: palette.colors.length ? [palette.colors[0]] : undefined,
    tooltip: { trigger: 'axis', valueFormatter: (value) => `${value} 次` },
    grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.value.map((point) => point.date.slice(5)),
      axisLine: { lineStyle: { color: palette.border } },
      axisLabel: { color: palette.textSecondary }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: palette.border } },
      axisLabel: { color: palette.textSecondary }
    },
    series: [
      {
        name: '自评次数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        data: trend.value.map((point) => point.count),
        lineStyle: { width: 2 },
        areaStyle: { opacity: 0.1 }
      }
    ]
  }
})

const categoryOption = computed(() => {
  const palette = readChartPalette()
  return {
    color: palette.colors.length ? palette.colors : undefined,
    tooltip: { trigger: 'item', formatter: '{b}：{c} 次（{d}%）' },
    legend: { bottom: 0, type: 'scroll', textStyle: { color: palette.textSecondary } },
    series: [
      {
        name: '分类分布',
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        // 不单靠颜色区分：直接标注分类名与占比（技能建议 + 04 B4.8）
        label: { formatter: '{b} {d}%' },
        data: category.value.map((item) => ({ name: item.name, value: item.count }))
      }
    ]
  }
})

function assessLabel(value) {
  return ASSESS_LABELS[value] || '—'
}

function assessType(value) {
  return ASSESS_TYPES[value] || 'info'
}

function sourceLabel(value) {
  return SOURCE_LABELS[value] || '—'
}

async function loadOverview() {
  overviewLoading.value = true
  try {
    const data = await getStatsOverview()
    overview.value = data
    overviewError.value = false
  } catch {
    overviewError.value = true
  } finally {
    overviewLoading.value = false
  }
}

async function loadTrend() {
  const seq = ++trendSeq
  trendLoading.value = true
  trendError.value = false
  try {
    const data = await getStatsTrend(trendDays.value)
    if (seq !== trendSeq) return
    trend.value = Array.isArray(data) ? data : []
  } catch {
    if (seq !== trendSeq) return
    trendError.value = true
  } finally {
    if (seq === trendSeq) trendLoading.value = false
  }
}

async function loadCategory() {
  const seq = ++categorySeq
  categoryLoading.value = true
  categoryError.value = false
  try {
    const data = await getStatsCategory()
    if (seq !== categorySeq) return
    category.value = Array.isArray(data) ? data : []
  } catch {
    if (seq !== categorySeq) return
    categoryError.value = true
  } finally {
    if (seq === categorySeq) categoryLoading.value = false
  }
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
  // 04 B5.2：切页后滚动到内容区顶部（scrollIntoView）
  if (topRef.value) topRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function goQuestionList() {
  router.push('/question')
}

onMounted(() => {
  loadOverview()
  loadTrend()
  loadCategory()
  loadRecords()
})
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

.records__stats {
  margin-bottom: var(--space-6);
}

.records__metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-3);
}

.records__metric {
  min-width: 0;
  padding: var(--space-4);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.records__metric-label {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.records__metric-value {
  margin-top: var(--space-2);
  color: var(--color-text);
  font-size: 26px;
  line-height: 34px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.records__metric-value--muted {
  color: var(--color-text-disabled);
}

.records__metric-hint {
  margin-top: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.records__stats-error {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.records__charts {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.records__chart-card {
  min-width: 0;
  padding: var(--space-4);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.records__chart-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.records__chart-title {
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
}

.records__chart-skeleton {
  padding: var(--space-3) 0;
}

.records__chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
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

@media (max-width: 1023px) {
  .records__metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .records__charts {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 767px) {
  .records__head {
    margin-bottom: var(--space-4);
  }

  /* 04 B7.2：指标卡 2 列（第 5 个占满整行），图表全宽上下堆叠 */
  .records__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .records__metric:last-child {
    grid-column: 1 / -1;
  }

  .records__metric {
    padding: var(--space-3);
  }

  .records__metric-value {
    font-size: 22px;
    line-height: 30px;
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

