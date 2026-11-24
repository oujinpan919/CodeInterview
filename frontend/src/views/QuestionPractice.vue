<template>
  <!--
    v4.2 题目练习页：三栏布局（左：题目列表 / 中：题目详情 / 右：本题目录）
    ----------------------------------------------------------------------
    布局：CSS Grid，左右定宽、中间 1fr 自适应。
    ≤1279px 收掉右栏（目录），≤1023px 收掉左栏（改抽屉），中间始终是详情区。
    详情区业务逻辑仍由 QuestionDetail.vue 承担，本页只负责「外层框架 + 切题」。
  -->
  <div class="qp-page">
    <div class="qp">
      <!-- ============ 左栏：题目列表 ============ -->
      <aside
        v-if="showLeft"
        class="qp-side"
        :class="{ 'qp-side--drawer': !isWide, 'qp-side--open': drawerOpen }"
        aria-label="题目列表"
      >
        <header class="qp-side__head">
          <h2 class="qp-side__title">题目列表</h2>
          <span v-if="sideList.length > 0" class="qp-side__count">{{ sideList.length }} 题</span>
        </header>

        <div class="qp-side__body">
          <div v-if="sideLoading" class="qp-side__skeleton" aria-busy="true">
            <el-skeleton :rows="6" animated />
          </div>

          <div v-else-if="sideError" class="qp-side__error">
            <p class="qp-side__error-text">{{ sideError }}</p>
            <el-button link type="primary" @click="loadSide">重试</el-button>
          </div>

          <!-- 空态：分类里没有题 / 分类未知 -->
          <div v-else-if="sideList.length === 0" class="qp-side__empty">
            <p class="qp-side__empty-title">这个题库还没有题目</p>
            <p class="qp-side__empty-desc">换个分类看看，或者去题库页筛选。</p>
            <el-button class="qp-side__empty-action" type="primary" @click="goQuestionList">
              去题库逛逛
            </el-button>
          </div>

          <ul v-else ref="sideListRef" class="qp-list">
            <li v-for="item in sideList" :key="item.id" class="qp-list__item">
              <button
                type="button"
                class="qp-list__btn"
                :class="{
                  'is-active': String(item.id) === String(activeId),
                  'is-visited': Boolean(formatViewedAt(item.id))
                }"
                :aria-current="String(item.id) === String(activeId) ? 'true' : undefined"
                :title="item.title"
                @click="switchQuestion(item)"
              >
                <!-- v4.3：看过的题标绿（含上次浏览时间），没看过的标蓝。 -->
                <span class="qp-list__title">{{ item.title }}</span>
                <span v-if="formatViewedAt(item.id)" class="qp-list__time">
                  上次浏览：{{ formatViewedAt(item.id) }}
                </span>
              </button>
            </li>
          </ul>
        </div>
      </aside>

      <div v-if="showLeft && !isWide && drawerOpen" class="qp-mask" @click="drawerOpen = false"></div>

      <!-- ============ 中栏：题目详情（原有业务组件，逻辑未改） ============ -->
      <main class="qp-main">
        <!-- 窄屏没有左栏时，给一个唤出入口，否则无法切换题目 -->
        <div v-if="!isWide" class="qp-main__bar">
          <el-button class="qp-main__drawer-btn" @click="drawerOpen = true">
            <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false">
              <path fill="currentColor" d="M3 5h18v2H3V5m0 6h18v2H3v-2m0 6h18v2H3v-2z" />
            </svg>
            <span>题目列表</span>
          </el-button>
        </div>

        <!-- :key 让切题时组件重建，避免上一题的答案展开态/笔记草稿串到下一题；
             @loaded 拿到详情数据生成右侧目录，不再单独请求详情（否则浏览量会被重复 +1） -->
        <QuestionDetail
          ref="detailRef"
          :key="activeId"
          :question-id="activeId"
          @loaded="handleDetailLoaded"
        />
      </main>

      <!-- ============ 右栏：本题目录 ============ -->
      <aside v-if="showRight" class="qp-toc" aria-label="本题目录">
        <div class="qp-toc__head">
          <h2 class="qp-toc__title">目录</h2>
        </div>

        <nav class="qp-toc__nav">
          <template v-if="outline.length > 0">
            <p class="qp-toc__group">回答重点</p>
            <ul class="qp-toc__list">
              <li v-for="item in outline" :key="item.key">
                <button type="button" class="qp-toc__link" @click="scrollToSection(item.text)">
                  {{ item.text }}
                </button>
              </li>
            </ul>
          </template>

          <p v-else class="qp-toc__placeholder">
            {{ tocHint }}
          </p>

          <!-- 固定入口：这两块在详情区一定存在，可帮助快速定位 -->
          <p class="qp-toc__group qp-toc__group--gap">本页区块</p>
          <ul class="qp-toc__list">
            <li v-for="anchor in PAGE_ANCHORS" :key="anchor.text">
              <button type="button" class="qp-toc__link" @click="scrollToSection(anchor.text)">
                {{ anchor.text }}
              </button>
            </li>
          </ul>
        </nav>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import dayjs from 'dayjs'
