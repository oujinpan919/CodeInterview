import { Marked } from 'marked'
import markedShiki from 'marked-shiki'
import DOMPurify from 'dompurify'

/**
 * 全站唯一的 Markdown 渲染链（02 §4.4 / 06 T06）：
 *   marked（异步）→ marked-shiki（Shiki 高亮）→ DOMPurify 清洗 → 由 MarkdownView 输出
 *
 * - Shiki 用 createHighlighterCore + 显式 10 种语言 + github-light，单例懒加载；
 *   不引 shiki 全量 bundle 入口。
 * - Shiki 失败时该代码块退化为无高亮的 <pre><code>，Markdown 其余部分照常渲染。
 * - 任何输出都必须经过 DOMPurify.sanitize(..., { USE_PROFILES: { html: true } })。
 */

const THEME = 'github-light'
const SUPPORTED_LANGUAGES = [
  'javascript',
  'typescript',
  'java',
  'sql',
  'bash',
  'json',
  'xml',
  'yaml',
  'python',
  'go'
]

const LANGUAGE_ALIASES = {
  js: 'javascript',
  jsx: 'javascript',
  mjs: 'javascript',
  cjs: 'javascript',
  ts: 'typescript',
  tsx: 'typescript',
  sh: 'bash',
  shell: 'bash',
  zsh: 'bash',
  yml: 'yaml',
  html: 'xml',
  htm: 'xml',
  vue: 'xml'
}

const LANGUAGE_LOADERS = {
  javascript: () => import('shiki/langs/javascript.mjs'),
  typescript: () => import('shiki/langs/typescript.mjs'),
  java: () => import('shiki/langs/java.mjs'),
  sql: () => import('shiki/langs/sql.mjs'),
  bash: () => import('shiki/langs/bash.mjs'),
  json: () => import('shiki/langs/json.mjs'),
  xml: () => import('shiki/langs/xml.mjs'),
  yaml: () => import('shiki/langs/yaml.mjs'),
  python: () => import('shiki/langs/python.mjs'),
  go: () => import('shiki/langs/go.mjs')
}

let highlighterPromise = null
let markedInstance = null

/** 单例懒加载：第一次渲染代码块时才创建 highlighter */
function loadHighlighter() {
  if (!highlighterPromise) {
    highlighterPromise = (async () => {
      const { createHighlighterCore } = await import('shiki/core')
      const { createOnigurumaEngine } = await import('shiki/engine/oniguruma')
      return createHighlighterCore({
        themes: [import('shiki/themes/github-light.mjs')],
        langs: Object.values(LANGUAGE_LOADERS).map((load) => load()),
        engine: createOnigurumaEngine(() => import('shiki/wasm'))
      })
    })()
  }
  return highlighterPromise
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function normalizeLanguage(lang) {
  const raw = String(lang || '')
    .trim()
    .toLowerCase()
  const mapped = LANGUAGE_ALIASES[raw] || raw
  return SUPPORTED_LANGUAGES.includes(mapped) ? mapped : ''
}

function fallbackCode(code, lang) {
  const language = String(lang || '').trim().toLowerCase()
  const className = language ? ` class="language-${escapeHtml(language)}"` : ''
  return `<pre class="md-code-fallback"><code${className}>${escapeHtml(code)}</code></pre>`
}

async function highlightCode(code, lang) {
  const language = normalizeLanguage(lang)
  if (!language) return fallbackCode(code, lang)
  try {
    const highlighter = await loadHighlighter()
    return highlighter.codeToHtml(code, { lang: language, theme: THEME })
  } catch {
    // Shiki 初始化/高亮失败时退化，DOMPurify 仍会在后面执行
    return fallbackCode(code, lang)
  }
}

function getMarked() {
  if (!markedInstance) {
    markedInstance = new Marked()
    markedInstance.use(markedShiki({ highlight: highlightCode }))
  }
  return markedInstance
}

// 外链统一 target=_blank + rel=noopener noreferrer（在清洗后由 hook 补属性）
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (!node || node.tagName !== 'A') return
  const href = node.getAttribute('href') || ''
  if (/^(https?:)?\/\//i.test(href)) {
    node.setAttribute('target', '_blank')
    node.setAttribute('rel', 'noopener noreferrer')
  }
})

function sanitize(html) {
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
}

/** Markdown → 已清洗 HTML；任何失败都退回纯文本（保留换行） */
export async function renderMarkdown(markdown) {
  const source = typeof markdown === 'string' ? markdown : ''
  if (!source.trim()) return ''
  try {
    const html = await getMarked().parse(source)
    return sanitize(html)
  } catch {
    return renderPlainText(source)
  }
}

/** 纯文本兜底（转义后放进 <pre>，再经 DOMPurify 清洗；任何路径都不得跳过 sanitize） */
export function renderPlainText(text) {
  const source = typeof text === 'string' ? text : ''
  return sanitize(`<pre class="md-plain-fallback">${escapeHtml(source)}</pre>`)
}
