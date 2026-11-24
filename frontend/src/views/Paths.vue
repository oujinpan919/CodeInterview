<template>
  <section class="cp">
    <!-- 页头 -->
    <header class="cp-head">
      <h1 class="cp-head__title">实习刷题路线</h1>
      <p class="cp-head__sub">
        按岗位拆开准备的路线：先看 JD 要什么，再决定刷什么题、做什么项目、面试讲什么。
        每个岗位都给出阶段安排、题单、项目产出和可量化的完成标准。
      </p>
      <p class="cp-head__note">
        <span class="cp-head__note-mark" aria-hidden="true">i</span>
        {{ disclaimer }}
      </p>
    </header>

    <!-- 岗位切换 -->
    <nav class="cp-tabs" role="tablist" aria-label="选择岗位路线">
      <button
        v-for="path in careers"
        :key="path.id"
        type="button"
        role="tab"
        class="cp-tab"
        :class="{ 'is-active': path.id === activeId }"
        :aria-selected="path.id === activeId ? 'true' : 'false'"
        @click="selectPath(path.id)"
      >
        <span class="cp-tab__name">{{ path.name }}</span>
        <span class="cp-tab__meta">{{ path.months }} · 每周约 {{ weeklyHoursNumber(path) }} 小时</span>
      </button>
    </nav>

    <template v-if="active">
      <!-- 岗位概览 -->
      <section class="cp-hero">
        <div class="cp-hero__main">
          <h2 class="cp-hero__title">{{ active.name }} · 实习路线</h2>
          <p class="cp-hero__tagline">{{ active.tagline }}</p>
          <p class="cp-hero__scenario">{{ active.scenario }}</p>
        </div>

        <dl class="cp-hero__facts">
          <div class="cp-fact">
            <dt>建议周期</dt>
            <dd>{{ active.months }}</dd>
          </div>
          <div class="cp-fact">
            <dt>投入强度</dt>
            <dd>{{ active.weeklyHours }}</dd>
          </div>
          <div class="cp-fact">
            <dt>题单总量</dt>
            <dd>{{ totalQuestions }} 题</dd>
          </div>
          <div class="cp-fact">
            <dt>适用岗位</dt>
            <dd>{{ active.targetRoles.length }} 类</dd>
          </div>
        </dl>

        <ul class="cp-roles">
          <li v-for="role in active.targetRoles" :key="role" class="cp-roles__item">{{ role }}</li>
        </ul>
      </section>

      <!-- JD 拆解 -->
      <section class="cp-block">
        <header class="cp-block__head">
          <h2 class="cp-block__title">岗位 JD 要求拆解</h2>
          <p class="cp-block__hint">按面试实际考察的模块拆开，每一项都对应后面的题单与阶段任务</p>
        </header>
        <div class="cp-jd">
          <article v-for="group in active.jd" :key="group.area" class="cp-jd__item">
            <h3 class="cp-jd__area">{{ group.area }}</h3>
            <ul class="cp-jd__list">
              <li v-for="(item, i) in group.items" :key="i">{{ item }}</li>
            </ul>
          </article>
        </div>
      </section>

      <!-- 阶段路径（时间轴） -->
      <section class="cp-block">
        <header class="cp-block__head">
          <h2 class="cp-block__title">分阶段学习路径</h2>
          <p class="cp-block__hint">四个阶段有先后依赖：基础没补齐就刷难题，效率会很低</p>
        </header>
        <ol class="cp-timeline">
          <li v-for="(stage, index) in active.stages" :key="stage.key" class="cp-stage">
            <div class="cp-stage__rail" aria-hidden="true">
              <span class="cp-stage__dot">{{ index + 1 }}</span>
            </div>
            <div class="cp-stage__body">
              <div class="cp-stage__top">
                <h3 class="cp-stage__name">{{ stage.name }}</h3>
                <span class="cp-stage__weeks">{{ stage.weeks }}</span>
              </div>
              <p class="cp-stage__goal">
                <span class="cp-stage__goal-label">阶段目标</span>
                {{ stage.goal }}
              </p>
              <div class="cp-stage__tasks">
                <p class="cp-stage__tasks-label">每周任务量</p>
                <ul class="cp-stage__list">
                  <li v-for="(task, i) in stage.weeklyTasks" :key="i">{{ task }}</li>
                </ul>
              </div>
              <p v-if="stage.deliverable" class="cp-stage__out">
                <span class="cp-stage__out-label">阶段产出</span>
                {{ stage.deliverable }}
              </p>
            </div>
          </li>
        </ol>
      </section>

      <!-- 题单 -->
      <section class="cp-block">
        <header class="cp-block__head">
          <h2 class="cp-block__title">刷题主题与题单</h2>
          <p class="cp-block__hint">
            共 {{ totalQuestions }} 题 · 点主题展开细则；「去刷题」会跳到题库对应分类
          </p>
        </header>

        <div class="cp-qlist">
          <article v-for="group in active.questionList" :key="group.topic" class="cp-qtopic">
            <button
              type="button"
              class="cp-qtopic__head"
              :aria-expanded="isOpen(group.topic) ? 'true' : 'false'"
              @click="toggleTopic(group.topic)"
            >
              <span class="cp-qtopic__caret" :class="{ 'is-open': isOpen(group.topic) }" aria-hidden="true">▾</span>
              <span class="cp-qtopic__name">{{ group.topic }}</span>
              <span class="cp-qtopic__count">{{ group.count }} 题</span>
              <span class="cp-qtopic__bar" aria-hidden="true">
                <i class="cp-qtopic__bar-easy" :style="{ width: pct(group.difficulty.easy, group.count) }"></i>
                <i class="cp-qtopic__bar-medium" :style="{ width: pct(group.difficulty.medium, group.count) }"></i>
                <i class="cp-qtopic__bar-hard" :style="{ width: pct(group.difficulty.hard, group.count) }"></i>
              </span>
            </button>

            <div v-show="isOpen(group.topic)" class="cp-qtopic__panel">
              <div class="cp-qtopic__difficulty">
                <span class="cp-diff cp-diff--easy">简单 {{ group.difficulty.easy }}</span>
                <span class="cp-diff cp-diff--medium">中等 {{ group.difficulty.medium }}</span>
                <span class="cp-diff cp-diff--hard">困难 {{ group.difficulty.hard }}</span>
                <span class="cp-diff__ratio">
                  比例 {{ ratioText(group.difficulty) }}
                </span>
              </div>

              <p class="cp-qtopic__row">
                <span class="cp-qtopic__label">高频题类型</span>
                {{ group.hot }}
              </p>
              <p class="cp-qtopic__row">
                <span class="cp-qtopic__label">手撕代码</span>
                {{ group.handWrite }}
              </p>

              <RouterLink
                v-if="group.category"
                class="cp-qtopic__link"
                :to="{ path: '/question', query: { categoryId: String(categoryIdOf(group.category)) } }"
              >
                去刷「{{ group.category }}」→
              </RouterLink>
              <span v-else class="cp-qtopic__nolink">题库暂无对应分类，按主题自行搜题</span>
            </div>
          </article>
        </div>
      </section>

      <!-- 实习准备产出 -->
      <section class="cp-block">
        <header class="cp-block__head">
          <h2 class="cp-block__title">实习准备产出</h2>
          <p class="cp-block__hint">项目、简历、面试表达三件事，比多刷 20 道题更能决定结果</p>
        </header>

        <div class="cp-portfolio">
          <article class="cp-pf-card">
            <h3 class="cp-pf-card__title">简历项目建议</h3>
            <ul class="cp-pf-card__list">
              <li v-for="(item, i) in active.portfolio.projects" :key="i">{{ item }}</li>
            </ul>
          </article>

          <article class="cp-pf-card">
            <h3 class="cp-pf-card__title">项目描述要点</h3>
            <ul class="cp-pf-card__list">
              <li v-for="(item, i) in active.portfolio.description" :key="i">{{ item }}</li>
            </ul>
          </article>

          <article class="cp-pf-card">
            <h3 class="cp-pf-card__title">面试可讲亮点</h3>
            <ul class="cp-pf-card__list">
              <li v-for="(item, i) in active.portfolio.highlights" :key="i">{{ item }}</li>
            </ul>
          </article>

          <article class="cp-pf-card cp-pf-card--ask">
            <h3 class="cp-pf-card__title">常见追问</h3>
            <ol class="cp-pf-card__list cp-pf-card__list--ordered">
              <li v-for="(item, i) in active.portfolio.followUps" :key="i">{{ item }}</li>
            </ol>
          </article>

          <article class="cp-pf-card cp-pf-card--check">
            <h3 class="cp-pf-card__title">项目自测清单</h3>
            <ul class="cp-checklist">
              <li v-for="(item, i) in active.portfolio.checklist" :key="i" class="cp-checklist__item">
                <span class="cp-checklist__box" aria-hidden="true">✓</span>
                {{ item }}
              </li>
            </ul>
          </article>
        </div>
      </section>

      <!-- 里程碑 -->
      <section class="cp-block">
        <header class="cp-block__head">
          <h2 class="cp-block__title">完成标准与里程碑</h2>
          <p class="cp-block__hint">达到全部四条，基本可以开始投递；单条未达成时优先补那一条</p>
        </header>
        <ul class="cp-milestones">
          <li v-for="(m, i) in active.milestones" :key="i" class="cp-milestone">
            <span class="cp-milestone__num">{{ i + 1 }}</span>
            <span class="cp-milestone__weight">{{ m.weight }}</span>
            <span class="cp-milestone__text">{{ m.text }}</span>
          </li>
        </ul>
      </section>

      <!-- 关联的题库题单（后端维护的现成题集） -->
      <section v-if="libraryPaths.length" class="cp-block">
        <header class="cp-block__head">
          <h2 class="cp-block__title">题库里的现成题单</h2>
          <p class="cp-block__hint">按难度排序录好的题集，适合直接开刷并记录进度</p>
        </header>
        <div class="cp-lib">
          <RouterLink
            v-for="lp in libraryPaths"
            :key="lp.id"
            class="cp-lib__item"
            :to="{ name: 'path-detail', params: { id: lp.id } }"
          >
            <span class="cp-lib__name">{{ lp.name }}</span>
            <span class="cp-lib__meta">
              {{ lp.completedCount === null ? `共 ${lp.totalCount} 题` : `${lp.completedCount} / ${lp.totalCount} 题` }}
            </span>
          </RouterLink>
        </div>
      </section>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { CAREER_PATHS, PATH_DISCLAIMER } from '@/data/careerPaths'
