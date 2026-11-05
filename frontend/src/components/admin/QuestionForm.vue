<template>
  <div class="question-form">
    <div class="question-form__columns" :class="{ 'question-form__columns--mobile': isMobile }">
      <section v-show="!isMobile || mobileTab === 'edit'" class="question-form__pane">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="question-form__form"
        >
          <el-form-item label="标题" prop="title">
            <el-input
              :model-value="form.title"
              placeholder="1–120 字"
              maxlength="120"
              show-word-limit
              @update:model-value="patch('title', $event)"
            />
          </el-form-item>

          <div class="question-form__row">
            <el-form-item label="分类" prop="categoryId" class="question-form__col">
              <el-select
                :model-value="form.categoryId"
                placeholder="请选择分类"
                class="question-form__select"
                @update:model-value="patch('categoryId', $event)"
              >
                <el-option
                  v-for="category in categories"
                  :key="category.id"
                  :label="category.name"
                  :value="category.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="难度" prop="difficulty" class="question-form__col">
              <el-radio-group
                :model-value="form.difficulty"
                @update:model-value="patch('difficulty', $event)"
              >
                <el-radio :value="1">简单</el-radio>
                <el-radio :value="2">中等</el-radio>
                <el-radio :value="3">困难</el-radio>
              </el-radio-group>
            </el-form-item>
          </div>

          <el-form-item label="标签" prop="tagIds">
            <el-select
              :model-value="form.tagIds"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="最多 10 个标签"
              class="question-form__select"
              @update:model-value="patch('tagIds', $event)"
            >
              <el-option
                v-for="tag in tags"
                :key="tag.id"
                :label="tag.name"
                :value="tag.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="来源" prop="source">
            <el-input
              :model-value="form.source"
              placeholder="仅后台可见，≤50 字"
              maxlength="50"
              show-word-limit
              @update:model-value="patch('source', $event)"
            />
          </el-form-item>

          <el-form-item label="题干（支持 Markdown）" prop="contentMd">
            <el-input
              :model-value="form.contentMd"
              type="textarea"
              :autosize="{ minRows: 18, maxRows: 30 }"
              maxlength="20000"
              show-word-limit
              placeholder="题干 Markdown，可含代码块"
              @update:model-value="patch('contentMd', $event)"
            />
          </el-form-item>

          <el-form-item label="参考答案" prop="answerMd">
            <el-input
              :model-value="form.answerMd"
              type="textarea"
              :autosize="{ minRows: 18, maxRows: 30 }"
              maxlength="20000"
              show-word-limit
              placeholder="参考答案 Markdown，必填"
              @update:model-value="patch('answerMd', $event)"
            />
          </el-form-item>
        </el-form>
      </section>

      <section v-show="!isMobile || mobileTab === 'preview'" class="question-form__pane">
        <div class="question-form__preview">
          <el-tabs v-model="previewTab">
            <el-tab-pane label="题干预览" name="content">
              <MarkdownView :content="previewContent" empty-text="题干为空" />
            </el-tab-pane>
            <el-tab-pane label="答案预览" name="answer">
              <MarkdownView :content="previewAnswer" empty-text="参考答案为空" />
            </el-tab-pane>
          </el-tabs>
        </div>
      </section>
    </div>

    <div class="question-form__bar">
      <el-button class="question-form__bar-button" @click="emit('cancel')">取消</el-button>
      <el-button
        class="question-form__bar-button"
        :loading="submitting"
        @click="handleSave(false)"
      >
        保存草稿
      </el-button>
      <el-button
        class="question-form__bar-button"
        type="primary"
        :loading="submitting"
        @click="handleSave(true)"
      >
        保存并发布
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useMediaQuery } from '@vueuse/core'
import { useDebounceFn } from '@vueuse/core'
import MarkdownView from '@/components/MarkdownView.vue'

const props = defineProps({
  modelValue: { type: Object, required: true },
  categories: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] },
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'submit', 'cancel'])

