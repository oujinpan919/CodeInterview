<template>
  <section class="profile-page">
    <!-- ============ 顶部身份横幅：头像 + 昵称 + 关键数据 + 主操作 ============ -->
    <section class="profile-hero" aria-labelledby="profile-hero-title">
      <div class="profile-hero__identity">
        <!-- v4.12：头像可上传。有图显示图，无图回退首字母色块；hover/focus 显示「更换」蒙层 -->
        <ImageUploader
          folder="avatar"
          aria-label="更换头像"
          class="profile-hero__avatar-uploader"
          @uploaded="handleAvatarUploaded"
        >
          <template #default="{ uploading }">
            <span class="profile-hero__avatar" :class="{ 'is-uploading': uploading }">
              <img
                v-if="avatarUrl"
                class="profile-hero__avatar-img"
                :src="avatarUrl"
                :alt="`${nickname} 的头像`"
              />
              <template v-else>{{ avatarText }}</template>
              <span class="profile-hero__avatar-mask" aria-hidden="true">
                {{ uploading ? '上传中…' : '更换' }}
              </span>
            </span>
          </template>
        </ImageUploader>
        <div class="profile-hero__meta">
          <div class="profile-hero__name-row">
            <h1 id="profile-hero-title" class="profile-hero__nickname">{{ nickname }}</h1>
            <el-tag :type="isAdmin ? 'warning' : 'info'" effect="plain" size="small">
              {{ isAdmin ? '管理员' : '普通用户' }}
            </el-tag>
          </div>
          <p v-if="joinedText || lastLoginText" class="profile-hero__sub">
            <span v-if="joinedText">加入于 {{ joinedText }}</span>
            <span v-if="joinedText && lastLoginText" class="profile-hero__dot">·</span>
            <span v-if="lastLoginText">最近登录 {{ lastLoginText }}</span>
          </p>
        </div>
      </div>

      <div class="profile-hero__stats">
        <div v-for="item in heroStats" :key="item.key" class="profile-hero__stat">
          <span class="profile-hero__stat-value">{{ item.value }}</span>
          <span class="profile-hero__stat-label">{{ item.label }}</span>
        </div>
      </div>
    </section>

    <!-- ============ 学习数据概览 ============ -->
    <section class="profile-section" aria-labelledby="profile-stats-title">
      <header class="profile-section__head">
        <h2 id="profile-stats-title" class="profile-section__title">学习数据</h2>
        <el-button link type="primary" @click="router.push('/records')">查看完整统计 →</el-button>
      </header>

      <div v-if="statsLoading" class="profile-stats profile-stats--loading" aria-busy="true">
        <div v-for="i in 4" :key="`sk-${i}`" class="profile-stats__skeleton" />
      </div>

      <div v-else class="profile-stats">
        <article v-for="card in statCards" :key="card.key" class="profile-stat">
          <p class="profile-stat__label">{{ card.label }}</p>
          <p class="profile-stat__value">
            {{ card.value }}<span v-if="card.unit" class="profile-stat__unit">{{ card.unit }}</span>
          </p>
          <p class="profile-stat__hint">{{ card.hint }}</p>
        </article>
      </div>

      <!-- 今日复习进度条：有到期才显示，避免空态噪声 -->
      <div v-if="!statsLoading && reviewTotal > 0" class="profile-review">
        <div class="profile-review__copy">
          <p class="profile-review__title">今日复习</p>
          <p class="profile-review__desc">
            <span v-if="review.overdueCount > 0" class="profile-review__overdue">
              逾期 {{ review.overdueCount }} 道
            </span>
            <span>待复习 {{ review.dueCount }} 道</span>
            <span>新题 {{ review.newCount }} 道</span>
            <span>约 {{ review.estimatedMinutes }} 分钟</span>
          </p>
        </div>
        <el-button type="primary" :loading="reviewLoading" @click="startReview">开始复习</el-button>
      </div>
    </section>

    <!-- ============ 快捷入口 ============ -->
    <section class="profile-section" aria-labelledby="profile-links-title">
      <header class="profile-section__head">
        <h2 id="profile-links-title" class="profile-section__title">快捷入口</h2>
      </header>

      <div class="profile-links">
        <button
          v-for="link in quickLinks"
          :key="link.key"
          type="button"
          class="profile-link"
          @click="router.push(link.to)"
        >
          <span class="profile-link__icon" aria-hidden="true">
            <svg
              viewBox="0 0 24 24"
              width="22"
              height="22"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path v-for="(d, i) in link.paths" :key="i" :d="d" />
              <circle
                v-for="(c, i) in link.circles || []"
                :key="`c-${i}`"
                :cx="c[0]"
                :cy="c[1]"
                :r="c[2]"
              />
            </svg>
          </span>
          <span class="profile-link__body">
            <span class="profile-link__title">{{ link.title }}</span>
            <span class="profile-link__desc">{{ link.desc }}</span>
          </span>
          <span v-if="link.badge" class="profile-link__badge">{{ link.badge }}</span>
          <span class="profile-link__arrow" aria-hidden="true">→</span>
        </button>
      </div>
    </section>

    <!-- ============ 账号设置 ============ -->
    <section class="profile-section" aria-labelledby="profile-settings-title">
      <header class="profile-section__head">
        <h2 id="profile-settings-title" class="profile-section__title">账号设置</h2>
      </header>

      <div class="profile-panels">
        <!-- 修改昵称 -->
        <section class="profile-panel">
          <h3 class="profile-panel__title">修改昵称</h3>
          <p class="profile-panel__desc">昵称同时是登录名，修改后需要重新登录。</p>

          <el-form
            ref="nicknameFormRef"
            :model="nicknameForm"
            :rules="nicknameRules"
            label-position="top"
            @submit.prevent="handleNicknameSubmit"
          >
            <el-form-item label="新昵称" prop="nickname" :error="nicknameFieldError">
              <el-input
                v-model="nicknameForm.nickname"
                maxlength="20"
                autocomplete="username"
                @input="nicknameFieldError = ''"
              />
            </el-form-item>

            <el-form-item label="当前密码" prop="password" :error="nicknamePasswordError">
              <el-input
                v-model="nicknameForm.password"
                type="password"
                show-password
                autocomplete="current-password"
                placeholder="请输入当前密码"
                maxlength="32"
                @input="nicknamePasswordError = ''"
              />
            </el-form-item>

            <p class="profile-panel__error" role="alert">{{ nicknameFormError }}</p>

            <el-button
              class="profile-panel__submit"
              type="primary"
              native-type="submit"
              :loading="nicknameSubmitting"
            >
              保存昵称
            </el-button>
          </el-form>
        </section>

        <!-- 修改密码 -->
        <section class="profile-panel">
          <h3 class="profile-panel__title">修改密码</h3>
          <p class="profile-panel__desc">修改成功后需要重新登录。</p>

          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-position="top"
            @submit.prevent="handlePasswordSubmit"
          >
            <el-form-item label="旧密码" prop="oldPassword" :error="oldPasswordError">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                show-password
                autocomplete="current-password"
                placeholder="请输入旧密码"
                maxlength="32"
                @input="oldPasswordError = ''"
              />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword" :error="newPasswordError">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                autocomplete="new-password"
                placeholder="6–32 位"
                maxlength="32"
                @input="newPasswordError = ''"
              />
            </el-form-item>

            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
                autocomplete="new-password"
                placeholder="请再次输入新密码"
                maxlength="32"
              />
            </el-form-item>

            <p class="profile-panel__error" role="alert">{{ passwordFormError }}</p>

            <el-button
              class="profile-panel__submit"
              type="primary"
              native-type="submit"
              :loading="passwordSubmitting"
            >
              保存密码
            </el-button>
          </el-form>
        </section>
      </div>

      <!-- 我的数据：导出 + 危险操作 -->
      <section class="profile-panel profile-panel--full">
        <h3 class="profile-panel__title">我的数据</h3>
        <p class="profile-panel__desc">
          把题目笔记导出成单个 Markdown 文件（按分类分组），随时可以带走或换工具。
        </p>
        <el-button :loading="exportingNotes" @click="handleExportNotes">
          导出笔记（.md）
        </el-button>
      </section>
    </section>

    <!-- ============ 退出登录 ============ -->
    <div class="profile-page__logout">
      <el-button type="danger" plain :loading="loggingOut" @click="handleLogout">
        退出登录
      </el-button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message-box.css'
