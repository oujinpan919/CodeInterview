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

      <div v-if="showSourceBar" class="question-detail__source">
        <template v-if="isRandom">
          <span class="question-detail__source-tag">随机练习</span>
          <div class="question-detail__source-actions">
            <el-button
              class="question-detail__source-action"
              :loading="randomLoading"
              @click="changeRandom"
            >
              换一题
            </el-button>
            <el-button
              class="question-detail__source-action"
              type="primary"
              :loading="startLoading"
              @click="startPractice"
            >
              开始练习
            </el-button>
          </div>
        </template>
        <template v-else>
          <div class="question-detail__source-info">
            <span class="question-detail__source-text">
              第 {{ progress.position }} 题 / 共 {{ progress.totalCount }} 题
            </span>
            <span class="question-detail__source-sub">
              已完成 {{ progress.completedCount }} 题
            </span>
          </div>
          <el-progress
            class="question-detail__source-progress"
            :percentage="progressPercent"
            :stroke-width="6"
            :show-text="false"
          />
        </template>
      </div>

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
        <el-button class="question-detail__action" @click="askAssistant">问助手</el-button>
        <el-button
          class="question-detail__action"
          :disabled="!speechSupported"
          @click="toggleSpeak"
        >
          {{ speaking ? '停止朗读' : '朗读题干' }}
        </el-button>
      </div>

      <!-- v3.6 C2：背题模式（仅练习会话）。
           键盘快捷键提示（空格/J/K/F/A 等）已按需求移除；快捷键本身仍然生效，
           只是不再常驻显示，避免正文上方多一条提示条。 -->
      <div v-if="isSession" class="question-detail__toolbar">
        <label class="question-detail__switch">
          <el-switch v-model="autoAdvance" size="small" />
          <span class="question-detail__switch-label">背题模式</span>
          <span class="question-detail__switch-hint">自评后自动看答案并翻下一题</span>
        </label>
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

      <!-- v3.6 C6：私人笔记（Markdown，自动保存；错题本与导出都复用这份内容） -->
      <section class="question-detail__card question-detail__note">
        <div class="question-detail__answer-head">
          <h2 class="question-detail__card-title">我的笔记</h2>
          <span v-if="noteStatusText" class="question-detail__note-status">{{ noteStatusText }}</span>
          <el-button class="question-detail__answer-toggle" type="primary" link @click="toggleNote">
            {{ noteExpanded ? '收起 ▴' : noteHasContent ? '展开笔记 ▾' : '写笔记 ▾' }}
          </el-button>
        </div>
        <p v-if="!noteExpanded && noteHasContent" class="question-detail__note-excerpt">
          {{ noteExcerpt }}
        </p>
        <div v-else-if="noteExpanded" class="question-detail__note-body">
          <p v-if="!userStore.isLogin" class="question-detail__note-guest">登录后才能写私人笔记</p>
          <p v-else-if="noteLoadFailed" class="question-detail__note-guest">
            笔记加载失败，为避免覆盖原内容，暂时只读。
            <el-button type="primary" link @click="retryLoadNote">重试</el-button>
          </p>
          <template v-else>
            <div class="question-detail__note-tabs">
              <el-radio-group v-model="noteMode" size="small">
                <el-radio-button value="edit">编辑</el-radio-button>
                <el-radio-button value="preview">预览</el-radio-button>
              </el-radio-group>
              <!-- v4.12：笔记支持配图 -->
              <ImageUploader
                v-if="noteMode === 'edit'"
                folder="note"
                aria-label="在笔记中插入图片"
                @uploaded="insertNoteImage"
              >
                <template #default="{ uploading }">
                  <el-button size="small" text type="primary" :loading="uploading">
                    {{ uploading ? '上传中…' : '插入图片' }}
                  </el-button>
                </template>
              </ImageUploader>
              <span class="question-detail__note-hint">停手 1.5 秒或失焦自动保存</span>
            </div>
            <el-input
              v-if="noteMode === 'edit'"
              v-model="noteContent"
              class="question-detail__note-input"
              type="textarea"
              :rows="6"
              maxlength="20000"
              show-word-limit
              placeholder="用 Markdown 写下你的理解、易错点、追问…"
              @blur="saveNote"
            />
            <MarkdownView v-else :content="noteContent" empty-text="还没有笔记内容" />
          </template>
        </div>
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

      <!-- v3.6 C10：相关题（同分类优先 + 共享标签），点击进入默认同分类上下文 -->
      <section v-if="similarQuestions.length > 0" class="question-detail__card question-detail__similar">
        <h2 class="question-detail__card-title">相关题</h2>
        <ul class="question-detail__similar-list">
          <li v-for="item in similarQuestions" :key="item.id">
            <RouterLink
              class="question-detail__similar-link"
              :to="{ name: 'question-detail', params: { id: item.id } }"
            >
              <span class="question-detail__similar-title">{{ item.title }}</span>
              <span class="question-detail__similar-meta">{{ item.categoryName }}</span>
            </RouterLink>
          </li>
        </ul>
      </section>

      <nav class="question-detail__neighbors" aria-label="上一题与下一题">
        <el-button
          class="question-detail__neighbor"
          :disabled="isSession || !neighbors.prevId"
          @click="goQuestion(neighbors.prevId)"
        >
          ← 上一题
        </el-button>
        <el-button
          class="question-detail__neighbor"
          :disabled="!nextTarget"
          @click="goQuestion(nextTarget)"
        >
          下一题 →
        </el-button>
      </nav>
    </template>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="calc(100% - 32px)"
      style="max-width: 480px"
    >
      <template v-if="dialogKind === 'choice'">
        <p class="question-detail__dialog-text">
          上次练到第 {{ progress.position }} 题，共 {{ progress.totalCount }} 题，已完成
          {{ progress.completedCount }} 题。
        </p>
      </template>
      <template v-else-if="dialogKind === 'done' || dialogKind === 'ended'">
        <p v-if="dialogKind === 'ended'" class="question-detail__dialog-text">
          部分题目已跳过或不可用
        </p>
        <p class="question-detail__dialog-text">完成题数 {{ progress.completedCount }}</p>
        <p class="question-detail__dialog-text">总题数 {{ progress.totalCount }}</p>
        <p class="question-detail__dialog-text">跳过题数 {{ progress.skippedCount }}</p>
      </template>
      <EmptyState
        v-else-if="dialogKind === 'empty'"
        :title="emptyTitle"
        :description="hasRandomFilters ? '换个筛选条件，或者清空筛选' : ''"
        :action-text="hasRandomFilters ? '清空筛选' : ''"
        @action="clearRandomFilters"
      />
      <template #footer>
        <div class="question-detail__dialog-actions">
          <template v-if="dialogKind === 'choice'">
            <el-button
              class="question-detail__dialog-action"
              type="primary"
              @click="continueExisting"
            >
              继续上次
            </el-button>
            <el-button class="question-detail__dialog-action" @click="abandonAndCreate">
              放弃并新建
            </el-button>
          </template>
          <template v-else-if="dialogKind === 'done' || dialogKind === 'ended'">
            <el-button
              class="question-detail__dialog-action"
              type="primary"
              @click="restartSession"
            >
              再来一组
            </el-button>
            <el-button class="question-detail__dialog-action" @click="goHome">回首页</el-button>
          </template>
          <el-button v-else class="question-detail__dialog-action" @click="dialogVisible = false">
            关闭
          </el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import {
  favoriteQuestion,
  getQuestionDetail,
  getRandomQuestion,
  getSimilarQuestions,
  unfavoriteQuestion
} from '@/api/question'
import { advanceSession, createSession, getCurrentSession } from '@/api/session'
import { submitAssessment } from '@/api/record'
import { getQuestionNote, saveQuestionNote } from '@/api/note'
import { getStatsOverview } from '@/api/stats'
import { useAssistantEntry } from '@/composables/useAssistantEntry'
import { useUserStore } from '@/stores/user'
import AssessmentBar from '@/components/AssessmentBar.vue'
import MarkdownView from '@/components/MarkdownView.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import DifficultyTag from '@/components/DifficultyTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import NotFound from '@/views/NotFound.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { openAssistant } = useAssistantEntry()

