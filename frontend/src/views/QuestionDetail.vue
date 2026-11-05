<template>
  <section class="question-detail">
    <div v-if="loading" class="question-detail__skeleton">
      <el-skeleton animated>
        <template #template>
          <el-skeleton-item variant="h1" class="question-detail__skeleton-title" />
          <el-skeleton-item variant="text" class="question-detail__skeleton-meta" />
          <div v-for="item in 3" :key="`skeleton-${item}`" class="question-detail__skeleton-card">
            <el-skeleton-item variant="text" />
            <el-skeleton-item variant="text" />
            <el-skeleton-item variant="text" />
          </div>
        </template>
      </el-skeleton>
    </div>

    <NotFound v-else-if="notFound" />

    <ErrorState v-else-if="loadError" :message="loadError" @retry="loadDetail" />

    <template v-else-if="question">
      <el-breadcrumb separator="/" class="question-detail__breadcrumb">
        <el-breadcrumb-item :to="{ path: '/question' }">题库</el-breadcrumb-item>
        <el-breadcrumb-item
          :to="{ path: '/question', query: { categoryId: question.categoryId } }"
        >
          {{ question.categoryName }}
        </el-breadcrumb-item>
        <el-breadcrumb-item>题目</el-breadcrumb-item>
      </el-breadcrumb>

      <h1 class="question-detail__title">{{ question.title }}</h1>

      <div class="question-detail__meta">
        <DifficultyTag :value="question.difficulty" />
        <span class="question-detail__meta-item">{{ question.categoryName }}</span>
        <span class="question-detail__meta-item">浏览 {{ question.viewCount }}</span>
        <span class="question-detail__meta-item">已练 {{ question.practiceCount }} 次</span>
      </div>

      <div v-if="tags.length > 0" class="question-detail__tags">
        <el-tag
          v-for="tag in tags"
          :key="tag.id"
          size="small"
          effect="plain"
          type="info"
          class="question-detail__tag"
        >
          {{ tag.name }}
        </el-tag>
      </div>

      <div class="question-detail__actions">
        <el-button
          class="question-detail__action"
          :type="favorited ? 'primary' : 'default'"
          :loading="favoriteLoading"
          @click="toggleFavorite"
        >
          {{ favorited ? '★ 已收藏' : '☆ 收藏' }}
        </el-button>
      </div>

      <section class="question-detail__card">
        <h2 class="question-detail__card-title">题干</h2>
        <MarkdownView :content="question.contentMd" empty-text="暂无题干" />
      </section>

      <section class="question-detail__card">
        <div class="question-detail__answer-head">
          <h2 class="question-detail__card-title">参考答案</h2>
          <el-button
            class="question-detail__answer-toggle"
            type="primary"
            link
            :aria-expanded="answerExpanded"
            @click="answerExpanded = !answerExpanded"
          >
            {{ answerExpanded ? '收起 ▴' : '展开参考答案 ▾' }}
          </el-button>
        </div>
        <MarkdownView
          v-if="answerExpanded"
          :content="question.answerMd"
          empty-text="暂无参考答案"
        />
      </section>

      <section class="question-detail__card question-detail__assess">
        <div class="question-detail__assess-head">
          <h2 class="question-detail__card-title">你觉得自己掌握了吗？</h2>
          <span v-if="todayCount !== null" class="question-detail__today">
            今日已刷 {{ todayCount }} 题
          </span>
        </div>
        <AssessmentBar
          :model-value="myAssessment"
          :loading="assessLoading"
          class="question-detail__assess-bar"
          @change="handleAssess"
        />
        <p v-if="!userStore.isLogin" class="question-detail__assess-tip">
          登录后才能记录自评，点击按钮会先引导登录
        </p>
      </section>

      <nav class="question-detail__neighbors" aria-label="上一题与下一题">
        <el-button
          class="question-detail__neighbor"
          :disabled="!neighbors.prevId"
          @click="goQuestion(neighbors.prevId)"
        >
          ← 上一题
        </el-button>
        <el-button
          class="question-detail__neighbor"
          :disabled="!neighbors.nextId"
          @click="goQuestion(neighbors.nextId)"
        >
          下一题 →
        </el-button>
      </nav>
    </template>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import {
  favoriteQuestion,
  getQuestionDetail,
  unfavoriteQuestion
} from '@/api/question'
import { submitAssessment } from '@/api/record'
import { getStatsOverview } from '@/api/stats'
import { useUserStore } from '@/stores/user'
import AssessmentBar from '@/components/AssessmentBar.vue'
import MarkdownView from '@/components/MarkdownView.vue'
import DifficultyTag from '@/components/DifficultyTag.vue'
import ErrorState from '@/components/ErrorState.vue'
import NotFound from '@/views/NotFound.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const loadError = ref('')
const notFound = ref(false)
const detail = ref(null)
const answerExpanded = ref(false)
// 收藏按钮：乐观更新，失败回滚（04 B4.3 / B5.2）
const favorited = ref(false)
const favoriteLoading = ref(false)
// 三档自评：当前档位来自详情接口 myAssessment；今日数只在自评成功后由响应给出
const myAssessment = ref(null)
const todayCount = ref(null)
const assessLoading = ref(false)