import { getQuestions, getRecentViews } from '@/api/question'
import { extractOutline } from '@/composables/useQuestionOutline'
import { useUserStore } from '@/stores/user'
import QuestionDetail from '@/views/QuestionDetail.vue'

/** 详情页里固定存在的两个区块，作为目录的兜底入口 */
const PAGE_ANCHORS = [{ text: '题干' }, { text: '参考答案' }]

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** ≥1024：左+中；<1024：只有中（左栏转抽屉） */
const isWide = useMediaQuery('(min-width: 1024px)')
/**
 * 目录阈值 1200：中间列至少要留 660px 正文宽度才可读
 * （1200 - 左右栏 268/240 - 间距 40 - 页面内边距 48 ≈ 604，再扣卡片内边距）。
 * 定 1280 会把 1243、1180 这类常见笔记本宽度直接判成无目录，等于白做。
 */
const isTocVisible = useMediaQuery('(min-width: 1200px)')
const showLeft = computed(() => isWide.value || drawerOpen.value)
const showRight = computed(() => isTocVisible.value)

const isLogin = computed(() => userStore.isLogin)
const activeId = computed(() => String(route.params.id || ''))

const drawerOpen = ref(false)
/** 详情组件引用：目录跳转需要它把折叠的答案展开 */
const detailRef = ref(null)
/** 左栏列表容器：切题后把高亮项滚进可视区 */
const sideListRef = ref(null)

/* ---------------------------------------------------------------------------
 * 左栏：当前题库的全部题目
 * ---------------------------------------------------------------------------
 * 数据 = 「分类题目全量列表」+「我的浏览时间映射」，两路合并：
 *   - 列表用 getQuestions 按当前题所属分类拉全量（该分类最多 81 题，2 页足够），
 *     排序沿用题库默认的 published_at DESC，与题库页看到的一致；
 *   - 浏览时间单独取 recent-views，做成 id → lastViewedAt 的 Map 后再回填。
 *     这样左栏是「完整题目」，但每道看过的题仍能显示「上次浏览」。
 */
const RECENT_LIMIT = 200
/** 单次最多翻几页兜底：分类最多 81 题（50/页），2 页就够，留 4 页余量防未来题库增长 */
const MAX_LIST_PAGES = 4
const RECENT_PAGE_SIZE = 50

const sideList = ref([])
const sideLoading = ref(false)
const sideError = ref('')
/** id → lastViewedAt，仅用于回填「上次浏览」 */
const viewedMap = ref(new Map())

/** 当前分类：优先用 URL 上的 categoryId，没有就等详情组件 loaded 后回填 */
const activeCategoryId = computed(() => {
  const raw = route.query.categoryId
  return raw ? String(raw) : ''
})

/** 拉一个分类下的全部题目（自动翻页到 total 为止） */
async function loadSideQuestions(categoryId) {
  const all = []
  let page = 1
  let total = 0
  while (page <= MAX_LIST_PAGES) {
    const data = await getQuestions({
      categoryId,
      page,
      pageSize: RECENT_PAGE_SIZE,
      sort: 'latest'
    })
    const list = (data && data.list) || []
    total = (data && data.total) || 0
    all.push(...list)
    if (all.length >= total || list.length === 0) break
    page += 1
  }
  return all
}

/** 拉当前用户的浏览时间映射（未登录直接返回空 Map） */
async function loadViewedMap() {
  if (!isLogin.value) return new Map()
  try {
    const data = await getRecentViews({ limit: RECENT_LIMIT })
    const rows = Array.isArray(data) ? data : []
    return new Map(rows.map((row) => [String(row.id), row.lastViewedAt]))
  } catch {
    // 浏览时间拿不到不影响题目列表本身，退化为不显示时间
    return new Map()
  }
}

