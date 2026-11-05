<template>
  <section class="auth-page">
    <div class="auth-card">
      <h1 class="auth-card__title">登录码上记</h1>
      <p class="auth-card__subtitle">面试题库 · 三档自评 · 错题复习</p>

      <p v-if="registered" class="auth-card__notice" role="status">注册成功，请登录</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="昵称" prop="nickname">
          <el-input
            ref="nicknameInputRef"
            v-model="form.nickname"
            placeholder="请输入昵称"
            maxlength="20"
            autocomplete="username"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            maxlength="32"
            autocomplete="current-password"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <p class="auth-card__error" role="alert">{{ formError }}</p>

        <el-button
          class="auth-card__submit"
          type="primary"
          native-type="submit"
          :loading="submitting"
          :disabled="lockSeconds > 0"
        >
          {{ submitText }}
        </el-button>
      </el-form>

      <p class="auth-card__footer">
        还没有账号？
        <RouterLink to="/register" class="auth-card__link">去注册</RouterLink>
      </p>
      <p class="auth-card__tip">会话仅在当前标签页有效，关闭标签页后需重新登录。</p>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const nicknameInputRef = ref(null)

const form = reactive({
  nickname: '',
  password: ''
})

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 20, message: '昵称不能超过 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { max: 32, message: '密码不能超过 32 位', trigger: 'blur' }
  ]
}

const submitting = ref(false)
const formError = ref('')
const lockSeconds = ref(0)
let lockTimer = null

const registered = computed(() => route.query.registered === '1')

const submitText = computed(() =>
  lockSeconds.value > 0 ? `${lockSeconds.value} 秒后可重试` : '登录'
)

// 只接受站内路径，避免 redirect 被用作开放跳转
const redirectTarget = computed(() => {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && /^\/(?!\/)/.test(redirect) ? redirect : '/'
})

onMounted(() => {
  if (nicknameInputRef.value) nicknameInputRef.value.focus()
})

onUnmounted(() => {
  if (lockTimer) clearInterval(lockTimer)
})

function startLock() {
  lockSeconds.value = 60
  if (lockTimer) clearInterval(lockTimer)
  lockTimer = setInterval(() => {
    lockSeconds.value -= 1
    if (lockSeconds.value <= 0) {
      clearInterval(lockTimer)
      lockTimer = null
      lockSeconds.value = 0
    }
  }, 1000)
}

function messageOf(error) {
  const code = error && error.code
  if (code === 40101) return '昵称或密码错误'
  if (code === 40102) return '账号已被禁用，请联系管理员'
  if (code === 42903) {
    startLock()
    return '尝试次数过多，请 10 分钟后再试'
  }
  if (code === 40300) return '没有权限执行该操作'
  return (error && error.message) || '登录失败，请稍后重试'
}

async function handleSubmit() {
  if (submitting.value || lockSeconds.value > 0) return
  if (!formRef.value) return
  formError.value = ''
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await userStore.login(form.nickname.trim(), form.password)
    router.replace(redirectTarget.value)
  } catch (error) {
    formError.value = messageOf(error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
}

.auth-card {
  width: 100%;
  max-width: 400px;
  padding: var(--space-8);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: 0 12px 32px rgba(var(--color-text-rgb), 0.08);
}

.auth-card__title {
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
  text-align: center;
}

.auth-card__subtitle {
  margin: var(--space-2) 0 var(--space-6);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  text-align: center;
}

.auth-card__notice {
  margin-bottom: var(--space-4);
  padding: var(--space-2) var(--space-3);
  color: var(--color-success);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  background-color: rgba(var(--color-success-rgb), 0.08);
  border-radius: var(--radius-tag);
}

.auth-card__error {
  min-height: 22px;
  margin: 0 0 var(--space-3);
  color: var(--color-danger);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.auth-card__submit {
  width: 100%;
  min-height: 44px;
  border-radius: var(--radius-button);
}

.auth-card__footer {
  margin-top: var(--space-5);
  color: var(--color-text-secondary);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  text-align: center;
}

.auth-card__link {
  color: var(--color-primary);
}

.auth-card__link:hover {
  color: var(--color-primary-hover);
  text-decoration: underline;
}

.auth-card__tip {
  margin-top: var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
  text-align: center;
}

.auth-card :deep(.el-input__wrapper) {
  min-height: 44px;
}

@media (max-width: 767px) {
  .auth-card {
    max-width: none;
    padding: var(--space-5) var(--space-4);
    border: 0;
    border-radius: var(--radius-card);
    box-shadow: none;
  }
}
</style>