const ASSESS_LABELS = { 1: '掌握', 2: '模糊', 3: '不会' }
// 从错题本「去复习」进入时 source=3（04 B4.7 / 01 F04）
const assessSource = computed(() => (route.query.from === 'wrong' ? 3 : 1))

/** 今日已刷数首屏展示（04 B4.3）：用已有统计接口取，失败不影响详情。 */
async function loadTodayCount() {
  try {
    const data = await getStatsOverview()
    if (data && typeof data.todayCount === 'number') todayCount.value = data.todayCount
  } catch {
    // 统计失败只影响该文案，不阻塞题干/答案/自评
  }
}

const question = computed(() => (detail.value ? detail.value.question : null))
const neighbors = computed(() =>
  detail.value && detail.value.neighbors ? detail.value.neighbors : { prevId: null, nextId: null }
)
const tags = computed(() => {
  const value = question.value && question.value.tags
  return Array.isArray(value) ? value : []
})

let loadSeq = 0

async function loadDetail() {
  const seq = ++loadSeq
  loading.value = true
  loadError.value = ''
  notFound.value = false
  answerExpanded.value = false
  try {
    const data = await getQuestionDetail(route.params.id)
    if (seq !== loadSeq) return
    detail.value = data
    favorited.value = Boolean(data.myFavorite)
    myAssessment.value = data.myAssessment == null ? null : data.myAssessment
    if (userStore.isLogin) loadTodayCount()
    window.scrollTo({ top: 0, behavior: 'auto' })
  } catch (error) {
    if (seq !== loadSeq) return
    detail.value = null
    if (error && error.code === 40400) {
      notFound.value = true
    } else {
      loadError.value = (error && error.message) || '加载失败，请稍后重试'
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

// 上下题切换复用同一组件，id 变化时重新拉取并回到折叠态
watch(
  () => route.params.id,
  () => {
    loadDetail()
  },
  { immediate: true }
)

function goQuestion(id) {
  if (!id) return
  router.push({ name: 'question-detail', params: { id } })
}

/** 游客点收藏先弹登录（04 B4.3），已登录走乐观更新 */
async function toggleFavorite() {
  if (favoriteLoading.value) return
  if (!userStore.isLogin) {
    try {
      await ElMessageBox.confirm('登录后才能收藏，去登录？', '需要登录', {
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
  const next = !favorited.value
  favorited.value = next
  favoriteLoading.value = true
  try {
    if (next) {
      await favoriteQuestion(question.value.id)
    } else {
      await unfavoriteQuestion(question.value.id)
    }
    ElMessage.success(next ? '已收藏' : '已取消收藏')
  } catch (error) {
    favorited.value = !next
    ElMessage.error((error && error.message) || '操作失败，请重试')
  } finally {
    favoriteLoading.value = false
  }
}

/**
 * 三档自评（01 F04 / 04 B4.3）：2s 内不清除 loading，成功后高亮档位并给轻提示。
 * wrongBook 为 null = 从未进过错题本，不提示「已加入错题本」（04 A3.4 #14）。
 */
const ASSESS_DEBOUNCE_MS = 2000

async function handleAssess(assess) {
  if (assessLoading.value) return
  if (!userStore.isLogin) {
    try {
      await ElMessageBox.confirm('登录后才能自评，去登录？', '需要登录', {
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
  const startedAt = Date.now()
  const previous = myAssessment.value
  // 乐观把档位切到被点的那档，AssessmentBar 的 loading 才会落在该按钮上（失败回滚）
  myAssessment.value = assess
  assessLoading.value = true
  try {
    const data = await submitAssessment(question.value.id, {
      selfAssess: assess,
      source: assessSource.value
    })
    if (data && typeof data.todayCount === 'number') todayCount.value = data.todayCount
    const label = ASSESS_LABELS[assess] || '已记录'
    const wrongBook = data ? data.wrongBook : null
    if (wrongBook && wrongBook.resolved === 0) {
      ElMessage.success(`已记录：${label}，已加入错题本`)
    } else if (wrongBook && wrongBook.resolved === 1) {
      ElMessage.success('已记录：掌握')
    } else {
      ElMessage.success(`已记录：${label}`)
    }
  } catch (error) {
    myAssessment.value = previous
    ElMessage.error((error && error.message) || '记录失败，请重试')
  } finally {
    const remain = ASSESS_DEBOUNCE_MS - (Date.now() - startedAt)
    if (remain > 0) await new Promise((resolve) => setTimeout(resolve, remain))
    assessLoading.value = false
  }
}
</script>

<style scoped>
.question-detail {
  min-width: 0;
}

.question-detail__breadcrumb {
  margin-bottom: var(--space-4);
}

.question-detail__title {
  margin-bottom: var(--space-3);
  overflow-wrap: anywhere;
}

.question-detail__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.question-detail__meta-item {
  white-space: nowrap;
}

.question-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.question-detail__tag {
  border-radius: var(--radius-tag);
}

.question-detail__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.question-detail__action {
  min-height: 44px;
  padding: 0 var(--space-5);
  border-radius: var(--radius-button);
}

.question-detail__assess-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-2) var(--space-3);
}

.question-detail__today {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.question-detail__assess-bar {
  margin-top: var(--space-4);
}

.question-detail__assess-tip {
  margin-top: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.question-detail__card {
  margin-top: var(--space-6);
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.question-detail__card-title {
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
}

.question-detail__card-title + * {
  margin-top: var(--space-3);
}

.question-detail__answer-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.question-detail__answer-toggle {
  min-height: 44px;
  font-size: var(--font-size-body);
}

.question-detail__answer-head + * {
  margin-top: var(--space-3);
}

.question-detail__neighbors {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  margin-top: var(--space-6);
}

.question-detail__neighbor {
  min-height: 44px;
  padding: 0 var(--space-5);
  border-radius: var(--radius-button);
}

.question-detail__skeleton-title {
  width: 70%;
}

.question-detail__skeleton-meta {
  width: 40%;
  margin-top: var(--space-3);
}

.question-detail__skeleton-card {
  margin-top: var(--space-6);
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.question-detail__skeleton-card :deep(.el-skeleton__item + .el-skeleton__item) {
  margin-top: var(--space-2);
}

@media (max-width: 767px) {
  .question-detail__card {
    padding: var(--space-4);
  }

  .question-detail__neighbors {
    flex-direction: column;
    gap: var(--space-3);
  }

  .question-detail__neighbor {
    width: 100%;
    margin-left: 0;
  }
}
</style>