import dayjs from 'dayjs'
import { getMe, updateAvatar, updatePassword, updateProfile } from '@/api/auth'
import { exportNotes } from '@/api/note'
import { getReviewSummary } from '@/api/review'
import { getStatsOverview } from '@/api/stats'
import { getWrongBookSummary } from '@/api/record'
import { createSession, getCurrentSession } from '@/api/session'
import { useUserStore } from '@/stores/user'
import ImageUploader from '@/components/ImageUploader.vue'

const router = useRouter()
const userStore = useUserStore()

const user = computed(() => userStore.user)
const nickname = computed(() => (user.value ? user.value.nickname : '—'))
const isAdmin = computed(() => userStore.isAdmin)
const avatarText = computed(() => {
  const value = nickname.value
  return value && value !== '—' ? value.slice(0, 1).toUpperCase() : '码'
})

/* ---------------- v4.12 头像上传 ---------------- */
/** 头像 URL 来自 store（登录/me 时写入），上传成功后原地更新，不需要重新拉接口 */
const avatarUrl = computed(() => (user.value && user.value.avatar) || '')
const avatarSaving = ref(false)

async function handleAvatarUploaded(result) {
  if (!result || !result.url || avatarSaving.value) return
  const previousAvatar = avatarUrl.value
  avatarSaving.value = true
  try {
    // setProfile 合并而非替换，避免把 role/createdAt 等字段冲掉
    userStore.setProfile({ avatar: result.url })
    await updateAvatar(result.url)
    ElMessage.success('头像已更新')
  } catch (error) {
    // 写库失败要把界面回滚到服务端真实值，否则会显示一张其实没保存的图
    userStore.setProfile({ avatar: previousAvatar })
    if (!error || !error.__notified) {
      ElMessage.error((error && error.message) || '头像保存失败')
    }
  } finally {
    avatarSaving.value = false
  }
}

