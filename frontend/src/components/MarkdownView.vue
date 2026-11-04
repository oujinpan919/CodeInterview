<template>
  <div class="markdown-view">
    <!-- 解析期间只显示纯文本，不渲染任何未清洗 HTML -->
    <pre v-if="parsing" class="markdown-view__pending">{{ content }}</pre>
    <pre v-else-if="!html" class="markdown-view__empty">{{ emptyText }}</pre>
    <!-- eslint-disable-next-line vue/no-v-html -->
    <div v-else class="markdown-view__body markdown-body" v-html="html"></div>
  </div>
</template>

<script setup>
import { ref, shallowRef, watch } from 'vue'
import { renderMarkdown, renderPlainText } from '@/composables/useMarkdown'
import '@/assets/styles/markdown.css'

const props = defineProps({
  content: { type: String, required: true },
  emptyText: { type: String, default: '暂无内容' }
})

const html = shallowRef('')
const parsing = ref(false)

let renderToken = 0

async function update(value) {
  const token = ++renderToken
  const source = typeof value === 'string' ? value : ''
  if (!source.trim()) {
    html.value = ''
    parsing.value = false
    return
  }
  parsing.value = true
  html.value = ''
  try {
    const rendered = await renderMarkdown(source)
    if (token === renderToken) html.value = rendered
  } catch {
    if (token === renderToken) html.value = renderPlainText(source)
  } finally {
    if (token === renderToken) parsing.value = false
  }
}

watch(() => props.content, update, { immediate: true })
</script>

<style scoped>
.markdown-view {
  min-width: 0;
}

.markdown-view__pending {
  margin: 0;
  color: var(--color-text-secondary);
  font-family: var(--font-family-base);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  white-space: pre-wrap;
  word-break: break-word;
}

.markdown-view__empty {
  margin: 0;
  color: var(--color-text-secondary);
  font-family: var(--font-family-base);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.markdown-view__body {
  min-width: 0;
}
</style>