/**
 * 父级（QuestionPractice）会传当前题目 id，并用 :key 重建组件。
 * 单独使用本组件时回退到路由参数；不能所有保存逻辑都依赖全局 route.params，
 * 否则切题时旧组件卸载会读到新题 id，把上一篇笔记写到下一篇。
 */
const props = defineProps({
  questionId: { type: [String, Number], default: null }
})
const currentQuestionId = computed(() => String(props.questionId ?? route.params.id ?? ''))
const loadedQuestionId = ref('')

/**
 * v4.2：把详情数据抛给外层（QuestionPractice 需要 answerMd 生成右侧目录）。
 * 单独发一次详情请求会让 view_count 每次翻倍（后端每次 detail 都会自增浏览数），
 * 所以外层只复用这里已拿到的结果，不重复请求。
 */
const emit = defineEmits(['loaded'])

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
// source=2 区分随机/会话练习来源；from 只是页面级 URL query，不传后端
const assessSource = computed(() => {
  if (route.query.from === 'wrong') return 3
  if (route.query.from === 'random' || route.query.from === 'session') return 2
  return 1
})

const isRandom = computed(() => route.query.from === 'random')
const isSession = computed(() => route.query.from === 'session')
const showSourceBar = computed(
  () => isRandom.value || (isSession.value && Boolean(session.value) && session.value.status === 1)
)
const session = ref(null)
const seenIds = ref([])
const randomLoading = ref(false)
const startLoading = ref(false)
const dialogVisible = ref(false)
const dialogKind = ref('')
const endedShown = ref(false)

