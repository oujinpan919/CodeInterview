<template>
  <template v-for="(part, index) in parts" :key="index">
    <mark v-if="part.hit" class="highlight-text__hit">{{ part.text }}</mark>
    <template v-else>{{ part.text }}</template>
  </template>
</template>

<script setup>
import { computed } from 'vue'

/**
 * 关键词高亮（v3.6 C4 / v3.7 抽成公共组件）：纯文本切片渲染，禁止拼 HTML。
 * 卡片列表与表格列表共用同一套切词逻辑，避免两份实现分叉。
 */
const props = defineProps({
  text: { type: String, default: '' },
  keyword: { type: String, default: '' }
})

const parts = computed(() => {
  const text = props.text || ''
  const keyword = (props.keyword || '').trim()
  if (!keyword || !text) {
    return [{ hit: false, text }]
  }
  const lowerText = text.toLowerCase()
  const lowerKeyword = keyword.toLowerCase()
  const result = []
  let cursor = 0
  while (cursor < text.length) {
    const at = lowerText.indexOf(lowerKeyword, cursor)
    if (at < 0) {
      result.push({ hit: false, text: text.slice(cursor) })
      break
    }
    if (at > cursor) {
      result.push({ hit: false, text: text.slice(cursor, at) })
    }
    result.push({ hit: true, text: text.slice(at, at + keyword.length) })
    cursor = at + keyword.length
  }
  return result.length > 0 ? result : [{ hit: false, text }]
})
</script>

<style scoped>
.highlight-text__hit {
  padding: 0 2px;
  color: inherit;
  background-color: #fef08a;
  border-radius: 2px;
}
</style>