async function loadSide() {
  sideError.value = ''
  const categoryId = activeCategoryId.value
  if (!categoryId) {
    // 分类未知时先空着，等详情 loaded 回填分类后再拉
    sideList.value = []
    return
  }
  sideLoading.value = true
  try {
    const [list, map] = await Promise.all([loadSideQuestions(categoryId), loadViewedMap()])
    viewedMap.value = map
    sideList.value = list
  } catch {
    sideList.value = []
    sideError.value = '题目列表加载失败'
  } finally {
    sideLoading.value = false
  }
}

/** 详情组件回填分类：URL 上没带 categoryId 时（例如直接输地址进来）用它补 */
function ensureCategory(categoryId) {
  if (!categoryId || activeCategoryId.value) return
  if (String(categoryId) === String(route.query.categoryId || '')) return
  // 写进 URL，这样刷新/切题都能保持同一题库
  router.replace({ query: { ...route.query, categoryId } })
}

/**
 * 上次浏览时间：固定「YYYY-MM-DD HH:mm」（年月日时分）。
 * 不做「今天/昨天」这类相对化处理 —— 用户明确要完整年月日时分，
 * 而且相对化在跨天边界上容易让人误判是哪一天。
 */
function formatViewedAt(id) {
  const value = viewedMap.value.get(String(id))
  if (!value) return ''
  const time = dayjs(value)
  if (!time.isValid()) return ''
  return time.format('YYYY-MM-DD HH:mm')
}

function goQuestionList() {
  router.push('/question')
}

/** 切题：保留全部 query（ctx/from/筛选），与详情页 goQuestion 的口径一致 */
function switchQuestion(item) {
  if (String(item.id) === activeId.value) {
    drawerOpen.value = false
    return
  }
  drawerOpen.value = false
  router.push({ name: 'question-detail', params: { id: item.id }, query: { ...route.query } })
}

/* ---------------------------------------------------------------------------
 * 右栏：目录
 * 数据来源是 QuestionDetail 通过 @loaded 抛出来的详情（含 answerMd）。
 * 千万不要在这里再调一次 getQuestionDetail —— 后端的 detail 每次都会
 * view_count + 1，重复请求会让浏览量翻倍。
 * ------------------------------------------------------------------------- */
const outline = ref([])
const tocHint = ref('目录加载中…')

/** 详情组件拉完详情后回调：提取目录 + 回填分类（用于拉左栏题目列表） */
function handleDetailLoaded(detail) {
  const answerMd = detail && detail.question ? detail.question.answerMd : ''
  outline.value = extractOutline(answerMd)
  tocHint.value = outline.value.length === 0 ? '这道题的答案没有分节标题' : '暂无目录'

  // 直接输地址进来时 URL 上没有 categoryId，用详情里的分类补齐并拉列表
  const categoryId = detail && detail.question ? detail.question.categoryId : null
  ensureCategory(categoryId)
}

/**
 * 目录点击 → 滚动到详情区里对应的小节。
 *
 * 两个现实约束：
 * 1. 答案区默认折叠，折叠时 Markdown 根本没渲染，小节 DOM 不存在 —— 先调用详情组件
 *    暴露的 revealAnswer() 展开，等下一帧再找元素；
 * 2. MarkdownView 不生成锚点 id，只能按文本内容匹配元素。
 */