const progress = computed(() => {
  const data = session.value
  return {
    position: data && typeof data.position === 'number' ? data.position : '—',
    totalCount: data && typeof data.totalCount === 'number' ? data.totalCount : '—',
    completedCount: data && typeof data.completedCount === 'number' ? data.completedCount : 0,
    skippedCount: data && typeof data.skippedCount === 'number' ? data.skippedCount : 0
  }
})
const progressPercent = computed(() => {
  const total = Number(session.value && session.value.totalCount) || 0
  const done = Number(session.value && session.value.completedCount) || 0
  return total > 0 ? Math.min(100, Math.round((done / total) * 100)) : 0
})
const nextTarget = computed(() =>
  isSession.value ? session.value && session.value.currentQuestionId : neighbors.value.nextId
)
const hasRandomFilters = computed(() => Object.keys(filterParams()).length > 0)
const emptyTitle = computed(() =>
  hasRandomFilters.value ? '当前筛选没有可练习的题目' : '题库建设中，暂无可练习的题目'
)
const dialogTitle = computed(
  () =>
    ({
      choice: '已有进行中的练习',
      done: '本组练习完成',
      ended: '本组练习已结束',
      empty: '随机练习'
    })[dialogKind.value] || ''
)

function filterParams() {
  const params = {}
  const { categoryId, tagId, difficulty } = route.query
  if (categoryId) params.categoryId = categoryId
  if (tagId) params.tagId = tagId
  if (difficulty) params.difficulty = difficulty
  return params
}

function randomParams() {
  const params = filterParams()
  if (seenIds.value.length) params.excludeIds = seenIds.value.join(',')
  return params
}

function syncSeen() {
  const id = String(route.params.id || '')
  if (id && !seenIds.value.includes(id)) seenIds.value.push(id)
}

function openDialog(kind) {
  dialogKind.value = kind
  dialogVisible.value = true
}

