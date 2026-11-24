<template>
  <div class="markdown-view">
    <!-- 解析期间只显示纯文本，不渲染任何未清洗 HTML -->
    <pre v-if="parsing" class="markdown-view__pending">{{ content }}</pre>
    <pre v-else-if="!html" class="markdown-view__empty">{{ emptyText }}</pre>
    <!-- eslint-disable-next-line vue/no-v-html -->
    <div v-else ref="bodyRef" class="markdown-view__body markdown-body" v-html="html"></div>
  </div>
</template>

<script setup>
import { nextTick, ref, shallowRef, watch } from 'vue'
import { renderMarkdown, renderPlainText } from '@/composables/useMarkdown'
import '@/assets/styles/markdown.css'

const props = defineProps({
  content: { type: String, required: true },
  emptyText: { type: String, default: '暂无内容' }
})

const html = shallowRef('')
const parsing = ref(false)
const bodyRef = ref(null)

let renderToken = 0

/**
 * v4.13：代码块语言标签 + 复制按钮。
 *
 * 为什么用 DOM 后处理而不是让 marked/DOMPurify 直接产出按钮：
 * 全站只允许 MarkdownView 里出现一处 v-html（scripts/check.sh 硬规则），
 * 所以按钮不能另起模板节点；同时把 <button> 塞进被清洗的 HTML 字符串里
 * 等于要放宽 DOMPurify 白名单、还要在 sanitize 后拼接事件，反而更脆。
 * 这里在 v-html 渲染完成后遍历已清洗的 DOM 包一层工具条，
 * 事件用 addEventListener 直接挂，不经过 innerHTML，也不新增 v-html。
 *
 * 幂等：pre 已被 .md-code 包裹则跳过，避免流式重渲染时套娃。
 */
const LANG_LABELS = {
  javascript: 'JavaScript',
  typescript: 'TypeScript',
  java: 'Java',
  sql: 'SQL',
  bash: 'Bash',
  json: 'JSON',
  xml: 'HTML',
  yaml: 'YAML',
  python: 'Python',
  go: 'Go'
}

function labelFor(lang) {
  if (!lang) return '代码'
  return LANG_LABELS[lang] || lang
}

/**
 * 无语言标记时嗅探代码内容。
 *
 * 题库里大量老数据写成裸 ```（导入时没带语言），只显示「代码」等于没信息；
 * 这里用几条高区分度特征猜一个，猜不出仍退回「代码」，不引入任何依赖。
 */
function sniffLang(code) {
  const text = (code || '').trim()
  if (!text) return ''
  if (/^\s*<(!doctype|html|div|dependency|bean|project)\b/i.test(text)) return 'xml'
  if (/^\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER)\b/i.test(text)) return 'sql'
  if (/^\s*[[{][\s\S]*[\]}]\s*$/.test(text) && /"[^"]+"\s*:/.test(text)) return 'json'
  if (/\b(public|private|protected)\s+(static\s+)?[\w<>[\]]+\s+\w+\s*\(/.test(text)) return 'java'
  // 无方法签名的 Java 片段：类型 + 变量声明，或以分号结尾的语句
  if (/\b(long|int|String|boolean|double|float|char|var|final)\s+\w+\s*=/.test(text)) return 'java'
  if (/\bSystem\.(out|currentTimeMillis|nanoTime)\b/.test(text)) return 'java'
  if (/\b(def|import\s+\w+$|from\s+\w+\s+import)\b/.test(text)) return 'python'
  if (/^\s*(\/\/|#!|[$>]\s)/.test(text)) return 'bash'
  return ''
}

/** 复制纯文本：优先 Clipboard API，失败退回 textarea + execCommand。 */
async function copyText(text) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    // 非安全上下文或用户拒绝授权，走下面的兜底
  }
  try {
    const area = document.createElement('textarea')
    area.value = text
    area.setAttribute('readonly', '')
    area.style.position = 'fixed'
    area.style.top = '-9999px'
    document.body.appendChild(area)
    area.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(area)
    return ok
  } catch {
    return false
  }
}

function flash(btn, ok) {
  const text = ok ? '已复制' : '复制失败'
  btn.textContent = text
  btn.classList.toggle('is-done', ok)
  clearTimeout(btn.__resetTimer)
  btn.__resetTimer = setTimeout(() => {
    btn.textContent = '复制'
    btn.classList.remove('is-done')
  }, 1600)
}

function decorateCodeBlocks() {
  const root = bodyRef.value
  if (!root) return
  root.querySelectorAll('pre').forEach((pre) => {
    if (pre.parentElement?.classList.contains('md-code')) return

    const wrap = document.createElement('div')
    wrap.className = 'md-code'
    pre.parentNode.insertBefore(wrap, pre)
    wrap.appendChild(pre)

    const bar = document.createElement('div')
    bar.className = 'md-code__bar'

    const codeEl = pre.querySelector('code')
    const lang = document.createElement('span')
    lang.className = 'md-code__lang'
    lang.textContent = labelFor(pre.dataset.lang || sniffLang(codeEl?.textContent || pre.textContent))

    const btn = document.createElement('button')
    btn.type = 'button'
    btn.className = 'md-code__copy'
    btn.textContent = '复制'
    btn.setAttribute('aria-label', '复制代码')
    btn.addEventListener('click', async () => {
      flash(btn, await copyText((codeEl || pre).textContent || ''))
    })

    bar.append(lang, btn)
    wrap.insertBefore(bar, pre)
  })
}

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
  // 等 v-html 真正落到 DOM 后再包工具条；token 已变说明内容又换了，交给新一轮
  if (token === renderToken && html.value) {
    await nextTick()
    if (token === renderToken) decorateCodeBlocks()
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
