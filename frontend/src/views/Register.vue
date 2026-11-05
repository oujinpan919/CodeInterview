<template>
  <section class="auth-page">
    <div class="auth-card">
      <h1 class="auth-card__title">注册码上记</h1>
      <p class="auth-card__subtitle">昵称就是你的登录名，注册后请记住</p>

      <p v-if="registerClosed" class="auth-card__closed" role="alert">
        注册已关闭，请联系管理员
      </p>

      <el-form
        v-else
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item
          label="昵称"
          prop="nickname"
          :error="nicknameError"
        >
          <el-input
            ref="nicknameInputRef"
            v-model="form.nickname"
            placeholder="2–20 个字符"
            maxlength="20"
            autocomplete="username"
            @input="nicknameError = ''"
          />
          <p class="auth-card__hint">
            2–20 个字符，可用中文、字母、数字、下划线、连字符
          </p>
          <p class="auth-card__hint auth-card__hint--strong">
            昵称就是你的登录名，请记住
          </p>
        </el-form-item>

        <el-form-item
          label="密码"
          prop="password"
          :error="passwordError"
        >
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="6–32 位"
            maxlength="32"
            autocomplete="new-password"
            @input="passwordError = ''"
          />
          <p class="auth-card__hint">6–32 位，不能与昵称相同</p>
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入密码"
            maxlength="32"
            autocomplete="new-password"
          />
        </el-form-item>

        <p class="auth-card__error" role="alert">{{ formError }}</p>

        <el-button
          class="auth-card__submit"
          type="primary"
          native-type="submit"
          :loading="submitting"
        >
          注册
        </el-button>
      </el-form>

      <p class="auth-card__footer">
        已有账号？
        <RouterLink to="/login" class="auth-card__link">去登录</RouterLink>
      </p>
    </div>
  </section>
</template>

<script setup>
import { nextTick, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { register } from '@/api/auth'

const router = useRouter()

const formRef = ref(null)
const nicknameInputRef = ref(null)

const form = reactive({
  nickname: '',
  password: '',
  confirmPassword: ''
})

const nicknameError = ref('')
const passwordError = ref('')
const formError = ref('')
const submitting = ref(false)
const registerClosed = ref(false)

const NICKNAME_PATTERN = /^[\u4e00-\u9fa5A-Za-z0-9_-]{2,20}$/

// 与后端 PasswordPolicy 保持一致：6–32 位；拒绝全同字符、连续递增（≥7 位）、等于昵称。
// 后端为让 123456 通过，把「顺序递增」阈值定为 7（见 PasswordPolicy 注释与 02 §10）。
function checkPassword(value) {
  const password = value || ''
  if (!password) return '请输入密码'
  if (password.length < 6 || password.length > 32) return '密码为 6–32 位'
  if (/^(.)\1+$/.test(password)) return '密码太简单，请换一个'
  if (isAscendingSequence(password)) return '密码太简单，请换一个'
  if (
    form.nickname &&
    password.toLowerCase() === form.nickname.trim().toLowerCase()
  ) {
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

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    {
      pattern: NICKNAME_PATTERN,
      message: '2–20 个字符，可用中文、字母、数字、下划线、连字符',
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const message = checkPassword(value)
        if (message) callback(new Error(message))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

function handleError(error) {
  const code = error && error.code
  if (code === 40300) {
    registerClosed.value = true
    return
  }
  if (code === 42900) {
    formError.value = '操作太频繁，请稍后再试'
    return
  }
  if (code === 40900) {
    nicknameError.value = '该昵称不可用，请换一个'
    nextTick(() => {
      if (nicknameInputRef.value) nicknameInputRef.value.focus()
    })
    return
  }
  if (code === 40000) {
    const message = (error && error.message) || '注册信息不完整，请检查'
    if (message.includes('昵称')) nicknameError.value = message
    else if (message.includes('密码')) passwordError.value = message
    else formError.value = message
    return
  }
  formError.value = (error && error.message) || '注册失败，请稍后重试'
}

async function handleSubmit() {
  if (submitting.value || registerClosed.value) return
  if (!formRef.value) return
  formError.value = ''
  nicknameError.value = ''
  passwordError.value = ''
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await register({
      nickname: form.nickname.trim(),
      password: form.password
    })
    router.replace('/login?registered=1')
  } catch (error) {
    handleError(error)
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

.auth-card__closed {
  padding: var(--space-4);
  color: var(--color-danger);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  text-align: center;
  background-color: rgba(var(--color-danger-rgb), 0.06);
  border-radius: var(--radius-button);
}

.auth-card__hint {
  width: 100%;
  margin-top: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.auth-card__hint--strong {
  color: var(--color-text);
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