function applyProgress(data) {
  if (!data) return
  session.value = data
  if (data.skippedNotice > 0) ElMessage.info(`有 ${data.skippedNotice} 道题已下架，已跳过`)
  if (!endedShown.value && (data.status === 2 || data.status === 4)) {
    endedShown.value = true
    openDialog(data.status === 2 ? 'done' : 'ended')
  }
}

async function loadSession() {
  if (!userStore.isLogin) {
    session.value = null
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    const data = await getCurrentSession()
    session.value = data || null
    if (data) applyProgress(data)
  } catch {
    session.value = null
  }
}

async function changeRandom() {
  if (randomLoading.value) return
  randomLoading.value = true
  try {
    const data = await getRandomQuestion(randomParams())
    if (!data || !data.id) {
      openDialog('empty')
      return
    }
    const id = String(data.id)
    if (!seenIds.value.includes(id)) seenIds.value.push(id)
    goQuestion(id)
  } catch (error) {
    ElMessage.error((error && error.message) || '加载失败，请稍后重试')
  } finally {
    randomLoading.value = false
  }
}

function goToSession(id) {
  if (!id) return
  router.push({
    name: 'question-detail',
    params: { id },
    query: { ...route.query, from: 'session' }
  })
}

async function createAndGo() {
  const data = await createSession(filterParams())
  session.value = data || null
  if (!data || !data.currentQuestionId) {
    openDialog('empty')
    return
  }
  goToSession(data.currentQuestionId)
}