async function scrollToSection(text) {
  // 先展开答案（小节几乎都在答案里），已展开时是幂等的
  if (detailRef.value && typeof detailRef.value.revealAnswer === 'function') {
    detailRef.value.revealAnswer()
    await nextTick()
  }

  const scroller = document.querySelector('.qp-page')
  if (!scroller) return
  const candidates = scroller.querySelectorAll(
    '.question-detail__card-title, .markdown-body strong, .markdown-body h1, .markdown-body h2, .markdown-body h3'
  )
  const target = Array.from(candidates).find((el) => el.textContent.trim().startsWith(text))
  if (target) {
    target.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

/* ---------------------------------------------------------------------------
 * 生命周期 / 路由响应
 * ------------------------------------------------------------------------- */
watch(
  () => route.params.id,
  () => {
    drawerOpen.value = false
    // 切题时先清空目录，避免上一题的小节短暂残留在右栏
    outline.value = []
    tocHint.value = '目录加载中…'
  },
  { immediate: true }
)

// 分类变化（首次进来回填 / 从题库页换分类）时重新拉左栏
watch(activeCategoryId, (id, previous) => {
  if (!id || id === previous) return
  loadSide()
})

// 首屏：URL 上已经带 categoryId（从题库页点进来）时直接拉，不必等详情 loaded
onMounted(() => {
  if (activeCategoryId.value) loadSide()
})

// 登录态变化（例如刚登录回来）时补拉一次，让「上次浏览」出现
watch(isLogin, () => {
  if (activeCategoryId.value) loadSide()
})

// 切题后把左侧当前项滚进可视区（题目多时用户可能看不到高亮项）
watch(activeId, async () => {
  drawerOpen.value = false
  await nextTick()
  const container = sideListRef.value
  if (!container) return
  const current = container.querySelector('.qp-list__btn.is-active')
  if (current) current.scrollIntoView({ block: 'nearest' })
})

function onKeydown(event) {
  if (event.key === 'Escape' && drawerOpen.value) drawerOpen.value = false
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ============================================================================
   三栏骨架
   ----------------------------------------------------------------------------
   用 Grid 而不是三个 flex 子项：左右定宽 + 中间 1fr 是 Grid 的标准用法，
   而且轨道在 1279 / 1023 两个断点切换时不会出现 flex 的 margin 抖动。
   ============================================================================ */
/* 外壳：铺满视口的灰底。路由走 fullBleed（底座是白底），
   若不铺这一层，宽屏下三栏两侧会露出白边，看起来像页面没对齐。 */
.qp-page {
  min-height: calc(100dvh - var(--header-height));
  background-color: var(--color-bg-page);
}

/*
  桌面（≥1024）的左栏要「贴页面最左边 + 从顶栏一直通到底部」，
  所以网格容器本身不再留左右内边距，改由各栏自己控制：
    - 左栏：无左边距、无上边距，吸顶到 --header-height；
    - 中/右栏：用 --qp-gap 保持呼吸，并各自保留上下内边距。
  窄屏（<1024）没有侧栏，容器恢复常规内边距。
*/
.qp {
  --qp-side-w: 268px;
  --qp-toc-w: 240px;
  --qp-gap: 20px;
  /* 中栏正文最大宽度：宽屏下别让一行字拉到 2000px 长（阅读体验） */
  --qp-main-max: 1080px;

  display: grid;
  grid-template-columns: var(--qp-side-w) minmax(0, 1fr) var(--qp-toc-w);
  gap: var(--qp-gap);
  align-items: start;
  width: 100%;
  /*
    这里**不能**用 max-width + margin:auto 居中 ——
    之前设了 1440px 上限，宽屏（≥1500）时整个三栏被推到中间，左侧空出一大块，
    左栏就不再贴页面最左边了。现在容器始终满宽，左栏自然从 x=0 开始；
    阅读宽度由中栏自己的 max-width 控制。
  */
  padding: 0 var(--space-6) 0 0;
  margin: 0;
}

/* 1024–1199：收掉右栏（目录） */
@media (max-width: 1199px) {
  .qp {
    grid-template-columns: var(--qp-side-w) minmax(0, 1fr);
  }
}

/* <1024：只剩中栏，左栏转为覆盖式抽屉 */
@media (max-width: 1023px) {
  .qp {
    display: block;
    padding: var(--space-4);
  }

  /* 抽屉模式下左栏是浮层，恢复常规卡片边距 */
  .qp-main {
    padding-top: 0;
    padding-bottom: 0;
  }
}

/* ---------- 左栏 ---------- */
/*
  贴住页面最左边、从全局顶栏下沿直通页面底部：
  - 取消圆角 / 左边框 / 上边距，让它成为页面的一条「边」而不是浮起的卡片；
  - 高度用 100dvh - header，比原来自适应内容更高，题目列表能一次多显示几行。
*/
.qp-side {
  position: sticky;
  top: var(--header-height);
  display: flex;
  flex-direction: column;
  height: calc(100dvh - var(--header-height));
  background-color: var(--color-bg-card);
  border-right: 1px solid var(--color-border);
  overflow: hidden;
}

.qp-side__head {
  display: flex;
  flex-shrink: 0;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-3) var(--space-2);
  border-bottom: 1px solid var(--color-border);
}

.qp-side__title {
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 600;
  line-height: 1.4;
}

.qp-side__count {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  font-variant-numeric: tabular-nums;
  line-height: 1.5;
}

/* 列表主体：唯一滚动容器，题多也不会把页面撑开 */
.qp-side__body {
  flex: 1 1 auto;
  min-height: 0;
  padding: var(--space-2) var(--space-2) var(--space-2) var(--space-1);
  overflow-y: auto;
  overscroll-behavior: contain;
  /* 细滚动条 + 留出与右侧卡片边缘的呼吸，默认滚动条会把文字压到贴边 */
  scrollbar-width: thin;
  scrollbar-gutter: stable;
}

.qp-side__skeleton {
  padding: var(--space-2);
}

.qp-side__error {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  align-items: flex-start;
  padding: var(--space-3);
}

.qp-side__error-text {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

.qp-side__empty {
  padding: var(--space-5) var(--space-3);
  text-align: center;
}

.qp-side__empty-title {
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 600;
  line-height: 1.5;
}

.qp-side__empty-desc {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 1.6;
}

.qp-side__empty-action {
  margin-top: var(--space-4);
}

.qp-list {
  padding: 0;
  margin: 0;
  list-style: none;
}

.qp-list__item + .qp-list__item {
  margin-top: 4px;
}

/* 列表项：题目名 +（可选）上次浏览。
   高度按「有没有浏览记录」分两档（v4.4）：
     有记录（双行）→ 80px，没记录（单行）→ 70px。
   两种行高都足够舒展，靠固定高度而不是 padding 撑开，
   这样混排时列表节奏是稳定的两档，不会参差不齐。 */
.qp-list__btn {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
  align-items: stretch;
  width: 100%;
  height: 70px;
  padding: 0 12px;
  color: inherit;
  font-family: inherit;
  text-align: left;
  background-color: transparent;
  border: 0;
  border-radius: var(--radius-button);
  cursor: pointer;
  transition:
    background-color var(--motion-fast) var(--motion-ease),
    color var(--motion-fast) var(--motion-ease);
}

/* 有浏览记录：双行 80px */
.qp-list__btn.is-visited {
  height: 80px;
}

.qp-list__btn:hover {
  background-color: var(--color-bg-page);
}

/* 激活态：左侧主色竖条 + 淡蓝底，比整块实心蓝更克制，也不抢中间正文 */
.qp-list__btn.is-active {
  position: relative;
  background-color: #eff6ff;
}

.qp-list__btn.is-active::before {
  position: absolute;
  top: 10px;
  bottom: 10px;
  left: 0;
  width: 3px;
  content: '';
  background-color: var(--color-primary);
  border-radius: 0 2px 2px 0;
}

/* 已读且选中时，竖条也跟着变绿，和标题同色系 */
.qp-list__btn.is-active.is-visited::before {
  background-color: var(--color-success-text);
}

/* 标题单行 + 省略号：列表是扫读用途，两行标题会让行高参差不齐，
   一屏少显示好几题。完整标题在 hover 的 title 属性与中间正文里都能看到。 */
.qp-list__title {
  /* 块级 + 满宽，text-overflow 才会生效（inline 元素上无效） */
  display: block;
  width: 100%;
  overflow: hidden;
  color: var(--color-text);
  /* v4.4：14.5px —— 比中栏正文 16px 小半档，比原来的 13.5 更清楚 */
  font-size: 14.5px;
  font-weight: 500;
  line-height: 1.6;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* v4.3 颜色语义：看过的绿、没看过的蓝。
   绿色用 --color-success-text（#15803d，白底 5.0:1），
   不用 --color-success(#16a34a，3.3:1) —— 那个只够做小徽标，14.5px 标题不够。 */
.qp-list__btn:not(.is-visited) .qp-list__title {
  color: var(--color-primary);
}

.qp-list__btn.is-visited .qp-list__title {
  color: var(--color-success-text);
}

/* 当前选中的题用「加粗 + 更深的同色系」强调，
   不再统一涂主色蓝 —— 否则会把「已读绿」的语义覆盖掉。 */
.qp-list__btn.is-active .qp-list__title {
  font-weight: 600;
}

.qp-list__btn.is-active:not(.is-visited) .qp-list__title {
  color: var(--color-primary);
}

.qp-list__btn.is-active.is-visited .qp-list__title {
  color: var(--color-success-text);
}

.qp-list__time {
  display: block;
  width: 100%;
  overflow: hidden;
  color: var(--color-text-secondary);
  /* v4.4：11 → 12px。268px 列表宽下「上次浏览：2026-11-20 15:27」仍能单行放下
     （约 160px），真正放不下时由 text-overflow 兜底显示省略号 */
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  line-height: 1.6;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 抽屉模式（<1024）：浮层通高，与桌面版一样从顶栏下沿到底部 */
.qp-side--drawer {
  position: fixed;
  top: var(--header-height);
  bottom: 0;
  left: 0;
  z-index: 60;
  width: min(84%, 320px);
  /* 桌面版用 height 通高，抽屉模式靠 top/bottom 定位，height 需清掉避免冲突 */
  height: auto;
  max-height: none;
  border-right: 0;
  border-radius: 0;
  transform: translateX(-102%);
  transition: transform var(--motion-base) var(--motion-ease);
}

.qp-side--drawer.qp-side--open {
  transform: translateX(0);
  box-shadow: 0 24px 48px -24px rgb(16 24 40 / 28%);
}

.qp-mask {
  position: fixed;
  inset: var(--header-height) 0 0 0;
  z-index: 55;
  background-color: rgb(16 24 40 / 24%);
}

/* ---------- 中栏 ---------- */
.qp-main {
  min-width: 0;
  /* 正文最大宽度：容器不再限宽，长行阅读体验由这里兜住 */
  max-width: var(--qp-main-max);
  /* 容器左右内边距已归零，上下呼吸由中栏自己兜（左栏现在是通高的边） */
  padding-top: var(--space-4);
  padding-bottom: var(--space-8);
}

.qp-main__bar {
  margin-bottom: var(--space-3);
}

.qp-main__drawer-btn {
  min-height: 40px;
}

.qp-main__drawer-btn > svg {
  margin-right: var(--space-1);
}

/* ---------- 右栏 ---------- */
.qp-toc {
  position: sticky;
  /* 容器上内边距已归零，这里直接把吸顶位置放到顶栏下沿 + 一点呼吸 */
  top: calc(var(--header-height) + var(--space-4));
  display: flex;
  flex-direction: column;
  max-height: calc(100dvh - var(--header-height) - var(--space-8));
  margin-top: var(--space-4);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  overflow: hidden;
}

.qp-toc__head {
  flex-shrink: 0;
  padding: var(--space-3) var(--space-3) var(--space-2);
  border-bottom: 1px solid var(--color-border);
}

.qp-toc__title {
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 600;
  line-height: 1.4;
}

.qp-toc__nav {
  flex: 1 1 auto;
  min-height: 0;
  padding: var(--space-3) var(--space-2) var(--space-4);
  overflow-y: auto;
  overscroll-behavior: contain;
}

.qp-toc__group {
  padding: 0 var(--space-2) var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.5;
}

.qp-toc__group--gap {
  margin-top: var(--space-4);
}

.qp-toc__list {
  padding: 0;
  margin: 0;
  list-style: none;
}

/* 目录项：本质是「跳到某小节」的按钮，做成一整行可点的窄条 */
.qp-toc__link {
  display: block;
  width: 100%;
  padding: var(--space-1) var(--space-2);
  overflow: hidden;
  color: var(--color-text-secondary);
  font-family: inherit;
  font-size: 13px;
  line-height: 1.6;
  text-align: left;
  white-space: nowrap;
  text-overflow: ellipsis;
  background: transparent;
  border: 0;
  border-left: 2px solid transparent;
  border-radius: 0 var(--radius-tag) var(--radius-tag) 0;
  cursor: pointer;
  transition:
    color var(--motion-fast) var(--motion-ease),
    background-color var(--motion-fast) var(--motion-ease),
    border-color var(--motion-fast) var(--motion-ease);
}

.qp-toc__link:hover {
  color: var(--color-primary);
  background-color: #eff6ff;
  border-left-color: var(--color-primary);
}

.qp-toc__placeholder {
  padding: 0 var(--space-2) var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 1.6;
}

@media (prefers-reduced-motion: reduce) {
  .qp-side--drawer {
    transition: none;
  }
}
</style>