import { getCategories } from '@/api/taxonomy'
import { getLearningPaths } from '@/api/path'

const careers = CAREER_PATHS
const disclaimer = PATH_DISCLAIMER

const STORAGE_KEY = 'codeinterview:career-path'

const activeId = ref(careers[0].id)
const openTopics = ref(new Set())
/** 展开第一个主题，避免首屏看起来全是折叠的空壳 */
if (careers[0].questionList[0]) openTopics.value = new Set([careers[0].questionList[0].topic])

/** 分类名 → id，用于「去刷题」跳转到题库筛选 */
const categoryMap = ref({})
/** 后端维护的现成题单（可能为空） */
const libraryPaths = ref([])

const active = computed(() => careers.find((item) => item.id === activeId.value) || null)

const totalQuestions = computed(() =>
  active.value ? active.value.questionList.reduce((sum, group) => sum + group.count, 0) : 0
)

function weeklyHoursNumber(path) {
  const match = String(path.weeklyHours).match(/\d+/)
  return match ? match[0] : ''
}

function selectPath(id) {
  activeId.value = id
  const first = careers.find((item) => item.id === id)
  openTopics.value = new Set(first && first.questionList[0] ? [first.questionList[0].topic] : [])
  persist()
  // 切换岗位后回到岗位概览，避免停在上一份路线的中间位置
  if (typeof window !== 'undefined') {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function persist() {
  try {
    sessionStorage.setItem(STORAGE_KEY, activeId.value)
  } catch {
    // 隐私模式下 sessionStorage 可能不可写，忽略即可
  }
}

function restore() {
  try {
    const saved = sessionStorage.getItem(STORAGE_KEY)
    if (saved && careers.some((item) => item.id === saved)) {
      activeId.value = saved
      const found = careers.find((item) => item.id === saved)
      openTopics.value = new Set(found && found.questionList[0] ? [found.questionList[0].topic] : [])
    }
  } catch {
    // 同上
  }
}

function isOpen(topic) {
  return openTopics.value.has(topic)
}

function toggleTopic(topic) {
  const next = new Set(openTopics.value)
  if (next.has(topic)) next.delete(topic)
  else next.add(topic)
  openTopics.value = next
}

function pct(value, total) {
  if (!total) return '0%'
  return `${Math.round((value / total) * 100)}%`
}

function ratioText(d) {
  const total = d.easy + d.medium + d.hard
  if (!total) return '—'
  const gcd = (a, b) => (b ? gcd(b, a % b) : a)
  const base = gcd(gcd(d.easy, d.medium), d.hard) || 1
  return `${d.easy / base} : ${d.medium / base} : ${d.hard / base}`
}

function categoryIdOf(name) {
  const hit = categoryMap.value[name]
  return hit === undefined ? '' : hit
}

async function loadCategoryMap() {
  try {
    const list = await getCategories()
    const map = {}
    ;(Array.isArray(list) ? list : []).forEach((cat) => {
      map[cat.name] = cat.id
    })
    categoryMap.value = map
  } catch {
    // 分类拿不到时「去刷题」链接会退化为跳题库首页，不影响内容阅读
    categoryMap.value = {}
  }
}

async function loadLibraryPaths() {
  try {
    const list = await getLearningPaths()
    libraryPaths.value = Array.isArray(list) ? list : []
  } catch {
    libraryPaths.value = []
  }
}

onMounted(() => {
  restore()
  loadCategoryMap()
  loadLibraryPaths()
})
</script>

<style scoped>
.cp {
  min-width: 0;
  padding-bottom: var(--space-8);
}

/* ---------- 页头 ---------- */
.cp-head {
  margin-bottom: var(--space-5);
}

.cp-head__title {
  color: var(--color-text);
  font-size: var(--font-size-h1);
  line-height: var(--line-height-h1);
  font-weight: 600;
}

.cp-head__sub {
  max-width: 780px;
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.cp-head__note {
  display: flex;
  gap: var(--space-2);
  align-items: flex-start;
  max-width: 780px;
  padding: var(--space-3);
  margin-top: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 1.7;
  background-color: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-button);
}

.cp-head__note-mark {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-top: 2px;
  color: #fff;
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
  line-height: 1;
  background-color: var(--color-text-secondary);
  border-radius: 50%;
}

/* ---------- 岗位切换 ---------- */
.cp-tabs {
  display: flex;
  gap: var(--space-2);
  padding-bottom: var(--space-2);
  margin-bottom: var(--space-5);
  overflow-x: auto;
  border-bottom: 1px solid var(--color-border);
  scrollbar-width: thin;
}

.cp-tab {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  gap: 2px;
  padding: var(--space-3) var(--space-4);
  color: var(--color-text-secondary);
  font-family: inherit;
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-bottom: 2px solid transparent;
  border-radius: var(--radius-button) var(--radius-button) 0 0;
  cursor: pointer;
  transition:
    color var(--motion-fast) var(--motion-ease),
    background-color var(--motion-fast) var(--motion-ease),
    border-color var(--motion-fast) var(--motion-ease);
}

.cp-tab:hover {
  color: var(--color-text);
  background-color: var(--color-bg-page);
}

.cp-tab.is-active {
  color: var(--color-primary);
  background-color: var(--color-bg-card);
  border-color: var(--color-border);
  border-bottom-color: var(--color-primary);
}

.cp-tab__name {
  font-size: var(--font-size-body);
  font-weight: 600;
  white-space: nowrap;
}

.cp-tab__meta {
  font-size: var(--font-size-aux);
  white-space: nowrap;
}

/* ---------- 岗位概览 ---------- */
.cp-hero {
  padding: var(--space-5);
  margin-bottom: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.cp-hero__title {
  color: var(--color-text);
  font-size: 20px;
  font-weight: 600;
  line-height: 1.4;
}

.cp-hero__tagline {
  margin-top: var(--space-1);
  color: var(--color-primary);
  font-size: var(--font-size-aux);
  font-weight: 500;
}

.cp-hero__scenario {
  max-width: 820px;
  margin-top: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  line-height: 1.8;
}

.cp-hero__facts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
  padding-top: var(--space-4);
  margin-top: var(--space-4);
  border-top: 1px dashed var(--color-border);
}

.cp-fact dt {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

.cp-fact dd {
  margin-top: 2px;
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 600;
}

.cp-roles {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.cp-roles__item {
  padding: 4px var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 1.6;
  background-color: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-tag);
}

/* ---------- 区块 ---------- */
.cp-block {
  margin-bottom: var(--space-6);
}

.cp-block__head {
  margin-bottom: var(--space-3);
}

.cp-block__title {
  color: var(--color-text);
  font-size: var(--font-size-h2);
  font-weight: 600;
  line-height: 1.5;
}

.cp-block__hint {
  margin-top: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 1.7;
}

/* ---------- JD 拆解 ---------- */
.cp-jd {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.cp-jd__item {
  padding: var(--space-4);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.cp-jd__area {
  padding-bottom: var(--space-2);
  margin-bottom: var(--space-2);
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 600;
  border-bottom: 1px solid var(--color-border);
}

.cp-jd__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.cp-jd__list li {
  position: relative;
  padding-left: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 1.75;
}

.cp-jd__list li::before {
  position: absolute;
  top: 9px;
  left: 2px;
  width: 4px;
  height: 4px;
  content: '';
  background-color: var(--color-border);
  border-radius: 50%;
}

/* ---------- 阶段时间轴 ---------- */
.cp-timeline {
  display: flex;
  flex-direction: column;
}

.cp-stage {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: var(--space-3);
}

.cp-stage__rail {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.cp-stage__dot {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  background-color: var(--color-primary);
  border-radius: 50%;
}

.cp-stage:not(:last-child) .cp-stage__rail::after {
  width: 2px;
  flex: 1;
  margin: var(--space-2) 0;
  content: '';
  background-color: var(--color-border);
}

.cp-stage__body {
  padding: var(--space-4);
  margin-bottom: var(--space-3);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.cp-stage__top {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  align-items: baseline;
  justify-content: space-between;
}

.cp-stage__name {
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 600;
}

.cp-stage__weeks {
  padding: 2px var(--space-2);
  color: var(--color-primary);
  font-size: var(--font-size-aux);
  background-color: rgb(var(--color-primary-rgb) / 8%);
  border-radius: var(--radius-tag);
}

.cp-stage__goal {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 1.75;
}

.cp-stage__goal-label,
.cp-stage__out-label {
  display: inline-block;
  padding: 1px 6px;
  margin-right: var(--space-1);
  color: var(--color-text);
  font-size: 11.5px;
  background-color: var(--color-bg-page);
  border-radius: var(--radius-tag);
}

.cp-stage__tasks {
  margin-top: var(--space-3);
}

.cp-stage__tasks-label {
  color: var(--color-text);
  font-size: var(--font-size-aux);
  font-weight: 500;
}

.cp-stage__list {
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-top: var(--space-1);
}

.cp-stage__list li {
  position: relative;
  padding-left: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 1.7;
}

.cp-stage__list li::before {
  position: absolute;
  top: 8px;
  left: 2px;
  width: 5px;
  height: 1.5px;
  content: '';
  background-color: var(--color-text-secondary);
  border-radius: 1px;
}

.cp-stage__out {
  padding-top: var(--space-2);
  margin-top: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 1.75;
  border-top: 1px dashed var(--color-border);
}

/* ---------- 题单 ---------- */
.cp-qlist {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.cp-qtopic {
  overflow: hidden;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.cp-qtopic__head {
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr) auto 96px;
  gap: var(--space-2);
  align-items: center;
  width: 100%;
  padding: var(--space-3) var(--space-4);
  font-family: inherit;
  text-align: left;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: background-color var(--motion-fast) var(--motion-ease);
}

.cp-qtopic__head:hover {
  background-color: var(--color-bg-page);
}

.cp-qtopic__caret {
  color: var(--color-text-secondary);
  font-size: 11px;
  transition: transform var(--motion-fast) var(--motion-ease);
}

.cp-qtopic__caret.is-open {
  transform: rotate(180deg);
}

.cp-qtopic__name {
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 600;
}

.cp-qtopic__count {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

/* 难度分布条：三段按题量比例铺满 */
.cp-qtopic__bar {
  display: flex;
  width: 96px;
  height: 5px;
  overflow: hidden;
  background-color: var(--color-bg-page);
  border-radius: 3px;
}

.cp-qtopic__bar-easy {
  height: 100%;
  background-color: #4caf82;
}

.cp-qtopic__bar-medium {
  height: 100%;
  background-color: #e6a23c;
}

.cp-qtopic__bar-hard {
  height: 100%;
  background-color: #e06c75;
}

.cp-qtopic__panel {
  padding: 0 var(--space-4) var(--space-4);
  border-top: 1px solid var(--color-border);
}

.cp-qtopic__difficulty {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  align-items: center;
  padding-top: var(--space-3);
}

.cp-diff {
  padding: 2px var(--space-2);
  font-size: var(--font-size-aux);
  line-height: 1.6;
  border-radius: var(--radius-tag);
}

.cp-diff--easy {
  color: #2f7a5b;
  background-color: rgb(76 175 130 / 12%);
}

.cp-diff--medium {
  color: #9a6a13;
  background-color: rgb(230 162 60 / 14%);
}

.cp-diff--hard {
  color: #b0505a;
  background-color: rgb(224 108 117 / 14%);
}

.cp-diff__ratio {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  font-variant-numeric: tabular-nums;
}

.cp-qtopic__row {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 1.8;
}

.cp-qtopic__label {
  display: inline-block;
  min-width: 76px;
  color: var(--color-text);
  font-weight: 500;
}

.cp-qtopic__link {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  margin-top: var(--space-3);
  color: var(--color-primary);
  font-size: var(--font-size-aux);
  font-weight: 500;
}

.cp-qtopic__link:hover {
  text-decoration: underline;
}

.cp-qtopic__nolink {
  display: inline-block;
  margin-top: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

/* ---------- 项目产出 ---------- */
.cp-portfolio {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.cp-pf-card {
  padding: var(--space-4);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.cp-pf-card__title {
  padding-bottom: var(--space-2);
  margin-bottom: var(--space-2);
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 600;
  border-bottom: 1px solid var(--color-border);
}

.cp-pf-card__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.cp-pf-card__list li {
  position: relative;
  padding-left: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 1.75;
}

.cp-pf-card__list li::before {
  position: absolute;
  top: 9px;
  left: 2px;
  width: 4px;
  height: 4px;
  content: '';
  background-color: var(--color-border);
  border-radius: 50%;
}

.cp-pf-card__list--ordered {
  counter-reset: followup;
}

.cp-pf-card__list--ordered li {
  counter-increment: followup;
  padding-left: var(--space-5);
}

.cp-pf-card__list--ordered li::before {
  top: 0;
  left: 0;
  width: auto;
  height: auto;
  color: var(--color-primary);
  font-size: var(--font-size-aux);
  font-weight: 600;
  content: counter(followup) '.';
  background: none;
  border-radius: 0;
}

.cp-pf-card--ask {
  border-color: rgb(var(--color-primary-rgb) / 24%);
}

.cp-checklist {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.cp-checklist__item {
  display: flex;
  gap: var(--space-2);
  align-items: flex-start;
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: 1.75;
}

.cp-checklist__box {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 15px;
  height: 15px;
  margin-top: 3px;
  color: var(--color-success-text, #15803d);
  font-size: 10px;
  background-color: rgb(76 175 130 / 12%);
  border-radius: 3px;
}

/* ---------- 里程碑 ---------- */
.cp-milestones {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.cp-milestone {
  display: grid;
  grid-template-columns: 26px auto minmax(0, 1fr);
  gap: var(--space-2);
  align-items: center;
  padding: var(--space-3) var(--space-4);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.cp-milestone__num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  color: var(--color-primary);
  font-size: var(--font-size-aux);
  font-weight: 600;
  background-color: rgb(var(--color-primary-rgb) / 9%);
  border-radius: 50%;
}

.cp-milestone__weight {
  padding: 2px var(--space-2);
  color: var(--color-text-secondary);
  font-size: 11.5px;
  background-color: var(--color-bg-page);
  border-radius: var(--radius-tag);
}

.cp-milestone__text {
  color: var(--color-text);
  font-size: var(--font-size-aux);
  line-height: 1.7;
}

/* ---------- 现成题单 ---------- */
.cp-lib {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.cp-lib__item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-4);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  transition: border-color var(--motion-fast) var(--motion-ease);
}

.cp-lib__item:hover {
  border-color: var(--color-primary);
}

.cp-lib__name {
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 600;
}

.cp-lib__meta {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
}

/* ---------- 响应式 ---------- */
@media (max-width: 1023px) {
  .cp-jd,
  .cp-portfolio,
  .cp-lib {
    grid-template-columns: minmax(0, 1fr);
  }

  .cp-hero__facts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .cp-hero {
    padding: var(--space-4);
  }

  .cp-hero__facts {
    gap: var(--space-2);
  }

  /* 题单头在窄屏改为两行：标题独占一行，题量与难度条落到第二行 */
  .cp-qtopic__head {
    grid-template-columns: 16px minmax(0, 1fr) auto;
    gap: var(--space-1) var(--space-2);
    padding: var(--space-3);
  }

  .cp-qtopic__bar {
    grid-column: 2 / -1;
    width: 100%;
  }

  .cp-qtopic__panel {
    padding: 0 var(--space-3) var(--space-3);
  }

  .cp-qtopic__label {
    display: block;
    min-width: 0;
  }

  .cp-stage {
    grid-template-columns: 28px minmax(0, 1fr);
    gap: var(--space-2);
  }

  .cp-stage__dot {
    width: 24px;
    height: 24px;
    font-size: 12px;
  }

  .cp-stage__body {
    padding: var(--space-3);
  }

  .cp-milestone {
    grid-template-columns: 22px minmax(0, 1fr);
    gap: var(--space-1) var(--space-2);
  }

  .cp-milestone__weight {
    justify-self: start;
  }

  .cp-milestone__text {
    grid-column: 1 / -1;
  }

  .cp-tab {
    padding: var(--space-2) var(--space-3);
  }
}
</style>