async function startPractice() {
  if (startLoading.value) return
  if (!userStore.isLogin) {
    try {
      await ElMessageBox.confirm('登录后才能开始练习，去登录？', '需要登录', {
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
  startLoading.value = true
  try {
    const current = await getCurrentSession()
    if (current && current.status === 1) {
      session.value = current
      openDialog('choice')
      return
    }
    await createAndGo()
  } catch (error) {
    ElMessage.error((error && error.message) || '操作失败，请重试')
  } finally {
    startLoading.value = false
  }
}

function continueExisting() {
  const id = session.value && session.value.currentQuestionId
  dialogVisible.value = false
  if (id) goToSession(id)
}

async function abandonAndCreate() {
  try {
    await ElMessageBox.confirm(
      '放弃后当前进度将保留为历史，并新建一组练习，确定吗？',
      '放弃并新建',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  dialogVisible.value = false
  startLoading.value = true
  try {
    await advanceSession(session.value.sessionId, { action: 'abandon' })
    await createAndGo()
  } catch (error) {
    ElMessage.error((error && error.message) || '操作失败，请重试')
  } finally {
    startLoading.value = false
  }
}

async function restartSession() {
  dialogVisible.value = false
  endedShown.value = false
  startLoading.value = true
  try {
    await createAndGo()
  } catch (error) {
    ElMessage.error((error && error.message) || '操作失败，请重试')
  } finally {
    startLoading.value = false
  }
}

async function clearRandomFilters() {
  dialogVisible.value = false
  await router.replace({
    name: 'question-detail',
    params: { id: route.params.id },
    query: { from: 'random' }
  })
  seenIds.value = []
  changeRandom()
}

function goHome() {
  dialogVisible.value = false
  router.push('/')
}

async function advanceAfterAssess() {
  const current = session.value
  const payload = { action: 'complete', questionId: route.params.id }
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const data = await advanceSession(current.sessionId, payload)
      applyProgress(data)
      return
    } catch (error) {
      if (attempt === 1) ElMessage.error((error && error.message) || '练习进度更新失败，请重试')
    }
  }
}

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
  const questionId = currentQuestionId.value
  if (!questionId) {
    loading.value = false
    notFound.value = true
    return
  }
  // 切题前先把上一篇的未落盘笔记写到原题，再切换 loadedQuestionId
  if (noteState.value === 'dirty') {
    await saveNote()
  }
  loadedQuestionId.value = questionId
  loading.value = true
  loadError.value = ''
  notFound.value = false
  answerExpanded.value = false
  stopSpeak()
  similarQuestions.value = []
  try {
    const data = await getQuestionDetail(questionId, buildNavParams())
    if (seq !== loadSeq) return
    detail.value = data
    favorited.value = Boolean(data.myFavorite)
    myAssessment.value = data.myAssessment == null ? null : data.myAssessment
    emit('loaded', data)
    loadNote(questionId)
    loadSimilar(questionId)
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

/**
 * v3.6 修1：把来源列表上下文带给详情接口，后端据此算「上一题/下一题」。
 * ctx=list → 题库列表（带筛选与排序）；ctx=favorite → 我的收藏；ctx=wrong → 错题本（带 scope）。
 */
function buildNavParams() {
  const ctx = route.query.ctx
  if (!ctx) return undefined
  const params = { ctx }
  if (ctx === 'wrong' && route.query.scope) params.scope = route.query.scope
  if (ctx === 'list') {
    const { categoryId, tagId, difficulty, keyword, sort } = route.query
    if (categoryId) params.categoryId = categoryId
    if (tagId) params.tagId = tagId
    if (difficulty) params.difficulty = difficulty
    if (keyword) params.keyword = keyword
    if (sort) params.sort = sort
  }
  return params
}

// 上下题切换复用同一组件，id 变化时重新拉取并回到折叠态
// 注意：两个 immediate watch 必须放在 setup 末尾注册 —— loadDetail 会用到后面的
// 语音/相关题/笔记状态（const），提前注册会在 TDZ 里抛错导致页面永远停在骨架屏。

function goQuestion(id) {
  if (!id) return
  // v3.6 修1：全量保留 query（ctx / scope / 筛选 / from），否则翻一题就丢了来源上下文
  router.push({ name: 'question-detail', params: { id }, query: { ...route.query } })
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
 * 问助手（04 B4.3 / 08 §4.1）：跳转到独立页并带入当前题目。
 *
 * v3.20（硬约束-2/D8）：不再是浮层展开，而是 `router.push('/assistant?questionId=x')`。
 * 题干由服务端按 questionId 查库拼进 prompt，前端只传 id。
 * 未登录拦截统一走 useAssistantEntry，不再内联重复实现（08 §11.3）。
 */
async function askAssistant() {
  if (!question.value) return
  await openAssistant({ questionId: question.value.id })
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
    if (isSession.value && session.value && session.value.status === 1) {
      await advanceAfterAssess()
      if (
        autoAdvance.value &&
        session.value &&
        session.value.status === 1 &&
        session.value.currentQuestionId &&
        !dialogVisible.value
      ) {
        // 背题模式（v3.6 C2）：先亮答案 1.5s 再自动翻下一题，全程不用碰鼠标
        answerExpanded.value = true
        await new Promise((resolve) => setTimeout(resolve, ANSWER_PEEK_MS))
        if (!dialogVisible.value) goQuestion(session.value.currentQuestionId)
      }
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

/* ---------------------------------------------------------------------------
 * v3.6 C2：背题模式 + 键盘流（04 C4）
 * 快捷键只在「不在输入框、无弹窗」时生效；页面上不再常驻显示快捷键提示。
 * ------------------------------------------------------------------------- */
const AUTO_ADVANCE_KEY = 'ci_auto_advance'
const ANSWER_PEEK_MS = 1500
const autoAdvance = ref(sessionStorage.getItem(AUTO_ADVANCE_KEY) === '1')

watch(autoAdvance, (value) => {
  sessionStorage.setItem(AUTO_ADVANCE_KEY, value ? '1' : '0')
})

function isTypingTarget(target) {
  if (!target || typeof target.tagName !== 'string') return false
  const tag = target.tagName
  return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || target.isContentEditable
}

function goNextQuestion() {
  if (nextTarget.value) goQuestion(nextTarget.value)
}

function goPrevQuestion() {
  // 练习会话内题目由服务端断点推进，不允许后退（避免进度与断点不一致）
  if (isSession.value) return
  if (neighbors.value.prevId) goQuestion(neighbors.value.prevId)
}

function onHotkey(event) {
  if (event.defaultPrevented || event.metaKey || event.ctrlKey || event.altKey) return
  if (isTypingTarget(event.target)) return
  if (dialogVisible.value) return
  const key = event.key
  if (key === ' ' || key === 'Spacebar') {
    event.preventDefault()
    answerExpanded.value = !answerExpanded.value
    return
  }
  if (key === 'j' || key === 'J' || key === 'ArrowRight') {
    event.preventDefault()
    goNextQuestion()
    return
  }
  if (key === 'k' || key === 'K' || key === 'ArrowLeft') {
    event.preventDefault()
    goPrevQuestion()
    return
  }
  if (key === 'f' || key === 'F') {
    event.preventDefault()
    toggleFavorite()
    return
  }
  if (key === 'a' || key === 'A') {
    event.preventDefault()
    askAssistant()
    return
  }
  if (key === '1' || key === '2' || key === '3') {
    event.preventDefault()
    handleAssess(Number(key))
  }
}

onMounted(() => {
  window.addEventListener('keydown', onHotkey)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onHotkey)
  stopSpeak()
  // 离开页面前把未落盘的笔记补一次（防抖窗口内切走会丢）
  if (noteState.value === 'dirty') saveNote()
})

/* ---------------------------------------------------------------------------
 * v3.6 C11：语音朗读（浏览器 SpeechSynthesis，零依赖零接口）
 * 只读题干；Markdown 语法先剥掉，代码块用「代码块」占位，避免念出反引号。
 * ------------------------------------------------------------------------- */
const speechSupported = typeof window !== 'undefined' && 'speechSynthesis' in window
const speaking = ref(false)

function stripMarkdown(text) {
  return (text || '')
    .replace(/```[\s\S]*?```/g, ' 代码块 ')
    .replace(/`([^`]*)`/g, '$1')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/[#>*_~|-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function stopSpeak() {
  if (!speechSupported) return
  window.speechSynthesis.cancel()
  speaking.value = false
}

function toggleSpeak() {
  if (!speechSupported) {
    ElMessage.info('当前浏览器不支持语音朗读')
    return
  }
  if (speaking.value) {
    stopSpeak()
    return
  }
  const text = stripMarkdown(question.value && question.value.contentMd)
  if (!text) {
    ElMessage.info('这道题没有可朗读的题干')
    return
  }
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'zh-CN'
  utterance.rate = 1
  utterance.onend = () => {
    speaking.value = false
  }
  utterance.onerror = () => {
    speaking.value = false
  }
  speaking.value = true
  window.speechSynthesis.speak(utterance)
}

/* ---------------------------------------------------------------------------
 * v3.6 C10：相关题（接口失败只隐藏该卡，不影响详情主体）
 * ------------------------------------------------------------------------- */
const similarQuestions = ref([])

async function loadSimilar(questionId) {
  similarQuestions.value = []
  try {
    const data = await getSimilarQuestions(questionId, { limit: 3 })
    similarQuestions.value = Array.isArray(data) ? data : []
  } catch {
    similarQuestions.value = []
  }
}

/* ---------------------------------------------------------------------------
 * v3.6 C6：私人笔记（Markdown，自动保存；04 C5 #48）
 * 空内容 = 删除该条；服务端限 20000 字符；错题本与导出共用这份内容。
 * ------------------------------------------------------------------------- */
const NOTE_SAVE_DELAY_MS = 1500
const noteExpanded = ref(false)
const noteContent = ref('')
const noteMode = ref('edit')
/** idle / dirty / saving / saved / error —— 只影响右上角状态文案 */
const noteState = ref('idle')
const noteUpdatedAt = ref(null)
const noteLoadFailed = ref(false)
/** 载入笔记期间屏蔽 watcher，避免把「读回来的内容」当成用户编辑 */
let noteSyncing = false
let noteLoaded = false
let noteTimer = null
let noteLoadSeq = 0

const noteHasContent = computed(() => noteContent.value.trim().length > 0)
const noteExcerpt = computed(() => {
  const text = noteContent.value
    .replace(/[#>*`_~\-\[\]()!]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  return text.length > 60 ? `${text.slice(0, 60)}…` : text
})
const noteStatusText = computed(() => {
  if (!userStore.isLogin) return ''
  if (noteLoadFailed.value) return '加载失败'
  if (noteState.value === 'saving') return '保存中…'
  if (noteState.value === 'dirty') return '未保存'
  if (noteState.value === 'error') return '保存失败'
  if (noteUpdatedAt.value) return `已保存 ${dayjs(noteUpdatedAt.value).format('MM-DD HH:mm')}`
  return ''
})

async function loadNote(questionId) {
  const seq = ++noteLoadSeq
  clearTimeout(noteTimer)
  noteExpanded.value = false
  noteMode.value = 'edit'
  noteState.value = 'idle'
  noteUpdatedAt.value = null
  noteLoaded = false
  noteLoadFailed.value = false
  noteSyncing = true
  noteContent.value = ''
  if (!userStore.isLogin) {
    await nextTick()
    noteSyncing = false
    return
  }
  try {
    const data = await getQuestionNote(questionId)
    if (seq !== noteLoadSeq) return
    noteContent.value = (data && data.contentMd) || ''
    noteUpdatedAt.value = (data && data.updatedAt) || null
    noteLoaded = true
  } catch {
    if (seq !== noteLoadSeq) return
    // 读取失败时不能把空内容当成「没有笔记」，否则用户一输入就会覆盖服务端原笔记。
    noteLoadFailed.value = true
  }
  await nextTick()
  if (seq === noteLoadSeq) noteSyncing = false
}

function retryLoadNote() {
  const id = loadedQuestionId.value || currentQuestionId.value
  if (id) loadNote(id)
}

async function saveNote() {
  if (!userStore.isLogin || !noteLoaded || noteLoadFailed.value) return
  if (noteState.value !== 'dirty') return
  const questionId = loadedQuestionId.value
  if (!questionId) return
  clearTimeout(noteTimer)
  noteState.value = 'saving'
  try {
    const data = await saveQuestionNote(questionId, noteContent.value)
    noteUpdatedAt.value = (data && data.updatedAt) || null
    noteState.value = 'saved'
  } catch {
    noteState.value = 'error'
  }
}

watch(noteContent, () => {
  if (!userStore.isLogin || !noteLoaded || noteSyncing) return
  noteState.value = 'dirty'
  clearTimeout(noteTimer)
  noteTimer = setTimeout(() => {
    saveNote()
  }, NOTE_SAVE_DELAY_MS)
})

/**
 * v4.12 把上传成功的图片追加到笔记末尾。
 * 直接改 noteContent 即可：上面的 watcher 会标 dirty 并触发 1.5s 防抖自动保存，
 * 不需要在这里再手动调 saveNote（重复调用会和防抖竞态）。
 */
function insertNoteImage(result) {
  if (!result || !result.url) return
  const snippet = `![图片](${result.url})`
  const current = noteContent.value
  noteContent.value = current.trim()
    ? `${current.replace(/\s+$/, '')}\n\n${snippet}\n`
    : `${snippet}\n`
  ElMessage.success('图片已插入，稍后自动保存')
}

async function toggleNote() {
  if (!userStore.isLogin) {
    try {
      await ElMessageBox.confirm('登录后才能写私人笔记，去登录？', '需要登录', {
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
  noteExpanded.value = !noteExpanded.value
  if (noteExpanded.value) noteMode.value = 'edit'
}

/**
 * v4.2：给外层（QuestionPractice 的右侧目录）提供的两个能力。
 * - revealAnswer()：答案默认折叠，目录里的小节在答案区；跳转前必须先展开，
 *   否则目标 DOM 不存在，点了没反应。
 * - answerExpanded：外层可读它决定是否提示「已展开」。
 */
defineExpose({ revealAnswer })

function revealAnswer() {
  answerExpanded.value = true
}

/* ---------------------------------------------------------------------------
 * 路由响应式：必须放在 setup 最末尾注册 —— loadDetail 会用到语音/相关题/笔记状态（const），
 * 提前注册 immediate 回调会在 TDZ 抛错，页面永远停在骨架屏（v3.6 实测踩过）。
 * ------------------------------------------------------------------------- */
watch(
  currentQuestionId,
  () => {
    loadDetail()
  },
  { immediate: true }
)

watch(
  () => route.query.from,
  () => {
    if (isSession.value) {
      loadSession()
    } else {
      session.value = null
      endedShown.value = false
    }
    if (isRandom.value) syncSeen()
  },
  { immediate: true }
)
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

  .question-detail__actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .question-detail__action {
    width: 100%;
    margin-left: 0;
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

.question-detail__source {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-3);
  margin-bottom: var(--space-4);
  padding: var(--space-3) var(--space-4);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.question-detail__source-tag {
  padding: 2px var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  background-color: color-mix(in srgb, var(--color-primary) 10%, transparent);
  border-radius: var(--radius-tag);
}

.question-detail__source-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-left: auto;
}

.question-detail__source-action {
  min-height: 44px;
  padding: 0 var(--space-4);
  border-radius: var(--radius-button);
}

.question-detail__source-info {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-2) var(--space-3);
  min-width: 0;
}

.question-detail__source-text {
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
}

.question-detail__source-sub {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.question-detail__source-progress {
  flex: 1 1 160px;
  min-width: 0;
}

.question-detail__dialog-text {
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.question-detail__dialog-text + .question-detail__dialog-text {
  margin-top: var(--space-2);
}

.question-detail__dialog-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-2);
}

.question-detail__dialog-action {
  min-height: 44px;
  margin-left: 0;
  border-radius: var(--radius-button);
}

@media (max-width: 767px) {
  .question-detail__source {
    flex-direction: column;
    align-items: stretch;
  }

  .question-detail__source-actions {
    flex-direction: column;
    margin-left: 0;
  }

  .question-detail__source-action {
    width: 100%;
  }

  .question-detail__source-progress {
    flex-basis: 100%;
  }

  .question-detail__dialog-actions {
    flex-direction: column;
  }

  .question-detail__dialog-action {
    width: 100%;
  }
}
/* ---------------------------------------------------------------------------
   v3.6 C2：背题模式开关 + 快捷键提示
   --------------------------------------------------------------------------- */
.question-detail__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-4);
  margin-top: var(--space-3);
  padding: var(--space-2) var(--space-3);
  background-color: var(--color-bg-page);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-button);
}

.question-detail__switch {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
}

.question-detail__switch-label {
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 600;
}

.question-detail__switch-hint {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

/* ---------------------------------------------------------------------------
   v3.6 C6：私人笔记
   --------------------------------------------------------------------------- */
.question-detail__note-status {
  margin-left: auto;
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

.question-detail__answer-head + .question-detail__note-status {
  margin-right: var(--space-2);
}

.question-detail__note-excerpt {
  display: -webkit-box;
  margin-top: var(--space-2);
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.question-detail__note-body {
  margin-top: var(--space-3);
}

.question-detail__note-tabs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.question-detail__note-hint {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

.question-detail__note-input :deep(.el-textarea__inner) {
  font-size: 16px;
}

.question-detail__note-guest {
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}
/* ---------------------------------------------------------------------------
   v3.6 C10：相关题
   --------------------------------------------------------------------------- */
.question-detail__similar-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.question-detail__similar-link {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-2);
  min-height: 44px;
  padding: var(--space-2) var(--space-3);
  background-color: var(--color-bg-page);
  border-radius: var(--radius-button);
  transition: background-color 0.15s ease;
}

.question-detail__similar-link:hover {
  background-color: #eff6ff;
}

.question-detail__similar-title {
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  overflow-wrap: anywhere;
}

.question-detail__similar-meta {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  white-space: nowrap;
}
</style>