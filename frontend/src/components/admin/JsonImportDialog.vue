<template>
  <el-dialog
    :model-value="modelValue"
    title="批量导入题目"
    :width="isMobile ? '92%' : '720px'"
    class="json-import"
    :close-on-click-modal="false"
    @update:model-value="close"
  >
    <template v-if="!result">
      <p class="json-import__hint">
        粘贴 JSON（格式见 03 §7）：支持顶层 category/tags 与 questions[]，单次 ≤50 题、≤1MB；默认导入为草稿。
      </p>
      <el-input
        v-model="jsonText"
        type="textarea"
        :autosize="{ minRows: 14, maxRows: 22 }"
        placeholder="粘贴 JSON 原文"
        class="json-import__textarea"
      />
      <p v-if="errorText" class="json-import__error" role="alert">{{ errorText }}</p>
    </template>

    <template v-else>
      <div class="json-import__result">
        <p class="json-import__summary">
          创建 <strong>{{ result.created }}</strong> 条 · 跳过
          <strong>{{ result.skipped }}</strong> 条 · 失败
          <strong>{{ result.failed }}</strong> 条
        </p>
        <ul v-if="result.errors && result.errors.length > 0" class="json-import__errors">
          <li v-for="item in result.errors.slice(0, 5)" :key="`${item.index}-${item.reason}`">
            第 {{ item.index + 1 }} 条（index={{ item.index }}）：{{ item.reason }}
          </li>
        </ul>
        <p v-if="result.errors && result.errors.length > 5" class="json-import__more">
          仅显示前 5 条错误，共 {{ result.errors.length }} 条
        </p>
      </div>
    </template>

    <template #footer>
      <div class="json-import__footer">
        <template v-if="!result">
          <el-button class="json-import__button" @click="close(false)">取消</el-button>
          <el-button
            class="json-import__button"
            type="primary"
            :loading="submitting"
            @click="submit"
          >
            开始导入
          </el-button>
        </template>
        <el-button v-else class="json-import__button" type="primary" @click="finish">完成</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useMediaQuery } from '@vueuse/core'
import { ElMessage } from 'element-plus'
import 'element-plus/theme-chalk/el-message.css'
import { importAdminQuestions } from '@/api/admin'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'success'])

const isMobile = useMediaQuery('(max-width: 767px)')

const jsonText = ref('')
const errorText = ref('')
const submitting = ref(false)
const result = ref(null)

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      jsonText.value = ''
      errorText.value = ''
      result.value = null
    }
  }
)

/** 把 JSON.parse 的 position 转成行列，便于定位（04 B4.10 前端校验）。 */
function locateError(text, message) {
  const match = /position (\d+)/.exec(message)
  if (!match) return message
  // 一元加号：JSON.parse 的错误位置是普通数值（不是 id，id 始终按字符串处理）
  const position = +match[1]
  const before = text.slice(0, position)
  const line = before.split('\n').length
  const column = position - before.lastIndexOf('\n')
  return `JSON 语法错误：第 ${line} 行第 ${column} 列`
}

function validateJson() {
  if (!jsonText.value.trim()) return '请粘贴 JSON 内容'
  if (jsonText.value.length > 1_000_000) return '单次导入内容不能超过 1MB'
  let parsed
  try {
    parsed = JSON.parse(jsonText.value)
  } catch (error) {
    return locateError(jsonText.value, (error && error.message) || 'JSON 语法错误')
  }
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return 'JSON 顶层必须是对象'
  const questions = parsed.questions
  if (!Array.isArray(questions) || questions.length === 0) return 'questions 必须是非空数组'
  if (questions.length > 50) return '单次最多导入 50 题'
  return ''
}

async function submit() {
  if (submitting.value) return
  const message = validateJson()
  if (message) {
    errorText.value = message
    return
  }
  errorText.value = ''
  submitting.value = true
  try {
    const data = await importAdminQuestions(jsonText.value)
    result.value = data
  } catch (error) {
    ElMessage.error((error && error.message) || '导入失败，请重试')
  } finally {
    submitting.value = false
  }
}

function close(value = false) {
  emit('update:modelValue', Boolean(value))
}

function finish() {
  emit('success', result.value)
  close(false)
}
</script>

<style scoped>
.json-import__hint {
  margin-bottom: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.json-import__textarea :deep(.el-textarea__inner) {
  font-family: var(--font-family-mono);
  font-size: 13px;
  line-height: 20px;
}

.json-import__error {
  margin-top: var(--space-2);
  color: var(--color-danger);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.json-import__summary {
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.json-import__errors {
  margin-top: var(--space-3);
  color: var(--color-danger);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.json-import__errors li + li {
  margin-top: var(--space-1);
}

.json-import__more {
  margin-top: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.json-import__footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.json-import__button {
  min-height: 44px;
  min-width: 96px;
  margin-left: 0;
  border-radius: var(--radius-button);
}

@media (max-width: 767px) {
  .json-import__footer {
    flex-direction: column;
  }

  .json-import__button {
    width: 100%;
  }
}
</style>