const isMobile = useMediaQuery('(max-width: 767px)')
const mobileTab = ref('edit')
const previewTab = ref('content')
const formRef = ref(null)

const form = computed(() => props.modelValue)

const previewContent = ref('')
const previewAnswer = ref('')

// 预览 500ms 防抖（04 B4.11）
const refreshPreview = useDebounceFn(() => {
  previewContent.value = form.value.contentMd || ''
  previewAnswer.value = form.value.answerMd || ''
}, 500)

watch(
  () => [form.value.contentMd, form.value.answerMd],
  () => {
    refreshPreview()
  },
  { immediate: true }
)

const rules = {
  title: [
    { required: true, message: '标题不能为空', trigger: 'blur' },
    { max: 120, message: '标题不能超过 120 字', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  contentMd: [
    { required: true, message: '题干不能为空', trigger: 'blur' },
    { max: 20000, message: '题干不能超过 20000 字符', trigger: 'blur' }
  ],
  answerMd: [
    { required: true, message: '参考答案不能为空', trigger: 'blur' },
    { max: 20000, message: '参考答案不能超过 20000 字符', trigger: 'blur' }
  ],
  tagIds: [
    {
      validator: (rule, value, callback) => {
        if (Array.isArray(value) && value.length > 10) callback(new Error('最多选择 10 个标签'))
        else callback()
      },
      trigger: 'change'
    }
  ],
  source: [{ max: 50, message: '来源不能超过 50 字', trigger: 'blur' }]
}

function patch(key, value) {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}

function toPayload() {
  return {
    title: (form.value.title || '').trim(),
    contentMd: form.value.contentMd || '',
    answerMd: form.value.answerMd || '',
    categoryId: form.value.categoryId,
    difficulty: form.value.difficulty,
    tagIds: Array.isArray(form.value.tagIds) ? form.value.tagIds : [],
    source: form.value.source || ''
  }
}

async function handleSave(publish) {
  if (props.submitting) return
  try {
    await formRef.value.validate()
  } catch {
    // 校验失败：定位到第一个错误字段（04 B4.11）
    const firstError = document.querySelector('.el-form-item__error')
    if (firstError) firstError.scrollIntoView({ block: 'center', behavior: 'smooth' })
    return
  }
  emit('submit', toPayload(), publish)
}
</script>

<style scoped>
.question-form {
  padding-bottom: 72px;
}

.question-form__columns {
  display: grid;
  grid-template-columns: minmax(0, 55fr) minmax(0, 45fr);
  gap: var(--space-6);
  align-items: start;
}

.question-form__pane {
  min-width: 0;
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.question-form__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--space-4);
}

.question-form__col {
  min-width: 0;
}

.question-form__select {
  width: 100%;
}

.question-form__preview {
  position: sticky;
  top: calc(var(--header-height) + var(--space-4));
  min-height: 320px;
}

.question-form__bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  display: flex;
  justify-content: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background-color: var(--color-bg-card);
  border-top: 1px solid var(--color-border);
}

.question-form__bar-button {
  min-height: 44px;
  min-width: 120px;
  margin-left: 0;
  border-radius: var(--radius-button);
}

@media (max-width: 1023px) {
  .question-form__columns {
    grid-template-columns: minmax(0, 1fr);
  }

  .question-form__preview {
    position: static;
  }
}

@media (max-width: 767px) {
  .question-form__row {
    grid-template-columns: minmax(0, 1fr);
    gap: 0;
  }

  .question-form__pane {
    padding: var(--space-4);
  }

  .question-form__bar {
    padding: var(--space-2) var(--space-3);
    padding-bottom: calc(var(--space-2) + env(safe-area-inset-bottom));
  }

  .question-form__bar-button {
    flex: 1 1 0;
    min-width: 0;
    padding: 0 var(--space-2);
  }

  .question-form__preview :deep(.el-textarea__inner) {
    height: 40vh;
  }
}
</style>