/* ---------------- 账号元信息（注册时间 / 最近登录） ---------------- */
/** 后端 /auth/me 已返回 createdAt / lastLoginAt，登录响应里也有；这里只在缺失时补拉一次 */
const profileDetail = ref(null)
const joinedText = computed(() => formatDate(profileDetail.value?.createdAt))
const lastLoginText = computed(() => formatRelative(profileDetail.value?.lastLoginAt))

function formatDate(value) {
  if (!value) return ''
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY 年 M 月 D 日') : ''
}

function formatRelative(value) {
  if (!value) return ''
  const parsed = dayjs(value)
  if (!parsed.isValid()) return ''
  const diffMinutes = dayjs().diff(parsed, 'minute')
  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes} 分钟前`
  const diffHours = dayjs().diff(parsed, 'hour')
  if (diffHours < 24) return `${diffHours} 小时前`
  const diffDays = dayjs().diff(parsed, 'day')
  if (diffDays < 30) return `${diffDays} 天前`
  return parsed.format('YYYY-MM-DD')
}

/* ---------------- 学习数据 ---------------- */
const statsLoading = ref(true)
const stats = ref(null)
const review = reactive({ dueCount: 0, overdueCount: 0, newCount: 0, estimatedMinutes: 0 })
const reviewTotal = computed(
  () => review.dueCount + review.newCount
)
const wrongSummary = reactive({ learning: 0, reviewing: 0, mastered: 0, total: 0 })
const favoriteCount = ref(0)
const reviewLoading = ref(false)

const statCards = computed(() => {
  const s = stats.value || {}
  const masteredPercent =
    typeof s.masteredRate === 'number' ? Math.round(s.masteredRate * 100) : null
  const coveragePercent =
    typeof s.coverageRate === 'number' ? Math.round(s.coverageRate * 100) : null
  return [
    {
      key: 'streak',
      label: '连续打卡',
      value: s.streak ?? 0,
      unit: ' 天',
      hint: s.streak > 0 ? '保持住' : '今天开始'
    },
    {
      key: 'week',
      label: '近 7 天自评',
      value: s.weekCount ?? 0,
      unit: ' 次',
      hint: `今日 ${s.todayCount ?? 0} 次`
    },
    {
      key: 'mastered',
      label: '掌握率',
      value: masteredPercent === null ? '—' : masteredPercent,
      unit: masteredPercent === null ? '' : '%',
      hint: `累计 ${s.totalQuestionCount ?? 0} 题`
    },
    {
      key: 'coverage',
      label: '题库覆盖',
      value: coveragePercent === null ? '—' : coveragePercent,
      unit: coveragePercent === null ? '' : '%',
      hint: `共 ${s.publishedQuestionCount ?? 0} 题`
    }
  ]
})

/** 顶部横幅里的精简数字 */
const heroStats = computed(() => [
  { key: 'streak', value: stats.value?.streak ?? 0, label: '连续天数' },
  { key: 'total', value: stats.value?.totalQuestionCount ?? 0, label: '已刷题数' },
  { key: 'wrong', value: wrongSummary.total, label: '错题本' },
  { key: 'fav', value: favoriteCount.value, label: '收藏' }
])

/* ---------------- 快捷入口 ---------------- */
/* 图标用 path d 数组表达，避免 v-html（全站仅 MarkdownView 允许 v-html，见 scripts/check.sh） */
const quickLinks = computed(() => [
  {
    key: 'wrong',
    title: '错题本',
    desc: wrongSummary.total > 0 ? `${wrongSummary.total} 道待巩固` : '暂时没有错题',
    to: '/wrong-book',
    paths: [
      'M4 5a2 2 0 0 1 2-2h9l5 5v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z',
      'M14 3v6h6',
      'M9 14l2 2 4-4'
    ],
    badge: wrongSummary.total > 0 ? String(wrongSummary.total) : ''
  },
  {
    key: 'favorites',
    title: '我的收藏',
    desc: favoriteCount.value > 0 ? `${favoriteCount.value} 道已收藏` : '还没收藏题目',
    to: '/favorites',
    paths: ['M12 3l2.9 6.1 6.6.9-4.8 4.6 1.2 6.6L12 18.1 6.1 21.2l1.2-6.6L2.5 10l6.6-.9z'],
    badge: ''
  },
  {
    key: 'records',
    title: '刷题记录',
    desc: '趋势与分类掌握',
    to: '/records',
    paths: ['M3 3v18h18', 'M7 15l4-5 3 3 5-7'],
    badge: ''
  },
  {
    key: 'paths',
    title: '刷题路线',
    desc: '按主题循序推进',
    to: '/paths',
    paths: ['M6 8.5v6A3.5 3.5 0 0 0 9.5 18H15'],
    circles: [
      [6, 6, 2.5],
      [18, 18, 2.5]
    ],
    badge: ''
  },
  {
    key: 'assistant',
    title: 'AI 助手',
    desc: '随时追问知识点',
    to: '/assistant',
    paths: [
      'M12 3l1.9 4.6L18.5 9.5 13.9 11.4 12 16l-1.9-4.6L5.5 9.5l4.6-1.9z',
      'M18 15l.9 2.1 2.1.9-2.1.9L18 21l-.9-2.1-2.1-.9 2.1-.9z'
    ],
    badge: ''
  }
])

/* ---------------- 数据加载 ---------------- */
async function loadOverview() {
  statsLoading.value = true
  const tasks = [
    getStatsOverview()
      .then((data) => {
        stats.value = data
      })
      .catch(() => {
        stats.value = null
      }),
    getReviewSummary()
      .then((data) => {
        Object.assign(review, {
          dueCount: data?.dueCount ?? 0,
          overdueCount: data?.overdueCount ?? 0,
          newCount: data?.newCount ?? 0,
          estimatedMinutes: data?.estimatedMinutes ?? 0
        })
      })
      .catch(() => {}),
    getWrongBookSummary()
      .then((data) => {
        Object.assign(wrongSummary, {
          learning: data?.learning ?? 0,
          reviewing: data?.reviewing ?? 0,
          mastered: data?.mastered ?? 0,
          total: data?.total ?? 0
        })
      })
      .catch(() => {})
  ]
  await Promise.all(tasks)
  statsLoading.value = false
}

/** 收藏数：后端没有独立计数接口，用收藏列表第一页的 total 字段 */
async function loadFavoriteCount() {
  try {
    const { getFavorites } = await import('@/api/question')
    const page = await getFavorites({ page: 1, pageSize: 1 })
    favoriteCount.value = Number(page?.total ?? 0)
  } catch {
    favoriteCount.value = 0
  }
}

/** 账号元信息：优先用 store 里已有的 user，缺字段时补拉 /auth/me */
async function loadProfileDetail() {
  const current = user.value
  if (current && current.createdAt) {
    profileDetail.value = current
    return
  }
  try {
    const data = await getMe()
    profileDetail.value = data
    if (data) userStore.setProfile(data)
  } catch {
    profileDetail.value = current || null
  }
}

onMounted(() => {
  loadProfileDetail()
  loadOverview()
  loadFavoriteCount()
})

/** 开始复习：与首页「今日计划」同一套逻辑——有进行中的会话就续做，否则新建一组 */
async function startReview() {
  if (reviewLoading.value) return
  reviewLoading.value = true
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
    const size = Math.min(reviewTotal.value, 20)
    const created = await createSession({ mode: 'today', size })
    if (!created || !created.currentQuestionId) {
      ElMessage.info('今日暂无可练习的题目')
      return
    }
    router.push({
      name: 'question-detail',
      params: { id: created.currentQuestionId },
      query: { from: 'session' }
    })
  } catch (error) {
    ElMessage.error((error && error.message) || '创建练习失败，请稍后重试')
  } finally {
    reviewLoading.value = false
  }
}

/* ---------------- 表单 ---------------- */
const nicknameFormRef = ref(null)
const passwordFormRef = ref(null)

const nicknameForm = reactive({ nickname: '', password: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const nicknameFieldError = ref('')
const nicknamePasswordError = ref('')
const nicknameFormError = ref('')
const oldPasswordError = ref('')
const newPasswordError = ref('')
const passwordFormError = ref('')

const nicknameSubmitting = ref(false)
const passwordSubmitting = ref(false)
const loggingOut = ref(false)
const exportingNotes = ref(false)

/** 导出笔记（C7）：后端返回 Blob，前端触发下载；文件名用日期，内容由后端按分类分组 */
async function handleExportNotes() {
  if (exportingNotes.value) return
  exportingNotes.value = true
  try {
    const payload = await exportNotes()
    const blob =
      payload instanceof Blob
        ? payload
        : new Blob([payload], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `notes-${new Date().toISOString().slice(0, 10)}.md`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    ElMessage.success('已导出笔记')
  } catch (error) {
    ElMessage.error((error && error.message) || '导出失败，请稍后重试')
  } finally {
    exportingNotes.value = false
  }
}

watch(
  user,
  (value) => {
    if (value && !nicknameForm.nickname) {
      nicknameForm.nickname = value.nickname
    }
  },
  { immediate: true }
)

const NICKNAME_PATTERN = /^[\u4e00-\u9fa5A-Za-z0-9_-]{2,20}$/

// 与后端 PasswordPolicy 一致：6–32 位；拒全同字符、连续递增（≥7 位）、等于昵称
function checkPassword(value, currentNickname) {
  const password = value || ''
  if (!password) return '请输入新密码'
  if (password.length < 6 || password.length > 32) return '密码为 6–32 位'
  if (/^(.)\1+$/.test(password)) return '密码太简单，请换一个'
  if (isAscendingSequence(password)) return '密码太简单，请换一个'
  if (currentNickname && password.toLowerCase() === currentNickname.toLowerCase()) {
    return '密码不能与昵称相同'
  }
  return ''
}

function isAscendingSequence(value) {
  if (value.length < 7) return false
  for (let i = 1; i < value.length; i += 1) {
    if (value.charCodeAt(i) !== value.charCodeAt(i - 1) + 1) return false
  }
  return true
}

const nicknameRules = {
  nickname: [
    { required: true, message: '请输入新昵称', trigger: 'blur' },
    {
      pattern: NICKNAME_PATTERN,
      message: '2–20 个字符，可用中文、字母、数字、下划线、连字符',
      trigger: 'blur'
    }
  ],
  password: [{ required: true, message: '请输入当前密码', trigger: 'blur' }]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value && value === passwordForm.oldPassword) {
          callback(new Error('新密码不能与旧密码相同'))
          return
        }
        const message = checkPassword(value, nickname.value)
        if (message) callback(new Error(message))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

function resetAndGoLogin() {
  userStore.reset()
  router.replace('/login')
}

async function handleNicknameSubmit() {
  if (nicknameSubmitting.value || !nicknameFormRef.value) return
  nicknameFieldError.value = ''
  nicknamePasswordError.value = ''
  nicknameFormError.value = ''
  const valid = await nicknameFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await ElMessageBox.confirm('昵称同时是登录名，修改后需要重新登录，确定修改吗？', '修改昵称', {
      type: 'warning',
      confirmButtonText: '确定修改',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  nicknameSubmitting.value = true
  try {
    await updateProfile({
      nickname: nicknameForm.nickname.trim(),
      password: nicknameForm.password
    })
    ElMessage.success('昵称已修改，请用新昵称重新登录')
    resetAndGoLogin()
  } catch (error) {
    const code = error && error.code
    const message = (error && error.message) || '修改失败，请稍后重试'
    if (code === 40900) {
      nicknameFieldError.value = '该昵称不可用，请换一个'
    } else if (code === 40000) {
      if (message.includes('密码')) nicknamePasswordError.value = message
      else if (message.includes('昵称')) nicknameFieldError.value = message
      else nicknameFormError.value = message
    } else {
      nicknameFormError.value = message
    }
  } finally {
    nicknameSubmitting.value = false
  }
}

async function handlePasswordSubmit() {
  if (passwordSubmitting.value || !passwordFormRef.value) return
  oldPasswordError.value = ''
  newPasswordError.value = ''
  passwordFormError.value = ''
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  passwordSubmitting.value = true
  try {
    await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码已修改，请用新密码重新登录')
    resetAndGoLogin()
  } catch (error) {
    const code = error && error.code
    const message = (error && error.message) || '修改失败，请稍后重试'
    if (code === 40000) {
      if (message.includes('原密码')) oldPasswordError.value = message
      else if (message.includes('密码')) newPasswordError.value = message
      else passwordFormError.value = message
    } else {
      passwordFormError.value = message
    }
  } finally {
    passwordSubmitting.value = false
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('退出后需要重新登录，确定退出吗？', '退出登录', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  loggingOut.value = true
  try {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.replace('/')
  } finally {
    loggingOut.value = false
  }
}
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  padding-bottom: var(--space-4);
}

/* ---------- 身份横幅 ---------- */
.profile-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
  padding: var(--space-6);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}

.profile-hero__identity {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  min-width: 0;
}

.profile-hero__avatar-uploader {
  flex: none;
}

.profile-hero__avatar {
  position: relative;
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  overflow: hidden;
  color: var(--color-on-primary);
  font-size: 28px;
  font-weight: 600;
  background-color: var(--color-primary);
  border-radius: 50%;
}

.profile-hero__avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 蒙层只在 hover / 键盘 focus 时出现：平时让头像本身干净地露出来 */
.profile-hero__avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: var(--font-size-aux);
  font-weight: 500;
  background-color: rgb(0 0 0 / 55%);
  opacity: 0;
  transition: opacity var(--motion-fast) var(--motion-ease);
}

.profile-hero__avatar-uploader:hover .profile-hero__avatar-mask,
.profile-hero__avatar-uploader:focus-within .profile-hero__avatar-mask,
.profile-hero__avatar.is-uploading .profile-hero__avatar-mask {
  opacity: 1;
}

.profile-hero__avatar.is-uploading {
  cursor: progress;
}

.profile-hero__meta {
  min-width: 0;
}

.profile-hero__name-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.profile-hero__nickname {
  overflow: hidden;
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-hero__sub {
  margin-top: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.profile-hero__dot {
  margin: 0 var(--space-2);
}

.profile-hero__stats {
  display: flex;
  flex: none;
  gap: var(--space-6);
}

.profile-hero__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 56px;
}

.profile-hero__stat-value {
  color: var(--color-primary);
  font-size: 24px;
  font-weight: 600;
  line-height: 32px;
  font-variant-numeric: tabular-nums;
}

.profile-hero__stat-label {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  white-space: nowrap;
}

/* ---------- 通用分区 ---------- */
.profile-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.profile-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.profile-section__title {
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
}

/* ---------- 学习数据 ---------- */
.profile-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-4);
}

.profile-stats--loading {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.profile-stats__skeleton {
  height: 112px;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  animation: profile-pulse 1.4s ease-in-out infinite;
}

@keyframes profile-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.55;
  }
}

.profile-stat {
  padding: var(--space-4) var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  transition:
    box-shadow var(--motion-base) var(--motion-ease),
    transform var(--motion-base) var(--motion-ease);
}

.profile-stat:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.profile-stat__label {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.profile-stat__value {
  margin-top: var(--space-2);
  color: var(--color-text);
  font-size: 28px;
  font-weight: 600;
  line-height: 36px;
  font-variant-numeric: tabular-nums;
}

.profile-stat__unit {
  margin-left: 2px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  font-weight: 400;
}

.profile-stat__hint {
  margin-top: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

/* 今日复习条 */
.profile-review {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  background-color: var(--color-accent-soft);
  border: 1px solid rgba(var(--color-accent-rgb), 0.24);
  border-radius: var(--radius-card);
}

.profile-review__title {
  color: var(--color-text);
  font-size: var(--font-size-body);
  font-weight: 600;
  line-height: var(--line-height-body);
}

.profile-review__desc {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.profile-review__overdue {
  color: var(--color-danger);
  font-weight: 600;
}

/* ---------- 快捷入口 ---------- */
.profile-links {
  display: grid;
  /*
   * 快捷入口固定 5 个，用 5 列排满一行，避免 auto-fill 在 1152px 容器下排成 3+2 留空位。
   * 单卡在 1200px 容器里约 221px：内边距 32 + 图标 40 + 间距 12 后剩约 137px 给文字，
   * 「查看每日趋势与分类掌握」这类 11 字描述正好一行放下。
   */
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-3);
}

.profile-link {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-3);
  text-align: left;
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  transition:
    box-shadow var(--motion-base) var(--motion-ease),
    transform var(--motion-base) var(--motion-ease),
    border-color var(--motion-base) var(--motion-ease);
}

.profile-link:hover {
  border-color: rgba(var(--color-primary-rgb), 0.4);
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.profile-link__icon {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: var(--color-primary);
  background-color: rgba(var(--color-primary-rgb), 0.1);
  border-radius: var(--radius-button);
}

.profile-link__icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.profile-link__body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 1px;
}

.profile-link__title {
  color: var(--color-text);
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  white-space: nowrap;
}

.profile-link__desc {
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-link__badge {
  flex: none;
  min-width: 20px;
  margin-left: auto;
  padding: 0 6px;
  color: var(--color-on-primary);
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  background-color: var(--color-primary);
  border-radius: var(--radius-full);
}

.profile-link__arrow {
  flex: none;
  margin-left: auto;
  color: var(--color-text-disabled);
  font-size: 14px;
}

.profile-link__badge + .profile-link__arrow {
  margin-left: var(--space-1);
}

/* ---------- 账号设置 ---------- */
.profile-panels {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.profile-panel {
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}

.profile-panel--full {
  margin-top: var(--space-4);
}

.profile-panel__title {
  font-size: var(--font-size-body);
  font-weight: 600;
  line-height: var(--line-height-body);
}

.profile-panel__desc {
  margin: var(--space-2) 0 var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.profile-panel__error {
  min-height: 22px;
  margin: 0 0 var(--space-3);
  color: var(--color-danger);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.profile-panel__submit {
  min-height: 44px;
  padding: 0 var(--space-6);
  border-radius: var(--radius-button);
}

.profile-panel :deep(.el-input__wrapper) {
  min-height: 44px;
}

.profile-panel :deep(.el-form-item) {
  margin-bottom: var(--space-4);
}

/* ---------- 退出登录 ---------- */
.profile-page__logout {
  padding-top: var(--space-5);
  border-top: 1px solid var(--color-border);
}

/* ---------- md：768–1023px ---------- */
@media (max-width: 1023px) {
  .profile-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .profile-stats--loading {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .profile-hero {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-4);
  }

  .profile-hero__stats {
    width: 100%;
    justify-content: space-between;
  }

  .profile-panels {
    grid-template-columns: minmax(0, 1fr);
  }

  /* 主内容宽 ≤976px 时 5 列只剩 185px/张，描述开始截断；退 3 列 */
  .profile-links {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

/* ---------- sm：<768px ---------- */
@media (max-width: 767px) {
  .profile-page {
    gap: var(--space-5);
  }

  .profile-hero {
    padding: var(--space-4);
  }

  .profile-hero__avatar {
    width: 56px;
    height: 56px;
    font-size: 22px;
  }

  .profile-hero__nickname {
    font-size: var(--font-size-h2);
    line-height: var(--line-height-h2);
  }

  .profile-stats,
  .profile-stats--loading {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--space-3);
  }

  .profile-stat {
    padding: var(--space-3) var(--space-4);
  }

  .profile-stat__value {
    font-size: 24px;
    line-height: 32px;
  }

  .profile-review {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
  }

  .profile-review :deep(.el-button) {
    width: 100%;
    min-height: 44px;
  }

  .profile-links {
    grid-template-columns: minmax(0, 1fr);
  }

  .profile-panel {
    padding: var(--space-4);
  }

  .profile-page__logout :deep(.el-button) {
    width: 100%;
    min-height: 44px;
  }
}
</style>
