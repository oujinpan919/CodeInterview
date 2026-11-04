<template>
  <section class="profile-page">
    <h1 class="profile-page__title">个人中心</h1>

    <div class="profile-page__grid">
      <aside class="profile-card">
        <div class="profile-card__avatar" aria-hidden="true">{{ avatarText }}</div>
        <h2 class="profile-card__nickname">{{ nickname }}</h2>
        <el-tag :type="isAdmin ? 'warning' : 'info'" effect="plain" size="small">
          {{ isAdmin ? '管理员' : '用户' }}
        </el-tag>
        <dl class="profile-card__meta">
          <div class="profile-card__meta-row">
            <dt>注册时间</dt>
            <dd>—</dd>
          </div>
        </dl>
        <p class="profile-card__tip">昵称就是登录名</p>
      </aside>

      <div class="profile-panels">
        <section class="profile-panel">
          <h2 class="profile-panel__title">修改昵称</h2>
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

        <section class="profile-panel">
          <h2 class="profile-panel__title">修改密码</h2>
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
    </div>

    <div class="profile-page__logout">
      <el-button type="danger" plain :loading="loggingOut" @click="handleLogout">
        退出登录
      </el-button>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message-box.css'
import { updatePassword, updateProfile } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const user = computed(() => userStore.user)
const nickname = computed(() => (user.value ? user.value.nickname : '—'))
const isAdmin = computed(() => userStore.isAdmin)
const avatarText = computed(() => {
  const value = nickname.value
  return value && value !== '—' ? value.slice(0, 1).toUpperCase() : '码'
})

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
  padding-bottom: var(--space-4);
}

.profile-page__title {
  margin-bottom: var(--space-6);
}

.profile-page__grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: var(--space-6);
  align-items: start;
}

.profile-card,
.profile-panel {
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: 0 4px 16px rgba(31, 41, 55, 0.04);
}

.profile-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-6);
  text-align: center;
}

.profile-card__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  color: var(--color-bg-card);
  font-size: 24px;
  font-weight: 600;
  background-color: var(--color-primary);
  border-radius: 50%;
}

.profile-card__nickname {
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
  word-break: break-all;
}

.profile-card__meta {
  width: 100%;
  margin: var(--space-2) 0 0;
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.profile-card__meta-row {
  display: flex;
  justify-content: space-between;
  margin: 0;
  padding: var(--space-1) 0;
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.profile-card__meta-row dt {
  color: var(--color-text-secondary);
}

.profile-card__meta-row dd {
  margin: 0;
  color: var(--color-text);
}

.profile-card__tip {
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.profile-panels {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.profile-panel {
  padding: var(--space-5) var(--space-6) var(--space-6);
}

.profile-panel__title {
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
}

.profile-panel__desc {
  margin: var(--space-2) 0 var(--space-5);
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

.profile-page__logout {
  margin-top: var(--space-8);
  padding-top: var(--space-5);
  border-top: 1px solid var(--color-border);
}

@media (max-width: 767px) {
  .profile-page__title {
    margin-bottom: var(--space-4);
  }

  .profile-page__grid {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-4);
  }

  .profile-panel {
    padding: var(--space-4);
  }

  .profile-page__logout {
    margin-top: var(--space-6);
  }

  .profile-page__logout :deep(.el-button) {
    width: 100%;
    min-height: 44px;
  }
}
</style>
