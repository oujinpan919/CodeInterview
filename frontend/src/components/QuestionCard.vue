<template>
  <article
    class="question-card"
    :class="{
      'question-card--offline': isOffline,
      'question-card--clickable': isClickable
    }"
    :role="isClickable ? 'button' : undefined"
    :tabindex="isClickable ? 0 : undefined"
    @click="handleClick"
    @keydown.enter.prevent="handleClick"
  >
    <div class="question-card__head">
      <!-- v4.1：未访问→主色蓝，已访问→绿色（与题库表格列表同一套语义） -->
      <h3
        class="question-card__title"
        :class="{
          'question-card__title--unvisited': question.visited === false,
          'question-card__title--visited': question.visited === true
        }"
      >
        <HighlightText :text="question.title" :keyword="keyword" />
      </h3>
      <el-tag v-if="isOffline" type="info" size="small" effect="plain" class="question-card__offline">
        已下架
      </el-tag>
    </div>

    <div class="question-card__meta">
      <DifficultyTag :value="question.difficulty" />
      <span v-if="question.categoryName" class="question-card__meta-item">
        {{ question.categoryName }}
      </span>
      <span v-if="hasViewCount" class="question-card__meta-item">浏览 {{ question.viewCount }}</span>
      <span v-if="hasPracticeCount" class="question-card__meta-item">
        已练 {{ question.practiceCount }} 次
      </span>
    </div>

    <div v-if="tags.length > 0" class="question-card__tags">
      <el-tag
        v-for="tag in tags"
        :key="tag.id"
        size="small"
        effect="plain"
        type="info"
        class="question-card__tag"
      >
        {{ tag.name }}
      </el-tag>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import DifficultyTag from '@/components/DifficultyTag.vue'
import HighlightText from '@/components/HighlightText.vue'

const props = defineProps({
  question: { type: Object, required: true },
  clickable: { type: Boolean, default: true },
  /** v3.6 C4：搜索关键词，命中片段用 <mark> 高亮（不拼 HTML，按纯文本切片渲染） */
  keyword: { type: String, default: '' }
})

const emit = defineEmits(['click'])

// 04 B2 / A1：status=2 已下架 → 置灰、显示「已下架」、不可点击
const isOffline = computed(() => props.question.status === 2)
const isClickable = computed(() => props.clickable && !isOffline.value)



const tags = computed(() => (Array.isArray(props.question.tags) ? props.question.tags : []))
const hasViewCount = computed(() => typeof props.question.viewCount === 'number')
const hasPracticeCount = computed(() => typeof props.question.practiceCount === 'number')

function handleClick() {
  if (!isClickable.value) return
  emit('click', props.question)
}
</script>

<style scoped>
.question-card {
  padding: var(--space-5);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  transition:
    border-color var(--motion-fast) var(--motion-ease),
    box-shadow var(--motion-base) var(--motion-ease),
    transform var(--motion-base) var(--motion-ease);
}

.question-card--clickable {
  cursor: pointer;
}

.question-card--clickable:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.question-card--clickable:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.question-card--offline {
  background-color: var(--color-bg-page);
  opacity: 0.7;
}

.question-card__head {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}

.question-card__title {
  flex: 1;
  color: var(--color-text);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  font-weight: 600;
  overflow-wrap: anywhere;
}

/* v4.1：与 QuestionList 的表格列表保持同一套已读配色 */
.question-card__title--unvisited {
  color: var(--color-primary);
}

.question-card__title--visited {
  /* --color-success 对白底仅 3.09:1，正文标题用深一档的 --color-success-text */
  color: var(--color-success-text);
}

.question-card__offline {
  flex-shrink: 0;
}

.question-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-3);
  margin-top: var(--space-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-aux);
  line-height: var(--line-height-aux);
}

.question-card__meta-item {
  white-space: nowrap;
}

.question-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.question-card__tag {
  border-radius: var(--radius-tag);
}

@media (max-width: 767px) {
  .question-card {
    padding: var(--space-4);
  }
}
</style>