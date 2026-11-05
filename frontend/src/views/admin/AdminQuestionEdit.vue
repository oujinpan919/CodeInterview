<template>
  <section class="admin-question-edit">
    <el-breadcrumb separator="/" class="admin-question-edit__breadcrumb">
      <el-breadcrumb-item>管理端</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ name: 'admin-questions' }">题目管理</el-breadcrumb-item>
      <el-breadcrumb-item>{{ isEdit ? '编辑题目' : '新建题目' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <header class="admin-question-edit__head">
      <el-button class="admin-question-edit__back" link type="primary" @click="goBack">
        ← 返回
      </el-button>
      <h1 class="admin-question-edit__title">{{ isEdit ? '编辑题目' : '新建题目' }}</h1>
    </header>

    <div v-if="loading" class="admin-question-edit__skeleton" aria-busy="true">
      <el-skeleton :rows="8" animated />
    </div>

    <ErrorState v-else-if="loadError" :message="loadError" @retry="loadPage" />

    <QuestionForm
      v-else
      v-model="model"
      :categories="categories"
      :tags="tags"
      :submitting="saving"
      @submit="handleSubmit"
      @cancel="goBack"
    />
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import {
  createAdminQuestion,
  getAdminQuestion,
  publishAdminQuestion,
  updateAdminQuestion
} from '@/api/admin'
import { getCategories, getTags } from '@/api/taxonomy'
import QuestionForm from '@/components/admin/QuestionForm.vue'
import ErrorState from '@/components/ErrorState.vue'

const route = useRoute()
const router = useRouter()

const questionId = ref(route.query.id || '')
const isEdit = computed(() => Boolean(questionId.value))
const loading = ref(true)
const loadError = ref('')
const saving = ref(false)
const categories = ref([])
const tags = ref([])

const model = ref(emptyModel())
// 快照必须是响应式的，否则保存后 dirty 计算属性仍读缓存值，路由守卫会误拦
const snapshot = ref('')

function emptyModel() {
  return {
    title: '',
    contentMd: '',
    answerMd: '',
    categoryId: null,
    difficulty: 2,
    tagIds: [],
    source: ''
  }
}

const dirty = computed(() => snapshot.value !== '' && JSON.stringify(model.value) !== snapshot.value)

function markClean() {
  snapshot.value = JSON.stringify(model.value)
}

async function loadPage() {
  loading.value = true
  loadError.value = ''
  try {
    const [categoryList, tagList] = await Promise.all([getCategories(), getTags()])
    categories.value = Array.isArray(categoryList) ? categoryList : []
    tags.value = Array.isArray(tagList) ? tagList : []
    if (isEdit.value) {
      const detail = await getAdminQuestion(questionId.value)
      model.value = {
        title: detail.title || '',
        contentMd: detail.contentMd || '',
        answerMd: detail.answerMd || '',
        categoryId: detail.categoryId,
        difficulty: detail.difficulty || 2,
        tagIds: Array.isArray(detail.tagIds) ? detail.tagIds : [],
        source: detail.source || ''
      }
    } else {
      model.value = emptyModel()
    }
    markClean()
  } catch (error) {
    loadError.value = (error && error.message) || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function handleSubmit(payload, publish) {
  if (saving.value) return
  saving.value = true
  try {
    let id = questionId.value
    if (isEdit.value) {
      await updateAdminQuestion(id, payload)
    } else {
      const created = await createAdminQuestion(payload)
      id = created.id
      questionId.value = id
    }
    if (publish) {
      await publishAdminQuestion(id)
      ElMessage.success('已保存并发布，前台立即可见')
    } else {
      ElMessage.success('已保存草稿')
    }
    markClean()
    router.push({ name: 'admin-questions' })
  } catch (error) {
    ElMessage.error((error && error.message) || '保存失败，请重试')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push({ name: 'admin-questions' })
}

function handleBeforeUnload(event) {
  if (!dirty.value) return
  event.preventDefault()
  event.returnValue = ''
}

onBeforeRouteLeave(async () => {
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('当前修改尚未保存，确定离开吗？', '未保存的修改', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑'
    })
    return true
  } catch {
    return false
  }
})

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  loadPage()
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped>
.admin-question-edit__breadcrumb {
  margin-bottom: var(--space-4);
}

.admin-question-edit__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.admin-question-edit__back {
  min-height: 44px;
  font-size: var(--font-size-body);
}

.admin-question-edit__title {
  color: var(--color-text);
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
  font-weight: 600;
}

.admin-question-edit__skeleton {
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

@media (max-width: 767px) {
  .admin-question-edit__title {
    font-size: var(--font-size-h2);
  }
}
</style>
